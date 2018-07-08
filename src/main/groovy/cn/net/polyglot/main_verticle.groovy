package cn.net.polyglot

import io.vertx.core.Vertx

Vertx vertx = vertx

vertx.deployVerticle(SecondVerticle.class.name)
