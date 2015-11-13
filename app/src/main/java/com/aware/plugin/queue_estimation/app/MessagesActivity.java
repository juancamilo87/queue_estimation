package com.aware.plugin.queue_estimation.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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


import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.aware.plugin.queue_estimation.R;

/**
 * Created by researcher on 22/06/15.
 */
public class MessagesActivity extends Activity {

    public static String getMessagesURL = "http://pan0166.panoulu.net/queue_estimation/get_messages.php";
    private static String postURL = "http://pan0166.panoulu.net/queue_estimation/post_message.php";
    private static final String MESSAGES_CSV_FILE = "messages.csv";

    private float y1,y2;
    static final int MIN_DISTANCE = 200;

    private MessagesAdapter adapter;
    private List<Message> messages;

    private EditText edt_alias;
//    private EditText edt_duration;
    private EditText edt_message;

    private ProgressBar progressBar;
    private TextView tv_no_messages;

    private String alias;
//    private Integer duration;
    private final static int DURATION = 43200;
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

    private OkHttpClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new OkHttpClient();
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
//        edt_duration = (EditText) findViewById(R.id.messages_duration);
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
        Log.d(HomeScreen.LOG_TAG, moveY + "");
        show_hide_btn = (ImageButton) findViewById(R.id.messages_hide_show_btn);

