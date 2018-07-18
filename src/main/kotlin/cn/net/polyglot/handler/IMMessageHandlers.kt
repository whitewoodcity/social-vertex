package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants.*
import cn.net.polyglot.config.NumberConstants
import cn.net.polyglot.config.TypeConstants.*
import cn.net.polyglot.utils.contains
import cn.net.polyglot.utils.getUserDirAndFile
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

fun handleTypes(fs: FileSystem, json: JsonObject): JsonObject {
  val type = json.getString("type", "")
  val version = json.getDouble("version", NumberConstants.CURRENT_VERSION)
  return when (type) {
    MESSAGE -> message(fs, json)// TODO
    SEARCH -> searchUser(fs, json)// TODO
    FRIEND -> friend(fs, json)
    USER -> userAuthorize(fs, json)
    else -> defaultMessage(fs, json)
  }
}

fun userAuthorize(fs: FileSystem, json: JsonObject): JsonObject {
  val action = json.getString("action")

  val id = json.getString("user")
  val crypto = json.getString("crypto")
  fun handleUserCheckIdAndCrypto(id: String?, json: JsonObject, crypto: String?): Boolean {
    if (id == null || crypto == null) return false
    if (!id.checkIdValid()) {
      json.put("info", "用户名格式错误")
      return false
    }
    if (!crypto.checkCryptoValid()) {
      json.put("info", "秘钥错误")
      return false
    }
    return true
  }
  if (!handleUserCheckIdAndCrypto(id, json, crypto)) return json

  val (userDir, userFile) = getUserDirAndFile(id)
  return when (action) {
    LOGIN -> handleUserLogin(fs, userFile, id, crypto, json)
    REGISTRY -> handleUserRegistry(fs, userFile, json, id, userDir)
    else -> defaultMessage(fs, json)
  }
}


fun searchUser(fs: FileSystem, json: JsonObject): JsonObject {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun message(fs: FileSystem, json: JsonObject): JsonObject {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun friend(fs: FileSystem, json: JsonObject): JsonObject {
  val action = json.getString("action")
  val from = json.getString("from")
  val to = json.getString("to")
  fun checkFromValid(json: JsonObject): Boolean {
    if ("from" !in json) {
      json.put("info", "failed on account of key")
      return false
    }
    return true
  }
  if (!checkFromValid(json)) return json
  return when (action) {
    DELETE -> handleFriendDelete(fs, json, from, to)
//   request to be friends
    REQUEST -> handleFriendRequest(fs, json)
//   reply whether to accept the request
    RESPONSE -> handleFriendResponse(fs, json, from, to)
//    list friends
    LIST -> handleFriendList(fs, json, from, to)
    else -> defaultMessage(fs, json)
  }
}

fun defaultMessage(fs: FileSystem, json: JsonObject): JsonObject {
  json.removeAll { it.key !in arrayOf("version", "type") }
  json.put("info", "Default info, please check all sent value is correct.")
  return json
}
