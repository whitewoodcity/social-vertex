package cn.net.polyglot.config

import java.io.File.separator

/**
 * @author zxj5470
 * @date 2018/7/19
 */

/**
 * all request types for social-vertex IM protocol
 */
object TypeConstants {
  const val MESSAGE = "message"
  const val SEARCH = "search"
  const val FRIEND = "friend"
  const val USER = "user"
}

/**
 * all action types.
 */
object ActionConstants {
  const val REQUEST = "request"
  const val RESPONSE = "response"
  const val DELETE = "delete"
  const val LIST = "list"

  // user
  const val LOGIN = "login"
  const val REGISTRY = "registry"
}

object FileSystemConstants {
  private const val APP_DATA_DIR_NAME = ".social-vertex"
  const val MAIN_VERTICLE = "cn/net/polyglot/main_verticle.groovy"
  const val USER_FILE = "user.json"
  const val FRIENDS = "friends"

  val USER_DIR = APP_DATA_DIR_NAME + separator + "user"
}

object JsonKeys {
  const val CRYPTO = "crypto"
}

object NumberConstants {
  /**
   * TCP time limit for keeping alive.
   */
  const val TIME_LIMIT = 3 * 60 * 1000L
  const val CURRENT_VERSION = 0.1
}
