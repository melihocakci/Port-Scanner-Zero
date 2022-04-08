package com.android.test;

import java.net.InetSocketAddress;
import java.net.Socket;

public class MyRunnable implements Runnable  {
    String ip;
    int port;

    public MyRunnable(String ip, int port){
        super();
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        try{
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 5000);
            socket.close();
            System.out.println("Port " + port + " is open");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Port " + port + " is closed");
        }
    }
}
