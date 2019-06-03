package cn.net.polyglot.verticle.message

import cn.net.polyglot.config.*
import cn.net.polyglot.module.tomorrow
import cn.net.polyglot.module.yesterday
import cn.net.polyglot.verticle.im.IMTcpServerVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.file.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File.separator
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MessageVerticle : CoroutineVerticle() {
  private lateinit var webClient: WebClient

  override suspend fun start() {
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      launch { it.reply(message(it.body())) }
    }
  }

  private suspend fun message(json: JsonObject): JsonObject {
    return try {
      when (json.getString(SUBTYPE)) {
        TEXT -> text(json)
        HISTORY -> history(json)
        else -> json.put(MESSAGE, false)
      }
    } catch (e:Exception){
      e.printStackTrace()
      json.put(MESSAGE, false).put(INFO, e.message)
    }
  }

  private suspend fun history(json: JsonObject): JsonObject {

    if (json.getString(DATE) == null) {
      val tomorrow = SimpleDateFormat("yyyy-MM-dd").format(Date().tomorrow())
      json.put(DATE, tomorrow)
    } else try {
      SimpleDateFormat("yyyy-MM-dd").parse(json.getString(DATE))
    } catch (e: ParseException) {
      json.remove(DATE)
      val tomorrow = SimpleDateFormat("yyyy-MM-dd").format(Date().tomorrow())
      json.put(DATE, tomorrow)
    }

    val date = SimpleDateFormat("yyyy-MM-dd").parse(json.getString(DATE)).yesterday()

    var yyyy = SimpleDateFormat("yyyy").format(date)
    var mm = SimpleDateFormat("MM").format(date)
    var dd = SimpleDateFormat("dd").format(date)

    if(!json.containsKey(FRIEND)){
      return json.put(MESSAGE, false).put(INFO, "Field: friend is required")
    }

    val dir = config.getString(DIR) + separator
    val friend = json.getString(FRIEND)
    val id = json.getString(ID)

    if(vertx.fileSystem().existsAwait("$dir$id$separator$friend$separator$yyyy$separator$mm$separator$dd.jsons")){
      val jsonArray = Buffer.buffer("[")
        .appendBuffer(vertx.fileSystem().readFileAwait("$dir$id$separator$friend$separator$yyyy$separator$mm$separator$dd.jsons"))
        .appendString("]")
        .toJsonArray()
      return json.put(MESSAGE, true).put(HISTORY, jsonArray).put(DATE, "$yyyy-$mm-$dd")
    }

    val yyyys =
      vertx.fileSystem().readDirAwait("$dir$id$separator$friend","\\d{4}")
        .map{it.substringAfterLast(separator)}
    yyyys.sorted()
    yyyys.reversed()
    if(!yyyys.isEmpty()&& yyyys.last()<=yyyy){
      for(i in 0 until yyyys.size){
        if(yyyys[i]<=yyyy){
          if(yyyys[i]!=yyyy){
            mm = "12"
            dd = "31"
            yyyy = yyyys[i]
          }
          break
        }
      }
    }else{
      return json.put(MESSAGE, true).put(HISTORY, JsonArray()).put(DATE, "$yyyy-$mm-$dd")
    }

    val mms =
      vertx.fileSystem().readDirAwait("$dir$id$separator$friend$separator$yyyy","\\d{2}")
        .map{it.substringAfterLast(separator)}
    mms.sorted()
    mms.reversed()
    if(!mms.isEmpty()&& mms.last()<=mm){
      for(i in 0 until mms.size){
        if(mms[i]<=mm){
          if(mms[i]!=mm){
            dd = "31"
            mm = mms[i]
          }
          break
        }
      }
    }else{
      return json.put(MESSAGE, true).put(HISTORY, JsonArray()).put(DATE, "$yyyy-$mm-$dd")
    }

    val dds =
      vertx.fileSystem().readDirAwait("$dir$id$separator$friend$separator$yyyy$separator$mm","\\d{2}.jsons")
        .map{it.substringAfterLast(separator).substringBefore(".")}
    dds.sorted()
    dds.reversed()
    if(!dds.isEmpty()&& dds.last()<=dd){
      for(i in 0 until dds.size){
        if(dds[i]<=dd){
          dd = dds[i]
          break
        }
      }
    }else{
      return json.put(MESSAGE, true).put(HISTORY, JsonArray()).put(DATE, "$yyyy-$mm-$dd")
    }

    val history = if(vertx.fileSystem().existsAwait("$dir$id$separator$friend$separator$yyyy$separator$mm$separator$dd.jsons")){
      Buffer.buffer("[")
        .appendBuffer(vertx.fileSystem().readFileAwait("$dir$id$separator$friend$separator$yyyy$separator$mm$separator$dd.jsons"))
        .appendString("]")
        .toJsonArray()
    }else{
      JsonArray()
    }

    return json.put(MESSAGE, true).put(HISTORY, history).put(DATE, "$yyyy-$mm-$dd")
  }

  private suspend fun text(json: JsonObject): JsonObject {
    val from = json.getString(ID)
    val to = json.getString(TO)
    if (from == null || to == null) {
      return json.put(MESSAGE, false)//wrong format
    }
    val fs = vertx.fileSystem()

    val date = Date()
    val today = SimpleDateFormat("yyyy-MM-dd").format(date)
    val instant = SimpleDateFormat("hh:mm:ss").format(date)

    json.put(DATE, today)
    json.put(TIME, instant)

    val yyyy = SimpleDateFormat("yyyy").format(date)
    val mm = SimpleDateFormat("MM").format(date)
    val dd = SimpleDateFormat("dd").format(date)

    val dir = config.getString(DIR) + separator

    if (!from.contains("@")) {
      val senderDir = "$dir$from$separator$to"
      if (!fs.existsAwait(senderDir)) {
        return json.put(MESSAGE, false)//错误，该用户没有该好友
      }
      val senderFile = "$senderDir$separator$yyyy$separator$mm$separator$dd.jsons"
      if (!fs.existsAwait(senderFile)) {
        if (!fs.existsAwait("$senderDir$separator$yyyy$separator$mm"))
          fs.mkdirsAwait("$senderDir$separator$yyyy$separator$mm")
//        fs.createFileAwait(senderFile)
        fs.writeFileAwait(senderFile, json.toBuffer())
      } else {
        fs.openAwait(senderFile, OpenOptions().setAppend(true))
          .write(Buffer.buffer(","))
          .write(json.toBuffer())
      }
    }

    if (to.contains("@")) {
      json.put(ID, from + "@" + config.getString(HOST))//把from加上域名
      webClient.put(config.getInteger(HTTP_PORT), to.substringAfterLast("@"), "/$MESSAGE/$${json.getString(SUBTYPE)}")
        .sendJsonObject(json.put(TO, to.substringBeforeLast("@"))) {}
    } else {
      val receiverDir = "$dir$to$separator$from"
      if (!fs.existsAwait(receiverDir)) {
        return json.put(MESSAGE, false)//错误，该用户没有该好友
      }
      val receiverFile = "$receiverDir$separator$yyyy$separator$mm$separator$dd.jsons"
      if (!fs.existsAwait(receiverFile)) {
        if (!fs.existsAwait("$receiverDir$separator$yyyy$separator$mm"))
          fs.mkdirsAwait("$receiverDir$separator$yyyy$separator$mm")
//        fs.createFileAwait(receiverFile)
        fs.writeFileAwait(receiverFile, json.toBuffer())
      } else {
        fs.openAwait(receiverFile, OpenOptions().setAppend(true))
          .write(Buffer.buffer(","))
          .write(json.toBuffer())
      }

      //尝试投递
      vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)
    }

    return json.put(MESSAGE, true)
  }
}
