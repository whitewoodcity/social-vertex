package cn.net.polyglot.verticle

import com.sun.deploy.util.BufferUtil
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler

class WebServerVerticle : AbstractVerticle(){

  override fun start() {
    val router = Router.router(vertx)

    router.route().handler(CookieHandler.create())
    router.route().handler(BodyHandler.create().setBodyLimit(1 * BufferUtil.MB))



  }
}
