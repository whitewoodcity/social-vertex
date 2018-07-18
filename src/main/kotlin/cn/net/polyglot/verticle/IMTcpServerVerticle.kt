package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.config.NumberConstants.TIME_LIMIT
import cn.net.polyglot.utils.text
import cn.net.polyglot.utils.tryJson
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMTcpServerVerticle : AbstractVerticle() {

  private val socketMap = hashMapOf<String, NetSocket>()
  private val activeMap = hashMapOf<String, Long>()

  override fun start() {
    val port = config().getInteger("port", DEFAULT_PORT)
    val options = NetServerOptions().setTcpKeepAlive(true)

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

        val json = it.text().tryJson()
        System.err.println(json)
        if (json == null) {
          socket.write("""{"info":"json format error"}""")
        } else {
          vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name, json) { ar ->
            if (ar.succeeded()) {
              val ret = ar.result().body()
              socket.write(ret.toString())
            }
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
