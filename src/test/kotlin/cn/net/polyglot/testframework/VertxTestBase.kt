package cn.net.polyglot.testframework

import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

/**
 * @author zxj5470
 * @date 2018/7/9
 */
/**
 * @usage
 * @see [cn.net.polyglot.SecondVerticleTest2]
 * @property verticle Class<*>
 * @property vertx Vertx
 * @property currentPort Int
 */
@RunWith(VertxUnitRunner::class)
abstract class VertxTestBase {
  lateinit var verticle: Class<out Verticle>
  inline fun <reified T : Verticle> setVerticle() {
    verticle = T::class.java
  }

  lateinit var vertx: Vertx
  abstract var currentPort: Int

  fun bootstrap(context: TestContext) {
    vertx = Vertx.vertx()
    val currentOptions = configPort(currentPort)
    vertx.deployVerticle(verticle.name, currentOptions, context.asyncAssertSuccess())
  }

  @Before
  abstract fun setUp(context: TestContext)

  @After
  fun tearDown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  abstract fun testApplication(context: TestContext)
}
