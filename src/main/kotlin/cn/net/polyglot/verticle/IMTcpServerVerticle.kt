package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.config.NumberConstants.TIME_LIMIT
import cn.net.polyglot.handler.handle
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
    val port = config.getInteger("port", DEFAULT_PORT + 1)

    val options = NetServerOptions().apply {
      isTcpKeepAlive = true
    }

//    val fs = vertx.fileSystem()

    launch(vertx.dispatcher()) {
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

          System.err.println(it.bytes.let { String(it) })
          val ret = handle(it)
          println(ret)
          socket.write(ret)
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
          socket.close()
          println(e.message)
          e.printStackTrace()
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
}
