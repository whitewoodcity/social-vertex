package cn.net.polyglot

import io.vertx.core.AbstractVerticle
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking

/**
 * @author zxj5470
 * @date 2018/7/8
 */
class CoroutineVerticle : AbstractVerticle() {
  override fun start() {
    val fs = vertx.fileSystem()
    vertx.createHttpServer().requestHandler { req ->
      val path = this::class.java.getResource("main_verticle.groovy").path
      fs.readFile(path) {
        runBlocking {
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
    }.listen(8080)
  }
}

private fun ByteArray.toKString(): String = String(this)
