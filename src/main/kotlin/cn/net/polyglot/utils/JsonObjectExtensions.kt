package cn.net.polyglot.utils

import cn.net.polyglot.config.JsonKeys
import io.vertx.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/16
 */
/**
 *
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

fun JsonObject.removeCrypto() = this.remove(JsonKeys.CRYPTO)

operator fun JsonObject.contains(key: String) = this.containsKey(key)
