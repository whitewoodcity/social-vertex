package cn.net.polyglot.verticle

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.config.JsonKeys
import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import io.vertx.core.AbstractVerticle
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.File.separator

class IMMessageVerticle : AbstractVerticle() {

  private lateinit var webClient: WebClient

  override fun start() {
    webClient = WebClient.create(vertx)

    // consume messages from Http/TcpServerVerticle to IMMessageVerticle
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      val json = it.body()
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
          FRIEND -> friend(it.body())
          MESSAGE -> message(it.body())
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

      val dir = config().getString("dir") + File.separator + user

      when (action) {
        "register" -> {
          if (vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")) {
            return result.put("info", "用户已存在")
          }
          vertx.fileSystem().mkdirsBlocking(dir)
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

    val dir = config().getString("dir") + File.separator + json.getString("keyword")
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

  fun friend(json: JsonObject) {
    val action = json.getString(JsonKeys.ACTION)
    val from = json.getString(JsonKeys.FROM)
    val to = json.getString(JsonKeys.TO)

    if (from == null) {
      return
    }

    when (action) {
      ActionConstants.DELETE -> {
      }
      ActionConstants.REQUEST -> {
        val dir = config().getString("dir") + separator
        vertx.fileSystem().mkdirsBlocking("$dir$from$separator.send")
        if(vertx.fileSystem().existsBlocking("$dir$from$separator.send$separator$to.json"))
          vertx.fileSystem().deleteBlocking("$dir$from$separator.send$separator$to.json")
        vertx.fileSystem().createFileBlocking("$dir$from$separator.send$separator$to.json")
        vertx.fileSystem().writeFileBlocking("$dir$from$separator.send$separator$to.json",json.toBuffer())

      }
      ActionConstants.RESPONSE -> {
      }
      else -> {}
    }
  }

  fun message(json: JsonObject) {
    val from = json.getString("from")
    val to = json.getString("to")
    if (from == null || to == null) {
      return
    }

    val host = config().getString("host")
    if (sameDomain(from, to, host)) {
      val fs = vertx.fileSystem()
      val sendDir = config().getString("dir") + File.separator + "user"+
        File.separator + json.getString("from")+File.separator+ ".send"
      val status = saveSendRecord(fs, sendDir, json)
      if (!status){
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
            println("status:" + it.result().body().getString("info"))
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
      println("Save failed:${e.message}")
    }
    return false
  }
}

