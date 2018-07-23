package cn.net.polyglot.utils

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.Json
import io.vertx.kotlin.core.json.array

/**
 * @author zxj5470
 * @date 2018/7/16
 */
/**
 * Because Vert.x JsonObject can't put a nullable value directly.
 * @receiver JsonObject
 * @param key String
 * @param value Any?
 * @return JsonObject this
 */
fun JsonObject.putNullable(key: String, value: Any?): JsonObject {
  if (value == null) {
    if (this.getValue(key) != null) {
      this.remove(key)
    }
    this.putNull(key)
  } else this.put(key, value)
  return this
}

/**
 * just a wrapper for converting a series of JsonObjects to JsonArray
 * @receiver Iterable<JsonObject>
 * @return JsonArray
 */
fun Iterable<JsonObject>.toJsonArray() = Json.array(this)
