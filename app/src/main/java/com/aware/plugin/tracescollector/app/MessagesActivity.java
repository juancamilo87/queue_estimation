package com.aware.plugin.tracescollector.app;

import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.plugin.tracescollector.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by researcher on 22/06/15.
 */
public class MessagesActivity extends Activity {

    private static String getURL = "http://pan0166.panoulu.net/queue_estimation/get_messages.php";
    private static String postURL = "http://pan0166.panoulu.net/queue_estimation/post_message.php";

    private MessagesAdapter adapter;
    private List<Message> messages;

    private EditText edt_alias;
    private EditText edt_duration;
    private EditText edt_message;

    private ProgressBar progressBar;
    private TextView tv_no_messages;

    private String alias;
    private Float duration;
    private String message;

    private String venue_id;

    private boolean messageSent;
    private boolean gettingMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages_activity);
        messageSent = false;
        gettingMessages = false;
        messages = new ArrayList<>();

        RecyclerView recList = (RecyclerView) findViewById(R.id.messages_list);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);

        adapter = new MessagesAdapter(messages);
        recList.setAdapter(adapter);

        edt_alias = (EditText) findViewById(R.id.messages_alias);
        edt_duration = (EditText) findViewById(R.id.messages_duration);
        edt_message = (EditText) findViewById(R.id.messages_message);
        progressBar = (ProgressBar) findViewById(R.id.messages_empty);
        tv_no_messages = (TextView) findViewById(R.id.messages_no_messages);

        Button post_btn = (Button) findViewById(R.id.messages_post_btn);

        post_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(verifyFields())
                {
                    postMessage();
                }
            }
        });

        venue_id = getIntent().getStringExtra("venue_id");

        if(getIntent().getBooleanExtra("message_posted",false))
        {
            findViewById(R.id.messages_bottom_container).setVisibility(View.GONE);
        }
        getMessages();
    }

    private boolean verifyFields()
    {
        if(!edt_message.getText().toString().trim().equals(""))
        {
            if(edt_message.getText().toString().trim().length()>=3)
            {
                alias = edt_alias.getText().toString().trim().equals("") ? null : edt_alias.getText().toString().trim();
                try{
                    duration = edt_duration.getText().toString().trim().equals("") ? -1 : Float.parseFloat(edt_duration.getText().toString().trim());
                }
                catch (Exception e)
                {
                    Toast.makeText(this, "Error on the duration",Toast.LENGTH_SHORT).show();
                    return false;
                }
                message = edt_message.getText().toString().trim();
                return true;
            }
            else
            {
                Toast.makeText(this, "Please input a message with at least 3 characters",Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        else
        {
            Toast.makeText(this, "Please input a message",Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void getMessages()
    {
        gettingMessages = true;
        refreshProgressBar();
        new GetMessagesTask().execute(venue_id);
    }

    private void postMessage()
    {
        edt_duration.setEnabled(false);
        edt_message.setEnabled(false);
        edt_alias.setEnabled(false);
        new PostMessagesTask(alias,duration,message, venue_id).execute();
    }

    private void messagePosted()
    {
        messageSent = true;
        findViewById(R.id.messages_bottom_container).setVisibility(View.GONE);
        Toast.makeText(this, "Message posted! :)", Toast.LENGTH_SHORT).show();
        getMessages();
    }

    private void errorPostingMessage()
    {
        edt_duration.setEnabled(true);
        edt_message.setEnabled(true);
        edt_alias.setEnabled(true);
        Toast.makeText(this, "Error posting message. Please try again...",Toast.LENGTH_SHORT).show();
    }

    private class GetMessagesTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {
            String jsonCategories = null;

            HttpClient client = new DefaultHttpClient();
            String URL = getURL + "?venue_id="+params[0];

            try
            {
                // Create Request to server and get response
                StringBuilder builder = new StringBuilder();
                HttpGet httpGet = new HttpGet(URL);

                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    Log.v("Getter", "Your data: " + builder.toString());
                    jsonCategories = builder.toString();
                } else {
                    Log.e("Getter", "Failed to download file");
                }
            }
            catch(Exception ex)
            {
                Log.e("Getter", "Failed"); //response data
            }


            return jsonCategories;
        }

        protected void onPostExecute(String result) {
            try
            {
                result = "{array:"+result+"}";
                Log.d("Messages",result);
                JSONObject jObject = new JSONObject(result);
                JSONArray jArray = jObject.getJSONArray("array");

                messages.clear();

                for (int i=0; i < jArray.length(); i++)
                {
                    JSONObject oneMessage = jArray.getJSONObject(i);
                    // Pulling items from the array
                    String aliasString = oneMessage.getString("alias");
                    String messageString = oneMessage.getString("message");
                    long timestamp = Long.parseLong(oneMessage.getString("timestamp"));
                    Message theMessage = new Message(aliasString, messageString, timestamp);
                    messages.add(theMessage);
                }
                adapter.notifyDataSetChanged();

            }
            catch(Exception e)
            {
                Log.e("JSON", "Failed"); //response data
            }
            gettingMessages = false;
            refreshProgressBar();
        }
    }

    private class PostMessagesTask extends AsyncTask<Void, Void, Boolean> {

        private String alias;
        private float duration;
        private String message;
        private String venue_id;

        public PostMessagesTask(String alias, Float duration, String message, String venue_id)
        {
            this.alias = alias;
            this.duration = duration;
            this.message = message;
            this.venue_id = venue_id;
        }

        protected Boolean doInBackground(Void... params) {

            JSONObject json = new JSONObject();
            try
            {
                json.put("username",this.alias);
                json.put("message",this.message);
                json.put("duration",this.duration);
                json.put("venue_id",this.venue_id);

                Log.d("JSON Order",json.toString());
            }
            catch(Exception e)
            {
                Log.e("Error creating JSON", "JSON could not be created");
                Log.e("Error message",e.getMessage());
            }

            String URL = postURL;
            HttpClient client = new DefaultHttpClient();
            HttpPost request = new HttpPost(URL);

            try
            {
                AbstractHttpEntity entity = new ByteArrayEntity(json.toString().getBytes("UTF8"));
                entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                request.setEntity(entity);
                HttpResponse response = client.execute(request);

                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    return true;

                }
                else
                {
                    return false;

                }


            }
            catch(Exception e)
            {
                Log.e("Error posting message", "HttpPost failed");
                Log.e("Error message",e.getMessage());
            }
            return false;
        }

        protected void onPostExecute(Boolean result) {
            refreshProgressBar();
            if(result)
            {
                messagePosted();
            }
            else
            {
                errorPostingMessage();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(messageSent)
            setResult(RESULT_OK);
        else
            setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onResume() {
        refreshProgressBar();
        super.onResume();
    }

    private void refreshProgressBar()
    {
        if(gettingMessages)
        {
            progressBar.setVisibility(View.VISIBLE);
            tv_no_messages.setVisibility(View.GONE);
        }
        else
        {
            progressBar.setVisibility(View.GONE);
            if(messages==null || messages.size()==0)
            {
                tv_no_messages.setVisibility(View.VISIBLE);
            }
            else
            {
                tv_no_messages.setVisibility(View.GONE);
            }
        }

    }
}
