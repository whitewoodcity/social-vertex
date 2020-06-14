package cn.net.polyglot.verticle

import cn.net.polyglot.verticle.community.CommunityVerticle
import cn.net.polyglot.verticle.community.DefaultVerticle
import cn.net.polyglot.verticle.community.LoginVerticle
import cn.net.polyglot.verticle.im.IMServletVerticle
import cn.net.polyglot.verticle.web.DispatchVerticle
import io.vertx.core.http.HttpMethod

class WebServerVerticle : DispatchVerticle() {

  //设置静态文件后缀，path若是这些后缀，则自动读取对应路径文件予以返回
  override fun initDispatchVerticle() {
    super.initDispatchVerticle()//.html
    indexPage = "/default"
    staticFileSuffix.addAll(setOf("ico", "css", "js", "text", "png", "jpg", "gif", "jpeg", "mp3", "avi", "mp4", "svg", "ttf", "woff", "woff2", "eot"))
  }

  //设置http方法为put时候，http请求体缺省为application/json，但是flutter中以及需要设置Content-Type为application/json，否则会自动设为text/plain
  override fun getDefaultContentTypeByHttpMethod(httpMethod: HttpMethod): String {
    return when (httpMethod) {
      HttpMethod.PUT -> "application/json"
      else -> super.getDefaultContentTypeByHttpMethod(httpMethod)
    }
  }

  override suspend fun getVerticleAddressByPath(httpMethod: HttpMethod, path: String): String {
    return when (httpMethod) {
      HttpMethod.GET, HttpMethod.POST -> when (path) {
        "/default" -> DefaultVerticle::class.java.name
        "/login", "/profile", "/register", "/update", "/portrait" -> LoginVerticle::class.java.name
        "/prepareArticle", "/submitArticle", "/prepareModifyArticle", "/modifyArticle", "/deleteArticle", "/prepareSearchArticle", "/searchArticle", "/article", "/community", "/uploadPortrait" -> CommunityVerticle::class.java.name
        else -> ""
      }
      HttpMethod.PUT -> IMServletVerticle::class.java.name
      else -> ""
    }
  }
}
