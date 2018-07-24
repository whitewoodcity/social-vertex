package cn.net.polyglot.verticle

import cn.net.polyglot.config.FileSystemConstants.CRLF
import cn.net.polyglot.config.NumberConstants.CURRENT_VERSION
import cn.net.polyglot.config.NumberConstants.TIME_LIMIT
import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import cn.net.polyglot.handler.defaultMessage
import cn.net.polyglot.handler.friend
import cn.net.polyglot.handler.search
import cn.net.polyglot.handler.user
import cn.net.polyglot.utils.text
import cn.net.polyglot.utils.tryJson
import cn.net.polyglot.utils.writeln
import com.google.common.collect.HashBiMap
import io.vertx.core.AbstractVerticle
import io.vertx.core.file.FileSystem
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.ext.web.client.WebClient
import java.util.*

/**
 * @author zxj5470
 * @date 2018/7/9
 */
class IMTcpServerVerticle : AbstractVerticle() {

  private val idMap = HashBiMap.create<String, String>()
  private val socketMap = HashBiMap.create<String, NetSocket>()
  private val activeMap = hashMapOf<String, Long>()
  // trigger NPE in unit test if initialize by ClassLoader
  private lateinit var webClient: WebClient

  override fun start() {
    val port = config().getInteger("tcp-port")
    val options = NetServerOptions().setTcpKeepAlive(true)
    val fs = vertx.fileSystem()
    webClient = WebClient.create(vertx)

    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      val json = it.body()
      val type = json.getString("type")
      val toUser = json.getString("to")
      if (type == MESSAGE) {
        val id = idMap[toUser]
        val toSocket = socketMap[id]
        val activeSocketTime = activeMap[id]
        if (toSocket == null || activeSocketTime == null ||
          (System.currentTimeMillis() - activeSocketTime) > TIME_LIMIT) {
          // not alive
          json.put("info", "not online")
          it.reply(json)
        } else {
          json.put("info", "OK")
          // send to receiver
          toSocket.writeln(json.toString())
          // response
          it.reply(json)
        }
      }
    }

    vertx.createNetServer(options).connectHandler { socket ->
      socket.handler {
        val socketId = socket.writeHandlerID()
        activeMap[socketId] = System.currentTimeMillis()
        socketMap[socketId] = socket

        val time = Calendar.getInstance().time
        System.err.println("Time: $time, socketID: $socketId\n${it.text()}")

        // avoid sticking packages
        val list = it.text().split(CRLF).filter { it.isNotEmpty() }
        list.forEach {
          handleEachTcpPackage(it, socket, fs, socketId)
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

  /**
   *
   * @param it String each jsonStr
   * @param socket NetSocket
   * @param fs FileSystem
   * @param socketId String
   */
  private fun handleEachTcpPackage(it: String, socket: NetSocket, fs: FileSystem, socketId: String) {
    val json = it.tryJson()
    if (json == null) {
      socket.writeln("""{"info":"json format error"}""")
    } else {
      val type = json.getString("type", "")
      val version = json.getDouble("version", CURRENT_VERSION)
      if (type == MESSAGE) {
        //            handleMessage(fs, json, socket)
        println("send to IMMessageVerticle")
        vertx.eventBus().send<JsonObject>(IMMessageVerticle::class.java.name, json) { ar ->
          if (ar.succeeded()) {
            val body = ar.result().body()
            socket.writeln(body.toString())
          } else {
            socket.writeln("""{"info":"failed"}""")
          }
        }
      } else {
        val ret = when (type) {
          SEARCH -> search(fs, json)
          FRIEND -> friend(fs, json)
          USER -> user(fs, json, loginTcpAction = {
            loginTcpAction(json, socketId, socket)
          })
          else -> defaultMessage(fs, json)
        }
        socket.writeln(ret.toString())
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
}
