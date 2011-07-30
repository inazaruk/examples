package com.inazaruk.backgroundthread;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

public class MainActivity extends FragmentActivity 
{
	BackgroundThreadFragment mBkthreadFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		FragmentManager manager = getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		mBkthreadFragment = new BackgroundThreadFragment();
		transaction.add(R.id.bkthread, mBkthreadFragment);		
		transaction.commit();
	}
	
	public void onReset(View v)
	{
		FragmentManager manager = getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.remove(mBkthreadFragment);
		
		mBkthreadFragment = new BackgroundThreadFragment();
		transaction.add(R.id.bkthread, mBkthreadFragment);
		
		transaction.commit();
	}
}