package com.inazaruk.app;

import com.google.common.base.Preconditions;

import android.app.Activity;
import android.os.Bundle;

public class AppActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preconditions.checkArgument(true);//verify guava is available
        setContentView(R.layout.main);
    }
}