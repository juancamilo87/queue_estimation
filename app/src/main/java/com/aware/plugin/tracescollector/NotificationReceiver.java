package com.aware.plugin.tracescollector;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

/**
 * Created by researcher on 03/06/15.
 */
public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int mNotificationId = 13548;

        mNotifyMgr.cancel(mNotificationId);

        Intent resultIntent = new Intent(context.getApplicationContext(), HelperActivity.class);
        resultIntent.putExtra(HelperActivity.NOTIFICATION_ID,mNotificationId);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context.getApplicationContext(),
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context.getApplicationContext())
                        .setSmallIcon(R.drawable.icon_remote_white)
                        .setContentTitle("Traces Collector")
                        .setContentText("You are not connected to the remote")
                        .setContentIntent(pendingIntent)
                        .addAction(R.drawable.icon_remote_white, "Connect", pendingIntent)
                        .setAutoCancel(true);
        // Sets an ID for the notification

        // Gets an instance of the NotificationManager service

        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }
}
