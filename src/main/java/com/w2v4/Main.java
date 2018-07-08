package com.w2v4;

import io.vertx.core.Vertx;

public class Main {
	public static void main(String[] args){
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(MyFirstVerticle.class.getName());
	}
}
