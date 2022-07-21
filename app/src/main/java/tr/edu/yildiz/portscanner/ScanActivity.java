package tr.edu.yildiz.portscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.SwitchPreference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

public class ScanActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Spinner scanTypeSpinner;
    private ProgressBar scanProgressBar;
    private EditText host_field;
    private EditText port_field;
    private TextView output_field;
    private TextView scannedText;
    private Button button;
    private double start;
    private boolean scanning;
    private final Handler handler = new Handler();

    private int timeout;
    private int maxThreadNum;

    // Used to load the 'portscanner' library on application startup.
    static {
        System.loadLibrary("portscanner");
    }

    /**
     * A native method that is implemented by the 'portscanner' native library,
     * which is packaged with this application.
     */
    public native String getServByPort(int port);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);

        scanTypeSpinner = findViewById(R.id.scan_type_spinner);
        scanProgressBar = findViewById(R.id.scan_progressBar);
        host_field = findViewById(R.id.host_field);
        port_field = findViewById(R.id.port_field);
        output_field = findViewById(R.id.output_field);
        output_field.setMovementMethod(new ScrollingMovementMethod());
        button = findViewById(R.id.button);
        scannedText = findViewById(R.id.scanned_text);

        button.setOnClickListener(this::toggleScan);

        ArrayAdapter<CharSequence> scanTypeAdapter = ArrayAdapter.createFromResource(this, R.array.scan_types, android.R.layout.simple_spinner_item);
        scanTypeAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        scanTypeSpinner.setAdapter(scanTypeAdapter);
        scanTypeSpinner.setOnItemSelectedListener(this);

        //To set isEULAAgreed preference false
        //setEULAAgreed(false, getApplicationContext());
        if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("isEULAAgreed", false))
            showEulaAlert();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_setting:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String[] texts = getResources().getStringArray(R.array.ports_by_scan_type);
        port_field.setText(texts[i]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public void toggleScan(View view) {
        if(scanning) {
            scanning = false;
            handler.removeCallbacks(progressBarUpdater);
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
            // get preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String pref1 = prefs.getString("max_thread_num", "128");
            maxThreadNum = Integer.parseInt(pref1);

            String pref2 = prefs.getString("timeout", "1000");
            timeout = Integer.parseInt(pref2);
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

    void updateScanProgress() {
        scanProgressBar.setProgress(scanProgressBar.getMax() - Scanner.getPortCount());
        String text = scanProgressBar.getProgress() + " / " + scanProgressBar.getMax();
        scannedText.setText(text);
    }

    //to update progress bar
    Runnable progressBarUpdater = new Runnable() {
        @Override
        public void run() {
            try {
                updateScanProgress();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(Scanner.getPortCount() == 0) {
                    handler.removeCallbacks(this);
                } else {
                    handler.postDelayed(this, 200);
                }
            }
        }
    };

    private void showEulaAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View eulaView = this.getLayoutInflater().inflate(R.layout.eula_alert_layout, null);
        builder.setView(eulaView);
        CheckBox agreeCheckBox = eulaView.findViewById(R.id.eula_agree_check_box);
        builder.setPositiveButton("AGREE", (dialogInterface, i) -> {
            setEULAAgreed(true, getApplicationContext());
            dialogInterface.dismiss();
        });
        builder.setNegativeButton("EXIT", (dialogInterface, i) -> {
            dialogInterface.cancel();
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        AlertDialog eulaAlert = builder.show();
        Button buttonPositive = eulaAlert.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonPositive.setEnabled(false);
        agreeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                buttonPositive.setEnabled(b);
            }
        });

    }

    private void setEULAAgreed(boolean state, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean("isEULAAgreed", state);
        prefEditor.apply();
    }

    // thread to handle scans
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
                HashMap<Integer, Boolean> hashMap = new HashMap<>();
                String[] ports = port_input.split(",");
                for(String str : ports) {
                    if(str.contains("-")) {
                        String[] gap = str.split("-");
                        int first = Integer.parseInt(gap[0]);
                        int last = Integer.parseInt(gap[1]);

                        // input control
                        if(gap.length != 2 || first > last || first < 1 || last > 65535) {
                            throw new Exception();
                        }

                        for(int j = first; j <= last; j++) {
                            if(hashMap.get(j) != null) {
                                throw new Exception();
                            }

                            portList.add(j);
                            hashMap.put(j, true);
                        }
                    } else {
                        int num = Integer.parseInt(str);

                        // input control
                        if(num < 1 || num > 65535 || hashMap.get(num) != null) {
                            throw new Exception();
                        }

                        portList.add(num);
                        hashMap.put(num, true);
                    }
                }
            } catch (Exception e) {
                error("Invalid port input");
                return;
            }

            // start updating progress bar
            scanProgressBar.setMax(portList.size());
            handler.post(progressBarUpdater);

            // scan ports
            Scanner.scanPorts(host, portList, maxThreadNum, timeout);

            // get results from Scanner class
            LinkedList<Integer> openPorts = Scanner.getOpenPorts();
            LinkedList<Integer> closedPorts = Scanner.getClosedPorts();
            LinkedList<Integer> filteredPorts = Scanner.getFilteredPorts();

            // print results
            if(scanning) {
                // end timer
                double end = System.currentTimeMillis();

                // build output string
                StringBuilder output = new StringBuilder();
                output.append("Scan completed in ").append((end - start) / 1000).append("s\n");

                // ports to be listed
                LinkedList<Port> outputPorts = new LinkedList<>();

                // closed ports
                if(closedPorts.size() > 10)  {
                    output.append("Not listed: ").append(closedPorts.size()).append(" closed ports\n");
                } else {
                    for(int portnum: closedPorts) {
                        Port port = new Port();
                        port.number = portnum;
                        port.state = "closed";
                        port.service = getServByPort(portnum);

                        outputPorts.add(port);
                    }
                }

                // filtered ports
                if(filteredPorts.size() > 10)  {
                    output.append("Not listed: ").append(filteredPorts.size()).append(" filtered ports\n");
                } else {
                    for(int portnum: filteredPorts) {
                        Port port = new Port();
                        port.number = portnum;
                        port.state = "filtered";
                        port.service = getServByPort(portnum);

                        outputPorts.add(port);
                    }
                }

                // open ports
                for(int portnum: openPorts) {
                    Port port = new Port();
                    port.number = portnum;
                    port.state = "open";
                    port.service = getServByPort(portnum);

                    outputPorts.add(port);
                }

                // add ports to output
                if(!outputPorts.isEmpty()) {
                    output.append("##################\n").append("PORT | STATUS | SERVICE\n");

                    Collections.sort(outputPorts, new comparePorts());

                    for(Port port: outputPorts) {
                        output.append(port.number).append("/tcp | ")
                                .append(port.state).append(" | ")
                                .append(port.service).append("\n");
                    }
                }

                // print resutls
                handler.post(() -> {
                    scanning = false;
                    button.setText(R.string.button_start);
                    output_field.setText(output.toString());
                });
            }
        }

        // print error and stop scan
        private void error(String message) {
            handler.post(() -> {
                scanning = false;
                button.setText(R.string.button_start);
                output_field.setText(message);
            });
        }

        private class comparePorts implements Comparator<Port> {
            @Override
            public int compare(Port port1, Port port2) {
                return port1.number - port2.number;
            }
        }
    }
}