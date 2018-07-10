package cn.net.polyglot.utils

import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/10
 */
fun FileSystem.mkdirIfNotExists(dirName: String = ".social-vertex") {
  val fs = this
  fs.exists(dirName) {
    if (it.result()) {
      println("$dirName directory exist")
    } else {
      fs.mkdir(dirName) { mkr ->
        if (mkr.succeeded()) {
          println("mkdir $dirName succeed")
        } else {
          println("mkdir failed, please check the permission")
        }
      }
    }
  }
}

/**
 *
 * @receiver JsonObject
 * @param key String
 * @param value Any?
 * @return JsonObject this
 */
fun JsonObject.putNullable(key: String, value: Any?): JsonObject {
  if (value == null) {
    if (this.getValue(key) != null) {
      this.remove(key)
    }
    this.putNull(key)
  } else this.put(key, value)
  return this
}

/**
 * try to convert to JSON
 * @receiver String
 * @return JsonObject
 */
fun String.tryJson(): JsonObject? {
  return try {
    JsonObject(this)
  } catch (e: Exception) {
    null
  }
}

fun Buffer.text() = String(this.bytes)
