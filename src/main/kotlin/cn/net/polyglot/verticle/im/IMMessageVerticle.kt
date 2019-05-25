/**
MIT License

Copyright (c) 2018 White Wood City

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package cn.net.polyglot.verticle.im

import cn.net.polyglot.config.*
import cn.net.polyglot.module.containsSensitiveWords
import cn.net.polyglot.module.lowerCaseValue
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.file.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File
import java.io.File.separator
import java.text.SimpleDateFormat
import java.util.*

@Deprecated("Old Verticles", replaceWith = ReplaceWith("UserVerticle","cn.net.polyglot.verticle.user"))
class IMMessageVerticle : CoroutineVerticle() {

  private lateinit var webClient: WebClient

  override suspend fun start() {
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

      launch {
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

    json.lowerCaseValue(ID)

    val subtype = json.getString(SUBTYPE)
    val result = JsonObject()
      .put(TYPE, json.getString(TYPE))
      .put(SUBTYPE, subtype)
      .put(subtype, false)

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
        else -> id.all { it.isLetterOrDigit() } && !containsSensitiveWords(id)//不包含有敏感词
      }

      if (!validId)
        return result.put(INFO, "用户名格式错误，仅允许不以数字开头的数字和字母组合，长度在4到20位之间")

      //validate password
      if (password == null || password.length != 32) {
        return result.put(INFO, "秘钥格式错误")
      }

      val dir = config.getString(DIR) + File.separator + id

      when (subtype) {
        REGISTER -> {
          if (vertx.fileSystem().existsAwait(dir + File.separator + "user.json")) {
            return result.put(INFO, "用户已存在")
          }
          vertx.fileSystem().mkdirsAwait(dir)
          vertx.fileSystem().createFileAwait(dir + File.separator + "user.json")
          json.removeAll { it.key in arrayOf(TYPE, SUBTYPE) }
          vertx.fileSystem().writeFileAwait(dir + File.separator + "user.json", json.toBuffer())

          return result.put(subtype, true)
        }
        OFFLINE -> {//离线消息及好友请求
          if(!verify(dir, password)){
            return result
          }

          val fs = vertx.fileSystem()
          val messages = JsonArray()
          val friends = JsonArray()
          val userJson = fs.readFileAwait("$dir${separator}user.json").toJsonObject()
          if (!fs.existsAwait("$dir$separator.message")) fs.mkdirAwait("$dir$separator.message")
          if (!fs.existsAwait("$dir$separator.receive")) fs.mkdirAwait("$dir$separator.receive")
          val messageList = fs.readDirAwait("$dir$separator.message")
          val receiveList = fs.readDirAwait("$dir$separator.receive")
          if (fs.existsAwait(dir + File.separator + "user.json")
            && json.getString(PASSWORD) == userJson.getString(PASSWORD)) {
            for (file in messageList) {
              val msgs = fs.readFileAwait(file).toString().trim().split(END)
              for (message in msgs) messages.add(JsonObject(message))
            }
            for (file in receiveList) {
              val requests = fs.readFileAwait(file).toString().trim().split(END)
              for (request in requests) friends.add(JsonObject(request))
            }
            if (friends.size() > 0) result.put(FRIENDS, friends)
            if (messages.size() > 0) result.put(MESSAGES, messages)

            fs.deleteRecursiveAwait("$dir$separator.message", true)

            return result.put(subtype, true)
              .put(ID, json.getString(ID))
          }
          return result.put(subtype, false)
            .put(ID, json.getString(ID))
        }
        LOGIN -> {
          if(!verify(dir, password)){
            return result
          }

          val fs = vertx.fileSystem()
          val friendList = JsonArray()
          val friends = fs.readDirAwait(dir)
          for (friend in friends) {
            val friendId = friend.substringAfterLast(File.separator)
            if (fs.lpropsAwait(friend).isDirectory && !friendId.startsWith(".")) {
              friendList.add(fs.readFileAwait("$friend${File.separator}$friendId.json").toJsonObject())
            }
          }

          json.mergeIn(vertx.fileSystem().readFileAwait(dir + File.separator + "user.json").toJsonObject())

          return result.put(subtype, true)
            .put(ID, json.getString(ID))
            .put(NICKNAME, json.getString(NICKNAME))
            .put(FRIENDS, friendList)
        }
        HISTORY->{
          if(!verify(dir, password)){
            return result
          }

          val friend = json.getString(FRIEND)
          val friendDir = dir+File.separator+friend
          val fs        = vertx.fileSystem()
          val messageList = fs.readDirAwait(friendDir,"\\d{4}-\\d{2}-\\d{2}\\.sv")
          messageList.sorted()
          messageList.reversed()
          val date = if (json.containsKey(DATE))
            json.getString(DATE)
          else
            SimpleDateFormat("yyyy-MM-dd").format(Date())

          val array= JsonArray()
          for(msg in messageList){
            if(date >= msg){
              val messages = fs.readFileAwait(msg).toString().trim().split(END)
              val jsonMsgs = JsonArray()
              for (message in messages){
                try {
                  val jsonMessage = JsonObject(message)
                  jsonMsgs.add(jsonMessage)
                }catch (e:Throwable){
                  //可能有并发操作导致的json string不完整，直接抛弃该string
                }
              }
              array.list.addAll(0,jsonMsgs.list)
            }
            if(array.size()>20 && date>msg) break//date > msg确保能够读取更早一天的记录，如果当天聊天记录超过20的话，没有该条件便会一直读取当天记录
          }
          result.put(HISTORY,true)
          result.put(MESSAGES,array)
          return result
        }
        else -> return defaultMessage(json)
      }
    } catch (e: Exception) {
      print(e)
      return result.put(INFO, "${e.message}")
    }
  }

  //取出为独立函数，为后续搜索聊天内容做准备
  private suspend fun verify(userDir:String, password:String):Boolean{

    if (vertx.fileSystem().existsAwait(userDir + File.separator + "user.json")) {
      val fs = vertx.fileSystem()
      val userJson = fs.readFileAwait(userDir + File.separator + "user.json").toJsonObject()
      if (userJson.getString(PASSWORD) == password) {
        return true
      }
    }

    return false
  }

  private suspend fun search(json: JsonObject): JsonObject {
//    val subtype = json.getString(SUBTYPE)

    val userDir = config.getString(DIR) + File.separator + json.getString(KEYWORD).toLowerCase()
    val userFile = userDir + File.separator + "user.json"

    json.clear()

    if (vertx.fileSystem().existsAwait(userFile)) {
      val buffer = vertx.fileSystem().readFileAwait(userFile)
      val resJson = buffer.toJsonObject()
      resJson.remove(PASSWORD)
      json.put(USER, resJson)
    } else {
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
        val dir = config.getString(DIR) + separator
        val fs = vertx.fileSystem()
        if (!json.getString(FROM).contains("@")) {    //本地保存发送记录

          if (!fs.existsAwait("$dir$from$separator.send"))
            fs.mkdirsAwait("$dir$from$separator.send")
          if (fs.existsAwait("$dir$from$separator.send$separator$to.json")) {
            fs.deleteAwait("$dir$from$separator.send$separator$to.json")
          }
          fs.createFileAwait("$dir$from$separator.send$separator$to.json")
          fs.writeFileAwait("$dir$from$separator.send$separator$to.json", json.toBuffer())

        }
        if (to.contains("@")) {    //如果跨域，转发给你相应的服务器
          json.put(FROM, json.getString(FROM) + "@" + config.getString(HOST))//把from加上域名
          webClient.put(config.getInteger(HTTP_PORT), to.substringAfterLast("@"), "/$USER/$REQUEST")
            .sendJsonObject(json.put(TO, to.substringBeforeLast('@'))) {}
        } else {    //接受是其他服务器发送过来的请求

          fs.mkdirsAwait("$dir$to$separator.receive")
          if (fs.existsAwait("$dir$to$separator.receive$separator$from.json")) {
            fs.deleteAwait("$dir$to$separator.receive$separator$from.json")
          }
          fs.createFileAwait("$dir$to$separator.receive$separator$from.json")
          fs.writeFileAwait("$dir$to$separator.receive$separator$from.json", json.toBuffer().appendString(END))
          //尝试投递
          vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)

        }
      }
      RESPONSE -> {
        val dir = config.getString(DIR) + separator
        val fs = vertx.fileSystem()

        if (!json.getString(FROM).contains("@")) {
          if (fs.existsAwait("$dir$from$separator.receive$separator$to.json")) {
            fs.deleteAwait("$dir$from$separator.receive$separator$to.json")//删除
            if (json.getBoolean(ACCEPT)) {
              if (!fs.existsAwait("$dir$from$separator$to")) {
                fs.mkdirsAwait("$dir$from$separator$to")
                val fileDir = "$dir$from$separator$to$separator$to.json"
                fs.createFileAwait(fileDir)
                fs.writeFileAwait(fileDir, JsonObject()
                  .put(ID, to)
                  .put(NICKNAME, json.getString(NICKNAME) ?: to)
                  .toBuffer())
              }
            }
          } else {
            return //错误，没有收到好友请求，流程到此结束
          }
        }

        if (json.getString(TO).contains("@")) {
          json.put(FROM, json.getString(FROM) + "@" + config.getString(HOST))//把from加上域名
          webClient.put(config.getInteger(HTTP_PORT), to.substringAfterLast("@"), "/$USER/$RESPONSE")
            .sendJsonObject(json.put(TO, to.substringBeforeLast("@"))) {}
        } else {
          if (fs.existsAwait("$dir$to$separator.send$separator$from.json")) {
            fs.deleteAwait("$dir$to$separator.send$separator$from.json")
            if (json.getBoolean(ACCEPT)) {
              if (!fs.existsAwait("$dir$to$separator$from")) {
                fs.mkdirsAwait("$dir$to$separator$from")
                val fileDir1 = "$dir$to$separator$from$separator$from.json"
                fs.createFileAwait(fileDir1)
                fs.writeFileAwait(fileDir1, JsonObject()
                  .put(ID, from)
                  .put(NICKNAME, json.getString(NICKNAME) ?: from)
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
    val instant = SimpleDateFormat("hh:mm:ss").format(Date())

    json.put(DATE, today)
    json.put(TIME, instant)

    val dir = config.getString(DIR) + separator

    if (!json.getString(FROM).contains("@")) {
      val senderDir = "$dir$from$separator$to"
      if (!fs.existsAwait(senderDir)) {
        return //错误，该用户没有该好友
      }
      val senderFile = "$senderDir$separator$today.sv"
      if (!fs.existsAwait(senderFile)) fs.createFileAwait(senderFile)
      fs.openAwait(senderFile, OpenOptions().setAppend(true)).write(json.toBuffer().appendBuffer(Buffer.buffer(END)))
    }

    if (json.getString(TO).contains("@")) {
      json.put(FROM, json.getString(FROM) + "@" + config.getString(HOST))//把from加上域名
      webClient.put(config.getInteger(HTTP_PORT), to.substringAfterLast("@"), "/$MESSAGE/$${json.getString(SUBTYPE)}")
        .sendJsonObject(json.put(TO, to.substringBeforeLast("@"))) {}
    } else {
      val receiverDir = "$dir$to$separator$from"
      if (!fs.existsAwait(receiverDir)) {
        return //错误，该用户没有该好友
      }
      val receiverFile = "$receiverDir$separator$today.sv"
      if (!fs.existsAwait(receiverFile)) fs.createFileAwait(receiverFile)
      fs.openAwait(receiverFile, OpenOptions().setAppend(true)).write(json.toBuffer().appendBuffer(Buffer.buffer(END)))
      //尝试投递
      vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)
    }
  }

  private fun defaultMessage(json: JsonObject): JsonObject {
    json.removeAll { it.key !in arrayOf(VERSION, TYPE) }
    json.put(INFO, "Default info, please check all sent value is correct.")
    return json
  }

}
