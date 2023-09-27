package com.enrih.folkbluetoothcompanion.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.enrih.folkbluetoothcompanion.bluetooth.CreateConnectThread.connectedThread;
import static com.enrih.folkbluetoothcompanion.constants.Statuses.*;

import com.enrih.folkbluetoothcompanion.R;
import com.enrih.folkbluetoothcompanion.bluetooth.CreateConnectThread;
import com.enrih.folkbluetoothcompanion.common.LineChartCustom;
import com.enrih.folkbluetoothcompanion.common.LineGraphSeriesCustom;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.android.material.slider.Slider;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity {

    private List<Float> degrees = List.of(-129.375F, -118.125F, -106.875F, -95.625F, -84.375F, -73.125F, -61.875F, -50.625F, -39.375F, -28.125F, -16.875F, -5.625F
            , 5.625F, 16.875F, 28.125F, 39.375F, 50.625F, 61.875F, 73.125F, 84.375F, 95.625F, 106.875F, 118.125F, 129.375F);

    private String deviceName = null;

    public boolean connected = false;
    public boolean detached = false;
    SharedPreferences settingsSP;
    private String deviceAddress;
    //public static Handler handler;

    public static CreateConnectThread createConnectThread;

    private double lastAngle = 0;
    private double lastSpeed = 0;

    private boolean isReverse = false;
    private boolean isOn = false;
    private boolean isSOn = true;

    private boolean isDataOn = true;
    private boolean isConsoleOn = true;

    private boolean isDefault = true;

    private int viewSwitch = 0;
    private boolean settings = false;

    Button button1, button2, button3, button4, button5, button6, buttonConnect, buttonSettings, buttonSaveSettings, buttonData, buttonConsole, buttonMod;
    TextView textViewSpeedL, textViewSpeedR, textViewStatus, textViewTrackAngle, textViewP, textViewI, textViewD, textViewMax, textViewMin, contentSwitch;
    EditText pSpeed, iSpeed, dSpeed, pTurn, iTurn, dTurn, pSpeedStep, iSpeedStep, dSpeedStep, pTurnStep, iTurnStep, dTurnStep;
    Slider sliderSpeed, sliderSpeedP, sliderSpeedI, sliderSpeedD, sliderTurnP, sliderTurnI, sliderTurnD;
    Toolbar toolbar;
    ProgressBar progressBar;
    LinearLayout layoutControls;
    TableLayout layoutSettings;
    FrameLayout layoutLine, layoutRadar;
    LineChart lineChart;
    LineChartCustom radarChart;

    GraphView graph1, graph2;

    private class BluetoothHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public BluetoothHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == CONNECTING_STATUS.getCode()) {
                    if (msg.arg1 == 1) {
                        Log.e("Bluetooth", "in run connected");
                        activity.toolbar.setSubtitle("Connected to " + deviceName);
                    } else if (msg.arg1 == -1) {
                        toolbar.setSubtitle("Device fails to connect");
                    }
                    progressBar.setVisibility(View.GONE);
                    buttonConnect.setEnabled(true);
                } else if (msg.what == MESSAGE_READ.getCode()) {
                    String arduinoMsg = msg.obj.toString();
                    Log.d("BT MESSAGE", arduinoMsg);
                    if (arduinoMsg.contains("PDC:")) {
                        textViewTrackAngle.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("SpeedL:")) {
                        textViewSpeedL.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("Time:")) {
                        textViewMin.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("Error:")) {
                        textViewMax.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    }
                        /*else if (arduinoMsg.contains("Error:")){
                            textViewStatus.setText(arduinoMsg.substring(arduinoMsg.indexOf(":")));
                        }*/
                    else if (arduinoMsg.contains("P:")) {
                        textViewP.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("I:")) {
                        textViewP.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("D:")) {
                        textViewD.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("SpeedR:")) {
                        //sliderSpeed.setValue(Float.parseFloat(arduinoMsg.substring(arduinoMsg.indexOf(":"))));
                        textViewSpeedR.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("Status:")) {
                        //sliderSpeed.setValue(Float.parseFloat(arduinoMsg.substring(arduinoMsg.indexOf(":"))));
                        textViewStatus.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("data:")) {
                        try {
                            String data = arduinoMsg.substring(arduinoMsg.indexOf(":") + 1);
                            ArrayList<ArrayList<Entry>> linePoints = new ArrayList<>();
                            ArrayList<ArrayList<Entry>> radarPoints = new ArrayList<>();

                            ArrayList<DataPoint[]> valueLinePoints = new ArrayList<>();
                            ArrayList<DataPoint[]> valueRadarPoints = new ArrayList<>();

                            ArrayList<String> datasets = new ArrayList<>(List.of(data.split("/")));
                            int dataMode = 0;
                            for (String dataset : datasets) {
                                if (dataset.isEmpty()) {
                                    break;
                                }
                                ArrayList<Entry> dataPointsLine = new ArrayList<>();
                                ArrayList<Entry> dataPointsRadar = new ArrayList<>();
                                ArrayList<String> points = new ArrayList<>(List.of(dataset.split(";")));

                                DataPoint[] valuesLine = new DataPoint[points.size()];
                                DataPoint[] valuesRadar = new DataPoint[points.size()];

                                int counter = 0;
                                for (String point : points) {
                                    if (dataMode == 2 && viewSwitch == 2) {

                                        float y = Float.parseFloat(point);
                                        List<Float> transformedPoints = radarTransformer(y, degrees.get(counter));

                                        DataPoint r = new DataPoint(transformedPoints.get(0), transformedPoints.get(1));
                                        valuesRadar[counter] = r;
                                    } else if (dataMode != 2 && viewSwitch == 1) {
                                        float x = Float.parseFloat(point.split(",")[0]);
                                        float y = Float.parseFloat(point.split(",")[1]);


                                        DataPoint l = new DataPoint(x, y);
                                        valuesLine[counter] = l;

                                        Entry entryLine = new Entry(x, y);
                                        dataPointsLine.add(entryLine);

                                    }
                                    counter++;
                                }
                                linePoints.add(dataPointsLine);

                                if (dataMode == 2) {
                                    valueRadarPoints.add(valuesRadar);
                                }
                                valueLinePoints.add(valuesLine);
                                dataMode++;
                            }
                            if (viewSwitch == 1) {
                                setUpChart(linePoints);
                            } else {
                                setUpGraph(valueLinePoints, valueRadarPoints);
                            }

                        } catch (Exception e) {

                        }
                    }
                }
            }
        }
    }

    private final BluetoothHandler handler = new BluetoothHandler(this);


    @RequiresApi(api = Build.VERSION_CODES.S)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsSP = this.getSharedPreferences("settings", 0);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[] {Manifest.permission.BLUETOOTH,
                                            Manifest.permission.BLUETOOTH_ADMIN,
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.BLUETOOTH_SCAN,
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.ACCESS_BACKGROUND_LOCATION

                    },
                    PERMISSIONS_REQUEST_CODE.getCode());

        }

        // UI Initialization
        initUI();

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress, handler);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler.handleMessage(handler.obtainMessage());

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        contentSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewSwitch < 2){
                    viewSwitch++;
                }
                else {
                    viewSwitch = 0;
                }
                switch (viewSwitch){
                    case 0:
                        contentSwitch.setText("Controls");
                        switchControlsGraph();
                        break;
                    case 1:
                        contentSwitch.setText("Line");
                        switchControlsGraph();
                        break;
                    case 2:
                        contentSwitch.setText("Radar");
                        switchControlsGraph();
                        break;
                }
            }
        });

        buttonSettings.setOnClickListener(view -> {
            settings = !settings;
            if (settings){
                layoutSettings.setVisibility(View.VISIBLE);
                layoutLine.setVisibility(View.GONE);
                layoutControls.setVisibility(View.GONE);
                //layoutRadar.setVisibility(View.GONE);
            }
            else {
                layoutSettings.setVisibility(View.GONE);
                switchControlsGraph();
            }

        });

        // Button to ON/OFF LED on Arduino Board
        button1.setOnClickListener(view -> {
            if (isOn){
                button1.setText("On");
                connectedThread.write("off");
            }
            else {
                button1.setText("Off");
                connectedThread.write("on");
            }
            isOn = !isOn;
        });

        button2.setOnClickListener(view -> {
            String cmdText = "startup";
            connectedThread.write(cmdText);
            button1.setText("Off");
            isOn = true;
        });

        button3.setOnClickListener(view -> {
            String cmdText = "startupL";
            connectedThread.write(cmdText);
            button1.setText("Off");
            isOn = true;
        });

        button4.setOnClickListener(view -> {
            String cmdText = "startupR";
            connectedThread.write(cmdText);
            button1.setText("Off");
            isOn = true;
        });

        /*
        button3.setOnClickListener(view -> {
            if (isReverse){
                button3.setText("Forward");
                connectedThread.write("reverse");
            }
            else {
                button3.setText("Reverse");
                connectedThread.write("forward");
            }
            isReverse = !isReverse;
        });*/

        buttonData.setOnClickListener(view -> {
            if (isDataOn){
                buttonData.setText("Data On ");
                connectedThread.write("dataOff");
            }
            else {
                buttonData.setText("Data Off");
                connectedThread.write("dataOn");
            }
            isDataOn = !isDataOn;
        });

        buttonConsole.setOnClickListener(view -> {
            if (isConsoleOn){
                buttonConsole.setText("Console On ");
                connectedThread.write("printOff");
            }
            else {
                buttonConsole.setText("Console Off");
                connectedThread.write("printOn");
            }
            isConsoleOn = !isConsoleOn;
        });

        buttonMod.setOnClickListener(view -> {
            if (isDefault){
                buttonMod.setText("Set Slow");
                connectedThread.write("MDef");
            }
            else {
                buttonMod.setText("Set Def");
                connectedThread.write("MSlow");
            }
            isDefault = !isDefault;
        });

        button5.setOnClickListener(view -> {
            if (isSOn){
                button5.setText("Serv. On");
                connectedThread.write("SOff");
            }
            else {
                button5.setText("Serv. Off");
                connectedThread.write("SOn");
            }
            isSOn = !isSOn;
        });

        button6.setOnClickListener(view -> {
            if (createConnectThread != null){
                String cmdText = "Disconnect\r\n";
                connectedThread.write(cmdText);
                createConnectThread.cancel();
                toolbar.setSubtitle("Disconnected!");
                connected = false;
                detached = true;
            }
        });
        buttonSaveSettings.setOnClickListener(view -> {
            SharedPreferences.Editor editor = settingsSP.edit();
            if (pSpeed.getText().length() > 0){
                sliderSpeedP.setValueTo(Integer.parseInt(pSpeed.getText().toString()));
                pSpeed.setText(pSpeed.getText().toString());
                editor.putInt("pSpeed", Integer.parseInt(pSpeed.getText().toString()));
            }
            if (iSpeed.getText().length() > 0){
                sliderSpeedI.setValueTo(Integer.parseInt(iSpeed.getText().toString()));
                iSpeed.setText(iSpeed.getText().toString());
                editor.putInt("iSpeed", Integer.parseInt(iSpeed.getText().toString()));

            }
            if (dSpeed.getText().length() > 0){
                sliderSpeedD.setValueTo(Integer.parseInt(dSpeed.getText().toString()));
                dSpeed.setText(dSpeed.getText().toString());
                editor.putInt("dSpeed", Integer.parseInt(dSpeed.getText().toString()));

            }
            if (pSpeedStep.getText().length() > 0){
                sliderSpeedP.setStepSize(Float.parseFloat(pSpeedStep.getText().toString()));
                pSpeedStep.setText(pSpeedStep.getText().toString());
                editor.putFloat("pSpeedStep", Float.parseFloat(pSpeedStep.getText().toString()));
            }
            if (iSpeedStep.getText().length() > 0){
                sliderSpeedI.setStepSize(Float.parseFloat(iSpeedStep.getText().toString()));
                iSpeedStep.setText(iSpeedStep.getText().toString());
                editor.putFloat("iSpeedStep", Float.parseFloat(iSpeedStep.getText().toString()));

            }
            if (dSpeedStep.getText().length() > 0){
                sliderSpeedD.setStepSize(Float.parseFloat(dSpeedStep.getText().toString()));
                dSpeedStep.setText(dSpeedStep.getText().toString());
                editor.putFloat("dSpeedStep", Float.parseFloat(dSpeedStep.getText().toString()));

            }
            if (pTurn.getText().length() > 0){
                sliderTurnP.setValueTo(Integer.parseInt(pTurn.getText().toString()));
                pTurn.setText(pTurn.getText().toString());
                editor.putInt("pTurn", Integer.parseInt(pTurn.getText().toString()));

            }
            if (iTurn.getText().length() > 0){
                sliderTurnI.setValueTo(Integer.parseInt(iTurn.getText().toString()));
                iTurn.setText(iTurn.getText().toString());
                editor.putInt("iTurn", Integer.parseInt(iTurn.getText().toString()));

            }
            if (dTurn.getText().length() > 0){
                sliderTurnD.setValueTo(Integer.parseInt(dTurn.getText().toString()));
                dTurn.setText(dTurn.getText().toString());
                editor.putInt("dTurn", Integer.parseInt(dTurn.getText().toString()));
            }
            if (pTurnStep.getText().length() > 0){
                sliderTurnP.setStepSize(Float.parseFloat(pTurnStep.getText().toString()));
                pTurnStep.setText(pTurnStep.getText().toString());
                editor.putFloat("pTurnStep", Float.parseFloat(pTurnStep.getText().toString()));

            }
            if (iTurnStep.getText().length() > 0){
                sliderTurnI.setStepSize(Float.parseFloat(iTurnStep.getText().toString()));
                iTurnStep.setText(iTurnStep.getText().toString());
                editor.putFloat("iTurnStep", Float.parseFloat(iTurnStep.getText().toString()));

            }
            if (dTurnStep.getText().length() > 0){
                sliderTurnD.setStepSize(Float.parseFloat(dTurnStep.getText().toString()));
                dTurnStep.setText(dTurnStep.getText().toString());
                editor.putFloat("dTurnStep", Float.parseFloat(dTurnStep.getText().toString()));
            }
            editor.apply();
            dismissKeyboard(this);
            Toast.makeText(getApplicationContext(),"Saved",Toast.LENGTH_SHORT).show();

        });

    }

    private void dismissKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != activity.getCurrentFocus())
            imm.hideSoftInputFromWindow(activity.getCurrentFocus()
                    .getApplicationWindowToken(), 0);
    }

    private List<Float> radarTransformer(float y, Float degree) {
        double offset = 0.05;
        float transDegree = 90 - degree < 0 ?  360 + (90 - degree) : 90 - degree;
        Float transX = (float) ((y + offset) * Math.cos(Math.toRadians(transDegree)));
        Float transY = (float) ((y + offset) * Math.sin(Math.toRadians(transDegree)));

        return List.of(transX, transY);
    }

    private void switchControlsGraph() {
        switch (viewSwitch){
            case 0:
                layoutControls.setVisibility(View.VISIBLE);
                layoutLine.setVisibility(View.GONE);
                layoutRadar.setVisibility(View.GONE);
                break;

            case 1:
                layoutControls.setVisibility(View.GONE);
                layoutLine.setVisibility(View.VISIBLE);
                layoutRadar.setVisibility(View.GONE);
                break;
                //layoutRadar.setVisibility(View.GONE);
            case 2:
                layoutControls.setVisibility(View.GONE);
                layoutLine.setVisibility(View.GONE);
                layoutRadar.setVisibility(View.VISIBLE);
                break;
                //
        }
    }

    private void setUpChart(ArrayList<ArrayList<Entry>> linePoints) {
        List<Integer> colors = List.of(Color.RED, Color.MAGENTA, Color.GREEN, Color.YELLOW);
        LineData lineData = new LineData();
        int counter = 0;
        if (viewSwitch == 1) {
            for (ArrayList<Entry> dataset : linePoints) {
                dataset.sort(new EntryXComparator());
                LineDataSet lineDataSet = new LineDataSet(dataset, "Data " + counter);
                lineDataSet.setColor(colors.get(counter));
                counter++;
                lineData.addDataSet(lineDataSet);
            }
            lineChart.setData(lineData);
            lineChart.invalidate();
        }
    }

    LineGraphSeries<DataPoint> lineSeries1, lineSeries2;
    LineGraphSeriesCustom<DataPoint> radarSeries;
    private void setUpGraph(ArrayList<DataPoint[]> linePoints, ArrayList<DataPoint[]> radarPoints) {
        List<Integer> colors = List.of(Color.RED, Color.MAGENTA, Color.GREEN, Color.YELLOW);
        if (viewSwitch == 1) {
            graph1.removeAllSeries();
            int counter = 0;
            try {
                lineSeries1 = new LineGraphSeries<>(linePoints.get(0));
                lineSeries2 = new LineGraphSeries<>(linePoints.get(1));
            }
            catch (Exception e){

            }

            lineSeries1.setTitle("Free space");
            //lineSeries1.setColor(colors.get(0));
            lineSeries2.setTitle("Free space unlimited");
            //lineSeries2.setColor(colors.get(1));

            graph1.addSeries(lineSeries1);
            graph1.addSeries(lineSeries2);

        }
        else if (viewSwitch == 2) {
            graph2.removeAllSeries();
            for (DataPoint[] points : radarPoints) {
                radarSeries = new LineGraphSeriesCustom<>(points);
                radarSeries.setDrawDataPoints(true);
                radarSeries.setDataPointsRadius(10);
                graph2.addSeries(radarSeries);
            }

        }
    }

    private void setUpRadar() {
        double radius = 1;
        double centerX = 0;
        double centerY = 0;

        List<Entry> dataPoints1 = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2 / 24;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            dataPoints1.add(new Entry((float) x, (float) y));
        }

        //dataPoints1.sort(new EntryXComparator());

        LineDataSet lineDataSet1 = new LineDataSet(dataPoints1, "limitedSpace");
        lineDataSet1.setColor(Color.RED);
        //lineDataSet2.setColor(Color.MAGENTA);

        LineData lineData = new LineData();
        lineData.addDataSet(lineDataSet1);
        //lineData.addDataSet(lineDataSet2);
        radarChart.setData(lineData);
        radarChart.removeAllViews();
        radarChart.invalidate();
    }

    private void initUI() {
        buttonConnect = findViewById(R.id.buttonConnect);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        /*Buttons*/
        button1 = findViewById(R.id.button_on_off);
        button2 = findViewById(R.id.button_startup);
        button3 = findViewById(R.id.button_startup_l);
        button4 = findViewById(R.id.button_startup_R);
        button5 = findViewById(R.id.button5);
        button6 = findViewById(R.id.button6);
        buttonSettings = findViewById(R.id.button_settings);
        buttonSaveSettings = findViewById(R.id.buttonSaveSettings);

        /*TextViews*/
        textViewSpeedL = findViewById(R.id.textView_speed_L_value);
        textViewSpeedR = findViewById(R.id.textView_speed_value);
        textViewStatus = findViewById(R.id.textView_status_value);
        textViewTrackAngle = findViewById(R.id.textView_angle_value);
        textViewP = findViewById(R.id.textView_P_value);
        textViewI = findViewById(R.id.textView_I_value);
        textViewD = findViewById(R.id.textView_D_value);
        textViewMax = findViewById(R.id.textView_maxZone_value);
        textViewMin = findViewById(R.id.textView_minZone_value);
        contentSwitch = findViewById(R.id.textView_caption_Controls);
        buttonData = findViewById(R.id.buttonData);
        buttonConsole = findViewById(R.id.buttonConsole);
        buttonMod = findViewById(R.id.buttonModButton);

        /*Sliders*/
        sliderSpeed = findViewById(R.id.slider_speed);
        sliderSpeed.addOnChangeListener(changeListener);
        sliderSpeed.addOnSliderTouchListener(touchListener);
        sliderSpeedP = findViewById(R.id.slider_speed_P);
        sliderSpeedP.addOnChangeListener(changeListener);
        sliderSpeedP.addOnSliderTouchListener(touchListener);
        sliderSpeedI = findViewById(R.id.slider_speed_I);
        sliderSpeedI.addOnChangeListener(changeListener);
        sliderSpeedI.addOnSliderTouchListener(touchListener);
        sliderSpeedD = findViewById(R.id.slider_speed_D);
        sliderSpeedD.addOnChangeListener(changeListener);
        sliderSpeedD.addOnSliderTouchListener(touchListener);
        sliderTurnP = findViewById(R.id.slider_turn_P);
        sliderTurnP.addOnSliderTouchListener(touchListener);
        sliderTurnI = findViewById(R.id.slider_turn_I);
        sliderTurnI.addOnSliderTouchListener(touchListener);
        sliderTurnD = findViewById(R.id.slider_turn_D);
        sliderTurnD.addOnSliderTouchListener(touchListener);

        /*EditText*/
        pSpeed = findViewById(R.id.number_speed_P);
        if (settingsSP.contains("pSpeed")) {
            pSpeed.setText(String.valueOf(settingsSP.getInt("pSpeed", 10)));
            sliderSpeedP.setValueTo(settingsSP.getInt("pSpeed", 10));
        }
        iSpeed = findViewById(R.id.number_speed_I);
        if (settingsSP.contains("iSpeed")) {
            iSpeed.setText(String.valueOf(settingsSP.getInt("iSpeed", 10)));
            sliderSpeedI.setValueTo(settingsSP.getInt("iSpeed", 10));
        }
        dSpeed = findViewById(R.id.number_speed_D);
        if (settingsSP.contains("dSpeed")) {
            dSpeed.setText(String.valueOf(settingsSP.getInt("dSpeed", 10)));
            sliderSpeedD.setValueTo(settingsSP.getInt("dSpeed", 10));
        }
        pSpeedStep = findViewById(R.id.number_speed_P_step);
        if (settingsSP.contains("pSpeedStep")) {
            pSpeedStep.setText(String.valueOf(settingsSP.getFloat("pSpeedStep", 1)));
            sliderSpeedP.setStepSize(settingsSP.getFloat("pSpeedStep", 1));
        }
        iSpeedStep = findViewById(R.id.number_speed_I_step);
        if (settingsSP.contains("iSpeedStep")) {
            iSpeedStep.setText(String.valueOf(settingsSP.getFloat("iSpeedStep", 1)));
            sliderSpeedI.setStepSize(settingsSP.getFloat("iSpeedStep", 1));
        }
        dSpeedStep = findViewById(R.id.number_speed_D_step);
        if (settingsSP.contains("dSpeedStep")) {
            dSpeedStep.setText(String.valueOf(settingsSP.getFloat("dSpeedStep", 1)));
            sliderSpeedD.setStepSize(settingsSP.getFloat("dSpeedStep", 1));
        }
        pTurn = findViewById(R.id.number_turn_P);
        if (settingsSP.contains("pTurn")) {
            pTurn.setText(String.valueOf(settingsSP.getInt("pTurn", 10)));
            sliderTurnP.setValueTo(settingsSP.getInt("pTurn", 10));
        }
        iTurn = findViewById(R.id.number_turn_I);
        if (settingsSP.contains("iTurn")) {
            iTurn.setText(String.valueOf(settingsSP.getInt("iTurn", 10)));
            sliderTurnI.setValueTo(settingsSP.getInt("iTurn", 10));
        }
        dTurn = findViewById(R.id.number_turn_D);
        if (settingsSP.contains("dTurn")) {
            dTurn.setText(String.valueOf(settingsSP.getInt("dTurn", 10)));
            sliderTurnD.setValueTo(settingsSP.getInt("dTurn", 10));
        }
        pTurnStep = findViewById(R.id.number_turn_P_step);
        if (settingsSP.contains("pTurnStep")) {
            pTurnStep.setText(String.valueOf(settingsSP.getFloat("pTurnStep", 1)));
            sliderTurnP.setStepSize(settingsSP.getFloat("pTurnStep", 1));
        }
        iTurnStep = findViewById(R.id.number_turn_I_step);
        if (settingsSP.contains("iTurnStep")) {
            iTurnStep.setText(String.valueOf(settingsSP.getFloat("iTurnStep", 1)));
            sliderTurnI.setStepSize(settingsSP.getFloat("iTurnStep", 1));
        }
        dTurnStep = findViewById(R.id.number_turn_D_step);
        if (settingsSP.contains("dTurnStep")) {
            dTurnStep.setText(String.valueOf(settingsSP.getFloat("dTurnStep", 1)));
            sliderTurnD.setStepSize(settingsSP.getFloat("dTurnStep", 1));
        }

        layoutControls =findViewById(R.id.controlls);
        layoutRadar = findViewById(R.id.radarChartLayout);
        layoutRadar.setVisibility(View.GONE);
        layoutLine = findViewById(R.id.lineChartLayout);
        layoutLine.setVisibility(View.GONE);

        layoutSettings = findViewById(R.id.Settings);
        layoutSettings.setVisibility(View.GONE);

        lineChart = findViewById(R.id.chart);
        radarChart = findViewById(R.id.chart1);
        radarChart.setHardwareAccelerationEnabled(false);
        //radarChart = findViewById(R.id.chart);
        //chart.setViewPortOffsets(0, 0, 0, 0);
        //chart.setMinimumHeight(500);

        lineChart.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        lineChart.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1500, 1000);
                        lineChart.setLayoutParams(params);

                        lineChart.setTranslationX(-200);
                        lineChart.setTranslationY(400);
                    }
                });

        radarChart.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        radarChart.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1000, 1000);
                        radarChart.setLayoutParams(params);

                        radarChart.setTranslationX(0);
                        radarChart.setTranslationY(800);
                    }
                });

        XAxis xAxisLine = lineChart.getXAxis();
        xAxisLine.setAxisMaximum(0.4F);
        xAxisLine.setAxisMinimum(-0.4F);
        xAxisLine.setGranularity(0.01F);
        xAxisLine.setGranularityEnabled(true);
        YAxis yAxisLine1 = lineChart.getAxisLeft();
        YAxis yAxisLine2 = lineChart.getAxisRight();
        yAxisLine1.setAxisMaximum(2F);
        yAxisLine1.setAxisMinimum(0);
        yAxisLine2.setEnabled(false);
        xAxisLine.setPosition(XAxis.XAxisPosition.BOTTOM);

        XAxis xAxisRadar = radarChart.getXAxis();
        xAxisRadar.setAxisMaximum(4.2F);
        xAxisRadar.setAxisMinimum(-5.2F);
        YAxis yAxisRadar1 = radarChart.getAxisLeft();
        YAxis yAxisRadar2 = radarChart.getAxisRight();
        yAxisRadar1.setAxisMaximum(4.2F);
        yAxisRadar1.setAxisMinimum(-4.2F);
        yAxisRadar2.setEnabled(false);
        xAxisRadar.setPosition(XAxis.XAxisPosition.BOTTOM);

        graph1 = findViewById(R.id.graph1);
        graph1.getViewport().setXAxisBoundsManual(true);
        graph1.getViewport().setMinX(-0.4);
        graph1.getViewport().setMaxX(0.4);
        graph1.getViewport().setYAxisBoundsManual(true);
        graph1.getViewport().setMinY(0);
        graph1.getViewport().setMaxY(2);

        graph2 = findViewById(R.id.graph2);
        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinX(-1.65);
        graph2.getViewport().setMaxX(1.65);
        graph2.getViewport().setYAxisBoundsManual(true);
        graph2.getViewport().setMinY(-1.65);
        graph2.getViewport().setMaxY(1.65);
        graph2.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        graph2.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1000, 1000);
                        graph2.setLayoutParams(params);

                        //graph2.setTranslationX(0);
                        //graph2.setTranslationY(800);
                    }
                });

        graph1.getLegendRenderer().setVisible(true);
        graph1.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);


        //setUpRadar();

        /*Joysticks*/
    }

    Slider.OnSliderTouchListener touchListener = new Slider.OnSliderTouchListener() {
        @SuppressLint("RestrictedApi")
        @Override
        public void onStartTrackingTouch(Slider slider) {

        }

        @SuppressLint("RestrictedApi")
        @Override
        public void onStopTrackingTouch(Slider slider) {
            if (slider.getId() == R.id.slider_speed){
                String cmdText = "Speed:" + slider.getValue();
                connectedThread.write(cmdText);
            }
            else if (slider.getId() == R.id.slider_speed_P){
                String cmdText = "Speed_P:" + slider.getValue();
                connectedThread.write(cmdText);
            }
            else if (slider.getId() == R.id.slider_speed_I){
                String cmdText = "Speed_I:" + slider.getValue();
                connectedThread.write(cmdText);
            }
            else if (slider.getId() == R.id.slider_speed_D){
                String cmdText = "Speed_D:" + slider.getValue();
                connectedThread.write(cmdText);
            }
            else if (slider.getId() == R.id.slider_turn_P){
                String cmdText = "Turn_P:" + slider.getValue();
                textViewP.setText(String.valueOf(slider.getValue()));
                connectedThread.write(cmdText);
            }
            else if (slider.getId() == R.id.slider_turn_I){
                String cmdText = "Turn_I:" + slider.getValue();
                textViewI.setText(String.valueOf(slider.getValue()));
                connectedThread.write(cmdText);
            }
            else if (slider.getId() == R.id.slider_turn_D){
                String cmdText = "Turn_D:" + slider.getValue();
                textViewD.setText(String.valueOf(slider.getValue()));
                connectedThread.write(cmdText);
            }
        }
    };

    private final Slider.OnChangeListener changeListener =
            (slider, value, fromUser) -> {
                if (slider.getId() == R.id.slider_speed_P){
                    String cmdText = "Speed_P:" + slider.getValue();
                    connectedThread.write(cmdText);
                }
                else if (slider.getId() == R.id.slider_speed_I){
                    String cmdText = "Speed_I:" + slider.getValue();
                    connectedThread.write(cmdText);
                }
                else if (slider.getId() == R.id.slider_speed_D){
                    String cmdText = "Speed_D:" + slider.getValue();
                    connectedThread.write(cmdText);
                }

            };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE.getCode()){
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow
                // in your app.
            } else {
                // Explain to the user that the feature is unavailable because
                // the feature requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */


    /* =============================== Thread for Data Transfer =========================================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}