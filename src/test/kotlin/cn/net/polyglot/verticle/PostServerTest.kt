package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.web.client.sendJsonObjectAwait
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.nio.file.Paths

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//按照名字升序执行代码
class PostServerTest {
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
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.publication.PublicationVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMServletVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.WebServerVerticle", option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  private val webClient = WebClient.create(vertx)

  @Test
  fun `test account registration`(context: TestContext){
    val async = context.async()
    GlobalScope.launch(vertx.dispatcher()) {
      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObjectAwait(JsonObject()
          .put(TYPE, USER)
          .put(SUBTYPE, REGISTER)
          .put(ID, "zxj001")
          .put(NICKNAME, "哲学家")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
          .put(PASSWORD2, "431fe828b9b8e8094235dee515562247")
          .put(VERSION, 0.1)
        )
      async.complete()
    }
  }

  @Test
  fun `test create post`(context: TestContext){
    val async = context.async()
    val json =
      JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, QUESTION).put(QUESTION, "小胖胖来了吗？").put(DESCRIPTION,"如题")
    GlobalScope.launch(vertx.dispatcher()) {
      val response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json)
      println(response.bodyAsJsonObject())
      context.assertTrue(response.bodyAsJsonObject().getBoolean(PUBLICATION))
      async.complete()
    }
  }

  @Test
  fun `test post history`(context: TestContext){
    val async = context.async()
    val json =
      JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, HISTORY)
    GlobalScope.launch(vertx.dispatcher()) {
      val response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json)
      println(response.bodyAsJsonObject())
      context.assertTrue(response.bodyAsJsonObject().getBoolean(PUBLICATION))
      async.complete()
    }
  }

  @Test
  fun `test history posted by zxj001 and retrieve the post published by zxj001`(context: TestContext){
    val async = context.async()
    val json =
      JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, HISTORY)
        .put(FROM, "zxj001")
    GlobalScope.launch(vertx.dispatcher()) {
      val response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json)
      println(response.bodyAsJsonObject())
      context.assertTrue(response.bodyAsJsonObject().getBoolean(PUBLICATION))

      json.put(DIR, response.bodyAsJsonObject().getJsonArray(HISTORY).getJsonObject(0).getString(DIR))
        .put(SUBTYPE, RETRIEVE)

      val response2 = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json)
      println(response2.bodyAsJsonObject())

      context.assertTrue(response2.bodyAsJsonObject().getBoolean(PUBLICATION))

      async.complete()
    }
  }
}
