package com.yildiz.scanner;

import static java.lang.Thread.sleep;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class Scanner implements Runnable{
    private static InetAddress host;
    private static LinkedList<Integer> portList;
    private static int threadNum;
    private static final LinkedList<Integer> openPorts = new LinkedList<Integer>();
    private static final ReentrantLock mutex = new ReentrantLock();
    private static boolean stop;

    public static LinkedList<Integer> scanPorts(InetAddress address, LinkedList<Integer> list) {
        host = address;
        portList = list;
        openPorts.clear();
        stop = false;

        // decide on thread number
        threadNum = (int) Math.sqrt(list.size());
        if(threadNum > 128) {
            threadNum = 128;
        }

        LinkedList<Thread> threads = new LinkedList<Thread>();
        // launch threads
        for(int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(new Scanner());
            thread.start();
            threads.add(thread);
        }

        // wait for threads to finish
        for(Thread thread: threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return openPorts;
    }

    public static void stopScan() {
        mutex.lock();
        stop = true;
        mutex.unlock();
    }

    @Override
    public void run() {
        while(true) {
            // lock mutex
            mutex.lock();
            // return if port list is empty
            if(stop || portList.isEmpty()) {
                threadNum--;
                mutex.unlock();
                return;
            }
            // get port
            int port = portList.getFirst();
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
