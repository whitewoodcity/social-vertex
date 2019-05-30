package cn.net.polyglot.verticle.message

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.im.IMTcpServerVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.file.createFileAwait
import io.vertx.kotlin.core.file.existsAwait
import io.vertx.kotlin.core.file.openAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MessageVerticle : CoroutineVerticle() {
  private lateinit var webClient: WebClient

  override suspend fun start() {
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      launch { it.reply(message(it.body())) }
    }
  }

  private suspend fun message(json: JsonObject):JsonObject {
    return when(json.getString(SUBTYPE)){
      TEXT -> text(json)
      else -> json.put(MESSAGE, false)
    }
  }

  private suspend fun text(json: JsonObject):JsonObject {
    val from = json.getString(ID)
    val to = json.getString(TO)
    if (from == null || to == null) {
      return json.put(MESSAGE, false)//wrong format
    }
    val fs = vertx.fileSystem()

    val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
    val instant = SimpleDateFormat("hh:mm:ss").format(Date())

    json.put(DATE, today)
    json.put(TIME, instant)

    val dir = config.getString(DIR) + File.separator

    if (!from.contains("@")) {
      val senderDir = "$dir$from${File.separator}$to"
      if (!fs.existsAwait(senderDir)) {
        return json.put(MESSAGE, false)//错误，该用户没有该好友
      }
      val senderFile = "$senderDir${File.separator}$today.sv"
      if (!fs.existsAwait(senderFile)) fs.createFileAwait(senderFile)
      fs.openAwait(senderFile, OpenOptions().setAppend(true))
        .write(json.toBuffer().appendBuffer(Buffer.buffer(END)))
    }

    if (to.contains("@")) {
      json.put(ID, from + "@" + config.getString(HOST))//把from加上域名
      webClient.put(config.getInteger(HTTP_PORT), to.substringAfterLast("@"), "/$MESSAGE/$${json.getString(SUBTYPE)}")
        .sendJsonObject(json.put(TO, to.substringBeforeLast("@"))) {}
    } else {
      val receiverDir = "$dir$to${File.separator}$from"
      if (!fs.existsAwait(receiverDir)) {
        return json.put(MESSAGE, false)//错误，该用户没有该好友
      }
      val receiverFile = "$receiverDir${File.separator}$today.sv"
      if (!fs.existsAwait(receiverFile)) fs.createFileAwait(receiverFile)
      fs.openAwait(receiverFile, OpenOptions().setAppend(true))
        .write(json.toBuffer().appendBuffer(Buffer.buffer(END)))
      //尝试投递
      vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)
    }

    return json.put(MESSAGE, true)
  }
}
