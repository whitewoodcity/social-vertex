package cn.net.polyglot.server

import cn.net.polyglot.config.DEFAULT_PORT
import io.vertx.core.http.HttpMethod
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class InstantVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val port = config.getInteger("port", DEFAULT_PORT)
    println(this.javaClass.name + "is deployed on $port port")
    val fs = vertx.fileSystem()
//    fs.mkdir(".social-vertx")
    vertx.createHttpServer().requestHandler { req ->
      launch(vertx.dispatcher()) {
        req.bodyHandler {
          if (req.method() == HttpMethod.POST) {
            val ret = handle(it)
            req.response()
              .putHeader("content-type", "text/plain")
              .end(ret)
          } else {
            req.response()
              .putHeader("content-type", "text/plain")
              .end("Your request method is not POST")
          }
        }
      }
    }.listen(port)
  }
}
