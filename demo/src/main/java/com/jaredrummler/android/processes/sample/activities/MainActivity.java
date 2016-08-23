/*
 * Copyright (C) 2015. Jared Rummler <jared.rummler@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jaredrummler.android.processes.sample.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.jaredrummler.android.processes.models.Stat;
import com.jaredrummler.android.processes.sample.R;
import com.jaredrummler.android.processes.sample.Service.Background_Service;
import com.jaredrummler.android.processes.sample.adapter.ProcessListAdapter;
import com.jaredrummler.android.processes.sample.utils.AndroidAppProcessLoader;
import com.jaredrummler.android.processes.sample.utils.HtmlBuilder;
import com.jaredrummler.android.processes.sample.utils.UsageApplication;
import com.jaredrummler.android.processes.sample.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

public class MainActivity extends Activity implements AndroidAppProcessLoader.Listener {

  private double totalTime=0;
  private ActivityManager managerActivity;
  private ListView listview;
  private ProcessListAdapter adapter;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    listview = (ListView)findViewById(R.id.listview);

    listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    listview.setFastScrollEnabled(true);
    new AndroidAppProcessLoader(this, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    Intent intent = new Intent(getApplicationContext(), Background_Service.class);
    startService(intent);
    Toast.makeText(MainActivity.this, "start service", Toast.LENGTH_SHORT).show();


  }


  private void insert(String pName, String uTime, String kTime){

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
    task.execute(pName, uTime, kTime);

  }



  @Override
  public void onComplete(List<AndroidAppProcess> processes) {

    adapter = new ProcessListAdapter(MainActivity.this, processes, getFragmentManager());

    listview.setAdapter(adapter);
    //setListAdapter(new ProcessListAdapter(getActivity(), processes));
    String log="";

    for(int i=0; i<listview.getCount(); i++){
      log="";
      log = processes.get(i).name+"\t";
      try {
        Stat stat = processes.get(i).stat();

        long userModeTicks = stat.utime();
        long kernelModeTicks = stat.stime();

        log += stat.utime()+"\t";
        log += stat.stime();

        totalTime+= stat.utime();
        Log.d("sinyoung", log);

        insert(Utils.getName(getApplicationContext(), processes.get(i)), String.valueOf(userModeTicks), String.valueOf(kernelModeTicks));


      } catch (IOException e) {
        e.printStackTrace();
      }
    }


    totalTime = totalTime/100;
    Log.d("sinyoung_total", UsageApplication.getFormattedUsageTime(totalTime));

  }


}
