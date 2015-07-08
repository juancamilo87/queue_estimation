package com.aware.plugin.tracescollector.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by researcher on 22/06/15.
 */
public class MessagesActivity extends Activity {

    public static String getMessagesURL = "http://pan0166.panoulu.net/queue_estimation/get_messages.php";
    private static String postURL = "http://pan0166.panoulu.net/queue_estimation/post_message.php";

    private float y1,y2;
    static final int MIN_DISTANCE = 200;

    private MessagesAdapter adapter;
    private List<Message> messages;

    private EditText edt_alias;
    private EditText edt_duration;
    private EditText edt_message;

    private ProgressBar progressBar;
    private TextView tv_no_messages;

    private String alias;
    private Integer duration;
    private String message;

    private String venue_id;

    private boolean messageSent;
    private boolean gettingMessages;
    private boolean bottom_shown;
    private Toast toast;

    private LinearLayout bottomContainer;
    private LinearLayout bottomTitle;
    private LinearLayout bottomRest;

    private ImageButton show_hide_btn;

    private float moveY;

    private RecyclerView recList;

    private Context context;

    private GestureDetector gestureDetector;

    private SharedPreferences prefs;

    private ImageButton post_btn;

    private LinearLayoutManager llm;

    private TextView tv_char_count;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages_activity);
        context = this;
        messageSent = false;
        gettingMessages = false;
        messages = new ArrayList<>();
        bottom_shown = true;


        recList = (RecyclerView) findViewById(R.id.messages_list);
        recList.setHasFixedSize(true);
        llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
