package com.inazaruk.example;

import android.os.Bundle;
import com.google.android.maps.MapActivity;

public class MyMapActivity extends MapActivity {
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.my_map_activity);
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
