package cn.net.polyglot

import io.vertx.core.AbstractVerticle

class MainVerticle extends AbstractVerticle {
  void start() {
    vertx.createHttpServer().requestHandler { req ->
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello World!")
    }.listen(8080)
  }
}
