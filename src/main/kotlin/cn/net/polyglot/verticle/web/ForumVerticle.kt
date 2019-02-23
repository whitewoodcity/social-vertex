package cn.net.polyglot.verticle.web

import cn.net.polyglot.config.*
import io.vertx.core.json.JsonObject
import java.io.File
import com.codahale.fastuuid.UUIDGenerator
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonArray
import io.vertx.kotlin.core.file.*
import java.security.SecureRandom
import java.time.LocalDateTime

class ForumVerticle : ServletVerticle() {
  private val generator = UUIDGenerator(SecureRandom())

  override suspend fun start() {
    super.start(this::class.java.name)
  }

  override suspend fun doPost(json: JsonObject, session: Session): JsonObject {

    return try {
      if (session.get(ID) == null) {
        return JsonObject()
          .put(TEMPLATE_PATH, "index.htm")
      }

      val dir = config.getString(DIR)

      val now = LocalDateTime.now()

      val datePath = dir + File.separator + COMMUNITY + File.separator + now.year + File.separator + now.monthValue + File.separator + now.dayOfMonth

      if (!vertx.fileSystem().existsAwait(datePath)) {
        vertx.fileSystem().mkdirsAwait(datePath)
      }

      val fullPath = datePath + File.separator + generator.generate().toString() + ".json"

      vertx.fileSystem().createFileAwait(fullPath)
      vertx.fileSystem().openAwait(fullPath, OpenOptions().setAppend(true))
        .write(json.getJsonObject(FORM_ATTRIBUTES)
          .put(ID, session.get(ID))
          .put(NICKNAME, session.get(NICKNAME)).toBuffer())

      JsonObject()
        .put("articles", getRecentArticles())
        .put(TEMPLATE_PATH, "dontuknow/index.html")
    } catch (throwable: Throwable) {
      throwable.printStackTrace()
      JsonObject().put(TEMPLATE_PATH, "error.htm")
    }
  }

  suspend fun getRecentArticles(): JsonArray {

    val articles = JsonArray()

    val dir = config.getString(DIR)

    var date = LocalDateTime.now()

    if (!vertx.fileSystem().readDirAwait(dir + File.separator + COMMUNITY).isEmpty()) {
      while (date.year > 2018 || articles.size() >= 20) {

        val uri = "year=${date.year}&month=${date.monthValue}&day=${date.dayOfMonth}"

        if (vertx.fileSystem().existsAwait(dir + File.separator + COMMUNITY + File.separator + date.year + File.separator + date.monthValue + File.separator + date.dayOfMonth)) {
          val list = vertx.fileSystem().readDirAwait(dir + File.separator + COMMUNITY + File.separator + date.year + File.separator + date.monthValue + File.separator + date.dayOfMonth)
          for (path in list) {
            if (path.endsWith("json")) {
              try {
                val file = vertx.fileSystem().readFileAwait(path).toJsonObject()

                val json =
                  JsonObject()
                    .put("uri", "$uri&name=${path.substringAfterLast("/").substringBefore(".")}")
                    .put("title", file.getString("title"))

                articles.add(json)
              }catch (throwable:Throwable){
                throwable.printStackTrace()
              }
            }
          }
        }

        date = date.minusDays(1)
      }
    }
    println(articles)

    return articles
  }
}
