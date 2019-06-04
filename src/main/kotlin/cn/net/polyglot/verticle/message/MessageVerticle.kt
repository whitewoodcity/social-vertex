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
import io.vertx.kotlin.core.json.jsonObjectOf
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
        HISTORY -> {
          val result = history(json)
          while(result.getJsonArray(HISTORY).size() in 1..9){
            json.put(DATE, result.getString(DATE))
            val tmpResult = history(json)
            result.put(DATE, tmpResult.getString(DATE))
            if(tmpResult.getJsonArray(HISTORY).size()==0){
              break
            }else{
              val history = tmpResult.getJsonArray(HISTORY)
              history.addAll(result.getJsonArray(HISTORY))
              result.put(HISTORY, history)
            }
          }

          result
        }
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

    val yyyy = SimpleDateFormat("yyyy").format(date)
    val mm = SimpleDateFormat("MM").format(date)
    val dd = SimpleDateFormat("dd").format(date)

    if(!json.containsKey(FRIEND)){
      return json.put(MESSAGE, false).put(INFO, "Field: friend is required")
    }

    val dir = config.getString(DIR) + separator
    val friend = json.getString(FRIEND)
    val id = json.getString(ID)
    val resultJson = jsonObjectOf()

    if(vertx.fileSystem().existsAwait("$dir$id$separator$friend$separator$yyyy$separator$mm$separator$dd.jsons")){
      val jsonArray = Buffer.buffer("[")
        .appendBuffer(vertx.fileSystem().readFileAwait("$dir$id$separator$friend$separator$yyyy$separator$mm$separator$dd.jsons"))
        .appendString("]")
        .toJsonArray()
      return resultJson.put(MESSAGE, true).put(HISTORY, jsonArray).put(DATE, "$yyyy-$mm-$dd")
    }

    val yyyys = vertx.fileSystem()
      .readDirAwait("$dir$id$separator$friend","\\d{4}")
      .map{it.substringAfterLast(separator)}
      .filter { it <= yyyy }
      .sorted().reversed()

    for(year in yyyys){
      val mms = vertx.fileSystem()
        .readDirAwait("$dir$id$separator$friend$separator$year","\\d{2}")
        .map{it.substringAfterLast(separator)}
        .filter { year + it <= yyyy + mm }
        .sorted().reversed()

      for(month in mms){
        val dds = vertx.fileSystem()
          .readDirAwait("$dir$id$separator$friend$separator$year$separator$month","\\d{2}.jsons")
          .map{it.substringAfterLast(separator).substringBefore(".")}
          .filter { year + month + it < yyyy + mm + dd }
          .sorted().reversed()

        if(dds.isNotEmpty()){
          val theDate = "$year-$month-${dds[0]}"

          val history =  Buffer.buffer("[")
            .appendBuffer(vertx.fileSystem().readFileAwait("$dir$id$separator$friend$separator$year$separator$month$separator${dds[0]}.jsons"))
            .appendString("]")
            .toJsonArray()

          return resultJson.put(MESSAGE, true).put(HISTORY, history).put(DATE, theDate)
        }
      }
    }

    return resultJson.put(MESSAGE, true).put(HISTORY, JsonArray()).put(DATE, "$yyyy-$mm-$dd")
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
