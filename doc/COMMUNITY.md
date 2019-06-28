[主菜单](../README.md)    

# 公共社区接口说明    

公共社区系统提供用户发表公开信息功能（publication），目前该系统仅支持通过HTTP请求访问，并使用PUT方法  
其子类型包括：  
用户提问（question）  
用户回答（answer）  
发表文章（article）  
发表想法（thought）  
[历史信息（history）](#历史信息)  
获取信息（retrieve）  

##历史信息  
以下是一个时序查询历史信息的例子：  
HTTP方法: PUT  
RUI: /  
```json
{
  "type":"publication",
  "subtype":"history",
  "id":"zxj2019",
  "password":"431fe828b9b8e8094235dee515562247",
  "from":"zxj2019",
  "time":"2019-11-04-04"
}
```  
解释说明  
```text
{
  "type":"publication",                           固定使用publication，与系统其他类型相区分
  "subtype":"history",                            history表示时序查询
  "id":"zxj2019",                                 用户名
  "password":"431fe828b9b8e8094235dee515562247",  密码
  "from":"zxj2019",                               非必填项，搜索zxj2019用户下发表的问题答案文章等，若不填则搜索所有用户发表的内容
  "time":"2019-11-04-04"                          非必填项，搜索2019年11月4日凌晨4点之前发表的内容，若不填则搜索当前时间之前公开发表的内容
}
```  