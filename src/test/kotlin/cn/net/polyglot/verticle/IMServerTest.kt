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
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.deploymentOptionsOf
import org.awaitility.Awaitility.await
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File.separator
import java.nio.file.Paths

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//按照名字升序执行代码
class IMServerTest {
  companion object {
    private val config = JsonObject()
      .put(VERSION, 0.1)
      .put(DIR, Paths.get("").toAbsolutePath().toString() + separator + "social-vertex")
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
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.friend.FriendVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.user.UserVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.search.SearchVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.community.WebServerVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMTcpServerVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMMessageVerticle", option, context.asyncAssertSuccess())
      vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMServletVerticle", option, context.asyncAssertSuccess())
    }

    @AfterClass
    @JvmStatic
    fun afterClass(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  private val webClient = WebClient.create(vertx)

  @Test
  fun testAccountRegister(context: TestContext) {
    val async = context.async()
    webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
      .sendJsonObject(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zxj2017")
        .put(NICKNAME, "哲学家")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562247")
        .put(VERSION, 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean(REGISTER))
        async.complete()
      }

    val async1 = context.async()
    webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
      .sendJsonObject(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "yangkui")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562248")
        .put(VERSION, 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean(REGISTER))
        async1.complete()
      }

    val async2 = context.async()
    webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
      .sendJsonObject(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zhaoce")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
        .put(PASSWORD2, "431fe828b9b8e8094235dee515562248")
        .put(VERSION, 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean(REGISTER))
        async2.complete()
      }
  }

  @Test
  fun testLogin(context: TestContext) {
    val async = context.async()
    webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
      .sendJsonObject(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(VERSION, 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean(LOGIN))
        async.complete()
      }
  }

  @Test
  fun testLoginFail(context: TestContext) {
    val async = context.async()
    webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
      .sendJsonObject(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562246")
        .put(VERSION, 0.1)
      ) { response ->
        println(response.result().body())
        context.assertFalse(response.result().body().toJsonObject().getBoolean(LOGIN))
        async.complete()
      }
  }

  @Test
  fun testSearch(context: TestContext) {
    val async = context.async()
    webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
      .sendJsonObject(JsonObject()
        .put(TYPE, SEARCH)
        .put(KEYWORD, "zxj2017")
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean(SEARCH))
        context.assertNotNull(response.result().body().toJsonObject().getJsonObject(USER))
        async.complete()
      }
  }

  @Test
  fun testFriendRequest(context: TestContext){
    webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
      .sendJsonObject(JsonObject()
        .put(TYPE, FRIEND)
        .put(SUBTYPE, REQUEST)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(FROM, "zxj2017")
        .put(TO, "yangkui")
      ){}
    await().until {
      //check zxj2017/.send/yangkui.json & yangkui/.receive/zxj2017.json two files exist
      vertx.fileSystem().existsBlocking(config.getString(DIR)+ separator + "zxj2017"+ separator +".send"+ separator +"yangkui.json")
        && vertx.fileSystem().existsBlocking(config.getString(DIR)+ separator + "yangkui"+ separator +".receive"+ separator +"zxj2017.json")
    }
  }

