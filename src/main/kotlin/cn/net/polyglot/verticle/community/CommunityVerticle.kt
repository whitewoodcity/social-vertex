package cn.net.polyglot.verticle.community

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.web.ServletVerticle
import com.codahale.fastuuid.UUIDGenerator
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import java.io.File.separator
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CommunityVerticle : ServletVerticle() {
  private val generator = UUIDGenerator(SecureRandom())

  override suspend fun doGet(request: HttpServletRequest): HttpServletResponse {
    return doGetAndPost(request)
  }

  override suspend fun doPost(request: HttpServletRequest): HttpServletResponse {
    return doGetAndPost(request)
  }

  private suspend fun doGetAndPost(request:HttpServletRequest): HttpServletResponse {
    return try {
      if (request.session.get(ID) == null) {
        return HttpServletResponse(HttpServletResponseType.TEMPLATE, "index.html")
      }

      val dir = config.getString(DIR)

      val now = LocalDateTime.now()

      val datePath = dir + separator + COMMUNITY + separator + now.year + separator + now.monthValue + separator + now.dayOfMonth

      val fileSystem = vertx.fileSystem()

      if (!fileSystem.exists(datePath).await()) {
        fileSystem.mkdirs(datePath).await()
      }

      when (request.getPath()) {
        "/submitArticle" -> {
          val fullPath = datePath + separator + generator.generate().toString() + ".json"
          fileSystem.createFile(fullPath).await()
          fileSystem.writeFile(fullPath,
            request.getFormAttributes()
              .put(ID, request.session.get(ID))
              .put(NICKNAME, request.session.get(NICKNAME)).toBuffer()).await()

          HttpServletResponse("community/index.htm", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/modifyArticle" -> {
          val fullPath = dir + separator + COMMUNITY + separator + request.getFormAttributes().getString("path") + ".json"
          fileSystem.delete(fullPath).await()
          fileSystem.createFile(fullPath).await()
          fileSystem.writeFile(fullPath,
            request.getFormAttributes()
              .put(ID, request.session.get(ID))
              .put(NICKNAME, request.session.get(NICKNAME)).toBuffer()).await()

          HttpServletResponse("community/index.htm", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/deleteArticle" -> {
          val path = dir + separator + COMMUNITY + separator + request.getParams().getString("path") + ".json"

          if (fileSystem.exists(path).await()) {
            if (request.session.get(ID) != fileSystem.readFile(path).await().toJsonObject().getString(ID))
              return HttpServletResponse(HttpServletResponseType.TEMPLATE, "index.html")

            fileSystem.delete(path).await()
          }

          HttpServletResponse("community/index.htm", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/community" -> {
          HttpServletResponse("community/index.htm", JsonObject().put(ARTICLES, getRecentArticles()))
        }
        "/article" -> {
          val path = dir + separator + COMMUNITY + separator + request.getParams().getString("path") + ".json"
          val articleJson = fileSystem.readFile(path).await().toJsonObject()
          articleJson.put("displayModificationPanel", request.session.get(ID) == articleJson.getString(ID))
          articleJson.put("path", request.getParams().getString("path"))
          HttpServletResponse("community/article.htm", articleJson)
        }
        "/prepareModifyArticle" -> {
          val path = dir + separator + COMMUNITY + separator + request.getParams().getString("path") + ".json"
          val articleJson = fileSystem.readFile(path).await().toJsonObject()
          articleJson.mergeIn(request.getParams())
          HttpServletResponse("community/modifyPost.htm", articleJson)
        }
        "/prepareSearchArticle" -> {
          HttpServletResponse(HttpServletResponseType.TEMPLATE, "community/search.htm")
        }
        "/searchArticle" -> {
          //考虑放到worker verticle中去执行，如果文件非常多，这里可能会有比较长的执行时间
          val keyword = request.getFormAttributes().getString("keyword")

          val date0 = LocalDateTime.parse(request.getFormAttributes().getString("date0") + "T00:00")
          val date1 = LocalDateTime.parse(request.getFormAttributes().getString("date1") + "T00:00")

          val d0 = if (date0.isBefore(date1)) date0 else date1
          var d1 = if (date0.isBefore(date1)) date1 else date0

          val articles = JsonArray()

          while (!d1.isBefore(d0)) {

            val uri = "${d1.year}${separator}${d1.monthValue}${separator}${d1.dayOfMonth}${separator}"
            val d1Path = dir + separator + COMMUNITY + separator + d1.year + separator + d1.monthValue + separator + d1.dayOfMonth
            if (fileSystem.exists(d1Path).await()) {
              val list = fileSystem.readDir(d1Path).await()

              for (path in list) {
                if (path.endsWith("json")) {
                  try {
                    val file = fileSystem.readFile(path).await().toJsonObject()

                    if (file.getString(TITLE).contains(keyword)) {
                      articles.add(JsonObject()
                        .put(PARAMETERS, "$uri${path.substringAfterLast("/").substringBefore(".")}")
                        .put(TITLE, file.getString(TITLE))
                        .put(DATE, d1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                    }
                  } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                  }
                }
              }
            }

            d1 = d1.minusDays(1)
          }

          HttpServletResponse("community/index.htm", JsonObject().put(ARTICLES, articles))
        }
        "/uploadPortrait" -> {

          val jarDir = config.getString(JAR_DIR)

          if (fileSystem.exists(dir + separator + request.session.get(ID) + separator + "portrait").await()) {
            fileSystem.delete(dir + separator + request.session.get(ID) + separator + "portrait").await()
          }

          fileSystem.move(jarDir + separator + request.getUploadFiles().getString("profile"), dir + separator + request.session.get(ID) + separator + "portrait").await()

          HttpServletResponse("community/index.htm", JsonObject().put(ARTICLES, getRecentArticles()))
        }
//        "/portrait" -> HttpServletResponse(dir + separator + session.get(ID) + separator + "portrait")
        else -> {//"/prepareArticle"
          HttpServletResponse(HttpServletResponseType.TEMPLATE, "community/post.htm")
        }
      }

    } catch (throwable: Throwable) {
      throwable.printStackTrace()
      HttpServletResponse(HttpServletResponseType.TEMPLATE, "error.html")
    }
  }

  private suspend fun getRecentArticles(): JsonArray {

    val articles = JsonArray()

    val dir = config.getString(DIR)

    var date = LocalDateTime.now()

    if (vertx.fileSystem().readDir(dir + separator + COMMUNITY).await().isNotEmpty()) {
      while (date.year > 2018 && articles.size() < 20) {

        val uri = "${date.year}${separator}${date.monthValue}${separator}${date.dayOfMonth}${separator}"

        if (vertx.fileSystem().exists(dir + separator + COMMUNITY + separator + date.year + separator + date.monthValue + separator + date.dayOfMonth).await()) {
          val list = vertx.fileSystem().readDir(dir + separator + COMMUNITY + separator + date.year + separator + date.monthValue + separator + date.dayOfMonth).await()
          for (path in list) {
            if (path.endsWith("json")) {
              try {
                val file = vertx.fileSystem().readFile(path).await().toJsonObject()

                val json =
                  JsonObject()
                    .put(PARAMETERS, "$uri${path.substringAfterLast("/").substringBefore(".")}")
                    .put(TITLE, file.getString(TITLE))
                    .put(DATE, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))

                articles.add(json)
              } catch (throwable: Throwable) {
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
