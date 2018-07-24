package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.config.FileSystemConstants.FRIENDS
import cn.net.polyglot.config.JsonKeys
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import java.io.File

/**
 * @author zxj5470
 * @date 2018/7/11
 */

/**
 * load friend list when login. @see [handleFriendList]
 * @param fs FileSystem
 * @param json JsonObject
 * @param userFile String
 * @param id String?
 * @param crypto String?
 * @param loginTcpAction () -> Unit
 * @return JsonObject
 */
fun handleUserLogin(fs: FileSystem, json: JsonObject, userFile: String, id: String?, crypto: String?, loginTcpAction: () -> Unit): JsonObject {
  try {
    val resJson = fs.readFileBlocking(userFile).toJsonObject()
    if (authorizeLogin(resJson, id, crypto)) {
      val userJson = JsonObject(mapOf(
        "id" to id
      ))
      json.put("user", userJson)
      handleFriendList(fs, json, id)
      json.put("login", true)
      loginTcpAction()
    } else {
      json.put("info", "密码不正确")
      json.put("login", false)
    }
  } catch (e: Exception) {
    e.printStackTrace()
    json.putNull("user")
    json.put("info", "the user $id not exists")
    json.put("login", false)
  } finally {
    json.remove(JsonKeys.CRYPTO)
    return json
  }
}

fun handleUserRegister(fs: FileSystem, json: JsonObject, userFile: String, id: String?, userDir: String): JsonObject {
  if (fs.existsBlocking(userFile)) {
    registerDefaultFailedJson(json)
  } else {
    try {
      fs.mkdirsBlocking(userDir)
      fs.writeFileBlocking(userFile, json.toBuffer())
      // after write
      json.remove(JsonKeys.CRYPTO)
      val friendDir = userDir + File.separator + FRIENDS
      fs.mkdirBlocking(friendDir)
      json.put("info", "注册成功")
      json.put(ActionConstants.REGISTER, true)
    } catch (e: Exception) {
      println("cannot mkdir $userDir")
      json.put("info", "cannot mkdir")
    }
  }
  return json
}

fun String.checkIdValid(): Boolean {
  if (this.length < 4) return false
  if (this[0].isDigit()) return false
  return this.all { it.isLetterOrDigit() or (it in arrayOf('.', '@')) }
}

/**
 * check whether the length of md5sum is 32
 * @receiver String
 * @return Boolean
 */
fun String.checkCryptoValid(): Boolean {
  return this.length == 32
}

/**
 * check id and password correct
 * @param resJson JsonObject
 * @param id String?
 * @param crypto String?
 * @return Boolean
 */
private fun authorizeLogin(resJson: JsonObject, id: String?, crypto: String?) =
  resJson.getString("user") == id &&
    resJson.getString("crypto") == crypto

private fun registerDefaultFailedJson(json: JsonObject) {
  json.remove(JsonKeys.CRYPTO)
  json.put(ActionConstants.REGISTER, false)
}
