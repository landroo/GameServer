package org.landroo.gameserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.RemoteViews;

public class HttpServerService extends Service
{
	private static final String TAG = "HttpServerService";
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SET_INT_VALUE = 3;
	public static final int MSG_SET_STRING_VALUE = 4;

	private String[] logText;
	private int logSize = 128;
	private int logLine = 0;
	private String ip;
	private int port = 8080;
	private int[] allWidgetIds;
	private HttpServer http = null;
	private int iLang = 0;

	private Format formatter = new SimpleDateFormat("HH:mm:ss");

	private NotificationManager notMan;
	private static boolean isRunning = false;

	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	// Handler of incoming messages from clients.
	class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				sendMessageToUI();
				sendMessageToWidget();
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_SET_INT_VALUE:
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	// handling log messages
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			String log = msg.getData().getString("log");
			if (log != null)
			{
				addLog(log);

				sendMessageToUI();
				sendMessageToWidget();
			}
		}
	};

	@Override
	public void onCreate()
	{
		// Log.i(TAG, "onCreate");
		super.onCreate();

		String sLang = Locale.getDefault().getDisplayLanguage();
		if (sLang.equals("magyar")) iLang = 1;

		if (http == null) startServer();

		showNotification();

		Date d = new Date();
		if (iLang == 0) addLog(formatter.format(d.getTime()) + " server started");
		else addLog(formatter.format(d.getTime()) + " server elindítva");
		sendMessageToUI();
		sendMessageToWidget();

		isRunning = true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Log.i("LocalService", "onStartCommand " + startId + ": " + intent);
		setWidgets(intent);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		// Log.i(TAG, "onStart");
		setWidgets(intent);

		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// Log.i(TAG, "onBind called");
		startServer();

		return mMessenger.getBinder();
	}

	@Override
	public boolean stopService(Intent intent)
	{
		// Log.i(TAG, "stopService");
		stopServer();

		return true;
	}

	@Override
	public void onDestroy()
	{
		// Log.i(TAG, "onDestroy");
		super.onDestroy();
		stopServer();
		notMan.cancel(R.string.service_stopped); // Cancel the persistent
													// notification.
		isRunning = false;
	}

	// handle log fi-lo list
	private void addLog(String textLine)
	{
		if (logLine == logSize)
		{
			logLine--;
			for (int i = 0; i < logLine; i++)
				logText[i] = logText[i + 1];
		}
		logText[logLine++] = textLine;
	}

	//
	private void stopServer()
	{
		if (http != null)
		{
			http.stop();
			http = null;

			Date d = new Date();
			if (iLang == 0) addLog(formatter.format(d.getTime()) + " server stopped");
			else addLog(formatter.format(d.getTime()) + " server megállítva");
			sendMessageToUI();
			sendMessageToWidget();
		}
		this.stopSelf();
	}

	//
	private void startServer()
	{
		SharedPreferences settings = getSharedPreferences("org.landroo.gameserver_preferences", MODE_PRIVATE);
		port = Integer.parseInt(settings.getString("port", "8080"));

		boolean b = settings.getBoolean("httplog", true);
		boolean httplog = b;
		b = settings.getBoolean("amobalog", true);
		boolean amobalog = b;

		try
		{
			logSize = Integer.parseInt(settings.getString("logbuffer", "128"));
		}
		catch (Exception ex)
		{
			Log.i(TAG, ex.getMessage());
		}

		logLine = 0;
		logText = new String[logSize];

		ip = getLocalIpAddress();
		if (iLang == 0)
		{
			if (ip.equals("")) addLog("Please connect to internet!");
			else addLog("Game server at http://" + ip + ":" + port);
		}
		else
		{
			if (ip.equals("")) addLog("Kérem kapcsolódjon az internetre!");
			else addLog("Game server a http://" + ip + ":" + port + " címen");
		}

		String rootPath = Environment.getExternalStorageDirectory().getPath() + "/gameServerRoot";
		buildHttpRoot(rootPath);
		try
		{
			http = new HttpServer(port, handler, rootPath);
			http.setLog(httplog, amobalog);
		}
		catch (IOException ioe)
		{
			Log.i("TAG", "Couldn't start server: " + ioe);
		}
	}

	// Local IP Address
	private String getLocalIpAddress()
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

	// build Http root
	private void buildHttpRoot(String rootPath)
	{
		String[] str = { "mkdir", rootPath };

		try
		{
			Process ps = Runtime.getRuntime().exec(str);
			try
			{
				ps.waitFor();
			}
			catch (InterruptedException e)
			{
				Log.i(TAG, "InterruptedException");
			}

			String sFile = rootPath + "/index.html";
			File f = new File(sFile);
			if (!f.exists())
			{
				copyResourceFile(R.raw.index, rootPath + "/index.html");
				copyResourceFile(R.raw.amoba, rootPath + "/amoba.png");
				copyResourceFile(R.raw.colorizer, rootPath + "/colorizer.png");
				copyResourceFile(R.raw.jewels, rootPath + "/jewels.png");
				copyResourceFile(R.raw.textreader, rootPath + "/textreader.png");
				copyResourceFile(R.raw.enghunbig, rootPath + "/enghunbig.png");
				copyResourceFile(R.raw.enghunmini, rootPath + "/enghunmini.png");
				copyResourceFile(R.raw.esphunmini, rootPath + "/esphunmini.png");
				copyResourceFile(R.raw.frahunbig, rootPath + "/frahunbig.png");
				copyResourceFile(R.raw.gerhunmini, rootPath + "/gerhunmini.png");
				copyResourceFile(R.raw.gameserver, rootPath + "/gameserver.png");
				copyResourceFile(R.raw.piper, rootPath + "/piper.png");
			}
		}
		catch (IOException e)
		{
			Log.i(TAG, "InterruptedException");
		}
	}

	// copy web files
	private void copyResourceFile(int rid, String targetFile) throws IOException
	{
		InputStream fin = ((Context) this).getResources().openRawResource(rid);
		FileOutputStream fos = new FileOutputStream(targetFile);

		int length;
		byte[] buffer = new byte[1024 * 32];
		while ((length = fin.read(buffer)) != -1)
			fos.write(buffer, 0, length);
		fin.close();
		fos.close();
	}

	// get widget IDs
	private void setWidgets(Intent intent)
	{
		if (intent != null)
		{
			// send log to widget
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
			allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			if (allWidgetIds != null)
			{
				for (int widgetId : allWidgetIds)
				{
					RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(),
							R.layout.widgetlayout);

					// Set the text
					String sLog = "";
					int st = logLine - 50 < 0 ? 0 : logLine - 50;
					for (int i = st; i < logLine; i++)
						sLog += logText[i] + "\n";
					remoteViews.setTextViewText(R.id.update, sLog);

					// Register an onClickListener
					Intent clickIntent = new Intent(this.getApplicationContext(), WidgetProvider.class);

					clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
					clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

					PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, clickIntent,
							PendingIntent.FLAG_UPDATE_CURRENT);
					remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
					appWidgetManager.updateAppWidget(widgetId, remoteViews);

					// Log.i("TAG", "widgetId: " + widgetId);
				}
			}
		}

		return;
	}

	// notification icon
	private void showNotification()
	{
		notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		CharSequence text = getText(R.string.service_started);
		Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, GameServerActivity.class), 0);
		notification.setLatestEventInfo(this, getText(R.string.service_label), text, contentIntent);
		notMan.notify(R.string.service_started, notification);
	}

	//
	public static boolean isRunning()
	{
		return isRunning;
	}

	// send message to all clients
	private void sendMessageToUI()
	{
		for (int i = mClients.size() - 1; i >= 0; i--)
		{
			try
			{
				String sLog = "";
				int st = logLine - 50 < 0 ? 0 : logLine - 50;
				for (int j = st; j < logLine; j++)
					sLog += logText[j] + "\n";

				// Send data as a String
				Bundle b = new Bundle();
				b.putString("log", sLog);
				Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
				msg.setData(b);
				mClients.get(i).send(msg);
			}
			catch (Exception ex)
			{
				// The client is dead. Remove it from the list; we are going
				// through the list from back to front so this is safe to do
				// inside the loop.
				mClients.remove(i);
			}
		}
	}

	// send message to all widgets
	private void sendMessageToWidget()
	{
		if (allWidgetIds != null)
		{
			for (int widgetId : allWidgetIds)
			{
				if (widgetId != 0)
				{
					RemoteViews remoteViews = new RemoteViews(HttpServerService.this.getApplicationContext().getPackageName(),
							R.layout.widgetlayout);
					String sLog = "";
					int st = logLine - 50 < 0 ? 0 : logLine - 50;
					for (int i = st; i < logLine; i++)
						sLog += logText[i] + "\n";
					remoteViews.setTextViewText(R.id.update, sLog);

					AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(HttpServerService.this
							.getApplicationContext());
					appWidgetManager.updateAppWidget(widgetId, remoteViews);
				}
			}
		}

		return;
	}
}
