package com.actionbarsherlock.widget.searchview.internal;

import com.actionbarsherlock.searchview.R;
import com.actionbarsherlock.widget.searchview.OnCloseListener;
import com.actionbarsherlock.widget.searchview.OnQueryTextListener;
import com.actionbarsherlock.widget.searchview.OnSuggestionListener;

import android.annotation.TargetApi;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;

public abstract class ForwardingSearchView extends LinearLayout implements ISearchView {
    
    private final ISearchView mSearchView;
    
    public ForwardingSearchView(Context context) {
        this(context, null);
    }

    public ForwardingSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSearchView = createSearchView(context, attrs);
        addView((View)mSearchView);
        
        /* make sure compat attributes are applied to any SearchView */
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SearchView, 0, 0);
        setIconifiedByDefault(a.getBoolean(R.styleable.SearchView_iconifiedByDefault, true));
        
        int maxWidth = a.getDimensionPixelSize(R.styleable.SearchView_maxWidth, -1);
        if (maxWidth != -1) {
            mSearchView.setMaxWidth(maxWidth);
        }
        CharSequence queryHint = a.getText(R.styleable.SearchView_queryHint);
        if (!TextUtils.isEmpty(queryHint)) {
            mSearchView.setQueryHint(queryHint);
        }
        int imeOptions = a.getInt(R.styleable.SearchView_imeOptions, -1);
        if (imeOptions != -1) {
            mSearchView.setImeOptions(imeOptions);
        }
        int inputType = a.getInt(R.styleable.SearchView_inputType, -1);
        if (inputType != -1) {
            mSearchView.setInputType(inputType);
        }
        
        boolean focusable = true;
        focusable = a.getBoolean(R.styleable.SearchView_focusable, focusable);        
        a.recycle();
                
        mSearchView.setFocusable(focusable);
    }
    
    abstract protected ISearchView createSearchView(Context context, AttributeSet attrs);

    @Override
    public void setSearchableInfo(SearchableInfo searchable) {
        mSearchView.setSearchableInfo(searchable);
    }

    @TargetApi(14)
    @Override
    public void setImeOptions(int imeOptions) {
        mSearchView.setImeOptions(imeOptions);
    }

    @TargetApi(16)
    @Override
    public int getImeOptions() {
        return mSearchView.getImeOptions();
    }

    @TargetApi(14)
    @Override
    public void setInputType(int inputType) {
        mSearchView.setInputType(inputType);
    }

    @TargetApi(16)
    @Override
    public int getInputType() {
        return mSearchView.getInputType();
    }

    @Override
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mSearchView.setOnQueryTextListener(listener);
    }

    @Override
    public void setOnCloseListener(OnCloseListener listener) {
        mSearchView.setOnCloseListener(listener);
    }

    @Override
    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        mSearchView.setOnQueryTextFocusChangeListener(listener);
    }

    @Override
    public void setOnSuggestionListener(OnSuggestionListener listener) {
        mSearchView.setOnSuggestionListener(listener);
    }

    @Override
    public void setOnSearchClickListener(OnClickListener listener) {
        mSearchView.setOnSearchClickListener(listener);
    }

    @Override
    public CharSequence getQuery() {
        return mSearchView.getQuery();
    }

    @Override
    public void setQuery(CharSequence query, boolean submit) {
        mSearchView.setQuery(query, submit);
    }

    @Override
    public void setQueryHint(CharSequence hint) {
        mSearchView.setQueryHint(hint);
    }

    @TargetApi(16)
    @Override
    public CharSequence getQueryHint() {
        return mSearchView.getQueryHint();
    }

    @Override
    public void setIconifiedByDefault(boolean iconified) {
        mSearchView.setIconifiedByDefault(iconified);
    }

    @Override
    public boolean isIconfiedByDefault() {
        return mSearchView.isIconfiedByDefault();
    }

    @Override
    public void setIconified(boolean iconify) {
        mSearchView.setIconified(iconify);
    }

    @Override
    public boolean isIconified() {
        return mSearchView.isIconified();
    }

    @Override
    public void setSubmitButtonEnabled(boolean enabled) {
        mSearchView.setSubmitButtonEnabled(enabled);
    }

    @Override
    public boolean isSubmitButtonEnabled() {
        return mSearchView.isSubmitButtonEnabled();
    }

    @Override
    public void setQueryRefinementEnabled(boolean enable) {
        mSearchView.setQueryRefinementEnabled(enable);
    }

    @Override
    public boolean isQueryRefinementEnabled() {
        return mSearchView.isQueryRefinementEnabled();
    }

    @Override
    public void setSuggestionsAdapter(CursorAdapter adapter) {
        mSearchView.setSuggestionsAdapter(adapter);
    }

    @Override
    public CursorAdapter getSuggestionsAdapter() {
        return mSearchView.getSuggestionsAdapter();
    }

    @Override
    public void setMaxWidth(int maxpixels) {
        mSearchView.setMaxWidth(maxpixels);
    }

    @TargetApi(16)
    @Override
    public int getMaxWidth() {
        return mSearchView.getMaxWidth();
    }    
}
