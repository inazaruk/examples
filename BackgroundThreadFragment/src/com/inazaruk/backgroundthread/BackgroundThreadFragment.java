package com.inazaruk.backgroundthread;

import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class BackgroundThreadFragment extends AsyncTaskFragment<Integer, Integer, Integer>
{
	TextView mStatus;
	TextView mProgress;
	TextView mResult;	
	Button mStart;
	Button mStop;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setVerbose(true);		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.bkthread, container, false);
		
		mStatus = (TextView)view.findViewById(R.id.status);
		mProgress = (TextView)view.findViewById(R.id.progress);
		mResult = (TextView)view.findViewById(R.id.result);
		mStart = (Button)view.findViewById(R.id.start);
		mStop  = (Button)view.findViewById(R.id.stop);
		
		mStart.setEnabled(getStatus() == Status.PENDING);
		mStop.setEnabled(getStatus() != Status.PENDING && !isCancelled());
				
		mStart.setOnClickListener(new OnClickListener()
		{		
			@Override
			public void onClick(View v)
			{
				mStart.setEnabled(false);
				mStop.setEnabled(true);
				execute(100, 100);
			}
		});
		
		mStop.setOnClickListener(new OnClickListener()
		{			
			@Override
			public void onClick(View v)
			{
				mStop.setEnabled(false);
				cancel(true);								
			}
		});
		
		return view;
	}

	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		mStatus.setText("starting...");
		mProgress.setText("-");
		mResult.setText("-");
	}
	
	@Override
	protected void onProgressUpdate(Integer... values)
	{		
		mProgress.setText(values[0].toString());
	}
	
	@Override
	protected Integer doInBackground(Integer... params)
	{		
		runOnUiThread(new Runnable()
		{			
			@Override
			public void run()
			{
				mStatus.setText("running");				
			}
		});		
		
		int counter = params[0];
		int timeout = params[1];
		
		int done = 0;
		for(int i = 0; i < counter && !isCancelled(); i++)
		{
			v("counter: "+i);
			done++;
			publishProgress(i);
			try
			{
				Thread.sleep(timeout);
			}
			catch(InterruptedException ex)
			{
				e(ex, "sleep interruption");
			}
		}
		
		return done;
	}
	
	@Override
	protected void onPostExecute(Integer result)
	{
		mStatus.setText("completed"); 
		mResult.setText(result.toString());
	}
	
	@Override
	protected void onCancelled()
	{
		mStatus.setText("cancelled");
		mResult.setText("x");
	}
	
	
}
