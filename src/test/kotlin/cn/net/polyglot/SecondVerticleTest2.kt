package cn.net.polyglot

import cn.net.polyglot.testframework.VertxTestBase
import cn.net.polyglot.testframework.shouldBe
import io.vertx.ext.unit.TestContext
import org.junit.Before
import org.junit.Test


/**
 * @author zxj5470
 * @date 2018/7/8
 */
class SecondVerticleTest2 : VertxTestBase() {
  override var currentPort = 8088

  init {
  	setVerticle<SecondVerticle>()
  }

  @Before
  override fun setUp(context: TestContext) = super.bootstrap(context)

  @Test
  override fun testApplication(context: TestContext) {
    val async = context.async()
    vertx.createHttpClient().getNow(currentPort, "localhost", "/") { response ->
      response.handler { body ->
        println("content:")
        println(body.toString())

        body.toString().contains("Hello") shouldBe true

        async.complete()
      }
    }
  }
}
