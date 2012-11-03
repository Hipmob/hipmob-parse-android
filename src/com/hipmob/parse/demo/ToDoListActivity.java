package com.hipmob.parse.demo;

import java.util.List;
import java.util.UUID;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.hipmob.android.HipmobCore;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.PushService;

public class ToDoListActivity extends ListActivity implements View.OnClickListener 
{
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;

	public static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;

	// Hipmob Application Identifier
    /*
     * Include your HIPMOB_KEY here.
	private static final String HIPMOB_KEY = "<key>";
    */
	
	private List<ParseObject> todos;
	private Dialog progressDialog;
	private Button support;

	private class RemoteDataTask extends AsyncTask<Void, Void, Void> 
	{
		// Override this method to do custom remote calls
		protected Void doInBackground(Void... params)
		{
			// make sure we have an installation
			getApplication();
			ParseInstallation pi = ParseInstallation.getCurrentInstallation();
			String id = pi.getString("hipmobId");
			if(id == null){
				String uid = ParseUser.getCurrentUser().getObjectId();
				if(uid == null){
					try{
						ParseUser.getCurrentUser().save();
						pi.put("hipmobId", ParseUser.getCurrentUser().getObjectId());
						pi.save();
					}catch(Exception e1){
						android.util.Log.v("ToDoListActivity", "Exception saving installation id ["+e1.getMessage()+"]", e1);
					}
				}
				// pi.saveInBackground(); // appears to cause a crash: quite confusing
			}
			
			// Gets the current list of todos in sorted order
			ParseQuery query = new ParseQuery("Todo");
			query.orderByDescending("_created_at");

			try {
				todos = query.find();
			} catch (ParseException e) {

			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			ToDoListActivity.this.progressDialog = ProgressDialog.show(ToDoListActivity.this, "",
					"Loading...", true);
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Void... values) {

			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Void result) {
			// Put the list of todos into the list view
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(ToDoListActivity.this,
					R.layout.todo_row);
			for (ParseObject todo : todos) {
				adapter.add((String) todo.get("name"));
			}
			setListAdapter(adapter);
			ToDoListActivity.this.progressDialog.dismiss();
			TextView empty = (TextView) findViewById(android.R.id.empty);
			empty.setVisibility(View.VISIBLE);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TextView empty = (TextView) findViewById(android.R.id.empty);
		empty.setVisibility(View.INVISIBLE);

		new RemoteDataTask().execute();
		registerForContextMenu(getListView());
	
		support = (Button)findViewById(R.id.support);
		support.setOnClickListener(this);
		
		// subscribe for push
		PushService.setDefaultPushCallback(this, com.hipmob.parse.demo.ToDoListActivity.class);
		PushService.subscribe(this, "", com.hipmob.parse.demo.ToDoListActivity.class);
	}

	private void createTodo() {
		Intent i = new Intent(this, CreateTodo.class);
		startActivityForResult(i, ACTIVITY_CREATE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (intent == null) {
			return;
		}
		final Bundle extras = intent.getExtras();

		switch (requestCode) {
		case ACTIVITY_CREATE:
			new RemoteDataTask() {
				protected Void doInBackground(Void... params) {
					String name = extras.getString("name");
					ParseObject todo = new ParseObject("Todo");
					todo.put("name", name);
					try {
						todo.save();
					} catch (ParseException e) {
					}

					super.doInBackground();
					return null;
				}
			}.execute();
			break;
		case ACTIVITY_EDIT:
			// Edit the remote object
			final ParseObject todo;
			todo = todos.get(extras.getInt("position"));
			todo.put("name", extras.getString("name"));

			new RemoteDataTask() {
				protected Void doInBackground(Void... params) {
					try {
						todo.save();
					} catch (ParseException e) {
					}
					super.doInBackground();
					return null;
				}
			}.execute();
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, INSERT_ID, 0, R.string.menu_insert);
		return result;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

			// Delete the remote object
			final ParseObject todo = todos.get(info.position);

			new RemoteDataTask() {
				protected Void doInBackground(Void... params) {
					try {
						todo.delete();
					} catch (ParseException e) {
					}
					super.doInBackground();
					return null;
				}
			}.execute();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case INSERT_ID:
			createTodo();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, CreateTodo.class);

		i.putExtra("name", todos.get(position).getString("name").toString());
		i.putExtra("position", position);
		startActivityForResult(i, ACTIVITY_EDIT);
	}

	@Override
	public void onClick(View v)
	{	
		// create an intent
		Intent i = new Intent(this, com.hipmob.android.HipmobCore.class);
		 
		// REQUIRED: set the appid to the key you're provided
		i.putExtra(HipmobCore.KEY_APPID, HIPMOB_KEY);
		 
		// Use the parse user id
		i.putExtra(HipmobCore.KEY_DEVICEID, ParseInstallation.getCurrentInstallation().getString("hipmobId"));
		
		// launch the chat window
		startActivity(i);
	}
}
