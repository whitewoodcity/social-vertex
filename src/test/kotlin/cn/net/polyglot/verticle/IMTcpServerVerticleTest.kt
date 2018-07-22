package cn.net.polyglot.verticle

import cn.net.polyglot.utils.writeln
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.DeploymentOptions
import org.junit.*
import org.junit.runner.RunWith

/**
 * @author zxj5470
 * @date 2018/7/9
 */

@RunWith(VertxUnitRunner::class)
class IMTcpServerVerticleTest {
  //init test, deploy verticles and close vert.x instance after running tests
  companion object {
    private val config = JsonObject().put("port", 8080)
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      val option = DeploymentOptions(config = config)
      vertx.deployVerticle(IMTcpServerVerticle::class.java.name, option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  private val client = vertx.createNetClient()

  @Test
  fun testApplication(context: TestContext) {
    //todo need to assert some response not just println debug info.
    val async = context.async()
    client.connect(config.getInteger("port"), "localhost") {
      if (it.succeeded()) {
        val socket = it.result()

        socket.handler {
          println(it.toString())
        }

        var i = 0
        vertx.setPeriodic(2333L) {
          socket.writeln("""{"type":"search","id":"zxj5470"}""")
          if (i < 3) i++
          else async.complete()
        }
      }
    }
  }
}
