/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.actionbarsherlock.widget.searchview.internal;

import com.actionbarsherlock.searchview.R;
import com.actionbarsherlock.view.CollapsibleActionView;
import com.actionbarsherlock.widget.searchview.OnCloseListener;
import com.actionbarsherlock.widget.searchview.OnQueryTextListener;
import com.actionbarsherlock.widget.searchview.OnSuggestionListener;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.CursorAdapter;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.WeakHashMap;

/**
 * Backported SearchView that is supposed to be used on platform <= 10.
 */
public class CompatSearchView extends LinearLayout implements CollapsibleActionView, ISearchView {

    private static final boolean DBG = false;
    private static final String LOG_TAG = "SearchView";

    /**
     * Private constant for removing the microphone in the keyboard.
     */
    private static final String IME_OPTION_NO_MICROPHONE = "nm";

    private OnQueryTextListener mOnQueryChangeListener;
    private OnCloseListener mOnCloseListener;
    private OnFocusChangeListener mOnQueryTextFocusChangeListener;
    private OnSuggestionListener mOnSuggestionListener;
    private OnClickListener mOnSearchClickListener;

    private boolean mIconifiedByDefault;
    private boolean mIconified;
    private CursorAdapter mSuggestionsAdapter;
    private View mSearchButton;
    private View mSubmitButton;
    private View mSearchPlate;
    private View mSubmitArea;
    private ImageView mCloseButton;
    private View mSearchEditFrame;
    private View mVoiceButton;
    private SearchAutoComplete mQueryTextView;
    private View mDropDownAnchor;
    private ImageView mSearchHintIcon;
    private boolean mSubmitButtonEnabled;
    private CharSequence mQueryHint;
    private boolean mQueryRefinement;
    private boolean mClearingFocus;
    private int mMaxWidth;
    private boolean mVoiceButtonEnabled;
    private CharSequence mOldQueryText;
    private CharSequence mUserQuery;
    private boolean mExpandedInActionView;
    private int mCollapsedImeOptions;

    private SearchableInfo mSearchable;
    private Bundle mAppSearchData;

