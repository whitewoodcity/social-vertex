package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.utils.mkdirIfNotExists
import cn.net.polyglot.utils.text
import cn.net.polyglot.utils.tryJson
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
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

    vertx.createNetClient().connect(port + 10, "localhost") {
      if (it.succeeded()) {
        val socket = it.result()
        socket.write("")
      }
    }

    vertx.createHttpServer().requestHandler { req ->
      req.bodyHandler { buffer ->
        if (req.method() == HttpMethod.POST) {
          val json = buffer.text().tryJson()
          if (json == null) {
            req.response()
              .putHeader("content-type", "text/plain")
              .end("""{"message":"json format error"}""")
          } else {
            launch(vertx.dispatcher()) {
              vertx.eventBus().send<JsonObject>("IMHttpServer to IMMessageVerticle",
                buffer.text()) { ar ->
                if (ar.succeeded()) {
                  val ret = ar.result().body()
                  println(ret)
                  req.response()
                    .putHeader("content-type", "text/plain")
                    .end(ret.toString())
                }
              }
            }
          }
        }
      }
    }.listen(port)
  }
}
