package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.config.NumberConstants
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.handler.handleRequests
import cn.net.polyglot.utils.text
import cn.net.polyglot.utils.tryJson
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
    val port = config().getInteger("port", DEFAULT_PORT)

    vertx.createHttpServer().requestHandler { req ->
      req.bodyHandler { buffer ->
        if (req.method() == HttpMethod.POST) {
          val json = buffer.text().tryJson()
          if (json == null) {
            req.response()
              .putHeader("content-type", "application/json")
              .end("""{"info":"json format error"}""")
          } else {

            val fs = vertx.fileSystem()
            val type = json.getString("type", "")
            val version = json.getDouble("version", NumberConstants.CURRENT_VERSION)

            if (type == MESSAGE) {
              // Message 为 接收到 TcpVerticle 发送出的 HTTP 请求
              crossDomainMessage(json, req)
            } else {
              val ret = handleRequests(fs, json, type)
              println(ret)
              req.response()
                .putHeader("content-type", "application/json")
                .end(ret.toString())
            }
          }
        } else {
          req.response()
            .putHeader("content-type", "application/json")
            .end("""{"info":"request method is not POST"}""")
        }
      }
    }.listen(port) {
      if (it.succeeded()) {
        println(this.javaClass.name + " is deployed on $port port")
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
