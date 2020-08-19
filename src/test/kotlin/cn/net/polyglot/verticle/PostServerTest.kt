package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.awaitility.Awaitility
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

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
    val vOptions = VertxOptions()
      .setWarningExceptionTime(5).setWarningExceptionTimeUnit(TimeUnit.MINUTES)
      .setBlockedThreadCheckInterval(5).setBlockedThreadCheckIntervalUnit(TimeUnit.MINUTES)
    private val vertx = Vertx.vertx(vOptions)

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      if (vertx.fileSystem().existsBlocking(config.getString(DIR)))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString(DIR), true)

      val option = deploymentOptionsOf(config = config)
      val fut0 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.user.UserVerticle", option)
      val fut1 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.publication.PublicationVerticle", option)
      val fut2 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMServletVerticle", option)
      val fut3 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.WebServerVerticle", option)

      Awaitility.await().until{
        fut0.isComplete &&
          fut1.isComplete &&
          fut2.isComplete &&
          fut3.isComplete
      }

      context.assertTrue(fut0.succeeded())
      context.assertTrue(fut1.succeeded())
      context.assertTrue(fut2.succeeded())
      context.assertTrue(fut3.succeeded())
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
        .sendJsonObject(JsonObject()
          .put(TYPE, USER)
          .put(SUBTYPE, REGISTER)
          .put(ID, "zxj001")
          .put(NICKNAME, "哲学家")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
          .put(PASSWORD2, "431fe828b9b8e8094235dee515562247")
          .put(VERSION, 0.1)
        ).await()
      // create another user
      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObject(JsonObject()
          .put(TYPE, USER)
          .put(SUBTYPE, REGISTER)
          .put(ID, "zxj002")
          .put(NICKNAME, "第二个哲学家")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
          .put(PASSWORD2, "431fe828b9b8e8094235dee515562247")
          .put(VERSION, 0.1)
        ).await()
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
      val response0 = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json0).await()
      println(response0.bodyAsJsonObject())
      context.assertTrue(response0.bodyAsJsonObject().getBoolean(PUBLICATION))
      async.countDown()

      val response1 = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json1).await()
      println(response1.bodyAsJsonObject())
      context.assertTrue(response1.bodyAsJsonObject().getBoolean(PUBLICATION))
      async.countDown()
    }
  }

  @Test
  fun `test getting user brief`(context: TestContext){
    val async = context.async()
    val json = JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
      .put(TYPE, PUBLICATION).put(SUBTYPE, USER_BRIEF)

    GlobalScope.launch(vertx.dispatcher()) {
      val failedResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      context.assertFalse(failedResponse.bodyAsJsonObject().getBoolean(PUBLICATION))

      json.put(UID,"zxj001")
      val resp1 = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      context.assertTrue(resp1.bodyAsJsonObject().getBoolean(PUBLICATION))
      context.assertNotNull(resp1.bodyAsJsonObject().getJsonObject(INFO))
      println(resp1.bodyAsJsonObject().getJsonObject(INFO))

      json.put(UID,"zxj002")
      val resp2 = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      context.assertTrue(resp2.bodyAsJsonObject().getBoolean(PUBLICATION))
      context.assertNotNull(resp2.bodyAsJsonObject().getJsonObject(INFO))
      println(resp2.bodyAsJsonObject().getJsonObject(INFO))

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
      val response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      println(response.bodyAsJsonObject())
      context.assertTrue(response.bodyAsJsonObject().getBoolean(PUBLICATION))

      json.put(DIR, response.bodyAsJsonObject().getJsonArray(HISTORY).getJsonObject(0).getString(DIR))
        .put(SUBTYPE, RETRIEVE)

      val response2 = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
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
      val response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      println(response.bodyAsJsonObject())
      context.assertTrue(response.bodyAsJsonObject().getBoolean(PUBLICATION))
      async.complete()
    }
  }

  @Test
  fun `test related function_comment`(context: TestContext){
    val async = context.async()
    GlobalScope.launch(vertx.dispatcher()) {
      val json = jsonObjectOf().put(ID, "zxj001")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION)
        .put(SUBTYPE, HISTORY)
      val response = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObject(json).await()
      val body = response.bodyAsJsonObject()
      context.assertTrue(body.getBoolean(PUBLICATION))
      context.assertTrue(body.getJsonArray(HISTORY).size() > 0)
      //get one article : prepare for comment
      //--- comment an article
      val oneArticle = body.getJsonArray(HISTORY).getJsonObject(0)
      val dir = oneArticle.getString(DIR)
      json.put(SUBTYPE, COMMENT)
        .put(DIR, dir)
        .put(CONTENT, "this is a comment: this article is very good and useful for me!")
      val comment1Response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val comment1Respbody = comment1Response.bodyAsJsonObject()
      println(comment1Respbody)
      context.assertTrue(comment1Respbody.getBoolean(PUBLICATION))

      //---comment list
      json.put(SUBTYPE, COMMENT_LIST)
        .remove(CONTENT)
      val commentListResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val commentListRespBody = commentListResponse.bodyAsJsonObject()
      println(commentListRespBody)
      context.assertTrue(commentListRespBody.getBoolean(PUBLICATION))
      context.assertTrue(commentListRespBody.getJsonArray(INFO).size() > 0)

      val aComment = commentListRespBody.getJsonArray(INFO).getJsonObject(0)
      val commentDir = aComment.getString(DIR)
      json.put(SUBTYPE, COMMENT).put(DIR,commentDir).put(CONTENT,"this is a comment of a comment")
      //--- comment a comment
      val comment2Response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val comment2RespBody = comment2Response.bodyAsJsonObject()
      context.assertTrue(comment2RespBody.getBoolean(PUBLICATION))

      //--- like the comment
      json.put(SUBTYPE, LIKE).put(DIR,commentDir)
      val likeCommentResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val likeCommentBody = likeCommentResponse.bodyAsJsonObject()
      context.assertTrue(likeCommentBody.getBoolean(PUBLICATION))

      //--- dislike the comment
      json.put(SUBTYPE, DISLIKE).put(DIR,commentDir)
      val dislikeCommentResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val dislikeCommentBody = dislikeCommentResponse.bodyAsJsonObject()
      context.assertTrue(dislikeCommentBody.getBoolean(PUBLICATION))

      //-- check like or dislike works
      json.put(SUBTYPE, COMMENT_LIST).put(DIR,dir)
        .remove(CONTENT)
      val checkCLResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val checkCLbody = checkCLResponse.bodyAsJsonObject()
      context.assertTrue(checkCLbody.getBoolean(PUBLICATION))
      val checkComments = checkCLbody.getJsonArray(INFO)
      context.assertTrue(checkComments.size() > 0)
      val cComment = checkComments.getJsonObject(0)
      context.assertTrue(cComment.getInteger(LIKE) > 0)
      context.assertTrue(cComment.getInteger(DISLIKE) > 0)
      context.assertTrue(cComment.getBoolean(LIKED))
      context.assertTrue(cComment.getBoolean(DISLIKED))


      //get the comment list of the comment
      json.remove(CONTENT)
      json.put(SUBTYPE, COMMENT_LIST).put(DIR,commentDir)
      val commentList2Response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val commentList2RespBoody = commentList2Response.bodyAsJsonObject()
      println("comments of a comment: $commentList2RespBoody")
      context.assertTrue(commentList2RespBoody.getBoolean(PUBLICATION))
      context.assertTrue(commentList2RespBoody.getJsonArray(INFO).size()>0)
      //----------------------------------
      val commentOfComent = commentList2RespBoody.getJsonArray(INFO).getJsonObject(0)

      //like the comment of comment
      val cocDir = commentOfComent.getString(DIR)
      json.put(SUBTYPE, LIKE).put(DIR,cocDir)
      val cocResposne = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val cocBody = cocResposne.bodyAsJsonObject()
      context.assertTrue(cocBody.getBoolean(PUBLICATION))

      //dislike the comment of comment
      json.put(SUBTYPE, DISLIKE).put(DIR,cocDir)
      val dislikeCocResposne = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val dislikeCocBody = dislikeCocResposne.bodyAsJsonObject()
      context.assertTrue(dislikeCocBody.getBoolean(PUBLICATION))

      async.complete()
    }
  }

  @Test
  fun `test related function_comment num`(context: TestContext){
    val async = context.async()
    GlobalScope.launch(vertx.dispatcher()) {
      val json = jsonObjectOf().put(ID, "zxj001")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION)
        .put(SUBTYPE, HISTORY)
      val response = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObject(json).await()
      val body = response.bodyAsJsonObject()
      context.assertTrue(body.getBoolean(PUBLICATION))
      context.assertTrue(body.getJsonArray(HISTORY).size() > 0)
      val oneArticle = body.getJsonArray(HISTORY).getJsonObject(0)
      val commentedNum = oneArticle.getInteger(COMMENTED_NUM)
      context.assertTrue(commentedNum > 0)
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
      val response = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObject(json).await()
      val body = response.bodyAsJsonObject()
      context.assertTrue(body.getBoolean(PUBLICATION))
      context.assertTrue(body.getJsonArray(HISTORY).size() > 0)
      //get one article : check the liked dislike collect
      val oneArticle = body.getJsonArray(HISTORY).getJsonObject(0)
      val dir = oneArticle.getString(DIR)
      context.assertTrue(oneArticle.getInteger(LIKE)==0)
      context.assertTrue(oneArticle.getInteger(DISLIKE)==0)
      context.assertTrue(oneArticle.getInteger(COLLECT)==0)
      context.assertFalse(oneArticle.getBoolean(LIKED))
      context.assertFalse(oneArticle.getBoolean(DISLIKED))
      context.assertFalse(oneArticle.getBoolean(COLLECTED))

      //like an article
      json.put(SUBTYPE,LIKE)
      json.put(DIR,dir)
      val likeResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val likeRespBody = likeResponse.bodyAsJsonObject()
      context.assertTrue(likeRespBody.getBoolean(PUBLICATION))

      //dislike an article
      json.put(SUBTYPE, DISLIKE)
      json.put(DIR,dir)
      val disLikeResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val disLikeRespBody = disLikeResponse.bodyAsJsonObject()
      context.assertTrue(disLikeRespBody.getBoolean(PUBLICATION))

      //collect an article
      json.put(SUBTYPE, COLLECT)
      json.put(DIR,dir)
      val collectResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val collectRespBody = collectResponse.bodyAsJsonObject()
      context.assertTrue(collectRespBody.getBoolean(PUBLICATION))
      //--------------------------------
      json.put(SUBTYPE, RETRIEVE)
      json.put(DIR,dir)
      val retrieveResponse = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObject(json).await()
      val retrieveRespBody = retrieveResponse.bodyAsJsonObject()
      context.assertTrue(retrieveRespBody.getInteger(LIKE)==1)
      context.assertTrue(retrieveRespBody.getInteger(DISLIKE)==1)
      context.assertTrue(retrieveRespBody.getInteger(COLLECT)==1)
      context.assertTrue(retrieveRespBody.getBoolean(LIKED))
      context.assertTrue(retrieveRespBody.getBoolean(DISLIKED))
      context.assertTrue(retrieveRespBody.getBoolean(COLLECTED))

      //------another user just check boolean values(liked disliked collected)--------
      val json2 = jsonObjectOf().put(ID, "zxj002")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION)
        .put(SUBTYPE, RETRIEVE)
        .put(DIR,dir)
      val response2 = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObject(json2).await()
      val body2 = response2.bodyAsJsonObject()
      context.assertTrue(body2.getInteger(LIKE)==1)
      context.assertTrue(body2.getInteger(DISLIKE)==1)
      context.assertTrue(body2.getInteger(COLLECT)==1)
      context.assertFalse(body2.getBoolean(LIKED))
      context.assertFalse(body2.getBoolean(DISLIKED))
      context.assertFalse(body2.getBoolean(COLLECTED))

      //--collect list
      json.put(SUBTYPE, COLLECT_LIST)
      json.remove(DIR)
      val collectListResponse = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObject(json).await()
      val collectListRespBody = collectListResponse.bodyAsJsonObject()
      context.assertTrue(collectListRespBody.getBoolean(PUBLICATION))
      context.assertTrue(collectListRespBody.getJsonArray(INFO).size() > 0)


      //----------undo the like/dislike/collect-----------------------------------------
      //cancle like an article
      json.put(SUBTYPE,LIKE)
      json.put(DIR,dir)
      val unlikeResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val unlikeRespBody = unlikeResponse.bodyAsJsonObject()
      context.assertTrue(unlikeRespBody.getBoolean(PUBLICATION))

      //cancle dislike an article
      json.put(SUBTYPE, DISLIKE)
      json.put(DIR,dir)
      val undisLikeResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
      val undisLikeRespBody = undisLikeResponse.bodyAsJsonObject()
      context.assertTrue(undisLikeRespBody.getBoolean(PUBLICATION))

      //cancle collect an article
