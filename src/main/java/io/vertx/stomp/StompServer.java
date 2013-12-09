package io.vertx.stomp;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;

import java.util.*;

/**
 * User: timfox
 * Date: 28/06/2011
 * Time: 00:19
 * <p/>
 * Simple STOMP 1.0 server implementation - doesn't currently handle transactions or acks and just does basic pub/sub
 */
public class StompServer {

  private final Vertx vertx;
  private final NetServer netServer;
  private final EventBus eventBus;

  public StompServer(Vertx vertx, NetServer netServer) {
    this.vertx = vertx;
    this.netServer = netServer;
    this.eventBus = vertx.eventBus();
    netServer.connectHandler(new Handler<NetSocket>() {
      @Override
      public void handle(NetSocket sock) {
        onConnect(sock);
      }
    });
  }

  private Map<String, Handler<Message>> messageHandlers = new HashMap<>();

  private void subscribe(String dest, StompConnection conn) {
    if (messageHandlers.containsKey(dest)) {
      // Already subscribed
      sendError("Already subscribed to " + dest, conn);
    } else {
      Handler<Message> handler = new Handler<Message>() {
        @Override
        public void handle(Message message) {
          // TODO

        }
      };
      eventBus.registerHandler(dest, handler);
      messageHandlers.put(dest, handler);
    }
  }

  private void unsubscribe(String dest, StompConnection conn) {
    Handler<Message> messageHandler = messageHandlers.get(dest);
    if (messageHandler == null) {
      sendError("Not subscribed to " + dest, conn);
    } else {
      eventBus.unregisterHandler(dest, messageHandler);
    }
  }

  private void sendError(String msg, StompConnection stompConnection) {
    Frame frame = Frame.errorFrame(msg);
    stompConnection.write(frame);
  }

  private void checkReceipt(Frame frame, StompConnection conn) {
    String receipt = frame.headers.get("receipt");
    if (receipt != null) {
      conn.write(Frame.receiptFrame(receipt));
    }
  }

  private void onConnect(final NetSocket sock) {
    final StompServerConnection conn = new StompServerConnection(sock);
    conn.frameHandler(new FrameHandler() {
      public void onFrame(Frame frame) {
        String dest;
        switch (frame.command) {
          case "CONNECT":
            conn.write(Frame.connectedFrame(UUID.randomUUID().toString()));
            return;
          case "SUBSCRIBE":
            dest = frame.headers.get("destination");
            subscribe(dest, conn);
            break;
          case "UNSUBSCRIBE":
            dest = frame.headers.get("destination");
            unsubscribe(dest, conn);
            break;
          case "SEND":
            dest = frame.headers.get("destination");
            //TODO
            ///JsonObject msg = new JsonObject();
            //eventBus.send(dest, message);
            break;
          default:
            throw new IllegalStateException("Unknown command: " + frame.command);
        }
        checkReceipt(frame, conn);
      }
    });
  }

}
