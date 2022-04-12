package com.yildiz.scanner;

import java.net.InetSocketAddress;
import java.net.Socket;

public class MyRunnable implements Runnable  {
    private String ip;
    private int port;
    private boolean open;

    public MyRunnable(String ip, int port){
        super();
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        try{
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 400);
            socket.close();
            System.out.println("Port " + port + " is open");
            open = true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Port " + port + " is closed");
            open = false;
        }
    }

    public boolean getResult() {
        return open;
    }
}
