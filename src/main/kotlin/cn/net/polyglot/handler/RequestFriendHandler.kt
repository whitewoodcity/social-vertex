package cn.net.polyglot.handler

import cn.net.polyglot.config.FileSystemConstants.FRIENDS
import cn.net.polyglot.config.FileSystemConstants.USER_FILE
import cn.net.polyglot.utils.getDirFromUserToFriendDIr
import cn.net.polyglot.utils.getUserDirAndFile
import cn.net.polyglot.utils.putNullable
import cn.net.polyglot.utils.toJsonArray
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import java.io.File
import java.io.IOException

/**
 * @author zxj5470
 * @date 2018/7/11
 */

/**
 *
 * @param fs FileSystem
 * @param json JsonObject
 * @param from String?
 * @param to String?
 * @param deletedFriendSide () -> Unit, which we can delete each other
 * @return JsonObject
 */
fun handleFriendDelete(fs: FileSystem, json: JsonObject, from: String?, to: String?, deletedFriendSide: () -> Unit = {}): JsonObject {
  if (from == null || to == null) {
    json.put("info", "failed")
    return json
  }
  val toUserDir = getDirFromUserToFriendDIr(from, to)
  val exist = fs.existsBlocking(toUserDir)
  if (exist) {
    try {
      fs.deleteRecursiveBlocking(toUserDir, true)
      json.put("info", "$from 删除好友 $to")
      deletedFriendSide()
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

fun handleFriendRequest(fs: FileSystem, json: JsonObject, action: () -> Unit = {}): JsonObject {
  json.put("info", "请求信息已发送")
  action()
  return json
}

fun handleFriendResponse(fs: FileSystem, json: JsonObject, from: String?, to: String?, config: JsonObject): JsonObject {
  val target = ArrayList<JsonObject>()
  target.add(0, JsonObject().put("id", "$to"))
  target.add(1, JsonObject().put("id", "$from"))
  val result = JsonObject()
    .put("type", "propel")
    .put("action", "inform")
    .put("version", "1.0")
    .put("target", JsonArray(target))
  val accept = json.getBoolean("accept")
  if (accept) {
    val dir = config.getString("dir") + File.separator + "user"
    val fromDir = dir + File.separator + from + File.separator + "friends" + File.separator + to
    val toDir = dir + File.separator + to + File.separator + "friends" + File.separator + from
    try {
      if (!fs.existsBlocking(fromDir) && !fs.existsBlocking(toDir)) {

        fs.mkdirsBlocking(toDir)
        fs.mkdirsBlocking(fromDir)
        fs.createFileBlocking(fromDir + File.separator + "$to.json")
          .writeFileBlocking(fromDir + File.separator + "$to.json", JsonObject()
            .put("id", "$to")
            .put("nickname", "$to").toBuffer())
        fs.createFileBlocking(toDir + File.separator + "$from.json")
          .writeFileBlocking(toDir + File.separator + "$from.json", JsonObject()
            .put("id", "$from")
            .put("nickname", "$from").toBuffer())
      } else {
        result.put("info", "不允许重复添加")
        return result
      }
    } catch (e: IOException) {
      result.put("info", "Server error！")
      return result
    }
    result.put("info", "我们已经是好友开始交谈吧！")
    return result


  } else {

    return result.put("info", "对方拒绝加你为好友")

  }

//  if (accept == null || from == null || to == null) {
//    json.put("info", "参数格式错误")
//    return json
//  }
//
//  val group = json.getString("group") ?: "我的好友"
//  val info =
//    if (accept) "对方已接受您的好友请求"
//    else "对方拒绝了您的好友请求"
//  json.put("info", info)
//
//  val fromUserDirToFriendDir = getDirFromUserToFriendDIr(from, to)
//  if (accept) {
//    try {
//      // if `from` user doesn't exist, it will be failed.
//      val fromDir = getUserDirAndFile(from).first
//      if (!fs.existsBlocking(fromDir)) {
//        json.put("info", "$from user doesn't exist.")
//        json.put("status", 1)
//        return json
//      }
//
//      fs.mkdirsBlocking(fromUserDirToFriendDir)
//      val filePath = fromUserDirToFriendDir + File.separator + USER_FILE
//      val writeJson = JsonObject().apply {
//        put("id", to)
//        put("nickname", to)
//        put("group", group)
//      }
//      fs.writeFileBlocking(filePath, writeJson.toBuffer())
//      json.put("status", 0)
//    } catch (e: Exception) {
//      json.put("status", 1)
//      return json
//    }
//  } else {
//    // rejection is included in `accept` => `info`
//  }
//  return json
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
    val groupByToJsonArray =
      friendUserFiles
        .map {
          fs.readFileBlocking(it).toJsonObject()
        }.groupBy {
          it.getString("group", "我的好友")
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
