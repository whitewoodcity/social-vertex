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
import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.nio.file.Paths

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class IMHttpServerVerticleTest {

  companion object {
    private val config = JsonObject()
      .put("dir", Paths.get("").toAbsolutePath().toString() + File.separator + "social-vertex")
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      //clean the directory
      if (vertx.fileSystem().existsBlocking(config.getString("dir")))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString("dir"), true)

      val option = DeploymentOptions(config = config)
      vertx.deployVerticle(IMMessageVerticle::class.java.name, option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }
}
