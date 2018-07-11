package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.config.EventBusConstants
import cn.net.polyglot.config.NumberConstants.TIME_LIMIT
import cn.net.polyglot.utils.mkdirIfNotExists
import cn.net.polyglot.utils.text
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch


/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMTcpServerVerticle : CoroutineVerticle() {

  companion object {
    val socketMap = hashMapOf<String, NetSocket>()
    val activeMap = hashMapOf<String, Long>()
  }

  override suspend fun start() {
    val port = config.getInteger("port", DEFAULT_PORT)
    val options = NetServerOptions().apply {
      isTcpKeepAlive = true
    }

    val fs = vertx.fileSystem()
    fs.mkdirIfNotExists()

    vertx.createNetServer(options).connectHandler { socket ->
      socket.handler {
        val socketId = socket.writeHandlerID()
        val time = activeMap[socketId]

        if (time != null) {
          if (System.currentTimeMillis() - time > TIME_LIMIT) {
            socketMap[socketId] = socket
          } else {

          }
        }

        activeMap[socketId] = System.currentTimeMillis()

        System.err.println(it.text())
        vertx.eventBus().send<String>(EventBusConstants.TCP_TO_MSG, it.text()) { ar ->
          if (ar.succeeded()) {
            val ret = ar.result().body()
            socket.write(ret)
          }
        }
      }

      // check active per 3 minutes
      val time = 10 * 1000L
      vertx.setPeriodic(time) {
        activeMap
          .filter { System.currentTimeMillis() - it.value > time }
          .forEach { id, _ ->
            println("remove $id connect")
            activeMap.remove(id)
            socketMap.remove(id)
          }
      }

      socket.closeHandler {
        activeMap.remove(socket.writeHandlerID())
        socketMap.remove(socket.writeHandlerID())
        println(socket.writeHandlerID())
      }

      socket.endHandler {
        println("end")
      }

      socket.exceptionHandler { e ->
        println(e.message)
        socket.close()
      }
    }.listen(port, "0.0.0.0") {
      if (it.succeeded()) {
        println("${this@IMTcpServerVerticle::class.java.name} is deployed on port $port")
      } else {
        System.err.println("bind port $port failed")
      }
    }
  }
}
