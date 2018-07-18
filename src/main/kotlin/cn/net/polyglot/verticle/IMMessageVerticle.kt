package cn.net.polyglot.verticle

import cn.net.polyglot.handler.handleEventBus
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject

/**
 * receive JsonObject from other Verticles. Please make sure what you send must be a JsonObject.
 * @author zxj5470
 * @date 2018/7/10
 */
@Deprecated("")
class IMMessageVerticle : AbstractVerticle() {
  override fun start() {
    println(this.javaClass.name + " is deployed.")

    vertx.eventBus().consumer<JsonObject>(IMMessageVerticle::class.java.name){
      it.handleEventBus(vertx)
    }
  }
}
