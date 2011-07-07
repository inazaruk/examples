package com.inazaruk.preferencespy.provider;

import android.net.Uri;
import android.provider.BaseColumns;
	
public class PreferenceSpy 
{
	private PreferenceSpy () {}
	
	public static final String AUTHORITY = "com.inazaruk.preferencespy.provider";	
	
	public static class Preferences implements BaseColumns
	{
		private Preferences() {}
		
		public static final String TABLE_NAME = "preferences";
		
		public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+TABLE_NAME);
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.inazaruk.prefererences";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.inazaruk.preferenceitem";
			
		/* type: string */
		public static final String REMOTE_PACKAGE = "RemotePackage";
		
		/* type: string */
		public static final String REMOTE_PREF_NAME = "RemoteName";
		
		/* type: string */
		public static final String LOCAL_PREF_NAME = "LocalName";
		
		/* type: long */
		public static final String REMOTE_LAST_MODIFIED_TIME = "RemoteLastModified";
		
		
		public static final String DEFAULT_SORT_ORDER = REMOTE_PACKAGE + " DESC";
	}
					
}	