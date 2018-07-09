package cn.net.polyglot

import cn.net.polyglot.config.DEFAULT_PORT
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
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


      //todo 用launch（dispatcher）和await result/event函数修改代码
      launch(vertx.dispatcher()){
        val result = awaitResult<Buffer> { fs.readFile(path,it) }
        //todo the rest
      }

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
