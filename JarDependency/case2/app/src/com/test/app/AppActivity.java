package com.test.app;

import com.test.jar.JarDemo;
import com.test.lib.LibDemo;

import android.app.Activity;
import android.os.Bundle;

public class AppActivity extends Activity {
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        LibDemo libDemo = new LibDemo();
        libDemo.useMe();
        
        JarDemo jarDemo = new JarDemo();
        jarDemo.useMe();
        
        
    }
}