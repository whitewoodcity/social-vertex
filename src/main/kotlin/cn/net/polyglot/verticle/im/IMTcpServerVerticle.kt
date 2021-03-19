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

package cn.net.polyglot.verticle.im

import cn.net.polyglot.config.*
import cn.net.polyglot.module.lowerCaseValue
import cn.net.polyglot.verticle.user.UserVerticle
import com.google.common.collect.HashBiMap
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch

class IMTcpServerVerticle : CoroutineVerticle() {

  private val socketMap = HashBiMap.create<NetSocket, String>()

  override suspend fun start() {

    val port = config.getInteger(TCP_PORT)

    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      val target = it.body().getString(TO)
      if (socketMap.containsValue(target)) {
        socketMap.inverse()[target]!!.write(it.body().toString().plus(END))
      }
    }

    vertx.createNetServer(NetServerOptions().setTcpKeepAlive(true)).connectHandler { socket ->

      //因为是bimap，不能重复存入null，会抛异常，所以临时先放一个字符串，等用户登陆之后便会替换该字符串，以用户名取代
      socketMap[socket] = socket.writeHandlerID()

      val parser = RecordParser
        .newDelimited(END) { launch { processJsonString(it.toString(), socket) } }
        .maxRecordSize(10240)//max is 10KB
        .exceptionHandler { socket.close() }

      socket.handler(parser)

      socket.closeHandler {
        socketMap.remove(socket)
      }

      socket.exceptionHandler {
        it.printStackTrace()
        socket.close()
      }
    }.listen(port) {
      if (it.succeeded()) {
        println("${this::class.java.name} is deployed")
      } else {
        println("${this::class.java.name} fail to deploy")
      }
    }
  }

  private suspend fun processJsonString(jsonString: String, socket: NetSocket) {
    val result = JsonObject()
    try {
      val json = JsonObject(jsonString)
        .put(FROM, socketMap[socket])
        .lowerCaseValue(ID)

      when (json.getString(TYPE)) {
        LOGIN -> {
          val id = json.getString(ID)
          val password = json.getString(PASSWORD)
          val requestJson = JsonObject().put(TYPE, USER).put(SUBTYPE, VERIFY).put(ID, id).put(PASSWORD, password)
          val responseJson = vertx.eventBus().request<JsonObject>(UserVerticle::class.java.name, requestJson).await().body()
          if(responseJson.getBoolean(VERIFY)){
            if (socketMap.containsValue(id) && socketMap.inverse()[id] != socket) {
              socketMap.inverse()[id]?.close()//表示之前连接的socket跟当前socket不是一个，设置单点登录
            }
            socketMap[socket] = id
          }else{
            socketMap.inverse()[id]?.close()
          }
        }
        else -> println(json)
      }
    } catch (e: Exception) {
      socket.write(result.put(INFO, "${e.message}").toString().plus(END))
    }
  }

}
