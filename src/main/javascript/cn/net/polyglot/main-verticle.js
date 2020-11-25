const File = Java.type('java.io.File');
const JsonObject = Java.type('io.vertx.core.json.JsonObject');

let currentPath = process.cwd();
console.log(`Current Path: ${currentPath}`);

const config = new JsonObject()
  .put('version',0.7)
  .put('dir', currentPath + File.separator + 'social-vertex')
  .put('jar-dir', currentPath)
  .put('host', "localhost")
  .put('tcp-port', 7373)
  .put('http-port', 8080)
  .put('https-port', 80443);
