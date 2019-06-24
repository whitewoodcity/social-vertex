package cn.net.polyglot.verticle.publication

import cn.net.polyglot.config.*
import com.codahale.fastuuid.UUIDGenerator
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.createFileAwait
import io.vertx.kotlin.core.file.mkdirsAwait
import io.vertx.kotlin.core.file.writeFileAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File.separator
import java.security.SecureRandom
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

  private suspend fun history(json: JsonObject): JsonObject {
    return json.put(PUBLICATION, true)
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
    val hh = SimpleDateFormat("hh").format(date)

    val dirName = generator.generate().toString()

    val communityPath = "${config.getString(DIR)}$separator$COMMUNITY$separator$yyyy$separator$mm$separator$dd$separator$hh$separator$dirName"

    fs.mkdirsAwait(communityPath)
    fs.writeFileAwait("$communityPath${separator}publication.json",json.toBuffer())

    val linkPath = "${config.getString(DIR)}$separator${json.getString(ID)}$separator$COMMUNITY$separator$yyyy$separator$mm$separator$dd$separator$hh"
    fs.mkdirsAwait(linkPath)
    fs.createFileAwait("$linkPath$separator$dirName")

    return json.put(PUBLICATION, true).put(PATH, "$yyyy$separator$mm$separator$dd$separator$hh$separator$dirName")
  }
}
