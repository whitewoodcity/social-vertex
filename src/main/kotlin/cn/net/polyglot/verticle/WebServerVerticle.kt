package cn.net.polyglot.verticle

import com.sun.deploy.util.BufferUtil
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
    router.route().handler(BodyHandler.create().setBodyLimit(1 * BufferUtil.MB))

    httpServer.requestHandler { req ->
      req.bodyHandler {
        if (req.method() != HttpMethod.POST) {
          println("方法不为POST")
          return@bodyHandler
        }
        val json = it.toJsonObject()
        val type = json.getString("type")
        when(type){
          "user"->{
            var result:JsonObject?
              vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,json) {
              result = it.result().body()
              req.response().end(result.toString())
            }
          }
        }
      }
    }.listen(8080) {
      if (it.succeeded()) {
        println("Succeed")
      } else {
        println("Failed:${it.cause()}")
      }
    }
  }

}

