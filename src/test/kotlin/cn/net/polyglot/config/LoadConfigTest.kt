package cn.net.polyglot.config

import cn.net.polyglot.testframework.shouldBe
import io.vertx.config.ConfigRetriever
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
class LoadConfigTest {
  private lateinit var vertx: Vertx
  private lateinit var retriever: ConfigRetriever

  @Before
  fun setUp(context: TestContext) {
    vertx = Vertx.vertx()
    retriever = ConfigRetriever.create(vertx, options)
  }

  @After
  fun tearDown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  @Test
  fun testDefault() {
    defaultJsonObject.getInteger("port") shouldBe DEFAULT_PORT
  }

  @Test
  fun testApplication(context: TestContext) {
    retriever.getConfig {
      if (it.failed()) {
        System.err.println("failed")
      } else {
        println(it.result().getInteger("port"))
      }
    }
  }
}
