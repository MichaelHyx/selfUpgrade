package com.vanilla.hyx.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private Button dialogUpdateBtn;
    private Button notificationUpdateBtn;
    private Button startScheduleUpdateBtn;
    private Button cancelScheduleUpdateBtn;
    private Button recycleBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dialogUpdateBtn = (Button) findViewById(R.id.update_button);
        notificationUpdateBtn = (Button) findViewById(R.id.update_notify);
        startScheduleUpdateBtn = (Button) findViewById(R.id.update_scheduled);
        cancelScheduleUpdateBtn = (Button) findViewById(R.id.update_cancel_scheduled);
        recycleBtn = (Button) findViewById(R.id.update_destroy);
        dialogUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        notificationUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //manager.checkUpdateForNotification();
            }
        });
        startScheduleUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //manager.startScheduledUpdate(10, 10);
            }
        });
        cancelScheduleUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //manager.cancelScheduledUpdate();
            }
        });
        recycleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //manager.recycle();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
