package io.vertx.stomp;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;

public class StompClient {

  private NetClient netClient;

  public StompClient(NetClient netClient) {
    this.netClient = netClient;
  }

  public void connect(int port, Handler<AsyncResult<StompConnection>> connectHandler) {
    connect(port, "localhost", null, null, connectHandler);
  }

  public void connect(int port, String host, Handler<AsyncResult<StompConnection>> connectHandler) {
    connect(port, host, null, null, connectHandler);
  }

  public void connect(int port, String host, final String username, final String password,
                      final Handler<AsyncResult<StompConnection>> connectHandler) {
    netClient.connect(port, host, new Handler<AsyncResult<NetSocket>>() {
      public void handle(AsyncResult<NetSocket> res) {
        if (res.succeeded()) {
          final StompConnection conn = new StompConnection(res.result());
          conn.connect(username, password, new Handler<AsyncResult<StompConnection>>() {
            public void handle(AsyncResult<StompConnection> res2) {
              connectHandler.handle(res2);
            }
          });
        } else {
          connectHandler.handle(new DefaultFutureResult<StompConnection>(res.cause()));
        }
      }
    });
  }
}


