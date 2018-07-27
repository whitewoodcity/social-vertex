package cn.net.polyglot.verticle

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.config.FileSystemConstants.FRIENDS
import cn.net.polyglot.config.JsonKeys
import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import cn.net.polyglot.handler.defaultMessage
import cn.net.polyglot.handler.handleFriendDelete
import cn.net.polyglot.handler.handleFriendList
import cn.net.polyglot.handler.handleFriendResponse
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import java.io.File

class IMMessageVerticle : AbstractVerticle() {

  private lateinit var webClient: WebClient

  override fun start() {
    webClient = WebClient.create(vertx)

    // consume messages from Http/TcpServerVerticle to IMMessageVerticle
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      val json = it.body()
      System.err.println(json)
      try {
        if (!json.containsKey("type")) {
          it.reply(JsonObject().putNull("type"))
          return@consumer
        }
        if (!json.containsKey("action")) {
          // type `message` doesn't need `action` key
          if (json.getString("type") != MESSAGE) {
            it.reply(JsonObject().putNull("action"))
          }
          return@consumer
        }
      } catch (e: Exception) {
        if (e is ClassCastException) {
          it.reply(JsonObject().put("info", "value type error"))
        } else {
          // e.message 应该算是危险写法, 有可能有注入风险
          it.reply(JsonObject().put("info", e.message ?: "other error"))
        }
        return@consumer
      }

      launch(vertx.dispatcher()) {
        when (json.getString("type")) {
        // future reply
          FRIEND -> friend(it.body(), it)
          MESSAGE -> message(it.body(), it)
        // synchronization reply
          USER -> it.reply(user(it.body()))
          SEARCH -> it.reply(search(it.body()))
          else -> it.reply(defaultMessage(it.body()))
        }
      }
    }
    println("${this::class.java.name} is deployed")
  }

  fun user(json: JsonObject): JsonObject {
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

      if (!validUser)
        return result.put("info", "用户名格式错误，仅允许不以数字开头的数字和字母组合，长度在4到20位之间")

      //validate crypto
      if (crypto == null || crypto.length != 32) {
        return result.put("info", "秘钥格式错误")
      }

      val dir = config().getString("dir") + File.separator + USER + File.separator + user
      val friendsDir = dir + File.separator + FRIENDS

      when (action) {
        "register" -> {
          if (vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")) {
            return result.put("info", "用户已存在")
          }
          vertx.fileSystem().mkdirsBlocking(friendsDir)
          vertx.fileSystem().createFileBlocking(dir + File.separator + "user.json")
          json.removeAll { it.key in arrayOf("type", "action") }
          vertx.fileSystem().writeFileBlocking(dir + File.separator + "user.json", json.toBuffer())

          return result.put(action, true)
        }
        else -> {//login as default action
          if (!vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")) {
            return result.put(action, false)
          }
          val userJson = vertx.fileSystem().readFileBlocking(dir + File.separator + "user.json").toJsonObject()
          return result.put(action, userJson.getString("crypto") == json.getString("crypto"))
        }
      }
    } catch (e: Exception) {
      return result.put("info", e.message)
    }
  }

  fun search(json: JsonObject): JsonObject {
    val action = json.getString("action")
    val result = JsonObject().putNull(action)

    val dir = config().getString("dir") + File.separator + USER + File.separator + json.getString("keyword")
    val userFile = dir + File.separator + "user.json"

    try {
      if (!vertx.fileSystem().existsBlocking(userFile))
        return result.putNull(action)

      val buffer = vertx.fileSystem().readFileBlocking(userFile)
      val resJson = buffer.toJsonObject()
      resJson.removeAll { it.key in arrayOf("crypto") }
      return result.put(action, resJson)

    } catch (e: Exception) {
      return result.put("info", e.message)
    }
  }

  fun friend(json: JsonObject, msg: Message<JsonObject>) {
    val action = json.getString(JsonKeys.ACTION)
    val from = json.getString(JsonKeys.FROM)
    val to = json.getString(JsonKeys.TO)
    val fs = vertx.fileSystem()
    val checkValid = json.containsKey(JsonKeys.FROM)
    val retJson = json.copy()
    if (!checkValid) {
      retJson.put(JsonKeys.INFO, "lack json key `from`")
      msg.reply(retJson)
      return
    }

    // list friends don't need neither `to` nor a Web Connection.
    if (action == ActionConstants.LIST) {
      msg.reply(handleFriendList(fs, json, from))
      return
    }

    when (action) {
      ActionConstants.DELETE -> handleFriendDelete(fs, retJson, from, to) {
        val oppositeJson = json.copy()
        oppositeJson.put(JsonKeys.FROM, to)
        oppositeJson.put(JsonKeys.TO, from)
        handleFriendDelete(fs, oppositeJson, to, from)
        msg.reply(retJson)
      }
//   request to be friends
      ActionConstants.REQUEST -> message(json, msg)
//   reply whether to accept the request
      ActionConstants.RESPONSE -> {
        val result = handleFriendResponse(fs, retJson, from, to,config())
        vertx.eventBus().send<JsonObject>(IMTcpServerVerticle::class.java.name,result){
          if (it.succeeded()){
            println(it.result().body())
          }else{
            print("failed:${it.cause()}")
          }
        }
      }
      else -> defaultMessage(fs, retJson)
    }
  }

  fun message(json: JsonObject, msg: Message<JsonObject>) {
    val from = json.getString("from")
    val to = json.getString("to")
    if (from == null || to == null) {
      msg.reply(JsonObject().put(JsonKeys.INFO, "please check key `from` and `to`."))
    }

    val host = config().getString("host")
    if (sameDomain(from, to, host)) {
      val fs = vertx.fileSystem()
      val sendDir = config().getString("dir") + File.separator + "user"+
        File.separator + json.getString("from")+File.separator+ ".send"
      val status = saveSendRecord(fs, sendDir, json)
      if (!status){
        msg.reply(JsonObject()
          .put("status","500")
          .put("info","Server error!")
        )
        return
      }
      vertx.eventBus().send<JsonObject>(IMTcpServerVerticle::class.java.name, json) {
        if (it.succeeded()) {
          val result = it.result().body()
          if (!result.getBoolean("status")) {
            val target = result.getString("to")
            val dir = config().getString("dir") + File.separator + "user" + File.separator + target + File.separator + ".receive"
            if (!fs.existsBlocking(dir)) {
              fs.mkdirBlocking(dir)
            }
            val filePath = dir + File.separator + to + ".json"
            if (!fs.existsBlocking(filePath)) {
              fs.createFileBlocking(filePath)
            }
            fs.writeFileBlocking(filePath, it.result().body().toBuffer())
          } else {
            print("status:" + it.result().body().getString("info"))
          }
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

  private fun sameDomain(from: String, to: String, host: String): Boolean {
    return when {
      '@' !in from || '@' !in to -> true
      else ->
        from.substringAfterLast("@").let {
          it == to.substringAfterLast("@") && it == host
        }
    }
  }

  private fun defaultMessage(json: JsonObject): JsonObject {
    json.removeAll { it.key !in arrayOf(JsonKeys.VERSION, JsonKeys.TYPE) }
    json.put(JsonKeys.INFO, "Default info, please check all sent value is correct.")
    return json
  }

  private fun saveSendRecord(fs: FileSystem, dir: String, json: JsonObject): Boolean {
    val fileDir = dir + File.separator + json.getString("to") + ".json"
    try {
      if (!fs.existsBlocking(dir)) {
        fs.mkdirsBlocking(dir)
      }
      if (!fs.existsBlocking(fileDir)) {
        fs.createFileBlocking(fileDir)
      }
      fs.writeFileBlocking(fileDir, json.toBuffer())
      return true
    } catch (e: Exception) {
      print("Save failed:${e.message}")
    }
    return false
  }
}

