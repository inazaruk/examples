package com.inazaruk.lib;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

	 
public class LibView extends TextView
{
    public LibView(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
 
        TypedArray array = ctx.obtainStyledAttributes(attrs, R.styleable.CustomAttrs);
        String text = array.getString(R.styleable.CustomAttrs_xattr);
        if(text != null)
        {
            setText(text);
        }
        array.recycle();
    }
}