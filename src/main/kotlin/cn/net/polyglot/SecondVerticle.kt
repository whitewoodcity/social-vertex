package cn.net.polyglot

import cn.net.polyglot.config.DEFAULT_PORT
import io.vertx.core.AbstractVerticle

/**
 * @author zxj5470
 * @date 2018/7/8
 */
class SecondVerticle : AbstractVerticle() {
  override fun start() {
    val port = config().getInteger("port", DEFAULT_PORT)
    println(this.javaClass.name +"is deployed on $port port")
    vertx.createHttpServer().requestHandler { req ->
      req.headers().forEach(::println)
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello World!")
    }.listen(port)
  }
}
