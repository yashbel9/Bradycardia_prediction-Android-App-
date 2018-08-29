package com.example.android.group_23;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class MainActivity extends Activity implements ServiceCallbacks {

    PeakDetection peakDetection = new PeakDetection();
    FileInputStream fileInputStream = null;
    Double[][] data1;
    private String FileName, FilePath;
    private static final Random RANDOM = new Random();
    private LineGraphSeries<DataPoint> series, series1, series2;
    private int xcoor = 0;
    private boolean flag1 = true, runFlag = false;
    private static final String TAG = MainActivity.class.getSimpleName();
    File mFolderStructure;
    Boolean mSuccess = false;
    private boolean bound = false;
    private static final int PICKFILE_RESULT_CODE = 0;
    private ParcelFileDescriptor mInputPFD = null;
    private  static ProgressBar progressBar;
    private ProgressBar  progressBar_cyclic;
    private static TextView textView;
    public static Handler handler = new Handler(Looper.getMainLooper());
    private static int progressStatus = 0;
    public double[] heartRates, temp_heart, variance, temp_variance;
    public double[][] trainData;
    public double mean, temp_mean;
    private RadioButton heartr, variance1;
    public RadioGroup radioButtonGroup;
    public int radioButtonID;
    private boolean flag = false, check_dir=false;
    long startTime, endTime;
    private String FILE_NAME = "all_data.txt";
    TextView textFile;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final GraphView graph = (GraphView) findViewById(R.id.plotgraph);
        final GraphView graph1 = (GraphView) findViewById(R.id.plotgraph1);
        textFile = (TextView)findViewById(R.id.textfile);
        series = new LineGraphSeries<DataPoint>();
        series1 = new LineGraphSeries<DataPoint>();
        series.setDrawDataPoints(true);
        series.setDrawBackground(true);
        Viewport viewport = graph.getViewport();
        //viewport.setYAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMinY(0);
        viewport.setMaxY(2000);
        viewport.setScrollable(true);

        Viewport viewport1 = graph1.getViewport();
        //viewport.setYAxisBoundsManual(true);
        viewport1.setMinX(0);
        viewport1.setMinY(0);
        viewport1.setMaxY(2000);
        viewport1.setScrollable(true);
        graph1.setVisibility(View.INVISIBLE);

        /**
         * Pass context to asyncTask
         */
//        mUploadDownload = new UploadDownload(this);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar_cyclic = (ProgressBar)findViewById(R.id.progressBar_cyclic);
//        textView = (TextView)findViewById(R.id.textView);
        progressBar_cyclic.setVisibility(View.GONE);

        radioButtonGroup = ((RadioGroup) findViewById(R.id.radioGroup));
        for (int i = 0; i < radioButtonGroup.getChildCount(); i++) {
            radioButtonGroup.getChildAt(i).setEnabled(false);
        }
        radioButtonGroup.check(R.id.radio_heartrate);

        if(!isStoragePermissionGranted() && !isInternetPermissionGranted()) {
            Log.e(TAG,"Storage Permission Denied!");
            Toast.makeText(MainActivity.this, "Storage Permission Denied!", Toast.LENGTH_SHORT).show();
            return;
        }
        final Button filePickerButton = findViewById(R.id.buttonFilePicker);
        final Button pick_button = findViewById(R.id.buttonpick);
        final Button pick_button1 = findViewById(R.id.accuracy);
        final Button prediction_button = findViewById(R.id.prediction);
        final Button prediction_button1 = findViewById(R.id.process_prediction);
        //Used to get the data which we give to the SVM prediction
        prediction_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try{
                            startTime = System.currentTimeMillis();
                            peakDetection.setCallbacks(MainActivity.this); // register
                            GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
                            gridLabel.setHorizontalAxisTitle("Time");
                            gridLabel = graph1.getGridLabelRenderer();
                            gridLabel.setHorizontalAxisTitle("Time");
                            System.out.println("Start");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    /**
                                     * The graph is paused and a temporary graph is overlapped when the STOP button onClick event occurs.
                                     */
                                    progressBar_cyclic.setVisibility(View.VISIBLE);
                                    filePickerButton.setEnabled(false);
                                }
                            });
                            //mFolderStructure = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "Android"+ File.separator +"data");

                            Log.d(TAG, "-------------------------------------------------------------" + FilePath);
                            FileInputStream fin =  new FileInputStream(FilePath);
                            Double[][] data = peakDetection.returnData(fin);

                            double[] column1 = new double[data.length-1];

                            for(int i=1;i<data.length-1;i++){
                                column1[i] = (double)data[i][1];
                            }

                            System.out.println("Data loaded successfully");

                            int[] peak_index = peakDetection.RpeakDetection(column1);

                            for(int i=0;i<peak_index.length;i++){
                                System.out.println(i+" "+peak_index[i]);
                            }
                            temp_heart = peakDetection.calculateHeartRate(peak_index);
                            System.out.println("------------------------------------HEART RATES:--------------------------------------------");
                            double sum = 0;

