package io.vertx.stomp;

import org.vertx.java.core.buffer.Buffer;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

class Frame {
  public String command;
  public final Map<String, String> headers;
  public final Buffer body;

  public Frame(String command, Map<String, String> headers, Buffer body) {
    this.command = command;
    this.headers = headers;
    this.body = body;
  }

  public Frame(String command, Buffer body) {
    this.command = command;
    this.headers = new HashMap<String, String>(4);
    this.body = body;
  }

  protected static Frame connectFrame() {
    return connectFrame(null, null);
  }

  protected static Frame connectFrame(String username, String password) {
    Frame frame = new Frame("CONNECT", null);
    frame.headers.put("login", username);
    frame.headers.put("passcode", password);
    return frame;
  }

  protected static Frame connectedFrame(String sessionID) {
    Frame frame = new Frame("CONNECTED", null);
    frame.headers.put("session", sessionID);
    return frame;
  }

  protected static Frame subscribeFrame(String destination) {
    Frame frame = new Frame("SUBSCRIBE", null);
    frame.headers.put("destination", destination);
    return frame;
  }

  protected static Frame unsubscribeFrame(String destination) {
    Frame frame = new Frame("UNSUBSCRIBE", null);
    frame.headers.put("destination", destination);
    return frame;
  }

  protected static Frame sendFrame(String destination, String body) {
    Buffer buff = new Buffer(body, "UTF-8");
    Frame frame = new Frame("SEND", buff);
    frame.headers.put("destination", destination);
    frame.headers.put("content-length", String.valueOf(buff.length()));
    return frame;
  }

  protected static Frame sendFrame(String destination, Buffer body) {
    Frame frame = new Frame("SEND", body);
    frame.headers.put("destination", destination);
    frame.headers.put("content-length", String.valueOf(body.length()));
    return frame;
  }

  protected static Frame receiptFrame(String receipt) {
    Frame frame = new Frame("RECEIPT", null);
    frame.headers.put("receipt-id", receipt);
    return frame;
  }

  protected static Frame errorFrame(String msg) {
    Frame frame = new Frame("ERROR", null);
    frame.headers.put("msg", msg);
    return frame;
  }

  protected static Frame messageFrame(String destination, Buffer body) {
    Frame frame = new Frame("MESSAGE", body);
    frame.headers.put("destination", destination);
    frame.headers.put("content-length", String.valueOf(body.length()));
    return frame;
  }

  public Buffer toBuffer() {
    try {
      byte[] bytes = headersString().toString().getBytes("UTF-8");
      Buffer buff = new Buffer((bytes.length + (body == null ? 0 : body.length()) + 1));
      buff.appendBytes(bytes);
      if (body != null) buff.appendBuffer(body);
      buff.appendByte((byte) 0);
      return buff;
    } catch (UnsupportedEncodingException thisWillNeverHappen) {
      return null;
    }
  }

  private StringBuilder headersString() {
    StringBuilder sb = new StringBuilder();
    sb.append(command).append('\n');
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        sb.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
      }
    }
    sb.append('\n');
    return sb;
  }

  public String toString() {
    StringBuilder buff = headersString();
    if (body != null) {
      buff.append(body.toString());
    }
    return buff.toString();
  }

  public StompMessage toStompMessage() {
    return new StompMessage(headers, body);
  }

}
