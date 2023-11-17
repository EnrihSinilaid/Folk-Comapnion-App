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

    private final List<Float> degrees = List.of(-106.875F, -95.625F, -84.375F, -73.125F, -61.875F, -50.625F, -39.375F, -28.125F, -16.875F, -5.625F
            , 5.625F, 16.875F, 28.125F, 39.375F, 50.625F, 61.875F, 73.125F, 84.375F, 95.625F, 106.875F);
    private String deviceName = null;

    public boolean connected = false;
    public boolean detached = false;
    SharedPreferences settingsSP;
    SharedPreferences PIDValuesSP;
    private String deviceAddress;
    public static CreateConnectThread createConnectThread;
    private double lastAngle = 0;
    private double lastSpeed = 0;
    private boolean isReverse = false;
    private boolean isOn = false;
    private boolean isConnected = false;
    private boolean isSOn = true;
    private boolean isDataOn = true;
    private boolean isConsoleOn = true;
    private boolean isDefault = true;
    private int viewSwitch = 0;
    private boolean settings = false;
    private int SwitchPID = 0;
    Button button1, button2, button3, button4, button5, button6, buttonConnect, buttonSettings, buttonSaveSettings, buttonData, buttonConsole, buttonMod, buttonSwitchPID;
    TextView textViewSpeedL, textViewSpeedR, textViewStatus, textViewTrackAngle, textViewPSpeedF, textViewISpeedF, textViewDSpeedF, textViewPSpeedB, textViewISpeedB, textViewDSpeedB, textViewP, textViewI, textViewD, textViewMax, textViewMin, contentSwitch;
    EditText pMaxEdit, pMinEdit, pStepEdit, iMaxEdit, iMinEdit, iStepEdit, dMaxEdit, dMinEdit, dSpeedEdit, maxPidMaxEdit, maxPidMinEdit, maxPidStepEdit, minPidMaxEdit, minPidMinEdit, minPidStepEdit, maxiPidMaxEdit, maxiPidMinEdit, maxiPidStepEdit;
    Slider sliderSpeed, sliderP, sliderI, sliderD, sliderPIDMax, sliderPIDMin, sliderMaxI;
    Toolbar toolbar;
    ProgressBar progressBar;
    LinearLayout layoutControls;
    TableLayout layoutSettings;
    FrameLayout layoutLine, layoutRadar;
    LineChart lineChart;
    LineChartCustom radarChart;
    GraphView graph1, graph2;
    private final BluetoothHandler handler = new BluetoothHandler(this);
    private final String[] requiredPermissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsSP = this.getSharedPreferences("settings", 0);
        PIDValuesSP = this.getSharedPreferences("PID", 0);
        List<String> missingPermissions = hasPermissions(this, requiredPermissions);
        if(!missingPermissions.isEmpty()){
            requestPermissions(missingPermissions.toArray(new String[0]), 100);
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
                sendData("off");
            }
            else {
                button1.setText("Off");
                sendData("on");
            }
            isOn = !isOn;
        });

        button2.setOnClickListener(view -> {
            String cmdText = "startup";
            sendData(cmdText);
            button1.setText("Off");
            isOn = true;
        });

        button3.setOnClickListener(view -> {
            String cmdText = "startupL";
            sendData(cmdText);
            button1.setText("Off");
            isOn = true;
        });

        button4.setOnClickListener(view -> {
            String cmdText = "startupR";
            sendData(cmdText);
            button1.setText("Off");
            isOn = true;
        });

        /*
        button3.setOnClickListener(view -> {
            if (isReverse){
                button3.setText("Forward");
                sendData("reverse");
            }
            else {
                button3.setText("Reverse");
                sendData("forward");
            }
            isReverse = !isReverse;
        });*/

        buttonData.setOnClickListener(view -> {
            if (isDataOn){
                buttonData.setText("Data On ");
                sendData("dataOff");
            }
            else {
                buttonData.setText("Data Off");
                sendData("dataOn");
            }
            isDataOn = !isDataOn;
        });

        buttonConsole.setOnClickListener(view -> {
            if (isConsoleOn){
                buttonConsole.setText("Console On ");
                sendData("printOff");
            }
            else {
                buttonConsole.setText("Console Off");
                sendData("printOn");
            }
            isConsoleOn = !isConsoleOn;
        });

        buttonMod.setOnClickListener(view -> {
            if (isDefault){
                buttonMod.setText("Set Slow");
                sendData("MDef");
            }
            else {
                buttonMod.setText("Set Def");
                sendData("MSlow");
            }
            isDefault = !isDefault;
        });

        buttonSwitchPID.setOnClickListener(view -> {
            SharedPreferences.Editor editor = PIDValuesSP.edit();
            switch(SwitchPID){
                case 0: {
                    buttonSwitchPID.setText("Back PID");
                    SwitchPID = 1;
                    editor.putFloat("pFSpeed", (sliderP.getValue()));
                    editor.putFloat("iFSpeed", (sliderI.getValue()));
                    editor.putFloat("dFSpeed", (sliderD.getValue()));
                    editor.apply();
                    sliderP.setValue(PIDValuesSP.getFloat("pBSpeed", settingsSP.getInt("pMin", 50)));
                    sliderI.setValue(PIDValuesSP.getFloat("iBSpeed", settingsSP.getInt("iMin", 50)));
                    sliderD.setValue(PIDValuesSP.getFloat("dBSpeed", settingsSP.getInt("dMin", 50)));
                    break;
                }
                case 1: {
                    buttonSwitchPID.setText("Turn PID");
                    SwitchPID = 2;
                    editor.putFloat("pBSpeed", (sliderP.getValue()));
                    editor.putFloat("iBSpeed", (sliderI.getValue()));
                    editor.putFloat("dBSpeed", (sliderD.getValue()));
                    editor.apply();
                    sliderP.setValue(PIDValuesSP.getFloat("pTurn", settingsSP.getInt("pMin", 50)));
                    sliderI.setValue(PIDValuesSP.getFloat("iTurn", settingsSP.getInt("iMin", 50)));
                    sliderD.setValue(PIDValuesSP.getFloat("dTurn", settingsSP.getInt("dMin", 50)));
                    break;
                }
                case 2:{
                    buttonSwitchPID.setText("Front PID");
                    SwitchPID = 0;
                    editor.putFloat("pTurn", (sliderP.getValue()));
                    editor.putFloat("iTurn", (sliderI.getValue()));
                    editor.putFloat("dTurn", (sliderD.getValue()));
                    editor.apply();
                    sliderP.setValue(PIDValuesSP.getFloat("pFSpeed", settingsSP.getInt("pMin", 50)));
                    sliderI.setValue(PIDValuesSP.getFloat("iFSpeed", settingsSP.getInt("iMin", 50)));
                    sliderD.setValue(PIDValuesSP.getFloat("dFSpeed", settingsSP.getInt("dMin", 50)));
                    break;
                }
            }
        });

        button5.setOnClickListener(view -> {
            if (isSOn){
                button5.setText("Serv. On");
                sendData("SOff");
            }
            else {
                button5.setText("Serv. Off");
                sendData("SOn");
            }
            isSOn = !isSOn;
        });

        button6.setOnClickListener(view -> {
            if (createConnectThread != null){
                String cmdText = "Disconnect";
                sendData(cmdText);
                createConnectThread.cancel();
                toolbar.setSubtitle("Disconnected!");
                connected = false;
                detached = true;
            }
        });
        buttonSaveSettings.setOnClickListener(view -> {
            SharedPreferences.Editor editor = settingsSP.edit();
            if (pMaxEdit.getText().length() > 0){
                sliderP.setValueTo(Integer.parseInt(pMaxEdit.getText().toString()));
                pMaxEdit.setText(pMaxEdit.getText().toString());
                editor.putInt("pMax", Integer.parseInt(pMaxEdit.getText().toString()));
            }
            if (iMaxEdit.getText().length() > 0){
                sliderI.setValueTo(Integer.parseInt(iMaxEdit.getText().toString()));
                iMaxEdit.setText(iMaxEdit.getText().toString());
                editor.putInt("iMax", Integer.parseInt(iMaxEdit.getText().toString()));

            }
            if (dMaxEdit.getText().length() > 0){
                sliderD.setValueTo(Integer.parseInt(dMaxEdit.getText().toString()));
                dMaxEdit.setText(dMaxEdit.getText().toString());
                editor.putInt("dMax", Integer.parseInt(dMaxEdit.getText().toString()));

            }
            if (pMinEdit.getText().length() > 0){
                sliderP.setValueFrom(Integer.parseInt(pMinEdit.getText().toString()));
                pMinEdit.setText(pMinEdit.getText().toString());
                editor.putInt("pMin", Integer.parseInt(pMinEdit.getText().toString()));
            }
            if (iMinEdit.getText().length() > 0){
                sliderI.setValueFrom(Integer.parseInt(iMinEdit.getText().toString()));
                iMinEdit.setText(iMinEdit.getText().toString());
                editor.putInt("iMin", Integer.parseInt(iMinEdit.getText().toString()));

            }
            if (dMinEdit.getText().length() > 0){
                sliderD.setValueFrom(Integer.parseInt(dMinEdit.getText().toString()));
                dMinEdit.setText(dMinEdit.getText().toString());
                editor.putInt("dMin", Integer.parseInt(dMinEdit.getText().toString()));

            }
            if (pStepEdit.getText().length() > 0){
                sliderP.setStepSize(Float.parseFloat(pStepEdit.getText().toString()));
                pStepEdit.setText(pStepEdit.getText().toString());
                editor.putFloat("pStep", Float.parseFloat(pStepEdit.getText().toString()));
            }
            if (iStepEdit.getText().length() > 0){
                sliderI.setStepSize(Float.parseFloat(iStepEdit.getText().toString()));
                iStepEdit.setText(iStepEdit.getText().toString());
                editor.putFloat("iStep", Float.parseFloat(iStepEdit.getText().toString()));

            }
            if (dSpeedEdit.getText().length() > 0){
                sliderD.setStepSize(Float.parseFloat(dSpeedEdit.getText().toString()));
                dSpeedEdit.setText(dSpeedEdit.getText().toString());
                editor.putFloat("dStep", Float.parseFloat(dSpeedEdit.getText().toString()));

            }
            if (maxPidMaxEdit.getText().length() > 0){
                sliderPIDMax.setValueTo(Integer.parseInt(maxPidMaxEdit.getText().toString()));
                maxPidMaxEdit.setText(maxPidMaxEdit.getText().toString());
                editor.putInt("maxPidMax", Integer.parseInt(maxPidMaxEdit.getText().toString()));

            }
            if (minPidMaxEdit.getText().length() > 0){
                sliderPIDMin.setValueTo(Integer.parseInt(minPidMaxEdit.getText().toString()));
                minPidMaxEdit.setText(minPidMaxEdit.getText().toString());
                editor.putInt("minPidMax", Integer.parseInt(minPidMaxEdit.getText().toString()));

            }
            if (maxiPidMaxEdit.getText().length() > 0){
                sliderMaxI.setValueTo(Integer.parseInt(maxiPidMaxEdit.getText().toString()));
                maxiPidMaxEdit.setText(maxiPidMaxEdit.getText().toString());
                editor.putInt("maxiPidMax", Integer.parseInt(maxiPidMaxEdit.getText().toString()));
            }
            if (maxPidMinEdit.getText().length() > 0){
                sliderPIDMax.setValueFrom(Integer.parseInt(maxPidMinEdit.getText().toString()));
                maxPidMinEdit.setText(maxPidMinEdit.getText().toString());
                editor.putInt("maxPidMin", Integer.parseInt(maxPidMinEdit.getText().toString()));

            }
            if (minPidMinEdit.getText().length() > 0){
                sliderPIDMin.setValueFrom(Integer.parseInt(minPidMinEdit.getText().toString()));
                minPidMinEdit.setText(minPidMinEdit.getText().toString());
                editor.putInt("minPidMin", Integer.parseInt(minPidMinEdit.getText().toString()));

            }
            if (maxiPidMinEdit.getText().length() > 0){
                sliderMaxI.setValueFrom(Integer.parseInt(maxiPidMinEdit.getText().toString()));
                maxiPidMinEdit.setText(maxiPidMinEdit.getText().toString());
                editor.putInt("maxiPidMin", Integer.parseInt(maxiPidMinEdit.getText().toString()));
            }
            if (maxPidStepEdit.getText().length() > 0){
                sliderPIDMax.setStepSize(Float.parseFloat(maxPidStepEdit.getText().toString()));
                maxPidStepEdit.setText(maxPidStepEdit.getText().toString());
                editor.putFloat("maxiPidStep", Float.parseFloat(maxPidStepEdit.getText().toString()));

            }
            if (minPidStepEdit.getText().length() > 0){
                sliderPIDMin.setStepSize(Float.parseFloat(minPidStepEdit.getText().toString()));
                minPidStepEdit.setText(minPidStepEdit.getText().toString());
                editor.putFloat("minPidStep", Float.parseFloat(minPidStepEdit.getText().toString()));

            }
            if (maxiPidStepEdit.getText().length() > 0){
                sliderMaxI.setStepSize(Float.parseFloat(maxiPidStepEdit.getText().toString()));
                maxiPidStepEdit.setText(maxiPidStepEdit.getText().toString());
                editor.putFloat("maxiPidStep", Float.parseFloat(maxiPidStepEdit.getText().toString()));
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
        buttonSwitchPID = findViewById(R.id.buttonPIDSwitch);

        /*TextViews*/
        textViewSpeedL = findViewById(R.id.textView_speed_L_value);
        textViewSpeedR = findViewById(R.id.textView_speed_value);
        textViewStatus = findViewById(R.id.textView_status_value);
        textViewTrackAngle = findViewById(R.id.textView_angle_value);
        textViewPSpeedF = findViewById(R.id.textView_P_value_Speed_F);
        textViewISpeedF = findViewById(R.id.textView_I_value_Speed_F);
        textViewDSpeedF = findViewById(R.id.textView_D_value_Speed_F);
        textViewPSpeedB = findViewById(R.id.textView_P_value_Speed_B);
        textViewISpeedB = findViewById(R.id.textView_I_value_Speed_B);
        textViewDSpeedB = findViewById(R.id.textView_D_value_Speed_B);
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
        sliderP = findViewById(R.id.slider_P);
        sliderP.addOnChangeListener(changeListener);
        sliderP.addOnSliderTouchListener(touchListener);
        sliderI = findViewById(R.id.slider_I);
        sliderI.addOnChangeListener(changeListener);
        sliderI.addOnSliderTouchListener(touchListener);
        sliderD = findViewById(R.id.slider_D);
        sliderD.addOnChangeListener(changeListener);
        sliderD.addOnSliderTouchListener(touchListener);
        sliderPIDMax = findViewById(R.id.slider_max);
        sliderPIDMax.addOnSliderTouchListener(touchListener);
        sliderPIDMin = findViewById(R.id.slider_min);
        sliderPIDMin.addOnSliderTouchListener(touchListener);
        sliderMaxI = findViewById(R.id.slider_max_i);
        sliderMaxI.addOnSliderTouchListener(touchListener);

        /*EditText*/
        pMaxEdit = findViewById(R.id.number_P_max);
        if (settingsSP.contains("pMax")) {
            pMaxEdit.setText(String.valueOf(settingsSP.getInt("pMax", 50)));
            sliderP.setValueTo(settingsSP.getInt("pMax", 50));
        }
        iMaxEdit = findViewById(R.id.number_I_max);
        if (settingsSP.contains("iMax")) {
            iMaxEdit.setText(String.valueOf(settingsSP.getInt("iMax", 50)));
            sliderI.setValueTo(settingsSP.getInt("iMax", 50));
        }
        dMaxEdit = findViewById(R.id.number_D_max);
        if (settingsSP.contains("dMax")) {
            dMaxEdit.setText(String.valueOf(settingsSP.getInt("dMax", 50)));
            sliderD.setValueTo(settingsSP.getInt("dMax", 50));
        }
        pMinEdit = findViewById(R.id.number_P_min);
        if (settingsSP.contains("pMin")) {
            pMinEdit.setText(String.valueOf(settingsSP.getInt("pMin", 0)));
            sliderP.setValueFrom(settingsSP.getInt("pMin", 0));
            sliderP.setValue(settingsSP.getInt("pMin", 0));
        }
        iMinEdit = findViewById(R.id.number_I_min);
        if (settingsSP.contains("iMin")) {
            iMinEdit.setText(String.valueOf(settingsSP.getInt("iMin", 0)));
            sliderI.setValueFrom(settingsSP.getInt("iMin", 0));
            sliderI.setValue(settingsSP.getInt("iMin", 0));
        }
        dMinEdit = findViewById(R.id.number_D_min);
        if (settingsSP.contains("dMin")) {
            dMinEdit.setText(String.valueOf(settingsSP.getInt("dMin", 0)));
            sliderD.setValueFrom(settingsSP.getInt("dMin", 0));
            sliderD.setValue(settingsSP.getInt("dMin", 0));
        }
        pStepEdit = findViewById(R.id.number_P_step);
        if (settingsSP.contains("pStep")) {
            pStepEdit.setText(String.valueOf(settingsSP.getFloat("pStep", 1)));
            sliderP.setStepSize(settingsSP.getFloat("pStep", 1));
        }
        iStepEdit = findViewById(R.id.number_I_step);
        if (settingsSP.contains("iStep")) {
            iStepEdit.setText(String.valueOf(settingsSP.getFloat("iStep", 1)));
            sliderI.setStepSize(settingsSP.getFloat("iStep", 1));
        }
        dSpeedEdit = findViewById(R.id.number_D_step);
        if (settingsSP.contains("dStep")) {
            dSpeedEdit.setText(String.valueOf(settingsSP.getFloat("dStep", 1)));
            sliderD.setStepSize(settingsSP.getFloat("dStep", 1));
        }
        maxPidMaxEdit = findViewById(R.id.number_max_max);
        if (settingsSP.contains("maxPidMax")) {
            maxPidMaxEdit.setText(String.valueOf(settingsSP.getInt("maxPidMax", 50)));
            sliderPIDMax.setValueTo(settingsSP.getInt("maxPidMax", 50));
        }
        minPidMaxEdit = findViewById(R.id.number_min_max);
        if (settingsSP.contains("minPidMax")) {
            minPidMaxEdit.setText(String.valueOf(settingsSP.getInt("minPidMax", 50)));
            sliderPIDMin.setValueTo(settingsSP.getInt("minPidMax", 50));
        }
        maxiPidMaxEdit = findViewById(R.id.number_maxi_max);
        if (settingsSP.contains("maxiPidMax")) {
            maxiPidMaxEdit.setText(String.valueOf(settingsSP.getInt("maxiPidMax", 50)));
            sliderMaxI.setValueTo(settingsSP.getInt("maxiPidMax", 50));
        }
        maxPidMinEdit = findViewById(R.id.number_max_min);
        if (settingsSP.contains("maxPidMin")) {
            maxPidMinEdit.setText(String.valueOf(settingsSP.getInt("maxPidMin", 0)));
            sliderPIDMax.setValueFrom(settingsSP.getInt("maxPidMin", 0));
            sliderPIDMax.setValue(settingsSP.getInt("maxPidMin", 0));
        }
        minPidMinEdit = findViewById(R.id.number_min_min);
        if (settingsSP.contains("minPidMin")) {
            minPidMinEdit.setText(String.valueOf(settingsSP.getInt("minPidMin", 0)));
            sliderPIDMin.setValueFrom(settingsSP.getInt("minPidMin", 0));
            sliderPIDMin.setValue(settingsSP.getInt("minPidMin", 0));
        }
        maxiPidMinEdit = findViewById(R.id.number_maxi_min);
        if (settingsSP.contains("maxiPidMin")) {
            maxiPidMinEdit.setText(String.valueOf(settingsSP.getInt("maxiPidMin", 0)));
            sliderMaxI.setValueFrom(settingsSP.getInt("maxiPidMin", 0));
            sliderMaxI.setValue(settingsSP.getInt("maxiPidMin", 0));
        }
        maxPidStepEdit = findViewById(R.id.number_max_step);
        if (settingsSP.contains("maxPidStep")) {
            maxPidStepEdit.setText(String.valueOf(settingsSP.getFloat("maxPidStep", 1)));
            sliderPIDMax.setStepSize(settingsSP.getFloat("maxPidStep", 1));
        }
        minPidStepEdit = findViewById(R.id.number_min_step);
        if (settingsSP.contains("minPidStep")) {
            minPidStepEdit.setText(String.valueOf(settingsSP.getFloat("minPidStep", 1)));
            sliderPIDMin.setStepSize(settingsSP.getFloat("minPidStep", 1));
        }
        maxiPidStepEdit = findViewById(R.id.number_maxi_step);
        if (settingsSP.contains("maxiPidStep")) {
            maxiPidStepEdit.setText(String.valueOf(settingsSP.getFloat("maxiPidStep", 1)));
            sliderMaxI.setStepSize(settingsSP.getFloat("maxiPidStep", 1));
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
        graph2.getViewport().setMinX(-1.55);
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
                String cmdText = "Dist:" + slider.getValue();
                sendData(cmdText);
            }
            else if (slider.getId() == R.id.slider_P){
                String cmdText = "";
                switch (SwitchPID){
                    case 0: {
                        cmdText = "Speed_P_F:" + slider.getValue();
                        textViewPSpeedF.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                    case 1: {
                        cmdText = "Speed_P_B:" + slider.getValue();
                        textViewPSpeedB.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                    case 2: {
                        cmdText = "Turn_P:" + slider.getValue();
                        textViewP.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                }
                sendData(cmdText);
            }
            else if (slider.getId() == R.id.slider_I){
                String cmdText = "";
                switch (SwitchPID){
                    case 0: {
                        cmdText = "Speed_I_F:" + slider.getValue();
                        textViewISpeedF.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                    case 1: {
                        cmdText = "Speed_I_B:" + slider.getValue();
                        textViewISpeedB.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                    case 2: {
                        cmdText = "Turn_I:" + slider.getValue();
                        textViewI.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                }
                sendData(cmdText);
            }
            else if (slider.getId() == R.id.slider_D){
                String cmdText = "";
                switch (SwitchPID){
                    case 0: {
                        cmdText = "Speed_D_F:" + slider.getValue();
                        textViewDSpeedF.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                    case 1: {
                        cmdText = "Speed_D_B:" + slider.getValue();
                        textViewDSpeedB.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                    case 2: {
                        cmdText = "Turn_D:" + slider.getValue();
                        textViewD.setText(String.valueOf(slider.getValue()));
                        break;
                    }
                }
                sendData(cmdText);
            }
            else if (slider.getId() == R.id.slider_max){
                String cmdText = "Max_Pid:" + slider.getValue();
                //textViewISpeedF.setText(String.valueOf(slider.getValue()));
                sendData(cmdText);
            }
            else if (slider.getId() == R.id.slider_min){
                String cmdText = "Min_Pid:" + slider.getValue();
                //textViewISpeedF.setText(String.valueOf(slider.getValue()));
                sendData(cmdText);
            }
            else if (slider.getId() == R.id.slider_max_i){
                String cmdText = "Max_I_Pid:" + slider.getValue();
                //textViewISpeedF.setText(String.valueOf(slider.getValue()));
                sendData(cmdText);
            }
        }
    };

    private final Slider.OnChangeListener changeListener =
            (slider, value, fromUser) -> {
                /*if (slider.getId() == R.id.slider_P){
                    String cmdText = "Speed_P:" + slider.getValue();
                    sendData(cmdText);
                }
                else if (slider.getId() == R.id.slider_I){
                    String cmdText = "Speed_I:" + slider.getValue();
                    sendData(cmdText);
                }
                else if (slider.getId() == R.id.slider_speed_D){
                    String cmdText = "Speed_D:" + slider.getValue();
                    sendData(cmdText);
                }*/

            };

    @Override
    protected void onPause() {
        super.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onResume() {
        super.onResume();
    }

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

    public static List<String> hasPermissions(Context context, String[] permissions) {
        List<String> missingPermissions = new ArrayList<>();
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }
        }
        return missingPermissions;
    }
    
    public void sendData(String msg){
        if (isConnected){
            connectedThread.write(msg);
        }
    }

    private class BluetoothHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public BluetoothHandler(MainActivity activity){
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == CONNECTING_STATUS.getCode()) {
                    if (msg.arg1 == 1) {
                        Log.e("Bluetooth", "in run connected");
                        activity.toolbar.setSubtitle("Connected to " + deviceName);
                        isConnected = true;
                        sendData("syncData");
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
                    } else if (arduinoMsg.contains("SpeedF:")) {
                        textViewSpeedR.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("SpeedB:")) {
                        textViewSpeedL.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("Time:")) {
                        textViewMin.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    } else if (arduinoMsg.contains("Error:")) {
                        textViewMax.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                    }
                        /*else if (arduinoMsg.contains("Error:")){
                            textViewStatus.setText(arduinoMsg.substring(arduinoMsg.indexOf(":")));
                        }*/
                    else if (arduinoMsg.contains("PF:")) {
                        textViewPSpeedF.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("pFSpeed", (Float.parseFloat((String) textViewPSpeedF.getText())));
                        editor.apply();
                        if (SwitchPID == 0){
                            sliderP.setValue(Float.parseFloat((String) textViewPSpeedF.getText()));
                        }
                    } else if (arduinoMsg.contains("IF:")) {
                        textViewISpeedF.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("iFSpeed", (Float.parseFloat((String) textViewISpeedF.getText())));
                        editor.apply();
                        if (SwitchPID == 0){
                            sliderI.setValue(Float.parseFloat((String) textViewISpeedF.getText()));
                        }
                    } else if (arduinoMsg.contains("DF:")) {
                        textViewDSpeedF.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("dFSpeed", (Float.parseFloat((String) textViewDSpeedF.getText())));
                        editor.apply();
                        if (SwitchPID == 0){
                            sliderD.setValue(Float.parseFloat((String) textViewDSpeedF.getText()));
                        }
                    }
                    else if (arduinoMsg.contains("PB:")) {
                        textViewPSpeedB.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("pBSpeed", (Float.parseFloat((String) textViewPSpeedB.getText())));
                        editor.apply();
                        if (SwitchPID == 1){
                            sliderP.setValue(Float.parseFloat((String) textViewPSpeedB.getText()));
                        }
                    } else if (arduinoMsg.contains("IB:")) {
                        textViewISpeedB.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("iBSpeed", (Float.parseFloat((String) textViewISpeedB.getText())));
                        editor.apply();
                        if (SwitchPID == 1){
                            sliderI.setValue(Float.parseFloat((String) textViewISpeedB.getText()));
                        }
                    } else if (arduinoMsg.contains("DB:")) {
                        textViewDSpeedB.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("dBSpeed", (Float.parseFloat((String) textViewDSpeedB.getText())));
                        editor.apply();
                        if (SwitchPID == 1){
                            sliderD.setValue(Float.parseFloat((String) textViewDSpeedB.getText()));
                        }
                    }
                    else if (arduinoMsg.contains("Turn_P:")) {
                        textViewP.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("pTurn", (Float.parseFloat((String) textViewP.getText())));
                        editor.apply();
                        if (SwitchPID == 2){
                            sliderP.setValue(Float.parseFloat((String) textViewP.getText()));
                        }
                    } else if (arduinoMsg.contains("Turn_I:")) {
                        textViewI.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("iTurn", (Float.parseFloat((String) textViewI.getText())));
                        editor.apply();
                        if (SwitchPID == 2){
                            sliderI.setValue(Float.parseFloat((String) textViewI.getText()));
                        }
                    } else if (arduinoMsg.contains("Turn_D:")) {
                        textViewD.setText(arduinoMsg.substring(arduinoMsg.indexOf(":") + 1));
                        SharedPreferences.Editor editor = PIDValuesSP.edit();
                        editor.putFloat("dTurn", (Float.parseFloat((String) textViewD.getText())));
                        editor.apply();
                        if (SwitchPID == 2){
                            sliderD.setValue(Float.parseFloat((String) textViewD.getText()));
                        }
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
}