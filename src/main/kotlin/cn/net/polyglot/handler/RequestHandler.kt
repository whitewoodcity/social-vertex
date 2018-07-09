@file:JvmName("RequestHandler")

package cn.net.polyglot.handler

import cn.net.polyglot.config.TypeConstants
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

fun handle(buffer: Buffer): String {
  return try {
    handle(String(buffer.bytes).toJsonObject())
  } catch (e: Exception) {
    """{"message":"json format error"}"""
  }
}

private fun String.toJsonObject() = JsonObject(this)

fun handle(json: JsonObject): String {
  val type = json.getString("type", "")
  return when (type) {
    TypeConstants.MESSAGE -> handleMessage(json)
    TypeConstants.SEARCH -> handleSearch(json)
    TypeConstants.FRIEND -> handleFriend(json)
    else -> {
      defaultMessage(json)
    }
  }
}

fun handleMessage(json: JsonObject): String {
  return json.toString()
}

fun handleSearch(json: JsonObject): String {
  json.put("action", "response")
  json.put("user", "null")
  return json.toString()
}

fun handleFriend(json: JsonObject): String {
  return json.toString()
}

fun defaultMessage(json: JsonObject): String {
  return json.toString()
}
