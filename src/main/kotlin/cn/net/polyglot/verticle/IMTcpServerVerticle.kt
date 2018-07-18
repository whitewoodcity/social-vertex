package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.config.NumberConstants.CURRENT_VERSION
import cn.net.polyglot.config.NumberConstants.TIME_LIMIT
import cn.net.polyglot.config.TypeConstants.*
import cn.net.polyglot.handler.*
import cn.net.polyglot.utils.text
import cn.net.polyglot.utils.tryJson
import io.vertx.core.AbstractVerticle
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket


/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMTcpServerVerticle : AbstractVerticle() {

  companion object {
    // map<userID: String，writeHandlerID: String>
    val idMap = hashMapOf<String, String>()
    val socketMap = hashMapOf<String, NetSocket>()
    val activeMap = hashMapOf<String, Long>()
  }

  override fun start() {
    val port = config().getInteger("port", DEFAULT_PORT)
    val options = NetServerOptions().apply {
      isTcpKeepAlive = true
    }

    vertx.createNetServer(options).connectHandler { socket ->
      socket.handler {
        val socketId = socket.writeHandlerID()
        activeMap[socketId] = System.currentTimeMillis()
        socketMap[socketId] = socket

        System.err.println(socketId + it.text())
        val json = it.text().tryJson()

        if (json == null) {
          socket.write("""{"info":"json format error"}""")
        } else {

          val fs = vertx.fileSystem()
          val type = json.getString("type", "")
          val version = json.getDouble("version", CURRENT_VERSION)
          val ret = when (type) {
            MESSAGE -> {
              message(fs, json, directlySend = { to ->
                val id = idMap[to] ?: return@message
                val toSocket = socketMap[id] ?: return@message
                val activeSocketTime = activeMap[id] ?: return@message
                // isActive
                if ((System.currentTimeMillis() - activeSocketTime) < TIME_LIMIT) {
                  toSocket.write(json.toBuffer())
                }
              }, indirectlySend = {
                // TODO
              })
            }
            SEARCH -> searchUser(fs, json)
            FRIEND -> friend(fs, json)
            USER -> userAuthorize(fs, json, loginTcpAction = {
              val user = json.getJsonObject("user").getString("id")
              // map user to socketId
              idMap[user] = socketId
              activeMap[socketId] = System.currentTimeMillis()
              socketMap[socketId] = socket
            })
            else -> defaultMessage(fs, json)
          }
          socket.write(ret.toString())
        }
      }

      vertx.setPeriodic(TIME_LIMIT) {
        activeMap
          .filter { System.currentTimeMillis() - it.value > TIME_LIMIT }
          .forEach { id, _ ->
            println("remove $id connect")
            activeMap.remove(id)
            socketMap[id]?.close()
            socketMap.remove(id)
            idMap.values.remove(id)
          }
      }

      socket.closeHandler {
        socket.writeHandlerID().let {
          println("close TCP connect $it")
          activeMap.remove(it)
          socketMap.remove(it)
          idMap.values.remove(it)
        }
      }

      socket.endHandler {
        println("end")
      }

      socket.exceptionHandler { e ->
        e.printStackTrace()
//        socket.close()
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
