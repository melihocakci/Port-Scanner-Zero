package com.yildiz.scanner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class Scanner implements Runnable{
    private static InetAddress host;
    private static LinkedList<Integer> portList;
    private static final LinkedList<Integer> openPorts = new LinkedList<Integer>();
    private static final LinkedList<Integer> closedPorts = new LinkedList<Integer>();
    private static final LinkedList<Integer> filteredPorts = new LinkedList<Integer>();
    private static final ReentrantLock mutex = new ReentrantLock();
    private static boolean stop;

    public static void scanPorts(InetAddress address, LinkedList<Integer> list) {
        host = address;
        portList = list;
        openPorts.clear();
        closedPorts.clear();
        filteredPorts.clear();
        stop = false;

        // decide on thread number
        int threadNum = (int) Math.sqrt(list.size());
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
            } catch (SocketTimeoutException e) {
                // timeout
                mutex.lock();
                filteredPorts.add(port);
                mutex.unlock();
            } catch (IOException e) {
                // connection failed
                mutex.lock();
                closedPorts.add(port);
                mutex.unlock();
            }
        }
    }

    public static LinkedList<Integer> getOpenPorts() {
        return openPorts;
    }

    public static LinkedList<Integer> getClosedPorts() {
        return closedPorts;
    }

    public static LinkedList<Integer> getFilteredPorts() {
        return filteredPorts;
    }
}
