package net.azib.ipscan.util;

import org.savarese.rocksaw.net.RawSocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;

public class IOUtils {
  public static void closeQuietly(Socket socket) {
    if (socket != null) try {
      socket.close();
    }
    catch (IOException ignore) {
    }
  }

  public static void closeQuietly(DatagramSocket socket) {
    if (socket != null) socket.close();
  }

  public static void closeQuietly(RawSocket socket) {
    if (socket != null) try {
      socket.close();
    }
    catch (IOException ignore) {
    }
  }

  public static void closeQuietly(Closeable closeable) {
    if (closeable != null) try {
      closeable.close();
    }
    catch (IOException ignore) {
    }
  }
}
