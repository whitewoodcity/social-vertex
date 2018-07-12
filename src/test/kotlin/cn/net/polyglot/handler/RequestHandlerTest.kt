package cn.net.polyglot.handler

import cn.net.polyglot.testframework.configPort
import cn.net.polyglot.testframework.deployAnonymousHandlerVerticle
import cn.net.polyglot.verticle.IMHttpServerVerticle
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

typealias JsonMessage = Message<JsonObject>


@RunWith(VertxUnitRunner::class)
class RequestHandlerTest {

  private lateinit var vertx: Vertx
  private lateinit var client: WebClient
  private val port = 8083

  @Before
  fun before(context: TestContext) {
    vertx = Vertx.vertx()
    client = WebClient.create(vertx)
    val opt = configPort(port)
    vertx.deployVerticle(IMHttpServerVerticle::class.java.name, opt)
  }

  @Test
  fun testHandleMessage(context: TestContext) {
    vertx.deployAnonymousHandlerVerticle(JsonMessage::handleMessage)
    val async = context.async()
    val json = JsonObject("""{
"type":"message",
"from":"inquiry@polyglot.net.cn",
"to":"customer@w2v4.com",
"body":"你好吗？",
"version":0.1}
""")
    println(json)
    post(json, async)
  }

  @Test
  fun testHandleUserRegistry(context: TestContext) {
    vertx.deployAnonymousHandlerVerticle(JsonMessage::handleUser)
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
    post(json, async)
  }

  @Test
  fun testHandleFriendRequest(context: TestContext) {
    vertx.deployAnonymousHandlerVerticle(JsonMessage::handleFriend)
    val async = context.async()
    val json = JsonObject("""{
"type":"friend",
"action":"request",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"message":"请添加我为你的好友，我是哲学家",
"version":0.1
}""")
    println("input")
    System.err.println(json)
    post(json, async)
  }

  @Test
  fun testHandleFriendResponse(context: TestContext) {
    vertx.deployAnonymousHandlerVerticle(JsonMessage::handleFriend)
    val async = context.async()
    val json = JsonObject("""{
"type":"friend",
"action":"response",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"accept":true,
"version":0.1
}""")
    println("input")
    System.err.println(json)
    post(json, async)
  }

  @Test
  fun testHandleFriendDelete(context: TestContext) {
    vertx.deployAnonymousHandlerVerticle(JsonMessage::handleFriend)
    val async = context.async()
    val json = JsonObject("""{
"type":"friend",
"action":"delete",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"version":0.1
}""")
    println("input")
    System.err.println(json)
    post(json, async)
  }

  private fun post(json: JsonObject, async: Async) {
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
