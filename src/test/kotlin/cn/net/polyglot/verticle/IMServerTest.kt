package cn.net.polyglot.verticle

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
      .put("version", 0.1)
      .put("dir", Paths.get("").toAbsolutePath().toString() + File.separator + "social-vertex")
      .put("tcp-port", 7373)
      .put("http-port", 7575)
      .put("host", "localhost")
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun beforeClass(context: TestContext) {
      if (vertx.fileSystem().existsBlocking(config.getString("dir")))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString("dir"), true)

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
    webClient.post(config.getInteger("http-port"), "localhost", "/user")
      .sendJsonObject(JsonObject()
        .put("type", "user")
        .put("subtype", "register")
        .put("id", "zxj2017")
        .put("password", "431fe828b9b8e8094235dee515562247")
        .put("version", 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean("register"))
        async.complete()
      }

    val async1 = context.async()
    webClient.post(config.getInteger("http-port"), "localhost", "/user")
      .sendJsonObject(JsonObject()
        .put("type", "user")
        .put("subtype", "register")
        .put("id", "yangkui")
        .put("password", "431fe828b9b8e8094235dee515562248")
        .put("version", 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean("register"))
        async1.complete()
      }
  }

  @Test
  fun testAccountsAddFriend(context: TestContext) {
    val async = context.async()
    val client0 = vertx.createNetClient()
    val client1 = vertx.createNetClient()

    client0.connect(config.getInteger("tcp-port"), config.getString("host")) {
      val socket = it.result()
      socket.write(JsonObject()
        .put("type", "user")
        .put("subtype", "login")
        .put("id", "yangkui")
        .put("password", "431fe828b9b8e8094235dee515562248").toString().plus("\r\n")
      )

      socket.handler {
        val result = it.toJsonObject()
        println(result)
        when (result.getString("type")) {
          "user" -> {
            context.assertTrue(result.getBoolean("login"))//登陆成功
            socket.write(JsonObject().put("type", "friend")
              .put("subtype", "request")
              .put("to", "zxj2017")
              .put("message", "请添加我为你的好友，我是yangkui")
              .put("version", 0.1).toString().plus("\r\n"))
          }
          "friend" -> {
            context.assertEquals(result.getString("subtype"), "response")

            context.assertTrue(!vertx.fileSystem().existsBlocking(config.getString("dir") + File.separator + "yangkui"
              + File.separator + ".send" + File.separator + "zxj2017.json"))

            context.assertTrue(!vertx.fileSystem().existsBlocking(config.getString("dir") + File.separator + "zxj2017"
              + File.separator + ".receive" + File.separator + "yangkui.json"))

            context.assertTrue(vertx.fileSystem().existsBlocking(config.getString("dir") + File.separator + "yangkui"
              + File.separator + "zxj2017" + File.separator + "zxj2017.json"))

            context.assertTrue(vertx.fileSystem().existsBlocking(config.getString("dir") + File.separator + "zxj2017"
              + File.separator + "yangkui" + File.separator + "yangkui.json"))

            context.assertTrue(result.getBoolean("accept"))

            client0.close()//一旦收到好友响应，确认硬盘上文件存在，便关闭两个clients，并结束该unit test
            client1.close()
            async.complete()
          }
          else -> {
            throw Exception("unexpected type")
          }
        }
      }

      socket.exceptionHandler {
        socket.close()
      }
    }
    client1.connect(config.getInteger("tcp-port"), config.getString("host")) {
      val socket = it.result()
      socket.write(JsonObject()
        .put("type", "user")
        .put("subtype", "login")
        .put("id", "zxj2017")
        .put("password", "431fe828b9b8e8094235dee515562247").toString().plus("\r\n")
      )

      socket.handler {
        val result = it.toJsonObject()
        println(result)
        val type = result.getString("type")
        when (type) {
          "user" -> {
            context.assertTrue(it.toJsonObject().getBoolean("login"))//登陆成功
          }
          "friend" -> {
            context.assertTrue(it.toJsonObject().getString("subtype") == "request")
            //检查yangkui/.send/zxj2017.json 和 zxj2017/.receive/yangkui.json 两个文件存在
            context.assertTrue(vertx.fileSystem().existsBlocking(
              config.getString("dir") + separator + "yangkui" + separator + ".send" + separator + "zxj2017.json"))

            context.assertTrue(vertx.fileSystem().existsBlocking(
              config.getString("dir") + separator + "zxj2017" + separator + ".receive" + separator + "yangkui.json"))

            socket.write(JsonObject().put("type", "friend")
              .put("subtype", "response")
              .put("to", result.getString("from"))
              .put("accept", true)
              .put("version", 0.1).toString().plus("\r\n"))
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

    netClient.connect(config.getInteger("tcp-port"), "localhost") {
      val socket = it.result()
      socket.write(JsonObject()
        .put("type", "user")
        .put("subtype", "login")
        .put("id", "zxj2017")
        .put("password", "431fe828b9b8e8094235dee515562247")
        .toString().plus("\r\n"))

      socket.handler {
        val result = it.toJsonObject()
        val type = result.getString("type")
        when (type) {
          "user" -> {
            context.assertTrue(it.toJsonObject().getBoolean("login"))
          }
          "message" -> {
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

    netClient1.connect(config.getInteger("tcp-port"), "localhost") {
      val socket = it.result()
      socket.write(JsonObject()
        .put("type", "user")
        .put("subtype", "login")
        .put("id", "yangkui")
        .put("password", "431fe828b9b8e8094235dee515562248")
        .toString().plus("\r\n"))

      socket.handler {
        val result = it.toJsonObject()
        val type = result.getString("type")
        when (type) {
          "user" -> {
            context.assertTrue(it.toJsonObject().getBoolean("login"))
            socket.write(JsonObject("""{
              "type":"message",
              "subtype":"text",
              "to":"zxj2017",
              "body":"你好吗？",
              "version":0.1
            }""").toString().plus("\r\n"))
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
    netClient.connect(config.getInteger("tcp-port"), "localhost") {
      val socket = it.result()
      socket.write(JsonObject()
        .put("type", "user")
        .put("subtype", "login")
        .put("id", "yangkui")
        .put("password", "431fe828b9b8e8094235dee515562248")
        .toString().plus("\r\n"))

      socket.handler {
        val result = it.toJsonObject()
        val type = result.getString("type")
        when (type) {
          "user" -> {
            context.assertTrue(it.toJsonObject().getBoolean("login"))
            socket.write(JsonObject("""{
              "type":"message",
              "subtype":"text",
              "to":"zxj2017",
              "body":"你好吗？",
              "version":0.1
            }""").toString().plus("\r\n"))
          }
          else -> {
            throw Exception("unexpected type")
          }
        }
      }
    }
    val path = config.getString("dir") + separator + "zxj2017" + separator + ".message" + separator + "yangkui.sv"
    await().until {
      vertx.fileSystem().existsBlocking(path)
    }
    val file = vertx.fileSystem().readFileBlocking(path)
    context.assertTrue(file.toJsonObject().getString("from") == "yangkui")
    netClient.close()
    async.complete()
  }
}
