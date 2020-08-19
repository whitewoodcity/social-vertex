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

//import io.reactiverse.es4x.impl.VertxFileSystem
import cn.net.polyglot.config.ACCEPT
import cn.net.polyglot.config.DATE
import cn.net.polyglot.config.DIR
import cn.net.polyglot.config.END
import cn.net.polyglot.config.FRIEND
import cn.net.polyglot.config.HISTORY
import cn.net.polyglot.config.ID
import cn.net.polyglot.config.KEYWORD
import cn.net.polyglot.config.LOGIN
import cn.net.polyglot.config.MESSAGE
import cn.net.polyglot.config.NICKNAME
import cn.net.polyglot.config.PASSWORD
import cn.net.polyglot.config.PASSWORD2
import cn.net.polyglot.config.REGISTER
import cn.net.polyglot.config.REQUEST
import cn.net.polyglot.config.RESPONSE
import cn.net.polyglot.config.SEARCH
import cn.net.polyglot.config.SUBTYPE
import cn.net.polyglot.config.TEXT
import cn.net.polyglot.config.TIME
import cn.net.polyglot.config.TO
import cn.net.polyglot.config.TYPE
import cn.net.polyglot.config.USER
import cn.net.polyglot.module.inNextYear
import cn.net.polyglot.verticle.friend.FriendVerticle
import cn.net.polyglot.verticle.im.IMServletVerticle
import cn.net.polyglot.verticle.im.IMTcpServerVerticle
import cn.net.polyglot.verticle.message.MessageVerticle
import cn.net.polyglot.verticle.search.SearchVerticle
import cn.net.polyglot.verticle.user.UserVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.delay
import org.junit.*
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File.separator
import java.text.SimpleDateFormat
import java.util.Date

