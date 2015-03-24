package com.testingtech.playits.canfilter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponderTask extends TimerTask {

  private String key;
  private Car2XEntry car2xEntry;
  private Socket socket;

  /**
   * Periodically sends a key-value par encoded as a JSON String over a socket's
   * output stream.
   * @param key openxc key
   * @param car2xEntry corresponding value
   * @param socket TCP responding channel
   */
  public ResponderTask(String key, Car2XEntry car2xEntry, Socket socket) {
    this.key = key;
    this.car2xEntry = car2xEntry;
    this.socket = socket;
  }

  @Override
  public void run() {
    if (car2xEntry.getTimestamp() > 0) {
      try {
        JSONObject response = createResponse();
        send(response);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  private JSONObject createResponse() throws JSONException {
    JSONObject response = new JSONObject();
    response.put("Timestamp", car2xEntry.getTimestamp());
    response.put("OpenXCKey", key);
    response.put("OBD2Key", car2xEntry.getObd2key());
    response.put("valueA", car2xEntry.getValueA());
    response.put("valueB", car2xEntry.getValueB());
    return response;
  }

  private void send(JSONObject jsonObject) {
    String jsonAsString = jsonObject.toString();
    byte[] bytes = jsonAsString.getBytes(Charset.forName("UTF-8"));
    OutputStream outputStream;
    try {
      outputStream = socket.getOutputStream();
      outputStream.write(bytes);
      outputStream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}