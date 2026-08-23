package com.example.oppo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class MainActivity extends Activity {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, heartRate;
    private FileWriter writer;
    private File csvFile;

    // UI Elements
    private TextView accelText, gyroText, hrText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Keep screen on & set the layout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // 2. Link the Java variables to the XML text boxes
        accelText = findViewById(R.id.accelText);
        gyroText = findViewById(R.id.gyroText);
        hrText = findViewById(R.id.hrText);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            heartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long timestamp = System.currentTimeMillis();
            String sensorType = "";
            float x = 0, y = 0, z = 0;

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                sensorType = "ACCEL";
                x = event.values[0]; y = event.values[1]; z = event.values[2];
                // Update UI (formatting to 2 decimal places so it doesn't flicker wildly)
                accelText.setText(String.format("Accel: X:%.2f  Y:%.2f  Z:%.2f", x, y, z));

            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                sensorType = "GYRO";
                x = event.values[0]; y = event.values[1]; z = event.values[2];
                gyroText.setText(String.format("Gyro: X:%.2f  Y:%.2f  Z:%.2f", x, y, z));

            } else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                sensorType = "HR";
                x = event.values[0];
                hrText.setText(String.format("Heart Rate: %.0f BPM", x));
            }

            if (!sensorType.isEmpty() && writer != null) {
                String row = timestamp + "," + sensorType + "," + x + "," + y + "," + z + "\n";
                try {
                    writer.append(row);
                    writer.flush();
                } catch (IOException e) {
                    Log.e("SensorData", "Error writing row", e);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onResume() {
        super.onResume();
        try {
            File directory = getExternalFilesDir(null);
            csvFile = new File(directory, "activity_data.csv");
            writer = new FileWriter(csvFile, false); // Overwrite mode
            if (csvFile.length() == 0) {
                writer.append("Timestamp,Sensor,X,Y,Z\n");
            }
        } catch (IOException e) {
            Log.e("SensorData", "File creation failed", e);
        }

        if (accelerometer != null) sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (gyroscope != null) sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        if (heartRate != null) sensorManager.registerListener(sensorListener, heartRate, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
            Log.e("SensorData", "Error closing file", e);
        }
    }

}