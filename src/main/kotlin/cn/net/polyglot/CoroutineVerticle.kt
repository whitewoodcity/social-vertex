package cn.net.polyglot

import cn.net.polyglot.config.DEFAULT_PORT
import io.vertx.core.AbstractVerticle
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking

/**
 * @author zxj5470
 * @date 2018/7/8
 */
class CoroutineVerticle : AbstractVerticle() {
  override fun start() {
    val port = config().getInteger("port", DEFAULT_PORT)
    println(this.javaClass.name + "is deployed on $port port")
    val fs = vertx.fileSystem()
    vertx.createHttpServer().requestHandler { req ->
      /**
       * It'll find the URL form ClassLoader if it can't find in common path.
       */
      val path = "cn/net/polyglot/main_verticle.groovy"
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
    }.listen(port)
  }
}

fun ByteArray.toKString(): String = String(this)
