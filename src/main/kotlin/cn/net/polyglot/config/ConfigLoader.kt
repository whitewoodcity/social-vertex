package cn.net.polyglot.config

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.config.ConfigStoreOptions
import io.vertx.kotlin.config.ConfigRetrieverOptions
import io.vertx.kotlin.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/8
 */
val fileStore = ConfigStoreOptions(
  type = "file",
  format = "json",
  config = JsonObject(
    "path" to "config.json"
  ))

val options = ConfigRetrieverOptions(stores = arrayListOf(fileStore))

const val DEFAULT_PORT = 8080
val defaultJsonObject = JsonObject(
  "port" to DEFAULT_PORT
)


private fun checkPortValid(port: Int): Boolean {
  return port in 1..65535
}

fun checkPortValidFromConfig(config: JsonObject): Boolean {
  return try {
    config.getInteger("port").let { checkPortValid(it) }
  } catch (e: Exception) {
    false
  }
}

/**
 * port++
 * @param config JsonObject
 */
fun portInc(config: JsonObject) {
  val port = config.getInteger("port", DEFAULT_PORT) + 1
  config.put("port", port)
}
