package cn.net.polyglot.verticle

import cn.net.polyglot.testframework.configPort
import cn.net.polyglot.utils.text
import io.vertx.core.Vertx
import io.vertx.core.net.NetClient
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author zxj5470
 * @date 2018/7/9
 */

@RunWith(VertxUnitRunner::class)
class IMTcpServerVerticleTest {
  private lateinit var vertx: Vertx
  private lateinit var client: NetClient
  private val port = 8081

  @Before
  fun before(context: TestContext) {
    vertx = Vertx.vertx()
    vertx.deployVerticle(IMMessageVerticle::class.java.name, context.asyncAssertSuccess())
    client = vertx.createNetClient()
    val opt = configPort(port)
    vertx.deployVerticle(IMTcpServerVerticle::class.java.name, opt,context.asyncAssertSuccess())
  }

  @Test
  fun testApplication(context: TestContext) {
    vertx.deployVerticle(IMMessageVerticle::class.java.name)

    val async = context.async()
    vertx.createNetClient().connect(port, "localhost") {
      if (it.succeeded()) {
        val socket = it.result()

        socket.handler {
          println(it.text())
        }

        var i = 0
        vertx.setPeriodic(2333L) {
          socket.write("""{"type":"search","id":"zxj5470"}""")
          if (i < 3) i++
          else async.complete()
        }
      }
    }
  }

  @After
  fun after(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }
}
