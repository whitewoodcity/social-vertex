package cn.net.polyglot.verticle.user

import cn.net.polyglot.config.*
import cn.net.polyglot.module.containsSensitiveWords
import cn.net.polyglot.module.lowerCaseValue
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch
import java.io.File

class UserVerticle:CoroutineVerticle() {

  override suspend fun start() {
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      launch { it.reply(user(it.body())) }
    }
  }

  private suspend fun user(json: JsonObject): JsonObject {

    json.lowerCaseValue(ID)

    val subtype = json.getString(SUBTYPE)
    val result = JsonObject()
      .put(TYPE, json.getString(TYPE))
      .put(SUBTYPE, subtype)
      .put(subtype, false)

    if (!json.containsKey(ID)) {
      return result
    }
    try {
      val id = json.getString(ID)

      //validate id
      val validId = when {
        id.length < 4 || id.length > 20 -> false
        id[0].isDigit() -> false
        else -> id.matches(Regex(pattern = "[a-zA-Z0-9]+")) && !containsSensitiveWords(id)//不包含有敏感词
      }

      if (!validId)
        return result.put(INFO, "用户名格式错误，仅允许不以数字开头的数字和拉丁字母组合，长度在4到20位之间")

      val dir = config.getString(DIR) + File.separator + id

      return when (subtype) {
        REGISTER -> {
          //validate password
          val password = json.getString(PASSWORD)
          if (password == null || password.length != 32) {
            return result.put(INFO, "秘钥格式错误")
          }

          if(password != json.getString(PASSWORD2)){
            return result.put(INFO, "两次输入密码不一致")
          }
          if (vertx.fileSystem().exists(dir).await()) {
            return result.put(INFO, "用户已存在")
          }
          vertx.fileSystem().mkdirs(dir).await()
          vertx.fileSystem().createFile(dir + File.separator + "password").await()
          vertx.fileSystem().writeFile(dir + File.separator + "password", Buffer.buffer(json.getString(PASSWORD))).await()
          vertx.fileSystem().createFile(dir + File.separator + "user.json").await()
          json.removeAll { it.key in arrayOf(TYPE, SUBTYPE, PASSWORD, PASSWORD2) }
          vertx.fileSystem().writeFile(dir + File.separator + "user.json", json.toBuffer()).await()

          result.put(subtype, true)
        }
        UPDATE -> {
          if (!vertx.fileSystem().exists(dir).await()) {
            return result.put(INFO, "用户不存在")
          }
          //检查password文件是否存在，若不存在，则表示密码存在user.json中，将其读出写入
          if(!vertx.fileSystem().exists(dir + File.separator + "password").await()){
            try{
              vertx.fileSystem().createFile(dir + File.separator + "password").await()
              val pw = vertx.fileSystem().readFile(dir + File.separator + "user.json").await().toJsonObject().getString(PASSWORD)
              vertx.fileSystem().writeFile(dir + File.separator + "password", Buffer.buffer(pw)).await()
            }catch (e:Throwable){
              e.printStackTrace()
            }
          }
          vertx.fileSystem().delete(dir + File.separator + "user.json").await()
          vertx.fileSystem().createFile(dir + File.separator + "user.json").await()
          json.removeAll { it.key in arrayOf(TYPE, SUBTYPE) }
          vertx.fileSystem().writeFile(dir + File.separator + "user.json", json.toBuffer()).await()
          result.put(subtype, true)
        }
        VERIFY -> {
          if (!vertx.fileSystem().exists(dir).await()) {
            return result.put(INFO, "用户不存在")
          }
          //检查password文件是否存在，若不存在，则表示密码存在user.json中，将其读出
          val password =
          if(vertx.fileSystem().exists(dir + File.separator + "password").await()){
            vertx.fileSystem().readFile(dir + File.separator + "password").await().toString()
          }else{
            vertx.fileSystem().readFile(dir + File.separator + "user.json").await().toJsonObject().getString(PASSWORD)
          }
          result.put(subtype, json.getString(PASSWORD) == password)
        }
        PROFILE -> {
          try{
            val jsonObject = vertx.fileSystem().readFile(dir + File.separator + "user.json").await().toJsonObject()

            if(vertx.fileSystem().exists(dir + File.separator + "password").await()) {
              val password = vertx.fileSystem().readFile(dir + File.separator + "password").await().toString()
              jsonObject.put(PASSWORD, password)
            }

            val fs = vertx.fileSystem()
            val friendList = JsonArray()
            val friends = fs.readDir(dir).await()
            for (friend in friends) {
              val friendId = friend.substringAfterLast(File.separator)
              if (fs.lprops(friend).await().isDirectory && !friendId.startsWith(".")) {
                try {
                  friendList.add(fs.readFile("$friend${File.separator}$friendId.json").await().toJsonObject())
                }catch (e:Throwable){
                  val friendJson = JsonObject().put(ID, friendId)
                  friendList.add(friendJson)
                }
              }
            }

            jsonObject.put(FRIENDS, friendList)

            val notificationList = JsonArray()
            if(vertx.fileSystem().exists(dir+File.separator+".receive").await()){
              val notifications = vertx.fileSystem().readDir(dir+File.separator+".receive").await()

              for(notification in notifications){
                try{
                  if(notification.endsWith(".json")){
                    notificationList.add(vertx.fileSystem().readFile(notification).await().toJsonObject())
                  }
                }catch (e:Throwable){
                  e.printStackTrace()
                }
              }
            }
            jsonObject.put(NOTIFICATIONS, notificationList)

            result.put(subtype, true)
              .put(JSON_BODY, jsonObject)
          }catch(e:Throwable){
            result.put(INFO, e.message)
          }
        }
        else -> result.put(INFO, "未指定操作子类型")
      }
    } catch (e: Exception) {
      print(e)
      return result
    }
  }

}
