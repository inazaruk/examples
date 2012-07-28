package com.actionbarsherlock.widget.searchview;

public interface OnSuggestionListener {

    boolean onSuggestionSelect(int position);

    boolean onSuggestionClick(int position);
}