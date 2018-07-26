package cn.net.polyglot.verticle

import cn.net.polyglot.config.JsonKeys
import cn.net.polyglot.config.TypeConstants
import cn.net.polyglot.utils.makeAppDirs
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMHttpServerVerticle : AbstractVerticle() {
  override fun start() {
    val port = config().getInteger("http-port")
    makeAppDirs(vertx)

    vertx.createHttpServer().requestHandler { req ->
      req.response()
        .putHeader("content-type", "application/json")

      req.bodyHandler { buffer ->
        if (req.method() != HttpMethod.POST) {
          req.response().end(JsonObject().put(JsonKeys.INFO, "request method is not POST").toString())
        }
        try {
          // make sure json format is correct and transmit it to IMMessageVerticle
          val json = buffer.toJsonObject()
          val type = json.getString(JsonKeys.TYPE)
          if (type !in TypeConstants.SUPPORTED_TYPE) {
            req.response()
              .putHeader("content-type", "application/json")
              .end(JsonObject().put(JsonKeys.INFO, "request type error").toString())
          }

          vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name, json) {
            if (it.succeeded()) {
              val msg = it.result().body()
              req.response()
                .putHeader("content-type", "application/json")
                .end(msg.toString())
            }
          }
        } catch (e: Exception) {
          req.response()
            .end(JsonObject().put(JsonKeys.INFO, "json format error").toString())
        }
      }
    }.listen(port) {
      if (it.succeeded()) {
        println(this.javaClass.name + " is deployed on port $port")
      } else {
        println("deploy on $port failed")
      }
    }
  }
}
