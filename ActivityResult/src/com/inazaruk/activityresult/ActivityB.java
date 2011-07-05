package com.inazaruk.activityresult;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ActivityB extends ActivityBase
{	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.b);		
	}
	
	public void onStartAClicked(View v)
	{
		startActivityForResult(new Intent(this, ActivityA.class), 
							   	ActivityHelper.AUTO_CLOSE_REQUEST_CODE);
	}
	
	public void onStartCClicked(View v)
	{
		startActivityForResult(new Intent(this, ActivityC.class), 
							   	ActivityHelper.AUTO_CLOSE_REQUEST_CODE);
	}	
}
