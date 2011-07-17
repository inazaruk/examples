package com.inazaruk.configchanges;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity 
{	
	private final String TAG = this.toString();
	
	public interface TaskListener
	{
		void onProgress(int max, int cur);
		void onCompleted(Object result);
		void onCancelled();
	}
	
	/**
	 * If configuration change is detected this varibale is set to true.
	 * It will be checked in onDestroy() to decide whether to cancel
	 * async task or not.  
	 */
	boolean mConfigurationChange = false;
	
	/**
	 * This variable is set to true just before onDestroy returns.
	 * This is used to decide whether UI can be update or not.
	 */
	boolean mDestroyed = false;
	
	TextView mProgressText;	
	CrossConfigAsyncTask mAsyncTask;
	
	/**
	 * Only update UI if activity is not destroyed.
	 */
	TaskListener mTaskListener = new TaskListener()
	{		
		@Override
		public void onProgress(int max, int cur)
		{
			Log.e(TAG, "TaskListener.onProgress: "+cur+"/"+max);
			if(!mDestroyed)
			{
				mProgressText.setText(cur+"/"+max);
			}
		}
		
		@Override
		public void onCompleted(Object result)
		{
			Log.e(TAG, "TaskListener.onCompleted: "+result);
			if(!mDestroyed)
			{
				mProgressText.setText("Completed "+result);
			}
		}
		
		@Override
		public void onCancelled()
		{
			Log.e(TAG, "TaskListener.onCancelled");
			if(!mDestroyed)
			{
				mProgressText.setText("Cancelled");
			}			
		}
	};		
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.e(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mProgressText = (TextView)findViewById(R.id.progress);				
		
		mAsyncTask = (CrossConfigAsyncTask)getLastNonConfigurationInstance();
		if(mAsyncTask != null)
		{
			Log.e(TAG, "onCreate - mAsyncTask from previos activity is picked up. "+mAsyncTask);
			mAsyncTask.setListener(mTaskListener);
		}
		
		System.gc();//force previous object of Activity to be garbage collect, if possible
	}
	
	@Override
	public Object onRetainNonConfigurationInstance()
	{
		Log.e(TAG, "onRetainNonConfigurationInstance - mAsyncTask is saved. "+mAsyncTask);
		mConfigurationChange = true;
		return mAsyncTask;
	}	
	
	@Override
	protected void onDestroy()
	{
		Log.e(TAG, "onDestroy");
		super.onDestroy();
		
		if(!mConfigurationChange)
		{
			Log.e(TAG, "onDestroy - non configuration change onDestroy.");
			if(mAsyncTask != null)
			{
				Log.e(TAG, "onDestroy - cancelling task as it was running.");
				mAsyncTask.cancel(true);
			}
		}
		mAsyncTask.setListener(null);
		mDestroyed = true;
	}
	
	/**
	 * This function is called if button "start" is pressed. 
	 * It starts new long running background task.  
	 */
	public void start(View v)
	{
		Log.e(TAG, "start - start new task.");
		
		if(mAsyncTask != null)
		{
			Log.e(TAG, "start - cancel old task.");
			mAsyncTask.cancel(true);			
		}
		mAsyncTask = new CrossConfigAsyncTask(getApplicationContext(), 3000);
		mAsyncTask.setListener(mTaskListener);
		mAsyncTask.execute();
	}
	
	/**
	 * This function is called if button "cancel" is pressed. 
	 * It cancels long running background task.  
	 */
	public void cancel(View v)
	{
		Log.e(TAG, "cancel");
		if(mAsyncTask != null)
		{
			Log.e(TAG, "cancel - old task is cancelled");
			mAsyncTask.cancel(true);
		}
	}
	
	
	@Override
	protected void finalize() throws Throwable
	{
		Log.e(TAG, "garbage collected");
		super.finalize();
	}	
	
	/**
	 * This class must reference anything from Activity, as it :
	 * 1) May live longer then any activity (that is destroyed due to config changes)
	 * 2) It may complete its task after onDestroy() is called on old activity,
	 *    but before onCreate() is called on new activity due to configuration change.
	 * 
	 */
	private static class CrossConfigAsyncTask extends AsyncTask<Void, Integer, Object>
	{
		private final String TAG = this.toString();
		
		private final Context mContext;
		private final int mDuration;
		private int mProgress;
		private Object mResult = null;
		
		/**
		 * Use weak reference so Activity that registers listener is not 
		 * held in memory because of this listener. 
		 */
		private WeakReference<TaskListener> mListener;
		
		
		/**
		 * Do not pass Activity as context to this constructor. This should 
		 * be Application context. If Activity is passed here memory leaks
		 * are possible, because thread may live much longer then Activity that crated
		 * this thread. 
		 */
		public CrossConfigAsyncTask(Context ctx, int duration)
		{
			mContext = ctx;
			mDuration = duration;
			mListener = null;
		}		
		
		/**
		 * setListener checks weather result is already available, and
		 * if so immediately reports it on listener's callback function.
		 * This is done in case task is completed just between 
		 * onDestroy() and onCreate().
		 */
		public void setListener(TaskListener listener)
		{
			synchronized (this)
			{
				if(listener == null)
				{
					mListener = null;
				}
				else
				{
					mListener = new WeakReference<TaskListener>(listener);
					if(getStatus() == Status.FINISHED)
					{
						listener.onCompleted(mResult);
					}
					else 
					{
						listener.onProgress(mDuration, mProgress);
					}
				}	
			}			
		}	
		
		public TaskListener getListener()
		{
			synchronized (this)
			{
				if(mListener != null)
				{
					return mListener.get();					
				}
				return null;
			}
		}
		
		@Override
		protected void onPreExecute()
		{
			Log.e(TAG, "onPreExecute");
			onProgressUpdate(0);
		}		
		
		@Override
		protected Object doInBackground(Void... params)
		{
			Log.e(TAG, "doInBackground");
			
			//emulate long running non-interruptable work
			for(int i = 0; i < mDuration; i++)
			{
				if(isCancelled()) return null;
				try
				{
					Thread.sleep(10);
				}
				catch(InterruptedException ex)
				{
					return null;
				}
				
				publishProgress(i);
				Log.e(TAG, "doInBackground: "+mProgress+"/"+mDuration);
			}
			return new Object();
		}
		
		@Override
		protected void onProgressUpdate(Integer... values)
		{			
			TaskListener listener = getListener();
			if(listener != null)
			{
				mProgress = values[0];
				listener.onProgress(mDuration, values[0]);
			}			
		}
		
		@Override
		protected void onPostExecute(Object result)
		{
			Log.e(TAG, "onPostExecute: "+result);
			mResult = result;
			TaskListener listener = getListener();
			if(listener != null)
			{
				listener.onCompleted(result);
			}			
		}
		
		@Override
		protected void onCancelled()
		{
			Log.e(TAG, "onCancelled.");
			TaskListener listener = getListener();
			if(listener != null)
			{
				listener.onCancelled();
			}		
		}
	}
	
}