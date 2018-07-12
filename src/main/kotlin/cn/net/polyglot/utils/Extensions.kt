package cn.net.polyglot.utils

import cn.net.polyglot.config.FileSystemConstants.USER_DIR
import cn.net.polyglot.config.FileSystemConstants.USER_FILE
import cn.net.polyglot.config.JsonKeys.CRYPTO
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import java.io.File.separator

/**
 * @author zxj5470
 * @date 2018/7/10
 */

/**
 *
 * @receiver FileSystem
 * @param dirName String
 * @param fail () -> Unit
 * @param success () -> Unit
 */
fun FileSystem.mkdirIfNotExists(dirName: String = ".social-vertex", fail: () -> Unit = {}, success: () -> Unit = {}) {
  val fs = this
  fs.exists(dirName) {
    if (it.result()) {
      println("$dirName directory exist")
      success()
    } else {
      // if not exists, mkdir
      fs.mkdir(dirName) { mkr ->
        if (mkr.succeeded()) {
          println("mkdir $dirName succeed")
          success()
        } else {
          println("mkdir fail, please check the permission")
          fail()
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

fun JsonObject.removeCrypto(): Any = this.remove(CRYPTO)

fun getDirAndFile(id: String?): Pair<String, String> {
  val userDir = "$USER_DIR$separator$id"
  val userFile = "$USER_DIR$separator$id$separator$USER_FILE"
  return Pair(userDir, userFile)
}
