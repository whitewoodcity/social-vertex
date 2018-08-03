package cn.net.polyglot.handler

import java.math.BigInteger
import java.security.MessageDigest

fun md5(string:String):String{
  val digest = MessageDigest.getInstance("MD5")
  digest.update(string.toByteArray())
  val hex = BigInteger(1, digest.digest()).toString(16)
  // 补齐BigInteger省略的前置0
  return String(CharArray(32 - hex.length)).replace("\u0000", "0") + hex
}
