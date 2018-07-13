# 服务器与服务器的即时通信协议 V0.1

服务器与服务器之间通信协议使用http协议传输，服务器端与客户端使用tcp协议，内容放在http的body中，以json形式予以存放，以下是一个服务器间消息实例：  
![sample](https://user-images.githubusercontent.com/5525436/42436872-f00af064-838d-11e8-8445-4f197b88508b.png)
```json
{
"type":"message",
"from":"inquiry@polyglot.net.cn",
"to":"customer@w2v4.com",
"body":"你好吗？",
"version":0.1
}
```
type类型有：  
### search - 搜索类型
定义搜索用户请求及响应  
```json
{
"type":"search",
"action":"request",
"user":"zxj@polyglot.net.cn",
"version":0.1
}
```
```json
{
"type":"search",
"action":"response",
"user":{"id":"zxj@polyglot.net.cn","nickname":"哲学家"},
"version":0.1
}
```
查询无结果
```json
{
"type":"search",
"action":"response",
"user":null,
"version":0.1
}
```
### message - 消息类型
```json
{
"type":"message",
"from":"inquiry@polyglot.net.cn",
"to":"customer@w2v4.com",
"body":"你好吗？",
"version":0.1
}
```
### friend - 好友类型  
#### 添加好友  
```json
{
"type":"friend",
"action":"request",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"message":"请添加我为你的好友，我是哲学家",
"version":0.1
}
```
#### 删除好友  
```json
{
"type":"friend",
"action":"delete",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"version":0.1
}
```
#### 答复好友请求
```json
{
"type":"friend",
"action":"response",
"from":"zxj@polyglot.net.cn",
"to":"customer@w2v4.com",
"accept":true,
"version":0.1
}
```

### user - 用户类型
> 传输时密码为 `crypto` 使用加密后的内容，默认使用 `MD5` 进行加密
> 示例中的 `crypto` 为 `zxj5470` 的 MD5 加密后的内容

#### 用户登录
```json
{
"type":"user",
"action":"login",
"user":"zxj5470",
"crypto":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
```
登录返回结果  
```json
{
"type":"user",
"action":"login",
"user":{
  "id":"zxj5470"
  },
"version":0.1,
"login":true
}
```

#### 用户注册
```json
{
"type":"user",
"action":"registry",
"user":"zxj5470",
"crypto":"431fe828b9b8e8094235dee515562247",
"version":0.1
}
```
返回结果  
```json
{
"type":"user",
"action":"registry",
"user":"zxj5470",
"version":0.1,
"info":"succeed."
}
```
错误结果  
- 用户名长度小于 4 或其他错误。（如有客户端则在验证，但在提供 Web API 服务时会在此处再次验证。）  
```json
{
  "type": "user",
  "action": "login",
  "user": " ",
  "version": 0.1,
  "info": "用户名格式错误",
  "registry": false
}
```