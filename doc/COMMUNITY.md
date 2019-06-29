[主菜单](../README.md)    

# 公共社区接口说明    

公共社区系统提供用户发表公开信息功能（publication），目前该系统仅支持通过HTTP请求访问，并使用PUT方法  
其子类型包括：  
用户提问（question）  
用户回答（answer）  
发表文章（article）  
发表想法（thought）  
[历史信息（history）](#历史信息)  
[获取信息（retrieve）](#获取信息)  

## 历史信息  
以下是一个时序查询历史信息的例子：  
HTTP方法: PUT  
URI: /  
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
  "from":"zxj2019",                               非必填项，查询zxj2019用户下发表的问题答案文章等，若不填则查询所有用户发表的内容
  "time":"2019-11-04-04"                          非必填项，查询2019年11月4日凌晨4点之前发表的内容，若不填则查询当前时间之前公开发表的内容
}
```  
发送上述请求至social vertex服务器后，服务器返回样例：
```json
{
  "publication":true,
  "history":[
    {"type":"publication","subtype":"question","description": "where is 小胖胖"}
  ],
  "time":"2019-11-04-04"
}
```  
解释说明  
```text
{
  "publication":true,                             true表示查询成功，false则表示失败
  "history":[                                     history为查询到的publication json列表
    {"type":"publication","subtype":"question","description": "where is 小胖胖"}
  ],
  "time":"2019-11-04-04"                          time表示查询至该时间点，下次时序查询时填入该字符串，便可继续向前查询
}
```  

## 获取信息  
以下是一个获取公开信息的例子：  
HTTP方法: PUT  
URI: /  
```json
{
  "type":"publication",
  "subtype":"retrieve",
  "dir":"/2019/06/29/15/387a71fc-f440-47ab-9d4a-bdbc7cbff5dd"
}
```  
解释说明  
```text
{
  "type":"publication",                                       固定使用publication，与系统其他类型相区分
  "subtype":"retrieve",                                       获取特定信息
  "dir":"/2019/06/29/15/387a71fc-f440-47ab-9d4a-bdbc7cbff5dd" 特性信息所在路径
}
```  
发送上述请求至social vertex服务器后，服务器返回样例：
```json
{
  "publication": true,
  "type": "publication",
  "subtype": "question"
}
```  
解释说明  
```text
{
  "publication":true,                             true表示查询成功，false则表示失败
  "type": "publication"...                        其余字段为信息存入时信息
}
```  