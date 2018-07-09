package cn.net.polyglot.verticle

import cn.net.polyglot.testframework.configPort
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
class IMHttpServerVerticleTest{
  private lateinit var vertx: Vertx
  private lateinit var client: WebClient
  @Before
  fun before(context: TestContext) {
    vertx  = Vertx.vertx()
    client = WebClient.create(vertx)
    val opt = configPort(8082)
    vertx.deployVerticle( IMHttpServerVerticle ::class.java.name,opt)
  }

  @Test
  fun sendMessage(context: TestContext) {
    var async = context.async()
    client.post(8082,"localhost","/")
      .sendJsonObject(JsonObject()
        .put("type","search")
        .put("user","zxj@polyglot.net.cn")){
        response ->
        println(response.result().bodyAsString())
        async.complete()
      }

  }

  @After
  fun after(context: TestContext) {
    vertx.close()
  }


}
