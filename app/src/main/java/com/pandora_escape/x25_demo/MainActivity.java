package com.pandora_escape.x25_demo;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.pandora_escape.x25_demo.message_db.Message;
import com.pandora_escape.x25_demo.message_db.MessagesContract;
import com.pandora_escape.x25_demo.message_db.MessagesDBHelper;

public class MainActivity extends AppCompatActivity {

	public static final String EXTRA_MESSAGE_TITLE = "com.pandora_escape.x25_demo.MESSAGE_TITLE";
	public static final String EXTRA_MESSAGE_BODY = "com.pandora_escape.x25_demo.MESSAGE_BODY";

	public static final String QR_SCAN_ADDRESS = "com.google.zxing.client.android.SCAN";
	public static final int QR_SCAN_REQUEST_CODE = 1;

	private static final int CONTENT_ANIM_FADE = 1500;
	private static final int SPLASH_ANIM_STATIC = 2000;
	private static final int SPLASH_ANIM_FADE = 1000;

	private static SimpleCursorAdapter mMessageCursorAdapter;
	private static MessagesDBHelper sMessagesDBHelper;
	private Cursor mCursor;

	private LinearLayout mContentLayout;
	private ListView mMessageListView;
	private ImageView mSplashImageView;
	private ImageView mLoadingImageView;

	private AnimationDrawable mSpinnerAnimation;

	private boolean mFirstRun = true;


