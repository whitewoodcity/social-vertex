
[系统说明](SYSTEM.md)

# 服务器与服务器的即时通信协议 V0.1

服务器与服务器之间通信协议使用http协议传输，服务器端与客户端使用tcp协议，内容放在http的body中，以json形式予以存放，以下是一个服务器间消息实例：  
![sample](https://user-images.githubusercontent.com/5525436/42436872-f00af064-838d-11e8-8445-4f197b88508b.png)

```json
{
"type":"message",
"to":"customer@w2v4.com",
"body":"你好吗？",
"version":0.1
}
```
消息根据type类型分为几个大类，再根据subtype分为小类  
type类型有：  
[用户类型](#1)  
[搜索类型](#2)  
[消息类型](#3)

<h3 id=1> user - 用户类型</h3>  

> - 传输时 `password` 为加密后的密码内容，使用 `MD5` 进行加密。如有需要请酌情增加加密选项。  
> - 示例中的 `password` 的值为 `zxj5470` 的 MD5 加密后的内容

#### 用户注册
```json
{
"type":"user",
"subtype":"register",
"id":"zxj5470",
"password":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
```
返回结果  
```json
{
"register":true
}
```

#### 错误样例  
- 用户名长度小于 4 或以数字开头时显示 `用户名格式错误`。
- 密码格式不正确（验证 MD5 长度为 32）
- 有对应通信客户端时建议直接在客户端部分做好验证。但若提供 Web Service API 则提示如下信息。  
```json
{
  "info": "用户名格式错误",
  "register": false
}
```

#### 用户登录
```json
{
"type":"user",
"subtype":"login",
"id":"zxj5470",
"password":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
```
登录返回结果

成功  
```json

{
"login":true,
"nickname":"哲学家",
"friends":[
  {"id":"yangkui","nickname":"杨奎"},
  {"id":"xiaopan","nickname":"小胖胖"}
  ]
}

```

失败
```json
{
"login":false
}
```

#### 用户离线消息
```json
{
"type":"user",
"subtype":"left",
"id":"zxj5470",
"password":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
```
离线返回结果

成功  
```json

{
"left":true,
"messages":[
  {"type":"message","subtype":"text","from":"yangkui","body":"在吗？"},
  {"type":"message","subtype":"text","from":"yangkui","body":"小胖胖过来了，一起吃饭"}
  ],
"friends":[
  {"type":"friend","subtype":"request","from":"zhaoce","message":"请加我为好友"}
  ]
}

```

失败
```json
{
"left":false
}
```

<h3 id=2> search - 搜索类型</h3>  

定义搜索用户请求及响应  
```json
{
"type":"search",
"subtype":"info",
"keyword":"zxj2017",
"version":0.1
}
```
```json
{
"user":{"id":"zxj2017","nickname":"哲学家"}
}
```
查询无结果
```json
{
"user":null
}
```
<h3 id=3> message - 消息类型</h3>  

发送消息
```json
{
"type":"message",
"subtype":"text",
"to":"yangkui",
"body":"你好吗？",
"version":0.1
}
```

### friend - 好友类型  
#### 添加好友  
```json
{
"type":"friend",
"subtype":"request",
"to":"customer@w2v4.com",
"message":"请添加我为你的好友，我是哲学家",
"version":0.1
}
```
#### 删除好友  
```json
{
"type":"friend",
"subtype":"delete",
"to":"customer@w2v4.com",
"version":0.1
}
```
#### 答复好友请求
> 以下的 json 结果表示 `to` 用户 接受了 `from` 用户的好友请求。
```json
{
"type":"friend",
"subtype":"response",
"to":"customer@w2v4.com",
"accept":true,
"version":0.1
}
```
