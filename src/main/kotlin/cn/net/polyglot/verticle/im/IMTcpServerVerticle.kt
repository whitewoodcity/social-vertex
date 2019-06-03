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
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.core.parsetools.RecordParser
import io.vertx.kotlin.core.eventbus.sendAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File.separator

class IMTcpServerVerticle : CoroutineVerticle() {

  private val socketMap = HashBiMap.create<NetSocket, String>()
//  private var buffer = Buffer.buffer()

  override suspend fun start() {

    val port = config.getInteger(TCP_PORT)

    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
//      val type = it.body().getString(TYPE)
      val target = it.body().getString(TO)
      if (socketMap.containsValue(target)) {
        socketMap.inverse()[target]!!.write(it.body().toString().plus(END))
      }
//      else if (type == MESSAGE) {//仅是message类型的时候，投递不成功会在此处存入硬盘，friend类型已经先行处理
//        val targetDir = config.getString(DIR) + separator + it.body().getString(TO) + separator + ".message"
//        val fs = vertx.fileSystem()
//        //注意此处不用异步处理，因为.message里面的文件都是临时的，不应该会影响性能，如果改成异步await函数，量大的时候这里会有并发处理异常
//        if (!fs.existsBlocking(targetDir)) fs.mkdirBlocking(targetDir)
//        if (!fs.existsBlocking("$targetDir$separator${it.body().getString(FROM)}.sv"))
//          fs.createFileBlocking("$targetDir$separator${it.body().getString(FROM)}.sv")
//        fs.openBlocking("$targetDir$separator${it.body().getString(FROM)}.sv", OpenOptions().setAppend(true))
//          .write(it.body().toBuffer().appendString(END))
//      }
    }
    vertx.createNetServer(NetServerOptions().setTcpKeepAlive(true)).connectHandler { socket ->

      //因为是bimap，不能重复存入null，会抛异常，所以临时先放一个字符串，等用户登陆之后便会替换该字符串，以用户名取代
      socketMap[socket] = socket.writeHandlerID()

      socket.handler {
        RecordParser
          .newDelimited(END) { buffer -> launch { processJsonString(buffer.toString(), socket) } }
          .maxRecordSize(10240)//max is 10KB
          .exceptionHandler { socket.close() }
          .handle(it)
      }

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
          val responseJson = vertx.eventBus().sendAwait<JsonObject>(UserVerticle::class.java.name, requestJson).body()
          if(responseJson.getBoolean(VERIFY)){
            if (socketMap.containsValue(id) && socketMap.inverse()[id] != socket) {
              socketMap.inverse()[id]?.close()//表示之前连接的socket跟当前socket不是一个，设置单点登录
            }
            socketMap[socket] = id
          }else{
            socketMap.inverse()[id]?.close()
          }
        }
//        USER, SEARCH -> {
//          val asyncResult = vertx.eventBus().sendAwait<JsonObject>(IMMessageVerticle::class.java.name, json)
//          val jsonObject = asyncResult.body()
//
//          if (jsonObject.containsKey(LOGIN) && jsonObject.getBoolean(LOGIN)) {
//            if (socketMap.containsValue(json.getString(ID)) && socketMap.inverse()[json.getString(ID)] != socket) {
//              socketMap.inverse()[json.getString(ID)]?.close()//表示之前连接的socket跟当前socket不是一个，设置单点登录
//            }
//            socketMap[socket] = json.getString(ID)
//          }
//          jsonObject.mergeIn(json).remove(PASSWORD)
//          jsonObject.remove(FROM)
//          socket.write(jsonObject.toString().plus(END))
//        }
        else -> {
          println(json)
//          vertx.eventBus().send(IMMessageVerticle::class.java.name, json)
        }
      }
    } catch (e: Exception) {
      socket.write(result.put(INFO, "${e.message}").toString().plus(END))
    }
  }

}
