package com.example.onur.IMU_Sensors_v2;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    private final Handler main_handler = new Handler();

    //TODO: add MQTT reconnect thread
    boolean logsActive = true;
    boolean isActivityOnForeground = true;

    //private String mqttBrokerAddress = "tcp://broker.hivemq.com:1883";
    //private String mqttBrokerIP = "10.1.10.238";
    private String mqttBrokerIP = "10.1.5.53";
    //private String mqttBrokerIP = "193.167.1.8";
    private String mqttBrokerAddress = "tcp://" + mqttBrokerIP + ":1883";

    Switch sensorSwitch;
    TextView tv_sensor_event_counter;
    EditText tv_sensor_readings1,tv_sensor_readings2,tv_sensor_readings3;
    Spinner speedSpinner;
    Button button_broker;
    CheckBox mqttCheckBox;

    SensorManager sm;

    int currentSensorSpeed = SensorHandler.SENSOR_DELAY_1HZ;

    MqttPublisher mqttPublisher = null;

    SensorHandler rbs;

    ReentrantLock connectLock;
    //MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setViews(); // Call findViewById methods

        connectLock = new ReentrantLock();

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        rbs = new SensorHandler(sm, currentSensorSpeed);

        //mqttPublisher = new MqttPublisher(this.getApplicationContext(), mqttBrokerAddress);
        mqttPublisher = new MqttPublisher(this.getApplicationContext(), mqttBrokerAddress, "androidsensor", "ahoy");
        //client = mqttPublisher.mqttClient;



        setCallbackListeners();

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            hLog(e.getMessage());
        }

        sensorSwitch.setChecked(true);
        speedSpinner.setSelection(1, true);
        isActivityOnForeground = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        hLog("Activity paused");
        isActivityOnForeground = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        hLog("Activity resumed");
        isActivityOnForeground = true;
    }

    private void hLog (String msg){
        if ( logsActive) {
            final String TAG = "HackathonApp";
            Log.i(TAG, msg);
        }
    }

    private MqttAndroidClient setCallbackListeners() {
        sensorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isON) {
                hLog("SensorSwitch " + "isOn=" + isON);
                if (isON) {

                    Thread tRBS = new Thread(rbs);
                    tRBS.start();
                } else {
                    if (rbs.isRunning())
                        rbs.terminate();

                }
            }
        });

        rbs.setSensorCallback(new SensorCallback() {
                                  @Override
                                  public void onSensorChanged(String _json, String _topic) {
                                      //hLog("MY_SENSOR_HANDLER " + "Callback with " + _topic + " " + _json);
                                      if (mqttPublisher.mqttClient.isConnected() && !mqttPublisher.isReconnecting) {
                                          mqttPublisher.publishMessage(_json, _topic);
                                      }
                                      else{
                                          if (isActivityOnForeground)
                                           ;// hLog("Mqtt is not connected, screen is updated, but nothing is published");
                                      }

                                      showRawSensorValues(_json,_topic);
                                  }
                              }
        );

        speedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                String selectedItem = parent.getItemAtPosition(position).toString(); //this is your selected item
                hLog("SPINNER " + selectedItem + " position " + position);

                int nextSpeed = SensorHandler.SENSOR_DELAY_1HZ;
                switch (position){
                    case 0:
                        nextSpeed = SensorHandler.SENSOR_DELAY_1HZ;
                        break;
                    case 1:
                        nextSpeed = SensorHandler.SENSOR_DELAY_10HZ;
                        break;
                    case 2:
                        nextSpeed = SensorHandler.SENSOR_DELAY_100HZ;
                        break;
                    default:
                        break;
                }
                hLog("SPINNER " + "Selected: " + nextSpeed);

                if (currentSensorSpeed != nextSpeed) {
                    currentSensorSpeed = nextSpeed;
                    rbs.setSensorDelay(currentSensorSpeed);
                }
            }
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        button_broker.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getMqttBrokerIP();
            }
        });

        return null;
    }

    private void setViews() {
        sensorSwitch    = (Switch) findViewById(R.id.sensor_switch);
        tv_sensor_event_counter = (TextView) findViewById(R.id.tv_sensor_event_counter);
        tv_sensor_readings1 = (EditText) findViewById(R.id.tv_sensor_readings1);
        tv_sensor_readings2 = (EditText) findViewById(R.id.tv_sensor_readings2);
        tv_sensor_readings3 = (EditText) findViewById(R.id.tv_sensor_readings3);
        button_broker = (Button) findViewById(R.id.button_broker);
        button_broker.setText(mqttBrokerIP);
        mqttCheckBox = (CheckBox) findViewById(R.id.mqttCheckBox);

        speedSpinner = (Spinner) findViewById(R.id.speedSpinner);
        java.util.ArrayList<String> strings = new java.util.ArrayList<>();
        strings.add("1 Hz");
        strings.add("10 Hz");
        //strings.add("100 Hz");
        ArrayAdapter spinnerAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, strings); //(AddMember.this, R.layout.support_simple_spinner_dropdown_item, strings);
        speedSpinner.setAdapter(spinnerAdapter);

    }

    private String m_Text;
    private void getMqttBrokerIP() {

        final Context context =  this.getApplicationContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789."));

        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();

                if (validIP(m_Text)) {
                    mqttBrokerIP = m_Text;
                    mqttBrokerAddress = "tcp://" + mqttBrokerIP + ":1883";
                    mqttPublisher = new MqttPublisher(context, mqttBrokerAddress, "androidsensor", "ahoy");

                    button_broker.setText(mqttBrokerIP);
                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private void showRawSensorValues (final String _json, final String _topic) {
        if (!isActivityOnForeground)
            return;

        final String UItext = _json + "\n";
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    switch (_topic) {
                        case "Trailer/ACCELEROMETER":
                            tv_sensor_readings1.setText(UItext);
                            break;
                        case "Trailer/GEOMAGNETIC_ROTATION_VECTOR":
                            tv_sensor_readings2.setText(UItext);
                            break;
                        case "numsensors":
                            tv_sensor_readings3.setText(UItext + " sensors registered.");
                            break;
                    }
                } catch (Exception e) {
                    hLog("EXCEPTION " + e.toString());
                }

                if (mqttPublisher.mqttClient.isConnected()) {
                    if (!mqttCheckBox.isChecked()){
                        mqttCheckBox.setChecked(true);
                        mqttCheckBox.setBackgroundColor(Color.GREEN);
                    }
                } else {
                        mqttCheckBox.setChecked(false);
                        mqttCheckBox.setBackgroundColor(Color.RED);

                }

            }
        });
    }
}