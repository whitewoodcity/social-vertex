package cn.net.polyglot.verticle.message

import cn.net.polyglot.config.*
import cn.net.polyglot.module.tomorrow
import cn.net.polyglot.module.yesterday
import cn.net.polyglot.verticle.im.IMTcpServerVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.file.*
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
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

  //返回至少20条聊天记录，如果所有的聊天记录不超过20条，则全部返回
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

    val yesterday = SimpleDateFormat("yyyy-MM-dd").parse(json.getString(DATE)).yesterday()

    val yyyy = SimpleDateFormat("yyyy").format(yesterday)
    val mm = SimpleDateFormat("MM").format(yesterday)
    val dd = SimpleDateFormat("dd").format(yesterday)

    if(!json.containsKey(FRIEND)){
      return json.put(MESSAGE, false).put(INFO, "Field: friend is required")
    }

    val dir = config.getString(DIR) + separator
    val friend = json.getString(FRIEND)
    val id = json.getString(ID)

    val history = jsonArrayOf()
    var date = "$yyyy-$mm-$dd"

    val yyyys = vertx.fileSystem()
      .readDir("$dir$id$separator$friend","\\d{4}").await()
      .map{it.substringAfterLast(separator)}
      .filter { it <= yyyy }
      .sorted().reversed()

    for(year in yyyys){
      val mms = vertx.fileSystem()
        .readDir("$dir$id$separator$friend$separator$year","\\d{2}").await()
        .map{it.substringAfterLast(separator)}
        .filter { year + it <= yyyy + mm }
        .sorted().reversed()

      for(month in mms){
        val dds = vertx.fileSystem()
          .readDir("$dir$id$separator$friend$separator$year$separator$month","\\d{2}.jsons").await()
          .map{it.substringAfterLast(separator).substringBefore(".")}
          .filter { year + month + it <= yyyy + mm + dd }
          .sorted().reversed()

        for(day in dds){
          val mergedHistory = Buffer.buffer("[")
            .appendBuffer(vertx.fileSystem().readFile("$dir$id$separator$friend$separator$year$separator$month$separator$day.jsons").await())
            .appendString("]")
            .toJsonArray().addAll(history)

          history.clear().addAll(mergedHistory)

          date = "$year-$month-$day"

          if(history.size()>=20){
            return json.put(MESSAGE, true).put(HISTORY, history).put(DATE, date)
          }
        }
      }
    }

    return json.put(MESSAGE, true).put(HISTORY, history).put(DATE, date)
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
      if (!fs.exists(senderDir).await()) {
        return json.put(MESSAGE, false)//错误，该用户没有该好友
      }
      val senderFile = "$senderDir$separator$yyyy$separator$mm$separator$dd.jsons"
      if (!fs.exists(senderFile).await()) {
        if (!fs.exists("$senderDir$separator$yyyy$separator$mm").await())
          fs.mkdirs("$senderDir$separator$yyyy$separator$mm").await()
        fs.writeFile(senderFile, json.toBuffer()).await()
      } else {
        fs.open(senderFile, OpenOptions().setAppend(true)).await()
          .write(Buffer.buffer(",").appendBuffer(json.toBuffer()))
      }
    }

    if (to.contains("@")) {
      json.put(ID, from + "@" + config.getString(HOST))//把from加上域名
      webClient.put(config.getInteger(HTTP_PORT), to.substringAfterLast("@"), "/$MESSAGE/$${json.getString(SUBTYPE)}")
        .sendJsonObject(json.put(TO, to.substringBeforeLast("@"))) {}
    } else {
      val receiverDir = "$dir$to$separator$from"
      if (!fs.exists(receiverDir).await()) {
        return json.put(MESSAGE, false)//错误，该用户没有该好友
      }
      val receiverFile = "$receiverDir$separator$yyyy$separator$mm$separator$dd.jsons"
      if (!fs.exists(receiverFile).await()) {
        if (!fs.exists("$receiverDir$separator$yyyy$separator$mm").await())
          fs.mkdirs("$receiverDir$separator$yyyy$separator$mm").await()
        fs.writeFile(receiverFile, json.toBuffer()).await()
      } else {
        fs.open(receiverFile, OpenOptions().setAppend(true)).await()
          .write(Buffer.buffer(",").appendBuffer(json.toBuffer()))
      }

      //尝试投递
      vertx.eventBus().send(IMTcpServerVerticle::class.java.name, json)
    }

    return json.put(MESSAGE, true)
  }
}
