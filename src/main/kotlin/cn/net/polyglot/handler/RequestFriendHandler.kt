package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants.DELETE
import cn.net.polyglot.config.ActionConstants.LIST
import cn.net.polyglot.config.ActionConstants.REQUEST
import cn.net.polyglot.config.ActionConstants.RESPONSE
import cn.net.polyglot.config.FileSystemConstants.FRIENDS
import cn.net.polyglot.config.FileSystemConstants.USER_FILE
import cn.net.polyglot.utils.*
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import java.io.File


/**
 * @author zxj5470
 * @date 2018/7/11
 */
@Deprecated("")
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

@Deprecated("")
private fun Message<JsonObject>.checkFromValid(json: JsonObject) {
  if ("from" !in json) {
    json.put("info", "failed for lack of key `from` ")
    this.reply(json)
  }
}

@Deprecated("")
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

@Deprecated("")
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
@Deprecated("")
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

@Deprecated("")
private fun Message<JsonObject>.handleFriendList(fs: FileSystem, json: JsonObject, from: String?, to: String?) {
  val (userDir, _) = getUserDirAndFile(from)
  val friendDir = userDir + File.separator + FRIENDS

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
          val groupByToJsonArray = res.list<Buffer>()
            .asSequence()
            .map { it.toJsonObject() }
            .groupBy { it.getString("group") }
            .map {
              JsonObject().apply {
                put("group", it.key)
                it.value.forEach { it.remove("group") }
                put("lists", it.value)
              }
            }.toJsonArray()
          json.put("results", groupByToJsonArray)
          this.reply(json)
        }
      }
    } else {
      json.put("info", "failed")
      this.reply(json)
    }
  }
}

private fun getFriendsDir(from: String?, to: String?): String {
  val (userDir, _) = getUserDirAndFile(from)
  val friendDir = userDir + File.separator + FRIENDS
  return friendDir + File.separator + to
}

@Deprecated("")
private fun Message<JsonObject>.checkAcceptKey(accept: Boolean?, json: JsonObject) {
  if (accept == null) {
    json.put("info", "参数格式错误")
    this.reply(json)
  }
}

fun handleFriendDelete(fs: FileSystem, json: JsonObject, from: String?, to: String?): JsonObject {
  val (userDir, _) = getUserDirAndFile(to)
  val exist = fs.existsBlocking(userDir)
  if (exist) {
    try {
      fs.deleteBlocking(userDir)
      json.put("info", "$from 删除好友 $to")
    } catch (e: Exception) {
      json.put("info", "failed")
      json.putNull("user")
    } finally {
      return json
    }
  } else {
    json.put("info", "$to 用户不存在")
    json.putNull("user")
    return json
  }
}

fun handleFriendRequest(fs: FileSystem, json: JsonObject): JsonObject {
  json.put("info", "请求信息已发送")
  // TODO: send request to receiver
  return json
}

fun handleFriendResponse(fs: FileSystem, json: JsonObject, from: String?, to: String?): JsonObject {
  val accept = json.getBoolean("accept")
  if (accept == null) {
    json.put("info", "参数格式错误")
    return json
  }

  val group = json.getString("group") ?: "我的好友"
  val info =
    if (accept) "对方已接受您的好友请求"
    else "对方拒绝了您的好友请求"
  json.put("info", info)
  // 添加接受者的好友
  val friendDirFromTo = getFriendsDir(from, to)
  if (accept) {
    try {
      fs.mkdirsBlocking(friendDirFromTo)
      val filePath = friendDirFromTo + File.separator + USER_FILE
      val writeJson = JsonObject().apply {
        put("id", to)
        put("nickname", to)
        put("group", group)
      }
      fs.writeFileBlocking(filePath, writeJson.toBuffer())
      json.put("status", 0)
    } catch (e: Exception) {
      json.put("status", 1)
      return json
    }
  }
  return json
}

/**
 * get the result when send json include
 * ```
 * {
 * "type":"friend",
 * "action":"list"
 * }
 * ```
 * or get the result when `login`
 * @param fs FileSystem
 * @param json JsonObject
 * @param from String?
 * @return JsonObject
 */
fun handleFriendList(fs: FileSystem, json: JsonObject, from: String?): JsonObject {
  val (userDir, _) = getUserDirAndFile(from)
  val friendDir = userDir + File.separator + FRIENDS

  try {
    val files = fs.readDirBlocking(friendDir)
    val friendUserFiles = files.map { it + File.separator + USER_FILE }
    val groupByToJsonArray = friendUserFiles.map {
      fs.readFileBlocking(it).toJsonObject()
    }.groupBy {
      it.getString("group")
    }.map {
      JsonObject().apply {
        put("group", it.key)
        it.value.forEach { it.remove("group") }
        put("lists", it.value)
      }
    }.toJsonArray()
    json.put("results", groupByToJsonArray)
    return json
  } catch (e: Exception) {
    e.printStackTrace()
    json.putNullable("info", e.message)
    return json
  }
}
