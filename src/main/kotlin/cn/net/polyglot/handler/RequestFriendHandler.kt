package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants.*
import cn.net.polyglot.utils.getUser
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
    REQUEST -> handleFriendRequest(json)
  // reply whether to accept the request
    RESPONSE -> handleFriendResponse(json)
  }
}

private fun Message<JsonObject>.handleFriendDelete(fs: FileSystem, json: JsonObject, from: String?, to: String?) {
  val (userDir, _) = getUser(to)
  fs.exists(userDir) { it ->
    if (it.succeeded()) {
      if (it.result()) {
        json.put("info", "$from 删除好友 $to")
        this.reply(json)
      } else {
        json.put("info", "$to 用户不存在")
        json.putNull("user")

        println(json.toString())
        val ret = JsonObject(json.toString())
        this.reply(ret)
      }
    }else{
      json.put("info","failed")
      this.reply(json)
    }
  }

}

private fun Message<JsonObject>.handleFriendRequest(json: JsonObject) {
  json.put("info", "请求信息已发送")
  this.reply(json)
}

private fun Message<JsonObject>.handleFriendResponse(json: JsonObject) {
  val accept = json.getBoolean("accept")
  val info =
    if (accept) "对方已接收您的好友请求"
    else "对方拒绝了您的好友请求"
  json.put("info", info)
  this.reply(json)
}
