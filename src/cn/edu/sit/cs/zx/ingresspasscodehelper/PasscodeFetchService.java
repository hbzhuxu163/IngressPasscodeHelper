package cn.edu.sit.cs.zx.ingresspasscodehelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.integer;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class PasscodeFetchService extends Service {
	static String TAG = "PasscodeFetchService";
	private static int MOOD_NOTIFICATIONS = 1;
	private Notification mN;
	private NotificationManager mNm;
	private Intent mI;
	private PendingIntent mP;
	private List<String> mCodesHistory;
	private Boolean mIsFetch = false;
	private int mSleepCount;

	// Binder given to clients
	private final IBinder mBinder = new LocalBinder() {
		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply,
				int flags) throws RemoteException {
			return super.onTransact(code, data, reply, flags);
		}
	};

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		PasscodeFetchService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return PasscodeFetchService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public void onDestroy() {
		mNm.cancel(MOOD_NOTIFICATIONS);
		mIsFetch = false;
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		// 预防重复通知的结构
		mCodesHistory = new ArrayList<String>();
		CodeHistoryLoad();

		// 初始化通知
		initNotify();
		// 开启新的线程从服务器获取数据
		Thread getArticlesThread = new Thread(null, mTask, "getNewPasscodes");
		getArticlesThread.start();

		super.onCreate();
	}

	private void initNotify() {
		mN = new Notification();
		// img
		mN.icon = R.drawable.ic_launcher;
		// text
		mN.tickerText = getString(R.string.service_passcode_fetch_notify_title);
		// audio
		mN.defaults = Notification.DEFAULT_SOUND;
		// click and auto_cancel
		mN.flags = Notification.FLAG_AUTO_CANCEL;
		mNm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// 点击顶部状态栏的消息后，welcomeactivity为需要跳转的页面
		mI = new Intent(this, MainCodeView.class);
		mP = PendingIntent.getActivity(this, 0, mI, 0);
	}

	Runnable mTask = new Runnable() {

		@Override
		public void run() {
			Log.d(TAG, "start connect webpage");
			mIsFetch = true;
			while (mIsFetch) {
				if(mSleepCount % 12==0)
				{
					fetchPasscode();
					mSleepCount=0;
				}
				try {
					Thread.sleep(5000);
					mSleepCount++;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		private void fetchPasscode() {
			
			ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo= conMan.getActiveNetworkInfo();
			if(networkInfo== null || !networkInfo.isConnected())
				return;
			
			String url = "http://gressbot.com/?mod=passcodeapi";
			// 第一步，创建HttpGet对象
			HttpGet httpGet = new HttpGet(url);
			// 第二步，使用execute方法发送HTTP GET请求，并返回HttpResponse对象
			HttpResponse httpResponse;
			
			Log.d(TAG,"start fetch");
			// AccountManager manager = (AccountManager)
			// getSystemService(ACCOUNT_SERVICE);
			// Account[] list = manager.getAccounts();
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
		}

		private void processHttpResult(String result) {
			try {
				JSONObject param = new JSONObject(result);
				JSONArray codeArray = param.getJSONArray("result");
				Log.d(TAG,"finish fetch. total:"+String.valueOf(codeArray.length()));
				// TODO:此处可能会有逻辑上的异常，比如老code重复发notify，需要仔细考虑
				if (codeArray.length() == 0) {
					mCodesHistory.clear();
					CodeHistorySave();
					return;
				}
				// 处理code
				int codeCount = 0;
				for (int i = 0; i < codeArray.length(); i++) {
					JSONObject object = codeArray.getJSONObject(i);
					// 检查是否有新code
					if (CodeHistryCheck(object.getString("code")))
						continue;
					mCodesHistory.add(object.getString("code"));
					codeCount++;
				}
				if (codeCount > 0) {
					CodeHistorySave();
					showNotification("New Code Comming +"
							+ String.valueOf(codeCount));
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

	private void showNotification(String contentText) {
//		mN =new Notification.Builder(PasscodeFetchService.this)
//        .setContentTitle(getString(R.string.service_passcode_fetch_notify_title))
//        .setContentText(contentText)
//        .setSmallIcon(R.drawable.ic_launcher)
//        .build();
		mN.setLatestEventInfo(PasscodeFetchService.this,
				getString(R.string.service_passcode_fetch_notify_title),
				contentText, mP);
		mNm.notify(MOOD_NOTIFICATIONS, mN);
	}

	private Boolean CodeHistryCheck(String code) {
		for (String s : mCodesHistory) {
			if (s.equals(code))
				return true;
		}
		return false;
	}

	private void CodeHistorySave() {
		StringBuilder csvList = new StringBuilder();
		for (String s : mCodesHistory) {
			csvList.append(s);
			csvList.append(",");
		}
		SharedPreferences preferences = getSharedPreferences("codeHistory",
				MODE_PRIVATE);
		Editor sharedPreferencesEditor = preferences.edit();
		sharedPreferencesEditor.putString("myList", csvList.toString());
		sharedPreferencesEditor.commit();
	}

	private void CodeHistoryLoad() {
		SharedPreferences sharedPreferences = getSharedPreferences(
				"codeHistory", MODE_PRIVATE);
		String csvList = sharedPreferences.getString("myList", "");
		String[] items = csvList.split(",");
		if (items.length <= 0)
			return;
		mCodesHistory.clear();
		for (int i = 0; i < items.length; i++) {
			mCodesHistory.add(items[i]);
		}
	}

}
