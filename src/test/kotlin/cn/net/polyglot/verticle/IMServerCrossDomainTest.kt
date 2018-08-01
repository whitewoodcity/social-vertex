package cn.net.polyglot.verticle

import cn.net.polyglot.config.*
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.DeploymentOptions
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//按照名字升序执行代码
class IMServerCrossDomainTest {
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
  }

/**
  @Test
  fun testAccountsAddFriendCrossDomain(context: TestContext) {
    val async = context.async()
    val client1 = vertx.createNetClient()
    val client2 = vertx.createNetClient()
    client1.connect(config.getInteger("tcp-port"), "localhost") {
      val socket = it.result()
      socket.handler {
        val result = it.toJsonObject()
        val type = result.getString("type")
        when (type) {
          "user" -> {
            context.assertTrue(result.getBoolean("login"))
            socket.write(JsonObject()
              .put("type", "friend")
              .put("action", "request")
              .put("to", "zxj2017@127.0.0.1")
              .put("message", "请添加我为你的好友，我是yangkui")
              .put("version", "0.1")
              .toString().plus("\r\n")
            )
          }
          "friend" -> {
            if (result.getString("action") == "response") {
              context.assertTrue(result.getBoolean("accept") == true)
            }
            client1.close()
            client2.close()
            async.complete()
          }
        }

      }
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "yangkui")
        .put("crypto", "431fe828b9b8e8094235dee515562248")
        .toString().plus("\r\n"))

    }

    client2.connect(config.getInteger("tcp-port"), "localhost") {
      val socket = it.result()
      socket.handler {
        val result = it.toJsonObject()
        val type = result.getString("type")
        when (type) {
          "friend" -> {
            socket.write(JsonObject().put("type", "friend")
              .put("action", "response")
              .put("to", result.getString("from"))
              .put("accept", true)
              .put("version", 0.1).toString().plus("\r\n"))
          }
          "user" -> {
            context.assertTrue(result.getBoolean("login"))
          }
        }
      }
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562247")
        .toString().plus("\r\n"))
    }
  }

  @Test
  fun testAccountsCommunicationCrossDomain(context: TestContext) {
    val netClient1 = vertx.createNetClient()
    val netClient2 = vertx.createNetClient()
    val async = context.async()
    netClient1.connect(config.getInteger("tcp-port"), config.getString("host")) {
      val socket = it.result()
      socket.handler {
        val type = it.toJsonObject().getString("type")
        when (type) {
          "user" -> {
            context.assertTrue(it.toJsonObject().getBoolean("login"))
            socket.write(JsonObject("""{
              "type":"message",
              "action":"text",
              "to":"yangkui@localhost",
              "body":"你好吗？",
              "version":0.1
            }""").toString().plus("\r\n"))

          }
        }
      }
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562247")
        .toString().plus("\r\n"))
    }
    netClient2.connect(config.getInteger("tcp-port"), config.getString("host")) {
      val socket = it.result()
      socket.handler {
        val type = it.toJsonObject().getString("type")
        when (type) {
          "user" -> {
            context.assertTrue(it.toJsonObject().getBoolean("login"))

          }
          "message" -> {
            println(it.toJsonObject())
            netClient1.close()
            netClient2.close()
            async.complete()
          }
        }

      }
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "yangkui")
        .put("crypto", "431fe828b9b8e8094235dee515562248")
        .toString().plus("\r\n"))
    }

  }
  @Test
  fun testAccountsOfflineCommunicationCrossDomain(context: TestContext) {
    val netClient1 = vertx.createNetClient()
    val async = context.async()
    netClient1.connect(config.getInteger("tcp-port"), config.getString("host")) {
      val socket = it.result()
      socket.handler {
        val type = it.toJsonObject().getString("type")
        when (type) {
          "user" -> {
            context.assertTrue(it.toJsonObject().getBoolean("login"))
            socket.write(JsonObject("""{
              "type":"message",
              "action":"text",
              "to":"yangkui@localhost",
              "body":"你好吗？",
              "version":0.1
            }""").toString().plus("\r\n"))
          }
          else->{
          }
        }
      }
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562247")
        .toString().plus("\r\n"))
    }
    val fs = vertx.fileSystem()
    val path = config.getString("dir") + File.separator + "yangkui" + File.separator + ".message" + File.separator + "zxj2017@127.0.0.1.sv"
    Awaitility.await().until {
      fs.existsBlocking(path)
    }
    val file = fs.readFileBlocking(path)
    context.assertTrue(file.toJsonObject().getString("from") == "zxj2017@127.0.0.1")
    netClient1.close()
    async.complete()
  }
*/
}
