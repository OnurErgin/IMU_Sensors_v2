package com.example.onur.IMU_Sensors_v2;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.currentTimeMillis;

/**
 * Created by ergin on 22.09.17.
 */

interface SensorCallback {
    void onSensorChanged(String sensorJson, String sensorTopic);
}

public class SensorHandler implements Runnable {

    boolean logsActive = false;
    /*
     * SENSOR_DELAY_GAME = 20.000 microsecond
     * SENSOR_DELAY_NORMAL = 180.000 microsecond
     * SENSOR_DELAY_UI = 60.000 microsecond mSensorManager.SENSOR_DELAY_UI
     * SENSOR_DELAY_FASTEST = As fast as possible
     */
    public static final int SENSOR_DELAY_100HZ = 10000;
    public static final int SENSOR_DELAY_10HZ = 100000;
    public static final int SENSOR_DELAY_1HZ = 1000000;

    private static int sensor_delay;    // SensorManager.SENSOR_DELAY_FASTEST; // 10000 microseconds = 100 Hz
                                        // SENSOR_DELAY_FASTEST (instead of 100Hz) causes too many data to be produced

    public List<Sensor> sensorList;
    public static List<Sensor> hackathonSensors; // List of sensors to be used in Hackathon
    SensorEvent accelerometerEvent = null,
                geomagneticRotationVectorEvent = null,
                lightEvent = null,
                rotationVectorEvent = null,
                gyroscopeEvent = null,
                gravityEvent = null,
                pressureEvent = null,
                ambientTemparatureEvent = null;

    String accelerometerJson = null,
            geomagneticRotationVectorJson = null,
            lightJson = null,
            rotationVectorJson = null,
            gyroscopeJson = null,
            gravityJson = null,
            pressureJson = null,
            ambientTemparatureJson = null;;


    private volatile boolean running;
    private volatile int threadKillCheckFrequency = 500; // milliseconds
    public SensorManager mSensorManager;
    volatile long sensor_event_counter = 0;

    SensorCallback sensorCallback;
    Timer timer;
    String mqttForeCh = "Trailer";

    class SensorJson {
        private String sensorType = "mqttChannel";
        private int valueLength = 0;
        private float values[] = {0,0,0,0,0,0};
        private long timestamp = 0;
        SensorJson () {
        }
    }


    /**
     *
     * @param _mSensorManager Sensor manager from android.hardware.SensorManager
     * @param _measuringFrequency microseconds. Frequency at which the sensor values are supposed to be published
     */
    public SensorHandler(SensorManager _mSensorManager, int _measuringFrequency) {
        sensorLog("started");

        mSensorManager = _mSensorManager;
        sensor_delay = _measuringFrequency;
        sensorList = getDefaultSensorList();

        fillHackathonSensorList();
        sensorLog(sensorList.toString());


    }

    /**
     *
     * @param _mSensorManager Sensor manager from android.hardware.SensorManager
     */
        public SensorHandler(SensorManager _mSensorManager) {
            this(_mSensorManager, SENSOR_DELAY_1HZ);
        }

        private void sensorLog (String msg){
            if (logsActive) {
                final String TAG = "SensorHandler";
                Log.i(TAG, msg);
            }
        }

        public List<Sensor> getDefaultSensorList() {

            List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            List<Sensor> defaultSensorList = new ArrayList<>();
            List<Sensor> nullSensorList = new ArrayList<>();

            for (int i = 0; i < sensorList.size(); i++) {

                Sensor default_sensor = mSensorManager.getDefaultSensor(sensorList.get(i).getType());

                if (!defaultSensorList.contains(default_sensor)) {
                    if (default_sensor != null) {
                        defaultSensorList.add(default_sensor);
                    } else {
                        boolean newSensorType = true;
                        for (Sensor _s : nullSensorList) {
                            if (_s.getType() == sensorList.get(i).getType())
                                newSensorType = false;
                        }
                        nullSensorList.add(sensorList.get(i));
                        if (newSensorType) {
                            defaultSensorList.add(sensorList.get(i));
                        }
                    }
                }

            }
            sensorList = defaultSensorList;
            return sensorList;
        }

        public void startAllSensors() {
            registerSensors(sensorList);
            Log.v("Thread check", "start all sensors");
        }

        public void startHackathonSensors() {
            registerSensors(hackathonSensors);
            sensorLog( hackathonSensors.toString());
        }

