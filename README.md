### 服务器与服务器的即时通信协议

服务器与服务器之间通信协议使用http协议传输，内容放在http的body中，以json形式予以存放，以下是一个服务器间消息实例：

```json

{
"type":"message",
"body":"你好吗？"
}

```

其中type类型有：
message - 普通消息
friend - 添加好友
unfriend - 删除好友
reply - 好友确认