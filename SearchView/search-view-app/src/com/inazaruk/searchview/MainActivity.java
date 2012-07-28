package com.inazaruk.searchview;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.searchview.OnQueryTextListener;
import com.actionbarsherlock.widget.searchview.internal.CompatSearchView;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends SherlockActivity implements OnQueryTextListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    
    public boolean onCreateOptionsMenu(Menu menu) {
       
        addCompatSearch(menu);
        try {
          addSearch(menu);
        } catch (Throwable ex) {
            Log.e("ERROR", "Failed to add search", ex);
        }
        return true;
    }
    
    private void addCompatSearch(Menu menu) {
        MenuItem item = menu.add("search");
        item.setIcon(android.R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        CompatSearchView sv = new CompatSearchView(this);
        sv.setQuery("test", false);
        sv.setOnQueryTextListener(this);
        sv.setSubmitButtonEnabled(true);
        sv.setQueryHint("TEST HINT");
        sv.setImeOptions(0);
        item.setActionView(sv);
    }
    
    @TargetApi(11)
    private void addSearch(Menu menu) {
        android.widget.SearchView sv = new android.widget.SearchView(this);
        sv.setQuery("test2", false);
        sv.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return MainActivity.this.onQueryTextSubmit(query);
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                return MainActivity.this.onQueryTextChange(newText);
            }
        });
        MenuItem item = menu.add("search2");
        item.setIcon(android.R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setActionView(sv);
    }
    
    @Override
    public boolean onQueryTextChange(String newText) {
        Log.e("CHANGE", newText);
        return false;
    }
    
    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.e("SUBMIT", query);
        return false;
    }
}