@RunWith(VertxUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//按照名字升序执行代码
class IMServerTest : AbstractIntegrationTest(vertx, config) {
  companion object {
    private val config = defaultTestConfig()
    private val vertx = Vertx.vertx()

    @BeforeClass
    @JvmStatic
    fun `before class`(context: TestContext) = runBlockingUnit {
      if (vertx.fileSystem().existsBlocking(config.getString(DIR)))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString(DIR), true)

      val option = deploymentOptionsOf(config = config)
      listOf(
        MessageVerticle::class.java,
        FriendVerticle::class.java,
        UserVerticle::class.java,
        SearchVerticle::class.java,
        WebServerVerticle::class.java,
        IMTcpServerVerticle::class.java,
        IMServletVerticle::class.java
      ).forEach {
        vertx.deployVerticle(it.name, option).await()
      }
    }

    @AfterClass
    @JvmStatic
    fun `after class`(context: TestContext) {
      if (vertx.fileSystem().existsBlocking(config.getString(DIR)))
        vertx.fileSystem().deleteRecursiveBlocking(config.getString(DIR), true)

      vertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun `test account register`() = runBlockingUnit {
    putJson(jsonRequest(USER, REGISTER)) {
      put(ID, "zxj2017")
      put(NICKNAME, "哲学家")
      put(PASSWORD, "431fe828b9b8e8094235dee515562247")
      put(PASSWORD2, "431fe828b9b8e8094235dee515562247")
    }.assertResponse {
      getBoolean(REGISTER)
    }

    putJson(jsonRequest(USER, REGISTER)) {
      put(ID, "yangkui")
      put(PASSWORD, "431fe828b9b8e8094235dee515562248")
      put(PASSWORD2, "431fe828b9b8e8094235dee515562248")
    }.assertResponse {
      getBoolean(REGISTER)
    }

    putJson(jsonRequest(USER, REGISTER)) {
      put(ID, "zhaoce")
      put(PASSWORD, "431fe828b9b8e8094235dee515562248")
      put(PASSWORD2, "431fe828b9b8e8094235dee515562248")
    }.assertResponse {
      getBoolean(REGISTER)
    }
  }

  @Test
  fun `test login`() = runBlockingUnit {
    putJson(jsonRequest(USER, LOGIN)) {
      put(ID, "zxj2017")
      put(PASSWORD, "431fe828b9b8e8094235dee515562247")
    }.assertResponse {
      getBoolean(LOGIN)
    }
  }

  @Test
  fun `test login fail`() = runBlockingUnit {
    putJson(jsonRequest(USER, LOGIN)) {
      put(ID, "zxj2017")
      put(PASSWORD, "431fe828b9b8e8094235dee515562246")
    }.assertResponse {
      !getBoolean(LOGIN)
    }
  }

  @Test
  fun `test search`() = runBlockingUnit {
    putJson(jsonRequest(SEARCH, null, null)) {
      put(KEYWORD, "zxj2017")
    }.apply {
      assertTrue(toJsonObject().getBoolean(SEARCH))
      assertNotNull(toJsonObject().getJsonObject(USER))
    }
  }

  @Test
  fun `test friend request`() {
    runBlockingUnit {
      //user:yangkui login
      writeToSocket(jsonRequest(LOGIN)) {
        put(ID, "yangkui")
        put(PASSWORD, "431fe828b9b8e8094235dee515562248")
      }

      //now send the friend request to the user:yangkui
      delay(100)//延迟一下，防止发得太快，前面的tcp代码来不及登陆
      putJson(jsonRequest(FRIEND, REQUEST)) {
        put(ID, "zxj2017")
        put(PASSWORD, "431fe828b9b8e8094235dee515562247")
        put(TO, "yangkui")
      }

      delay(100)
    }

    assertTrue(vertx.fileSystem().existsBlocking(config.getString(DIR) + separator + "zxj2017" + separator + ".send" + separator + "yangkui.json"))
    assertTrue(vertx.fileSystem().existsBlocking(config.getString(DIR) + separator + "yangkui" + separator + ".receive" + separator + "zxj2017.json"))
  }

  @Test
  fun `test friend response`() = runBlockingUnit {
    //user:zxj2017 login
    writeToSocket(jsonRequest(LOGIN)) {
      put(ID, "zxj2017")
      put(PASSWORD, "431fe828b9b8e8094235dee515562247")
    }

    //now send the frien request to the user:zxj2017
    delay(100)
    putJson(jsonRequest(FRIEND, RESPONSE)) {
      put(ID, "yangkui")
      put(PASSWORD, "431fe828b9b8e8094235dee515562248")
      put(TO, "zxj2017")
      put(ACCEPT, true)
    }
  }

  @Test
  fun `test messaging`() = runBlockingUnit {
    //user:zxj2017 login
    writeToSocket(jsonRequest(LOGIN)) {
      put(ID, "zxj2017")
      put(PASSWORD, "431fe828b9b8e8094235dee515562247").toString().plus(END)
    }

    //now send the message to the user:zxj2017
    delay(100)

    putJson(jsonRequest(MESSAGE, TEXT)) {
      put(ID, "yangkui")
      put(PASSWORD, "431fe828b9b8e8094235dee515562248")
      put(TO, "zxj2017")
      put(MESSAGE, "hi")
    }

    putJson(jsonRequest(MESSAGE, TEXT)) {
      put(ID, "yangkui")
      put(PASSWORD, "431fe828b9b8e8094235dee515562248")
      put(TO, "zxj2017")
      put(MESSAGE, "hello")
    }.assertResponse {
      getBoolean(MESSAGE)
    }
  }

  private suspend fun assertFriendHistory(expectedHistorySize: Int, date: String? = null) {
    putJson(jsonRequest(MESSAGE, HISTORY)) {
      put(ID, "yangkui")
      put(PASSWORD, "431fe828b9b8e8094235dee515562248")
      put(FRIEND, "zxj2017")
      date?.also { put(DATE, date) }
    }.apply {
      assertTrue(toJsonObject().getJsonArray(HISTORY).size() == expectedHistorySize)
    }
  }

  @Test
  fun `test messaging history`() = runBlockingUnit {
    val path0 = "${config.getString(DIR)}${separator}zxj2017${separator}yangkui${separator}2000${separator}01$separator"
    val path1 = "${config.getString(DIR)}${separator}yangkui${separator}zxj2017${separator}2000${separator}01$separator"
    vertx.fileSystem().mkdirs(path0).await()
    vertx.fileSystem().mkdirs(path1).await()
    vertx.fileSystem().createFile(path0 + "01.jsons").await()
    vertx.fileSystem().createFile(path1 + "01.jsons").await()
    val msg = JsonObject().put(TYPE, MESSAGE).put(SUBTYPE, TEXT)
      .put(ID, "zxj2017").put(TO, "yangkui").put(MESSAGE, "hi")
      .put(DATE, "2000-01-01").put(TIME, "00:00:00")
    vertx.fileSystem().writeFile(path0 + "01.jsons", msg.toBuffer()).await()
    vertx.fileSystem().writeFile(path1 + "01.jsons", msg.toBuffer()).await()

    assertFriendHistory(3)
    assertFriendHistory(3, SimpleDateFormat("yyyy-MM-dd").format(Date().inNextYear()))
    assertFriendHistory(1, SimpleDateFormat("yyyy-MM-dd").format(Date()))
    assertFriendHistory(0, "1999-12-31")
  }
}
