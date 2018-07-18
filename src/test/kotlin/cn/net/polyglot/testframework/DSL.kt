package cn.net.polyglot.testframework

import cn.net.polyglot.config.defaultJsonObject
import cn.net.polyglot.verticle.IMMessageVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.DeploymentOptions

/**
 * @author zxj5470
 * @date 2018/7/8
 */
infix fun Any.shouldBe(other: Any) = (this == other).also { result ->
  if (!result) {
    System.err.println("It should be \n$other\nBut actually it is \n$this")
  }
  assert(result)
}

fun configPort(port: Int = 8080) = DeploymentOptions(config = defaultJsonObject.apply { put("port", port) })

fun generateVerticle(function: Message<JsonObject>.(fs: FileSystem, jsonObject: JsonObject) -> Unit): AbstractVerticle {
  return object : AbstractVerticle() {
    override fun start() {
      println("function is deployed.")
      val eventBus = vertx.eventBus()
      val httpConsumer = eventBus.localConsumer<JsonObject>(IMMessageVerticle::class.java.name)
      httpConsumer.handler { msg ->
        function(msg, vertx.fileSystem(), msg.body())
      }
    }
  }
}

fun Vertx.deployAnonymousHandlerVerticle(function: Message<JsonObject>.(fs: FileSystem, jsonObject: JsonObject) -> Unit){
  this.deployVerticle(generateVerticle(function))
}
