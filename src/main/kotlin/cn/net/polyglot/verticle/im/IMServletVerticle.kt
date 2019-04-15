package cn.net.polyglot.verticle.im

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.user.UserVerticle
import cn.net.polyglot.verticle.web.ServletVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.sendAwait

class IMServletVerticle:ServletVerticle() {
  override suspend fun doPut(json: JsonObject, session: ServletVerticle.Session): Response {
    println("hello i am here"+json)
    val bodyJson = json.getJsonObject(BODY_AS_JSON)
    val type = bodyJson.getString(TYPE)
    println("hello i am here 1")
    return when (type) {
//      USER -> {
//        val responseMessage = vertx.eventBus().sendAwait<JsonObject>(UserVerticle::class.java.name, bodyJson)
//        Response(responseMessage.body())
//      }
      FRIEND, MESSAGE -> {
        vertx.eventBus().send(IMMessageVerticle::class.java.name, bodyJson)
        Response()
      }
      else ->{
        println("hello i am here"+bodyJson.toString())
        val responseJson = vertx.eventBus().sendAwait<JsonObject>(IMMessageVerticle::class.java.name, bodyJson).body()
        Response(responseJson)
      }
    }
  }
}
