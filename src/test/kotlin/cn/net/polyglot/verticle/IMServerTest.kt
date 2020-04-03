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
import cn.net.polyglot.module.inNextYear
//import io.reactiverse.es4x.impl.VertxFileSystem
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.file.createFileAwait
import io.vertx.kotlin.core.file.mkdirsAwait
import io.vertx.kotlin.core.file.writeFileAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.web.client.sendJsonObjectAwait
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.awaitility.Awaitility.await
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File.separator
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

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
    fun `before class`(context: TestContext) {
      if (vertx.fileSystem().existsBlocking(config.getString(DIR)))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString(DIR), true)

      val option = deploymentOptionsOf(config = config)
      val fut0 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.message.MessageVerticle", option)
      val fut1 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.friend.FriendVerticle", option)
      val fut2 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.user.UserVerticle", option)
      val fut3 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.search.SearchVerticle", option)
      val fut4 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.WebServerVerticle", option)
      val fut5 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMTcpServerVerticle", option)
      val fut6 = vertx.deployVerticle("kt:cn.net.polyglot.verticle.im.IMServletVerticle", option)
      
      await().until{
        fut0.isComplete &&
        fut1.isComplete &&
        fut2.isComplete &&
        fut3.isComplete &&
        fut4.isComplete &&
        fut5.isComplete &&
        fut6.isComplete
      }

      context.assertTrue(fut0.succeeded())
      context.assertTrue(fut1.succeeded())
      context.assertTrue(fut2.succeeded())
      context.assertTrue(fut3.succeeded())
      context.assertTrue(fut4.succeeded())
      context.assertTrue(fut5.succeeded())
      context.assertTrue(fut6.succeeded())
    }

    @AfterClass
    @JvmStatic
    fun `after class`(context: TestContext) {
      vertx.close(context.asyncAssertSuccess())
    }
  }

  private val webClient = WebClient.create(vertx)

  @Test
  fun `test account register`(context: TestContext) {
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
  fun `test login`(context: TestContext) {
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
  fun `test login fail`(context: TestContext) {
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
  fun `test search`(context: TestContext) {
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
  fun `test friend request`(context: TestContext){
    //user:yangkui login
    val async = context.async()

    val client = vertx.createNetClient()

    client.connect(config.getInteger(TCP_PORT), config.getString(HOST)) { asyncResult ->
      val socket = asyncResult.result()
      socket.write(JsonObject()
        .put(TYPE, LOGIN)
        .put(ID, "yangkui")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562248").toString().plus(END)
      )

      socket.handler {
        val result = JsonObject(it.toString().trim())
        println(result)

        if(result.getString(TYPE)== FRIEND && result.getString(SUBTYPE)==REQUEST){
          socket.close()
        }
      }

      socket.closeHandler{async.complete()}
    }

    //now send the frien request to the user:yangkui

    GlobalScope.launch(vertx.dispatcher()) {
      delay(100)//延迟一下，防止发得太快，前面的tcp代码来不及登陆
      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObject(JsonObject()
          .put(TYPE, FRIEND)
          .put(SUBTYPE, REQUEST)
          .put(ID, "zxj2017")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
          .put(TO, "yangkui")
        ) {}
    }
    await().until {
      //check zxj2017/.send/yangkui.json & yangkui/.receive/zxj2017.json two files exist
      vertx.fileSystem().existsBlocking(config.getString(DIR)+ separator + "zxj2017"+ separator +".send"+ separator +"yangkui.json")
        && vertx.fileSystem().existsBlocking(config.getString(DIR)+ separator + "yangkui"+ separator +".receive"+ separator +"zxj2017.json")
    }
  }

  @Test
  fun `test friend response`(context: TestContext){
    //user:zxj2017 login
    val async = context.async()

    val client = vertx.createNetClient()

    client.connect(config.getInteger(TCP_PORT), config.getString(HOST)) { asyncResult ->
      val socket = asyncResult.result()
      socket.write(JsonObject()
        .put(TYPE, LOGIN)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247").toString().plus(END)
      )

      socket.handler {
        val result = JsonObject(it.toString().trim())
        println(result)

        if(result.getString(TYPE)== FRIEND && result.getString(SUBTYPE)==RESPONSE && result.getBoolean(ACCEPT)){
          socket.close()
        }
      }

      socket.closeHandler{async.complete()}
    }

    //now send the frien request to the user:zxj2017
    GlobalScope.launch(vertx.dispatcher()) {
      delay(100)
      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObject(JsonObject()
          .put(TYPE, FRIEND)
          .put(SUBTYPE, RESPONSE)
          .put(ID, "yangkui")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
          .put(TO, "zxj2017")
          .put(ACCEPT, true)
        ) {}
    }
  }

  @Test
  fun `test messaging`(context: TestContext){
    //user:zxj2017 login
    val async = context.async()

    val client = vertx.createNetClient()

    client.connect(config.getInteger(TCP_PORT), config.getString(HOST)) { asyncResult ->
      val socket = asyncResult.result()
      socket.write(JsonObject()
        .put(TYPE, LOGIN)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247").toString().plus(END)
      )

      socket.handler {
        val result = JsonObject(it.toString().trim())
        println(result)

        if(result.getString(TYPE) == MESSAGE && result.getString(MESSAGE) == "hello"){
          socket.close()
        }
      }

      socket.closeHandler{async.complete()}
    }

    //now send the message to the user:zxj2017
    GlobalScope.launch(vertx.dispatcher()) {
      delay(100)

      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObjectAwait(JsonObject()
          .put(TYPE, MESSAGE)
          .put(SUBTYPE, TEXT)
          .put(ID, "yangkui")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
          .put(TO, "zxj2017")
          .put(MESSAGE, "hi")
        )

      val response = webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObjectAwait(JsonObject()
          .put(TYPE, MESSAGE)
          .put(SUBTYPE, TEXT)
          .put(ID, "yangkui")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
          .put(TO, "zxj2017")
          .put(MESSAGE, "hello")
        )
      context.assertTrue(response.bodyAsJsonObject().getBoolean(MESSAGE))
    }

  }

  @Test
  fun `test messaging history`(context: TestContext){
    val async = context.async(4)
    GlobalScope.launch(vertx.dispatcher()) {
      val path0 = "${config.getString(DIR)}${separator}zxj2017${separator}yangkui${separator}2000${separator}01$separator"
      val path1 = "${config.getString(DIR)}${separator}yangkui${separator}zxj2017${separator}2000${separator}01$separator"
      vertx.fileSystem().mkdirsAwait(path0)
      vertx.fileSystem().mkdirsAwait(path1)
      vertx.fileSystem().createFileAwait(path0+"01.jsons")
      vertx.fileSystem().createFileAwait(path1+"01.jsons")
      val msg = JsonObject().put(TYPE, MESSAGE).put(SUBTYPE, TEXT)
        .put(ID, "zxj2017").put(TO, "yangkui").put(MESSAGE, "hi")
        .put(DATE, "2000-01-01").put(TIME, "00:00:00")
      vertx.fileSystem().writeFileAwait(path0+"01.jsons",msg.toBuffer())
      vertx.fileSystem().writeFileAwait(path1+"01.jsons",msg.toBuffer())

      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObject(JsonObject()
          .put(TYPE, MESSAGE)
          .put(SUBTYPE, HISTORY)
          .put(ID, "yangkui")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
          .put(FRIEND, "zxj2017")
        ){
          println(it.result().bodyAsJsonObject())
          context.assertTrue(it.result().bodyAsJsonObject().getJsonArray(HISTORY).size()==3)
          async.countDown()
        }

      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObject(JsonObject()
          .put(TYPE, MESSAGE)
          .put(SUBTYPE, HISTORY)
          .put(ID, "yangkui")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
          .put(FRIEND, "zxj2017")
          .put(DATE,SimpleDateFormat("yyyy-MM-dd").format(Date().inNextYear()))
        ){
          println(it.result().bodyAsJsonObject())
          context.assertTrue(it.result().bodyAsJsonObject().getJsonArray(HISTORY).size()==3)
          async.countDown()
        }

      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObject(JsonObject()
          .put(TYPE, MESSAGE)
          .put(SUBTYPE, HISTORY)
          .put(ID, "yangkui")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
          .put(FRIEND, "zxj2017")
          .put(DATE, SimpleDateFormat("yyyy-MM-dd").format(Date()))
        ){
          println(it.result().bodyAsJsonObject())
          context.assertTrue(it.result().bodyAsJsonObject().getJsonArray(HISTORY).size()==1)
          async.countDown()
        }

      webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
        .sendJsonObject(JsonObject()
          .put(TYPE, MESSAGE)
          .put(SUBTYPE, HISTORY)
          .put(ID, "yangkui")
          .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
          .put(FRIEND, "zxj2017")
          .put(DATE, "1999-12-31")
        ){
          println(it.result().bodyAsJsonObject())
          context.assertTrue(it.result().bodyAsJsonObject().getJsonArray(HISTORY).size()==0)
          async.countDown()
        }
    }
  }
}
