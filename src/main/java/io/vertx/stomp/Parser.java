package io.vertx.stomp;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.parsetools.RecordParser;

import java.util.HashMap;
import java.util.Map;

/**
 * User: timfox
 * Date: 25/06/2011
 * Time: 19:44
 */
public class Parser implements Handler<Buffer> {

  public Parser(FrameHandler output) {
    this.output = output;
  }

  private final FrameHandler output;
  private Map<String, String> headers = new HashMap<String, String>();
  private static final byte[] EOL_DELIM = new byte[]{(byte) '\n'};
  private static final byte[] EOM_DELIM = new byte[]{0};
  private final RecordParser frameParser = RecordParser.newDelimited(EOL_DELIM, new Handler<Buffer>() {
    public void handle(Buffer line) {
      handleLine(line);
    }
  });
  private String command;
  private boolean inHeaders = true;

  public void handle(Buffer buffer) {
    frameParser.handle(buffer);
  }

  private void handleLine(Buffer buffer) {
    String line = buffer.toString().trim();
    if (inHeaders) {
      if (command == null) {
        command = line;
      } else if ("".equals(line)) {
        //End of headers
        inHeaders = false;
        String sHeader = headers.get("content-length");
        if (sHeader != null) {
          int contentLength = Integer.valueOf(sHeader);
          frameParser.fixedSizeMode(contentLength);
        } else {
          frameParser.delimitedMode(EOM_DELIM);
        }
      } else {
        String[] aline = line.split(":");
        headers.put(aline[0], aline[1]);
      }
    } else {
      Frame frame = new Frame(command, headers, buffer);
      command = null;
      headers = new HashMap<>();
      inHeaders = true;
      frameParser.delimitedMode(EOL_DELIM);
      output.onFrame(frame);
    }
  }
}
