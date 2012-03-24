package com.test.app;

import com.test.jar.JarDemo;

import android.app.Activity;
import android.os.Bundle;

public class AppActivity extends Activity {
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        JarDemo demo = new JarDemo();
        demo.useMe();
    }
}