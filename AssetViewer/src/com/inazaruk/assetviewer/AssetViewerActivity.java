package com.inazaruk.assetviewer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AssetViewerActivity extends Activity
{	
	static class Asset
	{
		Asset(String path, String text)
		{
			this.path = path;
			this.text = text;
		}
		
		String path;
		String text;
	}
	
	interface ProgressTracker
	{
		void onUpdate(String path);		
	}
	
	protected static void listAssets(AssetManager assetManager, String path, List<Asset> assets, 
									 ProgressTracker tracker)
			throws IOException
	{
		/* ingoring built-in asset folders */
		if(path.equals("images") || path.equals("webkit") || path.equals("sounds"))
		{
			return;
		}
		
		tracker.onUpdate(path);
				
		String [] list = assetManager.list(path);
		if(list != null && list.length > 0)
		{
			/* path points to directory */
			for(String entry : list)
			{	
				String entryPath = path;
				if(entryPath.length() > 0)
				{
					entryPath += "/";
				}
				entryPath += entry;
				
				listAssets(assetManager, entryPath, assets, tracker);
			}
		}
		else
		{
			/* path points to file, read first 512 bytes */
			
			byte [] data = new byte [512];
			int size = 0;
			InputStream in = assetManager.open(path);
			size = in.read(data);
			in.close();
			
			ByteArrayInputStream bin = new ByteArrayInputStream(data, 0, size);
			BufferedReader reader = new BufferedReader(new InputStreamReader(bin));
			
			String text = "";
			String line = null;
			while((line = reader.readLine()) != null)
			{
				text+=line+"\n";
			}
			assets.add(new Asset(path, text));
		}
	}
	
	static private class AssetAdapter extends BaseAdapter
	{
		final LayoutInflater m_infalter;		
		private List<Asset> m_assets = new LinkedList<Asset>();
		
		public AssetAdapter(Context ctx)
		{
			m_infalter = LayoutInflater.from(ctx);
		}
		
		public void update(List<Asset> assets)
		{
			m_assets = assets;			
			notifyDataSetInvalidated();
		}
		
		@Override
		public int getCount()
		{			
			return m_assets.size();
		}
		
		@Override
		public Asset getItem(int position)
		{			
			return m_assets.get(position);
		}
		
		@Override
		public long getItemId(int position)
		{			
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if(view == null)
			{
				view = m_infalter.inflate(R.layout.asset, parent, false);
			}
			
			TextView path = (TextView)view.findViewById(R.id.assetPath);
			TextView text = (TextView)view.findViewById(R.id.assetText);
			
			Asset asset = getItem(position);
			path.setText(asset.path);
			text.setText(asset.text);
			
			return view;
		}
		
		@Override
		public boolean hasStableIds()
		{			
			return true;
		}
	}
	
	Handler m_handler;
	AssetAdapter m_adapter;
	ListView m_list;
	TextView m_text;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		m_handler = new Handler();
		m_adapter = new AssetAdapter(this);
		
		m_list = (ListView)findViewById(R.id.list);
		m_text = (TextView)findViewById(R.id.text);
				
		m_list.setAdapter(m_adapter);
		m_text.setText(getString(R.string.loading, ""));
		
		AssetLister lister = new AssetLister();
		lister.execute();
	}
	
	class UIProgresTracker implements ProgressTracker
	{
		@Override
		public void onUpdate(final String path)
		{
			m_handler.post(new Runnable()
			{				
				@Override
				public void run()
				{
					m_text.setText(getString(R.string.loading, path));					
				}
			});				
		}
	}
	
	
	class AssetLister extends AsyncTask<Object, Object, Throwable>
	{		
		List<Asset> m_assets = new LinkedList<Asset>();
		
		@Override
		protected Throwable doInBackground(Object... params)
		{
			AssetManager assetManager = getAssets();
			try
			{
				listAssets(assetManager, "", m_assets, new UIProgresTracker());
				return null;
			}
			catch(IOException ex)
			{
				return ex;
			}			
		}
		
		@Override
		protected void onPostExecute(Throwable ex)
		{
			if(ex != null)
			{
				Toast.makeText(AssetViewerActivity.this,
								"Failed to list assets: "+ex, 
								Toast.LENGTH_LONG).show();				
			}			
			
			if(m_assets.size() == 0)
			{
				m_text.setText("No assets found");
				m_list.setVisibility(View.GONE);
			}
			else
			{
				m_adapter.update(m_assets);
				m_list.setVisibility(View.VISIBLE);
				m_text.setVisibility(View.GONE);
			}		
			
		}
	}
}