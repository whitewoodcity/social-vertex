package cn.net.polyglot;

import io.vertx.core.Launcher;

import static cn.net.polyglot.config.FileSystemConstants.MAIN_VERTICLE;


public class Main {
  public static void main(String[] args) {
    Launcher.executeCommand("run", MAIN_VERTICLE);
  }
}
