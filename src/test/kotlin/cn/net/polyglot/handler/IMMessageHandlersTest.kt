package cn.net.polyglot.handler

import io.vertx.core.Vertx
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * print Error Level for json input, and output the result then assert some keys.
 * @property vertx Vertx
 * @property fs FileSystem
 */
@Deprecated("")
@RunWith(VertxUnitRunner::class)
class IMMessageHandlersTest {
  lateinit var vertx: Vertx
  lateinit var fs: FileSystem

  @Before
  fun before() {
    vertx = Vertx.vertx()
    fs = vertx.fileSystem()
  }

  @Test
  fun testMessageCrossDomain(context: TestContext) {
    val json = JsonObject()
      .put("type", "message")
      .put("from", "inquiry@polyglot.net.cn")
      .put("to", "customer@w2v4.com")
      .put("body", "你好吗？")
      .put("version", 0.1)
    System.err.println(json)
//    val ret = message(fs, json)
//    println(ret)
//    assert(ret.getString("info") == "send message to other domain")
  }

  @Test
  fun testUserLogin(context: TestContext) {
    val fs = vertx.fileSystem()
    val json = JsonObject("""
{
"type":"user",
"action":"login",
"user":"zxj@polyglot.net.cn",
"crypto":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
""")
    System.err.println(json)
//    val ret = user(fs, json)
//    println(ret)
//    assert(ret.containsKey("login"))
  }

  @Test
  fun testUserRegister(context: TestContext) {
    val randomName = Random().ints(5).map { Math.abs(it) % 25 + 97 }.toArray().map { it.toChar() }.joinToString("")
    val json = JsonObject("""{
"type":"user",
"action":"register",
"user":"$randomName",
"crypto":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
""")
    System.err.println(json)
//    val ret = user(fs, json)
//    println(ret)
//    assert(ret.containsKey("register"))
  }

  @Test
  fun testHandleFriendRequest(context: TestContext) {
    val json = JsonObject("""{
"type":"friend",
"action":"request",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"message":"请添加我为你的好友，我是哲学家",
"version":0.1
}""")
    System.err.println(json)
//    val ret = friend(fs, json)
//    println(ret)
//    assert(ret.containsKey("info"))
  }


  @Test
  fun testHandleFriendResponse(context: TestContext) {
    val json = JsonObject("""{
"type":"friend",
"action":"response",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"accept":true,
"version":0.1
}""")
    System.err.println(json)
//    val ret = friend(fs, json)
//    println(ret)
//    assert(ret.containsKey("info"))
  }


  @Test
  fun testHandleFriendDelete(context: TestContext) {
    val json = JsonObject("""{
"type":"friend",
"action":"delete",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"version":0.1
}""")
    System.err.println(json)
//    val ret = friend(fs, json)
//    println(ret)
//    assert(ret.containsKey("info"))
  }

  @After
  fun after() {
    vertx.close()
  }
}
