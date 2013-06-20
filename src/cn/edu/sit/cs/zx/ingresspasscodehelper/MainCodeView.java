package cn.edu.sit.cs.zx.ingresspasscodehelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.edu.sit.cs.zx.ingresspasscodehelper.util.SystemUiHider;

import android.R.anim;
import android.R.bool;
import android.R.integer;
import android.R.string;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainCodeView extends Activity {
	private static final String TAG = "MainCodeView";
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	private ListView mCodeList;
	private List<String> mCodesHistory;
	private ArrayList<HashMap<String, String>> mCodesMap;
	private ClipboardManager clipboard = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main_code_view);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		mCodeList = (ListView) findViewById(R.id.listView1);
		mCodesHistory = new ArrayList<String>();
		mCodesMap = new ArrayList<HashMap<String, String>>();

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(
				mDelayHideTouchListener);

		// Log.d(TAG,"-----------------" );
		clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

		mCodeList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View arg1,
					int arg2, long arg3) {
				TextView codeView = (TextView)arg1.findViewById(android.R.id.text1);
				String tmpCodeString = codeView.getText().toString();
				Log.d(TAG,""+String.valueOf(mCodesMap.get(arg2).get("item")));
				SaveToClipboard(tmpCodeString);
				try {
					PackageManager packageManager = MainCodeView.this
							.getPackageManager();
					Intent intent = new Intent();
					intent = packageManager
							.getLaunchIntentForPackage("com.nianticproject.ingress");
					getApplicationContext().startActivity(intent);
				} catch (Exception e) {
					Log.i(TAG, e.toString());
				}
				return false;
			}

		});

		CodeHistoryLoad();
	}

	private void SaveToClipboard(String tmpCodeString) {
		Log.d(TAG, tmpCodeString);
		ClipData textCd = ClipData.newPlainText("kkk", tmpCodeString);
		clipboard.setPrimaryClip(textCd);
		Toast.makeText(getApplicationContext(), "拷贝到剪贴板 " + tmpCodeString,
				Toast.LENGTH_LONG).show();
	}
	
	private void NotifyCode(String code) {
		CodeHistorySave();

	}
	
	
	private Boolean CodeHistryCheck(String code)
	{
		for(String s : mCodesHistory){
		      if(s.equals(code))
		      return true;
		}
		return false;
	}
	
	private void CodeHistorySave() {
		StringBuilder csvList = new StringBuilder();
		for(String s : mCodesHistory){
		      csvList.append(s);
		      csvList.append(",");
		}
		SharedPreferences preferences= getSharedPreferences("codeHistory", MODE_PRIVATE);
		Editor sharedPreferencesEditor = preferences.edit();
		sharedPreferencesEditor.putString("myList", csvList.toString());
		sharedPreferencesEditor.commit();
	}
	
	private void CodeHistoryLoad() {
		SharedPreferences sharedPreferences= getSharedPreferences("codeHistory", MODE_PRIVATE);
		String csvList = sharedPreferences.getString("myList","");
		String[] items = csvList.split(",");
		if(items.length<=0)
			return;
		mCodesHistory.clear();
		for(int i=0; i < items.length; i++){
			mCodesHistory.add(items[i]);     
		}
	}
	
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// 联网获取信息
		new Thread(mHideRunnable).start();
		Log.d(TAG, "start fetch code from web page");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main_code_menu, menu);
		return true;
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			for (HashMap<String, String> iteMap : mCodesMap) { 
				Date date = new Date();
				int m = (int) ((date.getTime()/1000 - Long.parseLong(iteMap.get("time")))/60)+1;
				iteMap.put("desc", String.valueOf(m)+"分钟前 "+iteMap.get("item"));
	         } 

			// 操作界面
//			mCodeList.setAdapter(new SimpleAdapter(getApplicationContext(),
//					mCodesMap, android.R.layout.simple_list_item_2,
//					new String[] { "code", "item" }, new int[] {
//							android.R.id.text1, android.R.id.text2 }));

			mCodeList.setAdapter(new SimpleAdapter(getApplicationContext(),
			mCodesMap, android.R.layout.simple_list_item_2,
			new String[] { "code", "desc" }, new int[] {
					android.R.id.text1, android.R.id.text2 }));

//			for (HashMap<String, String> codeInfo : mCodesMap) {
//				if(CodeHistryCheck(codeInfo.get("item")))
//				{
//					//notify
//				}
//			}
			//Log.d(TAG, String.valueOf(mCodes.length));
			if (mCodesMap.size() > 0) {
				SaveToClipboard(mCodesMap.get(0).get("item"));
			}
			super.handleMessage(msg);
		}
	};

	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			// mSystemUiHider.hide();
			Log.d(TAG, "start connect webpage");
			String url = "http://gressbot.com/?mod=passcodeapi";
			// 第一步，创建HttpGet对象
			HttpGet httpGet = new HttpGet(url);
			// 第二步，使用execute方法发送HTTP GET请求，并返回HttpResponse对象
			HttpResponse httpResponse;
			try {
				HttpClient httpclient = new DefaultHttpClient();
				httpGet.setHeader(
						"User-Agent",
						"Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.6) Gecko/20100625Firefox/3.6.6 Greatwqs");
				httpGet.setHeader("Accept",
						"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				httpGet.setHeader("Accept-Language", "zh-cn,zh;q=0.5");
				httpGet.setHeader("Host", "gressbot.com");
				httpGet.setHeader("Accept-Charset",
						"ISO-8859-1,utf-8;q=0.7,*;q=0.7");
				httpGet.setHeader("Referer", "http://gressbot.com");

				httpResponse = httpclient.execute(httpGet);
				// Log.d(TAG,EntityUtils.toString(httpResponse.getEntity()));

				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					// 第三步，使用getEntity方法活得返回结果
					String result = EntityUtils.toString(httpResponse
							.getEntity());
					Log.d(TAG, result);

					try {
						JSONObject param = new JSONObject(result);
						JSONArray codeArray = param.getJSONArray("result");
						//mCodes = new String[codeArray.length()];
						mCodesMap.clear();

						for (int i = 0; i < codeArray.length(); i++) {
							JSONObject object = codeArray.getJSONObject(i);
							HashMap<String, String> iteMap = new HashMap<String, String>();
							iteMap.put("code", object.getString("code"));
							iteMap.put("item", object.getString("item"));
							mCodesMap.add(iteMap);
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			mHideHandler.sendEmptyMessage(1);

		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		// mHideHandler.removeCallbacks(mHideRunnable);
		// mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
}
