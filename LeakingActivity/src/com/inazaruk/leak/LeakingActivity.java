package com.inazaruk.leak;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.ConcurrentHashMap;

public class LeakingActivity extends Activity {
    
    private static final long UPDATE_INTERVAL = 1000; 
    private static final String ACTION_THREAD_COUNT_CHANGED = "threadcount"; 

    private static int sThreads = 0;
    private static ConcurrentHashMap<Integer, Integer> sThreadTimeouts = new ConcurrentHashMap<Integer, Integer>();

    private byte [] mData = new byte[5 * 1024 * 1024];
    private TextView mStatus;
    private ProgressBar mMemoryProgress;
    private Handler mHandler;
    private Leaker mLeaker;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            update();   
            mHandler.postDelayed(mRunnable, UPDATE_INTERVAL);
        }
    }; 
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mStatus = (TextView)findViewById(R.id.status);
        mMemoryProgress = (ProgressBar)findViewById(R.id.progress);
        mHandler = new Handler();
        mLeaker = new Leaker();
        mLeaker.start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,  new IntentFilter(ACTION_THREAD_COUNT_CHANGED));
        mRunnable.run();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        mHandler.removeCallbacks(mRunnable);
    }
    
    public void onGC(View v) {
        System.gc();
    }
    
    public void update() {
        StringBuilder builder = new StringBuilder();
              
        Runtime rt = Runtime.getRuntime(); 
        mMemoryProgress.setMax((int)rt.maxMemory());
        mMemoryProgress.setSecondaryProgress((int)rt.totalMemory());
        mMemoryProgress.setProgress((int)(rt.totalMemory() - rt.freeMemory()));
            
        float maxMem = rt.maxMemory() * 1f / 1024 / 1024;
        float totalMem = rt.totalMemory() * 1f / 1024 / 1024;
        float freeMem = rt.freeMemory() * 1f / 1024 / 1024;
            
        String mem = String.format("MAX=%.2f Mb\nTOTAL=%.2f Mb\nFREE=%.2f Mb\n", maxMem, totalMem, freeMem);
        builder.append(mem);
                
        synchronized (LeakingActivity.class) {
            builder.append("\n");
            builder.append("Threads: " + sThreads + "\n");
            builder.append("\n");
            
            for(Integer id : sThreadTimeouts.keySet()) {
                builder.append("thread " + id +" - " + sThreadTimeouts.get(id) + "ms\n");
            }
        }
        mStatus.setText(builder.toString());
    }
    
    private class Leaker extends Thread {
        
        @Override
        public void run() {
            synchronized (LeakingActivity.class) {
                sThreads++;
            }
            int timeout = 30;
            try {
                for(int i = timeout; i > 0; i--) {
                    synchronized (LeakingActivity.class) {
                        sThreadTimeouts.put(Process.myTid(), i);
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                //ignore
            }
            synchronized (LeakingActivity.class) {
                sThreadTimeouts.remove(Process.myTid());
                sThreads--;
            }
        }
    }
}