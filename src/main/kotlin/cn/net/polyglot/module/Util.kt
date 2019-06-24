/**
MIT License

Copyright (c) 2018 White Wood City

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package cn.net.polyglot.module

import cn.net.polyglot.config.SENSITIVE_WORDS
import io.vertx.core.json.JsonObject
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import java.util.regex.Pattern

fun md5(string:String):String{
  val digest = MessageDigest.getInstance("MD5")
  digest.update(string.toByteArray())
  val hex = BigInteger(1, digest.digest()).toString(16)
  // 补齐BigInteger省略的前置0
  return String(CharArray(32 - hex.length)).replace("\u0000", "0") + hex
}

fun containsSensitiveWords(string:String):Boolean{
  return SENSITIVE_WORDS.split(" ").any { string.contains(it) }
}

fun JsonObject.lowerCaseValue(field:String):JsonObject{
  try{
    if(this.getString(field)!=null){
      this.put(field, this.getString(field).toLowerCase())
    }
  }catch (e:Throwable){
    //json field may not be string
  }
  return this
}

/**
 * Parse out a mime type from a content type header.
 *
 * @param contentType
 *            e.g. "text/plain; charset=EUC-JP"
 * @return "text/plain"
 *
 */
fun getMimeTypeWithoutCharset(contentType:String):String {
  return contentType.split(";")[0].trim()
}

/**
 * Parse out a charset from a content type header.
 *
 * @param contentType
 *            e.g. "text/html; charset=EUC-JP"
 * @return "EUC-JP"
 *
 */
fun getCharsetFromContentType(contentType: String):String {
  val m = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)").matcher(contentType)
  return if (m.find()) {
    m.group(1).trim().toUpperCase()
  }else "UTF-8"
}

/**
 * get tomorrow date
 *
 * @return Date
 */
fun Date.tomorrow(): Date {
  val cal = Calendar.getInstance()
  cal.time = this
  cal.add(Calendar.DATE, 1)
  return cal.time
}

/**
 * get yesterday date
 *
 * @return Date
 */
fun Date.yesterday():Date{
  val cal = Calendar.getInstance()
  cal.time = this
  cal.add(Calendar.DATE, -1)
  return cal.time
}

/**
 * get tomorrow date
 *
 * @return Date
 */
fun Date.inNextYear(): Date {
  val cal = Calendar.getInstance()
  cal.time = this
  cal.add(Calendar.YEAR, 1)
  return cal.time
}

/**
 * get netx hour
 *
 * @return Date
 */
fun Date.nextHour(): Date{
  val cal = Calendar.getInstance()
  cal.time = this
  cal.add(Calendar.HOUR, 1)
  return cal.time
}

/**
 * get netx hour
 *
 * @return Date
 */
fun Date.lastHour(): Date{
  val cal = Calendar.getInstance()
  cal.time = this
  cal.add(Calendar.HOUR, -1)
  return cal.time
}

