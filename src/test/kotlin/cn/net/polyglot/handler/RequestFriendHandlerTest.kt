package cn.net.polyglot.handler

import cn.net.polyglot.config.makeDirsBlocking
import cn.net.polyglot.testframework.configPort
import cn.net.polyglot.testframework.deployAnonymousHandlerVerticle
import cn.net.polyglot.verticle.IMHttpServerVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class RequestFriendHandlerTest {

  private var vertx: Vertx = Vertx.vertx()
  private var client: WebClient
  private val port = 8083

  // use `init` instead of before because these file contains many tests.
  init {
    client = WebClient.create(vertx)
    val opt = configPort(port)
    vertx.deployVerticle(IMHttpServerVerticle::class.java.name, opt)
    makeDirsBlocking(vertx)
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
    client.post(json, async,port)
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
    client.post(json, async,port)
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
    client.post(json, async,port)
  }

  @After
  fun after(context: TestContext) {
    vertx.close()
  }
}
