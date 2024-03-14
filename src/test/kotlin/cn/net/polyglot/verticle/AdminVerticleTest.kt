package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.admin.AdminVerticle
import cn.net.polyglot.verticle.search.SearchVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.deploymentOptionsOf
import org.awaitility.Awaitility
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
class AdminVerticleTest {
  companion object {
    private val config = JsonObject()
      .put(VERSION, 0.1)
      .put(DIR, Paths.get("").toAbsolutePath().toString() + File.separator + "social-vertex")
      .put(TCP_PORT, 7373)
      .put(HTTP_PORT, 7575)
      .put(HOST, "localhost")
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      if (vertx.fileSystem().existsBlocking(config.getString(DIR)))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString(DIR), true)

      val option = deploymentOptionsOf(config = config)
      val fut0 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.admin.AdminVerticle", option)

      Awaitility.await().until{
        fut0.isComplete
      }
      context.assertTrue(fut0.succeeded())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun `test admin creation`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE, ADMIN)
        .put(SUBTYPE,REGISTADMIN)
        .put(ID, "superadmin")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562126")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562126")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTADMIN))
      context.assertTrue(it.result().body().getBoolean(REGISTADMIN))
      async.complete()
    }
    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE, ADMIN)
        .put(SUBTYPE,REGISTADMIN)
        .put(ID, "admin2")
        .put(AUTHORITY, AUTHOR_READONLY)
        .put(PASSWORD, "431fe828b9b8e8094235dee515562126")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562126")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTADMIN))
      context.assertTrue(it.result().body().getBoolean(REGISTADMIN))
      async.complete()
    }
  }

  @Test
  fun `test admin creation fail`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE, ADMIN)
        .put(SUBTYPE,REGISTADMIN)
        .put(ID, "admin4")
        .put(AUTHORITY, AUTHOR_WRITEANDREAD)
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562117")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTADMIN))
      context.assertFalse(it.result().body().getBoolean(REGISTADMIN))
      async.complete()
    }
  }


    @Test
  fun `test admin creation 1`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE, ADMIN)
        .put(SUBTYPE,REGISTADMIN)
        .put(ID, "admin3")
        .put(AUTHORITY, AUTHOR_WRITEANDREAD)
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTADMIN))
      context.assertTrue(it.result().body().getBoolean(REGISTADMIN))
      async.complete()
    }
  }

  @Test
  fun `test admin login`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE,ADMIN)
        .put(SUBTYPE,LOGINADMIN)
        .put(ID, "admin3")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(LOGINADMIN))
      context.assertTrue(it.result().body().getBoolean(LOGINADMIN))
      async.complete()
    }
  }
  @Test
  fun `test admin login 1`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE,ADMIN)
        .put(SUBTYPE,LOGINADMIN)
        .put(ID, "admin3")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562027")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(LOGINADMIN))
      context.assertFalse(it.result().body().getBoolean(LOGINADMIN))
      async.complete()
    }
  }
  @Test
  fun `test admin loginout`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE,ADMIN)
        .put(SUBTYPE,LOGOUTADMIN)
        .put(ID, "admin3")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(LOGOUTADMIN))
      context.assertTrue(it.result().body().getBoolean(LOGOUTADMIN))
      async.complete()
    }
  }
  @Test
  fun `test admin loginout 2`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE,ADMIN)
        .put(SUBTYPE,LOGOUTADMIN)
        .put(ID, "admin3")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(LOGOUTADMIN))
      context.assertFalse(it.result().body().getBoolean(LOGOUTADMIN))
      async.complete()
    }
  }
  @Test
  fun `test admin udpate`(context: TestContext) {
    val async = context.async()

    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE, ADMIN)
        .put(ID,"admin2")
        .put(AUTHORITY, AUTHOR_READONLY)
        .put(SUBTYPE, UPDATEADMIN)
        .put(PASSWORD, "431fe828b9b8e8094235dee515562126")
        .put(PASSWORD2, "431fe828b9b8e8094235dee51556212a")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(UPDATEADMIN))
      context.assertTrue(it.result().body().getBoolean(UPDATEADMIN))
      async.complete()
    }
  }
  @Test
  fun `test admin udpate 2`(context: TestContext) {
    val async = context.async()

    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE, ADMIN)
        .put(ID,"admin2")
        .put(AUTHORITY, AUTHOR_READONLY)
        .put(SUBTYPE, UPDATEADMIN)
        .put(PASSWORD, "431fe828b9b8e8094235dee515562121")
        .put(PASSWORD2, "431fe828b9b8e8094235dee51556212a")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(UPDATEADMIN))
      context.assertFalse(it.result().body().getBoolean(UPDATEADMIN))
      async.complete()
    }
  }
  @Test
  fun `test admin unregist`(context: TestContext) {
    val async = context.async()

    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE, ADMIN)
        .put(ID,"admin2")
        .put(SUBTYPE, UNREGISTADMIN)
        .put(PASSWORD, "431fe828b9b8e8094235dee51556212a")
        .put(PASSWORD2, "431fe828b9b8e8094235dee51556212a")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(UNREGISTADMIN))
      context.assertTrue(it.result().body().getBoolean(UNREGISTADMIN))
      async.complete()
    }
  }
  @Test
  fun `test admin unregist 2`(context: TestContext) {
    val async = context.async()

    vertx.eventBus().request<JsonObject>(AdminVerticle::class.java.name,
      JsonObject()
        .put(TYPE, ADMIN)
        .put(ID,"admin2")
        .put(SUBTYPE, UNREGISTADMIN)
        .put(PASSWORD, "431fe828b9b8e8094235dee51556212a")
        .put(PASSWORD2, "431fe828b9b8e8094235dee51556212a")){
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(UNREGISTADMIN))
      context.assertFalse(it.result().body().getBoolean(UNREGISTADMIN))
      async.complete()
    }
  }

}
