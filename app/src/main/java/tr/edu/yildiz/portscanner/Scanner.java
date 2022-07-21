package tr.edu.yildiz.portscanner;

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
    private static int timeout;
    private static final LinkedList<Integer> openPorts = new LinkedList<>();
    private static final LinkedList<Integer> closedPorts = new LinkedList<>();
    private static final LinkedList<Integer> filteredPorts = new LinkedList<>();
    private static final ReentrantLock mutex = new ReentrantLock();
    private static boolean stopScan;

    public static void scanPorts(InetAddress address, LinkedList<Integer> list, int maxThreadNum, int num) {
        host = address;
        portList = list;
        timeout = num;

        openPorts.clear();
        closedPorts.clear();
        filteredPorts.clear();
        stopScan = false;

        // decide on thread number
        int threadNum = (int) Math.sqrt(list.size());
        if(threadNum > maxThreadNum) {
            threadNum = maxThreadNum;
        }

        LinkedList<Thread> threads = new LinkedList<>();
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
        stopScan = true;
        mutex.unlock();
    }

    @Override
    public void run() {
        while(true) {
            // lock mutex
            mutex.lock();
            // return if port list is empty or scan has to stop
            if(stopScan || portList.isEmpty()) {
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
                socket.connect(new InetSocketAddress(host, port), timeout);
                // connection successful, end connection
                socket.close();
                mutex.lock();
                openPorts.add(port);
                mutex.unlock();
            } catch (SocketTimeoutException e) {
                // connection timeout
                mutex.lock();
                filteredPorts.add(port);
                mutex.unlock();
            } catch (IOException e) {
                // connection rejected
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

    public static int getPortCount() {
        mutex.lock();
        int count = portList.size();
        mutex.unlock();
        return count;
    }
}
