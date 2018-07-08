# 服务器与服务器的即时通信协议 V0.1

服务器与服务器之间通信协议使用http协议传输，内容放在http的body中，以json形式予以存放，以下是一个服务器间消息实例：

```json
{
"type":"message",
"body":"你好吗？"
}
```
其中type类型有：  
### search - 搜索类型
定义搜索用户请求及响应  
```json
{
"type":"search",
"action":"request",
"user":"zxj@polyglot.net.cn"
}
```
```json
{
"type":"search",
"action":"response",
"user":{"id":"zxj@polyglot.net.cn","nickname":"哲学家"}
}
```
```json
{
"type":"search",
"action":"response",
"user":null
}
```
### message - 消息类型
```json
{
"type":"message",
"sender":"inquiry@polyglot.net.cn",
"receiver":"customer@w2v4.com",
"body":"你好吗？"
}
```

friend - 请求类型  
follow - 跟随类型  