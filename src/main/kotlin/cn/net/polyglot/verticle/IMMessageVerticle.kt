package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.json.JsonObject
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
        if (!json.containsKey(TYPE)) {
          it.reply(JsonObject().putNull(TYPE))
          return@consumer
        }
        if (!json.containsKey(SUBTYPE)) {
          // type `message` doesn't need `action` key
          it.reply(JsonObject().putNull(SUBTYPE))
          return@consumer
        }
      } catch (e: Exception) {
        if (e is ClassCastException) {
          it.reply(JsonObject().put(INFO, "value type error"))
        } else {
          it.reply(JsonObject().put(INFO, "${e.message}"))
        }
        return@consumer
      }

      launch(vertx.dispatcher()) {
        when (json.getString(TYPE)) {
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

  private suspend fun user(json: JsonObject): JsonObject {

    val subtype = json.getString(SUBTYPE)
    val result = JsonObject().put(subtype, false)

    if (!json.containsKey(ID) || !json.containsKey(PASSWORD)) {
      return result
    }
    try {
      val id = json.getString(ID)
      val password = json.getString(PASSWORD)

      //validate id
      val validId = when {
        id.length < 4 || id.length > 20 -> false
        id[0].isDigit() -> false
        else -> id.all { it.isLetterOrDigit() }
      }

      if (!validId)
        return result.put(INFO, "用户名格式错误，仅允许不以数字开头的数字和字母组合，长度在4到20位之间")

      //validate password
      if (password == null || password.length != 32) {
        return result.put(INFO, "秘钥格式错误")
      }

      val dir = config().getString(DIR) + File.separator + id

      when (subtype) {
        REGISTER -> {
          if (vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")) {
            return result.put(INFO, "用户已存在")
          }
          vertx.fileSystem().mkdirsBlocking(dir)
          vertx.fileSystem().createFileBlocking(dir + File.separator + "user.json")
          json.removeAll { it.key in arrayOf(TYPE, SUBTYPE) }
          vertx.fileSystem().writeFileBlocking(dir + File.separator + "user.json", json.toBuffer())

          return result.put(subtype, true)
        }
        OFFLINE -> {

          val fs = vertx.fileSystem()
          val messages = JsonArray()
          val friends = JsonArray()
          val userJson = fs.readFileBlocking("$dir${separator}user.json").toJsonObject()
          if(!fs.existsBlocking("$dir$separator.message")) fs.mkdirBlocking("$dir$separator.message")
          if(!fs.existsBlocking("$dir$separator.receive")) fs.mkdirBlocking("$dir$separator.receive")
          val messageList = fs.readDirBlocking("$dir$separator.message")
          val receiveList = fs.readDirBlocking("$dir$separator.receive")
          if (fs.existsBlocking(dir + File.separator + "user.json")
            && json.getString(PASSWORD) == userJson.getString(PASSWORD)) {
            for (file in messageList) {
              val msgs = fs.readFileBlocking(file).toString().trim().split(END)
              for (message in msgs) messages.add(JsonObject(message))
            }
            for (file in receiveList) {
              val requests = fs.readFileBlocking(file).toString().trim().split(END)
              for (request in requests) friends.add(JsonObject(request))
            }
            if(friends.size()>0) result.put(FRIENDS, friends)
            if(messages.size()>0) result.put(MESSAGES, messages)
            return result.put(subtype, true)
              .put(ID, json.getString(ID))
          }
          return result.put(subtype, false)
            .put(ID, json.getString(ID))
        }
        else -> {//login as default subtype
          if (!vertx.fileSystem().existsBlocking(dir + File.separator + "user.json")) {
            return result.put(subtype, false)
          }
          val fs = vertx.fileSystem()
          val userJson = fs.readFileBlocking(dir + File.separator + "user.json").toJsonObject()
          val friendList = JsonArray()
          val friends = fs.readDirBlocking(dir)
          if (userJson.getString(PASSWORD) == json.getString(PASSWORD)) {
            for (friend in friends) {
              val friendId = friend.substringAfterLast(File.separator)
              if (fs.lpropsBlocking(friend).isDirectory && !friendId.startsWith(".")) {
                friendList.add(fs.readFileBlocking("$friend${File.separator}$friendId.json").toJsonObject())
              }
            }
            return result.put(subtype,true)
              .put(NICKNAME,json.getString(ID))
              .put(FRIENDS,friendList)
          }
          return result.put(subtype, false)
        }
      }
    } catch (e: Exception) {
      return result.put(INFO, "${e.message}")
    }
  }

  private suspend fun search(json: JsonObject): JsonObject {
    val subtype = json.getString(SUBTYPE)

    val dir = config().getString(DIR) + File.separator + json.getString(KEYWORD)
    val userFile = dir + File.separator + "user.json"

    json.clear()

    if(vertx.fileSystem().existsBlocking(userFile)){
      val buffer = vertx.fileSystem().readFileBlocking(userFile)
      val resJson = buffer.toJsonObject()
      resJson.remove(PASSWORD)
      json.put(USER, resJson)
    }else{
      json.putNull(USER)
    }

    return json
  }

  private suspend fun friend(json: JsonObject) {
    val subtype = json.getString(SUBTYPE)
    val from = json.getString(FROM)
    val to = json.getString(TO)
    if (from == null || to == null) {
      //不做处理，不需要反复确认，因为io层次一多，反复确认会导致代码和性能上的浪费，不值得花大力气去确保这点意外
      //确保错误情况不会影响系统便可
      return
    }

    when (subtype) {
      DELETE -> {
      }
      REQUEST -> {
        val dir = config().getString(DIR) + separator
        val fs = vertx.fileSystem()
        if (!json.getString(FROM).contains("@")) {    //本地保存发送记录

          if(!fs.existsBlocking("$dir$from$separator.send"))
            fs.mkdirsBlocking("$dir$from$separator.send")
          if (fs.existsBlocking("$dir$from$separator.send$separator$to.json")) {
            fs.deleteBlocking("$dir$from$separator.send$separator$to.json")
          }
          fs.createFileBlocking("$dir$from$separator.send$separator$to.json")
          fs.writeFileBlocking("$dir$from$separator.send$separator$to.json", json.toBuffer())

        }
        if (to.contains("@")) {    //如果跨域，转发给你相应的服务器
          json.put(FROM, json.getString(FROM) + "@" + config().getString(HOST))//把from加上域名
          webClient.put(config().getInteger(HTTP_PORT), to.substringAfterLast("@"), "/$USER/$REQUEST")
            .sendJsonObject(json.put(TO, to.substringBeforeLast('@'))) {}
        } else {    //接受是其他服务器发送过来的请求

          fs.mkdirsBlocking("$dir$to$separator.receive")
          if (fs.existsBlocking("$dir$to$separator.receive$separator$from.json")) {
            fs.deleteBlocking("$dir$to$separator.receive$separator$from.json")
          }
          fs.createFileBlocking("$dir$to$separator.receive$separator$from.json")
          fs.writeFileBlocking("$dir$to$separator.receive$separator$from.json", json.toBuffer().appendString(END))
          //尝试投递
          vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)

        }
      }
      RESPONSE -> {
        val dir = config().getString(DIR) + separator
        val fs = vertx.fileSystem()

        if (!json.getString(FROM).contains("@")) {
          if (fs.existsBlocking("$dir$from$separator.receive$separator$to.json")) {
            fs.deleteBlocking("$dir$from$separator.receive$separator$to.json")//删除
            if (json.getBoolean(ACCEPT)) {
              if (!fs.existsBlocking("$dir$from$separator$to")) {
                fs.mkdirsBlocking("$dir$from$separator$to")
                val fileDir = "$dir$from$separator$to$separator$to.json"
                fs.createFileBlocking(fileDir)
                fs.writeFileBlocking(fileDir, JsonObject()
                  .put(ID, to)
                  .put(NICKNAME, json.getString(NICKNAME) ?: to)
                  .toBuffer())
              }
            }
          }else{
            return //错误，没有收到好友请求，流程到此结束
          }
        }

        if (json.getString(TO).contains("@")){
          json.put(FROM, json.getString(FROM) + "@" + config().getString(HOST))//把from加上域名
          webClient.put(config().getInteger(HTTP_PORT), to.substringAfterLast("@"), "/$USER/$RESPONSE")
            .sendJsonObject(json.put(TO, to.substringBeforeLast("@"))) {}

        }else{
          if (fs.existsBlocking("$dir$to$separator.send$separator$from.json")) {
            fs.deleteBlocking("$dir$to$separator.send$separator$from.json")
            if (json.getBoolean(ACCEPT)) {
              if (!fs.existsBlocking("$dir$to$separator$from")) {
                fs.mkdirsBlocking("$dir$to$separator$from")
                val fileDir1 = "$dir$to$separator$from$separator$from.json"
                fs.createFileBlocking(fileDir1)
                fs.writeFileBlocking(fileDir1, JsonObject()
                  .put(ID, from)
                  .put(NICKNAME, json.getString(NICKNAME)?:from)
                  .toBuffer())
              }
              //尝试投递
              vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)
            }
          }
        }
      }
      else -> {
      }
    }
  }

  private suspend fun message(json: JsonObject) {
    val from = json.getString(FROM)
    val to = json.getString(TO)
    if (from == null || to == null) {
      return
    }
    val fs = vertx.fileSystem()

    val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
    val dir = config().getString(DIR) + File.separator
    val separator = File.separator
    val senderDir = "$dir$from$separator$to$separator$today.sv"
    val receiverDir = "$dir$to$separator$from$separator$today.sv"
    if (!fs.existsBlocking(senderDir)) fs.createFileBlocking(senderDir)
    if (!fs.existsBlocking(receiverDir)) fs.createFileBlocking(receiverDir)
    fs.openBlocking(senderDir, OpenOptions().setAppend(true)).write(json.toBuffer())
    fs.openBlocking(receiverDir, OpenOptions().setAppend(true)).write(json.toBuffer())

    vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)
  }


  private fun defaultMessage(json: JsonObject): JsonObject {
    json.removeAll { it.key !in arrayOf(VERSION, TYPE) }
    json.put(INFO, "Default info, please check all sent value is correct.")
    return json
  }

}

