package com.inazaruk.example;

import android.app.Activity;

public class MyMapFragment extends ActivityHostFragment {
    
    @Override
    protected Class<? extends Activity> getActivityClass() {
        return MyMapActivity.class;
    }
}
