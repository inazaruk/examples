package com.inazaruk.activityresult;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ActivityA extends ActivityBase
{
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a);		
	}
	
	public void onStartBClicked(View v)
	{
		startActivityForResult(new Intent(this, ActivityB.class), 
										  ActivityHelper.AUTO_CLOSE_REQUEST_CODE);
	}
	
	
	 
}
