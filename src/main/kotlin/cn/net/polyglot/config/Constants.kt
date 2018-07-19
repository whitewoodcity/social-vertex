package cn.net.polyglot.config

import java.io.File

/**
 * @author zxj5470
 * @date 2018/7/19
 */
object ActionConstants {
  const val REQUEST = "request"
  const val RESPONSE = "response"
  const val DELETE = "delete"
  const val LIST = "list"

  const val LOGIN = "login"
  const val REGISTRY = "registry"
}

object FileSystemConstants {
  const val MAIN_VERTICLE = "cn/net/polyglot/main_verticle.groovy"
  const val APP_DATA_DIR_NAME = ".social-vertex"
  val USER_DIR = APP_DATA_DIR_NAME + File.separator + "user"

  const val USER_FILE = "user.json"
  const val FRIENDS = "friends"
}

object JsonKeys {
  const val CRYPTO = "crypto"
}

object NumberConstants {
  const val TIME_LIMIT = 3 * 60 * 1000L
  const val CURRENT_VERSION = 0.1
}


object TypeConstants {
  const val MESSAGE = "message"
  const val SEARCH = "search"
  const val FRIEND = "friend"
  const val USER = "user"
}
