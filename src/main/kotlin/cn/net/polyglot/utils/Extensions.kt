package cn.net.polyglot.utils

import io.vertx.core.net.NetSocket

/**
 * @author zxj5470
 * @date 2018/7/10
 */

/**
 * avoid sticking packages, and `\r\n` should be handled at the client-side.
 * @receiver NetSocket
 * @param str String
 */
fun NetSocket.writeln(str: String) {
  this.write(str + "\r\n")
}
