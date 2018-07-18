package cn.net.polyglot.handler

import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

interface IMMessageHandlers {
  fun register(fs: FileSystem, user:String):JsonObject
  fun searchUser(fs: FileSystem, json: JsonObject):JsonObject
  fun message(fs: FileSystem, userFile: String, id: String?, crypto: String?, json: JsonObject):JsonObject
  fun friend(fs: FileSystem, from:String, to: String):JsonObject
  fun deleteFriend(fs: FileSystem, from:String, to: String):JsonObject
}
