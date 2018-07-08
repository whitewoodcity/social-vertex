package cn.net.polyglot

import io.vertx.core.Vertx

Vertx vertx = vertx

vertx.createHttpServer().requestHandler { req ->
  req.response()
    .putHeader("content-type", "text/plain")
    .end("Hello World!")
}.listen(8080)

vertx.deployVerticle(SecondVerticle.class.name)
