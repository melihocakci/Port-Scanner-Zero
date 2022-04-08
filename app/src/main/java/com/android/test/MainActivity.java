package com.android.test;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    EditText ip_field;
    EditText port_field;
    TextView output_field;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ip_field = findViewById(R.id.ip_field);
        port_field = findViewById(R.id.port_field);
        output_field = findViewById(R.id.output_field);
    }

    public void startScan(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        String ip = ip_field.getText().toString();
        int port = Integer.parseInt(port_field.getText().toString());
        Runnable myRunnable = new MyRunnable(ip, port);
        Thread thread = new Thread(myRunnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(((MyRunnable) myRunnable).getResult()) {
            String result = "Port " + port + " is open";
            output_field.setText(result);
        } else {
            String result = "Port " + port + " is closed";
            output_field.setText(result);
        }

        /*
        try{
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 5000);
            socket.close();
            String result = "Port " + port + " is open";
            output_field.setText(result);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            String result = "Port " + port + " is closed";
            output_field.setText(result);
        }*/
    }
}