//  @Test
//  fun testAccountsAddFriend(context: TestContext) {
//    val async = context.async()
//    val client0 = vertx.createNetClient()
//    val client1 = vertx.createNetClient()
//
//    client0.connect(config.getInteger(TCP_PORT), config.getString(HOST)) { asyncResult ->
//      val socket = asyncResult.result()
//      socket.write(JsonObject()
//        .put(TYPE, USER)
//        .put(SUBTYPE, LOGIN)
//        .put(ID, "yangkui")
//        .put(PASSWORD, "431fe828b9b8e8094235dee515562248").toString().plus(END)
//      )
//
//      socket.handler {
//        val result = JsonObject(it.toString().trim())
//        println(result)
//        when (result.getString(TYPE)) {
//          USER -> {
//            context.assertTrue(result.getBoolean(LOGIN))//登陆成功
//            socket.write(JsonObject().put(TYPE, FRIEND)
//              .put(SUBTYPE, REQUEST)
//              .put(TO, "zxj2017")
//              .put(MESSAGE, "请添加我为你的好友，我是yangkui")
//              .put(VERSION, 0.1).toString().plus(END))
//          }
//          FRIEND -> {
//            context.assertEquals(result.getString(SUBTYPE), RESPONSE)
//
//            context.assertTrue(!vertx.fileSystem().existsBlocking(config.getString(DIR) + File.separator + "yangkui"
//              + File.separator + ".send" + File.separator + "zxj2017.json"))
//
//            context.assertTrue(!vertx.fileSystem().existsBlocking(config.getString(DIR) + File.separator + "zxj2017"
//              + File.separator + ".receive" + File.separator + "yangkui.json"))
//
//            context.assertTrue(vertx.fileSystem().existsBlocking(config.getString(DIR) + File.separator + "yangkui"
//              + File.separator + "zxj2017" + File.separator + "zxj2017.json"))
//
//            context.assertTrue(vertx.fileSystem().existsBlocking(config.getString(DIR) + File.separator + "zxj2017"
//              + File.separator + "yangkui" + File.separator + "yangkui.json"))
//
//            context.assertTrue(result.getBoolean(ACCEPT))
//
//            client0.close()//一旦收到好友响应，确认硬盘上文件存在，便关闭两个clients，并结束该unit test
//            client1.close()
//            async.complete()
//          }
//          else -> {
//            context.assertTrue(false)
//          }
//        }
//      }
//
//      socket.exceptionHandler {
//        socket.close()
//      }
//    }
//    client1.connect(config.getInteger(TCP_PORT), config.getString(HOST)) { asyncResult ->
//      val socket = asyncResult.result()
//      socket.write(JsonObject()
//        .put(TYPE, USER)
//        .put(SUBTYPE, LOGIN)
//        .put(ID, "zxj2017")
//        .put(PASSWORD, "431fe828b9b8e8094235dee515562247").toString().plus(END)
//      )
//
//      socket.handler {
//        val result = it.toJsonObject()
//        println(result)
//        val type = result.getString(TYPE)
//        when (type) {
//          USER -> {
//            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))//登陆成功
//          }
//          FRIEND -> {
//            context.assertTrue(it.toJsonObject().getString(SUBTYPE) == REQUEST)
//            //检查yangkui/.send/zxj2017.json 和 zxj2017/.receive/yangkui.json 两个文件存在
//            context.assertTrue(vertx.fileSystem().existsBlocking(
//              config.getString(DIR) + separator + "yangkui" + separator + ".send" + separator + "zxj2017.json"))
//
//            context.assertTrue(vertx.fileSystem().existsBlocking(
//              config.getString(DIR) + separator + "zxj2017" + separator + ".receive" + separator + "yangkui.json"))
//
//            socket.write(JsonObject().put(TYPE, FRIEND)
//              .put(SUBTYPE, RESPONSE)
//              .put(TO, result.getString("from"))
//              .put(ACCEPT, true)
//              .put(VERSION, 0.1).toString().plus(END))
//          }
//        }
//      }
//    }
//  }
//
//  @Test
//  fun testAccountsCommunication(context: TestContext) {
//    val netClient = vertx.createNetClient()
//    val netClient1 = vertx.createNetClient()
//
//    val async = context.async()
//
//    netClient.connect(config.getInteger(TCP_PORT), "localhost") { asyncResult ->
//      val socket = asyncResult.result()
//      socket.write(JsonObject()
//        .put(TYPE, USER)
//        .put(SUBTYPE, LOGIN)
//        .put(ID, "zxj2017")
//        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
//        .toString().plus(END))
//
//      socket.handler {
//        val result = JsonObject(it.toString().trim())
//        val type = result.getString(TYPE)
//        when (type) {
//          USER -> {
//            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))
//          }
//          MESSAGE -> {
//            socket.close()
//            netClient.close()
//            netClient1.close()
//            async.complete()
//          }
//          else -> {
//            context.assertTrue(false)
//          }
//        }
//      }
//    }
//
//    netClient1.connect(config.getInteger(TCP_PORT), "localhost") { asyncResult ->
//      val socket = asyncResult.result()
//      socket.write(JsonObject()
//        .put(TYPE, USER)
//        .put(SUBTYPE, LOGIN)
//        .put(ID, "yangkui")
//        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
//        .toString().plus(END))
//
//      socket.handler {
//        val result = JsonObject(it.toString().trim())
//        val type = result.getString(TYPE)
//        when (type) {
//          USER -> {
//            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))
//            socket.write(JsonObject().put(TYPE, MESSAGE)
//              .put(SUBTYPE, TEXT)
//              .put(TO, "zxj2017")
//              .put(BODY, "你好吗？")
//              .put(VERSION, 0.1).toString().plus(END))
//          }
//          else -> {
//            context.assertTrue(false)
//          }
//        }
//      }
//    }
//  }
//
//  @Test
//  fun testAccountsOfflineCommunication(context: TestContext) {
//    val netClient = vertx.createNetClient()
//    val async = context.async()
//    netClient.connect(config.getInteger(TCP_PORT), "localhost") { asyncResult ->
//      val socket = asyncResult.result()
//      socket.write(JsonObject()
//        .put(TYPE, USER)
//        .put(SUBTYPE, LOGIN)
//        .put(ID, "yangkui")
//        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
//        .toString().plus(END))
//
//      socket.handler {
//        val result = JsonObject(it.toString().trim())
//        val type = result.getString(TYPE)
//        when (type) {
//          USER -> {
//            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))
//            println(it.toJsonObject())
//            socket.write(JsonObject().put(TYPE, MESSAGE)
//              .put(SUBTYPE, TEXT)
//              .put(TO, "zxj2017")
//              .put(BODY, "你好吗？")
//              .put(VERSION, 0.1).toString().plus(END))
//
//            socket.write(JsonObject().put(TYPE, MESSAGE)
//              .put(SUBTYPE, TEXT)
//              .put(TO, "zxj2017")
//              .put(BODY, "你收到了吗？")
//              .put(VERSION, 0.1).toString().plus(END))
//          }
//          else -> {
//            context.assertTrue(false)
//          }
//        }
//      }
//    }
//    val path = config.getString(DIR) + separator + "zxj2017" + separator + ".message" + separator + "yangkui.sv"
//    await().until {
//      vertx.fileSystem().existsBlocking(path)
//    }
//    val file = vertx.fileSystem().readFileBlocking(path)
//    println(file.toString())
//    context.assertEquals(file.toString().trim().split(END).size,2)
//    context.assertTrue(JsonObject(file.toString().trim().split(END)[0]).getString(FROM) == "yangkui")
//    netClient.close()
//    async.complete()
//  }
//
//  @Test
//  fun testAccountsOfflineFriendRequest(context: TestContext) {
//    val netClient = vertx.createNetClient()
//    val async = context.async()
//    netClient.connect(config.getInteger(TCP_PORT), "localhost") { asyncResult ->
//      val socket = asyncResult.result()
//      socket.write(JsonObject()
//        .put(TYPE, USER)
//        .put(SUBTYPE, LOGIN)
//        .put(ID, "zhaoce")
//        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
//        .toString().plus(END))
//
//      socket.handler {
//        val result = JsonObject(it.toString().trim())
//        val type = result.getString(TYPE)
//        when (type) {
//          USER -> {
//            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))
//            println(it.toJsonObject())
//            socket.write(JsonObject().put(TYPE, FRIEND)
//              .put(SUBTYPE, REQUEST)
//              .put(TO, "zxj2017")
//              .put(MESSAGE, "请加我为你的好友")
//              .put(VERSION, 0.1).toString().plus(END))
//
//          }
//          else -> {
//            context.assertTrue(false)
//          }
//        }
//      }
//    }
//    val path = config.getString(DIR) + separator + "zxj2017" + separator + ".receive" + separator + "zhaoce.json"
//    await().until {
//      vertx.fileSystem().existsBlocking(path)
//    }
//    val file = vertx.fileSystem().readFileBlocking(path)
//    context.assertTrue(file.toJsonObject().getString("from") == "zhaoce")
//    netClient.close()
//    async.complete()
//  }
//
//  @Test
//  fun testAccountsOfflineInform(context: TestContext) {
//    val async = context.async()
//
//    webClient.put(config.getInteger(HTTP_PORT), config.getString(HOST), "/").sendJson(
//      JsonObject()
//        .put(TYPE, USER)
//        .put(SUBTYPE, OFFLINE)
//        .put(ID, "zxj2017")
//        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
//    ) {
//      if (it.succeeded()) {
//        val result = it.result().body().toJsonObject()
//        println(result)
//        context.assertTrue(result.getBoolean(OFFLINE))
//        context.assertTrue(result.containsKey(MESSAGES))
//        context.assertTrue(result.containsKey(FRIENDS))
//
//        async.complete()
//      }
//    }
//  }
//  @Test
//  fun  testAccountsHistoryInform(context: TestContext){
//    val async = context.async()
//    val  date = SimpleDateFormat("yyyy-MM-dd").format(Date(Date().time + (1000 * 60 * 60 * 24)))
//    webClient.put(config.getInteger(HTTP_PORT), config.getString(HOST),"/").sendJson(
//      JsonObject()
//        .put(TYPE, USER)
//        .put(SUBTYPE, HISTORY)
//        .put(DATE,date)
//        .put(ID,"zxj2017")
//        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
//        .put(FRIEND,"yangkui")
//        .put(VERSION,0.2)
//    ){
//      val result = it.result().body().toJsonObject()
//      assert(result.getBoolean("history"))
//      async.complete()
//    }
//  }
}