    /*
     * SearchView can be set expanded before the IME is ready to be shown during
     * initial UI setup. The show operation is asynchronous to account for this.
     */
    private Runnable mShowImeRunnable = new Runnable() {
        public void run() {
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                //TODO: this was commented out during back porting
                //imm.showSoftInputUnchecked(0, null);
            }
        }
    };

    private Runnable mUpdateDrawableStateRunnable = new Runnable() {
        public void run() {
            updateFocusedState();
        }
    };

    private Runnable mReleaseCursorRunnable = new Runnable() {
        public void run() {
//TODO: this was commented out during back porting
//            if (mSuggestionsAdapter != null && mSuggestionsAdapter instanceof SuggestionsAdapter) {
//                mSuggestionsAdapter.changeCursor(null);
//            }
        }
    };

    // For voice searching
    private final Intent mVoiceWebSearchIntent;
    private final Intent mVoiceAppSearchIntent;

    // A weak map of drawables we've gotten from other packages, so we don't load them
    // more than once.
    private final WeakHashMap<String, Drawable.ConstantState> mOutsideDrawablesCache =
            new WeakHashMap<String, Drawable.ConstantState>();

    public CompatSearchView(Context context) {
        this(context, null);
    }

    public CompatSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.search_view, this, true);

        mSearchButton = findViewById(R.id.search_button);
        mQueryTextView = (SearchAutoComplete) findViewById(R.id.search_src_text);
        mQueryTextView.setSearchView(this);

        mSearchEditFrame = findViewById(R.id.search_edit_frame);
        mSearchPlate = findViewById(R.id.search_plate);
        mSubmitArea = findViewById(R.id.submit_area);
        mSubmitButton = findViewById(R.id.search_go_btn);
        mCloseButton = (ImageView) findViewById(R.id.search_close_btn);
        mVoiceButton = findViewById(R.id.search_voice_btn);
        mSearchHintIcon = (ImageView) findViewById(R.id.search_mag_icon);

        mSearchButton.setOnClickListener(mOnClickListener);
        mCloseButton.setOnClickListener(mOnClickListener);
        mSubmitButton.setOnClickListener(mOnClickListener);
        mVoiceButton.setOnClickListener(mOnClickListener);
        mQueryTextView.setOnClickListener(mOnClickListener);

        mQueryTextView.addTextChangedListener(mTextWatcher);
        mQueryTextView.setOnEditorActionListener(mOnEditorActionListener);
        mQueryTextView.setOnItemClickListener(mOnItemClickListener);
        mQueryTextView.setOnItemSelectedListener(mOnItemSelectedListener);
        mQueryTextView.setOnKeyListener(mTextKeyListener);
        // Inform any listener of focus changes
        mQueryTextView.setOnFocusChangeListener(new OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                if (mOnQueryTextFocusChangeListener != null) {
                    mOnQueryTextFocusChangeListener.onFocusChange(CompatSearchView.this, hasFocus);
                }
            }
        });

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SearchView, 0, 0);
        setIconifiedByDefault(a.getBoolean(R.styleable.SearchView_iconifiedByDefault, true));
        
        int maxWidth = a.getDimensionPixelSize(R.styleable.SearchView_maxWidth, -1);
        if (maxWidth != -1) {
            setMaxWidth(maxWidth);
        }
        CharSequence queryHint = a.getText(R.styleable.SearchView_queryHint);
        if (!TextUtils.isEmpty(queryHint)) {
            setQueryHint(queryHint);
        }
        int imeOptions = a.getInt(R.styleable.SearchView_imeOptions, -1);
        if (imeOptions != -1) {
            setImeOptions(imeOptions);
        }
        int inputType = a.getInt(R.styleable.SearchView_inputType, -1);
        if (inputType != -1) {
            setInputType(inputType);
        }
        
        boolean focusable = true;
        focusable = a.getBoolean(R.styleable.SearchView_focusable, focusable);        
        a.recycle();
        
        setFocusable(focusable);

        // Save voice intent for later queries/launching
        mVoiceWebSearchIntent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        mVoiceWebSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mVoiceWebSearchIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        mVoiceAppSearchIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mVoiceAppSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mDropDownAnchor = findViewById(mQueryTextView.getDropDownAnchor());
        if (mDropDownAnchor != null) {
//TODO: this was commented out during back porting            
//            mDropDownAnchor.addOnLayoutChangeListener(new OnLayoutChangeListener() {
//                @Override
//                public void onLayoutChange(View v, int left, int top, int right, int bottom,
//                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                    adjustDropDownSizeAndPosition();
//                }
//
//            });
        }

        updateViewsVisibility(mIconifiedByDefault);
        updateQueryHint();
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setSearchableInfo(android.app.SearchableInfo)
     */
    public void setSearchableInfo(SearchableInfo searchable) {
        mSearchable = searchable;
        if (mSearchable != null) {
            //TODO: this was commented out during back porting
            //updateSearchAutoComplete();
            updateQueryHint();
        }
        // Cache the voice search capability
        mVoiceButtonEnabled = hasVoiceSearch();

        if (mVoiceButtonEnabled) {
            // Disable the microphone on the keyboard, as a mic is displayed near the text box
            // TODO: use imeOptions to disable voice input when the new API will be available
            mQueryTextView.setPrivateImeOptions(IME_OPTION_NO_MICROPHONE);
        }
        updateViewsVisibility(isIconified());
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setAppSearchData(android.os.Bundle)
     */
    public void setAppSearchData(Bundle appSearchData) {
        mAppSearchData = appSearchData;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setImeOptions(int)
     */
    public void setImeOptions(int imeOptions) {
        mQueryTextView.setImeOptions(imeOptions);
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#getImeOptions()
     */
    public int getImeOptions() {
        return mQueryTextView.getImeOptions();
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setInputType(int)
     */
    public void setInputType(int inputType) {
        mQueryTextView.setInputType(inputType);
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#getInputType()
     */
    public int getInputType() {
        return mQueryTextView.getInputType();
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#requestFocus(int, android.graphics.Rect)
     */
    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) return false;
        // Check if SearchView is focusable.
        if (!isFocusable()) return false;
        // If it is not iconified, then give the focus to the text field
        if (!isIconified()) {
            boolean result = mQueryTextView.requestFocus(direction, previouslyFocusedRect);
            if (result) {
                updateViewsVisibility(false);
            }
            return result;
        } else {
            return super.requestFocus(direction, previouslyFocusedRect);
        }
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#clearFocus()
     */
    @Override
    public void clearFocus() {
        mClearingFocus = true;
        setImeVisibility(false);
        super.clearFocus();
        mQueryTextView.clearFocus();
        mClearingFocus = false;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setOnQueryTextListener(com.actionbarsherlock.widget.searchview.OnQueryTextListener)
     */
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setOnCloseListener(com.actionbarsherlock.widget.searchview.OnCloseListener)
     */
    public void setOnCloseListener(OnCloseListener listener) {
        mOnCloseListener = listener;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setOnQueryTextFocusChangeListener(android.view.View.OnFocusChangeListener)
     */
    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        mOnQueryTextFocusChangeListener = listener;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setOnSuggestionListener(com.actionbarsherlock.widget.searchview.OnSuggestionListener)
     */
    public void setOnSuggestionListener(OnSuggestionListener listener) {
        mOnSuggestionListener = listener;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setOnSearchClickListener(android.view.View.OnClickListener)
     */
    public void setOnSearchClickListener(OnClickListener listener) {
        mOnSearchClickListener = listener;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#getQuery()
     */
    public CharSequence getQuery() {
        return mQueryTextView.getText();
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setQuery(java.lang.CharSequence, boolean)
     */
    public void setQuery(CharSequence query, boolean submit) {
        mQueryTextView.setText(query);
        if (query != null) {
            mQueryTextView.setSelection(mQueryTextView.length());
            mUserQuery = query;
        }

        // If the query is not empty and submit is requested, submit the query
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setQueryHint(java.lang.CharSequence)
     */
    public void setQueryHint(CharSequence hint) {
        mQueryHint = hint;
        updateQueryHint();
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#getQueryHint()
     */
    public CharSequence getQueryHint() {
        if (mQueryHint != null) {
            return mQueryHint;
        } else if (mSearchable != null) {
            CharSequence hint = null;
            int hintId = mSearchable.getHintId();
            if (hintId != 0) {
                hint = getContext().getString(hintId);
            }
            return hint;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setIconifiedByDefault(boolean)
     */
    public void setIconifiedByDefault(boolean iconified) {
        if (mIconifiedByDefault == iconified) return;
        mIconifiedByDefault = iconified;
        updateViewsVisibility(iconified);
        updateQueryHint();
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#isIconfiedByDefault()
     */
    public boolean isIconfiedByDefault() {
        return mIconifiedByDefault;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setIconified(boolean)
     */
    public void setIconified(boolean iconify) {
        if (iconify) {
            onCloseClicked();
        } else {
            onSearchClicked();
        }
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#isIconified()
     */
    public boolean isIconified() {
        return mIconified;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setSubmitButtonEnabled(boolean)
     */
    public void setSubmitButtonEnabled(boolean enabled) {
        mSubmitButtonEnabled = enabled;
        updateViewsVisibility(isIconified());
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#isSubmitButtonEnabled()
     */
    public boolean isSubmitButtonEnabled() {
        return mSubmitButtonEnabled;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setQueryRefinementEnabled(boolean)
     */
    public void setQueryRefinementEnabled(boolean enable) {
        mQueryRefinement = enable;
//TODO: this was commented out during back porting
//        if (mSuggestionsAdapter instanceof SuggestionsAdapter) {
//            ((SuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
//                    enable ? SuggestionsAdapter.REFINE_ALL : SuggestionsAdapter.REFINE_BY_ENTRY);
//        }
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#isQueryRefinementEnabled()
     */
    public boolean isQueryRefinementEnabled() {
        return mQueryRefinement;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setSuggestionsAdapter(android.support.v4.widget.CursorAdapter)
     */
    public void setSuggestionsAdapter(CursorAdapter adapter) {
        mSuggestionsAdapter = adapter;

        mQueryTextView.setAdapter(mSuggestionsAdapter);
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#getSuggestionsAdapter()
     */
    public CursorAdapter getSuggestionsAdapter() {
        return mSuggestionsAdapter;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#setMaxWidth(int)
     */
    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;

        requestLayout();
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#getMaxWidth()
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Let the standard measurements take effect in iconified state.
        if (isIconified()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        switch (widthMode) {
        case MeasureSpec.AT_MOST:
            // If there is an upper limit, don't exceed maximum width (explicit or implicit)
            if (mMaxWidth > 0) {
                width = Math.min(mMaxWidth, width);
            } else {
                width = Math.min(getPreferredWidth(), width);
            }
            break;
        case MeasureSpec.EXACTLY:
            // If an exact width is specified, still don't exceed any specified maximum width
            if (mMaxWidth > 0) {
                width = Math.min(mMaxWidth, width);
            }
            break;
        case MeasureSpec.UNSPECIFIED:
            // Use maximum width, if specified, else preferred width
            width = mMaxWidth > 0 ? mMaxWidth : getPreferredWidth();
            break;
        }
        widthMode = MeasureSpec.EXACTLY;
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode), heightMeasureSpec);
    }

    private int getPreferredWidth() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.search_view_preferred_width);
    }

    private void updateViewsVisibility(final boolean collapsed) {
        mIconified = collapsed;
        // Visibility of views that are visible when collapsed
        final int visCollapsed = collapsed ? VISIBLE : GONE;
        // Is there text in the query
        final boolean hasText = !TextUtils.isEmpty(mQueryTextView.getText());

        mSearchButton.setVisibility(visCollapsed);
        updateSubmitButton(hasText);
        mSearchEditFrame.setVisibility(collapsed ? GONE : VISIBLE);
        mSearchHintIcon.setVisibility(mIconifiedByDefault ? GONE : VISIBLE);
        updateCloseButton();
        updateVoiceButton(!hasText);
        updateSubmitArea();
    }

    private boolean hasVoiceSearch() {
        if (mSearchable != null && mSearchable.getVoiceSearchEnabled()) {
            Intent testIntent = null;
            if (mSearchable.getVoiceSearchLaunchWebSearch()) {
                testIntent = mVoiceWebSearchIntent;
            } else if (mSearchable.getVoiceSearchLaunchRecognizer()) {
                testIntent = mVoiceAppSearchIntent;
            }
            if (testIntent != null) {
                ResolveInfo ri = getContext().getPackageManager().resolveActivity(testIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                return ri != null;
            }
        }
        return false;
    }

    private boolean isSubmitAreaEnabled() {
        return (mSubmitButtonEnabled || mVoiceButtonEnabled) && !isIconified();
    }

    private void updateSubmitButton(boolean hasText) {
        int visibility = GONE;
        if (mSubmitButtonEnabled && isSubmitAreaEnabled() && hasFocus()
                && (hasText || !mVoiceButtonEnabled)) {
            visibility = VISIBLE;
        }
        mSubmitButton.setVisibility(visibility);
    }

    private void updateSubmitArea() {
        int visibility = GONE;
        if (isSubmitAreaEnabled()
                && (mSubmitButton.getVisibility() == VISIBLE
                        || mVoiceButton.getVisibility() == VISIBLE)) {
            visibility = VISIBLE;
        }
        mSubmitArea.setVisibility(visibility);
    }

    private void updateCloseButton() {
        final boolean hasText = !TextUtils.isEmpty(mQueryTextView.getText());
        // Should we show the close button? It is not shown if there's no focus,
        // field is not iconified by default and there is no text in it.
        final boolean showClose = hasText || (mIconifiedByDefault && !mExpandedInActionView);
        mCloseButton.setVisibility(showClose ? VISIBLE : GONE);
        mCloseButton.getDrawable().setState(hasText ? ENABLED_STATE_SET : EMPTY_STATE_SET);
    }

    private void postUpdateFocusedState() {
        post(mUpdateDrawableStateRunnable);
    }

    private void updateFocusedState() {
        boolean focused = mQueryTextView.hasFocus();
        mSearchPlate.getBackground().setState(focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET);
        mSubmitArea.getBackground().setState(focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mUpdateDrawableStateRunnable);
        post(mReleaseCursorRunnable);
        super.onDetachedFromWindow();
    }

    private void setImeVisibility(final boolean visible) {
        if (visible) {
            post(mShowImeRunnable);
        } else {
            removeCallbacks(mShowImeRunnable);
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    /**
     * Called by the SuggestionsAdapter
     * @hide
     */
    /* package */void onQueryRefine(CharSequence queryText) {
        setQuery(queryText);
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {

        public void onClick(View v) {
            if (v == mSearchButton) {
                onSearchClicked();
            } else if (v == mCloseButton) {
                onCloseClicked();
            } else if (v == mSubmitButton) {
                onSubmitQuery();
            } else if (v == mVoiceButton) {
                onVoiceClicked();
            } else if (v == mQueryTextView) {
                forceSuggestionQuery();
            }
        }
    };

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mSearchable == null) {
            return false;
        }

        // if it's an action specified by the searchable activity, launch the
        // entered query with the action key
        
//TODO: this was commented out during back porting        
//        SearchableInfo.ActionKeyInfo actionKey = mSearchable.findActionKey(keyCode);
//        if ((actionKey != null) && (actionKey.getQueryActionMsg() != null)) {
//            launchQuerySearch(keyCode, actionKey.getQueryActionMsg(), mQueryTextView.getText()
//                    .toString());
//            return true;
//        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * React to the user typing "enter" or other hardwired keys while typing in
     * the search box. This handles these special keys while the edit box has
     * focus.
     */
    View.OnKeyListener mTextKeyListener = new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            // guard against possible race conditions
            if (mSearchable == null) {
                return false;
            }

            if (DBG) {
                Log.d(LOG_TAG, "mTextListener.onKey(" + keyCode + "," + event + "), selection: "
                        + mQueryTextView.getListSelection());
            }

            // If a suggestion is selected, handle enter, search key, and action keys
            // as presses on the selected suggestion
//TODO: this was commented out during back porting            
//            if (mQueryTextView.isPopupShowing()
//                    && mQueryTextView.getListSelection() != ListView.INVALID_POSITION) {
//                return onSuggestionsKey(v, keyCode, event);
//            }
//
//            // If there is text in the query box, handle enter, and action keys
//            // The search key is handled by the dialog's onKeyDown().
//            if (!mQueryTextView.isEmpty() && event.hasNoModifiers()) {
//                if (event.getAction() == KeyEvent.ACTION_UP) {
//                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
//                        v.cancelLongPress();
//
//                        // Launch as a regular search.
//                        launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, mQueryTextView.getText()
//                                .toString());
//                        return true;
//                    }
//                }
//                if (event.getAction() == KeyEvent.ACTION_DOWN) {
//                    SearchableInfo.ActionKeyInfo actionKey = mSearchable.findActionKey(keyCode);
//                    if ((actionKey != null) && (actionKey.getQueryActionMsg() != null)) {
//                        launchQuerySearch(keyCode, actionKey.getQueryActionMsg(), mQueryTextView
//                                .getText().toString());
//                        return true;
//                    }
//                }
//            }
            return false;
        }
    };

    /**
     * React to the user typing while in the suggestions list. First, check for
     * action keys. If not handled, try refocusing regular characters into the
     * EditText.
     */
//TODO: this was commented out during back porting
//    private boolean onSuggestionsKey(View v, int keyCode, KeyEvent event) {
//        // guard against possible race conditions (late arrival after dismiss)
//        if (mSearchable == null) {
//            return false;
//        }
//        if (mSuggestionsAdapter == null) {
//            return false;
//        }
//        if (event.getAction() == KeyEvent.ACTION_DOWN && event.hasNoModifiers()) {
//            // First, check for enter or search (both of which we'll treat as a
//            // "click")
//            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SEARCH
//                    || keyCode == KeyEvent.KEYCODE_TAB) {
//                int position = mQueryTextView.getListSelection();
//                return onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
//            }
//
//            // Next, check for left/right moves, which we use to "return" the
//            // user to the edit view
//            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
//                // give "focus" to text editor, with cursor at the beginning if
//                // left key, at end if right key
//                // TODO: Reverse left/right for right-to-left languages, e.g.
//                // Arabic
//                int selPoint = (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? 0 : mQueryTextView
//                        .length();
//                mQueryTextView.setSelection(selPoint);
//                mQueryTextView.setListSelection(0);
//                mQueryTextView.clearListSelection();
//                mQueryTextView.ensureImeVisible(true);
//
//                return true;
//            }
//
//            // Next, check for an "up and out" move
//            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && 0 == mQueryTextView.getListSelection()) {
//                // TODO: restoreUserQuery();
//                // let ACTV complete the move
//                return false;
//            }
//
//            // Next, check for an "action key"
//            SearchableInfo.ActionKeyInfo actionKey = mSearchable.findActionKey(keyCode);
//            if ((actionKey != null)
//                    && ((actionKey.getSuggestActionMsg() != null) || (actionKey
//                            .getSuggestActionMsgColumn() != null))) {
//                // launch suggestion using action key column
//                int position = mQueryTextView.getListSelection();
//                if (position != ListView.INVALID_POSITION) {
//                    Cursor c = mSuggestionsAdapter.getCursor();
//                    if (c.moveToPosition(position)) {
//                        final String actionMsg = getActionKeyMessage(c, actionKey);
//                        if (actionMsg != null && (actionMsg.length() > 0)) {
//                            return onItemClicked(position, keyCode, actionMsg);
//                        }
//                    }
//                }
//            }
//        }
//        return false;
//    }

    /**
     * For a given suggestion and a given cursor row, get the action message. If
     * not provided by the specific row/column, also check for a single
     * definition (for the action key).
     *
     * @param c The cursor providing suggestions
     * @param actionKey The actionkey record being examined
     *
     * @return Returns a string, or null if no action key message for this
     *         suggestion
     */
//TODO: this was commented out during back porting
//    private static String getActionKeyMessage(Cursor c, SearchableInfo.ActionKeyInfo actionKey) {
//        String result = null;
//        // check first in the cursor data, for a suggestion-specific message
//        final String column = actionKey.getSuggestActionMsgColumn();
//        if (column != null) {
//            result = SuggestionsAdapter.getColumnString(c, column);
//        }
//        // If the cursor didn't give us a message, see if there's a single
//        // message defined
//        // for the actionkey (for all suggestions)
//        if (result == null) {
//            result = actionKey.getSuggestActionMsg();
//        }
//        return result;
//    }

    private int getSearchIconId() {
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.searchViewSearchIcon,
                outValue, true);
        return outValue.resourceId;
    }

    private CharSequence getDecoratedHint(CharSequence hintText) {
        // If the field is always expanded, then don't add the search icon to the hint
        if (!mIconifiedByDefault) return hintText;

        SpannableStringBuilder ssb = new SpannableStringBuilder("   "); // for the icon
        ssb.append(hintText);
        Drawable searchIcon = getContext().getResources().getDrawable(getSearchIconId());
        int textSize = (int) (mQueryTextView.getTextSize() * 1.25);
        searchIcon.setBounds(0, 0, textSize, textSize);
        ssb.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    private void updateQueryHint() {
        if (mQueryHint != null) {
            mQueryTextView.setHint(getDecoratedHint(mQueryHint));
        } else if (mSearchable != null) {
            CharSequence hint = null;
            int hintId = mSearchable.getHintId();
            if (hintId != 0) {
                hint = getContext().getString(hintId);
            }
            if (hint != null) {
                mQueryTextView.setHint(getDecoratedHint(hint));
            }
        } else {
            mQueryTextView.setHint(getDecoratedHint(""));
        }
    }

    /**
     * Updates the auto-complete text view.
     */
//TODO: this was commented out during back porting    
//    private void updateSearchAutoComplete() {
//        mQueryTextView.setDropDownAnimationStyle(0); // no animation
//        mQueryTextView.setThreshold(mSearchable.getSuggestThreshold());
//        mQueryTextView.setImeOptions(mSearchable.getImeOptions());
//        int inputType = mSearchable.getInputType();
//        // We only touch this if the input type is set up for text (which it almost certainly
//        // should be, in the case of search!)
//        if ((inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
//            // The existence of a suggestions authority is the proxy for "suggestions
//            // are available here"
//            inputType &= ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
//            if (mSearchable.getSuggestAuthority() != null) {
//                inputType |= InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
//                // TYPE_TEXT_FLAG_AUTO_COMPLETE means that the text editor is performing
//                // auto-completion based on its own semantics, which it will present to the user
//                // as they type. This generally means that the input method should not show its
//                // own candidates, and the spell checker should not be in action. The text editor
//                // supplies its candidates by calling InputMethodManager.displayCompletions(),
//                // which in turn will call InputMethodSession.displayCompletions().
//                inputType |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
//            }
//        }
//        mQueryTextView.setInputType(inputType);
//        if (mSuggestionsAdapter != null) {
//            mSuggestionsAdapter.changeCursor(null);
//        }
//        // attach the suggestions adapter, if suggestions are available
//        // The existence of a suggestions authority is the proxy for "suggestions available here"
//        if (mSearchable.getSuggestAuthority() != null) {
//            mSuggestionsAdapter = new SuggestionsAdapter(getContext(),
//                    this, mSearchable, mOutsideDrawablesCache);
//            mQueryTextView.setAdapter(mSuggestionsAdapter);
//            ((SuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
//                    mQueryRefinement ? SuggestionsAdapter.REFINE_ALL
//                    : SuggestionsAdapter.REFINE_BY_ENTRY);
//        }
//    }

    /**
     * Update the visibility of the voice button.  There are actually two voice search modes,
     * either of which will activate the button.
     * @param empty whether the search query text field is empty. If it is, then the other
     * criteria apply to make the voice button visible.
     */
    private void updateVoiceButton(boolean empty) {
        int visibility = GONE;
        if (mVoiceButtonEnabled && !isIconified() && empty) {
            visibility = VISIBLE;
            mSubmitButton.setVisibility(GONE);
        }
        mVoiceButton.setVisibility(visibility);
    }

    private final OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {

        /**
         * Called when the input method default action key is pressed.
         */
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            onSubmitQuery();
            return true;
        }
    };

    private void onTextChanged(CharSequence newText) {
        CharSequence text = mQueryTextView.getText();
        mUserQuery = text;
        boolean hasText = !TextUtils.isEmpty(text);
        updateSubmitButton(hasText);
        updateVoiceButton(!hasText);
        updateCloseButton();
        updateSubmitArea();
        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }

    private void onSubmitQuery() {
        CharSequence query = mQueryTextView.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null
                    || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                if (mSearchable != null) {
                    launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, query.toString());
                    setImeVisibility(false);
                }
                dismissSuggestions();
            }
        }
    }

    private void dismissSuggestions() {
        mQueryTextView.dismissDropDown();
    }

    private void onCloseClicked() {
        CharSequence text = mQueryTextView.getText();
        if (TextUtils.isEmpty(text)) {
            if (mIconifiedByDefault) {
                // If the app doesn't override the close behavior
                if (mOnCloseListener == null || !mOnCloseListener.onClose()) {
                    // hide the keyboard and remove focus
                    clearFocus();
                    // collapse the search field
                    updateViewsVisibility(true);
                }
            }
        } else {
            mQueryTextView.setText("");
            mQueryTextView.requestFocus();
            setImeVisibility(true);
        }

    }

    private void onSearchClicked() {
        updateViewsVisibility(false);
        mQueryTextView.requestFocus();
        setImeVisibility(true);
        if (mOnSearchClickListener != null) {
            mOnSearchClickListener.onClick(this);
        }
    }

    private void onVoiceClicked() {
        // guard against possible race conditions
        if (mSearchable == null) {
            return;
        }
        SearchableInfo searchable = mSearchable;
        try {
            if (searchable.getVoiceSearchLaunchWebSearch()) {
                Intent webSearchIntent = createVoiceWebSearchIntent(mVoiceWebSearchIntent,
                        searchable);
                getContext().startActivity(webSearchIntent);
            } else if (searchable.getVoiceSearchLaunchRecognizer()) {
                Intent appSearchIntent = createVoiceAppSearchIntent(mVoiceAppSearchIntent,
                        searchable);
                getContext().startActivity(appSearchIntent);
            }
        } catch (ActivityNotFoundException e) {
            // Should not happen, since we check the availability of
            // voice search before showing the button. But just in case...
            Log.w(LOG_TAG, "Could not find voice search activity");
        }
    }

    void onTextFocusChanged() {
        updateViewsVisibility(isIconified());
        // Delayed update to make sure that the focus has settled down and window focus changes
        // don't affect it. A synchronous update was not working.
        postUpdateFocusedState();
        if (mQueryTextView.hasFocus()) {
            forceSuggestionQuery();
        }
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#onWindowFocusChanged(boolean)
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        postUpdateFocusedState();
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#onActionViewCollapsed()
     */
    @Override
    public void onActionViewCollapsed() {
        clearFocus();
        updateViewsVisibility(true);
        mQueryTextView.setImeOptions(mCollapsedImeOptions);
        mExpandedInActionView = false;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.widget.searchview.ISearchView2#onActionViewExpanded()
     */
    @Override
    public void onActionViewExpanded() {
        if (mExpandedInActionView) return;

        mExpandedInActionView = true;
        mCollapsedImeOptions = mQueryTextView.getImeOptions();
        mQueryTextView.setImeOptions(mCollapsedImeOptions | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        mQueryTextView.setText("");
        setIconified(false);
    }

//TODO: this was commented out during back porting    
//    @Override
//    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
//        super.onInitializeAccessibilityEvent(event);
//        event.setClassName(SearchView.class.getName());
//    }
//
//    @Override
//    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
//        super.onInitializeAccessibilityNodeInfo(info);
//        info.setClassName(SearchView.class.getName());
//    }

    private void adjustDropDownSizeAndPosition() {
        if (mDropDownAnchor.getWidth() > 1) {
            Resources res = getContext().getResources();
            int anchorPadding = mSearchPlate.getPaddingLeft();
            Rect dropDownPadding = new Rect();
            int iconOffset = mIconifiedByDefault
                    ? res.getDimensionPixelSize(R.dimen.dropdownitem_icon_width)
                    + res.getDimensionPixelSize(R.dimen.dropdownitem_text_padding_left)
                    : 0;
            mQueryTextView.getDropDownBackground().getPadding(dropDownPadding);
            mQueryTextView.setDropDownHorizontalOffset(-(dropDownPadding.left + iconOffset)
                    + anchorPadding);
            mQueryTextView.setDropDownWidth(mDropDownAnchor.getWidth() + dropDownPadding.left
                    + dropDownPadding.right + iconOffset - (anchorPadding));
        }
    }

    private boolean onItemClicked(int position, int actionKey, String actionMsg) {
//TODO: this was commented out during back porting        
//        if (mOnSuggestionListener == null
//                || !mOnSuggestionListener.onSuggestionClick(position)) {
//            launchSuggestion(position, KeyEvent.KEYCODE_UNKNOWN, null);
//            setImeVisibility(false);
//            dismissSuggestions();
//            return true;
//        }
        return false;
    }

    private boolean onItemSelected(int position) {
        if (mOnSuggestionListener == null
                || !mOnSuggestionListener.onSuggestionSelect(position)) {
            rewriteQueryFromSuggestion(position);
            return true;
        }
        return false;
    }

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        /**
         * Implements OnItemClickListener
         */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(LOG_TAG, "onItemClick() position " + position);
            onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
        }
    };

    private final OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener() {

        /**
         * Implements OnItemSelectedListener
         */
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(LOG_TAG, "onItemSelected() position " + position);
            CompatSearchView.this.onItemSelected(position);
        }

        /**
         * Implements OnItemSelectedListener
         */
        public void onNothingSelected(AdapterView<?> parent) {
            if (DBG)
                Log.d(LOG_TAG, "onNothingSelected()");
        }
    };

    /**
     * Query rewriting.
     */
    private void rewriteQueryFromSuggestion(int position) {
        CharSequence oldQuery = mQueryTextView.getText();
        Cursor c = mSuggestionsAdapter.getCursor();
        if (c == null) {
            return;
        }
        if (c.moveToPosition(position)) {
            // Get the new query from the suggestion.
            CharSequence newQuery = mSuggestionsAdapter.convertToString(c);
            if (newQuery != null) {
                // The suggestion rewrites the query.
                // Update the text field, without getting new suggestions.
                setQuery(newQuery);
            } else {
                // The suggestion does not rewrite the query, restore the user's query.
                setQuery(oldQuery);
            }
        } else {
            // We got a bad position, restore the user's query.
            setQuery(oldQuery);
        }
    }

    /**
     * Launches an intent based on a suggestion.
     *
     * @param position The index of the suggestion to create the intent from.
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return true if a successful launch, false if could not (e.g. bad position).
     */
//TODO: this was commented out during back porting    
//    private boolean launchSuggestion(int position, int actionKey, String actionMsg) {
//        Cursor c = mSuggestionsAdapter.getCursor();
//        if ((c != null) && c.moveToPosition(position)) {
//
//            Intent intent = createIntentFromSuggestion(c, actionKey, actionMsg);
//
//            // launch the intent
//            launchIntent(intent);
//
//            return true;
//        }
//        return false;
//    }

    /**
     * Launches an intent, including any special intent handling.
     */
    private void launchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            // If the intent was created from a suggestion, it will always have an explicit
            // component here.
            getContext().startActivity(intent);
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Failed launch activity: " + intent, ex);
        }
    }

    /**
     * Sets the text in the query box, without updating the suggestions.
     */
    private void setQuery(CharSequence query) {
        //TODO: this was commented out during back porting
        //mQueryTextView.setText(query, true);
        mQueryTextView.setText(query);
        // Move the cursor to the end
        mQueryTextView.setSelection(TextUtils.isEmpty(query) ? 0 : query.length());
    }

    private void launchQuerySearch(int actionKey, String actionMsg, String query) {
        String action = Intent.ACTION_SEARCH;
        Intent intent = createIntent(action, null, null, query, actionKey, actionMsg);
        getContext().startActivity(intent);
    }

    /**
     * Constructs an intent from the given information and the search dialog state.
     *
     * @param action Intent action.
     * @param data Intent data, or <code>null</code>.
     * @param extraData Data for {@link SearchManager#EXTRA_DATA_KEY} or <code>null</code>.
     * @param query Intent query, or <code>null</code>.
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @param mode The search mode, one of the acceptable values for
     *             {@link SearchManager#SEARCH_MODE}, or {@code null}.
     * @return The intent.
     */
    private Intent createIntent(String action, Uri data, String extraData, String query,
            int actionKey, String actionMsg) {
        // Now build the Intent
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want. We don't want to do this in in-app search though,
        // as it can be destructive to the activity stack.
        if (data != null) {
            intent.setData(data);
        }
        intent.putExtra(SearchManager.USER_QUERY, mUserQuery);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData);
        }
        if (mAppSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, mAppSearchData);
        }
        if (actionKey != KeyEvent.KEYCODE_UNKNOWN) {
            intent.putExtra(SearchManager.ACTION_KEY, actionKey);
            intent.putExtra(SearchManager.ACTION_MSG, actionMsg);
        }
        intent.setComponent(mSearchable.getSearchActivity());
        return intent;
    }

    /**
     * Create and return an Intent that can launch the voice search activity for web search.
     */
    private Intent createVoiceWebSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        Intent voiceIntent = new Intent(baseIntent);
        ComponentName searchActivity = searchable.getSearchActivity();
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null
                : searchActivity.flattenToShortString());
        return voiceIntent;
    }

    /**
     * Create and return an Intent that can launch the voice search activity, perform a specific
     * voice transcription, and forward the results to the searchable activity.
     *
     * @param baseIntent The voice app search intent to start from
     * @return A completely-configured intent ready to send to the voice search activity
     */
    private Intent createVoiceAppSearchIntent(Intent baseIntent, SearchableInfo searchable) {
        ComponentName searchActivity = searchable.getSearchActivity();

        // create the necessary intent to set up a search-and-forward operation
        // in the voice search system.   We have to keep the bundle separate,
        // because it becomes immutable once it enters the PendingIntent
        Intent queryIntent = new Intent(Intent.ACTION_SEARCH);
        queryIntent.setComponent(searchActivity);
        PendingIntent pending = PendingIntent.getActivity(getContext(), 0, queryIntent,
                PendingIntent.FLAG_ONE_SHOT);

        // Now set up the bundle that will be inserted into the pending intent
        // when it's time to do the search.  We always build it here (even if empty)
        // because the voice search activity will always need to insert "QUERY" into
        // it anyway.
        Bundle queryExtras = new Bundle();

        // Now build the intent to launch the voice search.  Add all necessary
        // extras to launch the voice recognizer, and then all the necessary extras
        // to forward the results to the searchable activity
        Intent voiceIntent = new Intent(baseIntent);

        // Add all of the configuration options supplied by the searchable's metadata
        String languageModel = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
        String prompt = null;
        String language = null;
        int maxResults = 1;

        Resources resources = getResources();
        if (searchable.getVoiceLanguageModeId() != 0) {
            languageModel = resources.getString(searchable.getVoiceLanguageModeId());
        }
        if (searchable.getVoicePromptTextId() != 0) {
            prompt = resources.getString(searchable.getVoicePromptTextId());
        }
        if (searchable.getVoiceLanguageId() != 0) {
            language = resources.getString(searchable.getVoiceLanguageId());
        }
        if (searchable.getVoiceMaxResults() != 0) {
            maxResults = searchable.getVoiceMaxResults();
        }
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, searchActivity == null ? null
                : searchActivity.flattenToShortString());

        // Add the values that configure forwarding the results
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, pending);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE, queryExtras);

        return voiceIntent;
    }

    /**
     * When a particular suggestion has been selected, perform the various lookups required
     * to use the suggestion.  This includes checking the cursor for suggestion-specific data,
     * and/or falling back to the XML for defaults;  It also creates REST style Uri data when
     * the suggestion includes a data id.
     *
     * @param c The suggestions cursor, moved to the row of the user's selection
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return An intent for the suggestion at the cursor's position.
     */
//TODO: this was commented out during back porting    
//    private Intent createIntentFromSuggestion(Cursor c, int actionKey, String actionMsg) {
//        try {
//            // use specific action if supplied, or default action if supplied, or fixed default
//            String action = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_ACTION);
//
//            if (action == null) {
//                action = mSearchable.getSuggestIntentAction();
//            }
//            if (action == null) {
//                action = Intent.ACTION_SEARCH;
//            }
//
//            // use specific data if supplied, or default data if supplied
//            String data = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA);
//            if (data == null) {
//                data = mSearchable.getSuggestIntentData();
//            }
//            // then, if an ID was provided, append it.
//            if (data != null) {
//                String id = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
//                if (id != null) {
//                    data = data + "/" + Uri.encode(id);
//                }
//            }
//            Uri dataUri = (data == null) ? null : Uri.parse(data);
//
//            String query = getColumnString(c, SearchManager.SUGGEST_COLUMN_QUERY);
//            String extraData = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
//
//            return createIntent(action, dataUri, extraData, query, actionKey, actionMsg);
//        } catch (RuntimeException e ) {
//            int rowNum;
//            try {                       // be really paranoid now
//                rowNum = c.getPosition();
//            } catch (RuntimeException e2 ) {
//                rowNum = -1;
//            }
//            Log.w(LOG_TAG, "Search Suggestions cursor at row " + rowNum +
//                            " returned exception" + e.toString());
//            return null;
//        }
//    }

    private void forceSuggestionQuery() {
//        mQueryTextView.doBeforeTextChanged();
//        mQueryTextView.doAfterTextChanged();
    }

    static boolean isLandscapeMode(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Callback to watch the text field for empty/non-empty
     */
    private TextWatcher mTextWatcher = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int before, int after) { }

        public void onTextChanged(CharSequence s, int start,
                int before, int after) {
            CompatSearchView.this.onTextChanged(s);
        }

        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Local subclass for AutoCompleteTextView.
     * @hide
     */
    public static class SearchAutoComplete extends AutoCompleteTextView {

        private int mThreshold;
        private CompatSearchView mSearchView;

        public SearchAutoComplete(Context context) {
            super(context);
            mThreshold = getThreshold();
        }

        public SearchAutoComplete(Context context, AttributeSet attrs) {
            super(context, attrs);
            mThreshold = getThreshold();
        }

        public SearchAutoComplete(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            mThreshold = getThreshold();
        }

        void setSearchView(CompatSearchView searchView) {
            mSearchView = searchView;
        }

        @Override
        public void setThreshold(int threshold) {
            super.setThreshold(threshold);
            mThreshold = threshold;
        }

        /**
         * Returns true if the text field is empty, or contains only whitespace.
         */
        private boolean isEmpty() {
            return TextUtils.getTrimmedLength(getText()) == 0;
        }

        /**
         * We override this method to avoid replacing the query box text when a
         * suggestion is clicked.
         */
        @Override
        protected void replaceText(CharSequence text) {
        }

        /**
         * We override this method to avoid an extra onItemClick being called on
         * the drop-down's OnItemClickListener by
         * {@link AutoCompleteTextView#onKeyUp(int, KeyEvent)} when an item is
         * clicked with the trackball.
         */
        @Override
        public void performCompletion() {
        }

        /**
         * We override this method to be sure and show the soft keyboard if
         * appropriate when the TextView has focus.
         */
        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);

            if (hasWindowFocus && mSearchView.hasFocus() && getVisibility() == VISIBLE) {
                InputMethodManager inputManager = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(this, 0);
                // If in landscape mode, then make sure that
                // the ime is in front of the dropdown.
                if (isLandscapeMode(getContext())) {
                    //TODO: this was commented out during back porting
                    //ensureImeVisible(true);
                }
            }
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            mSearchView.onTextFocusChanged();
        }

        /**
         * We override this method so that we can allow a threshold of zero,
         * which ACTV does not.
         */
        @Override
        public boolean enoughToFilter() {
            return mThreshold <= 0 || super.enoughToFilter();
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // special case for the back key, we do not even try to send it
                // to the drop down list but instead, consume it immediately
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.startTracking(event, this);
                    }
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.handleUpEvent(event);
                    }
                    if (event.isTracking() && !event.isCanceled()) {
                        mSearchView.clearFocus();
                        mSearchView.setImeVisibility(false);
                        return true;
                    }
                }
            }
            return super.onKeyPreIme(keyCode, event);
        }

    }
}
