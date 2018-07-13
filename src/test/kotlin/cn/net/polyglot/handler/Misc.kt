package cn.net.polyglot.handler

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.Async
import io.vertx.ext.web.client.WebClient

typealias JsonMessage = Message<JsonObject>

fun WebClient.post(json: JsonObject, async: Async, port: Int) {
  this.post(port, "localhost", "/")
    .sendJsonObject(json) { response ->
      if (response.succeeded()) {
        println(response.result().bodyAsJsonObject())
        async.complete()
      } else {
        System.err.println("failed")
        async.complete()
      }
    }
}
