package cn.net.polyglot.verticle.search

import cn.net.polyglot.config.DIR
import cn.net.polyglot.config.SEARCH
import cn.net.polyglot.config.USER
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.existsAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File

class SearchVerticle : CoroutineVerticle() {
  override suspend fun start() {
    vertx.eventBus().consumer<String>(this::class.java.name) {
      launch { it.reply(search(it.body())) }
    }
  }

  private suspend fun search(keyword: String): JsonObject {
    if(keyword.isBlank()) return JsonObject().put(SEARCH,false)
    val dir = config.getString(DIR) + File.separator + keyword
    if(vertx.fileSystem().existsAwait(dir)){
      return JsonObject().put(SEARCH, true).put(USER, keyword)
    }
    return JsonObject().put(SEARCH, true)
  }
}
