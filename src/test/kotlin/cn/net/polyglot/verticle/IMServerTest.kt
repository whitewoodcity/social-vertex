package cn.net.polyglot.verticle

import cn.net.polyglot.utils.writeln
import io.vertx.core.Launcher
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

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//按照名字升序执行代码
class IMServerTest {
  //init test, deploy verticles and close vert.x instance after running tests
  companion object {
    private val config = JsonObject()
      .put("version",0.1)
      .put("dir",File(Launcher::class.java.protectionDomain.codeSource.location.toURI()).parent + File.separator + "social-vertex")
      .put("tcp-port", 7373)
      .put("http-port", 7575)
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      val option = DeploymentOptions(config = config)
      vertx.deployVerticle(IMTcpServerVerticle::class.java.name, option, context.asyncAssertSuccess())
      vertx.deployVerticle(IMHttpServerVerticle::class.java.name, option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  private val webClient = WebClient.create(vertx)
  private val netClient = vertx.createNetClient()

  @Test
  fun testAccountRegister(context: TestContext) {
    val async = context.async()
    webClient.post(config.getInteger("http-port"), "localhost", "/")
      .sendJsonObject(JsonObject()
        .put("type", "search")
        .put("user", "zxj@polyglot.net.cn")) { response ->
        if (response.succeeded()) {
          println(response.result().body().toString())
          async.complete()
        } else {
          System.err.println("failed")
          async.complete()
        }
      }
  }

  @Test
  fun testSearch(context: TestContext) {
    val async = context.async()
    netClient.connect(config.getInteger("tcp-port"), "localhost") {
      if (it.succeeded()) {
        val socket = it.result()

        socket.handler {
          // avoid sticking packages
          val buffers = it.toString().split("\r\n").filter { it.isNotEmpty() }
          buffers.forEach {
            val ret = it.contains("response")
            println(it)
            assert(ret)
          }
          async.complete()
        }
        socket.writeln("""{"type":"search","action":"request","user":"zxj@polyglot.net.cn","version":0.1}""")
      }
    }
  }

  @Test
  fun testApplication(context: TestContext) {
    //todo need to assert some response not just println debug info.
  }
}
