package cn.net.polyglot.handler

import cn.net.polyglot.config.FileSystemConstants.FRIENDS
import cn.net.polyglot.config.FileSystemConstants.USER_FILE
import cn.net.polyglot.utils.getDistFromUserToDist
import cn.net.polyglot.utils.getUserDirAndFile
import cn.net.polyglot.utils.putNullable
import cn.net.polyglot.utils.toJsonArray
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import java.io.File


/**
 * @author zxj5470
 * @date 2018/7/11
 */

fun handleFriendDelete(fs: FileSystem, json: JsonObject, from: String?, to: String?): JsonObject {
  if (from == null || to == null) {
    json.put("info", "failed")
    return json
  }
  val toUserDir = getDistFromUserToDist(from, to)
  val exist = fs.existsBlocking(toUserDir)
  println(toUserDir)
  if (exist) {
    try {
      fs.deleteRecursiveBlocking(toUserDir, true)
      json.put("info", "$from 删除好友 $to")
    } catch (e: Exception) {
      e.printStackTrace()
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
  if (accept == null || from == null || to == null) {
    json.put("info", "参数格式错误")
    return json
  }

  val group = json.getString("group") ?: "我的好友"
  val info =
    if (accept) "对方已接受您的好友请求"
    else "对方拒绝了您的好友请求"
  json.put("info", info)

  val friendDirFromTo = getDistFromUserToDist(from, to)
  if (accept) {
    try {
      // if `from` user doesn't exist, it will be failed.
      if (!fs.existsBlocking(getUserDirAndFile(from).first)) {
        json.put("info", "$from user doesn't exist.")
        json.put("status", 1)
        return json
      }

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
  } else {
    // rejection is included in `accept` info
  }
  return json
}

/**
 * get the result when send json with
 * ```
 * type == "friend",
 * action == "list"
 * ```
 * or get the result when user login.@see [handleUserLogin]
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
