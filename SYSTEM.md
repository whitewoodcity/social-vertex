
[即时通信协议](README.md)

# 系统说明

## 启动
Unix 环境下作为后台程序启动运行 `social-vertex-${version}-prod.jar` 包。

```bash
java -jar social-vertex-0.1-prod.jar 1>>log.out 2>>err.out &
```
分别将 `stdout` 和 `stderr` 的输出重定向至 `log.out` 和 `err.out` 文件。

## 配置文件
当social-vertx启动时，系统会尝试读取jar同一目录下的config.json文件，若该文件不存在，则使用缺省配置，缺省配置如下：

```json
{ 
  "version":0.1,
  "dir":"./social-vertex",
  "tcp-port":7373,
  "http-port":7575,
  "host":"localhost"
}
```

若config.json文件存在，则系统会尝试将读取来的配置文件覆盖原有配置

## 用户目录
系统中针对每一个用户，都将建立独立的文件夹，例如对于用户yangkui而言，  
系统会新建一个${dir}/yangkui文件夹，该文件夹下必需包含有user.json文件，  
user.json文件储存用户的信息，例如密码，昵称，手机号等，  
亦可能包含有.send，.receive以及.message三个文件夹，  
.send和.receive文件夹分别储存发送的好友请求以及收到的好友请求信息，  
.message文件夹用于储存用户离线时，收到的消息。  
当用户成功建立好友联系，则会在该用户文件夹下新建一个好友同名文件夹，以存储相关信息，  
该文件夹下包括该用户同名json以及按照日期建立的聊天日志文件：
```text       
├── yangkui    
│   ├── user.json    
│   ├── .send    
│   │    ├──zxj2017.json  
│   ├── .received  
│   │    ├──chengenzhao.json  
│   ├── .message  
│   │    ├──zxj2017.sv  
│   ├── zhang3   
│   │    ├──zhang3.json  
│   │    ├──2018-08-07.sv  
│   ├── li4  
│   │    ├──li4.json  
│   │    ├──2018-08-08.sv  
```

后缀为json文件格式均可直接解析为Json，后缀为sv文件格式则是多个json的集合，json与json之间通过/r/n区分，例如一个典型的sv文件如下：  
```json
{"type":"message","subtype":"text","to":"zxj2017","body":"你好吗？","from":"yangkui"}
"/r/n"
{"type":"message","subtype":"text","to":"zxj2017","body":"你好吗？","from":"yangkui"}
"/r/n"  
```
系统可根据需要用"/r/n"做切割成独立的Json字符串。  
.send和.receive文件夹下的Json为好友请求Json，格式可参考[即时通信协议添加请求部分](README.md#添加请求)  
好友同名文件夹下的好友同名json格式同user.json一致  
所有sv文件内Json均为消息类型，格式可参考[即时通信协议消息类型部分](README.md#message---消息类型)  