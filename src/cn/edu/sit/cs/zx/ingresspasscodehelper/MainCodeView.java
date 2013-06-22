package cn.edu.sit.cs.zx.ingresspasscodehelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.layout;
import android.R.id;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import cn.edu.sit.cs.zx.ingresspasscodehelper.util.SystemUiHider;

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
	private ArrayList<HashMap<String, String>> mCodesMap;
	private ClipboardManager clipboard = null;
    private Calendar c;  

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main_code_view);
		initSysUi();

		//系统的初始化
		mCodeList = (ListView) findViewById(R.id.listView1);
		mCodesMap = new ArrayList<HashMap<String, String>>();
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

		startService(new Intent("cn.edu.sit.cs.zx.ingresspasscodehelper.PasscodeFetchService"));
	}

	private void initSysUi() {
		//原有系统自带的初始化
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

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
	}

    /**
     * 保存字符串到剪贴板，并给出提示
     * @param tmpCodeString 提示内容
     */
	private void SaveToClipboard(String tmpCodeString) {
		Log.d(TAG, tmpCodeString);
		ClipData textCd = ClipData.newPlainText("kkk", tmpCodeString);
		clipboard.setPrimaryClip(textCd);
		Toast.makeText(getApplicationContext(), "拷贝到剪贴板 " + tmpCodeString,
				Toast.LENGTH_LONG).show();
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
	protected void onDestroy() {
		// TODO 是否自动停止，或者加配置进行控制
		//stopService(new Intent("cn.edu.sit.cs.zx.ingresspasscodehelper.PasscodeFetchService"));
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main_code_menu, menu);
		MenuItem item = menu.findItem(R.id.activity_main_code_menu_background_notify1);
		if(item!=null)
		{
			boolean isStart = getSharedPreferences("PasscodeConfig",MODE_PRIVATE).getBoolean("isServiceRun", true);
			item.setChecked(isStart);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId()==R.id.activity_main_code_menu_background_notify1)
		{
			if(item.isChecked())
			{
				item.setChecked(false);
				stopService(new Intent("cn.edu.sit.cs.zx.ingresspasscodehelper.PasscodeFetchService"));
			}
			else
			{
				item.setChecked(true);
				startService(new Intent("cn.edu.sit.cs.zx.ingresspasscodehelper.PasscodeFetchService"));
			}
			getSharedPreferences("PasscodeConfig",MODE_PRIVATE).edit().putBoolean("isServiceRun", item.isChecked()).commit();
		}
		return super.onOptionsItemSelected(item);
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
				Log.d(TAG,"time of code:"+iteMap.get("time"));
				
				Long long1 = Long.parseLong(iteMap.get("time"));
				int m = (int) ((date.getTime()/1000 - long1)/60)+1;
				String timeDesc = String.valueOf(m % 60)+"分钟前                        ";
				if((m-m % 60)/60>0)
					timeDesc=String.valueOf((m-m % 60)/60)+"小时"+timeDesc;
				iteMap.put("desc", timeDesc+" [Coxxs指数："+iteMap.get("point")+"]                "+iteMap.get("item"));
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
			//AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
			//Account[] list = manager.getAccounts();
			try {
				HttpClient httpclient = new DefaultHttpClient();
				initHttpHeader(httpGet);

				httpResponse = httpclient.execute(httpGet);
				// Log.d(TAG,EntityUtils.toString(httpResponse.getEntity()));

				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					// 第三步，使用getEntity方法活得返回结果
					String result = EntityUtils.toString(httpResponse
							.getEntity());
					Log.d(TAG, result);

					processHttpResult(result);
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//提示UI线程绑定数据
			mHideHandler.sendEmptyMessage(1);

		}

		private void processHttpResult(String result) {
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
					iteMap.put("time", object.getString("time"));
					iteMap.put("point", object.getString("point"));
					mCodesMap.add(iteMap);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void initHttpHeader(HttpGet httpGet) {
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
	
   
    /**
     * 用来判断服务是否运行.
     * @param context
     * @param className 判断的服务名字
     * @return true 在运行 false 不在运行
     */
    public static boolean isServiceRunning(Context mContext,String className) {
        boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager)
		mContext.getSystemService(Context.ACTIVITY_SERVICE); 
        List<ActivityManager.RunningServiceInfo> serviceList 
        = activityManager.getRunningServices(30);
       if (!(serviceList.size()>0)) {
            return false;
        }
        for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }
      
	
}
