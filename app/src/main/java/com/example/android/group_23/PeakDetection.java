package com.example.android.group_23;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.*;
import java.io.*;

public class PeakDetection{
    private static final String TAG = PeakDetection.class.getSimpleName();
	private static int progressStatus = 0;
    private static ServiceCallbacks serviceCallbacks;


    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

	public Double [][] returnData (FileInputStream fin){

		Double[][] data=new Double[230402][2];
		try{
			DataInputStream myInput = new DataInputStream(fin);
			String thisLine;
			int row=1;
			while ((thisLine = myInput.readLine()) != null) {  // while loop begins here
				//System.out.println(thisLine);
                /*if (serviceCallbacks != null) {
                    serviceCallbacks.addEntry(progressStatus+1);
                }*/
                /*new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (serviceCallbacks != null) {
                            serviceCallbacks.addEntry(progressStatus+1);
                        }
                    }
                }).start();*/
//                handler.post(updateRunnable);

				String[] arr=thisLine.split("\t");
				data[row][0]=Double.parseDouble(arr[0]);
				data[row][1]=Double.parseDouble(arr[1]);
				row=row+1;
			}

		} catch( IOException io){
			System.out.println("IOException");
		}
		return data;

	}

    public double[] calculateHeartRate(int[] peak_indices)
    {
        double[] heartRates = new double[peak_indices.length-2];

        for(int i=0;i<peak_indices.length-2;i++)
        {
            heartRates[i] = (double)(60*250)/(peak_indices[i+2] - peak_indices[i]);
        }
        return heartRates;
    }

