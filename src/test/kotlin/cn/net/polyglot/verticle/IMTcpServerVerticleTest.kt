package cn.net.polyglot.verticle

import cn.net.polyglot.testframework.VertxTestBase
import cn.net.polyglot.utils.text
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
    vertx.deployVerticle(IMMessageVerticle::class.java.name)

    val async = context.async()
    vertx.createNetClient().connect(currentPort, "localhost") {
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
}
