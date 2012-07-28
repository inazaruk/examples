
package com.actionbarsherlock.widget.searchview.internal;

import com.actionbarsherlock.widget.searchview.OnCloseListener;
import com.actionbarsherlock.widget.searchview.OnQueryTextListener;
import com.actionbarsherlock.widget.searchview.OnSuggestionListener;

import android.annotation.TargetApi;
import android.app.SearchableInfo;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.CursorAdapter;

public interface ISearchView {
    
    @TargetApi(14)
    void setImeOptions(int imeOptions);

    @TargetApi(16)
    int getImeOptions();
    
    @TargetApi(14)
    void setInputType(int inputType);

    @TargetApi(16)
    int getInputType();
    
    CharSequence getQuery();

    void setQuery(CharSequence query, boolean submit);

    void setQueryHint(CharSequence hint);

    @TargetApi(16)
    CharSequence getQueryHint();

    void setIconifiedByDefault(boolean iconified);

    boolean isIconfiedByDefault();

    void setIconified(boolean iconify);

    boolean isIconified();
    
    void setSubmitButtonEnabled(boolean enabled);

    boolean isSubmitButtonEnabled();

    void setQueryRefinementEnabled(boolean enable);

    boolean isQueryRefinementEnabled();
    
    void setSuggestionsAdapter(CursorAdapter adapter);

    CursorAdapter getSuggestionsAdapter();

    void setMaxWidth(int maxpixels);

    @TargetApi(16)
    int getMaxWidth();
   
    void setSearchableInfo(SearchableInfo searchable);

    void setOnQueryTextListener(OnQueryTextListener listener);

    void setOnCloseListener(OnCloseListener listener);

    void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener);

    void setOnSuggestionListener(OnSuggestionListener listener);

    void setOnSearchClickListener(OnClickListener listener);
    
    void setFocusable(boolean focusable);
    
    void clearFocus();    

}
