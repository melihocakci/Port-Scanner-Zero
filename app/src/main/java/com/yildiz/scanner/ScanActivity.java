package com.yildiz.scanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.LinkedList;

public class ScanActivity extends AppCompatActivity {
    private EditText ip_field;
    private EditText port_field;
    private TextView output_field;
    private double start;
    private int portNum;
    private LinkedList<Integer> openPorts;
    private boolean scanning;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);

        ip_field = findViewById(R.id.host_field);
        port_field = findViewById(R.id.port_field);
        output_field = findViewById(R.id.output_field);
    }

    public void toggleScan(View view) {
        if(scanning) {
            scanning = false;
            Scanner.stopScan();
            print("Scan stopped");
            return;
        }

        // started scan
        scanning = true;
        // start timer
        start = System.currentTimeMillis();
        // close keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // get host address from text field
                String hostname = ip_field.getText().toString();
                // get port list from text field
                String port_input = port_field.getText().toString();

                // get host address
                InetAddress host;
                try {
                    host = InetAddress.getByName(hostname);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                // get port list
                LinkedList<Integer> portList = new LinkedList<Integer>();
                try {
                    String[] ports = port_input.split(",");
                    for(String i : ports) {
                        if(i.contains("-")) {
                            String[] gap = i.split("-");
                            int first = Integer.parseInt(gap[0]);
                            int last = Integer.parseInt(gap[1]);
                            for(int j = first; j <= last; j++) {
                                portList.add(j);
                            }
                        } else {
                            portList.add(Integer.parseInt(i));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                portNum = portList.size();
                openPorts = Scanner.scanPorts(host, portList);

                if(scanning) {
                    // end timer
                    double end = System.currentTimeMillis();
                    StringBuilder output = new StringBuilder();
                    output.append("Scan completed.\n");
                    output.append("Time elapsed: ").append((end - start) / 1000).append("s\n");
                    output.append(portNum - openPorts.size()).append(" closed ports\n");
                    for(int i = 0; i < openPorts.size(); i++) {
                        output.append("Port ").append(openPorts.get(i)).append(" is open\n");
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            print(output.toString());
                        }
                    });
                    scanning = false;
                }
            }
        };

        Thread scanHandler = new Thread(runnable);
        scanHandler.start();

        output_field.setText("Scanning..");
    }

    public void print(String text) {
        output_field.setText(text);
    }
}