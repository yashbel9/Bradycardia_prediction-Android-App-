package com.example.android.group_23;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import static libsvm.svm_parameter.PRECOMPUTED;
//SVM prediction Class
public class SvmPredict extends AppCompatActivity {
    Boolean activityFlag=false;
    private svm_parameter params;
    private int cross_validation;
    private int nr_fold;
    private svm_problem prob;
    private double accuracy=0;


    public String predictAction(double[] var){
        setParameters();
        svm_node[] nodes = new svm_node[var.length];
        for (int i = 0; i < var.length; i++)
        {
            svm_node node = new svm_node();
            node.index = i;
            node.value = var[i];
            nodes[i] = node;
        }

        try {
            svm_model model = get_data();
            int totalClasses = 2;
            int[] labels = new int[totalClasses];
            svm.svm_get_labels(model, labels);

            double[] prob_estimates = new double[totalClasses];
            double v = svm.svm_predict_probability(model, nodes, prob_estimates);

            for (int i = 0; i < totalClasses; i++){
                System.out.println("(" + labels[i] + ":" + prob_estimates[i] + ")");
            }

            System.out.println("Value"+v);

            String result;
            if(v==1)
                result = "Bradycardia Predicted";
            else
                result = "No Bradycardia Predicted";


            System.out.print("PREDICTION:"+result);
            return result;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void setParameters(){
        params = new svm_parameter();
        params.svm_type = svm_parameter.C_SVC;
        params.kernel_type = svm_parameter.RBF;
        params.degree = 3;
        params.gamma = 0.007;
        params.coef0 = 0;
        params.nu = 0.5;
        params.cache_size = 100;
        params.C = 1000;
        params.eps = 1e-7;
        params.p = 0.1;
        params.shrinking = 1;
        params.probability = 0;
        params.nr_weight = 0;
        params.weight_label = new int[0];
        params.weight = new double[0];
        cross_validation = 1;
        nr_fold = 3;
    }

    private static double atod(String s)
    {
        double d = Double.parseDouble(s);
        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            System.err.print("NaN or Infinity in input\n");
            System.exit(1);
        }
        return(d);
    }
//Used to train the model
    public svm_model get_data() throws IOException {

        prob = new svm_problem();
        prob.l=0;
        Vector<Double> labels = new Vector<Double>();
        Vector<svm_node[]> features = new Vector<svm_node[]>();
        int max_index = 0;
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "all_data.txt";
        File file = new File(fileName.toString());
        FileInputStream is = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        while(true)
        {
            String line = br.readLine();
            if(line == null) break;
            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
            labels.addElement(atod(st.nextToken()));
            prob.l++;
            int m = st.countTokens()/2;
            svm_node[] feature = new svm_node[m];
            for(int j=0;j<m;j++)
            {
                feature[j] = new svm_node();
                feature[j].index = Integer.parseInt(st.nextToken());
                feature[j].value = atod(st.nextToken());
            }
            if(m>0) max_index = Math.max(max_index, feature[m-1].index);
            features.addElement(feature);
        }

        prob.x = new svm_node[prob.l][];
        for(int i=0;i<prob.l;i++)
            prob.x[i] = features.elementAt(i);
        prob.y = new double[prob.l];
        for(int i=0;i<prob.l;i++)
            prob.y[i] = labels.elementAt(i);

        if(params.gamma == 0 && max_index > 0)
            params.gamma = 1.0/max_index;

        if(params.kernel_type == PRECOMPUTED)
            for(int i=0;i<prob.l;i++)
            {
                if (prob.x[i][0].index != 0)
                {
                    System.err.print("Wrong input format: first column must be 0:sample_serial_number\n");
                    System.exit(1);
                }
                if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > max_index)
                {
                    System.err.print("Wrong input format: sample_serial_number out of range\n");
                    System.exit(1);
                }
            }
        svm_model model1 = svm.svm_train(prob,params);
        br.close();
        return model1;
    }
//Used for calculating accuracy
    void cross_validation()
    {
        int i;
        int total_correct = 0;
        double total_error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[prob.l];
        svm.svm_cross_validation(prob,params,nr_fold,target);
        if(params.svm_type == svm_parameter.EPSILON_SVR ||
                params.svm_type == svm_parameter.NU_SVR)
        {
            for(i=0;i<prob.l;i++)
            {
                double y = prob.y[i];
                double v = target[i];
                total_error += (v-y)*(v-y);
                sumv += v;
                sumy += y;
                sumvv += v*v;
                sumyy += y*y;
                sumvy += v*y;
            }
            System.out.print("Cross Validation Mean squared error = "+total_error/prob.l+"\n");
            System.out.print("Cross Validation Squared correlation coefficient = "+
                    ((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
                            ((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy))
            );
        }
        else {
            for (i = 0; i < prob.l; i++)
                if (target[i] == prob.y[i])
                    ++total_correct;
            accuracy = 100.0 * total_correct / prob.l;
            Toast.makeText(getBaseContext(), "Cross Validation Accuracy = " + accuracy, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parameters);
        TextView param = (TextView) findViewById(R.id.parameters);

        try{
            setParameters();
            get_data();
            String error_msg = svm.svm_check_parameter(prob, params);
            if (error_msg != null) {
                Toast.makeText(getBaseContext(), error_msg, Toast.LENGTH_LONG).show();
            }
            cross_validation();
            param.setText("Accuracy" + "=" + accuracy+ " ");


        }catch (Exception e){
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}