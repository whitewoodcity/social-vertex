package cn.net.polyglot.verticle.community

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.web.ServletVerticle
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import java.io.File.separator

class DefaultVerticle : ServletVerticle() {
  override suspend fun doGet(request: HttpServletRequest): HttpServletResponse {
    return HttpServletResponse("default.htm", JsonObject().put(ARTICLES, getRecentArticles()))
  }

  private suspend fun getRecentArticles(): JsonArray {

    val articles = JsonArray()

    val dir = config.getString(DIR)

    if (vertx.fileSystem().exists("$dir$separator$COMMUNITY").await() && vertx.fileSystem().readDir("$dir$separator$COMMUNITY").await().isNotEmpty()) {
      val years = vertx.fileSystem().readDir("$dir$separator$COMMUNITY").await().sortedDescending()
      loop@ for (year in years) {
        if (vertx.fileSystem().readDir(year).await().isNotEmpty()) {
          val months = vertx.fileSystem().readDir(year).await().sortedDescending()
          for (month in months) {
            if (vertx.fileSystem().props(month).await().isDirectory && vertx.fileSystem().readDir(month).await().isNotEmpty()) {
              val days = vertx.fileSystem().readDir(month).await().sortedDescending()
              for (day in days) {
                if (vertx.fileSystem().props(day).await().isDirectory && vertx.fileSystem().readDir(day).await().isNotEmpty()) {
                  val hours = vertx.fileSystem().readDir(day).await().sortedDescending()
                  for (hour in hours) {
                    if (vertx.fileSystem().props(hour).await().isDirectory && vertx.fileSystem().readDir(hour).await().isNotEmpty()) {
                      val directories = vertx.fileSystem().readDir(hour).await()
                      for (directory in directories) {
                        if (vertx.fileSystem().exists("$directory${separator}publication.json").await()) {
                          val json = vertx.fileSystem().readFile("$directory${separator}publication.json").await().toJsonObject()
                          articles.add(json)
                          if (articles.size() >= 10) break@loop
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return articles
  }
}
