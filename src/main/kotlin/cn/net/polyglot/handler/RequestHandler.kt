@file:JvmName("RequestHandler")

package cn.net.polyglot.handler

import cn.net.polyglot.config.TypeConstants
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

fun handle(buffer: Buffer): String {
  return try {
    handle(buffer.toJsonObject())
  } catch (e: Exception) {
    """{"message":"json format error"}"""
  }
}

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
  TODO()
}

fun handleSearch(json: JsonObject): String {
  json.put("action", "response")
  json.put("user", "null")
  return json.toString()
}

fun handleFriend(json: JsonObject): String {
  TODO()
}

fun defaultMessage(json: JsonObject): String {
  return "NOTHING"
}
