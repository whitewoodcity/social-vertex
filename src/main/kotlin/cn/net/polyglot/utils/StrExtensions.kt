package cn.net.polyglot.utils

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/11
 */
/**
 * try to convert to JSON
 * @receiver String
 * @return JsonObject
 */
fun String.tryJson(): JsonObject? {
  return try {
    JsonObject(this)
  } catch (e: Exception) {
    null
  }
}

fun Buffer.text() = String(this.bytes)

fun String.checkValid(): Boolean {
  if (this.length < 4) return false
  if(this[0].isDigit()) return false
  return this.all { it.isLetterOrDigit()}
}
