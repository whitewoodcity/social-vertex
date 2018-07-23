# 系统配置说明

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