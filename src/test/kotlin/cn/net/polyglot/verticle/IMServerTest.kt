package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.DeploymentOptions
import org.awaitility.Awaitility.await
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.io.File.separator
import java.nio.file.Paths

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//按照名字升序执行代码
class IMServerTest {
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

      val option = DeploymentOptions(config = config)
      vertx.deployVerticle(IMTcpServerVerticle::class.java.name, option, context.asyncAssertSuccess())
      vertx.deployVerticle(WebServerVerticle::class.java.name, option, context.asyncAssertSuccess())
      vertx.deployVerticle(IMMessageVerticle::class.java.name, option, context.asyncAssertSuccess())
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
    webClient.post(config.getInteger(HTTP_PORT), "localhost", "/user")
      .sendJsonObject(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .put(VERSION, 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean(REGISTER))
        async.complete()
      }

    val async1 = context.async()
    webClient.post(config.getInteger(HTTP_PORT), "localhost", "/user")
      .sendJsonObject(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "yangkui")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
        .put(VERSION, 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean(REGISTER))
        async1.complete()
      }

    val async2 = context.async()
    webClient.post(config.getInteger(HTTP_PORT), "localhost", "/user")
      .sendJsonObject(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, REGISTER)
        .put(ID, "zhaoce")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
        .put(VERSION, 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean(REGISTER))
        async2.complete()
      }
  }

  @Test
  fun testAccountsAddFriend(context: TestContext) {
    val async = context.async()
    val client0 = vertx.createNetClient()
    val client1 = vertx.createNetClient()

    client0.connect(config.getInteger(TCP_PORT), config.getString(HOST)) { asyncResult ->
      val socket = asyncResult.result()
      socket.write(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "yangkui")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562248").toString().plus(END)
      )

      socket.handler {
        val result = it.toJsonObject()
        println(result)
        when (result.getString(TYPE)) {
          USER -> {
            context.assertTrue(result.getBoolean(LOGIN))//登陆成功
            socket.write(JsonObject().put(TYPE, FRIEND)
              .put(SUBTYPE, REQUEST)
              .put(TO, "zxj2017")
              .put(MESSAGE, "请添加我为你的好友，我是yangkui")
              .put(VERSION, 0.1).toString().plus(END))
          }
          FRIEND -> {
            context.assertEquals(result.getString(SUBTYPE), RESPONSE)

            context.assertTrue(!vertx.fileSystem().existsBlocking(config.getString(DIR) + File.separator + "yangkui"
              + File.separator + ".send" + File.separator + "zxj2017.json"))

            context.assertTrue(!vertx.fileSystem().existsBlocking(config.getString(DIR) + File.separator + "zxj2017"
              + File.separator + ".receive" + File.separator + "yangkui.json"))

            context.assertTrue(vertx.fileSystem().existsBlocking(config.getString(DIR) + File.separator + "yangkui"
              + File.separator + "zxj2017" + File.separator + "zxj2017.json"))

            context.assertTrue(vertx.fileSystem().existsBlocking(config.getString(DIR) + File.separator + "zxj2017"
              + File.separator + "yangkui" + File.separator + "yangkui.json"))

            context.assertTrue(result.getBoolean(ACCEPT))

            client0.close()//一旦收到好友响应，确认硬盘上文件存在，便关闭两个clients，并结束该unit test
            client1.close()
            async.complete()
          }
          else -> {
            context.assertTrue(false)
          }
        }
      }

      socket.exceptionHandler {
        socket.close()
      }
    }
    client1.connect(config.getInteger(TCP_PORT), config.getString(HOST)) { asyncResult ->
      val socket = asyncResult.result()
      socket.write(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247").toString().plus(END)
      )

      socket.handler {
        val result = it.toJsonObject()
        println(result)
        val type = result.getString(TYPE)
        when (type) {
          USER -> {
            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))//登陆成功
          }
          FRIEND -> {
            context.assertTrue(it.toJsonObject().getString(SUBTYPE) == REQUEST)
            //检查yangkui/.send/zxj2017.json 和 zxj2017/.receive/yangkui.json 两个文件存在
            context.assertTrue(vertx.fileSystem().existsBlocking(
              config.getString(DIR) + separator + "yangkui" + separator + ".send" + separator + "zxj2017.json"))

            context.assertTrue(vertx.fileSystem().existsBlocking(
              config.getString(DIR) + separator + "zxj2017" + separator + ".receive" + separator + "yangkui.json"))

            socket.write(JsonObject().put(TYPE, FRIEND)
              .put(SUBTYPE, RESPONSE)
              .put(TO, result.getString("from"))
              .put(ACCEPT, true)
              .put(VERSION, 0.1).toString().plus(END))
          }
        }
      }
    }
  }

  @Test
  fun testAccountsCommunication(context: TestContext) {
    val netClient = vertx.createNetClient()
    val netClient1 = vertx.createNetClient()

    val async = context.async()

    netClient.connect(config.getInteger(TCP_PORT), "localhost") { asyncResult ->
      val socket = asyncResult.result()
      socket.write(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        .toString().plus(END))

      socket.handler {
        val result = it.toJsonObject()
        val type = result.getString(TYPE)
        when (type) {
          USER -> {
            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))
          }
          MESSAGE -> {
            socket.close()
            netClient.close()
            netClient1.close()
            async.complete()
          }
          else -> {
            context.assertTrue(false)
          }
        }
      }
    }

    netClient1.connect(config.getInteger(TCP_PORT), "localhost") { asyncResult ->
      val socket = asyncResult.result()
      socket.write(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "yangkui")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
        .toString().plus(END))

      socket.handler {
        val result = it.toJsonObject()
        val type = result.getString(TYPE)
        when (type) {
          USER -> {
            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))
            socket.write(JsonObject().put(TYPE, MESSAGE)
              .put(SUBTYPE, TEXT)
              .put(TO, "zxj2017")
              .put(BODY, "你好吗？")
              .put(VERSION, 0.1).toString().plus(END))
          }
          else -> {
            context.assertTrue(false)
          }
        }
      }
    }
  }

  @Test
  fun testAccountsOfflineCommunication(context: TestContext) {
    val netClient = vertx.createNetClient()
    val async = context.async()
    netClient.connect(config.getInteger(TCP_PORT), "localhost") { asyncResult ->
      val socket = asyncResult.result()
      socket.write(JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LOGIN)
        .put(ID, "yangkui")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562248")
        .toString().plus(END))

      socket.handler {
        val result = it.toJsonObject()
        val type = result.getString(TYPE)
        when (type) {
          USER -> {
            context.assertTrue(it.toJsonObject().getBoolean(LOGIN))
            println(it.toJsonObject())
            socket.write(JsonObject().put(TYPE, MESSAGE)
              .put(SUBTYPE, TEXT)
              .put(TO, "zxj2017")
              .put(BODY, "你好吗？")
              .put(VERSION, 0.1).toString().plus(END))

            socket.write(JsonObject().put(TYPE, MESSAGE)
              .put(SUBTYPE, TEXT)
              .put(TO, "zxj2017")
              .put(BODY, "你收到了吗？")
              .put(VERSION, 0.1).toString().plus(END))
          }
          else -> {
            context.assertTrue(false)
          }
        }
      }
    }
    val path = config.getString(DIR) + separator + "zxj2017" + separator + ".message" + separator + "yangkui.sv"
    await().until {
      vertx.fileSystem().existsBlocking(path)
    }
    val file = vertx.fileSystem().readFileBlocking(path)
    context.assertTrue(file.toJsonObject().getString("from") == "yangkui")
    netClient.close()
    async.complete()
  }
  
  @Test
  fun testAccountsOfflineInform(context: TestContext) {
    val async = context.async()

    webClient.post(config.getInteger(HTTP_PORT), config.getString(HOST), "/user").sendJson(
      JsonObject()
        .put(TYPE, USER)
        .put(SUBTYPE, LEFT)
        .put(ID, "zxj2017")
        .put(PASSWORD, "431fe828b9b8e8094235dee515562247")
    ) {
      if (it.succeeded()) {
        val result = it.result().body().toJsonObject()
        context.assertTrue(result.getBoolean(LEFT))
        println(result)
        async.complete()
      }

    }
  }
}
