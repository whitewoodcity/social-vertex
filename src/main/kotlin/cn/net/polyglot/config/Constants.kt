package cn.net.polyglot.config

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
object SubtypeConstants {
  const val REQUEST = "request"
  const val RESPONSE = "response"
  const val DELETE = "delete"
  const val LIST = "list"

  // user
  const val LOGIN = "login"
  const val REGISTER = "register"
}

object JsonKeys {
  const val TYPE = "type"
  const val SUBTYPE = "subtype"
  const val VERSION = "version"

  const val FROM = "from"
  const val TO = "to"

  const val INFO = "info"

  const val NICKNAME = "nickname"
}
