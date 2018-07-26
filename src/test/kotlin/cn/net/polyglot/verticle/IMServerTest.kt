package cn.net.polyglot.verticle

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
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
     /* if (vertx.fileSystem().existsBlocking(config.getString("dir")))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString("dir"), true)*/

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
        .put("action", "register")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562247")
        .put("version", 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean("register"))
        async.complete()
      }
  }

  @Test
  fun testAccountsCommunication(context: TestContext) {//依赖前一个方法
    val netClient0 = vertx.createNetClient()

    val async0 = context.async()

    netClient0.connect(config.getInteger("tcp-port"), "localhost") {
      val socket = it.result()
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562247")
        .toString().plus("\r\n"))

      socket.handler {
        println(it.toString())
        context.assertTrue(it.toJsonObject().getBoolean("login"))
        socket.close()
        netClient0.close()
        async0.complete()
      }
    }

  }

  @Test
  fun testApplyAddFriend(context: TestContext) {
    val async = context.async()
    val netClient = vertx.createNetClient()
    netClient.connect(config.getInteger("tcp-port"), config.getString("host")) {
      if (it.succeeded()) {
        val socket = it.result()
        val json = JsonObject("""{
          "type":"friend",
          "action":"request",
          "from":"zxj@polyglot.net.cn",
          "to":"zxj2017",
          "message":"请添加我为你的好友，我是哲学家",
          "version":0.1
        }""").toString().plus("\r\n")
        socket.closeHandler {
          println("close")
        }
        socket.exceptionHandler{
          print("Error:${it.cause?.message}")
        }
        socket.write(json)
       // async.complete()
      } else {
        print("failed:${it.cause()}")

      }
    }


  }

}
