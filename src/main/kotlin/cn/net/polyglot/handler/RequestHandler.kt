@file:JvmName("RequestHandler")

package cn.net.polyglot.handler

import cn.net.polyglot.config.FileSystemConstants.USER_DIR
import cn.net.polyglot.config.FileSystemConstants.USER_FILE
import cn.net.polyglot.config.NumberConstants.CURRENT_VERSION
import cn.net.polyglot.config.TypeConstants.*
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import java.io.File.separator

fun Message<JsonObject>.handleEventBus(vertx: Vertx) {
  val fs = vertx.fileSystem()
  val json = this.body()

  val type = json.getString("type", "")
  val version = json.getDouble("version", CURRENT_VERSION)

  when (type) {
    MESSAGE -> handleMessage(fs, json)
    SEARCH -> handleSearch(fs, json)
    FRIEND -> handleFriend(fs, json)
    USER -> handleUser(fs, json)
    else -> defaultMessage(fs, json)
  }
}

fun Message<JsonObject>.handleMessage(fs: FileSystem, json: JsonObject) {
  val from = json.getString("from")
  val to = json.getString("to")
  val body = json.getString("body")

  val userDir = "$USER_DIR$separator$to"
  fs.exists(userDir){
    if (it.succeeded()){
      if(it.result()){
        json.put("info", "OK")
      }else{
        json.put("info","no such user $to")
      }
    }
  }
  this.reply(json)
}

fun Message<JsonObject>.handleSearch(fs: FileSystem, json: JsonObject) {
  val id = json.getString("id")
  val userDir = "$USER_DIR$separator$id"
  val userFile = "$USER_DIR$separator$id$separator$USER_FILE"

  fs.readFile(USER_DIR) { resBuffer ->
    if (resBuffer.succeeded()) {
      val resJson = resBuffer.result().toJsonObject()
      json.put("user", resJson)
    } else {
      json.putNull("user")
    }
    this.reply(json)
  }
}

fun Message<JsonObject>.defaultMessage(fs: FileSystem, json: JsonObject) {
  json.removeAll { it.key !in arrayOf("version", "type") }
  json.put("info", "Default info, please check all sent value is correct.")
  this.reply(json)
}
