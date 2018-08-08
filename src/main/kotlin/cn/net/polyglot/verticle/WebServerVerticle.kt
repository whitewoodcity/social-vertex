package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import com.sun.deploy.util.BufferUtil.MB
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler

class WebServerVerticle : AbstractVerticle() {

  override fun start() {
    val httpServer = vertx.createHttpServer()
    val router = Router.router(vertx)

    router.route().handler(CookieHandler.create())
    router.route().handler(BodyHandler.create().setBodyLimit(1 * MB))

    router.put("/:$TYPE/:$SUBTYPE").handler { routingContext ->
      try {
        val type = routingContext.request().getParam(TYPE)
        val subtype = routingContext.request().getParam(SUBTYPE)

        val json = routingContext.bodyAsJson
          .put(TYPE, type)
          .put(SUBTYPE, subtype)
        when (type) {
          FRIEND, MESSAGE -> {
            vertx.eventBus().send(IMMessageVerticle::class.java.name, json)
            routingContext.response().end()
          }
          else -> {
            var result: JsonObject?
            vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name, json) {
              result = it.result().body()
              routingContext.response().end(result.toString())
            }
          }
        }
      } catch (e: Exception) {
        routingContext.response().end(e.message)
        return@handler
      }
    }

    httpServer.requestHandler(router::accept).listen(config().getInteger(HTTP_PORT)) {
      if (it.succeeded()) {
        println("${this::class.java.name} is deployed")
      } else {
        println("${this::class.java.name} fail to deploy")
      }
    }
  }

}

