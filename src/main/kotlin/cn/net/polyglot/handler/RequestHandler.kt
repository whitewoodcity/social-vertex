@file:JvmName("RequestHandler")

package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.config.NumberConstants
import cn.net.polyglot.config.TypeConstants
import cn.net.polyglot.utils.putNullable
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

fun Message<JsonObject>.handleEventBus(vertx: Vertx) {
  val fs = vertx.fileSystem()
  val json = this.body()

  val type = json.getString("type", "")
  val version = json.getDouble("version", NumberConstants.CURRENT_VERSION)

  when (type) {
    TypeConstants.MESSAGE -> handleMessage(fs, json)
    TypeConstants.SEARCH -> handleSearch(fs, json)
    TypeConstants.FRIEND -> handleFriend(fs, json)
    TypeConstants.LOGIN -> handleLogin(fs, json)
    else -> {
      defaultMessage(fs, json)
    }
  }
}

fun Message<JsonObject>.handleMessage(fs: FileSystem, json: JsonObject) {
  val from = json.getString("from")
  val to = json.getString("to")
  val body = json.getString("body")
//    fs.readFile(".social-vertex/message.json") { resBuffer ->
//      if (resBuffer.succeeded()) {
//        // TODO
//      } else {
//        json.putNull("user")
//      }
  this.reply(json)
//    }
}

fun Message<JsonObject>.handleSearch(fs: FileSystem, json: JsonObject) {
  fs.readFile(".social-vertex/user.json") { resBuffer ->
    if (resBuffer.succeeded()) {
      val resJson = resBuffer.result().toJsonObject()
      val user = json.getString("user")
      val users = resJson.getJsonArray("users")
      val ret = users.asSequence()
        .map { it as JsonObject }
        .find { it.getString("id") == user }
      json.putNullable("user", ret)
    } else {
      json.putNull("user")
    }
    this.reply(json)
  }
}

fun Message<JsonObject>.handleFriend(fs: FileSystem, json: JsonObject) {
  val action = json.getString("action")
  val from = json.getString("from")
  val to = json.getString("to")
  when (action) {
    ActionConstants.DELETE -> {
      fs.readFile(".social-vertex/friend.json") { resBuffer ->
        if (resBuffer.succeeded()) {
          val ret = JsonObject()
          json.putNullable("user", ret)
        } else {
          json.putNull("user")
        }
      }
      json.put("info", "删除好友 $to")
    }

  // request to be friends
    ActionConstants.REQUEST -> {
      json.put("info", "请求信息已发送")
    }

  // reply whether to accept the request
    ActionConstants.RESPONSE -> {
      val accept = json.getBoolean("accept")
      val info = accept.run {
        if (this) "对方已接收您的好友请求"
        else "对方拒绝了您的好友请求"
      }
      json.put("info", info)
    }
  }
  this.reply(json)
}

fun Message<JsonObject>.handleLogin(fs: FileSystem, json: JsonObject) {
  val id = json.getString("id")
  val crypto = json.getString("crypto")
  json.removeAll { it.key in arrayOf("crypto") }

  fs.readFile(".social-vertex/user.json") { resBuffer ->
    if (resBuffer.succeeded()) {
      val resJson = resBuffer.result().toJsonObject()

      val users = resJson.getJsonArray("users")
      val ret = users.asSequence()
        .map { it as JsonObject }
        .find { it.getString("id") == id && it.getString("crypto") == crypto }
      json.putNullable("user", ret?.apply { remove("crypto") })
    } else {
      json.putNull("user")
    }
    this.reply(json)
  }
  this.reply(json)
}


fun Message<JsonObject>.defaultMessage(fs: FileSystem, json: JsonObject) {
  json.removeAll { it.key !in arrayOf("version", "type") }
  json.put("info","Default info, please check all sent value is correct.")
  this.reply(json)
}
