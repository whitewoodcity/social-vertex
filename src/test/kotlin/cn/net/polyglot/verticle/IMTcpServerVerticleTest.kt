package cn.net.polyglot.verticle

import cn.net.polyglot.testframework.VertxTestBase
import io.vertx.ext.unit.TestContext
import org.junit.Test

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMTcpServerVerticleTest : VertxTestBase() {
  override var currentPort = 8085

  init {
    setVerticle<IMTcpServerVerticle>()
  }

  @Test
  override fun testApplication(context: TestContext) {
    val async = context.async()
    vertx.createNetClient().connect(currentPort, "localhost") {
      if (it.succeeded()) {
        val socket = it.result()

        socket.handler {
          println(it.bytes.let { String(it) })
        }

        var i = 0
        vertx.setPeriodic(2333L) {
          socket.write("""{"type":"search","id":"${socket.writeHandlerID()}","timestamp":"${System.currentTimeMillis()}"}""")
          if (i < 4) i++
          else async.complete()
        }
      }
    }
  }
}
