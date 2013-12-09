package io.vertx.stomp;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.shareddata.Shareable;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * User: tim
 * Date: 27/06/11
 * Time: 18:31
 */
public class StompConnection implements Shareable{

  private final NetSocket socket;
  private Handler<StompMessage> errorHandler;
  private Handler<AsyncResult<StompConnection>> connectHandler;
  protected boolean connected;
  private Map<String, Handler<StompMessage>> subscriptions = new HashMap<>();
  private Map<String, Handler<Void>> waitingReceipts = new HashMap<>();

  protected StompConnection(NetSocket socket) {
    this.socket = socket;
    socket.dataHandler(new Parser(new FrameHandler() {
      public void onFrame(Frame frame) {
        handleFrame(frame);
      }
    }));
  }

  public void errorHandler(Handler<StompMessage> errorHandler) {
    this.errorHandler = errorHandler;
  }

  public void close() {
    socket.close();
  }

  // Send without receipt
  public void send(String dest, Buffer body) {
    send(dest, new HashMap<String, String>(4), body, null);
  }

  // Send without receipt
  public void send(String dest, Map<String, String> headers, Buffer body) {
    send(dest, headers, body, null);
  }

  // Send with receipt
  public void send(String dest, Buffer body, Handler<Void> receiptCallback) {
    send(dest, new HashMap<String, String>(4), body, receiptCallback);
  }

  // Send with receipt
  public void send(String dest, Map<String, String> headers, Buffer body, Handler<Void> receiptCallback) {
    Frame frame = new Frame("SEND", headers, body);
    frame.headers.put("destination", dest);
    addReceipt(frame, receiptCallback);
    write(frame);
  }

  // Subscribe without receipt
  public void subscribe(String dest, Handler<StompMessage> subscription) {
    subscribe(dest, subscription, null);
  }

  // Subscribe with receipt
  public void subscribe(String dest, Handler<StompMessage> subscription, Handler<Void> receiptHandler) {
    if (subscriptions.containsKey(dest)) {
      throw new IllegalArgumentException("Already subscribed to " + dest);
    }
    subscriptions.put(dest, subscription);
    Frame frame = Frame.subscribeFrame(dest);
    addReceipt(frame, receiptHandler);
    write(frame);
  }

  // Unsubscribe without receipt
  public void unsubscribe(String dest) {
    unsubscribe(dest, null);
  }

  //Unsubscribe with receipt
  public void unsubscribe(String dest, Handler<Void> receiptHandler) {
    subscriptions.remove(dest);
    Frame frame = Frame.unsubscribeFrame(dest);
    addReceipt(frame, receiptHandler);
    write(frame);
  }

  public void write(Frame frame) {
    socket.write(frame.toBuffer());
  }

  protected void connect(String username, String password, Handler<AsyncResult<StompConnection>> connectHandler) {
    this.connectHandler = connectHandler;
    write(Frame.connectFrame(username, password));
  }

  private void handleMessage(Frame msg) {
    String dest = msg.headers.get("destination");
    Handler<StompMessage> sub = subscriptions.get(dest);
    sub.handle(msg.toStompMessage());
  }

  private void addReceipt(Frame frame, Handler<Void> receiptHandler) {
    if (receiptHandler != null) {
      String receipt = UUID.randomUUID().toString();
      frame.headers.put("receipt", receipt);
      waitingReceipts.put(receipt, receiptHandler);
    }
  }

  protected void handleFrame(Frame frame) {
    switch (frame.command) {
      case "CONNECTED":
        connected = true;
        connectHandler.handle(new DefaultFutureResult<>(this));
        break;
      case "MESSAGE":
        handleMessage(frame);
        break;
      case "RECEIPT":
        String receipt = frame.headers.get("receipt-id");
        Handler<Void> receiptHandler = waitingReceipts.get(receipt);
        if (receiptHandler == null) {
          throw new IllegalStateException("No receipt handler for receipt " + receipt);
        }
        receiptHandler.handle(null);
        break;
      case "ERROR":
        if (!connected) {
          String msg = frame.headers.get("msg");
          if (msg == null) {
            msg = "Unknown error";
          }
          connectHandler.handle(new DefaultFutureResult<StompConnection>(new ConnectException(msg)));
        } else if (errorHandler != null) {
          errorHandler.handle(frame.toStompMessage());
        }
      default:
        throw new IllegalStateException("Unknown command: " + frame.command);
    }

  }
}
