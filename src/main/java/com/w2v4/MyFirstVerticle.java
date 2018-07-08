package com.w2v4;

import io.vertx.core.AbstractVerticle;

public class MyFirstVerticle extends AbstractVerticle {
	public void start() {
		vertx.createHttpServer().requestHandler(req -> {
			req.response()
					.putHeader("content-type", "text/plain")
					.end("Hello World!");
		}).listen(8080);
	}
}