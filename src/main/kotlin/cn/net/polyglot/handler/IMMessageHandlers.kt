package cn.net.polyglot.handler

import cn.net.polyglot.config.JsonKeys
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

fun defaultMessage(fs: FileSystem, json: JsonObject): JsonObject {
  json.removeAll { it.key !in arrayOf(JsonKeys.VERSION, JsonKeys.TYPE) }
  json.put(JsonKeys.INFO, "Default info, please check all sent value is correct.")
  return json
}
