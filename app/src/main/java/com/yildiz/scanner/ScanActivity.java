package com.yildiz.scanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import java.net.InetAddress;
import java.util.LinkedList;

public class ScanActivity extends AppCompatActivity {
    private Menu menu;
    private Toolbar toolbar;
    private EditText host_field;
    private EditText port_field;
    private TextView output_field;
    private Button button;
    private double start;
    private boolean scanning;
    private final Handler handler = new Handler();

    // Used to load the 'native_test' library on application startup.
    static {
        System.loadLibrary("Port-Scanner-Zero");
    }

    /**
     * A native method that is implemented by the 'native_test' native library,
     * which is packaged with this application.
     */
    public native String getServByPort(int port);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);

        host_field = findViewById(R.id.host_field);
        port_field = findViewById(R.id.port_field);
        output_field = findViewById(R.id.output_field);
        output_field.setMovementMethod(new ScrollingMovementMethod());
        button = findViewById(R.id.button);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_main_setting) {
            Intent intent = new Intent(ScanActivity.this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void toggleScan(View view) {
        if(scanning) {
            scanning = false;
            Scanner.stopScan();
            button.setText(R.string.button_start);
            output_field.setText("Scan stopped");
        } else {
            if(host_field.getText().length() == 0 || port_field.getText().length() == 0) {
                output_field.setText("Please fill the fields");
                return;
            }
            // started scan
            scanning = true;
            // start timer
            start = System.currentTimeMillis();
            // close keyboard
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            // start scan handler
            Runnable scanHandler = new ScanHandler();
            Thread thread = new Thread(scanHandler);
            thread.start();

            button.setText(R.string.button_stop);
            output_field.setText("Scanning");
        }
    }

    private class ScanHandler implements Runnable {

        @Override
        public void run() {
            // get host address from text field
            String host_input = host_field.getText().toString();
            // get port list from text field
            String port_input = port_field.getText().toString();

            // get host address
            InetAddress host;
            try {
                host = InetAddress.getByName(host_input);
            } catch (Exception e) {
                error("Cannot get host address");
                return;
            }

            // get port list
            LinkedList<Integer> portList = new LinkedList<Integer>();
            try {
                String[] ports = port_input.split(",");
                for(String str : ports) {
                    if(str.contains("-")) {
                        String[] gap = str.split("-");
                        int first = Integer.parseInt(gap[0]);
                        int last = Integer.parseInt(gap[1]);

                        if(gap.length != 2 || first > last || first < 1 || last > 65535) {
                            throw new Exception();
                        }

                        for(int j = first; j <= last; j++) {
                            portList.add(j);
                        }
                    } else {
                        int num = Integer.parseInt(str);

                        if(num < 1 || num > 65535) {
                            throw new Exception();
                        }

                        portList.add(num);
                    }
                }
            } catch (Exception e) {
                error("Invalid port input");
                return;
            }

            Scanner.scanPorts(host, portList);
            LinkedList<Integer> openPorts = Scanner.getOpenPorts();
            LinkedList<Integer> closedPorts = Scanner.getClosedPorts();
            LinkedList<Integer> filteredPorts = Scanner.getFilteredPorts();

            if(scanning) {
                // end timer
                double end = System.currentTimeMillis();
                StringBuilder output = new StringBuilder();
                output.append("Scan completed in ").append((end - start) / 1000).append("s\n");

                if(closedPorts.size() > 10)  {
                    output.append(closedPorts.size()).append(" closed ports\n");
                } else {
                    for(int port: closedPorts) {
                        output.append("Port ").append(port).append(" is closed\n");
                    }
                }

                if(filteredPorts.size() > 10)  {
                    output.append(filteredPorts.size()).append(" filtered ports\n");
                } else {
                    for(int port: filteredPorts) {
                        output.append("Port ").append(port).append(" is filtered\n");
                    }
                }

                for(int port: openPorts) {
                    output.append("Port ").append(port).append(": ").append(getServByPort(port)).append("\n");
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        scanning = false;
                        button.setText(R.string.button_start);
                        output_field.setText(output.toString());
                    }
                });
            }
        }

        private void error(String message) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    button.setText(R.string.button_start);
                    output_field.setText(message);
                }
            });
        }
    }
}