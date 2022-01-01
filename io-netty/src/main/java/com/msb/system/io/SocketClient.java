package com.bjmashibing.system.io;

import java.io.*;
import java.net.Socket;

public class SocketClient {

  public static void main(String[] args) {

    try {
      Socket client = new Socket("192.168.2.88", 9090);
      client.setSendBufferSize(20);
      client.setTcpNoDelay(true);

      OutputStream out = client.getOutputStream();

      InputStream in = System.in;
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      while (true) {
        String line = reader.readLine();
        System.out.println("line: " + line);
        if (line != null) {
          byte[] bb = (line + "\n").getBytes();
          for (byte b : bb) {
            out.write(b);
          }
          //System.out.println("client shutting down output");
          //client.shutdownOutput();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