        show_hide_btn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (gestureDetector.onTouchEvent(event)) {
                    Log.d(HomeScreen.LOG_TAG, "tap");
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
                                        Log.d(HomeScreen.LOG_TAG, "down");
                                    }
                                }
                                else
                                {
                                    if(!bottom_shown)
                                    {
                                        showView();
                                        //Toast.makeText(context, "Down to up swipe", Toast.LENGTH_SHORT).show ();
                                        Log.d(HomeScreen.LOG_TAG, "up");
                                    }
                                }
                            }
                            else
                            {
                                Log.d(HomeScreen.LOG_TAG, "none");
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
                                    Log.d(HomeScreen.LOG_TAG, "down");
                                }
                            }
                            else
                            {
                                if(!bottom_shown)
                                {
                                    showView();
                                    //Toast.makeText(context, "Down to up swipe", Toast.LENGTH_SHORT).show ();
                                    Log.d(HomeScreen.LOG_TAG, "up");
                                }
                            }

                        }
                        else
                        {
                            Log.d(HomeScreen.LOG_TAG, "none");
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

        prefs = getSharedPreferences("com.aware.plugin.queuetracescollector.messages",MODE_PRIVATE);

        if(!prefs.getString("alias","").equals(""))
        {
            edt_alias.setText(prefs.getString("alias",""));
        }

//        if(!prefs.getString("duration","").equals(""))
//        {
//            edt_duration.setText(prefs.getString("duration",""));
//        }

        ((ImageButton) findViewById(R.id.messages_info_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle("Wall Information")
                        .setMessage("In this wall you can leave messages for other users in the queue right now or in the future.\n\n" +
                                "The message will be visible for everyone in this queue for one month."/*can be set, so messages can seem to" +
                                " disappear randomly."*/)
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
//                try{
//                    duration = edt_duration.getText().toString().trim().equals("") ? null : Integer.parseInt(edt_duration.getText().toString().trim());
//                }
//                catch (Exception e)
//                {
//                    toast.setText("Please enter a valid duration");
//                    toast.show();
//                    return false;
//                }
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
        getMessagesRequest(venue_id);
//        new GetMessagesTask().execute(venue_id);
    }

    private void postMessage()
    {
//        edt_duration.setEnabled(false);
        edt_message.setEnabled(false);
        edt_alias.setEnabled(false);
        post_btn.setEnabled(false);
        postMessagesRequest(alias, DURATION, message, venue_id);
//        new PostMessagesTask(alias, DURATION, message, venue_id).execute();
    }

    private void messagePosted()
    {
        //messageSent = true;
        //findViewById(R.id.messages_bottom_container).setVisibility(View.GONE);
        hideView();
//        edt_duration.setEnabled(true);
        edt_message.setEnabled(true);
        edt_alias.setEnabled(true);
        post_btn.setEnabled(true);
        edt_message.setText("");
        Toast.makeText(this, "Message posted! :)", Toast.LENGTH_SHORT).show();
        getMessages();
    }

    private void errorPostingMessage()
    {
//        edt_duration.setEnabled(true);
        edt_message.setEnabled(true);
        edt_alias.setEnabled(true);
        post_btn.setEnabled(true);
        Toast.makeText(this, "Error posting message. Please try again...",Toast.LENGTH_SHORT).show();
    }

    private void getMessagesRequest(String... params)
    {


        String URL = getMessagesURL + "?venue_id="+params[0];

        try
        {
            // Create Request to server and get response
            Request request = new Request.Builder().url(URL).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e(HomeScreen.LOG_TAG, "Failed to download file");
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    String jsonCategories = null;
                    if (response.code() == 200) {
                        String response_body = response.body().string();
                        Log.v(HomeScreen.LOG_TAG, "Your data: " + response_body);
                        jsonCategories = response_body;
                    } else {
                        Log.e(HomeScreen.LOG_TAG, "Failed to download file");
                    }

                    try
                    {
                        jsonCategories = "{array:"+jsonCategories+"}";
                        Log.d(HomeScreen.LOG_TAG,jsonCategories);
                        JSONObject jObject = new JSONObject(jsonCategories);
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

                    }
                    catch(Exception e)
                    {
                        Log.e(HomeScreen.LOG_TAG, "Failed"); //response data
                    }

                    new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                            llm.scrollToPosition(adapter.getItemCount()-1);
                            gettingMessages = false;
                            refreshProgressBar();
                        }
                    });



                }
            });


        }
        catch(Exception ex)
        {
            Log.e(HomeScreen.LOG_TAG, "Failed"); //response data
        }



    }

    private void postMessagesRequest(String alias, Integer duration, String message, String venue_id)
    {

        HomeScreen.appendToCSV(MESSAGES_CSV_FILE,new String[]{alias, message, duration+"", venue_id});
        JSONObject json = new JSONObject();
        try
        {
            json.put("username", alias);
            json.put("message", message);
            json.put("duration", duration);
            json.put("venue_id", venue_id);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("alias",alias);
//                if(this.duration == null)
//                {
//                    editor.putString("duration","");
//                }
//                else
//                {
//                    editor.putString("duration",this.duration+"");
//                }
            editor.commit();

            Log.d(HomeScreen.LOG_TAG, json.toString());
        }
        catch(Exception e)
        {
            Log.e(HomeScreen.LOG_TAG, "JSON could not be created");
            Log.e(HomeScreen.LOG_TAG,e.getMessage());
        }

        String URL = postURL;
        try
        {
            RequestBody requestBody = RequestBody.create(HomeScreen.JSON, json.toString());
            Request request = new Request.Builder().url(URL).post(requestBody).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e(HomeScreen.LOG_TAG, "HttpPost failed");
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    final boolean result;
                    if (response.code() == 200) {
                        result = true;
                    }
                    else
                    {
                        result = false;
                    }

                    new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
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
                    });
                }
            });


        }
        catch(Exception e)
        {
            Log.e(HomeScreen.LOG_TAG, "HttpPost failed");
            Log.e(HomeScreen.LOG_TAG,e.getMessage());
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
                Log.d(HomeScreen.LOG_TAG, height + "");
                final int small_height = recList.getHeight()-(int)moveY;
                Log.d(HomeScreen.LOG_TAG, small_height + "");
                bottomContainer.setVisibility(View.VISIBLE);
                bottomContainer.setAlpha(1.0f);
                bottomRest.setVisibility(View.VISIBLE);
                bottomRest.setAlpha(0.0f);
                bottomContainer.setTranslationY(moveY);
                recList.setTranslationY(moveY);

                //bottomRest.setAlpha(0.0f);
                Log.d(HomeScreen.LOG_TAG, recList.getHeight() + "");
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
                                Log.d(HomeScreen.LOG_TAG, recList.getHeight() + "");
                                recList.setTranslationY(0);
                                Log.d(HomeScreen.LOG_TAG, recList.getHeight() + "");
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
