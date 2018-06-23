package com.example.onur.IMU_Sensors_v2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ergin on 30.10.17.
 */

public class MqttPublisher {

    boolean logsActive = false;

    String MqttBrokerAddress;

    Context context;
    MqttAndroidClient mqttClient = null;
    MqttConnectOptions mqttOptions = null;

    boolean isReconnecting = false;
    MqttReconnector selfReconnector;
    ReentrantLock connectLock;

    public MqttPublisher(Context ctx, String mqttBrokerAddress) {
        connectLock = new ReentrantLock();

        MqttBrokerAddress = mqttBrokerAddress;
        context = ctx;

        MQTTconnect();


    }

    public MqttPublisher(Context ctx, String mqttBrokerAddress, String username, String password) {

        connectLock = new ReentrantLock();

        mqttOptions = new MqttConnectOptions();
        mqttOptions.setUserName(username);
        mqttOptions.setPassword(password.toCharArray());

        MqttBrokerAddress = mqttBrokerAddress;
        context = ctx;

        MQTTconnect();


    }

   private void mqttLog (String msg){
       if (logsActive) {
           final String TAG = "MQTT Client";
           Log.i(TAG, msg);
       }
   }

   private void setMqttClientCallback() {
       //mqttClient.setCallback(null);
       mqttClient.setCallback(new MqttCallback() {

           @Override
           public void connectionLost(Throwable arg0) {
               // connectionLost

               mqttLog("Connection Lost, running reconnector");
               runReconnector();
               mqttClient.setCallback(null);
           }

           @Override
           public void deliveryComplete(IMqttDeliveryToken arg0) {
               // Delivery Complete, do nothing
           }

           @Override
           public void messageArrived(String arg0, MqttMessage arg1)
                   throws Exception {
               // message arrived, do nothing
           }
       });
    }
   private void runReconnector () {
        // Run only one reconnector at a time

       connectLock.lock();
       try {
           if (isReconnecting)
               return;

           selfReconnector = new MqttReconnector(mqttClient, mqttOptions);

           //selfReconnector = MqttReconnector.getInstance(mqttClient,mqttOptions);

           Thread recon = new Thread(selfReconnector);
           recon.start();
       } finally {
           connectLock.unlock();
       }

   }


    private MqttAndroidClient MQTTconnect () {

        String clientId;
        MqttAndroidClient client;
        IMqttToken token;

        if (mqttClient == null) {   // define new client
            clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(context, MqttBrokerAddress, clientId);
        } else // use existing client
            client = mqttClient;

        try {
            if (mqttOptions == null)
                token = client.connect();
            else
                token = client.connect(mqttOptions);

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    mqttLog("onSuccess ");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    mqttLog("onFailure " + exception.toString());
                    runReconnector();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        mqttClient = client;
        setMqttClientCallback();
        return(client);
    }

    private void publishMessage (SensorEvent event){

        if (!mqttClient.isConnected()) {
            mqttLog("mqttClient is not connected.");
            return;
        }


        String topic = "IMU/ACCELEROMETER";
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                topic = "IMU/ACCELEROMETER";
                break;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                topic = "IMU/ROTATION";
                break;
            default:
                break;

        }

        final String payload = prepareSensorJson(event);

        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            final IMqttToken token = mqttClient.publish(topic, message);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    // Log.i("IMqttToken", "publish successful");
                    mqttLog("token:" + token.toString() + " " + payload);
                    token.setActionCallback(null);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    mqttLog("publish onFailure: " + asyncActionToken.toString() + " exception: " + exception.getMessage());
                    token.setActionCallback(null);
                }
            });
            //sensorCallback.sensorHandlerEvent(token.getMessageId());
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishMessage (String _payload, String _topic){

        if (!mqttClient.isConnected()) {
            mqttLog("mqttClient is not connected.");
            return;
        }


        String topic = _topic;
        final String payload = _payload;

        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            final IMqttToken token = mqttClient.publish(topic, message);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    // Log.i("IMqttToken", "publish successful");
                    mqttLog("token:" + token.toString() + " " + payload);
                    token.setActionCallback(null);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    mqttLog("publish onFailure: " + asyncActionToken.toString() + " exception: " + exception.getMessage());

                }
            });
            //sensorCallback.sensorHandlerEvent(token.getMessageId());
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }


    class SensorJson {
        private int sensorType = -1;
        private String mqttChannel = "mqttChannel";

        private int valueLength = 0;
        private float values[] = {0,0,0,0,0,0};
        SensorJson () {
        }
    }

    private String prepareSensorJson (SensorEvent event) {

        SensorJson obj = new SensorJson();


        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                obj.sensorType = event.sensor.getType();
                obj.mqttChannel = "ACCELEROMETER";
                obj.valueLength = event.values.length;
                obj.values = event.values;
                break;

            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                obj.sensorType = event.sensor.getType();
                obj.mqttChannel = "ROTATION";
                obj.valueLength = event.values.length;
                obj.values = event.values;
                break;

            default:
                break;
        }

        Gson gson = new Gson();
        String json = gson.toJson(obj);

        mqttLog(json);

        return(json);
    }

    private class MqttReconnector implements Runnable {

        MqttAndroidClient client;
        MqttConnectOptions options;
        int i = 1;
        final short retryPeriod = 3000; // milliseconds

        public MqttReconnector (MqttAndroidClient t_client, MqttConnectOptions t_options) {
            isReconnecting = true;
            client = t_client;
            options = t_options;
        }

        @Override
        public void run() {

            while (!client.isConnected()) {
                mqttLog("Trying to reconnect #" + i++ + " every " + retryPeriod/1000f + " seconds");
                mqttClient.setCallback(null);
                try {
                    if (options != null) {
                        client.connect(options);
                    } else {
                        client.connect();
                    }
                } catch (MqttException e) {
                    mqttLog(e.getMessage());
                }

                try {
                    Thread.sleep(retryPeriod);
                } catch (Exception e) {
                    mqttLog(e.getMessage());
                }

            }
            isReconnecting = false;
            setMqttClientCallback();
        }
    }
}
