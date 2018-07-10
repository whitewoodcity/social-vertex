package cn.net.polyglot.verticle

import cn.net.polyglot.handler.handleEventBus
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

/**
 * receive JsonObject from other Verticles. Please make sure what you send must be a JsonObject.
 * @author zxj5470
 * @date 2018/7/10
 */
class IMMessageVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val eventBus = vertx.eventBus()
    val consumer = eventBus.localConsumer<JsonObject>("IMHttpServer to IMMessageVerticle")
    consumer.handler { msg ->
      msg.handleEventBus(vertx)
    }
  }
}