        public void stopSensors() {
            timer.cancel();
            unRegisterSensors(mSensorManager);
        }

        public boolean isRunning () {
            return running;
        }

        public void setSensorDelay (int _delay) {
            sensor_delay = _delay;
            if (isRunning()) {
                // restart sensors
                stopSensors();
                startAndReport();
            }
        }
        @Override
        public void run() {

            sensorLog("thread started to run.");

            running = true;

            //startAllSensors();
            startAndReport();

            while (running) {
                try {
                    Thread.sleep(threadKillCheckFrequency);
                } catch (InterruptedException e) {
                    sensorLog(e.toString());
                }

            } // while running
            sensorLog("Stopping");
        }

    private void startAndReport() {
        startHackathonSensors();

        sensorCallback.onSensorChanged("{\"numsensors\":" + hackathonSensors.size() + "}", "numsensors");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //Log.i("TIMER", "Timer started with period: " + sensor_delay/1000);
                if (sensorCallback == null)
                    return;

                if (accelerometerEvent != null)
                    sensorCallback.onSensorChanged(accelerometerJson, mqttForeCh + "/" + getSimpleSensorName(accelerometerEvent.sensor.getType()));
                    //publishMessage(accelerometerEvent);
                if (geomagneticRotationVectorEvent != null)
                    sensorCallback.onSensorChanged(geomagneticRotationVectorJson, mqttForeCh + "/" + getSimpleSensorName(geomagneticRotationVectorEvent.sensor.getType()) );
                    //publishMessage(geomagneticRotationVectorEvent);

                if (lightEvent != null & false)
                    sensorCallback.onSensorChanged(lightJson, mqttForeCh + "/" + getSimpleSensorName(lightEvent.sensor.getType()) );

                if (rotationVectorEvent != null)
                    sensorCallback.onSensorChanged(rotationVectorJson, mqttForeCh + "/" + getSimpleSensorName(rotationVectorEvent.sensor.getType()) );

                if (gyroscopeEvent != null)
                    sensorCallback.onSensorChanged(gyroscopeJson, mqttForeCh + "/" + getSimpleSensorName(gyroscopeEvent.sensor.getType()) );

                if (gravityEvent != null)
                    sensorCallback.onSensorChanged(gravityJson, mqttForeCh + "/" + getSimpleSensorName(gravityEvent.sensor.getType()) );

                if (pressureEvent != null & false)
                    sensorCallback.onSensorChanged(pressureJson, mqttForeCh + "/" + getSimpleSensorName(pressureEvent.sensor.getType()) );

                if (ambientTemparatureEvent != null & false)
                    sensorCallback.onSensorChanged(ambientTemparatureJson, mqttForeCh + "/" + getSimpleSensorName(ambientTemparatureEvent.sensor.getType()) );
            }
        }, sensor_delay/1000, sensor_delay/1000);  // sensor_delay/1000 ms period
    }

    private void registerSensors(List<Sensor> listOfSensors) {
            for (int i = 0; i < listOfSensors.size(); i++) {
                mSensorManager.registerListener(mSensorListener, listOfSensors.get(i), sensor_delay, sensor_delay);
                sensorLog(listOfSensors.get(i).getName() + " with " + sensor_delay + " delay");
            }

        sensorLog("sensors registered.");
        }

        private void unRegisterSensors(SensorManager tSensorManager) {
            mSensorManager.unregisterListener(mSensorListener);
        }

        private SensorEventListener mSensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, final int accuracy) {

            }

            @Override
            public void onSensorChanged(SensorEvent event) {

                sensor_event_counter++;
                if (event.sensor.getType() == Sensor.TYPE_LIGHT && true)
                    sensorLog( "counter = " + sensor_event_counter + ", Light: " + event.values[0]);

                if (sensor_event_counter%10 == 1 && false)
                    sensorLog( "counter = " + sensor_event_counter + ", Type: " + event.sensor.getName() + ", Value[0]: " + event.values[0]);


                String sensorJson = prepareSensorJson(event);
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        accelerometerEvent = event;
                        accelerometerJson = sensorJson;
                        break;
                    case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                        geomagneticRotationVectorEvent = event;
                        geomagneticRotationVectorJson = sensorJson;
                        break;
                    case Sensor.TYPE_LIGHT:
                        lightEvent = event;
                        lightJson = sensorJson;
                        break;
                    case Sensor.TYPE_ROTATION_VECTOR:
                        rotationVectorEvent = event;
                        rotationVectorJson = sensorJson;
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        gyroscopeEvent = event;
                        gyroscopeJson = sensorJson;
                        break;
                    case Sensor.TYPE_GRAVITY:
                        gravityEvent = event;
                        gravityJson = sensorJson;
                        break;
                    case Sensor.TYPE_PRESSURE:
                        pressureEvent = event;
                        pressureJson = sensorJson;
                        break;
                    case Sensor.TYPE_AMBIENT_TEMPERATURE:
                        ambientTemparatureEvent = event;
                        ambientTemparatureJson = sensorJson;
                        break;
                    default:
                        break;
                }
                //publishMessage(event);
            }
        };

        // Terminate the running thread
        public void terminate() {
            stopSensors();
            running = false;
        }

        private void fillHackathonSensorList () {
            hackathonSensors = new ArrayList<>();
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
                hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null)
                hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT));

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null)
                hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR));

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null)
                hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
                hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null)
                hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null)
                hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE));

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null)
                hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE));
            //hackathonSensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MOTION_DETECT)); //null on huawei

            Log.i("Registered Sensors: ", hackathonSensors.size() + " sensors:  " +  hackathonSensors.toString());


            //Log.e("SensorNAMES: " , mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR).getName());
            //Log.e("SensorNAMES: " , mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY).getName());
        }

        private String prepareSensorJson (SensorEvent event) {

            SensorJson obj = new SensorJson();


            /*
            switch(event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    obj.sensorType = getSimpleSensorName(event.sensor.getType());
                    obj.valueLength = event.values.length;
                    obj.values = event.values;
                    break;

                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    obj.sensorType = "ROTATION";
                    obj.valueLength = event.values.length;
                    obj.values = event.values;
                    break;

                case Sensor.TYPE_LIGHT:
                    obj.sensorType = "LIGHT";
                    obj.valueLength = event.values.length;
                    obj.values = event.values;
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    obj.sensorType = "ROTATION_VECTOR";
                    obj.valueLength = event.values.length;
                    obj.values = event.values;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    obj.sensorType = "GYROSCOPE";
                    obj.valueLength = event.values.length;
                    obj.values = event.values;
                    break;
                case Sensor.TYPE_GRAVITY:
                    obj.sensorType = "GRAVITY";
                    obj.valueLength = event.values.length;
                    obj.values = event.values;
                    break;
                case Sensor.TYPE_PRESSURE:
                    obj.sensorType = "PRESSURE";
                    obj.valueLength = event.values.length;
                    obj.values = event.values;
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    obj.sensorType = "AMBIENT_TEMPERATURE";
                    obj.valueLength = event.values.length;
                    obj.values = event.values;
                    break;

                default:
                    break;
            }
            */
            obj.sensorType = getSimpleSensorName(event.sensor.getType());
            obj.valueLength = event.values.length;
            obj.values = event.values;
            obj.timestamp = currentTimeMillis();

            Gson gson = new Gson();
            String json = gson.toJson(obj);

            //sensorLog(json);

            return(json);
        }

        public String getSimpleSensorName (int sensorType) {
            String simpleSensorName = "";
            switch(sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    simpleSensorName = "ACCELEROMETER";
                    break;

                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    simpleSensorName = "GEOMAGNETIC_ROTATION_VECTOR";
                    break;

                case Sensor.TYPE_LIGHT:
                    simpleSensorName = "LIGHT";
                    break;

                case Sensor.TYPE_ROTATION_VECTOR:
                    simpleSensorName = "ROTATION_VECTOR";
                    break;

                case Sensor.TYPE_GYROSCOPE:
                    simpleSensorName = "GYROSCOPE";
                    break;

                case Sensor.TYPE_GRAVITY:
                    simpleSensorName = "GRAVITY";
                    break;

                case Sensor.TYPE_PRESSURE:
                    simpleSensorName = "PRESSURE";
                    break;

                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    simpleSensorName = "AMBIENT_TEMPERATURE";
                    break;

                default:
                    break;
            }
            return simpleSensorName;
        }

        public void setSensorCallback(SensorCallback callback) {
            sensorCallback = callback;
        }
 }
