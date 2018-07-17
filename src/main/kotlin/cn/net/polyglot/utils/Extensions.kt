package cn.net.polyglot.utils

import cn.net.polyglot.config.FileSystemConstants.USER_DIR
import cn.net.polyglot.config.FileSystemConstants.USER_FILE
import io.vertx.core.file.FileSystem
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
fun FileSystem.mkdirsIfNotExists(dirName: String = ".social-vertex", fail: () -> Unit = {}, success: () -> Unit = {}) {
  val fs = this
  fs.exists(dirName) {
    if (it.result()) {
      println("$dirName directory exist")
      success()
    } else {
      // if not exists, mkdir
      fs.mkdirs(dirName) { mkr ->
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
 * return ".social-vertex/user/yourID"
 * @param id String?
 * @return Pair<String, String>
 */
fun getUserDirAndFile(id: String?): Pair<String, String> {
  val userDir = "$USER_DIR$separator$id"
  val userFile = "$USER_DIR$separator$id$separator$USER_FILE"
  return Pair(userDir, userFile)
}
