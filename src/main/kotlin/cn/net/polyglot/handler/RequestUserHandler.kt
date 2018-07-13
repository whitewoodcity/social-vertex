package cn.net.polyglot.handler

import cn.net.polyglot.config.ActionConstants
import cn.net.polyglot.utils.getDirAndFile
import cn.net.polyglot.utils.mkdirIfNotExists
import cn.net.polyglot.utils.removeCrypto
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/11
 */

/**
 * registry
 * @receiver Message<JsonObject>
 * @param fs FileSystem
 * @param json JsonObject
 */
fun Message<JsonObject>.handleUser(fs: FileSystem, json: JsonObject) {
  val action = json.getString("action")

  val id = json.getString("user")
  val crypto = json.getString("crypto")
  handleUserCheckIdAndCrypto(id, json, crypto)

  val (userDir, userFile) = getDirAndFile(id)
  when (action) {
    ActionConstants.LOGIN -> handleUserLogin(fs, userFile, id, crypto, json)
    ActionConstants.REGISTRY -> handleUserRegistry(fs, userFile, json, id, userDir)
  }
}

/**
 * if it is necessary
 * @receiver Message<JsonObject>
 * @param id String json["user"]
 * @param json JsonObject
 * @param crypto String
 */
fun Message<JsonObject>.handleUserCheckIdAndCrypto(id: String, json: JsonObject, crypto: String) {
  if (!id.checkIdValid()) {
    // TODO 客户端检查。如果提供 Service API 则此处验证返回失败
    json.put("info","用户名格式错误")
    defaultFailedWithCrypto(json)
  }
  if (!crypto.checkCryptoValid()) {
    json.put("info","秘钥错误")
    defaultFailedWithCrypto(json)
  }
}

fun Message<JsonObject>.handleUserLogin(fs: FileSystem, userFile: String, id: String?, crypto: String?, json: JsonObject) {
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
      json.put("info","the user $id not exists")
    }
    json.removeCrypto()
    this.reply(json)
  }
}

fun Message<JsonObject>.handleUserRegistry(fs: FileSystem, userFile: String, json: JsonObject, id: String?, userDir: String) {
  fs.exists(userFile) {
    // if exists then failed
    if (it.result()) {
      json.put("info", "user $id already exists")
      defaultFailedWithCrypto(json)
    } else {
      fs.mkdirIfNotExists(userDir,
        fail = {
          println("cannot mkdir $userDir")
          json.put("info", "cannot mkdir")
          defaultFailedWithCrypto(json)
        },
        success = {
          fs.createFile(userFile) {
            if (it.succeeded()) {
              System.err.println(json)
//              json.removeAll {  }
              fs.writeFile(userFile, json.toBuffer()) {
                if (it.succeeded()) {
                  json.removeCrypto()
                  json.put("info", "succeed.")
                  this.reply(json)
                }
              }
            } else {
              defaultFailedWithCrypto(json)
            }
          }
        })
    }
  }
}

fun Message<JsonObject>.defaultFailedWithCrypto(json: JsonObject) {
  json.removeCrypto()
  json.put(ActionConstants.REGISTRY, false)
  this.reply(json)
}

fun String.checkIdValid(): Boolean {
  if (this.length < 4) return false
  if (this[0].isDigit()) return false
  return this.all { it.isLetterOrDigit() }
}

fun String.checkCryptoValid(): Boolean {
  return this.length == 32
}