//                            DataPoint[] bradycardia_points = new DataPoint[heartRates.length];
//                            int cnt = 0;

                            flag = false;
                            for(int i=0;i<temp_heart.length;i++){
                                sum+=temp_heart[i];
                                if(temp_heart[i]<=60)
                                {
//                                    bradycardia_points[i] = new DataPoint(i, heartRates[i]);
//                                    cnt++;
                                    flag = true;
                                }
                            }

                            temp_mean = sum / temp_heart.length;
                            temp_variance = new double[temp_heart.length];
                            System.out.println("------------------------------------VARIANCES:--------------------------------------------");
                            Log.d(TAG,"Length is: " + temp_heart.length);;
                            boolean check = false;
                            for(int i=0;i<temp_heart.length;i++){
                                temp_variance[i] = (temp_heart[i] - temp_mean)*(temp_heart[i] - temp_mean);
                                if(i>=2000) {
                                    break;
                                }
                                System.out.println(temp_variance[i]);
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    /**
                                     * The graph is paused and a temporary graph is overlapped when the STOP button onClick event occurs.
                                     */
                                    progressBar_cyclic.setVisibility(View.GONE);
                                    filePickerButton.setEnabled(true);
                                }
                            });
                            System.out.println("END!");

                        }catch(Exception e){
                            System.out.println(e);
                        }
                    }
                }).start();

            }
            //SvmPredict svmPredict = new SvmPredict();
            //Toast.makeText(MainActivity.this, "Prediction:Subject is "+svmPredict.predictAction(temp_variance), Toast.LENGTH_SHORT).show();
        });
        //Used to give prediction
        prediction_button1.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                SvmPredict svmPredict = new SvmPredict();
                Toast.makeText(MainActivity.this, "Prediction:Subject is "+svmPredict.predictAction(temp_variance), Toast.LENGTH_SHORT).show();


            }});
        //Used to pick up the file
        pick_button.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent,PICKFILE_RESULT_CODE);

            }});
        //Used to get Accuracy
        pick_button1.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub

                Intent intent = new Intent(MainActivity.this,SvmPredict.class);
                startActivity(intent);

            }});
        /**
         * Handling RUN button onClick event
         */

        heartr = (RadioButton)findViewById(R.id.radio_heartrate);
        variance1 = (RadioButton)findViewById(R.id.radio_variance);
        //Used to process our data
        filePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try{
                            startTime = System.currentTimeMillis();
                            peakDetection.setCallbacks(MainActivity.this); // register
                            GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
                            gridLabel.setHorizontalAxisTitle("Time");
                            gridLabel = graph1.getGridLabelRenderer();
                            gridLabel.setHorizontalAxisTitle("Time");
                            System.out.println("Start");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    /**
                                     * The graph is paused and a temporary graph is overlapped when the STOP button onClick event occurs.
                                     */
                                    progressBar_cyclic.setVisibility(View.VISIBLE);
                                    filePickerButton.setEnabled(false);
                                }
                            });
                            mFolderStructure = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "Android"+ File.separator +"data");

                            Log.d(TAG, "-------------------------------------------------------------" + FilePath);
                            FileInputStream fin =  new FileInputStream(FilePath);
                            Double[][] data = peakDetection.returnData(fin);

                            double[] column1 = new double[data.length-1];

                            for(int i=1;i<data.length-1;i++){
                                column1[i] = (double)data[i][1];
                            }

                            System.out.println("Data loaded successfully");
                            //System.out.println(column1.length);

                            //System.out.println(column1[column1.length-1]);

                            int[] peak_index = peakDetection.RpeakDetection(column1);

                            System.out.println("------------------------------------PEAK RATES:--------------------------------------------");
                            for(int i=0;i<peak_index.length;i++){
                                System.out.println(i+" "+peak_index[i]);
                            }
                            heartRates = peakDetection.calculateHeartRate(peak_index);
                            System.out.println("------------------------------------HEART RATES:--------------------------------------------");
                            double sum = 0;
                            DataPoint[] points = new DataPoint[heartRates.length];
