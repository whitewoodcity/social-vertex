package cn.net.polyglot.verticle.publication

import cn.net.polyglot.config.*
import cn.net.polyglot.module.lastHour
import cn.net.polyglot.module.nextHour
import cn.net.polyglot.module.tomorrow
import cn.net.polyglot.module.yesterday
import com.codahale.fastuuid.UUIDGenerator
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.*
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File.separator
import java.security.SecureRandom
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PublicationVerticle : CoroutineVerticle() {
  private val generator = UUIDGenerator(SecureRandom())

  override suspend fun start() {
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      launch { it.reply(article(it.body())) }
    }
  }

  private suspend fun article(json: JsonObject): JsonObject {
    return try {
      when (json.getString(SUBTYPE)) {
        QUESTION,ARTICLE,IDEA, ANSWER -> post(json)
        HISTORY -> history(json)
        else -> json.put(PUBLICATION, false)
      }
    } catch (e:Exception){
      e.printStackTrace()
      json.put(PUBLICATION, false).put(INFO, e.message)
    }
  }

  private suspend fun post(json: JsonObject): JsonObject {
    val fs = vertx.fileSystem()

    val date = Date()
    val today = SimpleDateFormat("yyyy-MM-dd").format(date)
    val time = SimpleDateFormat("hh:mm:ss").format(date)

    json.put(DATE, today)
    json.put(TIME, time)

    val yyyy = SimpleDateFormat("yyyy").format(date)
    val mm = SimpleDateFormat("MM").format(date)
    val dd = SimpleDateFormat("dd").format(date)
    val hh = SimpleDateFormat("HH").format(date)

    val dirName = generator.generate().toString()

    val communityPath = "${config.getString(DIR)}$separator$COMMUNITY$separator$yyyy$separator$mm$separator$dd$separator$hh$separator$dirName"

    fs.mkdirsAwait(communityPath)
    fs.writeFileAwait("$communityPath${separator}publication.json",json.toBuffer())

    val linkPath = "${config.getString(DIR)}$separator${json.getString(ID)}$separator$COMMUNITY$separator$yyyy$separator$mm$separator$dd$separator$hh"
    fs.mkdirsAwait(linkPath)
    fs.createFileAwait("$linkPath$separator$dirName")

    return json.put(PUBLICATION, true).put(PATH, "$yyyy$separator$mm$separator$dd$separator$hh$separator$dirName")
  }

  private suspend fun history(json: JsonObject): JsonObject {

    if (json.getString(TIME) == null) {
      val nextHour = SimpleDateFormat("yyyy-MM-dd-HH").format(Date().nextHour())
      json.put(TIME, nextHour)
    } else try {
      SimpleDateFormat("yyyy-MM-dd-HH").parse(json.getString(TIME))
    } catch (e: ParseException) {
      json.remove(TIME)
      val nextHour = SimpleDateFormat("yyyy-MM-dd-HH").format(Date().nextHour())
      json.put(TIME, nextHour)
    }

    val time = SimpleDateFormat("yyyy-MM-dd-HH").parse(json.getString(TIME)).lastHour()

    val yyyy = SimpleDateFormat("yyyy").format(time)
    val mm = SimpleDateFormat("MM").format(time)
    val dd = SimpleDateFormat("dd").format(time)
    val hh = SimpleDateFormat("HH").format(time)

    val dir = if(json.containsKey(FROM)){
      if(!vertx.fileSystem().existsAwait("${config.getString(DIR)}$separator${json.getString(FROM)}")){
        return json.put(PUBLICATION, false).put(INFO, "User doesn't exist")
      }
      "${config.getString(DIR)}$separator${json.getString(FROM)}$separator$COMMUNITY"
    }else{
      "${config.getString(DIR)}$separator$COMMUNITY"
    }
    if(!vertx.fileSystem().existsAwait(dir)){
      vertx.fileSystem().mkdirsAwait(dir)
    }

    val history = jsonArrayOf()
    var until = "$yyyy-$mm-$dd-$hh"

    val yyyys = vertx.fileSystem()
      .readDirAwait(dir,"\\d{4}")
      .map{it.substringAfterLast(separator)}
      .filter { it <= yyyy }
      .sorted().reversed()

    loop@for(year in yyyys){
      val mms = vertx.fileSystem()
        .readDirAwait("$dir$separator$year","\\d{2}")
        .map{it.substringAfterLast(separator)}
        .filter { year + it <= yyyy + mm }
        .sorted().reversed()

      for(month in mms){
        val dds = vertx.fileSystem()
          .readDirAwait("$dir$separator$year$separator$month","\\d{2}")
          .map{it.substringAfterLast(separator)}
          .filter { year + month + it <= yyyy + mm + dd }
          .sorted().reversed()

        for(day in dds){
          val hhs = vertx.fileSystem()
            .readDirAwait("$dir$separator$year$separator$month$separator$day","\\d{2}")
            .map{it.substringAfterLast(separator)}
            .filter { year + month + day + it <= yyyy + mm + dd + hh }
            .sorted().reversed()

          for(hour in hhs){
            val publicationList = vertx.fileSystem()
              .readDirAwait("$dir$separator$year$separator$month$separator$day$separator$hour")

            for(publicationPath in publicationList){
              val props = vertx.fileSystem().propsAwait(publicationPath)
              val publicationFilePath = if(props.isDirectory){
                "$publicationPath$separator" + "publication.json"
              }else{
                "${config.getString(DIR)}$separator$COMMUNITY$separator$year$separator$month$separator$day$separator$hour$separator${publicationPath.substringAfterLast(separator)}$separator" + "publication.json"
              }
              val file = vertx.fileSystem().readFileAwait(publicationFilePath)
                .toJsonObject().put(DIR, publicationFilePath.substringAfterLast(COMMUNITY).substringBeforeLast(separator))
              until = "$year-$month-$day-$hour"
              history.add(file)
            }

            if(history.size()>=20) break@loop
          }
        }
      }
    }

    return json.put(PUBLICATION, true).put(HISTORY, history).put(TIME, until)
  }
}
