package cn.net.polyglot.verticle

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.DeploymentOptions
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.nio.file.Paths

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class IMMessageVerticleTest {

  companion object {
    private val config = JsonObject()
      .put("dir", Paths.get("").toAbsolutePath().toString() + File.separator + "social-vertex")
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      if(vertx.fileSystem().existsBlocking(config.getString("dir")))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString("dir"),true)

      val option = DeploymentOptions(config = config)
      vertx.deployVerticle(IMMessageVerticle::class.java.name, option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      //clean the directory
      if(vertx.fileSystem().existsBlocking(config.getString("dir")))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString("dir"),true)

      vertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun testJsonFormat(context: TestContext){
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("type"))
      context.assertTrue(it.result().body().getValue("type")==null)
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject().put("type","user")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("action"))
      context.assertTrue(it.result().body().getValue("action")==null)
      async2.complete()
    }

    val async3 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject().put("type",123)){
      println(it.result().body())
      context.assertTrue(it.result().body().getValue("type")==null)
      context.assertTrue(it.result().body().getValue("action")==null)
      async3.complete()
    }
  }

  @Test
  fun testUserCreation(context: TestContext){
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action","register")
        .put("user", "zxj2017")
        .put("crypto","431fe828b9b8e8094235dee515562127")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("register"))
      context.assertTrue(it.result().body().getBoolean("register"))
      async1.complete()
    }
  }

  @Test
  fun testUserCreationFail(context: TestContext){
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action","register")
        .put("user", "zxj2017")
        .put("crypto","abcd")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("register"))
      context.assertTrue(!it.result().body().getBoolean("register"))
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action","register")
        .put("user", "zxj2017")
        .put("crypto","431fe828b9b8e8094235dee515562127")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("register"))
      context.assertTrue(!it.result().body().getBoolean("register"))
      async2.complete()
    }
  }

  @Test
  fun testUserLogin(context: TestContext){
    val async = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action","login")
        .put("user", "zxj2017")
        .put("crypto","431fe828b9b8e8094235dee515562127")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("login"))
      context.assertTrue(it.result().body().getBoolean("login"))
      async.complete()
    }
  }

  @Test
  fun testUserLoginFail(context: TestContext){
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action","login")
        .put("user", "zxj2018")
        .put("crypto","431fe828b9b8e8094235dee515562127")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("login"))
      context.assertTrue(!it.result().body().getBoolean("login"))
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action","login")
        .put("user", "zxj2017")
        .put("crypto","431fe828b9b8e8094235dee515562126")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("login"))
      context.assertTrue(!it.result().body().getBoolean("login"))
      async2.complete()
    }
  }
  @Test
 fun testUserSearch(context: TestContext){
    val async3 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
      .put("type","search")
      .put("action","user")
      .put("keyword","zxj2017")
      .put("version",".1")
    ){
      print(it.result().body())
      context.assertTrue(it.result().body().containsKey("user"))
      context.assertNotNull(it.result().body().getJsonObject("user"))
      async3.complete()
    }
  }
}
