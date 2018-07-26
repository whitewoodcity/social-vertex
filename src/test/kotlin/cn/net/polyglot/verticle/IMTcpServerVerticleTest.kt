package cn.net.polyglot.verticle

/**
 * @author zxj5470
 * @date 2018/7/26
 */
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.DeploymentOptions
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class IMTcpServerVerticleTest {

  companion object {
    private val config = JsonObject().put("version", 0.1)
      .put("dir", "social-vertex")
      .put("host", "localhost")
      .put("tcp-port", 7373)
      .put("http-port", 7575)
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      //clean the directory
      if (vertx.fileSystem().existsBlocking(config.getString("dir")))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString("dir"), true)

      val option = DeploymentOptions(config = config)
      vertx.deployVerticle(IMTcpServerVerticle::class.java.name, option, context.asyncAssertSuccess())
      vertx.deployVerticle(IMMessageVerticle::class.java.name, option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun testSearch(context: TestContext) {
    val async = context.async()
    val client = vertx.createNetClient()
    val requestJson = JsonObject()
      .put("type", "search")
      .put("action", "user")
      .put("id", "zxj5470")

    client.connect(config.getInteger("tcp-port"), "localhost") {
      if (it.succeeded()) {
        val socket = it.result()
        socket.handler {
          println(it.toString())
          async.complete()
        }
        socket.write(requestJson.toString().plus("\r\n"))
      }
    }
  }
}
