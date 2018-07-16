package cn.net.polyglot.config;

import java.io.File;

public class FileSystemConstants {
  public static final String MAIN_VERTICLE = "cn/net/polyglot/main_verticle.groovy";
  public static final String APP_DATA_DIR_NAME = ".social-vertex";
  public static final String USER_DIR = APP_DATA_DIR_NAME + File.separator + "user";

  public static final String USER_FILE = "user.json";
  public static final String FRIENDS = "friends";
}
