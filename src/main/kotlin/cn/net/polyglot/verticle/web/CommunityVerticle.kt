package cn.net.polyglot.verticle.web

import cn.net.polyglot.config.*
import com.codahale.fastuuid.UUIDGenerator
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.*
import java.io.File
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CommunityVerticle : ServletVerticle() {
  private val generator = UUIDGenerator(SecureRandom())

  override suspend fun start() {
    super.start(this::class.java.name)
  }

  override suspend fun doGet(json: JsonObject, session: Session): JsonObject {
    return doGetAndPost(json, session)
  }

  override suspend fun doPost(json: JsonObject, session: Session): JsonObject {
    return doGetAndPost(json, session)
  }

  suspend fun doGetAndPost(json: JsonObject, session: Session): JsonObject {
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

      when(json.getString(PATH)){
        "/submitArticle" -> {
          val fullPath = datePath + File.separator + generator.generate().toString() + ".json"
          vertx.fileSystem().createFileAwait(fullPath)
          vertx.fileSystem().writeFileAwait(fullPath,
            json.getJsonObject(FORM_ATTRIBUTES)
              .put(ID, session.get(ID))
              .put(NICKNAME, session.get(NICKNAME)).toBuffer())

          JsonObject()
            .put(VALUES, JsonObject().put(ARTICLES, getRecentArticles()))
            .put(TEMPLATE_PATH, "community/index.html")
        }
        "/community" -> {
          JsonObject()
            .put(VALUES, JsonObject().put(ARTICLES, getRecentArticles()))
            .put(TEMPLATE_PATH, "community/index.html")
        }
        "/article" -> {
          val path = dir + File.separator + COMMUNITY + File.separator + json.getJsonObject(PARAMS).getString("path") + ".json"
          val articleJson = vertx.fileSystem().readFileAwait(path).toJsonObject()
          articleJson.put("displayModificationPanel", session.get(ID) == articleJson.getString(ID))
          articleJson.put("path", json.getJsonObject(PARAMS).getString("path"))
          JsonObject()
            .put(VALUES, articleJson)
            .put(TEMPLATE_PATH, "community/article.html")
        }
        "/prepareModifyArticle" -> {
          val path = dir + File.separator + COMMUNITY + File.separator + json.getJsonObject(PARAMS).getString("path") + ".json"
          val articleJson = vertx.fileSystem().readFileAwait(path).toJsonObject()
          articleJson.mergeIn(json.getJsonObject(PARAMS))
          JsonObject()
            .put(VALUES, articleJson)
            .put(TEMPLATE_PATH, "community/modifyPost.html")
        }
        else -> {//"/prepareArticle"
          JsonObject().put(TEMPLATE_PATH, "community/post.html")
        }
      }

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
      while (date.year > 2018 && articles.size() < 20) {

        val uri = "${date.year}${File.separator}${date.monthValue}${File.separator}${date.dayOfMonth}${File.separator}"

        if (vertx.fileSystem().existsAwait(dir + File.separator + COMMUNITY + File.separator + date.year + File.separator + date.monthValue + File.separator + date.dayOfMonth)) {
          val list = vertx.fileSystem().readDirAwait(dir + File.separator + COMMUNITY + File.separator + date.year + File.separator + date.monthValue + File.separator + date.dayOfMonth)
          for (path in list) {
            if (path.endsWith("json")) {
              try {
                val file = vertx.fileSystem().readFileAwait(path).toJsonObject()

                val json =
                  JsonObject()
                    .put(PARAMETERS, "$uri${path.substringAfterLast("/").substringBefore(".")}")
                    .put(TITLE, file.getString(TITLE))
                    .put(DATE, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))

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

    return articles
  }
}