//        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);
        recList.setLayoutManager(llm);
        adapter = new MessagesAdapter(messages);
        recList.setAdapter(adapter);
        gestureDetector = new GestureDetector(this, new SingleTapConfirm());

        edt_alias = (EditText) findViewById(R.id.messages_alias);
        edt_duration = (EditText) findViewById(R.id.messages_duration);
        edt_message = (EditText) findViewById(R.id.messages_message);
        progressBar = (ProgressBar) findViewById(R.id.messages_empty);
        tv_no_messages = (TextView) findViewById(R.id.messages_no_messages);
        bottomContainer = (LinearLayout) findViewById(R.id.messages_bottom_container);
        bottomTitle = (LinearLayout) findViewById(R.id.messages_title_bar);
        bottomRest = (LinearLayout) findViewById(R.id.messages_bottom_rest);
        tv_char_count = (TextView) findViewById(R.id.message_char_count);

        edt_message.setHorizontallyScrolling(false);
        edt_message.setMaxLines(3);

        edt_message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length()>0)
                {
                    int value = getResources().getInteger(R.integer.messages_max_chars_message);
                    value = value - s.toString().length();
                    tv_char_count.setText(value+" chars left");
                    tv_char_count.setVisibility(View.VISIBLE);
                }
                else
                {
                    tv_char_count.setVisibility(View.GONE);
                }
            }
        });

        post_btn = (ImageButton) findViewById(R.id.messages_post_btn);

        post_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (verifyFields(false)) {
                    postMessage();
                }
            }
        });

        moveY = bottomContainer.getHeight()-bottomTitle.getHeight();
        Log.d("moveY", moveY + "");
        show_hide_btn = (ImageButton) findViewById(R.id.messages_hide_show_btn);

        show_hide_btn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (gestureDetector.onTouchEvent(event)) {
                    Log.d("swipe", "tap");
                    toggleView();
                    return true;
                } else {
                    switch(event.getAction())
                    {
                        case MotionEvent.ACTION_DOWN:
                            y1 = event.getY();
                            break;
                        case MotionEvent.ACTION_UP:
                            y2 = event.getY();
                            float deltaY = y2 - y1;
                            if (Math.abs(deltaY) > MIN_DISTANCE)
                            {
                                if(y2 > y1)
                                {
                                    if(bottom_shown)
                                    {
                                        hideView();
                                        //Toast.makeText(context, "Up to down swipe", Toast.LENGTH_SHORT).show ();
                                        Log.d("swipe", "down");
                                    }
                                }
                                else
                                {
                                    if(!bottom_shown)
                                    {
                                        showView();
                                        //Toast.makeText(context, "Down to up swipe", Toast.LENGTH_SHORT).show ();
                                        Log.d("swipe", "up");
                                    }
                                }
                            }
                            else
                            {
                                Log.d("swipe", "none");
                                return false;
                            }
                    }
                }
                return true;
            }
        });

        bottomTitle.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        y1 = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        y2 = event.getY();
                        float deltaY = y2 - y1;
                        if (Math.abs(deltaY) > MIN_DISTANCE)
                        {
                            if(y2 > y1)
                            {
                                if(bottom_shown)
                                {
                                    hideView();
                                    //Toast.makeText(context, "Up to down swipe", Toast.LENGTH_SHORT).show ();
                                    Log.d("swipe", "down");
                                }
                            }
                            else
                            {
                                if(!bottom_shown)
                                {
                                    showView();
                                    //Toast.makeText(context, "Down to up swipe", Toast.LENGTH_SHORT).show ();
                                    Log.d("swipe", "up");
                                }
                            }

                        }
                        else
                        {
                            Log.d("swipe", "none");
                            return true;
                        }
                }
                return false;
            }
        });

        venue_id = getIntent().getStringExtra("venue_id");

        if(getIntent().getBooleanExtra("message_posted",false))
        {
            bottomContainer.setVisibility(View.GONE);
            bottom_shown = false;
            show_hide_btn.setImageResource(R.drawable.ic_expand_less_black_48dp);
        }
        getMessages();

        prefs = getSharedPreferences("com.aware.plugin.tracescollector.messages",MODE_PRIVATE);

        if(!prefs.getString("alias","").equals(""))
        {
            edt_alias.setText(prefs.getString("alias",""));
        }

        if(!prefs.getString("duration","").equals(""))
        {
            edt_duration.setText(prefs.getString("duration",""));
        }

        ((ImageButton) findViewById(R.id.messages_info_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle("Wall Information")
                        .setMessage("In this wall you can leave messages for other users in the queue right now or in the future.\n\n" +
                                "The duration the message will be visible for everyone in this queue can be set, so messages can seem to" +
                                " disappear randomly.")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(R.drawable.ic_chat_black_48dp)
                        .show();
            }
        });

    }

    private boolean verifyFields(boolean fromKeyboard)
    {
        if (toast == null) {
            toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        if(fromKeyboard)
        {
            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 100);
        }
        else
        {
            toast.setGravity(Gravity.CENTER, 0, 0);
        }
        //toast.cancel();
        if(!edt_message.getText().toString().trim().equals(""))
        {
            if(edt_message.getText().toString().trim().length()>=3)
            {
                alias = edt_alias.getText().toString().trim().equals("") ? null : edt_alias.getText().toString().trim();
                try{
                    duration = edt_duration.getText().toString().trim().equals("") ? null : Integer.parseInt(edt_duration.getText().toString().trim());
                }
                catch (Exception e)
                {
                    toast.setText("Please enter a valid duration");
                    toast.show();
                    return false;
                }
                message = edt_message.getText().toString().trim();
                toast.cancel();
                return true;
            }
            else
            {
                toast.setText("Please input a message with at least 3 characters");
                toast.show();
                return false;
            }
        }
        else
        {
            toast.setText("Please input a message");
            toast.show();
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
        post_btn.setEnabled(false);
        new PostMessagesTask(alias, duration, message, venue_id).execute();
    }

    private void messagePosted()
    {
        //messageSent = true;
        //findViewById(R.id.messages_bottom_container).setVisibility(View.GONE);
        hideView();
        edt_duration.setEnabled(true);
        edt_message.setEnabled(true);
        edt_alias.setEnabled(true);
        post_btn.setEnabled(true);
        edt_message.setText("");
        Toast.makeText(this, "Message posted! :)", Toast.LENGTH_SHORT).show();
        getMessages();
    }

    private void errorPostingMessage()
    {
        edt_duration.setEnabled(true);
        edt_message.setEnabled(true);
        edt_alias.setEnabled(true);
        post_btn.setEnabled(true);
        Toast.makeText(this, "Error posting message. Please try again...",Toast.LENGTH_SHORT).show();
    }

    private class GetMessagesTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {
            String jsonCategories = null;

            HttpClient client = new DefaultHttpClient();
            String URL = getMessagesURL + "?venue_id="+params[0];

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
                llm.scrollToPosition(adapter.getItemCount()-1);
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
        private Integer duration;
        private String message;
        private String venue_id;

        public PostMessagesTask(String alias, Integer duration, String message, String venue_id)
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
                json.put("username", this.alias);
                json.put("message", this.message);
                json.put("duration", this.duration);
                json.put("venue_id", this.venue_id);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("alias",this.alias);
                if(this.duration == null)
                {
                    editor.putString("duration","");
                }
                else
                {
                    editor.putString("duration",this.duration+"");
                }
                editor.commit();

                Log.d("JSON Order", json.toString());
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

    private void hideView()
    {
        if (bottom_shown) {

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                moveY = bottomRest.getHeight();
                //Log.d("moveY", moveY + "");
                //Prepare the View for the animation
                bottomContainer.setAlpha(1.0f);

                //Start the animation
                bottomContainer.animate()
                        .translationY(bottomRest.getHeight())
                        .alpha(1.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                bottomRest.setVisibility(View.GONE);
                                bottomContainer.setTranslationY(0);
                                show_hide_btn.setImageResource(R.drawable.ic_expand_less_black_48dp);
                            }
                        });
                bottomRest.animate()
                        .alpha(0.0f);

                recList.animate()
                        .translationY(bottomRest.getHeight())
                        .alpha(1.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                recList.setTranslationY(0);
                            }
                        });

            } else {
                bottomRest.setVisibility(View.GONE);
                show_hide_btn.setImageResource(R.drawable.ic_expand_less_black_48dp);
            }

            bottom_shown = !bottom_shown;
        }
    }

    private void showView()
    {
        if(!bottom_shown) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                //Prepare the View for the animation
                bottomContainer.setVisibility(View.VISIBLE);
                bottomContainer.setAlpha(1.0f);
                bottomRest.setVisibility(View.VISIBLE);
                bottomRest.setAlpha(0.0f);
                bottomContainer.setTranslationY(moveY);
                recList.setTranslationY(moveY);
                //bottomRest.setAlpha(0.0f);

                //Start the animation
                bottomContainer.animate()
                        .translationY(0)
                        .alpha(1.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                bottomContainer.setTranslationY(0);
                                show_hide_btn.setImageResource(R.drawable.ic_expand_more_black_48dp);
                            }
                        });
                bottomRest.animate()
                        .alpha(1.0f);
                recList.animate()
                        .translationY(0);
            } else {
                bottomRest.setVisibility(View.VISIBLE);
                show_hide_btn.setImageResource(R.drawable.ic_expand_more_black_48dp);
            }

            bottom_shown = !bottom_shown;
        }
    }

    private void toggleView()
    {
        if (bottom_shown) {

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                moveY = bottomRest.getHeight();
                //Log.d("moveY", moveY + "");
                //Prepare the View for the animation
                bottomContainer.setAlpha(1.0f);

                //Start the animation
                bottomContainer.animate()
                        .translationY(bottomRest.getHeight())
                        .alpha(1.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                bottomRest.setVisibility(View.GONE);
                                bottomContainer.setTranslationY(0);
                                show_hide_btn.setImageResource(R.drawable.ic_expand_less_black_48dp);
                            }
                        });
                bottomRest.animate()
                        .alpha(0.0f);
                recList.animate()
                        .translationY(bottomRest.getHeight())
                        .alpha(1.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                recList.setTranslationY(0);
                            }
                        });
            } else {
                bottomRest.setVisibility(View.GONE);
                show_hide_btn.setImageResource(R.drawable.ic_expand_less_black_48dp);
            }

        } else {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                //Prepare the View for the animation
                final int height = recList.getHeight();
                Log.d("height", height + "");
                final int small_height = recList.getHeight()-(int)moveY;
                Log.d("small_height", small_height + "");
                bottomContainer.setVisibility(View.VISIBLE);
                bottomContainer.setAlpha(1.0f);
                bottomRest.setVisibility(View.VISIBLE);
                bottomRest.setAlpha(0.0f);
                bottomContainer.setTranslationY(moveY);
                recList.setTranslationY(moveY);

                //bottomRest.setAlpha(0.0f);
                Log.d("newHeight", recList.getHeight() + "");
                //Start the animation
                bottomContainer.animate()
                        .translationY(0)
                        .alpha(1.0f)
                        .setListener(new AnimatorListenerAdapter() {

                            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                bottomContainer.setTranslationY(0);
                                show_hide_btn.setImageResource(R.drawable.ic_expand_more_black_48dp);
                            }
                        });
                bottomRest.animate()
                        .alpha(1.0f);
                recList.animate()
                        .translationY(0)
                        .setListener(new AnimatorListenerAdapter() {

                            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                Log.d("newHeightB", recList.getHeight() + "");
                                recList.setTranslationY(0);
                                Log.d("newHeightB1", recList.getHeight() + "");
                            }
                        });
            } else {
                bottomRest.setVisibility(View.VISIBLE);
                show_hide_btn.setImageResource(R.drawable.ic_expand_more_black_48dp);
            }

        }
        bottom_shown = !bottom_shown;
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }
}
