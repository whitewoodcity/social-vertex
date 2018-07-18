package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.config.FileSystemConstants.FRIENDS
import cn.net.polyglot.utils.getUserDirAndFile
import cn.net.polyglot.utils.mkdirsIfNotExists
import cn.net.polyglot.utils.removeCrypto
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import java.io.File

/**
 * @author zxj5470
 * @date 2018/7/11
 */

/**
 * @receiver Message<JsonObject>
 * @param fs FileSystem
 * @param json JsonObject
 */
fun Message<JsonObject>.handleUser(fs: FileSystem, json: JsonObject) {
  val action = json.getString("action")

  val id = json.getString("user")
  val crypto = json.getString("crypto")
  handleUserCheckIdAndCrypto(id, json, crypto)

  val (userDir, userFile) = getUserDirAndFile(id)
  when (action) {
    ActionConstants.LOGIN -> handleUserLogin(fs, userFile, id, crypto, json)
    ActionConstants.REGISTRY -> handleUserRegistry(fs, userFile, json, id, userDir)
  }
}

private fun Message<JsonObject>.handleUserLogin(fs: FileSystem, userFile: String, id: String?, crypto: String?, json: JsonObject) {
  fs.readFile(userFile) {
    if (it.succeeded()) {
      val resJson = it.result().toJsonObject()
      if (resJson.getString("user") == id &&
        resJson.getString("crypto") == crypto) {
        val userJson = JsonObject(mapOf(
          "id" to id
        ))
        json.put("user", userJson)
        json.put("login", true)
      } else {
        json.put("login", false)
      }
    } else {
      // not succeed means the file not exists
      json.putNull("user")
      json.put("info", "the user $id not exists")
    }
    json.removeCrypto()
    this.reply(json)
  }
}

private fun Message<JsonObject>.handleUserRegistry(fs: FileSystem, userFile: String, json: JsonObject, id: String?, userDir: String) {
  fs.exists(userFile) {
    // if exists then failed
    if (it.result()) {
      json.put("info", "user $id already exists")
      registryDefaultFailed(json)
    } else {
      fs.mkdirsIfNotExists(userDir,
        fail = {
          println("cannot mkdir $userDir")
          json.put("info", "cannot mkdir")
          registryDefaultFailed(json)
        },
        success = {
          fs.writeFile(userFile, json.toBuffer()) {
            if (it.succeeded()) {
              json.removeCrypto()
              val friendDir = userDir + File.separator + FRIENDS
              fs.mkdir(friendDir) {
                if (it.succeeded()) {
                  json.put("info", "注册成功")
                  json.put(ActionConstants.REGISTRY, true)
                  this.reply(json)
                }
              }
            }
          }
        }
      )
    }
  }
}

fun Message<JsonObject>.registryDefaultFailed(json: JsonObject) {
  registryDefaultFailedJson(json)
  this.reply(json)
}

private fun registryDefaultFailedJson(json: JsonObject) {
  json.removeCrypto()
  json.put(ActionConstants.REGISTRY, false)
}

fun String.checkIdValid(): Boolean {
  if (this.length < 4) return false
  if (this[0].isDigit()) return false
  return this.all { it.isLetterOrDigit() or (it in arrayOf('.', '@')) }
}

fun String.checkCryptoValid(): Boolean {
  return this.length == 32
}

/**
 * if it is necessary
 * @receiver Message<JsonObject> ...
 * @param id String json.getString("user")
 * @param json JsonObject
 * @param crypto String
 */
fun Message<JsonObject>.handleUserCheckIdAndCrypto(id: String, json: JsonObject, crypto: String) {
  if (!id.checkIdValid()) {
    json.put("info", "用户名格式错误")
    registryDefaultFailed(json)
  }
  if (!crypto.checkCryptoValid()) {
    json.put("info", "秘钥错误")
    registryDefaultFailed(json)
  }
}


fun handleUserLogin(fs: FileSystem, userFile: String, id: String?, crypto: String?, json: JsonObject): JsonObject {
  try {
    val resJson = fs.readFileBlocking(userFile).toJsonObject()
    if (resJson.getString("user") == id &&
      resJson.getString("crypto") == crypto) {
      val userJson = JsonObject(mapOf(
        "id" to id
      ))
      json.put("user", userJson)
      json.put("login", true)
    } else {
      json.put("login", false)
    }
  } catch (e: Exception) {
    json.putNull("user")
    json.put("info", "the user $id not exists")
  } finally {
    return json
  }
}

fun handleUserRegistry(fs: FileSystem, userFile: String, json: JsonObject, id: String?, userDir: String): JsonObject {
  if (fs.existsBlocking(userFile)) {
    registryDefaultFailedJson(json)
  } else {
    try {
      fs.mkdirsBlocking(userDir)
      fs.writeFileBlocking(userFile, json.toBuffer())
      // after write
      json.removeCrypto()
      val friendDir = userDir + File.separator + FRIENDS
      fs.mkdirBlocking(friendDir)
      json.put("info", "注册成功")
      json.put(ActionConstants.REGISTRY, true)
    } catch (e: Exception) {
      println("cannot mkdir $userDir")
      json.put("info", "cannot mkdir")
    }
  }
  return json
}
