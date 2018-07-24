package cn.net.polyglot.verticle

import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.makeAppDirs
import cn.net.polyglot.handler.handleRequests
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
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
          req.response()
            .end(JsonObject().put("info", "request method is not POST").toString())

          return@bodyHandler
        }
        try {
          val json = buffer.toJsonObject()
          val fs = vertx.fileSystem()
          val type = json.getString("type")

          when (type) {
          // Http 中 Message 类型为 接收到 TcpVerticle 发送出的 HTTP 请求, 由于请求必须为异步,
          // 因此需与普通的 handler 区分处理
            MESSAGE -> crossDomainMessage(json, req)
            else -> {
              val ret = handleRequests(fs, json, type)
              println(ret)
              req.response()
                .putHeader("content-type", "application/json")
                .end(ret.toString())
            }
          }
        } catch (e: Exception) {
          req.response()
            .end(JsonObject().put("info", "json format error").toString())
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

  private fun crossDomainMessage(json: JsonObject?, req: HttpServerRequest) {
    vertx.eventBus().send<JsonObject>(IMHttpServerVerticle::class.java.name, json) {
      if (it.succeeded()) {
        val msg = it.result().body()
        req.response()
          .putHeader("content-type", "application/json")
          .end(msg.toString())
      }
    }
  }
}
