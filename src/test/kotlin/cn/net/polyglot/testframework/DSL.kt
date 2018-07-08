package cn.net.polyglot.testframework

import cn.net.polyglot.config.defaultJsonObject
import io.vertx.kotlin.core.DeploymentOptions

/**
 * @author zxj5470
 * @date 2018/7/8
 */
infix fun Any.shouldBe(other: Any) = (this == other).also { result ->
  if (!result) {
    System.err.println("It should be \n$other\nBut actually it is \n$other")
  }
  assert(result)
}

fun configPort(port: Int = 8080) = DeploymentOptions(config = defaultJsonObject.apply { put("port", port) })
