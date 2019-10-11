package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.jsonObjectOf
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
      .put(VERSION, 0.4)
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
    val async = context.async(2)
    val json0 =
      JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, QUESTION).put(TITLE, "小胖胖来了吗？")
        .put(CONTENT,"新浪声明：新浪网登载此文出于传递更多信息之目的，并不意味着赞同其观点或证实其描述。文章内容仅供参考，不构成投资建议。投资者据此操作，风险自担。")

    val json1 =
      JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, QUESTION).put(TITLE, "qingming来了吗？")
        .put(CONTENT,"新浪声明：新浪网登载此文出于传递更多信息之目的，并不意味着赞同其观点或证实其描述。文章内容仅供参考，不构成投资建议。投资者据此操作，风险自担。新浪声明：新浪网登载此文出于传递更多信息之目的，并不意味着赞同其观点或证实其描述。文章内容仅供参考，不构成投资建议。投资者据此操作，风险自担。新浪声明：新浪网登载此文出于传递更多信息之目的，并不意味着赞同其观点或证实其描述。文章内容仅供参考，不构成投资建议。投资者据此操作，风险自担。新浪声明：新浪网登载此文出于传递更多信息之目的，并不意味着赞同其观点或证实其描述。文章内容仅供参考，不构成投资建议。投资者据此操作，风险自担。新浪声明：新浪网登载此文出于传递更多信息之目的，并不意味着赞同其观点或证实其描述。文章内容仅供参考，不构成投资建议。投资者据此操作，风险自担。新浪声明：新浪网登载此文出于传递更多信息之目的，并不意味着赞同其观点或证实其描述。文章内容仅供参考，不构成投资建议。投资者据此操作，风险自担。")

    GlobalScope.launch(vertx.dispatcher()) {
      val response0 = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json0)
      println(response0.bodyAsJsonObject())
      context.assertTrue(response0.bodyAsJsonObject().getBoolean(PUBLICATION))
      async.countDown()

      val response1 = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json1)
      println(response1.bodyAsJsonObject())
      context.assertTrue(response1.bodyAsJsonObject().getBoolean(PUBLICATION))
      async.countDown()
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
  fun `test related function_like_dislike_collect`(context: TestContext){
    val async = context.async()
    val json = jsonObjectOf().put(ID, "zxj001")
      .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
      .put(TYPE, PUBLICATION)
      .put(SUBTYPE, HISTORY)
    GlobalScope.launch(vertx.dispatcher()) {
      val response = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObjectAwait(json)
      val body = response.bodyAsJsonObject()
      context.assertTrue(body.getBoolean(PUBLICATION))
      context.assertTrue(body.getJsonArray(HISTORY).size() > 0)
      //get one article : check the liked dislike collect
      val oneArticle = body.getJsonArray(HISTORY).getJsonObject(0)
      val dir = oneArticle.getString(DIR)
      context.assertTrue(oneArticle.getInteger(LIKE)==0)
      context.assertTrue(oneArticle.getInteger(DISLIKE)==0)
      context.assertTrue(oneArticle.getInteger(COLLECT)==0)

      //like an article
      json.put(SUBTYPE,LIKE)
      json.put(DIR,dir)
      val likeResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json)
      val likeRespBody = likeResponse.bodyAsJsonObject()
      context.assertTrue(likeRespBody.getBoolean(PUBLICATION))

      //dislike an article
      json.put(SUBTYPE, DISLIKE)
      json.put(DIR,dir)
      val disLikeResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json)
      val disLikeRespBody = disLikeResponse.bodyAsJsonObject()
      context.assertTrue(disLikeRespBody.getBoolean(PUBLICATION))

      //collect an article
      json.put(SUBTYPE, COLLECT)
      json.put(DIR,dir)
      val collectResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json)
      val collectRespBody = collectResponse.bodyAsJsonObject()
      context.assertTrue(collectRespBody.getBoolean(PUBLICATION))
      //--------------------------------
      json.put(SUBTYPE, RETRIEVE)
      json.put(DIR,dir)
      val retrieveResponse = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObjectAwait(json)
      val retrieveRespBody = retrieveResponse.bodyAsJsonObject()
      context.assertTrue(retrieveRespBody.getInteger(LIKE)==1)
      context.assertTrue(retrieveRespBody.getInteger(DISLIKE)==1)
      context.assertTrue(retrieveRespBody.getInteger(COLLECT)==1)

      async.complete()
    }

  }

  @Test
  fun `test update article`(context: TestContext){
    val async = context.async()
    val json = JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, HISTORY)

    GlobalScope.launch(vertx.dispatcher()) {
      val response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json)
      val body = response.bodyAsJsonObject()
      context.assertTrue(body.getBoolean(PUBLICATION))
      context.assertTrue(body.getJsonArray(HISTORY).size() > 0)

      val oneArticle = body.getJsonArray(HISTORY).getJsonObject(0)
      val originalTitle = oneArticle.getString(TITLE)
      val originalContent = oneArticle.getString(CONTENT)
      val dir = oneArticle.getString(DIR)
      println("originalTitle:$originalTitle, originalContent:$originalContent, dir:$dir")

      oneArticle.put(TITLE,"new title hahaha")
      oneArticle.put(CONTENT,"new Content new Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Content" +
        "new Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Content" +
        "new Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Contentnew Content")
      oneArticle.put(SUBTYPE, UPDATE).put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")

      val updateResps = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(oneArticle)
      context.assertTrue(updateResps.bodyAsJsonObject().getBoolean(PUBLICATION))

      val json0 = JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, RETRIEVE).put(DIR,dir)

      val retrieveResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObjectAwait(json0)
      val newAritcle = retrieveResponse.bodyAsJsonObject()
      val newTitle = newAritcle.getString(TITLE)
      val newContent = newAritcle.getString(CONTENT)
      println("new article: $newAritcle")
      context.assertNotEquals(originalTitle,newTitle)
      context.assertNotEquals(originalContent,newContent)

      context.assertEquals(newAritcle.getString(TITLE),oneArticle.getString(TITLE))
      context.assertEquals(newAritcle.getString(CONTENT),oneArticle.getString(CONTENT))
      async.complete()
    }
  }
}
