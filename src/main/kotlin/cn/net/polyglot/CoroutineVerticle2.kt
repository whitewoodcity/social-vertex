package cn.net.polyglot

import cn.net.polyglot.config.DEFAULT_PORT
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import io.vertx.kotlin.coroutines.CoroutineVerticle as VertxCoroutineVerticle

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class CoroutineVerticle2 : VertxCoroutineVerticle() {
  override suspend fun start() = runBlocking<Unit> {
    val port = config.getInteger("port", DEFAULT_PORT)
    println(this.javaClass.name + "is deployed on $port port")
    val fs = vertx.fileSystem()
    vertx.createHttpServer().requestHandler { req ->
      /**
       * It'll find the URL form ClassLoader if it can't find in common path.
       */
      val path = "cn/net/polyglot/main_verticle.groovy"
      fs.readFile(path) {
        launch {
          if (it.succeeded()) {
            val res = async { it.result().bytes.toKString() }
            req.response()
              .putHeader("content-type", "text/plain")
              .end(res.await())
          } else {
            req.response()
              .putHeader("content-type", "text/plain")
              .end("read file failed")
          }
        }
      }
    }.listen(port)
  }
}
