package cn.net.polyglot

import io.vertx.core.AbstractVerticle

/**
 * @author zxj5470
 * @date 2018/7/8
 */
class SecondVerticle : AbstractVerticle() {
  override fun start() {
    vertx.createHttpServer().requestHandler { req ->
      req.headers().forEach(::println)
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello World!")
    }.listen(8080)
  }
}
