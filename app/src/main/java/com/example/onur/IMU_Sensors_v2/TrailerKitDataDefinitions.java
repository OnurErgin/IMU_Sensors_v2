package com.example.onur.IMU_Sensors_v2;

import java.util.List;

/**
 * Created by ergin on 19.09.17.
 */

public class TrailerKitDataDefinitions {

    public Message message = new Message();

    TrailerKitDataDefinitions () {

    }

    public class Message {
        Message () {}
        public String messageId;
        public int messageVersion;
        public String deviceId;
        public int deviceType;
        public String timestamp;
        public List<SensorInfo> sensors;
    }

    public class SensorInfo {
        SensorInfo() {}
        public int type;
        public List<SensorMeasurement> measurements;
    }

    public class SensorMeasurement {
        SensorMeasurement() {}
        public String timestamp;
        public String key;
        public double value;
    }


}


/*
{
  "messageId": "3539046d-cf55-473e-aa03-23409634959c",
  "messageVersion": 1,
  "deviceId": "juha-test-virtual",
  "deviceType": 1,
  "timestamp": "2017-07-07T09:26:06",
  "sensors": [{
    "type": 0,
    "measurements": [{
      "timestamp": "2017-07-07T09:26:06",
      "key": "latitude",
      "value": "52.50722"
    }, {
      "timestamp": "2017-07-07T09:26:06",
      "key": "longitude",
      "value": "13.14583"
    }]
  }, {
    "type": 1,
    "measurements": [{
      "timestamp": "2017-07-07T09:26:06",
      "key": "celsius",
      "value": "20.8649986893241"
    }]
  }, {
    "type": 2,
    "measurements": [{
      "timestamp": "2017-07-07T09:26:06",
      "key": "percentage",
      "value": "24.0366605501793"
    }]
  }]
}
*/