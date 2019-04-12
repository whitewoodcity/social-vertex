package cn.net.polyglot.verticle.community

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.web.ServletVerticle
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

  override suspend fun doGet(json: JsonObject, session: Session): Response {
    return doGetAndPost(json, session)
  }

  override suspend fun doPost(json: JsonObject, session: Session): Response {
    return doGetAndPost(json, session)
  }

  suspend fun doGetAndPost(json: JsonObject, session: Session): Response {
    return try {
      if (session.get(ID) == null) {
        return Response(ResponseType.TEMPLATE_PATH, "index.htm")
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

          Response(ResponseType.TEMPLATE_PATH, "community/index.html", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/modifyArticle" -> {
          val fullPath = dir + File.separator + COMMUNITY + File.separator + json.getJsonObject(FORM_ATTRIBUTES).getString("path") + ".json"
          vertx.fileSystem().deleteAwait(fullPath)
          vertx.fileSystem().createFileAwait(fullPath)
          vertx.fileSystem().writeFileAwait(fullPath,
            json.getJsonObject(FORM_ATTRIBUTES)
              .put(ID, session.get(ID))
              .put(NICKNAME, session.get(NICKNAME)).toBuffer())

          Response(ResponseType.TEMPLATE_PATH, "community/index.html", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/deleteArticle" -> {
          val path = dir + File.separator + COMMUNITY + File.separator + json.getJsonObject(PARAMS).getString("path") + ".json"

          if(vertx.fileSystem().existsAwait(path)){
            if(session.get(ID)!=vertx.fileSystem().readFileAwait(path).toJsonObject().getString(ID))
              return Response(ResponseType.TEMPLATE_PATH, "index.htm")

            vertx.fileSystem().deleteAwait(path)
          }

          Response(ResponseType.TEMPLATE_PATH, "community/index.html", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/community" -> {
          Response(ResponseType.TEMPLATE_PATH, "community/index.html", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/article" -> {
          val path = dir + File.separator + COMMUNITY + File.separator + json.getJsonObject(PARAMS).getString("path") + ".json"
          val articleJson = vertx.fileSystem().readFileAwait(path).toJsonObject()
          articleJson.put("displayModificationPanel", session.get(ID) == articleJson.getString(ID))
          articleJson.put("path", json.getJsonObject(PARAMS).getString("path"))
          Response(ResponseType.TEMPLATE_PATH, "community/index.html", articleJson)
        }
        "/prepareModifyArticle" -> {
          val path = dir + File.separator + COMMUNITY + File.separator + json.getJsonObject(PARAMS).getString("path") + ".json"
          val articleJson = vertx.fileSystem().readFileAwait(path).toJsonObject()
          articleJson.mergeIn(json.getJsonObject(PARAMS))
          Response(ResponseType.TEMPLATE_PATH,  "community/modifyPost.html", articleJson)
        }
        "/prepareSearchArticle" -> {
          Response(ResponseType.TEMPLATE_PATH,  "community/search.html")
        }
        "/searchArticle" -> {
          //考虑放到worker verticle中去执行，如果文件非常多，这里可能会有比较长的执行时间
          val keyword = json.getJsonObject(FORM_ATTRIBUTES).getString("keyword")

          val date0 = LocalDateTime.parse(json.getJsonObject(FORM_ATTRIBUTES).getString("date0")+"T00:00")
          val date1 = LocalDateTime.parse(json.getJsonObject(FORM_ATTRIBUTES).getString("date1")+"T00:00")

          val d0 = if(date0.isBefore(date1)) date0 else date1
          var d1 = if(date0.isBefore(date1)) date1 else date0

          val articles = JsonArray()

          while(!d1.isBefore(d0)){

            val uri = "${d1.year}${File.separator}${d1.monthValue}${File.separator}${d1.dayOfMonth}${File.separator}"
            val d1Path = dir + File.separator + COMMUNITY + File.separator + d1.year + File.separator + d1.monthValue + File.separator + d1.dayOfMonth
            if(vertx.fileSystem().existsAwait(d1Path)){
              val list = vertx.fileSystem().readDirAwait(d1Path)

              for (path in list) {
                if (path.endsWith("json")) {
                  try {
                    val file = vertx.fileSystem().readFileAwait(path).toJsonObject()

                    if(file.getString(TITLE).contains(keyword)){
                      articles.add(JsonObject()
                        .put(PARAMETERS, "$uri${path.substringAfterLast("/").substringBefore(".")}")
                        .put(TITLE, file.getString(TITLE))
                        .put(DATE, d1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                    }
                  }catch (throwable:Throwable){
                    throwable.printStackTrace()
                  }
                }
              }
            }

            d1 = d1.minusDays(1)
          }

          Response(ResponseType.TEMPLATE_PATH,  "community/index.html", JsonObject().put(ARTICLES, articles))
        }
        "/uploadPortrait" -> {

          val jarDir = config.getString(JAR_DIR)

          if(vertx.fileSystem().existsAwait(dir+File.separator+session.get(ID)+File.separator+"portrait")){
            vertx.fileSystem().deleteAwait(dir+File.separator+session.get(ID)+File.separator+"portrait")
          }

          vertx.fileSystem().moveAwait(jarDir+File.separator+json.getJsonObject(UPLOAD_FILES).getString("profile"), dir+File.separator+session.get(ID)+File.separator+"portrait")

          Response(ResponseType.TEMPLATE_PATH,  "community/index.html", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/portrait" -> Response(ResponseType.FILE_PATH, dir+File.separator+session.get(ID)+File.separator+"portrait")
        else -> {//"/prepareArticle"
          Response(ResponseType.TEMPLATE_PATH, "community/post.html")
        }
      }

    } catch (throwable: Throwable) {
      throwable.printStackTrace()
      Response(ResponseType.TEMPLATE_PATH, "error.htm")
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