//                            DataPoint[] bradycardia_points = new DataPoint[heartRates.length];
//                            int cnt = 0;
                            LineGraphSeries<DataPoint> bradycardia_series = new LineGraphSeries<>();
                            bradycardia_series.setColor(Color.RED);

                            flag = false;
                            for(int i=0;i<heartRates.length;i++){
                                sum+=heartRates[i];
                                System.out.println(heartRates[i]);
                                points[i] = new DataPoint(i, heartRates[i]);
                                if(heartRates[i]<=60)
                                {
//                                    bradycardia_points[i] = new DataPoint(i, heartRates[i]);
//                                    cnt++;
                                    flag = true;
                                    bradycardia_series.appendData(new DataPoint(i, heartRates[i]), true, heartRates.length);
                                }
                            }
                            LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>(points);
                            graph.getViewport().setScalable(true);
                            graph.getViewport().setScalableY(true);
                            graph.addSeries(series2);
                            graph.addSeries(bradycardia_series);

                            mean = sum / heartRates.length;

                            Log.d(TAG,"MEAN HEART RATE:"+mean);
                            variance = new double[heartRates.length];
                            System.out.println("------------------------------------VARIANCES:--------------------------------------------");
                            final DataPoint[] points1 = new DataPoint[heartRates.length];
                            double[][] labelArray = new double[heartRates.length][2];
                            Log.d(TAG,"Length is: " + heartRates.length);
                            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

                            File file = new File(baseDir, FILE_NAME);
                            FileWriter writer = new FileWriter(file, true);
                            String value = "";
                            boolean check = false;
                            for(int i=0;i<heartRates.length;i++){
                                variance[i] = (heartRates[i] - mean)*(heartRates[i] - mean);
                                if(i<2000) {
                                    value += String.valueOf(i+":"+variance[i]+" ");
                                }
                                System.out.println(variance[i]);
                                points1[i] = new DataPoint(i, variance[i]);
                                labelArray[i][0] = variance[i];

                                if(heartRates[i] <= 60)
                                {
                                    check = true;
                                    labelArray[i][1] = 1;
                                    int index = i-5;
                                    while(index>=0 && index<=i)
                                    {
                                        labelArray[index][1] = 1;
                                        index++;
                                    }
                                    Log.d(TAG,heartRates[i]+":"+labelArray[i][1]);
                                }
                                else
                                {
                                    labelArray[i][1] = 0;
                                    Log.d(TAG,heartRates[i]+":"+labelArray[i][1]);
                                }
                            }
                            System.out.println(value);
                            if(check)
                                writer.append("1" + " ");
                            else
                                writer.append("0" + " ");
                            writer.append(value + "\n");
                            writer.close();
                            LineGraphSeries<DataPoint> series3 = new LineGraphSeries<>(points1);
                            graph1.getViewport().setScalable(true);
                            graph1.getViewport().setScalableY(true);
                            graph1.addSeries(series3);
                            radioButtonGroup = ((RadioGroup) findViewById(R.id.radioGroup));
                            radioButtonID = radioButtonGroup.getCheckedRadioButtonId();


                            for(int i=0;i<labelArray.length;i++)
                            {
                                Log.d(TAG,labelArray[i][0]+":"+labelArray[i][1]);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    /**
                                     * The graph is paused and a temporary graph is overlapped when the STOP button onClick event occurs.
                                     */
                                    progressBar_cyclic.setVisibility(View.GONE);
                                    filePickerButton.setEnabled(true);
                                    LineGraphSeries<DataPoint> series3 = new LineGraphSeries<>(points1);
                                    graph1.getViewport().setScalable(true);
                                    graph1.getViewport().setScalableY(true);

                                    for (int i = 0; i < radioButtonGroup.getChildCount(); i++) {
                                        radioButtonGroup.getChildAt(i).setEnabled(true);
                                    }
                                    String str = flag ? "YES":"NO";
                                    Toast.makeText(getApplicationContext(), "Bradycardia Detected:"+ str, Toast.LENGTH_SHORT).show();
                                    endTime = System.currentTimeMillis();
                                    Toast.makeText(getApplicationContext(), "Performance time(sec):"+ (endTime-startTime)/1000, Toast.LENGTH_SHORT).show();
                                }
                            });
                            radioButtonGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
                            {
                                @Override
                                public void onCheckedChanged(RadioGroup group, int checkedId) {
                                    // checkedId is the RadioButton selected
                                    Log.d(TAG,"checkedId:"+checkedId+";"+R.id.radio_variance);
                                    if(checkedId == R.id.radio_variance) {
                                        graph.setVisibility(View.INVISIBLE);
                                        graph1.setVisibility(View.VISIBLE);
                                        GridLabelRenderer gridLabel = graph1.getGridLabelRenderer();
                                        gridLabel.setHorizontalAxisTitle("Time");
                                    }
                                    else
                                    {
                                        graph.setVisibility(View.VISIBLE);
                                        graph1.setVisibility(View.INVISIBLE);
                                        GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
                                        gridLabel.setHorizontalAxisTitle("Time");
                                    }
                                }
                            });
                            System.out.println("END!");

                        }catch(Exception e){
                            System.out.println(e);
                        }
                    }
                }).start();



            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    final public Runnable updateRunnable = new Runnable() {
        public void run() {
            //call the activity method that updates the UI
            if(progressStatus<100){
                progressStatus+=1;
                progressBar.setProgress(progressStatus);
//                textView.setText(progressStatus+"/"+progressBar.getMax());
            } else {
                progressStatus = 0;
            }
            progressBar_cyclic.setVisibility(View.VISIBLE);;
        }
    };
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if(resultCode==RESULT_OK){

                    FilePath = data.getData().getPath();
                    FileName = data.getData().getLastPathSegment();
                    String[] temp = FileName.split("/");
                    FileName = temp[temp.length-1];
                    textFile.setText("Full Path: \n" + FileName + "\n"+ FilePath);
                    check_dir=true;
                }
                break;

        }
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    public void addEntry(final int progressStatus) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            handler.post(new Runnable() {
                public void run() {
                    progressBar.setProgress(progressStatus);
                    textView.setText(progressStatus+"/"+progressBar.getMax());
                }
            });
            }
        });

    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"WRITE_EXTERNAL_STORAGE Permission is granted");
                return true;
            } else {

                Log.d(TAG,"WRITE_EXTERNAL_STORAGE Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else {
            Log.d(TAG,"WRITE_EXTERNAL_STORAGE Permission is granted");
            return true;
        }
    }
    private void copy(File source, File destination) throws IOException {

        FileChannel in = new FileInputStream(source).getChannel();
        FileChannel out = new FileOutputStream(destination).getChannel();

        try {
            in.transferTo(0, in.size(), out);
        } catch(Exception e){
            Log.d("Exception", e.toString());
        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }
    public  boolean isInternetPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.INTERNET)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"INTERNET Permission is granted");
                return true;
            } else {

                Log.d(TAG,"INTERNET Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
                return false;
            }
        }
        else {
            Log.d(TAG,"INTERNET Permission is granted");
            return true;
        }
    }


}