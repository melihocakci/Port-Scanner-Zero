package com.yildiz.scanner;

import android.annotation.SuppressLint;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class Scanner implements Runnable{
    private static String host;
    private static LinkedList<Integer> portList;
    @SuppressLint("StaticFieldLeak")
    private static ScanActivity scanActivity;
    private static final LinkedList<Integer> openPorts = new LinkedList<Integer>();
    private static final ReentrantLock mutex = new ReentrantLock();
    private static int threadNum;

    public static void startScan(String str, LinkedList<Integer> list, ScanActivity activity) {
        host = str;
        portList = list;
        scanActivity = activity;
        openPorts.clear();
        // launch threads
        if(list.size() < 128) {
            threadNum = list.size();
        } else {
            threadNum = 128;
        }
        for(int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(new Scanner());
            thread.start();
        }
    }

    @Override
    public void run() {
        int port;
        while(true) {
            // lock mutex
            mutex.lock();
            // return if port list is empty
            if(portList.isEmpty()) {
                if(threadNum == 1) {
                    scanActivity.results(openPorts);
                }
                threadNum--;
                mutex.unlock();
                return;
            }
            // get port
            port = portList.getFirst();
            portList.removeFirst();
            // get socket
            Socket socket = new Socket();
            // unlock mutex
            mutex.unlock();
            try {
                // start connection
                socket.connect(new InetSocketAddress(host, port), 1000);
                // connection successful, end connection
                socket.close();
                mutex.lock();
                openPorts.add(port);
                mutex.unlock();
                //System.out.println("Port " + port + " is open");
            } catch (Exception ex) {
                // connection failed
            }
        }
    }

}