	public int[] RpeakDetection(double[] data){


		//System.out.println("Data"+data.length);

		double LPFECG[] = new double[data.length];

		for(int i=13;i<=45;i++){
			int index = i - 12;
			if(index < 3)
				LPFECG[index] = 0.5*(data[i]- 2*data[i - 6] + data[i - 12]);
			else
				LPFECG[index] = 0.5*(2*LPFECG[index - 1] - LPFECG[index - 2] + data[i] - 2*data[i - 6] + data[i - 12]);
		}

		/*System.out.println("len "+LPFECG.length);
		for(int z = 0; z<33; z++){
			System.out.println(z+" "+LPFECG[z]);
		}*/

		System.out.println("LPFECG done");

		double HPFECG[] = new double[data.length];

		for(int i = 46;i<data.length;i++){
			/*progressStatus += 1;
			handler.post(new Runnable() {
				public void run() {
					progressBar.setProgress(progressStatus);
					textView.setText(progressStatus+"/"+progressBar.getMax());
				}
			});*/


            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    if (serviceCallbacks != null) {
                        serviceCallbacks.addEntry(progressStatus+1);
                    }
                }
            }).start();*/
            /*MainActivity mainActivity = new MainActivity();
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });*/
			int index = i - 12;
			int index2 = i - 45;
			LPFECG[index] = 0.5*(2*LPFECG[index - 1] - LPFECG[index - 2] + data[i] - 2*data[i - 6] + data[i - 12]);
			if(index2 < 2){
				HPFECG[index2] = (1/32.0)*(32*LPFECG[index - 16] + (LPFECG[index] - LPFECG[index - 32]));
			}
			else
				HPFECG[index2] = (1/32.0)*(32*LPFECG[index - 16] - (HPFECG[index2 - 1] + LPFECG[index] - LPFECG[index - 32]));
		}

		System.out.println("HPFECG done");
		int x_index_val = 0;
		for(int i=1;i<HPFECG.length;i++){
			if(HPFECG[i]==0){
				x_index_val = i;
				break;
			}
		}
		//System.out.println("X_index"+ x_index_val);

		//double x[] = -HPFECG; //Filtered ECG
		//double x[] = new double[data.length];
		//double x[] = new double[230356];
		double x[] = new double[x_index_val];

		for(int i=1;i<x_index_val;i++){
			x[i] = -1*HPFECG[i];
		}

		/*for(int i = 1;i<=100;i++){
			//System.out.println(i+": "+x[i]);
		}*/
		System.out.println("copy x done");

		double BaseLine = mean(x);

		//System.out.println("mean: "+mean(x));
		//System.out.println("max: "+max(x));
		//System.out.println("min: "+min(x));

		double DynamicRangeUp = max(x) - BaseLine;
		double DynamicRangeDown = BaseLine - min(x);
		double thresholdUp = 0.002*DynamicRangeUp;
		double thresholdR = 0.5*DynamicRangeUp;
		double thresholdDown = 0.000002*DynamicRangeDown;
		double thresholdQ = 0.1*DynamicRangeDown;

		int up = 1;
		double PreviousPeak = x[1];
		int k = 0;
		double maximum = (double)-1000;
		double minimum = (double)1000;
		int possiblePeak = 0;
		int Rpeak = 0;
		List<Integer> Rpeak_index = new ArrayList<Integer>();
		//int Rpeak_index[] = new int[data.length];
		int Qpeak = 0;
		int Qpeak_index[] = new int[data.length];
		int Speak = 0;
		int Speak_index[] = new int[data.length];
		int PeakType = 0;
		int i = 1;

		//System.out.println(maximum);
		/*System.out.println("BaseLine: "+BaseLine);
		System.out.println("DynamicRangeUp: "+DynamicRangeUp);
		System.out.println("DynamicRangeDown: "+DynamicRangeDown);
		System.out.println("thresholdUp: "+thresholdUp);
		System.out.println("thresholdR: "+thresholdR);
		System.out.println("thresholdDown: "+thresholdDown);
		System.out.println("thresholdQ: "+thresholdQ);	*/


		//System.out.println("x end: "+x[230356]);
		/*System.out.println("x end: "+x[230355]);
		System.out.println("x end: "+x[230354]);
		System.out.println("x end: "+x[230353]);*/
		//System.out.println("x len: "+x.length);


		int peak_index[] = new int[data.length];


		while(i < x.length){
            /*if (serviceCallbacks != null) {
                serviceCallbacks.addEntry(progressStatus+1);
            }*/
			if(x[i] > maximum)
				maximum = x[i];

			if(x[i] < minimum)
				minimum = x[i];

			if(up == 1){
				if(x[i] < maximum){
					if(possiblePeak == 0){
						possiblePeak = i;
					}


					if(x[i] < (maximum-thresholdUp)){
						k = k + 1;
						peak_index[k] = possiblePeak - 1;
						minimum  = x[i];
						up = 0;
						possiblePeak = 0;

						if(PeakType == 0){
							if(x[peak_index[k]]>(BaseLine+thresholdR)){

								Rpeak = Rpeak + 1;
								Rpeak_index.add(peak_index[k]);
								//Rpeak_index = [Rpeak_index peak_index(k)];
								PreviousPeak = x[peak_index[k]];
								//System.out.println("case 1");

							}
						}
						else{
							if((Math.abs((x[peak_index[k]] - PreviousPeak) / PreviousPeak) > 1.5) && (x[peak_index[k]] > BaseLine +thresholdR)){
								Rpeak = Rpeak + 1;
								Rpeak_index.add(peak_index[k]);
								//Rpeak_index = [Rpeak_index peak_index(k)];
								PreviousPeak = x[peak_index[k]];
								PeakType = 2;
								//System.out.println("case 2");
							}
						}
					}
				}
			}
			else{
				if(x[i] > minimum){
					if(possiblePeak == 0)
						possiblePeak = i;

					if(x[i] > (minimum + thresholdDown)){
						k = k + 1;
						peak_index[k] = possiblePeak-1;
						maximum = x[i];

						up = 1;
						possiblePeak = 0;
					}
				}
			}//else


			i=i+1;
		}

		//int[] Rpeakarray = new int[Rpeak_index.size()];
		//Integer[] Rpeakarray = Rpeak_index.toArray(new Integer[Rpeak_index.size()]);

		//System.out.println(Rpeak_index.size());
		/*
		for(int j = 0; j<5; j++){
			System.out.println(Rpeak_index.get(j));
		}*/

		int[] Rpeakarray = toIntArray(Rpeak_index);
		return Rpeakarray;
	}

	public static int[] toIntArray(List<Integer> list){
		int[] ret = new int[list.size()];
		for(int i = 0;i < ret.length;i++)
			ret[i] = list.get(i);
		return ret;
	}

	public static double mean(double[] m) {
		double sum = 0;
		for (int i = 0; i < m.length; i++) {
			sum += m[i];
		}
		return sum / m.length;
	}

	public static double max(double[] m) {

		double max = -1*Double.MAX_VALUE;
		for(int i = 0; i < m.length; i++) {
			if(m[i] > max) {
				max = m[i];
			}
		}
		return max;
	}

	public static double min(double[] m){
		double min = Double.MIN_VALUE;
		for(int i = 0; i < m.length; i++) {
			if(m[i] < min) {
				min = m[i];
			}
		}
		return min;
	}
	public static void updateProgressBar()
    {
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {

                });

            }
        });*/
    }
}
