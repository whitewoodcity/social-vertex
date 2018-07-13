package cn.net.polyglot.verticle

import cn.net.polyglot.testframework.configPort
import cn.net.polyglot.utils.text
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
class IMHttpServerVerticleTest {
  private lateinit var vertx: Vertx
  private lateinit var client: WebClient
  private val port = 8082

  @Before
  fun before(context: TestContext) {
    vertx = Vertx.vertx()
    vertx.deployVerticle(IMMessageVerticle::class.java.name)
    client = WebClient.create(vertx)
    val opt = configPort(port)
    vertx.deployVerticle(IMHttpServerVerticle::class.java.name, opt)
  }

  @Test
  fun sendMessage(context: TestContext) {
    val async = context.async()
    client.post(port, "localhost", "/")
      .sendJsonObject(JsonObject()
        .put("type", "search")
        .put("user", "zxj@polyglot.net.cn")) { response ->
        if (response.succeeded()) {
          println(response.result().body().text())
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
