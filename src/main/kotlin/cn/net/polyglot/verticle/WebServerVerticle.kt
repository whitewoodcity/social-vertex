package cn.net.polyglot.verticle

import com.sun.deploy.util.BufferUtil.MB
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpMethod
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

    httpServer.requestHandler { req ->
      req.bodyHandler {
        if (req.method() != HttpMethod.POST) {
          req.response().end("request method is not POST")
          return@bodyHandler
        }
        try {
          val json = it.toJsonObject()
          val type = json.getString("type")
          when (type) {
            "message" -> {
              vertx.eventBus().send(IMMessageVerticle::class.java.name, json)
              req.response().end()
            }
            else -> {
              var result: JsonObject?
              vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name, json) {
                result = it.result().body()
                req.response().end(result.toString())
              }
            }
          }
        }catch (e:Exception){
          req.response().end(e.message)
          return@bodyHandler
        }
      }
    }.listen(config().getInteger("http-port")) {
      if (it.succeeded()) {
        println("${this::class.java.name} is deployed")
      } else {
        println("${this::class.java.name} fail to deploy")
      }
    }
  }

}