	// Functions
	private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView parent, View v, int position, long id) {
			mCursor.moveToPosition(position);

			int titleIndex = mCursor.getColumnIndex(MessagesContract.MessagesAll.COLUMN_NAME_TITLE);
			int bodyIndex = mCursor.getColumnIndex(MessagesContract.MessagesAll.COLUMN_NAME_BODY);

			displayMessage(mCursor.getString(titleIndex), mCursor.getString(bodyIndex));
		}
	};

	public void scanQR(View view) {
		mLoadingImageView.setVisibility(View.VISIBLE);
		mSpinnerAnimation.start();

		try {
			Intent intent = new Intent(QR_SCAN_ADDRESS);
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes

			startActivityForResult(intent, QR_SCAN_REQUEST_CODE);

		} catch (Exception e) {

			Uri marketUri = Uri.parse("market://details?id=" + QR_SCAN_ADDRESS);
			Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
			startActivity(marketIntent);
		}
	}

	public void displayMessage(Message message) {
		Intent MessageIntent = new Intent(this, QR_Display.class);

		if (message == null || message.getTitle() == null) {
			MessageIntent.putExtra(EXTRA_MESSAGE_TITLE, getString(com.pandora_escape.x25_demo.R.string.scan_error_title));
			MessageIntent.putExtra(EXTRA_MESSAGE_BODY, getString(com.pandora_escape.x25_demo.R.string.scan_error_body));
		} else {
			MessageIntent.putExtra(EXTRA_MESSAGE_TITLE, message.getTitle());
			MessageIntent.putExtra(EXTRA_MESSAGE_BODY, message.getBody());
		}

		startActivity(MessageIntent);
	}

	/**
	 * Displays a message on screen by sending an intent to the QR_Display activity
	 *
	 * @param title Title of the message
	 * @param body  Body of the message
	 */
	public void displayMessage(String title, String body) {
		Intent MessageIntent = new Intent(this, QR_Display.class);

		MessageIntent.putExtra(EXTRA_MESSAGE_TITLE, title);
		MessageIntent.putExtra(EXTRA_MESSAGE_BODY, body);

		startActivity(MessageIntent);
	}

	// Activity Lifecycle
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.pandora_escape.x25_demo.R.layout.activity_main);

		sMessagesDBHelper = MessagesDBHelper.getInstance(this);

		// Build ListView
		mMessageListView = (ListView) findViewById(com.pandora_escape.x25_demo.R.id.clueListView);
		mCursor = sMessagesDBHelper.getDiscoveredMessages();
		mMessageCursorAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1,
				mCursor,
				new String[]{MessagesContract.COLUMN_NAME_TITLE},
				new int[]{android.R.id.text1},
				0);
		mMessageListView.setAdapter(mMessageCursorAdapter);

		mMessageListView.setOnItemClickListener(mMessageClickedHandler);

		mContentLayout = (LinearLayout) findViewById(com.pandora_escape.x25_demo.R.id.content_layout);
		mSplashImageView = (ImageView) findViewById(com.pandora_escape.x25_demo.R.id.splash_image_view);
		mLoadingImageView = (ImageView) findViewById(com.pandora_escape.x25_demo.R.id.spinner_image_view);
		mSpinnerAnimation = (AnimationDrawable) mLoadingImageView.getDrawable();

		mContentLayout.setVisibility(View.GONE);

		mSplashImageView.setAlpha((float) 1);
	}

	/**
	 * Dispatch onStart() to all fragments.  Ensure any created loaders are
	 * now started.
	 */
	@Override
	protected void onStart() {
		super.onStart();

		mLoadingImageView.setVisibility(View.GONE);

		Runnable contentFaceIn = new Runnable() {
			@Override
			public void run() {
				mContentLayout.setAlpha(0);
				mContentLayout.setVisibility(View.VISIBLE);
				mContentLayout.animate().alpha(1).setDuration(CONTENT_ANIM_FADE).start();
			}
		};

		if (mFirstRun) {
			mContentLayout.setVisibility(View.GONE);
			mContentLayout.setAlpha(0);

			mSplashImageView.animate().scaleX(1).scaleY(1).setDuration(2000).start();

			mSplashImageView.animate().setStartDelay(SPLASH_ANIM_STATIC).alpha((float) 0.1)
					.setDuration(SPLASH_ANIM_FADE).withStartAction(contentFaceIn).start();
		} else {
			contentFaceIn.run();
		}



		mFirstRun = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Liberate resources
		mCursor.close();
		sMessagesDBHelper.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(com.pandora_escape.x25_demo.R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		Cursor cursor;

		switch (id) {
			case R.id.solve:
				Intent intentSolve = new Intent(this,QR_Display.class);
				intentSolve.putExtra(EXTRA_MESSAGE_TITLE, "Solution to the puzzle");
				intentSolve.putExtra(EXTRA_MESSAGE_BODY, "Password A: 312101\nPassword B: 312102\nPassword C: 312103");
				startActivity(intentSolve);
				break;
			case R.id.populate:
				// Database
				sMessagesDBHelper.addAllMessages();

				cursor = sMessagesDBHelper.getDiscoveredMessages();
				mMessageCursorAdapter.changeCursor(cursor);
				mMessageCursorAdapter.notifyDataSetChanged();
				mCursor = cursor;
				break;
			case R.id.delete_msgs:
				// Database
				sMessagesDBHelper.removeAllMessages();

				cursor = sMessagesDBHelper.getDiscoveredMessages();
				mMessageCursorAdapter.changeCursor(cursor);
				mMessageCursorAdapter.notifyDataSetChanged();
				mCursor = cursor;
				break;
			case R.id.action_about:
				//Intent intent = new Intent(this,SettingsActivity.class);
				Intent intentAbout = new Intent(this,AboutActivity.class);
				startActivity(intentAbout);
				break;
		}

		return id == com.pandora_escape.x25_demo.R.id.action_about || super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == QR_SCAN_REQUEST_CODE) {

			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");

				sMessagesDBHelper.addDiscoveredMessage(contents);

				Cursor cursor = sMessagesDBHelper.getDiscoveredMessages();
				mMessageCursorAdapter.changeCursor(cursor);
				mMessageCursorAdapter.notifyDataSetChanged();
				mCursor = cursor;

				Message message = MessagesDBHelper.getMessage(contents);

				displayMessage(message);
			}
/*            if(resultCode == RESULT_CANCELED){
                //handle cancel
            }*/
		}
	}
}
