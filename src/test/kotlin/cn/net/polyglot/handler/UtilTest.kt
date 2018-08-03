package cn.net.polyglot.handler

import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class UtilTest{
  @Test
  fun testMd5(context: TestContext){
    context.assertEquals(md5("123456"),"e10adc3949ba59abbe56e057f20f883e")
  }
}
