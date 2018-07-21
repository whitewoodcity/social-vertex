package cn.net.polyglot

import cn.net.polyglot.verticle.IMHttpServerVerticle
import cn.net.polyglot.verticle.IMTcpServerVerticle
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

Vertx vertx = vertx

final int DEFAULT_PORT = 8080
JsonObject defaultConfig = new JsonObject().put("port", DEFAULT_PORT)

ConfigStoreOptions fileStore = new ConfigStoreOptions()
  .setType("file")
  .setConfig(new JsonObject().put("path", "config.json"))

ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore)

ConfigRetriever retriever = ConfigRetriever.create(vertx, options)

retriever.getConfig { ar ->

  if (ar.failed()) {

    System.out.println("The configuration file: config.json does not exist or in wrong format, use default config.")
    deployVerticles(defaultConfig)

  } else {

    JsonObject config = ar.result()
    int port = config.getInteger("port", DEFAULT_PORT)

    if (0 <= port && port < 65536) {
      deployVerticles(config)
    } else {
      System.out.println("The configuration file: config.json does not exist or in wrong format, use default config.")
      deployVerticles(defaultConfig)
    }

  }
}

private void deployVerticles(JsonObject config) {
//   8081
  config.put("port", config.getInteger("port") + 1)
  vertx.deployVerticle(IMHttpServerVerticle.class.name, new DeploymentOptions().setConfig(config))

//  8082
  config.put("port", config.getInteger("port") + 1)
  vertx.deployVerticle(IMTcpServerVerticle.class.name, new DeploymentOptions().setConfig(config))

}
