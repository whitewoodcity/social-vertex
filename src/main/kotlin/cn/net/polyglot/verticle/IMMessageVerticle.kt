package cn.net.polyglot.verticle

import cn.net.polyglot.config.EventBusConstants.HTTP_TO_MSG
import cn.net.polyglot.config.EventBusConstants.TCP_TO_MSG
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

    val eventBus = vertx.eventBus()
    val httpConsumer = eventBus.localConsumer<JsonObject>(HTTP_TO_MSG)
    val tcpConsumer = eventBus.localConsumer<JsonObject>(TCP_TO_MSG)

    httpConsumer.handler { msg ->
      msg.handleEventBus(vertx)
    }

    tcpConsumer.handler { msg ->
      msg.handleEventBus(vertx)
    }
  }

}
