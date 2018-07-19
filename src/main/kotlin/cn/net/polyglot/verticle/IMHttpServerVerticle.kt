package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.config.NumberConstants
import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import cn.net.polyglot.handler.*
import cn.net.polyglot.utils.text
import cn.net.polyglot.utils.tryJson
import io.vertx.core.AbstractVerticle
import io.vertx.core.file.FileSystem
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMHttpServerVerticle : AbstractVerticle() {
  override fun start() {
    val port = config().getInteger("port", DEFAULT_PORT)

    vertx.createHttpServer().requestHandler { req ->
      req.bodyHandler { buffer ->
        if (req.method() == HttpMethod.POST) {
          val json = buffer.text().tryJson()
          if (json == null) {
            req.response()
              .putHeader("content-type", "application/json")
              .end("""{"info":"json format error"}""")
          } else {

            fun handleTypes(fs: FileSystem, json: JsonObject): JsonObject {
              val type = json.getString("type", "")
              val version = json.getDouble("version", NumberConstants.CURRENT_VERSION)
              return when (type) {
                MESSAGE -> message(fs, json, directlySend = {}, indirectlySend = {})
                SEARCH -> searchUser(fs, json)
                FRIEND -> friend(fs, json)
                USER -> userAuthorize(fs, json)
                else -> defaultMessage(fs, json)
              }
            }

            val ret = handleTypes(vertx.fileSystem(), json)
            req.response()
              .putHeader("content-type", "application/json")
              .end(ret.toString())
          }
        } else {
          req.response().end("""{"info":"request method is not POST"}""")
        }
      }
    }.listen(port) {
      if (it.succeeded()) {
        println(this.javaClass.name + " is deployed on $port port")
      } else {
        println("deploy on $port failed")
      }
    }
  }
}
