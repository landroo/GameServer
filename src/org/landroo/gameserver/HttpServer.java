package org.landroo.gameserver;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HttpServer extends NanoHTTPD
{
	private static final String TAG = "HttpServer";
	
	private AmobaServer amobaServer;
	private Handler handler;
	private File homeDir;
	
	public boolean httpLog = true;
	public boolean amobaLog = true;
	
	private Format formatter = new SimpleDateFormat("HH:mm:ss");
	
	public HttpServer(int port, Handler h, String wwwroot) throws IOException
	{
		super(port, new File("."));
		
		handler = h;
		amobaServer = new AmobaServer(h);
		
		homeDir = new File(wwwroot);
	}

	public Response serve(String uri, String method, Properties header, Properties parms, Properties files)
	{
		if(httpLog)
		{
			// TODO
			Date d = new Date();
			byte[] bytes = method.getBytes();
			boolean bText = true;
			for(int i = 0; i < bytes.length; i++)
			{
				if(bytes[i] < 32 || bytes[i] > 'z')
				{
					bText = false;
					break;
				}
			}
			
			if(bText)
			{
				showLog(formatter.format(d.getTime()) + " HTTP " + method + "->" + uri + "->" + parms);
			}
			else
			{
				String log = formatter.format(d.getTime()) + " HTTP (";
				for(int i = 0; i < bytes.length; i++)
					log += String.format("%X", bytes[i]) + " ";
				showLog(log + ")");
			}
		}
		
		if(parms.getProperty("game") != null)
		{
			String msg = "&";
			try
			{
				String game = parms.getProperty("game");
				String name = URLDecoder.decode(parms.getProperty("name"), "ISO-8859-1");
				String command = URLDecoder.decode(parms.getProperty("command"), "ISO-8859-1");
				if(game.equals("amoba")) msg = this.amobaServer.processCommand(name, command);
			}
			catch (UnsupportedEncodingException e)
			{
				Log.i(TAG, e.getMessage());
			}
			
			return new NanoHTTPD.Response(HTTP_OK, MIME_HTML, msg);
		}
		
		return serveFile( uri, header, homeDir, true ); 
	}
	
    private void showLog(String sAlert)
    {
        Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("log", sAlert);
        msg.setData(b);
        handler.sendMessage(msg);
    }
    
    public void setLog(boolean httplog, boolean amobalog)
    {
    	this.httpLog = httplog;
    	this.amobaServer.gameLog = amobalog;
    }
}
