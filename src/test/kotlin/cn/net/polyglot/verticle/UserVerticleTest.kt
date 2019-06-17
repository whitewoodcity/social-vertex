package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.im.IMMessageVerticle
import cn.net.polyglot.verticle.search.SearchVerticle
import cn.net.polyglot.verticle.user.UserVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.deploymentOptionsOf
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
class UserVerticleTest {
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
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.user.UserVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.search.SearchVerticle", option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun `test user creation`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(UserVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562126")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562126")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTER))
      context.assertTrue(it.result().body().getBoolean(REGISTER))
      async.complete()
    }
  }

  @Test
  fun `test user creation fail`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(UserVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562126")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTER))
      context.assertFalse(it.result().body().getBoolean(REGISTER))
      async.complete()
    }
  }

  @Test
  fun `test user creation fail 2`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(UserVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTER))
      context.assertFalse(it.result().body().getBoolean(REGISTER))
      async.complete()
    }
  }

  @Test
  fun `test user update`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(UserVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, UPDATE)
        .put(ID, "zxj2017")
        .put(NICKNAME, "ZXJ")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(UPDATE))
      context.assertTrue(it.result().body().getBoolean(UPDATE))
      async.complete()
    }
  }

  @Test
  fun `test user valid`(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(UserVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, PROFILE)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(PROFILE))
      context.assertTrue(it.result().body().getBoolean(PROFILE))
      async.complete()
    }
  }

  @Test
  fun `test user search`(context: TestContext) {
    val async0 = context.async()
    vertx.eventBus().send<JsonObject>(SearchVerticle::class.java.name, "zxj2017") {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(SEARCH))
      context.assertTrue(it.result().body().getBoolean(SEARCH))
      async0.complete()
    }
  }

  @Test
  fun `test user search fail`(context: TestContext) {
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(SearchVerticle::class.java.name, "") {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(SEARCH))
      context.assertFalse(it.result().body().getBoolean(SEARCH))
      async1.complete()
    }
  }
}
