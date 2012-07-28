package com.inazaruk.searchview;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.os.Bundle;
import android.widget.backport.SearchView;
import android.widget.backport.SearchView.OnQueryTextListener;

public class MainActivity extends SherlockActivity implements OnQueryTextListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add("search");
        item.setIcon(android.R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        SearchView sv = new SearchView(this);
        sv.setQuery("test", false);
        sv.setOnQueryTextListener(this);
        item.setActionView(sv);
        return true;
    }
    
    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }
    
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
}
