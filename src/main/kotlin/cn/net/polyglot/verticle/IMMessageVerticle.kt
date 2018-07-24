package cn.net.polyglot.verticle

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.config.JsonKeys
import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import cn.net.polyglot.handler.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import java.io.File

class IMMessageVerticle : AbstractVerticle() {

  private lateinit var webClient: WebClient

  override fun start() {
    webClient = WebClient.create(vertx)

    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      val json = it.body()
      try{
        val type = json.getString("type")
        val action = json.getString("action")

        if(type==null){
          it.reply(JsonObject().putNull("type"))
          return@consumer
        }

        if(action==null) {
          // message has no `action` key
          if (type != "message") {
            it.reply(JsonObject().putNull("action"))
            return@consumer
          }
        }
      }catch (e:Exception){
        it.reply(JsonObject()
          .putNull("type").putNull("action")
          .put("info",e.message))
        return@consumer
      }
      launch(vertx.dispatcher()) {
        when (it.body().getString("type")) {
          USER -> it.reply(user(it.body()))
          SEARCH -> it.reply(search(it.body()))
          FRIEND -> it.reply(friend(it.body()))
          MESSAGE -> message(it.body(), it)
          else -> defaultMessage(it.body())
        }
      }
    }
    println("${this::class.java.name} is deployed")
  }

  private fun user(json: JsonObject) :JsonObject{
    val action = json.getString("action")
    val result = JsonObject().put(action, false)

    if (!json.containsKey("user") || !json.containsKey("crypto")) {
      return result
    }
    try {
      val user = json.getString("user")
      val crypto = json.getString("crypto")

      //validate user
      val validUser = when {
        user.length < 4 || user.length > 20 -> false
        user[0].isDigit() -> false
        else -> user.all { it.isLetterOrDigit() }
      }

      if(!validUser)
        return result.put("info","用户名格式错误，仅允许不以数字开头的数字和字母组合，长度在4到20位之间")

      //validate crypto
      if(crypto == null || crypto.length != 32){
        return result.put("info","秘钥格式错误")
      }

      val dir = config().getString("dir") + File.separator + USER + File.separator + user

      when(action){
        "register" -> {
          if(vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")){
            return result.put("info","用户已存在")
          }
          vertx.fileSystem().mkdirsBlocking(dir)
          vertx.fileSystem().createFileBlocking(dir + File.separator + "user.json")
          json.removeAll {it.key in arrayOf("type", "action")}
          vertx.fileSystem().writeFileBlocking(dir + File.separator + "user.json", json.toBuffer())

          return result.put(action,true)
        }
        else -> {//login as default action
          if(!vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")){
            return result.put(action, false)
          }
          val userJson = vertx.fileSystem().readFileBlocking(dir + File.separator + "user.json").toJsonObject()
          return result.put(action, userJson.getString("crypto") == json.getString("crypto"))
        }
      }
    }catch (e:Exception){
      return result.put("info",e.message)
    }
  }

  private fun search(json: JsonObject): JsonObject {
    val action = json.getString("action")
    val result = JsonObject().putNull(action)

    val dir = config().getString("dir") + File.separator + USER + File.separator + json.getString("keyword")
    val userFile = dir + File.separator + "user.json"

    try {
      if(!vertx.fileSystem().existsBlocking(userFile))
        return result.putNull(action)

      val buffer = vertx.fileSystem().readFileBlocking(userFile)
      val resJson = buffer.toJsonObject()
      resJson.removeAll { it.key in arrayOf("crypto") }
      return result.put(action, resJson)

    } catch (e: Exception) {
      return result.put("info", e.message)
    }
  }

  private fun friend(json: JsonObject): JsonObject {
    val action = json.getString(JsonKeys.ACTION)
    val from = json.getString(JsonKeys.FROM)
    val to = json.getString(JsonKeys.TO)
    val fs = vertx.fileSystem()
    val checkValid = json.containsKey(JsonKeys.FROM)
    if (!checkValid) {
      json.put(JsonKeys.INFO, "lack json key `from`")
      return json
    }

    return when (action) {
      ActionConstants.DELETE -> handleFriendDelete(fs, json, from, to)
//   request to be friends
      ActionConstants.REQUEST -> handleFriendRequest(fs, json)
//   reply whether to accept the request
      ActionConstants.RESPONSE -> handleFriendResponse(fs, json, from, to)
//    list friends
      ActionConstants.LIST -> handleFriendList(fs, json, from)
      else -> defaultMessage(fs, json)
    }
  }

  private fun message(json: JsonObject, msg: Message<JsonObject>) {
    val from = json.getString("from")
    val to = json.getString("to")
    val host = json.getString("host") ?: "test.net.cn"
    val isSameDomain =
      when {
        from == null || to == null -> false
        '@' !in from || '@' !in to -> true
        else -> from.substringAfterLast("@")
          .let {
            it == to.substringAfterLast("@")
              && it == host
          }
      }

    if (isSameDomain) {
      // TCP
      vertx.eventBus().send<JsonObject>(IMTcpServerVerticle::class.java.name, json) {
        if (it.succeeded()) {
          println(it.result().body())
          msg.reply(JsonObject("""{"info":"TCP succeed"}"""))
        }
      }
    } else {
      // webClient
      val port = config().getInteger("http-port")
      webClient.post(port, host, "/").sendJsonObject(json) {
        if (it.succeeded()) {
          val result = it.result()
          println(result.bodyAsJsonObject())
          msg.reply(result.bodyAsJsonObject())
        }
      }
    }
  }

  private fun defaultMessage(json: JsonObject): JsonObject {
    json.removeAll { it.key !in arrayOf(JsonKeys.VERSION, JsonKeys.TYPE) }
    json.put(JsonKeys.INFO, "Default info, please check all sent value is correct.")
    return json
  }
}
