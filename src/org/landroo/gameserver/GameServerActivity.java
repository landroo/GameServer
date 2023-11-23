package org.landroo.gameserver;

/*
 This is a simple http webserver with embedded game servlets.
 This server contains a servlet, which is handle the players actions.
 - You can put it in the launcher as a widget.
 - You can set the log type.
 - You can set the port of the server.
 - You can use it as an own game and web server if you change the index.html in the SDCard/gameServerRoot.
 After start it shows the address, you can set this address in the amoba setting and you can play with your friends.
 I suggest to turn on in the phone developer setting the stay awake option, and put the phone on charger.
 If you have a question, drop me a mail landroo9@gmail.com :)

 v 1.1
 - Some small big fix.
 - Bug fix url encode.
 - Add hungarian language.

 v 1.2
 - Some small big fix.
 - Bug fix url encode.
 - Add hungarian language.
 - Add direct connect support between players.

 Ez egy egyszerű http web kiszolgáló beépített játék servlettel.
 A beépített servlet kezeli a játékosok műveleteit.
 - Kihelyezhető minialkalmazásként a képernyőre.
 - Beállítható a loggolás típusa.
 - Használható saját játék és web kiszolgálóként ha lecseréljük az index.html állományt a SDCard/gameServerRoot könyvtárban.
 Indítás után kiírja a a kiszolgál az IP címét amelyet beállíthatunk az amőba játékban és ezen keresztül játszhatunk a társainkkal.
 Javaslom állítsuk be az android eszközön a Fejlesztőknek menü alatt a Maradjon bekapcsolva kapcsolót és csatlakoztassuk a készüléket a töltőre.
 Ha kérdés merülne fel küldj egy levelet a landroo9@gmail.com címre :)

 v 1.1
 - Néhány apróbb hiba javítása.
 - Hibajavítás az URL kódolásnál.
 - Magyar nyelv támogatás hozzáadása.

 v 1.2
 - Néhány hibajavítás.
 - Hibajavítás az URL kódolásnál.
 - Közvetlen kapcsolat támogatása a játékosok között.
 */
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class GameServerActivity extends Activity implements ScrollViewListener
{
	private static final String TAG = "GameServerActivity";

	private TextView headView;
	private TextView logView;
	private ScrollView scrollView;

	private boolean scrollog = true;

	private int iLang = 0;
	private String lastLog = "";

	private Messenger mService = null;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private boolean mIsBound = false;

	private ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			mService = new Messenger(service);
			try
			{
				Message msg = Message.obtain(null, HttpServerService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			}
			catch (Exception ex)
			{
				// In this case the service has crashed before we could even do
				// anything with it
			}
		}

		public void onServiceDisconnected(ComponentName className)
		{
			mService = null;
		}
	};

	class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case HttpServerService.MSG_SET_INT_VALUE:
				break;
			case HttpServerService.MSG_SET_STRING_VALUE:
				String log = msg.getData().getString("log");
				//Log.i(TAG, log);
				lastLog = log;
				logView.setText(lastLog);
				if (scrollog) scrollView.fullScroll(View.FOCUS_DOWN);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		headView = (TextView) this.findViewById(R.id.info_txt);
		logView = (TextView) this.findViewById(R.id.log_txt);

		scrollView = (ScrollView) this.findViewById(R.id.scrollView1);

		if (bindService(new Intent(this, HttpServerService.class), mConnection, Context.BIND_AUTO_CREATE)) mIsBound = true;

		String sLang = Locale.getDefault().getDisplayLanguage();
		if (sLang.equals("magyar")) iLang = 1;

		restoreMe(savedInstanceState);

		CheckIfServiceIsRunning();

		SharedPreferences settings = getSharedPreferences("org.landroo.gameserver_preferences", MODE_PRIVATE);
		String port = settings.getString("port", "8080");

		if(checkWifi())
			if (iLang == 0) headView.setText("Game server run at: http://" + getLocalIpAddress() + ":" + port);
			else headView.setText("Game server fut a : http://" + getLocalIpAddress() + ":" + port + " címen");
		else
			if (iLang == 0) headView.setText("Wifi not connected!");
			else headView.setText("A Wifi nincs bekapcsolva!");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
		case R.id.menu_start:
			startService(new Intent(GameServerActivity.this, HttpServerService.class));
			doBindService();
			return true;

		case R.id.menu_stop:
			stopService(new Intent(GameServerActivity.this, HttpServerService.class));
			doUnbindService();
			return true;

		case R.id.menu_settings:
			Intent SettingsIntent = new Intent(this, SettingsScreen.class);
			startActivity(SettingsIntent);
			return true;

		case R.id.menu_exit:
			doUnbindService();
			this.finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if (mIsBound)
		{
			// Detach our existing connection.
			doUnbindService();
			mIsBound = false;
		}
	}

	public void onScrollChanged(ScrollChange scrollView, int x, int y, int oldx, int oldy)
	{
	}

	@Override
	public synchronized void onResume()
	{
		SharedPreferences settings = getSharedPreferences("org.landroo.gameserver_preferences", MODE_PRIVATE);

		this.scrollog = settings.getBoolean("scrollog", true);

		doBindService();

		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString("lastlog", lastLog);
	}

	private void restoreMe(Bundle state)
	{
		if (state != null)
		{
			lastLog = state.getString("lastlog");
		}
	}

	private void CheckIfServiceIsRunning()
	{
		// If the service is running when the activity starts, we want to
		// automatically bind to it.
		if (HttpServerService.isRunning())
		{
			doBindService();
		}
	}

	private void sendMessageToService(int intvaluetosend)
	{
		if (mIsBound)
		{
			if (mService != null)
			{
				try
				{
					Message msg = Message.obtain(null, HttpServerService.MSG_SET_INT_VALUE, intvaluetosend, 0);
					msg.replyTo = mMessenger;
					mService.send(msg);
				}
				catch (Exception ex)
				{
				}
			}
		}
	}

	void doBindService()
	{
		bindService(new Intent(this, HttpServerService.class), mConnection, Context.BIND_AUTO_CREATE);
		startService(new Intent(this, HttpServerService.class));
		mIsBound = true;
	}

	void doUnbindService()
	{
		if (mIsBound)
		{
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (mService != null)
			{
				try
				{
					Message msg = Message.obtain(null, HttpServerService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				}
				catch (Exception ex)
				{
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	public String getLocalIpAddress()
	{
		String res = "";
		try
		{
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
					{
						res += " " + inetAddress.getHostAddress();
					}
				}
			}
		}
		catch (Exception ex)
		{
			Log.i(TAG, "Error reading network status!");
		}

		// Log.i(TAG, res);

		return res;
	}
	
	private boolean checkWifi()
	{
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) return true;
		
		return false;
	}
}
