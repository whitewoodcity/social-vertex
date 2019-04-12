package cn.net.polyglot.verticle.community

import cn.net.polyglot.verticle.im.IMServletVerticle
import cn.net.polyglot.verticle.web.DispatchVerticle
import io.vertx.core.http.HttpMethod

class WebServerVerticle: DispatchVerticle() {
  override suspend fun getVerticleAddressByPath(httpMethod: HttpMethod, path: String): String {
    return when(httpMethod){
      HttpMethod.GET, HttpMethod.POST -> when(path){
        "/login","/profile","/register","/update","/portrait" -> LoginVerticle::class.java.name
        "/prepareArticle", "/submitArticle","/prepareModifyArticle","/modifyArticle","/deleteArticle","/prepareSearchArticle","/searchArticle","/article", "/community","/uploadPortrait" -> CommunityVerticle::class.java.name
        else -> ""
      }
      HttpMethod.PUT -> IMServletVerticle::class.java.name
      else -> ""
    }
  }
}
