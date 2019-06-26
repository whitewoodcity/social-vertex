
[系统说明](SYSTEM.md)  
[公共社区系统说明](doc/COMMUNITY.md)    
[即时消息系统说明](doc/MESSENGER.md)    

# 即时通信协议 V0.4

## 术语

* 消息：不同物理机器之间的信息传递的最小单位，以[Json](https://www.json.org/)形式封装，传输协议使用Http或Tcp  
* 双向：当消息发出之后，需等待对方响应，如此一问一答式的消息称为双向消息  
* 单向：当消息发出之后，无需对方响应的消息称为单向消息，当使用Http协议传输单向消息时，服务器收到请求之后会立即发送空响应

## 传输协议

服务器与服务器之间通信使用Http协议传输，服务器端与客户端使用Tcp和Http协议传输，当使用Http协议传输时，IM使用的方法为PUT方法，对应的路径为/:type/:subtype，其余消息内容放在body中，以Json形式予以存放，以下便是一个消息传输实例：  
![sample](https://user-images.githubusercontent.com/5525436/42436872-f00af064-838d-11e8-8445-4f197b88508b.png)

method: PUT  
uri: /message/text

```json
{
  "to":"yangkui@w2v4.com",
  "body":"你好吗？",
  "version":0.2
}
```
以下是一个Tcp协议传输消息的例子：

```json
{
  "type":"message",
  "subtype":"text",
  "to":"yangkui",
  "body":"你好吗？",
  "version":0.2
}
```
值得注意的是，通过Http协议传输的Json中的type和subtype必填字段：双向消息通过Http协议传输，单向消息通过Tcp协议传输，仅登陆等消息亦会通过Tcp协议传输，Tcp协议使用"/r/n"拆包  

## 封装协议（格式）

消息使用Json格式封装，根据type类型分为大类，再根据subtype分为小类，其中type类型有：  
[用户类型（双向消息）](#user---用户类型)  
[搜索类型（双向消息）](#search---搜索类型)   
[好友类型（单向消息）](#friend---好友类型)  
[消息类型（单向消息）](#message---消息类型)  

### user - 用户类型   
[用户注册（双向消息）](#用户注册)  
[用户登陆（双向消息）](#用户登录)  
离线消息（双向消息）  
[历史消息（双向消息）](#历史消息)  

#### 用户注册  
method: PUT  
uri: /  
```json
{
  "type":"user",
  "subtype":"register",
  "id":"zxj2019",
  "password":"431fe828b9b8e8094235dee515562247",
  "password2":"431fe828b9b8e8094235dee515562247",
  "nickname":"哲学家2019",
  "version":0.4
}
```  
>传输时 `password` 和 `password2` 为加密后的密码内容，使用 `MD5` 进行加密。    

响应  
```json
{
  "register":true
}
```
或
```json
{
  "register": false,
  "info": "用户名格式错误"
}
```

#### 用户登录  
method: PUT  
uri: /
```json
{
  "type":"user",
  "subtype":"login",
  "id":"zxj5470",
  "password":"431fe828b9b8e8094235dee515562247",
  "version":0.4
}
```
>传输时 `password` 为加密后的密码内容，使用 `MD5` 进行加密。  

响应  
```json
{
  "type":"user",
  "subtype":"login",
  "login":true,
  "nickname":"哲学家",
  "friends":[
    {"id":"yangkui","nickname":"杨奎"},
    {"id":"xiaopan","nickname":"小胖胖"}
  ],
  "notifications":[
    {"type":"friend","subtype":"request","from":"zhaoce","message":"请加我为好友"}
  ]
}
```
或
```json
{
  "login":false
}
```

登陆时需建立Tcp连接，并发送
```json
{
  "type":"login",
  "id":"zxj5470",
  "password":"431fe828b9b8e8094235dee515562247",
  "version":0.4
}
```
以识别该连接

#### 历史消息
method:PUT       
uri:/
```json
{
 "type":"message",
 "subtype":"history",
 "date":"2018-08-17",
 "id":"zxj2017",
 "password":"431fe828b9b8e8094235dee515562247",
 "friend":"yangkui",
 "version":"0.4"
}
```

>查询2018-08-17之前的聊天记录，若此项为空，则读取最新的大于20条聊天记录。  

响应
```json
{
  "message":true,
  "history":[
    {
      "type":"message","subtype":"text","to":"zxj2017","body":"你好吗？","version":0.1,"from":"yangkui"
    },
    {
      "type":"message","subtype":"text","to":"zxj2017","body":"你好吗？","version":0.1,"from":"yangkui"
    }
  ]
}
```  
或
```json
{
  "message":false
}
```  

### search - 搜索类型   
[搜索信息（双向消息）](#搜索信息)  

#### 搜索信息  
method: PUT  
uri: /search/info  
```json
{
  "type":"search",
  "subtype":"info",
  "keyword":"zxj2017",
  "version":0.2
}
```
响应
```json
{
  "user":{"id":"zxj2017","nickname":"哲学家"}
}
```
或
```json
{
  "user":null
}
```
### friend - 好友类型  
[添加请求（单向消息）](#添加请求)  
[答复请求（单向消息）](#答复请求)  

#### 添加请求  
method: PUT  
uri: /friend/request  
```json
{
  "type":"friend",
  "subtype":"request",
  "to":"customer@w2v4.com",
  "message":"请添加我为你的好友，我是哲学家",
  "version":0.2
}
```
#### 答复请求
以下的 json 结果表示当前用户接受了来自`to`用户的好友请求。  
method: PUT  
uri: /friend/response  
```json
{
  "type":"friend",
  "subtype":"response",
  "to":"customer@w2v4.com",
  "accept":true,
  "version":0.2
}
```
### message - 消息类型  
[文本消息（单向消息）](#文本消息)  

#### 文本消息  
method: PUT  
uri: /message/text  
```json
{
  "type":"message",
  "subtype":"text",
  "to":"yangkui",
  "body":"你好吗？",
  "uuid":"367d76c0962011e88dcb0b109a354f54",
  "version":0.2
}
```
值得留意的是：每一个消息都建议添加uuid以便识别，虽然该域并非必需。
