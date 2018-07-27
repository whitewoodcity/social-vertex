package cn.net.polyglot.verticle

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.core.json.get
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
      //clean the directory
        if(vertx.fileSystem().existsBlocking(config.getString("dir")))
          vertx.fileSystem().deleteRecursiveBlocking(config.getString("dir"),true)

      val option = DeploymentOptions(config = config)
      vertx.deployVerticle(IMMessageVerticle::class.java.name, option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun testJsonFormat(context: TestContext) {
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("type"))
      context.assertNull(it.result().body().getValue("type"))
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject().put("type", "user")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("action"))
      context.assertNull(it.result().body().getValue("action"))
      async2.complete()
    }

    val async3 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject().put("type", 123)) {
      println(it.result().body())
      context.assertNull(it.result().body().getValue("type"))
      context.assertNull(it.result().body().getValue("action"))
      async3.complete()
    }
  }

  @Test
  fun testUserCreation(context: TestContext) {
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action", "register")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("register"))
      context.assertTrue(it.result().body().getBoolean("register"))
      async1.complete()
    }
  }

  @Test
  fun testUserCreationFail(context: TestContext) {
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action", "register")
        .put("user", "zxj2017")
        .put("crypto", "abcd")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("register"))
      context.assertTrue(!it.result().body().getBoolean("register"))
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action", "register")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("register"))
      context.assertTrue(!it.result().body().getBoolean("register"))
      async2.complete()
    }
  }

  @Test
  fun testUserLogin(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("login"))
      context.assertTrue(it.result().body().getBoolean("login"))
      async.complete()
    }
  }

  @Test
  fun testUserLoginFail(context: TestContext) {
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2018")
        .put("crypto", "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("login"))
      context.assertTrue(!it.result().body().getBoolean("login"))
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562126")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("login"))
      context.assertTrue(!it.result().body().getBoolean("login"))
      async2.complete()
    }
  }

  @Test
  fun testUserSearch(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "search")
        .put("action", "user")
        .put("keyword", "zxj2017")
        .put("version", 0.1)
    ) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("user"))
      context.assertNotNull(it.result().body().getJsonObject("user"))
      async.complete()
    }

    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "search")
        .put("action", "user")
        .put("keyword", "zxj2018")
        .put("version", 0.1)
    ) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey("user"))
      context.assertNull(it.result().body().getJsonObject("user"))
      async1.complete()
    }
  }

  @Test
  fun testUserFriendList(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put("type", "friend")
        .put("action", "list")
        .put("from", "zxj2017")
        .put("version", 0.1)) {
      println(it.result().body())
      async.complete()
    }
  }
}








