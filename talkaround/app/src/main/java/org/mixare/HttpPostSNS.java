package org.mixare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.mixare.reality.PhysicalPlace;

import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

/**
 * Created by siosi on 2016-06-03.
 */
public class HttpPostSNS extends Activity implements OnClickListener{


    TextView tvIsConnected;
    EditText etName,etMessage;
    Button btnPost;

    Person person;
    public static PhysicalPlace curlocate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        curlocate = (PhysicalPlace)intent.getSerializableExtra("currentGPSInfo2");

        // get reference to the views
        // tvIsConnected = (TextView) findViewById(R.id.tvIsConnected);
        etName = (EditText) findViewById(R.id.etName);
        etMessage = (EditText) findViewById(R.id.etMessage);
        btnPost = (Button) findViewById(R.id.btnPost);

        // check if you are connected or not
        //  if(isConnected()){
        //      tvIsConnected.setText("");
        //  }
        //  else{
        //      tvIsConnected.setBackgroundColor(0xFFCCCC00);
        //      tvIsConnected.setText("인터넷 연결을 확인해주세요");
        //  }

        // add click listener to Button "POST"
        btnPost.setOnClickListener(this);

    }

    public static String POST(String url, Person person){
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();


            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);

            String json = "";

            // 3. build jsonObject
            JSONObject jsonObject = new JSONObject();

            String userName = person.getName();
            String message = person.getMessage();
            Double curlon = curlocate.getLongitude();
            Double curlan = curlocate.getLatitude();

            Log.i("1",userName);
            Log.i("2",message);
            Log.i("3",curlon.toString());
            Log.i("4",curlan.toString());

            // StringEntity params = new StringEntity()
            jsonObject.put("longitude",Double.toString(curlon));
            jsonObject.put("latitude",Double.toString(curlan));
            jsonObject.put("name", userName);
            jsonObject.put("message", message);

            // 4. convert JSONObject to JSON to String
            json = jsonObject.toString();

            Log.i("5",json.toString());

            // 5. set json to StringEntity
            StringEntity se = new StringEntity(json,"utf-8");

            Log.i("6",se.toString());
            // 6. set httpPost Entity
            httpPost.setEntity(se);

            // 7. Set some headers to inform server about the type of the content
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        return result;
    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
    @Override
    public void onClick(View view) {

        switch(view.getId()){
            case R.id.btnPost:
                if(!validate())
                    Toast.makeText(getBaseContext(), "Enter some data!", Toast.LENGTH_LONG).show();
                // call AsynTask to perform network operation on separate thread
                person = new Person();
                person.setName(etName.getText().toString());
                person.setMessage(etMessage.getText().toString());
                new HttpAsyncTask().execute("http://lab.khlug.org/manapie/javap/postMessage.php");

                break;
        }

    }
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return POST(urls[0],person);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Data Sent!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean validate(){
        if(etName.getText().toString().trim().equals(""))
            return false;
        else if(etMessage.getText().toString().trim().equals(""))
            return false;
        else
            return true;
    }
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }


}
