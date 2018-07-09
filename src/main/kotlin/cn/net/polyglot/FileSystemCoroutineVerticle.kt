package cn.net.polyglot

import cn.net.polyglot.config.DEFAULT_PORT
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class FileSystemCoroutineVerticle : CoroutineVerticle() {
  override suspend fun start()  {
    val port = config.getInteger("port", DEFAULT_PORT)
    println(this.javaClass.name + "is deployed on $port port")
    val fs = vertx.fileSystem()
    vertx.createHttpServer().requestHandler { req ->
      /**
       * It'll find the URL from ClassLoader path if it can't find in local path.
       */
      val path = "cn/net/polyglot/main_verticle.groovy"

      launch(vertx.dispatcher()) {
        try {
          val result = awaitResult<Buffer> { fs.readFile(path, it) }
          val returnContent = result.bytes.toKString()
          req.response()
            .putHeader("content-type", "text/plain")
            .end(returnContent)
        } catch (e: Exception) {
          e.printStackTrace()
          req.response()
            .putHeader("content-type", "text/plain")
            .end("read file failed")
        }
      }
    }.listen(port)
  }
}

fun ByteArray.toKString(): String = String(this)
