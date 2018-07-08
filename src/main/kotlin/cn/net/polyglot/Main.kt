package cn.net.polyglot

import com.w2v4.MyFirstVerticle
import io.vertx.core.Vertx

/**
 * @author zxj5470
 * @date 2018/7/8
 */
fun main(args: Array<String>) {
  val vertx = Vertx.vertx()
  vertx.deployVerticle(MyFirstVerticle::class.java.name)
}
