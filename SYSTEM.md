# 系统配置说明

## 启动
Unix 环境下作为后台程序启动运行 `social-vertex-0.1-prod.jar` 包。

```bash
java -jar social-vertex-0.1-prod.jar 1>>suc_`date +%Y%m%d`.txt 2>>err_`date +%Y%m%d`.txt &
```
分别将 `stdout` 和 `stderr` 的输出重定向至 `suc_20180801.txt` 和 `err_20180801.txt` 文件。

## 配置文件
当social-vertx启动时，系统会尝试读取jar同一目录下的config.json文件，若该文件不存在，则使用缺省配置，缺省配置如下：

```json
{ 
  "version":0.1,
  "dir":"./social-vertex",
  "tcp-port":7373,
  "http-port":7575
}
```

若config.json文件存在，则系统会尝试将读取来的配置文件覆盖原有配置

## 用户目录
系统中针对每一个用户，都将建立独立的文件夹，例如对于用户yangkui而言，
系统会新建一个${dir}/yangkui文件夹，该文件夹下必需包含有user.json文件，
可能包含有.send和.receive两个文件夹，user.json文件储存用户的信息，例如密码，昵称，手机号等，
.send和.receive文件夹分别储存发送的好友请求以及收到的好友请求信息，
当用户成功建立好友联系，则会在该用户文件夹下新建一个文件夹，以存储相关信息：
```text       
├── yangkui    
│   ├── user.json    
│   ├── .send    
│   │    ├──zxj2017.json  
│   ├── .received  
│   │    ├──chengenzhao.json  
│   ├── zhang3   
│   ├── li4  
```