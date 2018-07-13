package cn.net.polyglot.handler

import cn.net.polyglot.config.makeDirsBlocking
import cn.net.polyglot.testframework.configPort
import cn.net.polyglot.testframework.deployAnonymousHandlerVerticle
import cn.net.polyglot.verticle.IMHttpServerVerticle
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(VertxUnitRunner::class)
class RequestUserHandlerTest {

  private var vertx: Vertx = Vertx.vertx()
  private var client: WebClient
  private val port = 8085

  // use `init` instead of before because these file contains many tests.
  init {
    client = WebClient.create(vertx)
    val opt = configPort(port)
    vertx.deployVerticle(IMHttpServerVerticle::class.java.name, opt)
    makeDirsBlocking(vertx)
    createTestUser()
  }


  @Test
  fun testHandleUserRegistry(context: TestContext) {
    vertx.deployAnonymousHandlerVerticle(JsonMessage::handleUser)
    val async = context.async()
    val randomName = Random().ints(5).map { Math.abs(it) % 25 + 97 }.toArray().map { it.toChar() }.joinToString("")
    val json = JsonObject("""{
"type":"user",
"action":"registry",
"user":"$randomName",
"crypto":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
""")
    println(json)
    client.post(json, async, port)
  }

  @Test
  fun testHandleUserLogin(context: TestContext) {
    vertx.deployAnonymousHandlerVerticle(JsonMessage::handleUser)
    val async = context.async()
    val randomString = Random().ints(5).map { Math.abs(it) % 25 + 97 }.toArray().map { it.toChar() }.joinToString("")
    println(randomString)
    val json = JsonObject("""{
"type":"user",
"action":"login",
"user":"yecbv",
"crypto":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
""")
    println(json)
    client.post(json, async, port)
  }

  @After
  fun after(context: TestContext) {
    vertx.close()
  }

  private fun createTestUser() {
    try {
      vertx.fileSystem().mkdirsBlocking(".social-vertex/user/yecbv")
      vertx.fileSystem().writeFileBlocking(".social-vertex/user/yecbv/user.json", Buffer.buffer("""{"type":"user","action":"login","user":"yecbv","crypto":"431fe828b9b8e8094235dee515562247","version":0.1}"""))
    } catch (e: Exception) {
    }
  }
}
