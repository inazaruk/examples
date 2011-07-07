package com.inazaruk.contentconsumer;

import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.inazaruk.preferencespy.provider.PreferenceSpy.Preferences;

public class ContentConsumerActivity extends Activity
{
	private static final String TAG = ContentConsumerActivity.class.getSimpleName();
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);		
						
		/* insert */		
		ContentValues values = new ContentValues();
		values.put(Preferences.REMOTE_PACKAGE, getPackageName());
		values.put(Preferences.REMOTE_PREF_NAME, "remotePreferences");
		values.put(Preferences.LOCAL_PREF_NAME, "localPreferences");
		values.put(Preferences.REMOTE_LAST_MODIFIED_TIME, 0L);				
		Uri uri = getContentResolver().insert(Preferences.CONTENT_URI, values);
					
		printValues();
		
		/* update */		
		values = new ContentValues();		
		values.put(Preferences.REMOTE_LAST_MODIFIED_TIME, new Date().getTime());				
		getContentResolver().update(uri, values, null, null);		
	}
	
	public void printValues()
	{
		String [] projection = new String[] 
        {
  			Preferences._ID,
  			Preferences.REMOTE_PACKAGE,
  			Preferences.REMOTE_PREF_NAME,
  			Preferences.LOCAL_PREF_NAME,
  			Preferences.REMOTE_LAST_MODIFIED_TIME
  		};	
		
		Cursor cursor = this.managedQuery(Preferences.CONTENT_URI,
							projection, null, null, null);
						
		cursor.moveToFirst();		
		do
		{
			String id = cursor.getString(cursor.getColumnIndex(Preferences._ID));
			String pkg = cursor.getString(cursor.getColumnIndex(Preferences.REMOTE_PACKAGE)); 
			String remoteName = cursor.getString(cursor.getColumnIndex(Preferences.REMOTE_PREF_NAME));
			String localName  = cursor.getString(cursor.getColumnIndex(Preferences.LOCAL_PREF_NAME));
			long lastModified = cursor.getLong(cursor.getColumnIndex(Preferences.REMOTE_LAST_MODIFIED_TIME));
			
			Log.v(TAG, "Record: "+id+", "+pkg+", "+remoteName+", "+localName+", "+lastModified);
		}
		while(cursor.moveToNext());		
		cursor.close();	
	}
}