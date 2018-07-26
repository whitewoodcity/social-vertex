package cn.net.polyglot.verticle


import com.google.common.collect.HashBiMap
import com.sun.deploy.util.BufferUtil.MB
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMTcpServerVerticle : AbstractVerticle() {

  private val socketMap = HashBiMap.create<NetSocket, String>()
  private var buffer = Buffer.buffer()

  override fun start() {
    val port = config().getInteger("tcp-port")

    vertx.createNetServer(NetServerOptions().setTcpKeepAlive(true)).connectHandler { socket ->

      //因为是bimap，不能重复存入null，会抛异常，所以临时先放一个字符串，等用户登陆之后便会替换该字符串，以用户名取代
      socketMap[socket] = socket.writeHandlerID()

      socket.handler {
        buffer.appendBuffer(it)
        if(buffer.toString().endsWith("\r\n")){
          val msgs = buffer.toString().substringBeforeLast("\r\n").split("\r\n")
          for(s in msgs){
            processJsonString(s, socket)
          }
          buffer = Buffer.buffer()
        }

        if(buffer.length() > 1*MB){
          buffer = Buffer.buffer()
        }
      }

      socket.closeHandler {
        socketMap.remove(socket)
      }

      socket.exceptionHandler { e ->
        e.printStackTrace()
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

  private fun processJsonString(jsonString:String, socket:NetSocket){
    val result = JsonObject()
    try{
      val json = JsonObject(jsonString)

      //todo dealing with json request here
      when(json.getString("type")){
        "user" -> vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name,json){
          val resultJson = it.result().body()

          if(resultJson.getBoolean("login"))
            socketMap[socket] = json.getString("user")

          socket.write(it.result().body().toBuffer())}
        else -> {

        }
      }
    }catch (e:Exception){
      socket.write(result.put("info", e.message).toBuffer())
    }
  }
}
