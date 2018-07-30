package cn.net.polyglot.verticle

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.config.JsonKeys
import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import io.vertx.core.AbstractVerticle
import io.vertx.core.file.FileSystem
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.File.separator
import java.text.SimpleDateFormat
import java.util.*

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
          it.reply(JsonObject().putNull("action"))
          return@consumer
        }
      } catch (e: Exception) {
        if (e is ClassCastException) {
          it.reply(JsonObject().put("info", "value type error"))
        } else {
          it.reply(JsonObject().put("info", "${e.message}"))
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
      return result.put("info", "${e.message}")
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
      return result.put("info", "${e.message}")
    }
  }

  private fun friend(json: JsonObject) {
    val action = json.getString(JsonKeys.ACTION)
    var from = json.getString(JsonKeys.FROM)
    var to = json.getString(JsonKeys.TO)
    if (from == null || to == null) {
      //不做处理，不需要反复确认，因为io层次一多，反复确认会导致代码和性能上的浪费，不值得花大力气去确保这点意外
      //确保错误情况不会影响系统便可
      return
    }

    when (action) {
      ActionConstants.DELETE -> {
      }
      ActionConstants.REQUEST -> {
        if (to.contains("@")) {
          json.put("from", json.getString("from") + "@" + config().getString("host"))//把from加上域名
          webClient.post(config().getInteger("http-port"), to.substringAfterLast("@"), "/user").sendJsonObject(json) {}
        } else {
          if (json.getString("from").contains('@')) {
            from = from.substringBeforeLast('@')
          }
          val dir = config().getString("dir") + separator
          vertx.fileSystem().mkdirsBlocking("$dir$from$separator.send")
          if (vertx.fileSystem().existsBlocking("$dir$from$separator.send$separator$to.json"))
            vertx.fileSystem().deleteBlocking("$dir$from$separator.send$separator$to.json")
          vertx.fileSystem().createFileBlocking("$dir$from$separator.send$separator$to.json")
          vertx.fileSystem().writeFileBlocking("$dir$from$separator.send$separator$to.json", json.toBuffer())


          vertx.fileSystem().mkdirsBlocking("$dir$to$separator.receive")
          if (vertx.fileSystem().existsBlocking("$dir$to$separator.receive$separator$from.json"))
            vertx.fileSystem().deleteBlocking("$dir$to$separator.receive$separator$from.json")
          vertx.fileSystem().createFileBlocking("$dir$to$separator.receive$separator$from.json")
          vertx.fileSystem().writeFileBlocking("$dir$to$separator.receive$separator$from.json", json.toBuffer())
          //尝试投递
          vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)
        }
      }
      ActionConstants.RESPONSE -> {
        if (json.getString("to").contains('@')) {
          webClient.post(config().getInteger("http-port"), json.getString("to").substringAfterLast('@'), "/user").sendJson(json) {}
        } else {
          val dir = config().getString("dir") + separator
          val fs = vertx.fileSystem()

          if (fs.existsBlocking("$dir$from$separator.receive$separator$to.json") &&
            fs.existsBlocking("$dir$to$separator.send$separator$from.json")) {
            if (json.getBoolean("accept")) {
              if (!fs.existsBlocking("$dir$from$separator$to")) {
                fs.mkdirsBlocking("$dir$from$separator$to")
                val fileDir = "$dir$from$separator$to$separator$to.json"
                fs.createFileBlocking(fileDir)
                fs.writeFileBlocking(fileDir, JsonObject()
                  .put("id", to)
                  .put("nickName", to)
                  .toBuffer())
              }
              if (!fs.existsBlocking("$dir$to$separator$from")) {
                fs.mkdirsBlocking("$dir$to$separator$from")
                val fileDir1 = "$dir$to$separator$from$separator$from.json"
                fs.createFileBlocking(fileDir1)
                fs.writeFileBlocking(fileDir1, JsonObject()
                  .put("id", from)
                  .put("nickName", from)
                  .toBuffer())
              }
            }
            fs.deleteBlocking("$dir$from$separator.receive$separator$to.json")
            fs.deleteBlocking("$dir$to$separator.send$separator$from.json")
            vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)
          }
        }
      }
      else -> {
      }
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
      saveSendRecord(fs, json)
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

  private fun saveSendRecord(fs: FileSystem, json: JsonObject): Boolean {   //将该条消息分别写入双方的发送记录中
    try {
      val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
      val dir = config().getString("dir") + File.separator
      val from = json.getString("from")
      val to = json.getString("to")
      val separator = File.separator
      val senderDir = "$dir$from$separator$to$separator$today.sv"
      val receiverDir = "$dir$to$separator$from$separator$today.sv"
      if (!fs.existsBlocking(senderDir)) fs.createFileBlocking(senderDir)
      if (!fs.existsBlocking(receiverDir)) fs.createFileBlocking(receiverDir)
      fs.openBlocking(senderDir, OpenOptions().setAppend(true)).write(json.toBuffer())
      fs.openBlocking(receiverDir, OpenOptions().setAppend(true)).write(json.toBuffer())
    } catch (e: Exception) {
      return false
    }
    return true
  }
}

