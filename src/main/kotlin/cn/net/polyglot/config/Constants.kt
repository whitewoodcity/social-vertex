package cn.net.polyglot.config

/**
 * @author zxj5470
 * @date 2018/7/9
 */
object TypeConstants {
  const val MESSAGE = "message"
  const val SEARCH = "search"
  const val FRIEND = "friend"
  const val LOGIN = "login"
}

object ActionConstants {
  const val REQUEST = "request"
  const val RESPONSE = "response"
  const val DELETE = "delete"
}

object NumberConstants {
  const val TIME_LIMIT = 30 * 1000L
  const val CURRENT_VERSION = 0.1
}

object EventBusConstants {
  const val HTTP_TO_MSG = "Http2Msg"
  const val TCP_TO_MSG = "Hcp2Msg"
}
