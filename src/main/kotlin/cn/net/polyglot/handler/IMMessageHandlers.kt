package cn.net.polyglot.handler

import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

fun register(fs: FileSystem, user: String): JsonObject {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun searchUser(fs: FileSystem, json: JsonObject): JsonObject {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun message(fs: FileSystem, userFile: String, id: String?, crypto: String?, json: JsonObject): JsonObject {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun friend(fs: FileSystem, from: String, to: String): JsonObject {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun deleteFriend(fs: FileSystem, from: String, to: String): JsonObject {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}
