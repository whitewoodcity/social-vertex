package cn.net.polyglot

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import test.MyVerticle


@RunWith(VertxUnitRunner::class)
class ClientTest {
  private lateinit var vertx: Vertx

  @Before
  fun before(context: TestContext) {
    vertx = Vertx.vertx()
    vertx.deployVerticle(MyVerticle::class.java.name)
  }

  @Test
  fun sendMessage(context: TestContext) {
    var async = context.async()
    var client = WebClient.create(vertx)
    client.post("https://localhost:8080")
      .sendJsonObject(JsonObject()
        .put("type","search")
        .put("user","zxj@polyglot.net.cn")){response ->
        async.complete()
      }

  }

  @After
  fun after(context: TestContext) {
    vertx.close()
  }


}
