/*
 * ----------------------------------------------------------------------------
 *  (C) Copyright Testing Technologies.  All Rights Reserved.
 *
 *  All copies of this program, whether in whole or in part, and whether
 *  modified or not, must display this and all other embedded copyright
 *  and ownership notices in full.
 *
 *  See the file COPYRIGHT for details of redistribution and use.
 *
 *  You should have received a copy of the COPYRIGHT file along with
 *  this file; if not, write to the Testing Technologies,
 *  Michaelkirchstr. 17/18, 10179 Berlin, Germany.
 *
 *  TESTING TECHNOLOGIES DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS
 *  SOFTWARE. IN NO EVENT SHALL TESTING TECHNOLOGIES BE LIABLE FOR ANY
 *  SPECIAL, DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
 *  AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
 *  ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 *  THIS SOFTWARE.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
 *  EITHER EXPRESSED OR IMPLIED, INCLUDING ANY KIND OF IMPLIED OR
 *  EXPRESSED WARRANTY OF NON-INFRINGEMENT OR THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 * ----------------------------------------------------------------------------
 */
package canfilter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

public class Car2xEntryUpdater extends Thread {

  private static final String DEFAULT_TCP_SERVER_HOST = "localhost";
  private static final int DEFAULT_TCP_SERVER_PORT = 50001;

  private Hashtable<String, Car2XEntry> car2xEntries;
  private InetSocketAddress address;

  /**
   * Updates openXC resp. obd2 values within a hashtable.
   * @param car2xEntries
   */
  public Car2xEntryUpdater(Hashtable<String, Car2XEntry> car2xEntries) {
    this.car2xEntries = car2xEntries;
    this.address = new InetSocketAddress(DEFAULT_TCP_SERVER_HOST,
        DEFAULT_TCP_SERVER_PORT);
  }

  /**
   * Connects to the openXC simulator and updates values as long as the
   *  hashtable has entries.
   */
  public void run() {
    System.out.println("Entry updater started.");
    Socket sock = null;
    Scanner scanner = null;

    try {
      // establish the socket
      sock = new Socket();
      sock.connect(address);
      System.out.println("Entry updater connected to host : "
          + address.getHostName() + " on local port "
          + address.getPort());

      scanner = new Scanner(sock.getInputStream()).useDelimiter("}");

      while (!car2xEntries.isEmpty()) {
        JSONObject jsonObject = new JSONObject(scanner.next() + "}");
        Car2XEntry car2xEntry = car2xEntries
            .get(jsonObject.get("name"));
        if (car2xEntry != null) {
          updateEntry(car2xEntry, jsonObject);
        }
      }

    } catch (SocketTimeoutException e) {
      System.out.println("[Timeout] Could not connect to "
          + address.getHostName() + ":" + address.getPort());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          sock.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (scanner != null) {
        scanner.close();
      }
      System.out.println("Entry updater stopped.");
    }
  }

  // TODO add obd2 key
  private void updateEntry(Car2XEntry car2xEntry, JSONObject jsonObject)
      throws JSONException {
    car2xEntry.setTimestamp(new Date().getTime());
    car2xEntry.setValueA(jsonObject.get("value").toString());
    String event = jsonObject.getString("event");
    if (event != null) {
      car2xEntry.setValueB(event);
    }
  }
}
