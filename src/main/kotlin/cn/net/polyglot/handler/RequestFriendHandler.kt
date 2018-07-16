package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants.*
import cn.net.polyglot.config.FileSystemConstants.FRIENDS
import cn.net.polyglot.config.FileSystemConstants.USER_FILE
import cn.net.polyglot.utils.contains
import cn.net.polyglot.utils.getUserDirAndFile
import cn.net.polyglot.utils.mkdirsIfNotExists
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.JsonArray
import java.io.File


/**
 * @author zxj5470
 * @date 2018/7/11
 */

fun Message<JsonObject>.handleFriend(fs: FileSystem, json: JsonObject) {
  val action = json.getString("action")
  val from = json.getString("from")
  val to = json.getString("to")
  checkFromValid(json)
  when (action) {
    DELETE -> handleFriendDelete(fs, json, from, to)
//   request to be friends
    REQUEST -> handleFriendRequest(json)
//   reply whether to accept the request
    RESPONSE -> handleFriendResponse(fs, json, from, to)
//    list friends
    LIST -> handleFriendList(fs, json, from, to)
  }
}

private fun Message<JsonObject>.checkFromValid(json: JsonObject) {
  if ("from" !in json) {
    json.put("info", "failed for lack of key `from` ")
    this.reply(json)
  }
}

private fun Message<JsonObject>.handleFriendDelete(fs: FileSystem, json: JsonObject, from: String?, to: String?) {
  val (userDir, _) = getUserDirAndFile(to)
  fs.exists(userDir) { it ->
    if (it.succeeded()) {
      if (it.result()) {
        json.put("info", "$from 删除好友 $to")
        this.reply(json)
      } else {
        json.put("info", "$to 用户不存在")
        json.putNull("user")
        this.reply(json)
      }
    } else {
      json.put("info", "failed")
      this.reply(json)
    }
  }

}

private fun Message<JsonObject>.handleFriendRequest(json: JsonObject) {
  json.put("info", "请求信息已发送")
  this.reply(json)
}

/**
 * 好友请求对方接受后在服务器端添加数据, 以便 list 时获取
 * @receiver Message<JsonObject>
 * @param fs FileSystem
 * @param json JsonObject
 */
private fun Message<JsonObject>.handleFriendResponse(fs: FileSystem, json: JsonObject, from: String?, to: String?) {
  val accept = json.getBoolean("accept")
  checkAcceptKey(accept, json)

  val group = json.getString("group") ?: "我的好友"
  val info =
    if (accept) "对方已接受您的好友请求"
    else "对方拒绝了您的好友请求"
  json.put("info", info)
  // 添加接受者的好友
  val friendDirFromTo = getFriendsDir(from, to)
  if (accept) {
    fs.mkdirsIfNotExists(friendDirFromTo, fail = {
      System.err.println("mkdir failed")
      json.put("status", 1)
      this.reply(json)
    }, success = {
      val filePath = friendDirFromTo + File.separator + USER_FILE
      val writeJson = JsonObject().apply {
        put("id", to)
        put("nickname", to)
        put("group", group)
      }
      fs.writeFile(filePath, writeJson.toBuffer()) {
        if (it.succeeded()) {
          json.put("status", 0)
        }
        this.reply(json)
      }
    })
  }
}

private fun Message<JsonObject>.checkAcceptKey(accept: Boolean?, json: JsonObject) {
  if (accept == null) {
    json.put("info", "参数格式错误")
    this.reply(json)
  }
}


private fun Message<JsonObject>.handleFriendList(fs: FileSystem, json: JsonObject, from: String?, to: String?) {
  val (userDir, _) = getUserDirAndFile(from)
  val friendDir = userDir + File.separator + FRIENDS

  fun getFiles(): Iterable<Any> {
//    val friendList = Collections.synchronizedList(emptyList<String>())
    fs.readDir(friendDir) {
      if (it.succeeded()) {
        val files = it.result()
        val friendUserFiles = files.map { it + File.separator + USER_FILE }
        val buffers = friendUserFiles.map { Future.future<Buffer>() }
        friendUserFiles.forEachIndexed { index, s ->
          fs.readFile(s, buffers[index].completer())
        }
        CompositeFuture.all(buffers).setHandler { ar ->
          if (ar.succeeded()) {
            val res = ar.result()
            val results = res.list<Buffer>()
              .asSequence()
              .map { it.toJsonObject() }
              .map {
                JsonObject(mapOf(
                  "id" to it.getString("to"),
                  "name" to (it.getString("name") ?: it.getString("id")),
                  "group" to it.getString("group")
                ))
              }.toList().toTypedArray()
            json.put("results", JsonArray(*results))
            this.reply(json)
          }
        }
      }
    }
    return emptyList()
  }

  fun Any.readToJson() {
    TODO()
  }
  getFiles().forEach {
    it.readToJson()
  }

}

private fun getFriendsDir(from: String?, to: String?): String {
  val (userDir, _) = getUserDirAndFile(from)
  val friendDir = userDir + File.separator + FRIENDS
  return friendDir + File.separator + to
}



