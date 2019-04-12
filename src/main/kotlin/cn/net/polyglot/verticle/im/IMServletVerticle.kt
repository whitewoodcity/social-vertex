package cn.net.polyglot.verticle.im

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.web.ServletVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.sendAwait

class IMServletVerticle:ServletVerticle() {
  override suspend fun doPut(json: JsonObject, session: ServletVerticle.Session): Response {
    val bodyJson = json.getJsonObject(BODY_AS_JSON)
    val type = bodyJson.getString(TYPE)
    return when (type) {
      FRIEND, MESSAGE -> {
        vertx.eventBus().send(IMMessageVerticle::class.java.name, bodyJson)
        Response()
      }
      else ->{
        val responseJson = vertx.eventBus().sendAwait<JsonObject>(IMMessageVerticle::class.java.name, bodyJson).body()
        Response(responseJson)
      }
    }
  }
}