//      json.put(SUBTYPE, COLLECT)
//      json.put(DIR,dir)
//      val uncollectResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
//      val uncollectRespBody = uncollectResponse.bodyAsJsonObject()
//      context.assertTrue(uncollectRespBody.getBoolean(PUBLICATION))

      json.put(SUBTYPE, RETRIEVE)
      json.put(DIR,dir)
      val retrieveResponse2 = webClient.put(config.getInteger(HTTP_PORT),"localhost","/").sendJsonObject(json).await()
      val retrieveRespBody2 = retrieveResponse2.bodyAsJsonObject()
      context.assertTrue(retrieveRespBody2.getInteger(LIKE)==0)
      context.assertTrue(retrieveRespBody2.getInteger(DISLIKE)==0)
//      context.assertTrue(retrieveRespBody2.getInteger(COLLECT)==0)
      context.assertFalse(oneArticle.getBoolean(LIKED))
      context.assertFalse(oneArticle.getBoolean(DISLIKED))
//      context.assertFalse(oneArticle.getBoolean(COLLECTED))

      async.complete()
    }

  }

  @Test
  fun `test update article`(context: TestContext){
    val async = context.async()
    val json = JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, HISTORY)

    GlobalScope.launch(vertx.dispatcher()) {
      val response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json).await()
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

      val updateResps = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(oneArticle).await()
      context.assertTrue(updateResps.bodyAsJsonObject().getBoolean(PUBLICATION))

      val json0 = JsonObject().put(ID, "zxj001").put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(TYPE, PUBLICATION).put(SUBTYPE, RETRIEVE).put(DIR,dir)

      val retrieveResponse = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/").sendJsonObject(json0).await()

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
