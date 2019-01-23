/**
MIT License

Copyright (c) 2018 White Wood City

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import cn.net.polyglot.verticle.im.IMMessageVerticle
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
      .put(DIR, Paths.get("").toAbsolutePath().toString() + File.separator + "social-vertex")
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      //clean the directory
        if(vertx.fileSystem().existsBlocking(config.getString(DIR)))
          vertx.fileSystem().deleteRecursiveBlocking(config.getString(DIR),true)

      val option = DeploymentOptions(config = config)
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMMessageVerticle", option, context.asyncAssertSuccess())
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
      context.assertTrue(it.result().body().containsKey(TYPE))
      context.assertNull(it.result().body().getValue(TYPE))
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject().put(TYPE, USER)) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(SUBTYPE))
      context.assertNull(it.result().body().getValue(SUBTYPE))
      async2.complete()
    }

    val async3 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject().put(TYPE, 123)) {
      println(it.result().body())
      context.assertNull(it.result().body().getValue(TYPE))
      context.assertNull(it.result().body().getValue(SUBTYPE))
      async3.complete()
    }
  }

  @Test
  fun testUserCreation(context: TestContext) {
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTER))
      context.assertTrue(it.result().body().getBoolean(REGISTER))
      async1.complete()
    }
  }

  @Test
  fun testUserCreationFail(context: TestContext) {
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zxj2017")
        .put(PASSWORD, "abcd")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTER))
      context.assertTrue(!it.result().body().getBoolean(REGISTER))
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(REGISTER))
      context.assertTrue(!it.result().body().getBoolean(REGISTER))
      async2.complete()
    }
  }

  @Test
  fun testUserLogin(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(LOGIN))
      context.assertTrue(it.result().body().getBoolean(LOGIN))
      async.complete()
    }
  }

  @Test
  fun testUserLoginFail(context: TestContext) {
    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "zxj2018")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562127")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(LOGIN))
      context.assertTrue(!it.result().body().getBoolean(LOGIN))
      async1.complete()
    }

    val async2 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562126")) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(LOGIN))
      context.assertTrue(!it.result().body().getBoolean(LOGIN))
      async2.complete()
    }
  }

  @Test
  fun testUserSearch(context: TestContext) {
    val async = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put(TYPE, SEARCH)
        .put(SUBTYPE, INFO)
        .put(KEYWORD, "zxj2017")
        .put(VERSION, 0.1)
    ) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(USER))
      context.assertNotNull(it.result().body().getJsonObject(USER))
      async.complete()
    }

    val async1 = context.async()
    vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,
      JsonObject()
        .put(TYPE, SEARCH)
        .put(SUBTYPE, INFO)
        .put(KEYWORD, "zxj2018")
        .put(VERSION, 0.1)
    ) {
      println(it.result().body())
      context.assertTrue(it.result().body().containsKey(USER))
      context.assertNull(it.result().body().getJsonObject(USER))
      async1.complete()
    }
  }
}
