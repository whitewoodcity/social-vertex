package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.DeploymentOptions
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.nio.file.Paths
import java.util.ArrayList

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

      val option = DeploymentOptions(config = config)
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.WebServerVerticle", option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  private val webClient = WebClient.create(vertx)

  @Test
  fun testIndex(context: TestContext){
    val async = context.async()
    webClient.get(config.getInteger(HTTP_PORT), "localhost", "/")
      .send{
        response ->
        context.assertTrue(response.result().body().toString().contains("Social Vertex"))
        async.complete()
      }
  }

//  @Test
//  fun testTemplate(context: TestContext) {
//    val async = context.async()
//
//    val list = ArrayList<String>()
//    list.add("test0")
//    list.add("test1")
//
//    webClient.post(config.getInteger(HTTP_PORT), "localhost", "/index.htm")
//      .sendForm(MultiMap.caseInsensitiveMultiMap().set("test", list))
////      .send()
//      { response ->
//        println(response.result().body())
////        context.assertTrue(response.result().body().toString().contains("nice"))
//        async.complete()
//      }
//  }
}
