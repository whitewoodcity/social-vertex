package cn.net.polyglot.verticle.community

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.web.ServletVerticle
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.existsAwait
import io.vertx.kotlin.core.file.propsAwait
import io.vertx.kotlin.core.file.readDirAwait
import io.vertx.kotlin.core.file.readFileAwait
import java.io.File.separator

class DefaultVerticle : ServletVerticle() {
  override suspend fun doGet(request: HttpServletRequest): HttpServletResponse {
    return HttpServletResponse("default.htm", JsonObject().put(ARTICLES, getRecentArticles()))
  }

  private suspend fun getRecentArticles(): JsonArray {

    val articles = JsonArray()

    val dir = config.getString(DIR)

    if (vertx.fileSystem().existsAwait("$dir$separator$COMMUNITY") && vertx.fileSystem().readDirAwait("$dir$separator$COMMUNITY").isNotEmpty()) {
      val years = vertx.fileSystem().readDirAwait("$dir$separator$COMMUNITY").sortedDescending()
      loop@ for (year in years) {
        if (vertx.fileSystem().readDirAwait(year).isNotEmpty()) {
          val months = vertx.fileSystem().readDirAwait(year).sortedDescending()
          for (month in months) {
            if (vertx.fileSystem().propsAwait(month).isDirectory && vertx.fileSystem().readDirAwait(month).isNotEmpty()) {
              val days = vertx.fileSystem().readDirAwait(month).sortedDescending()
              for (day in days) {
                if (vertx.fileSystem().propsAwait(day).isDirectory && vertx.fileSystem().readDirAwait(day).isNotEmpty()) {
                  val hours = vertx.fileSystem().readDirAwait(day).sortedDescending()
                  for (hour in hours) {
                    if (vertx.fileSystem().propsAwait(hour).isDirectory && vertx.fileSystem().readDirAwait(hour).isNotEmpty()) {
                      val directories = vertx.fileSystem().readDirAwait(hour)
                      for (directory in directories) {
                        if (vertx.fileSystem().existsAwait("$directory${separator}publication.json")) {
                          val json = vertx.fileSystem().readFileAwait("$directory${separator}publication.json").toJsonObject()
                          if (articles.add(json).size() > 10) break@loop
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
