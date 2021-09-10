package com.example.carexpirationdates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

//Restarts the service when the app is closed by the user
public class Restarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            context.startForegroundService(new Intent(context, Receiver.class));
        }
        else
        {
            context.startService(new Intent(context, Receiver.class));
        }
    }
}