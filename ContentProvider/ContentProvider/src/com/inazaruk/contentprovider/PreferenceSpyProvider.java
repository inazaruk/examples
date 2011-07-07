package com.inazaruk.contentprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.inazaruk.preferencespy.provider.PreferenceSpy;
import com.inazaruk.preferencespy.provider.PreferenceSpy.Preferences;

public class PreferenceSpyProvider extends ContentProvider 
{	
	private static final String TAG = "PreferencesContentProvider";

    private static final String DATABASE_NAME = "preferencespy.db";
    private static final int DATABASE_VERSION = 2;
        
    private static final int PREFERENCES_TABLE = 1;
    private static final int PREFERENCES_TABLE_ID = 2;
    
    private static final UriMatcher sUriMatcher;
    
    static 
    {
    	sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    	sUriMatcher.addURI(PreferenceSpy.AUTHORITY, Preferences.TABLE_NAME, PREFERENCES_TABLE);
    	sUriMatcher.addURI(PreferenceSpy.AUTHORITY, Preferences.TABLE_NAME + "/#", PREFERENCES_TABLE_ID);    	
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper
    {
    	 DatabaseHelper(Context context) 
    	 {
             super(context, DATABASE_NAME, null, DATABASE_VERSION);
         }
    	
    	@Override
    	public void onCreate(SQLiteDatabase db)
    	{
    		 db.execSQL("CREATE TABLE " + Preferences.TABLE_NAME + " ("
                     + Preferences._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                     + Preferences.REMOTE_PACKAGE+ " TEXT,"
                     + Preferences.REMOTE_PREF_NAME + " TEXT,"
                     + Preferences.LOCAL_PREF_NAME + " TEXT,"
                     + Preferences.REMOTE_LAST_MODIFIED_TIME + " INTEGER"                     
                     + ");");         	
    	}
    	
    	@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    	{
    		 Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                     + newVersion + ", which will destroy all old data");
             db.execSQL("DROP TABLE IF EXISTS "+Preferences.TABLE_NAME);
             onCreate(db);
    	}
    }
    
    DatabaseHelper mOpenHelper;
    
	@Override
	public boolean onCreate()
	{	 
		mOpenHelper = new DatabaseHelper(getContext());
		return true;		
	}	
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder)
	{	
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(Preferences.TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
        case PREFERENCES_TABLE:            
            break;

        case PREFERENCES_TABLE_ID:            
            qb.appendWhere(Preferences._ID + "=" + uri.getLastPathSegment());
            break;
            
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Preferences.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;		
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues initialValues)
	{
        if (sUriMatcher.match(uri) != PREFERENCES_TABLE) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
                        
        if(values.containsKey(Preferences.REMOTE_LAST_MODIFIED_TIME) == false)
        {
        	values.put(Preferences.REMOTE_LAST_MODIFIED_TIME, 0L);        	
        }
        
        if(values.containsKey(Preferences.REMOTE_PACKAGE) == false)
        {
        	 throw new SQLException("Failed to insert row into " + uri +". "+
        			 				 "No "+Preferences.REMOTE_PACKAGE+" was not specified.");
        }
        
        if(values.containsKey(Preferences.REMOTE_PREF_NAME) == false)
        {
        	 throw new SQLException("Failed to insert row into " + uri +". "+
        			 				 "No "+Preferences.REMOTE_PREF_NAME+" was not specified.");
        }
        
        if(values.containsKey(Preferences.LOCAL_PREF_NAME) == false)
        {
        	 throw new SQLException("Failed to insert row into " + uri +". "+
        			 				 "No "+Preferences.LOCAL_PREF_NAME+" was not specified.");
        }
        

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(Preferences.TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Preferences.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) 
	{
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
        case PREFERENCES_TABLE:
            count = db.update(Preferences.TABLE_NAME, values, selection, selectionArgs);
            break;

        case PREFERENCES_TABLE_ID:
            String id = uri.getLastPathSegment();
            count = db.update(Preferences.TABLE_NAME, values, Preferences._ID + "=" + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	};
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{	
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
        case PREFERENCES_TABLE:
            count = db.delete(Preferences.TABLE_NAME, selection, selectionArgs);
            break;
        case PREFERENCES_TABLE_ID:
            String noteId = uri.getLastPathSegment();
            count = db.delete(Preferences.TABLE_NAME, Preferences._ID + "=" + noteId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection+ ')' : ""), 
                    selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
	
	@Override
	public String getType(Uri uri)
	{	
		switch(sUriMatcher.match(uri))
		{
		case PREFERENCES_TABLE:
			return Preferences.CONTENT_TYPE;
		case PREFERENCES_TABLE_ID:
			return Preferences.CONTENT_ITEM_TYPE;		
		}
		return null;
	}
}
