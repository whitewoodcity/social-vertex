package cn.net.polyglot.verticle.admin

import cn.net.polyglot.config.*
import cn.net.polyglot.module.containsSensitiveWords
import cn.net.polyglot.module.lowerCaseValue
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.file.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import java.io.File

/**
 * type:
 *  Admin
 * subtype:
 *  registAdmin
 *  updateAdmin
 *  loginAdmin
 *  logoutAdmin
 *  unregistAdmin
 * id,
 * password,
 * password2,
 * authority,
 *
 */
class AdminVerticle :CoroutineVerticle(){
  override suspend fun start() {
    vertx.eventBus().consumer<JsonObject>(this::class.java.name) {
      launch { it.reply(admin(it.body())) }
    }
  }

  /**
   * super密码存放在 super_password.json里面
   * 普通的管理员信息,id,权限存放在:.admin/ id / info.json 里面
   * 普通的管理员密码存放在:.admin/ id / password.json 里面
   *
   * 1.注册
   * [1]:如果注册的id是super，若super_password.json不存在，则创建，并且把密码写入，若存在，则返回失败
   * [2]:如果id不是super，查询.admin/ id 文件夹是否存在，若不存在，则创建，把id,权限写入info.json里面，
   *     密码存放在password.json里面，若存在文件夹，则返回失败
   *
   * 2.更新
   * [1]:如果更新的id是super，则返回失败
   * [2]:查询./admin/ id 文件夹是否存在，若存在，则修改相关内容，若不存在，则返回失败
   *
   * 3.登陆
   * [1]:如果登陆的id是super，则从super_password里面获取密码和登陆状态，
   *   判断是否一致，若一致则判断登陆状态，如果为false，则登陆成功，登陆状态更改为true，否则登陆失败
   * [2]:id不是super，判断是否存在.admin/id文件夹，若不存在，则返回失败，
   *   从.admin/ id / password.json 里面获取密码，从info.json 里面获取登陆状态，
   *   判断密码是否一致，若一致则判断登陆状态。如果为false，则登陆成功，登陆状态更改为true，否则失败
   *
   * 4.登出
   * [1]:如果登陆的id是super，则从super_password里面获取密码和登陆状态，
   *   判断是否一致，若一致则判断登陆状态，如果为true，则登出成功，登陆状态更改为false，否则登陆失败
   * [2]:id不是super，判断是否存在.admin/id文件夹，若不存在，则返回失败，
   *   从.admin/ id / password.json 里面获取密码，从info.json 里面获取登陆状态，
   *   判断密码是否一致，若一致则判断登陆状态。如果为true，则登陆成功，登陆状态更改为false，否则失败
   *
   * 5.注销
   * [1]:如果登陆的id是super，则返回注销失败
   * [2]:id不是super，判断是否存在.admin/id文件夹，若不存在，则返回失败
   *     从.admin/ id / password.json 里面获取密码，从info.json 里面获取登陆状态，
   *     判断密码是否一致，如果一致，则删除./admin/id 文件夹和下面所有的内容，返回成功，否则返回失败
   */
  private suspend fun admin(json: JsonObject): JsonObject{
    json.lowerCaseValue(ID)

    val subtype = json.getString(SUBTYPE)
    val result = JsonObject()
      .put(TYPE, json.getString(TYPE))
      .put(SUBTYPE, subtype)
      .put(subtype, false)

    if (!json.containsKey(ID)) {
      return result
    }
    val id = json.getString(ID)

    //validate id
    val validId = when {
      id.length < 4 || id.length > 20 -> false
      id[0].isDigit() -> false
      else -> id.matches(Regex(pattern = "[a-zA-Z0-9]+")) && !containsSensitiveWords(id)//不包含有敏感词
    }

    if (!validId)
      return result.put(INFO, "用户名格式错误，仅允许不以数字开头的数字和拉丁字母组合，长度在4到20位之间")
    val password = json.getString(PASSWORD)
    if (password == null || password.length != 32) {
      return result.put(INFO, "秘钥格式错误")
    }

    if(subtype!= UPDATEADMIN&&password != json.getString(PASSWORD2)){
        return result.put(INFO, "两次输入密码不一致")
    }

    when(subtype){
      REGISTADMIN->{
        regist(json,result);
      }
      UPDATEADMIN->{
        update(json,result);
      }
      LOGINADMIN->{
        login(json,result);
      }
      LOGOUTADMIN->{
        logout(json,result);
      }
      UNREGISTADMIN->{
        unregist(json,result);
      }
    }
    return result;
  }
  private suspend fun regist(json: JsonObject, result: JsonObject){
    val id=json.getString(ID)
    val DIRPATH=config.getString(DIR);
    var IDPATH=DIRPATH + File.separator +id;
    val subtype=json.getString(SUBTYPE)
    vertx.fileSystem().mkdirsAwait(DIRPATH)
    if(id==SUPERADMIN){
      //检查super_password.json是否存在，若不存在则创建，否则失败
      if(!vertx.fileSystem().existsAwait(DIRPATH+File.separator+"super_password.json")){
        try{
          vertx.fileSystem().createFileAwait(DIRPATH+File.separator+"super_password.json")
          val pss=JsonObject().put(PASSWORD,json.getString(PASSWORD))
          vertx.fileSystem().writeFileAwait(DIRPATH+File.separator+"super_password.json", Buffer.buffer(pss.toString()))
          result.put(subtype,true)
        }catch (e:Throwable){
          e.printStackTrace()
          result.put(INFO,"EXCEPTION WHEN SAVE PASSWORD FOR SUPER")
        }
      }else{
        result.put(INFO,"ALREADY HAS SUPER PASSWORD, YOU CAN NOT MODIFY")
      }
    }else{
      //如果不存在该用户，则创建,否则失败
      if(!vertx.fileSystem().existsAwait(IDPATH)){
        try{
          vertx.fileSystem().mkdirsAwait(IDPATH)
          vertx.fileSystem().createFileAwait(IDPATH+File.separator+"password.json")
          vertx.fileSystem().createFileAwait(IDPATH+File.separator+"info.json")
          val pss=JsonObject().put(PASSWORD,json.getString(PASSWORD))
          val pss2=JsonObject().put(AUTHORITY,json.getString(AUTHORITY))
          pss2.put(LOGINSTATE,false)
          vertx.fileSystem().writeFileAwait(IDPATH+File.separator+"password.json", Buffer.buffer(pss.toString()))
          vertx.fileSystem().writeFileAwait(IDPATH+File.separator+"info.json", Buffer.buffer(pss2.toString()))
          result.put(subtype,true)
        }catch (e:Throwable){
          e.printStackTrace()
          result.put(INFO,"EXCEPTION IN SAVE PASSWORD AND INFO FOR ADMIN")
        }
      }else{
        result.put(INFO,"DO NOT HAVE THIS ADMIN")
      }
    }
  }
  private suspend fun update(json: JsonObject, result: JsonObject){
    val id=json.getString(ID)
    val DIRPATH=config.getString(DIR);
    var IDPATH=DIRPATH + File.separator +id;
    val subtype=json.getString(SUBTYPE)
    if(id==SUPERADMIN)return;

    if(vertx.fileSystem().existsAwait(IDPATH)){
      try{
        val p1=json.getString(PASSWORD);
        val savepw=vertx.fileSystem().readFileAwait(IDPATH+File.separator+"password.json")
          .toJsonObject().getString(PASSWORD)
        if(p1==savepw){
          val pss=JsonObject().put(PASSWORD,json.getString(PASSWORD2))
          val pss2=JsonObject().put(AUTHORITY,json.getString(AUTHORITY))
          pss2.put(LOGINSTATE,false)
          vertx.fileSystem().deleteAwait(IDPATH+File.separator+"password.json")
          vertx.fileSystem().deleteAwait(IDPATH+File.separator+"info.json")
          vertx.fileSystem().writeFileAwait(IDPATH+File.separator+"password.json", Buffer.buffer(pss.toString()))
          vertx.fileSystem().writeFileAwait(IDPATH+File.separator+"info.json", Buffer.buffer(pss2.toString()))
          result.put(subtype,true)
        }else{
          result.put(INFO,"PASSWORD INPUTTED NOT CORRECT!")
        }
      }catch (e:Throwable){
        e.printStackTrace()
        result.put(INFO,"EXCEPTION WHEN SAVE PASSWORD FOR ADMIN")
      }
    }else{
      result.put(INFO,"DO NOT HAVE THIS ADMIN")
    }
  }
  private suspend fun login(json: JsonObject, result: JsonObject){
    val id=json.getString(ID)
    val DIRPATH=config.getString(DIR)
    var IDPATH=DIRPATH + File.separator +id
    val subtype=json.getString(SUBTYPE)
    val password=json.getString(PASSWORD)

    if(vertx.fileSystem().existsAwait(IDPATH)){
      try{
        val savepw=vertx.fileSystem().readFileAwait(IDPATH+File.separator+"password.json")
          .toJsonObject().getString(PASSWORD)
        val loginstate=vertx.fileSystem().readFileAwait(IDPATH+File.separator+"info.json")
          .toJsonObject().getBoolean(LOGINSTATE)
        if(password==savepw&&loginstate==false){
            val infojson=vertx.fileSystem().readFileAwait(IDPATH+File.separator+"info.json")
                .toJsonObject()
          vertx.fileSystem().deleteAwait(IDPATH+File.separator+"info.json")
          infojson.put(LOGINSTATE,true)
          vertx.fileSystem().writeFileAwait(IDPATH+File.separator+"info.json",Buffer.buffer(infojson.toString()))
          result.put(subtype,true)
        }else{
          result.put(INFO,"PASSWORD NOT CORRECT! OR ALREADY LOGINED")
        }
      }catch (e:Throwable){
        e.printStackTrace()
        result.put(INFO,"EXCEPTION WHEN SAVE PASSWORD FOR ADMIN")
      }
    }else{
      result.put(INFO,"DO NOT HAVE THIS ADMIN")
    }
  }
  private suspend fun logout(json: JsonObject, result: JsonObject){
    val id=json.getString(ID)
    val DIRPATH=config.getString(DIR)
    var IDPATH=DIRPATH + File.separator +id
    val subtype=json.getString(SUBTYPE)
    val password=json.getString(PASSWORD)

    if(vertx.fileSystem().existsAwait(IDPATH)){
      try{
        val savepw=vertx.fileSystem().readFileAwait(IDPATH+File.separator+"password.json")
          .toJsonObject().getString(PASSWORD)
        val loginstate=vertx.fileSystem().readFileAwait(IDPATH+File.separator+"info.json")
          .toJsonObject().getBoolean(LOGINSTATE)
        if(password==savepw&&loginstate==true){
          val infojson=vertx.fileSystem().readFileAwait(IDPATH+File.separator+"info.json")
            .toJsonObject()
          vertx.fileSystem().deleteAwait(IDPATH+File.separator+"info.json")
          infojson.put(LOGINSTATE,false)
          vertx.fileSystem().writeFileAwait(IDPATH+File.separator+"info.json",Buffer.buffer(infojson.toString()))
          result.put(subtype,true)
        }else{
          result.put(INFO,"PASSWORD NOT CORRECT! OR ALREADY LOGINOUTED")
        }
      }catch (e:Throwable){
        e.printStackTrace()
        result.put(INFO,"EXCEPTION WHEN SAVE PASSWORD FOR ADMIN")
      }
    }else{
      result.put(INFO,"DO NOT HAVE THIS ADMIN")
    }
  }
  private suspend fun unregist(json: JsonObject, result: JsonObject){
    val id=json.getString(ID)
    val DIRPATH=config.getString(DIR)
    var IDPATH=DIRPATH + File.separator +id
    val subtype=json.getString(SUBTYPE)
    val password=json.getString(PASSWORD)
    if(id==SUPERADMIN) {
      result.put(INFO, "YOU CAN NOT UNREGIST SUPER")
      return
    }

    if(vertx.fileSystem().existsAwait(IDPATH)){
      try{
        val savepw=vertx.fileSystem().readFileAwait(IDPATH+File.separator+"password.json")
          .toJsonObject().getString(PASSWORD)
        if(password==savepw) {
          vertx.fileSystem().deleteAwait(IDPATH+File.separator+"info.json")
          vertx.fileSystem().deleteAwait(IDPATH+File.separator+"password.json")
          vertx.fileSystem().deleteAwait(IDPATH)
          result.put(subtype,true)
        }else{
          result.put(INFO,"PASSWORD IS NOT CORRECT!")
        }
      }catch (e:Throwable){
        e.printStackTrace()
        result.put(INFO,"EXCEPTION WHEN CLEAR ADMIN INFO")
      }
    }else{
      result.put(INFO,"DO NOT HAVE THIS ADMIN")
    }
  }
}
