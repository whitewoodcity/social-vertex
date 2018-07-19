package cn.net.polyglot.verticle

import cn.net.polyglot.config.DEFAULT_PORT
import cn.net.polyglot.config.NumberConstants.CURRENT_VERSION
import cn.net.polyglot.config.NumberConstants.TIME_LIMIT
import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import cn.net.polyglot.config.getHttpPortFromDomain
import cn.net.polyglot.handler.*
import cn.net.polyglot.utils.text
import cn.net.polyglot.utils.tryJson
import io.vertx.core.AbstractVerticle
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.ext.web.client.WebClient

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMTcpServerVerticle : AbstractVerticle() {
  private val idMap = hashMapOf<String, String>()
  private val socketMap = hashMapOf<String, NetSocket>()
  private val activeMap = hashMapOf<String, Long>()
  // trigger NPE in unit test if initialize by ClassLoader
  lateinit var webClient: WebClient

  override fun start() {
    val port = config().getInteger("port", DEFAULT_PORT)
    val options = NetServerOptions().setTcpKeepAlive(true)
    val fs = vertx.fileSystem()
    webClient = WebClient.create(vertx)

    // get message from IMHttpServerVerticle eventBus
    vertx.eventBus().localConsumer<JsonObject>(IMHttpServerVerticle::class.java.name) {
      val json = it.body()
      val type = json.getString("type")
      val toUser = json.getString("to")
      if (type == MESSAGE) {
        val id = idMap[toUser] ?: return@localConsumer
        val toSocket = socketMap[id] ?: return@localConsumer
        val activeSocketTime = activeMap[id] ?: return@localConsumer
        if ((System.currentTimeMillis() - activeSocketTime) < TIME_LIMIT) {
          toSocket.write(json.toBuffer())
        }
      }
    }

    vertx.createNetServer(options).connectHandler { socket ->
      socket.handler {
        val socketId = socket.writeHandlerID()
        activeMap[socketId] = System.currentTimeMillis()
        socketMap[socketId] = socket

        System.err.println("$socketId\n${it.text()}")
        val json = it.text().tryJson()

        if (json == null) {
          socket.write("""{"info":"json format error"}""")
        } else {

          val type = json.getString("type", "")
          val version = json.getDouble("version", CURRENT_VERSION)

          if (type == MESSAGE) {
            handleMessage(fs, json, port, socket)
          } else {
            val ret = when (type) {
              SEARCH -> searchUser(fs, json)
              FRIEND -> friend(fs, json)
              USER -> userAuthorize(fs, json, loginTcpAction = {
                loginTcpAction(json, socketId, socket)
              })
              else -> defaultMessage(fs, json)
            }
            socket.write(ret.toString())
          }
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

  private fun loginTcpAction(json: JsonObject, socketId: String, socket: NetSocket) {
    val user = json.getJsonObject("user").getString("id")
    // map user to socketId
    idMap[user] = socketId
    activeMap[socketId] = System.currentTimeMillis()
    socketMap[socketId] = socket
  }

  private fun handleMessage(fs: FileSystem, json: JsonObject, port: Int, socket: NetSocket) {
    val ret = message(fs, json, directlySend = { toUser ->
      val id = idMap[toUser] ?: return@message
      val toSocket = socketMap[id] ?: return@message
      val activeSocketTime = activeMap[id] ?: return@message
      if ((System.currentTimeMillis() - activeSocketTime) < TIME_LIMIT) {
        toSocket.write(json.toBuffer())
      }
    }, indirectlySend = { toUser ->
      // 发送至跨域名的服务器，使用 WebClient
      val host = toUser.substringAfter('@')
      val p = getHttpPortFromDomain(host)
      webClient.post(p, host, "/").sendJsonObject(json) {
        if (it.succeeded()) {
//          val webClientJsonObject = it.result().bodyAsJsonObject()
//          socket.write(webClientJsonObject.toBuffer())
        }
      }
    })
    socket.write(ret.toBuffer())
  }
}
