package cn.net.polyglot.verticle

import cn.net.polyglot.config.DIR
import cn.net.polyglot.config.HOST
import cn.net.polyglot.config.HTTP_PORT
import cn.net.polyglot.config.SUBTYPE
import cn.net.polyglot.config.TCP_PORT
import cn.net.polyglot.config.TYPE
import cn.net.polyglot.config.VERSION
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import java.io.File
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// 否则可能被类型推断成有返回值的测试方法，从而被JUnit忽略
fun runBlockingUnit(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit) = runBlocking(context, block)

fun defaultTestConfig(): JsonObject = JsonObject()
  .put(VERSION, 0.1)
  .put(DIR, "${Paths.get("").toAbsolutePath()}${File.separator}social-vertex")
  .put(TCP_PORT, 7373)
  .put(HTTP_PORT, 7575)
  .put(HOST, "localhost")

fun HttpResponse<Buffer>.toJsonObject() = body().also { println(it) }.toJsonObject()

fun jsonRequest(type: String, subtype: String? = null, version: String? = "0.1") = JsonObject().apply {
  put(TYPE, type)
  subtype?.also { put(SUBTYPE, subtype) }
  version?.also { put(VERSION, version) }
}

abstract class AbstractIntegrationTest(val vertx: Vertx, val config: JsonObject) {
  val webClient = WebClient.create(vertx)
  val netClient = vertx.createNetClient()

  suspend fun putJson(jsonObject: JsonObject, block: JsonObject.() -> Unit) =
    webClient.put(config.getInteger(HTTP_PORT), "localhost", "/")
      .sendJsonObject(jsonObject.apply(block)).await()

  fun HttpResponse<Buffer>.assertResponse(block: JsonObject.() -> Boolean) {
    Assert.assertTrue(block(this.toJsonObject()))
  }

  suspend fun writeToSocket(jsonObject: JsonObject, block: JsonObject.() -> Unit) =
    netClient.connect(config.getInteger(TCP_PORT), config.getString(HOST)).await()
      .apply { handler { println(JsonObject(it.toString().trim())) } }
      .end(jsonObject.apply(block).toBuffer()).await()
}
