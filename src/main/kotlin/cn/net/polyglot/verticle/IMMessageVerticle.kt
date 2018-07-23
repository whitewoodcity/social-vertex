package cn.net.polyglot.verticle

import cn.net.polyglot.config.TypeConstants.FRIEND
import cn.net.polyglot.config.TypeConstants.MESSAGE
import cn.net.polyglot.config.TypeConstants.SEARCH
import cn.net.polyglot.config.TypeConstants.USER
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import java.io.File

class IMMessageVerticle : AbstractVerticle() {

  override fun start() {
    vertx.eventBus().consumer<JsonObject>(IMMessageVerticle::class.java.name) {
      launch(vertx.dispatcher()) {
        when (it.body().getString("type")) {
          USER -> it.reply(user(it.body()))
          SEARCH -> search((it.body()))
          FRIEND -> friend((it.body()))
          MESSAGE -> message((it.body()))
          else -> {
          }
        }
      }
    }
  }

  private fun user(json: JsonObject) :JsonObject{
    val result = JsonObject().put("register", false)

    if(!json.containsKey("user")||!json.containsKey("crypto")){
      return result
    }
    try{
      val user = json.getString("user")
      val crypto = json.getString("crypto")

      //validate user
      val validUser = when {
        user.length < 4 || user.length > 20 -> false
        user[0].isDigit() -> false
        else -> user.all { it.isLetterOrDigit() or (it in arrayOf('.', '@')) }
      }

      if(!validUser)
        return result.put("info","用户名格式错误，仅允许不以数字开头的数字和字母组合，长度在4到20位之间")

      //validate crypto
      if(crypto == null || crypto.length != 32){
        return result.put("info","秘钥格式错误")
      }

      val dir = config().getString("dir") + File.separator + user
      vertx.fileSystem().mkdirsBlocking(dir)
      vertx.fileSystem().createFileBlocking(dir + File.separator + "user.json")
      vertx.fileSystem().writeFileBlocking(dir + File.separator + "user.json", json.toBuffer())

      return result.put("register",true)
    }catch (e:Exception){
      return result.put("info",e.message)
    }
  }

  private fun search(json: JsonObject) {

  }

  private fun friend(json: JsonObject) {

  }

  private fun message(json: JsonObject) {

  }
}
