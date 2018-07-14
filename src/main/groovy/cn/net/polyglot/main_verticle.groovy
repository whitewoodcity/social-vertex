package cn.net.polyglot

import cn.net.polyglot.config.ConfigLoader
import cn.net.polyglot.verticle.IMHttpServerVerticle
import cn.net.polyglot.verticle.IMMessageVerticle
import cn.net.polyglot.verticle.IMTcpServerVerticle
import io.vertx.config.ConfigRetriever
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

Vertx vertx = vertx

/**
 * load `config.json` at the place where in the same directory as the jar file ,
 * and it'll load inner file if `config.json` not exists
 * or the file is in a wrong json format.
 */
options = ConfigLoader.options
ConfigLoader.makeAppDirs(vertx)

ConfigRetriever retriever = ConfigRetriever.create(vertx, options)

retriever.getConfig { ar ->

  if (ar.failed()) {
    failedDefault()
  } else {

    JsonObject config = ar.result()
    if (ConfigLoader.checkPortValidFromConfig(config)) {

      deployVerticles(config)

    } else {
      failedDefault()
    }
  }
}

private void deployVerticles(JsonObject config) {
  vertx.deployVerticle(IMMessageVerticle.class.name, new DeploymentOptions().setConfig(config))

//   8081
  ConfigLoader.portInc(config)
  vertx.deployVerticle(IMHttpServerVerticle.class.name, new DeploymentOptions().setConfig(config))

//  8082
  ConfigLoader.portInc(config)
  vertx.deployVerticle(IMTcpServerVerticle.class.name, new DeploymentOptions().setConfig(config))

}

private void failedDefault() {

  System.out.println("The configuration file: config.json does not exist or in wrong format, use default config.")

  JsonObject config = ConfigLoader.defaultJsonObject

  deployVerticles(config)
}
