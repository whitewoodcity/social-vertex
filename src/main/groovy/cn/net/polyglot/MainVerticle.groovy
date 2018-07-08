package cn.net.polyglot

import io.vertx.core.AbstractVerticle

class MainVerticle extends AbstractVerticle {
  void start() {
    vertx.deployVerticle(SecondVerticle.class.name)
  }
}
