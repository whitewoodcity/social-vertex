package cn.net.polyglot

import cn.net.polyglot.testframework.configPort
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


/**
 * @author zxj5470
 * @date 2018/7/8
 */
@RunWith(VertxUnitRunner::class)
class CoroutineVerticleTest {
  private lateinit var vertx: Vertx
  private val currentPort = 8082

  @Before
  fun setUp(context: TestContext) {
    vertx = Vertx.vertx()
    val currentOptions = configPort(currentPort)
    vertx.deployVerticle(CoroutineVerticle::class.java.name, currentOptions, context.asyncAssertSuccess())
  }

  @After
  fun tearDown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  @Test
  fun testApplication(context: TestContext) {
    val async = context.async()
    val allContent = StringBuilder()
    vertx.createHttpClient().getNow(currentPort, "localhost", "/") { response ->

      response.handler { body ->
        val ret = body.bytes.toKString()
        allContent.append(ret)
      }

      response.endHandler {
        println(allContent)
        context.assertTrue(allContent.contains("ConfigLoaderKt"))
        async.complete()
      }
    }
  }
}
