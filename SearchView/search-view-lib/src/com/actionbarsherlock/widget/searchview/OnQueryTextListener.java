package com.actionbarsherlock.widget.searchview;

public interface OnQueryTextListener {

    boolean onQueryTextSubmit(String query);

    boolean onQueryTextChange(String newText);
}