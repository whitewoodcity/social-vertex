package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.handler.handle
import io.vertx.core.file.FileSystem
import io.vertx.core.http.HttpMethod
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMHttpServerVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val port = config.getInteger("port", DEFAULT_PORT)
    println(this.javaClass.name + " is deployed on $port port")

    val fs = vertx.fileSystem()
    fs.mkdirIfNotExists()

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

  private fun FileSystem.mkdirIfNotExists(dirName: String = ".social-vertex") {
    val fs = this
    fs.exists(dirName) {
      if (it.result()) {
        println("$dirName directory exist")
      } else {
        fs.mkdir(dirName) { mkr ->
          if (mkr.succeeded()) {
            println("mkdir $dirName succeed")
          } else {
            println("mkdir failed, please check the permission")
          }
        }
      }
    }
  }
}
