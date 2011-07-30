package com.inazaruk.backgroundthread;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;

public abstract class AsyncTaskFragment<Params, Progress, Result> extends Fragment
{
	static private long sTaskIdCounter = 1;
	public class FragmentTask extends AsyncTask<Params, Progress, Result>
	{		
		private final long mId;
		public FragmentTask()
		{
			synchronized (AsyncTaskFragment.class)
			{
				mId = sTaskIdCounter++;
			}
		}
		
		@Override
		protected void onPreExecute()
		{			
			v(mId+": onPreExecute()");
			AsyncTaskFragment.this.onPreExecute();
		}
		
		@Override
		protected void onProgressUpdate(Progress... values) 
		{
			v(mId+": onProgressUpdate()");
			AsyncTaskFragment.this.onProgressUpdate(values);
		};
				
		@Override
		protected Result doInBackground(Params... params) 
		{
			v(mId+": doInBackground()");
			return AsyncTaskFragment.this.doInBackground(params);
		};
		
		@Override
		protected void onPostExecute(Result result) 
		{
			v(mId+": onPostExecute(): " + result);
			AsyncTaskFragment.this.onPostExecute(result);
		};
		
		@Override
		protected void onCancelled()
		{ 
			v(mId+": onCancelled()");
			AsyncTaskFragment.this.onCancelled();
		}		
		
		protected void publishProgressHelper(Progress...progress)
		{
			super.publishProgress(progress);
		}
	}
	
	private Handler mHandler;
	private FragmentTask mAsyncTask;
	
	private boolean mVerbose = false;
	private String mLogTag = getClass().getSimpleName();
		
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		v("onCreate()");
		super.onCreate(savedInstanceState);
		
		/* this allows to survive configuration changes */
		setRetainInstance(true);
		
		mHandler = new Handler();
		mAsyncTask = new FragmentTask();
	}	
	
	@Override
	public void onDestroy()
	{
		v("onDestroy()");
		super.onDestroy();
		mAsyncTask.cancel(true);
	}	
	
		
	/********************************/
	/**       Helpers			   **/
	/********************************/
	
	protected void runOnUiThread(Runnable runnable)
	{
		mHandler.post(runnable);
	}
	
	protected Handler getHandler()
	{
		return mHandler;
	}
	
	/********************************/
	/** 	AsyncTask callbacks    **/
	/********************************/	
	
	protected void onPreExecute(){}
		
	protected void onProgressUpdate(Progress... values){};
			
	protected abstract Result doInBackground(Params... params);
			
	protected void onPostExecute(Result result){};
		
	protected void onCancelled(){}
	
	protected void publishProgress(Progress...progress)
	{
		mAsyncTask.publishProgressHelper(progress);
	}	
	
	/*********************************/
	/**     AsyncTask functions 	**/
	/*********************************/
			
	public void execute(Params... params)	
	{	
		v("execute()", params.length);		
		mAsyncTask.execute(params);						
	}
	
	public Result get() throws CancellationException, InterruptedException, ExecutionException 
	{
		v("get()");
		return mAsyncTask.get();
	}
	
	public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
			TimeoutException
	{
		v("get(timeout, unit)");
		return mAsyncTask.get(timeout, unit);
	}
	
	public boolean cancel(boolean mayInterruptIfRunning) 
	{
		v("cancel()");
		return mAsyncTask.cancel(mayInterruptIfRunning);
	}
	
	public boolean isCancelled()
	{
		return mAsyncTask.isCancelled();
	}
	
	public AsyncTask.Status getStatus()
	{
		return mAsyncTask.getStatus();
	}
	
	public AsyncTask<Params, Progress, Result> getAsyncTask()
	{
		return mAsyncTask;
	}
	
	/********************************/
	/**         	Logs           **/
	/********************************/
	
	public void setVerbose(boolean verbose)
	{
		mVerbose = verbose;
	}
	
	public boolean isVerbose()
	{
		return mVerbose;
	}
	
	public void setLogTag(String tag)
	{
		mLogTag = tag;
	}
	
	public String getLogTag()
	{
		return mLogTag;
	}
	
	protected void v(String fmt, Object... objs)
	{
		if(mVerbose)
		{
			Log.v(mLogTag, String.format(fmt, objs));
		}
	}
	
	protected void v(Throwable ex, String fmt, Object... objs)
	{
		if(mVerbose)
		{
			Log.v(mLogTag, String.format(fmt, objs), ex);
		}
	}
	
	protected void w(String fmt, Object... objs)
	{
		if(mVerbose)
		{
			Log.w(mLogTag, String.format(fmt, objs));
		}
	}
	
	protected void w(Throwable ex, String fmt, Object... objs)
	{
		if(mVerbose)
		{
			Log.w(mLogTag, String.format(fmt, objs), ex);
		}
	}
	
	protected void e(String fmt, Object... objs)
	{
		if(mVerbose)
		{
			Log.e(mLogTag, String.format(fmt, objs));
		}
	}
	
	protected void e(Throwable ex, String fmt, Object... objs)
	{
		if(mVerbose)
		{
			Log.e(mLogTag, String.format(fmt, objs), ex);
		}
	}
}
