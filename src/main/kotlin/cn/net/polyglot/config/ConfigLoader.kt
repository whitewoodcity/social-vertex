@file:JvmName("ConfigLoader")

package cn.net.polyglot.config

import cn.net.polyglot.config.FileSystemConstants.USER_DIR
import io.vertx.core.Vertx
import io.vertx.kotlin.config.ConfigRetrieverOptions
import io.vertx.kotlin.config.ConfigStoreOptions
import io.vertx.kotlin.core.json.JsonObject

/**
 * @author zxj5470
 * @date 2018/7/8
 */
val fileStore = ConfigStoreOptions(
  type = "file",
  format = "json",
  config = JsonObject(
    "path" to "config.json"
  ))

/**
 * load `config.json` at the place where in the same directory as the jar file ,
 * and it'll load inner file if `config.json` not exists
 * or the file is in a wrong json format.
 */
val options = ConfigRetrieverOptions(stores = arrayListOf(fileStore))

fun makeAppDirs(vertx: Vertx): Boolean {
  val fs = vertx.fileSystem()
  var result = true
  try {
    if (fs.existsBlocking(USER_DIR)) {
      println("$USER_DIR already exists")
    } else {
      fs.mkdirsBlocking(USER_DIR)
      println("create $USER_DIR success")
    }
  } catch (e: Exception) {
    result = false
    println("cannot create $USER_DIR")
  }
  return result
}

/**
 * temporary API. Read file or get `domain-port pair` later
 * @param domain String
 * @return Int
 */
fun getHttpPortFromDomain(domain: String): Int {
  return when (domain) {
    "polyglot.net.cn" -> 8081
    else -> 8081
  }
}
