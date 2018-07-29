package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.config.JsonKeys
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

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

@Deprecated("unused")
private fun userExistBefore(json: JsonObject): JsonObject {
  json.remove(JsonKeys.CRYPTO)
  json.put(JsonKeys.INFO, "user has already existed.")
  json.put(ActionConstants.REGISTER, false)
  return json
}
