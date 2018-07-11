package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants.*
import cn.net.polyglot.config.FileSystemConstants.USER_DIR
import cn.net.polyglot.utils.putNullable
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/11
 */

fun Message<JsonObject>.handleFriend(fs: FileSystem, json: JsonObject) {
  val action = json.getString("action")
  val from = json.getString("from")
  val to = json.getString("to")
  when (action) {
    DELETE -> handleFriendDelete(fs, json, from, to)

  // request to be friends
    REQUEST -> {
      json.put("info", "请求信息已发送")
      this.reply(json)
    }

  // reply whether to accept the request
    RESPONSE -> {
      val accept = json.getBoolean("accept")
      val info =
        if (accept) "对方已接收您的好友请求"
        else "对方拒绝了您的好友请求"
      json.put("info", info)
      this.reply(json)
    }
  }
  this.reply(json)
}

private fun Message<JsonObject>.handleFriendDelete(fs: FileSystem, json: JsonObject, from: String?, to: String?) {
  fs.readFile(USER_DIR) { resBuffer ->
    if (resBuffer.succeeded()) {
      val ret = JsonObject()
      json.putNullable("user", ret)
    } else {
      json.putNull("user")
    }
  }
  json.put("info", "$from 删除好友 $to")
  this.reply(json)
}
