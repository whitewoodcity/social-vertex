package cn.net.polyglot.config

import java.io.File.separator

/**
 * @author zxj5470
 * @date 2018/7/9
 */
object TypeConstants {
  const val MESSAGE = "message"
  const val SEARCH = "search"
  const val FRIEND = "friend"
  const val USER = "user"
}

object ActionConstants {
  const val REQUEST = "request"
  const val RESPONSE = "response"
  const val DELETE = "delete"

  const val LOGIN = "login"
  const val REGISTRY = "registry"
}

object NumberConstants {
  const val TIME_LIMIT = 30 * 1000L
  const val CURRENT_VERSION = 0.1
}

object EventBusConstants {
  const val HTTP_TO_MSG = "Http2Msg"
  const val TCP_TO_MSG = "Hcp2Msg"
}

object Keys{
  const val CRYPTO = "crypto"
}

object FileSystemConstants {
  const val APP_DATA_DIR_NAME = ".social-vertex"
  val USER_DIR = "$APP_DATA_DIR_NAME${separator}user"
  const val USER_FILE = "user.json"
}
