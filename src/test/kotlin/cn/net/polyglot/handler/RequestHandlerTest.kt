package cn.net.polyglot.handler

import cn.net.polyglot.config.EventBusConstants
import cn.net.polyglot.testframework.configPort
import cn.net.polyglot.verticle.IMHttpServerVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner::class)
class RequestHandlerTest {
  private lateinit var vertx: Vertx
  private lateinit var client: WebClient
  private val port = 8091

  @Before
  fun before(context: TestContext) {
    vertx = Vertx.vertx()
    val opt = configPort(port)
    vertx.deployVerticle(IMHttpServerVerticle::class.java.name, opt)
    client = WebClient.create(vertx)
  }

  @Test
  fun testHandleMessage(context: TestContext) {
    vertx.deployVerticle(TestHandleMessageVerticle::class.java.name)
    val async = context.async()
    val json = JsonObject(mapOf(
      "type" to "message",
      "from" to "inquiry@polyglot.net.cn",
      "to" to "customer@w2v4.com",
      "body" to "你好吗？",
      "version" to 0.1
    ))
    println(json)
    client.post(port, "localhost", "/")
      .sendJsonObject(json) { response ->
        if (response.succeeded()) {
          println(response.result().bodyAsJsonObject())
          async.complete()
        } else {
          System.err.println("failed")
          async.complete()
        }
      }
  }

  @Test
  fun testHandleUserRegistry(context: TestContext) {
    vertx.deployVerticle(TestHandleUserVerticle::class.java.name)
    val async = context.async()
    val json = JsonObject("""{
"type":"user",
"action":"registry",
"user":"zxj5470",
"crypto":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
""")
    println(json)
    client.post(port, "localhost", "/")
      .sendJsonObject(json) { response ->
        if (response.succeeded()) {
          println(response.result().bodyAsJsonObject())
          async.complete()
        } else {
          System.err.println("failed")
          async.complete()
        }
      }
  }

  @After
  fun after(context: TestContext) {
    vertx.close()
  }
}

class TestHandleMessageVerticle : AbstractVerticle() {
  override fun start() {
    println(this.javaClass.name + " is deployed.")
    val eventBus = vertx.eventBus()
    val httpConsumer = eventBus.localConsumer<JsonObject>(EventBusConstants.HTTP_TO_MSG)
    httpConsumer.handler { msg ->
      msg.handleMessage(vertx.fileSystem(), msg.body())
    }
  }
}

class TestHandleUserVerticle : AbstractVerticle() {
  override fun start() {
    println(this.javaClass.name + " is deployed.")
    val eventBus = vertx.eventBus()
    val httpConsumer = eventBus.localConsumer<JsonObject>(EventBusConstants.HTTP_TO_MSG)
    httpConsumer.handler { msg ->
      msg.handleUser(vertx.fileSystem(), msg.body())
    }
  }
}
