package com.inazaruk.activityresult;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;

public class ActivityBase extends Activity
{ 
	public void onCloseClicked(View v)
	{
		finish();
	}
	
	public void onAutoCloseClicked(View v)
	{
		setResult(ActivityHelper.AUTO_CLOSE_RESULT_CODE);
		finish();
	}
	
	 @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{		 
		 if(requestCode == ActivityHelper.AUTO_CLOSE_REQUEST_CODE && 
		    requestCode == ActivityHelper.AUTO_CLOSE_RESULT_CODE)
		 {
			 Log.e(getClass().getSimpleName(), "onActivityResult - auto close, propagating");
			 setResult(ActivityHelper.AUTO_CLOSE_RESULT_CODE);
			 finish();
		 }
		 else
		 {
			 Log.e(getClass().getSimpleName(), "onActivityResult - no auto close");
		 }
	}
}
