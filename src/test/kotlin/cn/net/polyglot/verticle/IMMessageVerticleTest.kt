package cn.net.polyglot.verticle

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.DeploymentOptions
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Paths

@RunWith(VertxUnitRunner::class)
class IMMessageVerticleTest {

  companion object {
    private val config = JsonObject()
      .put("version",0.1)
      .put("dir", Paths.get("").toAbsolutePath().toString() + File.separator + "social-vertex")
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      val option = DeploymentOptions(config = config)
      vertx.deployVerticle(IMMessageVerticle::class.java.name, option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      //clean the directory
      vertx.fileSystem().deleteRecursiveBlocking(config.getString("dir"),true)

      vertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun testUserCreationFail(context: TestContext){
    val async = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action","register")
        .put("user", "zxj2017")
        .put("crypto","abcd")){
      context.assertTrue(it.result().body().containsKey("register"))
      context.assertTrue(!it.result().body().getBoolean("register"))
      async.complete()
    }
  }

  @Test
  fun testUserCreation(context: TestContext){
    val async = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action","register")
        .put("user", "zxj2017")
        .put("crypto","431fe828b9b8e8094235dee515562127")){
      context.assertTrue(it.result().body().containsKey("register"))
      context.assertTrue(it.result().body().getBoolean("register"))
      async.complete()
    }
  }
}
