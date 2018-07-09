package cn.net.polyglot.yangkui

import io.vertx.core.Vertx
import io.vertx.core.file.FileSystem
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.unit.TestContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import test.MyVerticle

@RunWith(VertxUnitRunner::class)
class VertxTest {
  private lateinit var vertx: Vertx
  private lateinit var client: WebClient
  private lateinit var fileSystem:FileSystem
  @Before
  fun befor(context: TestContext) {
    vertx = Vertx.vertx()
    vertx.deployVerticle(MyVerticle::class.java.name)
    client = WebClient.create(vertx)
  }

  @Test
  fun test(context: TestContext) {
    var async = context.async()
    client.get(8085, "localhost", "/").send { response ->
      var result: String = response.result().bodyAsString()
      if (result.trim() == "move") {
          fileSystem = vertx.fileSystem()
          fileSystem.readFile("./file/test1.txt") {
            if (it.succeeded()) {
              println(it.result())
            } else {
              println("找不到文件/文件不存在")
            }
          }
        }
      async.complete()
      }

  }

  @After
  fun after(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())

  }

}
