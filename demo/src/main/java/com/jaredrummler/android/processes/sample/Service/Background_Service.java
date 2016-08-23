package com.jaredrummler.android.processes.sample.Service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.jaredrummler.android.processes.models.Stat;
import com.jaredrummler.android.processes.sample.utils.AndroidAppProcessLoader;
import com.jaredrummler.android.processes.sample.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public class Background_Service extends Service  implements Runnable, AndroidAppProcessLoader.Listener{

    private int mStartId;
    private Handler mHandler;
    private boolean mRunning;
    private static final int TIMER_PERIOD = 10*1000;//10초

    public Background_Service() {
    }


    @Override
    public void onCreate(){
        Log.d("service", "service start");
        super.onCreate();
        mHandler = new Handler();
        mRunning = false;

    }

    @Override
    public void onStart(Intent intent, int startId){
        Log.d("service", "service ID : " + startId);
        super.onStart(intent, startId);
        mStartId = startId;
        new AndroidAppProcessLoader(getApplicationContext(), this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if(!mRunning) {
            mHandler.postDelayed(this, TIMER_PERIOD);
            mRunning = true;
        }

    }

    public void onDestory(){
        mRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



    @Override
    public void run() {

        if(!mRunning){
            Log.d("service", "run after destory");
            return;
        }else{
            Log.d("Sinyoung", "10초");
            new AndroidAppProcessLoader(getApplicationContext(), this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            mHandler.postDelayed(this, TIMER_PERIOD);
        }

    }



    private void insert(String ProcessName, String UTime, String KTime){

        class InsertData extends AsyncTask<String, Void, String> {
            //ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                //loading.dismiss();
                //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }

            @Override
            protected String doInBackground(String... params) {

                try{

                    String ProcessName = (String)params[0];
                    String Utime = (String)params[1];
                    String Ktime = (String) params[2];

                    String link = "";
                    String data = "";
                    link = "http://192.168.1.34:80/Pdata.php";
                    data = URLEncoder.encode("pname", "UTF-8") + "=" + URLEncoder.encode(ProcessName, "UTF-8");
                    data += "&" + URLEncoder.encode("utime", "UTF-8") + "=" + URLEncoder.encode(Utime, "UTF-8");
                    data += "&" + URLEncoder.encode("ktime", "UTF-8") + "=" + URLEncoder.encode(Ktime, "UTF-8");

                    URL url = new URL(link);
                    URLConnection conn = url.openConnection();

                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                    wr.write(data);
                    wr.flush();

                   // Log.d("Sinyoung", "썼습니다");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    StringBuilder sb = new StringBuilder();
                    String line = null;

                    // Read Server Response
                    while((line = reader.readLine()) != null)
                    {
                        sb.append(line);
                        break;
                    }
                    return sb.toString();
                }
                catch(Exception e){
                    return new String("Exception: " + e.getMessage());
                }

            }
        }

        InsertData task = new InsertData();
        task.execute(ProcessName, UTime, KTime);
    }

    @Override
    public void onComplete(List<AndroidAppProcess> processes) {


        Log.d("Sinyoung", "insert 합니다");
        for(int i = 0; i<processes.size(); i++){

            Stat stat = null;
            try {
                stat = processes.get(i).stat();
            } catch (IOException e) {
                e.printStackTrace();
            }
            long userModeTicks = stat.utime();
            long kernelModeTicks = stat.stime();

            insert(Utils.getName(getApplicationContext(), processes.get(i)), String.valueOf(userModeTicks), String.valueOf(kernelModeTicks));

        }

    }
}
