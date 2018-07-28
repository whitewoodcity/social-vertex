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
        .put("action", "register")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562247")
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
        .put("action", "register")
        .put("user", "yangkui")
        .put("crypto", "431fe828b9b8e8094235dee515562248")
        .put("version", 0.1)
      ) { response ->
        println(response.result().body())
        context.assertTrue(response.result().body().toJsonObject().getBoolean("register"))
        async1.complete()
      }
  }

  //todo friend和message，都是单向信息流动，不需要任何的response和reply，所有的响应都以相反方向单向发送，请依照此设计完善unit tests
  @Test
  fun testAccountsAddFriend(context: TestContext) {
    val async = context.async()
    vertx.createNetClient().connect(config.getInteger("tcp-port"), config.getString("host")) {
      val socket = it.result()
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "yangkui")
        .put("crypto", "431fe828b9b8e8094235dee515562248").toString().plus("\r\n")
      )

      socket.handler {
        val result = it.toJsonObject()
        println(result)
        when (result.getString("type")) {
          "user" -> {
            context.assertTrue(result.getBoolean("login"))//登陆成功
            socket.write(JsonObject().put("type", "friend")
              .put("action", "request")
              .put("to", "zxj2017")
              .put("message", "请添加我为你的好友，我是yangkui")
              .put("version", 0.1).toString().plus("\r\n"))
          }
          "friend" -> {
            //todo  添加收到好友响应时候的文件检查，可参考下面的代码

            //------------------把代码写在上面
            async.complete()
          }
          else -> {

          }
        }
      }

      socket.exceptionHandler {
        socket.close()
      }
    }
    vertx.createNetClient().connect(config.getInteger("tcp-port"), config.getString("host")) {
      val socket = it.result()
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562247").toString().plus("\r\n")
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
            //检查yangkui/.send/zxj2017.json 和 zxj2017/.receive/yangkui.json 两个文件存在
            context.assertTrue(vertx.fileSystem().existsBlocking(
              config.getString("dir") + separator + "yangkui" + separator + ".send" + separator + "zxj2017.json"))

            context.assertTrue(vertx.fileSystem().existsBlocking(
              config.getString("dir") + separator + "zxj2017" + separator + ".receive" + separator + "yangkui.json"))

            //todo 实现该测试案例的逻辑
            socket.write(JsonObject().put("type", "friend")
              .put("action", "response")
              .put("to", result.getString("from"))
              .put("accept", true)
              .put("version", 0.1).toString().plus("\r\n"))
          }
        }

      }
      socket.write(JsonObject()
        .put("type", "user")
        .put("action", "login")
        .put("user", "zxj2017")
        .put("crypto", "431fe828b9b8e8094235dee515562247").toString().plus("\r\n")
      )
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

}
