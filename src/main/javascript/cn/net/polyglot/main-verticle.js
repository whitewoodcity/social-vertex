// import start
const File = Java.type('java.io.File');
const ConfigStoreOptions = Java.type('io.vertx.config.ConfigStoreOptions');
const ConfigRetrieverOptions = Java.type('io.vertx.config.ConfigRetrieverOptions');
const ConfigRetriever = Java.type('io.vertx.config.ConfigRetriever');
const DeploymentOptions = Java.type('io.vertx.core.DeploymentOptions');
// import end

let currentPath = process.cwd();
console.log(`Current Path: ${currentPath}`);

let config = {
  'version': 0.7,
  'dir': currentPath + File.separator + 'social-vertex',
  'jar-dir': currentPath,
  'host': 'localhost',
  'tcp-port': 7373,
  'http-port': 80,
  'https-port': 443
}

const fileStore = new ConfigStoreOptions()
  .setType("file")
  .setConfig({
    'path': 'config.json'
  });

const options = new ConfigRetrieverOptions().addStore(fileStore);
const retriever = ConfigRetriever.create(vertx, options);

(async () => {
  try {
    const conf = await retriever.getConfig();
    config = Object.assign(config, conf);
  } catch (e) {
    console.log('The configuration file: config.json does not exist or in wrong format, use default config.');
  } finally {
    retriever.close();
  }

  try {
    if (! await vertx.fileSystem().exists(config['dir'])) {
      await vertx.fileSystem().mkdir(config['dir']);
    }

    const option = new DeploymentOptions().setConfig(config);
    const verticles = [
      'kt:cn.net.polyglot.verticle.message.MessageVerticle',
      'kt:cn.net.polyglot.verticle.friend.FriendVerticle',
      'kt:cn.net.polyglot.verticle.search.SearchVerticle',
      'kt:cn.net.polyglot.verticle.user.UserVerticle',
      'kt:cn.net.polyglot.verticle.publication.PublicationVerticle',
      'kt:cn.net.polyglot.verticle.im.IMTcpServerVerticle',
      'kt:cn.net.polyglot.verticle.im.IMServletVerticle',
      'kt:cn.net.polyglot.verticle.WebServerVerticle',
      'kt:cn.net.polyglot.verticle.community.DefaultVerticle',
      'kt:cn.net.polyglot.verticle.community.LoginVerticle',
      'kt:cn.net.polyglot.verticle.community.CommunityVerticle'
    ];
    for (const verticle of verticles) {
      await vertx.deployVerticle(verticle, option);
    }
  } catch (e) {
    console.log(e);
  }
})();
