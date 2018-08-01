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
        if (!json.containsKey("subtype")) {
          // type `message` doesn't need `action` key
          it.reply(JsonObject().putNull("subtype"))
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
    val subtype = json.getString("subtype")
    val result = JsonObject().put(subtype, false)

    if (!json.containsKey("id") || !json.containsKey("password")) {
      return result
    }
    try {
      val id = json.getString("id")
      val password = json.getString("password")

      //validate id
      val validId = when {
        id.length < 4 || id.length > 20 -> false
        id[0].isDigit() -> false
        else -> id.all { it.isLetterOrDigit() }
      }

      if (!validId)
        return result.put("info", "用户名格式错误，仅允许不以数字开头的数字和字母组合，长度在4到20位之间")

      //validate password
      if (password == null || password.length != 32) {
        return result.put("info", "秘钥格式错误")
      }

      val dir = config().getString("dir") + File.separator + id

      when (subtype) {
        "register" -> {
          if (vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")) {
            return result.put("info", "用户已存在")
          }
          vertx.fileSystem().mkdirsBlocking(dir)
          vertx.fileSystem().createFileBlocking(dir + File.separator + "user.json")
          json.removeAll { it.key in arrayOf("type", "subtype") }
          vertx.fileSystem().writeFileBlocking(dir + File.separator + "user.json", json.toBuffer())

          return result.put(subtype, true)
        }
        else -> {//login as default subtype
          if (!vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")) {
            return result.put(subtype, false)
          }
          val userJson = vertx.fileSystem().readFileBlocking(dir + File.separator + "user.json").toJsonObject()
          if (subtype == "left") {
            return result.put(subtype, userJson.getString("password") == json.getString("password"))
              .put("id", json.getString("id"))
          }
          return result.put(subtype, userJson.getString("password") == json.getString("password"))
        }
      }
    } catch (e: Exception) {
      return result.put("info", "${e.message}")
    }
  }

  fun search(json: JsonObject): JsonObject {
    val subtype = json.getString("subtype")
    val result = JsonObject().putNull(subtype)

    val dir = config().getString("dir") + File.separator + json.getString("keyword")
    val userFile = dir + File.separator + "user.json"

    try {
      if (!vertx.fileSystem().existsBlocking(userFile))
        return result.putNull(subtype)

      val buffer = vertx.fileSystem().readFileBlocking(userFile)
      val resJson = buffer.toJsonObject()
      resJson.removeAll { it.key in arrayOf("password") }
      return result.put(subtype, resJson)

    } catch (e: Exception) {
      return result.put("info", "${e.message}")
    }
  }

  private fun friend(json: JsonObject) {
    val subtype = json.getString(JsonKeys.SUBTYPE)
    val from = json.getString(JsonKeys.FROM)
    val to = json.getString(JsonKeys.TO)
    if (from == null || to == null) {
      //不做处理，不需要反复确认，因为io层次一多，反复确认会导致代码和性能上的浪费，不值得花大力气去确保这点意外
      //确保错误情况不会影响系统便可
      return
    }

    when (subtype) {
      ActionConstants.DELETE -> {
      }
      ActionConstants.REQUEST -> {
        val dir = config().getString("dir") + separator
        val fileSystem = vertx.fileSystem()
        if (!json.getString("from").contains('@')) {    //本地保存发送记录

          fileSystem.mkdirsBlocking("$dir$from$separator.send")
          if (fileSystem.existsBlocking("$dir$from$separator.send$separator$to.json")) {
            fileSystem.deleteBlocking("$dir$from$separator.send$separator$to.json")
          }
          fileSystem.createFileBlocking("$dir$from$separator.send$separator$to.json")
          fileSystem.writeFileBlocking("$dir$from$separator.send$separator$to.json", json.toBuffer())

        }
        if (to.contains("@")) {    //如果跨域，转发给你相应的服务器
          json.put("from", json.getString("from") + "@" + config().getString("host"))//把from加上域名
          webClient.post(config().getInteger("http-port"), to.substringAfterLast("@"), "/user")
            .sendJsonObject(json.put("to", to.substringBeforeLast('@'))) {}
        } else {    //接受是其他服务器发送过来的请求

          fileSystem.mkdirsBlocking("$dir$to$separator.receive")
          if (fileSystem.existsBlocking("$dir$to$separator.receive$separator$from.json")) {
            fileSystem.deleteBlocking("$dir$to$separator.receive$separator$from.json")
          }
          fileSystem.createFileBlocking("$dir$to$separator.receive$separator$from.json")
          fileSystem.writeFileBlocking("$dir$to$separator.receive$separator$from.json", json.toBuffer())
          //尝试投递
          vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)

        }
      }
      ActionConstants.RESPONSE -> {
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
    val fs = vertx.fileSystem()

    val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
    val dir = config().getString("dir") + File.separator
    val separator = File.separator
    val senderDir = "$dir$from$separator$to$separator$today.sv"
    val receiverDir = "$dir$to$separator$from$separator$today.sv"
    if (!fs.existsBlocking(senderDir)) fs.createFileBlocking(senderDir)
    if (!fs.existsBlocking(receiverDir)) fs.createFileBlocking(receiverDir)
    fs.openBlocking(senderDir, OpenOptions().setAppend(true)).write(json.toBuffer())
    fs.openBlocking(receiverDir, OpenOptions().setAppend(true)).write(json.toBuffer())

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
  }


  private fun defaultMessage(json: JsonObject): JsonObject {
    json.removeAll { it.key !in arrayOf(JsonKeys.VERSION, JsonKeys.TYPE) }
    json.put(JsonKeys.INFO, "Default info, please check all sent value is correct.")
    return json
  }

}

