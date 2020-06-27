package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.Vertx
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.deploymentOptionsOf
import org.awaitility.Awaitility.await
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.nio.file.Paths

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//按照名字升序执行代码
class WebServerTest {
  companion object {
    private val config = JsonObject()
      .put(VERSION, 0.1)
      .put(DIR, Paths.get("").toAbsolutePath().toString() + File.separator + "social-vertex")
      .put(TCP_PORT, 7373)
      .put(HTTP_PORT, 7575)
      .put(HOST, "localhost")
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {

      if (vertx.fileSystem().existsBlocking(config.getString(DIR)))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString(DIR), true)

      val option = deploymentOptionsOf(config = config)
      val fut0 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.WebServerVerticle", option)
      val fut1 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.community.LoginVerticle", option)
      val fut2 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.community.DefaultVerticle", option)
      await().until{
        fut0.isComplete && fut1.isComplete && fut2.isComplete
      }
      context.assertTrue(fut0.succeeded())
      context.assertTrue(fut1.succeeded())
      context.assertTrue(fut2.succeeded())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  private val webClient = WebClient.create(vertx)

  @Test
  fun `test index`(context: TestContext){
    val async = context.async()
    webClient.get(config.getInteger(HTTP_PORT), "localhost", "/")
      .send{
        response ->
        context.assertTrue(response.result().body().toString().contains("html"))
        println(response.result().headers().toList())
        context.assertTrue(response.result().headers().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*", true))
        context.assertTrue(response.result().headers().contains(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*", true))
        async.complete()
      }
  }
}
