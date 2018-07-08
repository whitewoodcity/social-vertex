package cn.net.polyglot

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
class SecondVerticleTest {
  private lateinit var vertx: Vertx

  @Before
  fun setUp(context: TestContext) {
    vertx = Vertx.vertx()
    vertx.deployVerticle(SecondVerticle::class.java.name, context.asyncAssertSuccess())
  }

  @After
  fun tearDown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  @Test
  fun testApplication(context: TestContext) {
    val async = context.async()
    vertx.createHttpClient().getNow(8080, "localhost", "/") { response ->
      response.handler { body ->
        println("content:")
        println(body.toString())
        context.assertTrue(body.toString().contains("Hello"))
        async.complete()
      }
    }
  }
}
