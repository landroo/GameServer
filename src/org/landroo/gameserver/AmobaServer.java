package org.landroo.gameserver;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Locale;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AmobaServer
{
	private static final String TAG = "AmobaServer";
	private static final String error = "error";
	private static final int TIME_Out = 120000;
	private static final String DROID_NAME = "WebDroid";

	private static final List<AmobaPlayer> mUsers = new ArrayList<AmobaPlayer>();
	
	private Handler handler;
	private Format formatter = new SimpleDateFormat("HH:mm:ss");

	public boolean gameLog = true;

	private int iLang = 0;

	/**
	 * player class
	 * 
	 * @author rkovacs
	 * 
	 */
	public class AmobaPlayer
	{
		public String name;
		public long time;

		public String Steps = "";
		public String Messages = "";
		public String Ivitations = "";
		public String Added = "";
		public String Removed = "";
		public String NewTable = "";
		public String UndoStep = "";
		public String Address = "";

		public AmobaClass amobaClass;

		public AmobaPlayer(String name, long time, String ip)
		{
			this.name = name;
			this.time = time;
			this.Address = ip;
		}
	}

	// Constructor
	public AmobaServer(Handler h)
	{
		handler = h;

		String sLang = Locale.getDefault().getDisplayLanguage();
		if (sLang.equals("magyar")) iLang = 1;

		addUser(DROID_NAME, "");
	}

	// Új felhasználó hozzáadása
	public String addUser(String strNewUserName, String ip)
	{
		boolean bOK = false;
		String strLastUser = "error";

		updateUser("");

		synchronized (this)
		{
			if (!checkUser(strNewUserName))
			{
				for (AmobaServer.AmobaPlayer player : mUsers)
					player.Added += strNewUserName + "&" + ip + ";";

				Date d = new Date();
				AmobaPlayer sUser = new AmobaPlayer(strNewUserName, d.getTime(), ip);
				mUsers.add(sUser);

				strLastUser = strNewUserName;
				bOK = true;

				if (iLang == 0) LogMessage(formatter.format(sUser.time) + " amoba " + strNewUserName + " added");
				else LogMessage(formatter.format(sUser.time) + " amoba " + strNewUserName + " hozzáadva");
			}
		}
		
		if (!bOK)
		{
			Date d = new Date();
			if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + strNewUserName + " not added!");
			else LogMessage(formatter.format(d.getTime()) + " amoba " + strNewUserName + " nincs hozzáadva!");
		}
		return strLastUser;
	}

	// Felhasználó név létezéséne ellenőrzése
	private boolean checkUser(String strName)
	{
		boolean bOK = false;
		String sLabel;

		for (int i = 0; i < mUsers.size(); i++)
		{
			sLabel = mUsers.get(i).name;
			if (sLabel.equals(strName))
			{
				bOK = true;
				// if (iLang == 0) LogMessage(strName + " exist!");
				// else LogMessage(strName + " van már!");
				break;
			}
		}
		if (!bOK)
		{
			Date d = new Date();
			if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + strName + " NOT exist!");
			else LogMessage(formatter.format(d.getTime()) + " amoba " + strName + " még nincs!");
		}
		
		return bOK;
	}

	// delete user
	public String deleteUser(String sName)
	{
		boolean bOK = false;
		String sLabel = "";
		String sRes = "7";
		long lTime;

		synchronized (this)
		{
			for (int i = 0; i < mUsers.size(); i++)
			{
				sLabel = mUsers.get(i).name;
				if (sLabel.equals(sName))
				{
					lTime = mUsers.get(i).time;
					bOK = true;
					mUsers.remove(i);
					for (AmobaServer.AmobaPlayer player : mUsers)
						player.Removed += sName + ";";
					if (iLang == 0) LogMessage(formatter.format(lTime) + " amoba " + sName + " removed");
					else LogMessage(formatter.format(lTime) + " amoba " + sName + " eltávolítva");
					break;
				}
			}
		}
		if (!bOK)
		{
			Date d = new Date();
			if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + sName + " NOT exist!");
			else LogMessage(formatter.format(d.getTime()) + " amoba " + sName + " nem található!");
		}

		return sRes;
	}

	// delete or update user and return the steps and messages
	public String updateUser(String strName)
	{
		Date newDate = new Date();
		boolean bOK = false;
		long lDiff = 0;

		AmobaPlayer player;

		String sRet = "";
		String sLog = "";

		synchronized (this)
		{
			do
			{
				bOK = false;
				for (int i = 0; i < mUsers.size(); i++)
				{
					player = mUsers.get(i);
					lDiff = newDate.getTime() - player.time;
					if (lDiff > TIME_Out && !player.name.equals(this.DROID_NAME))
					{
						bOK = true;
						for (AmobaServer.AmobaPlayer user : mUsers)
							user.Removed += player.name + ";";
						mUsers.remove(i);
						if (iLang == 0) LogMessage(formatter.format(newDate.getTime()) + " amoba " + player.name + " removed because " + lDiff + " > " + TIME_Out);
						else LogMessage(formatter.format(newDate.getTime()) + " amoba " + player.name + " eltávolítva " + lDiff + " > " + TIME_Out);
					}
				}
			}
			while (bOK);

			if (!strName.equals(""))
			{
				for (int i = 0; i < mUsers.size(); i++)
				{
					player = mUsers.get(i);
					if (player.name.equals(strName))
					{
						lDiff = player.time;
						player.time = newDate.getTime();
						/*
						if (iLang == 0) 
							LogMessage(formatter.format(newDate.getTime()) + " " + strName + " updated from " + formatter.format(lDiff));
						else
							LogMessage(formatter.format(newDate.getTime()) + " " + strName + " frissítés " + formatter.format(lDiff) + " ről");
						*/
						// steps roli;5;10;miki;18;33;
						if (!player.Steps.equals(""))
						{
							sRet += player.Steps;
							player.Steps = "";
							if (iLang == 0) sLog += " Step";
							else sLog += " Lépés";
						}

						// messages :feri;Hali;laci;szia;
						sRet += ":";
						if (!player.Messages.equals(""))
						{
							sRet += player.Messages;
							player.Messages = "";
							if (iLang == 0) sLog += " Message";
							else sLog += " Üzenet";
						}

						// invitations :laci;1280;720;40;
						sRet += ":";
						if (!player.Ivitations.equals(""))
						{
							sRet += player.Ivitations;
							player.Ivitations = "";
							if (iLang == 0) sLog += " Invitation";
							else sLog += " meghívás";
						}

						// added :tibi;
						sRet += ":";
						if (!player.Added.equals(""))
						{
							sRet += player.Added;
							player.Added = "";
							if (iLang == 0) sLog += " Login";
							else sLog += " belépés";
						}

						// removed :tibi;
						sRet += ":";
						if (!player.Removed.equals(""))
						{
							sRet += player.Removed;
							player.Removed = "";
							if (iLang == 0) sLog += " Logout";
							else sLog += " kilépés";
						}

						// newtable :tibi;
						sRet += ":";
						if (!player.NewTable.equals(""))
						{
							sRet += player.NewTable;
							player.NewTable = "";
							if (iLang == 0) sLog += " New Game";
							else sLog += " új játék";
						}

						// undo :tibi;
						sRet += ":";
						if (!player.UndoStep.equals(""))
						{
							sRet += player.UndoStep;
							player.UndoStep = "";
							if (iLang == 0) sLog += " UndoStep";
							else sLog += " visszavon";
						}

						//
						if (!sLog.equals(""))
						{
							if (iLang == 0) LogMessage(formatter.format(player.time) + " amoba " + strName + sLog + " update");
							else LogMessage(formatter.format(player.time) + " amoba " + strName + sLog + " frissítés");
						}
						break;
					}
				}
			}
		}

		return sRet;
	}

	private void LogMessage(String message)
	{
		if (this.gameLog)
		{
			Message msg = handler.obtainMessage();
			Bundle b = new Bundle();
			b.putString("log", message);
			msg.setData(b);
			handler.sendMessage(msg);
		}

		Log.i(TAG, message);
	}

	/**
	 * command parser
	 * 
	 * @param name
	 * @param command
	 * @return
	 */
	public String processCommand(String name, String command)
	{
		String sRes = error;
		String[] sComm = command.split(";", -1);
		
		try
		{
			// login: 1
			// in: http://192.168.0.122:8080/?game=amoba&name=dani&command=1
			// out: error | miki;feri;laci;
			if (sComm[0].equals("1")) sRes = procLogin(name, sComm);
	
			// poll: 2
			// in: http://192.168.0.122:8080/?game=amoba&name=dani&command=2
			// out: error | roli;5;10;miki;18;33;:feri;Hali;:laci;1280;720;40;
			if (sComm[0].equals("2"))
			{
				if(checkUser(name) == false) sRes = procLogin(name, sComm);
				else sRes = this.updateUser(name);
			}
	
			// user list: 3
			// in: http://192.168.0.122:8080/?game=amoba&name=dani&command=3
			// out: error | roli;miki;feri;laci;
			if (sComm[0].equals("3") && checkUser(name)) sRes = procUserlist(name);
	
			// invite: 4
			// in: http://192.168.0.122:8080/?game=amoba&name=dani&command=4;laci;1280;720;40;
			// out: error | 4
			if (sComm[0].equals("4") && checkUser(name)) sRes = procInvite(name, sComm);
	
			// step: 5
			// in: http://192.168.0.122:8080/?game=amoba&name=dani&command=5;laci;38;26
			// out: error | 5
			if (sComm[0].equals("5") && checkUser(name)) sRes = procStep(name, sComm);
	
			// message: 6
			// in http://192.168.0.122:8080/?game=amoba&name=feri&command=6;Dani;Hali;Miki;Hali
			// out: error | 6
			if (sComm[0].equals("6") && checkUser(name)) sRes = procMessage(name, sComm);
	
			// logout: 7
			// in http://192.168.0.122:8080/?game=amoba&name=feri&command=7
			// out: error | 7
			if (sComm[0].equals("7") && checkUser(name)) sRes = deleteUser(name);
	
			// new: 8
			// in http://192.168.0.122:8080/?game=amoba&name=feri&command=8
			// out: error | 8
			if (sComm[0].equals("8") && checkUser(name)) sRes = procNew(name, sComm);
	
			// undo: 9
			// http://192.168.0.122:8080/?game=amoba&name=feri&command=8
			// out: error | 9
			if (sComm[0].equals("9") && checkUser(name)) sRes = procUndo(name, sComm);
		}
		catch(Exception ex)
		{
			LogMessage("" + ex);
		}

		return sRes;
	}

	// process login
	private String procLogin(String name, String[] sComm)
	{
		Date d = new Date();
		String sRes = addUser(name, sComm[1]);
		if (!sRes.equals(error))
		{
			sRes = "";
			for (AmobaServer.AmobaPlayer player : mUsers)
				if (!player.name.equals(name)) sRes += player.name + "&" + player.Address + ";";
			if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + name + " login success (" + sComm[1] + ")");
			else LogMessage(formatter.format(d.getTime()) + " amoba " + name + " sikeresen belépett (" + sComm[1] + ")");
		}
		else if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + name + " login failed! (" + sComm[1] + ")");
		else LogMessage(formatter.format(d.getTime()) + " amoba " + name + " sikertelen belépés! (" + sComm[1] + ")");

		return sRes;
	}

	// process incoming messages
	// 6;landroo;Hali
	private String procMessage(String name, String[] sComm)
	{
		String sRes = "6";
		String partner = "";
		String message = "";
		Date d = new Date();
		for (int i = 1; i < sComm.length; i += 2)
		{
			if(sComm[i].equals("")) break;
			partner = sComm[i];
			message = sComm[i + 1];
			for (AmobaServer.AmobaPlayer player : mUsers)
			{
				if (partner.equals(DROID_NAME) && player.name.equals(name))
				{
					player.Messages += DROID_NAME + ";" + message + ";";
					if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + "Mesage added from " + DROID_NAME + " to " + player.name + " (" + message + ")");
					else LogMessage(formatter.format(d.getTime()) + " amoba " + "Üzenet " + DROID_NAME + " -> " + player.name + " (" + message + ")");
				}
				else if (!partner.equals(DROID_NAME) && player.name.equals(partner))
				{
					player.Messages += name + ";" + message + ";";
					if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + "Mesage added from " + name + " to " + player.name + " (" + message + ")");
					else LogMessage(formatter.format(d.getTime()) + " amoba " + "Üzenet " + name + " -> " + player.name + " (" + message + ")");
				}
			}
		}

		return sRes;
	}

	// process game ivitations
	// 4;laci;1280;720;40;
	private String procInvite(String name, String[] sComm)
	{
		String sRes = "4";
		String partner = "";
		String width = "";
		String height = "";
		String size = "";
		Date d = new Date();
		for (int i = 1; i < sComm.length; i += 4)
		{
			if(sComm[i].equals("")) break;
			partner = sComm[i];
			width = sComm[i + 1];
			height = sComm[i + 2];
			size = sComm[i + 3];
			for (AmobaServer.AmobaPlayer player : mUsers)
			{
				// invite the web droid
				if (partner.equals(DROID_NAME) && player.name.equals(name))
				{
					// TODO
					int w = Integer.parseInt(width);
					int h = Integer.parseInt(height);
					player.amobaClass = new AmobaClass(Integer.parseInt(size));
					player.amobaClass.initGame(w, h);
					player.amobaClass.setField(w / 2, h / 2, 2);
					
					player.Ivitations += DROID_NAME + ";" + width + ";" + height + ";" + size + ";";
					
					if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + "Invite added from " + name + " to " + DROID_NAME + " (" + width + ", " + height	+ ", " + size + ")");
					else LogMessage(formatter.format(d.getTime()) + " amoba " + "Meghívás " + name + " -> " + DROID_NAME + " (" + width + ", " + height + ", " + size + ")");
				}
				// invite another player
				else if (!partner.equals(DROID_NAME) && player.name.equals(partner))
				{
					player.Ivitations += name + ";" + width + ";" + height + ";" + size + ";";

					if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + "Invite added from " + name + " to " + player.name + " (" + width + ", " + height + ", " + size + ")");
					else LogMessage(formatter.format(d.getTime()) + " amoba " + "Meghívás " + name + " -> " + player.name + " (" + width + ", " + height + ", " + size + ")");
				}
			}
		}

		return sRes;
	}

	// process game steps
	// 5;laci;37;39;
	private String procStep(String name, String[] sComm)
	{
		String sRes = "5";
		String partner = "";
		String x = "";
		String y = "";
		Date d = new Date();
		for (int i = 1; i < sComm.length; i += 3)
		{
			if(sComm[i].equals("")) break;
			partner = sComm[i];
			x = sComm[i + 1];
			y = sComm[i + 2];
			for (AmobaServer.AmobaPlayer player : mUsers)
			{
				// play with the web droid
				if (partner.equals(DROID_NAME) && player.name.equals(name))
				{
					// TODO
					int ix = Integer.parseInt(x);
					int iy = Integer.parseInt(y);
					player.amobaClass.setField(ix, iy, 1);
					int[] iAI = player.amobaClass.amobaAI(2);
					player.amobaClass.setField(iAI[0], iAI[1], 2);

					player.Steps += DROID_NAME + ";" + iAI[0] + ";" + iAI[1] + ";";

					if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + "Step added from " + name + " to " + DROID_NAME + " (" + x + ", " + y + ")");
					else LogMessage(formatter.format(d.getTime()) + " amoba " + "Lépés " + name + " -> " + DROID_NAME + " (" + x + ", " + y + ")");
				}
				else if (!partner.equals(DROID_NAME) && player.name.equals(partner))
				{
					player.Steps += name + ";" + x + ";" + y + ";";

					if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + "Step added from " + name + " to " + player.name + " (" + x + ", " + y + ")");
					else LogMessage(formatter.format(d.getTime()) + " amoba " + "Lépés " + name + " -> " + player.name + " (" + x + ", " + y + ")");
				}
			}
		}

		return sRes;
	}

	// process user list
	private String procUserlist(String name)
	{
		Date d = new Date();
		if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba Userlist called: " + name);
		else LogMessage(formatter.format(d.getTime()) + " amoba " + "Felhasználó lista: " + name);
		String sRes = "";
		for (AmobaServer.AmobaPlayer player : mUsers)
			if (!player.name.equals(name)) sRes += player.name + "&" + player.Address + ";";
		if (sRes.equals("")) if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba Userlist is empty!");
		else LogMessage(formatter.format(d.getTime()) + " amoba " + "A felhasználó lista üres!");
		return sRes;
	}

	// process new table
	// 8;landroo
	private String procNew(String name, String[] sComm)
	{
		String sRes = "8";
		String partner = "";
		Date d = new Date();
		for (int i = 1; i < sComm.length; i += 1)
		{
			partner = sComm[i];
			for (AmobaServer.AmobaPlayer player : mUsers)
			{
				if (partner.equals(DROID_NAME) && player.name.equals(name))
				{
					// TODO
					// player.amobaClass.
				}
				else if (!partner.equals(DROID_NAME) && player.name.equals(partner))
				{
					player.NewTable += name + ";";
					if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + "New table request added from " + name + " to " + player.name);
					else LogMessage(formatter.format(d.getTime()) + " amoba " + "Új játék kérelem " + name + " -> " + player.name);
				}
			}
		}

		return sRes;
	}

	// process undo
	// 9;landroo
	private String procUndo(String name, String[] sComm)
	{
		String sRes = "9";
		String partner = "";
		Date d = new Date();
		for (int i = 1; i < sComm.length; i += 1)
		{
			partner = sComm[i];
			for (AmobaServer.AmobaPlayer player : mUsers)
			{
				if (partner.equals(DROID_NAME))
				{
					// TODO
				}
				else if (!partner.equals(DROID_NAME) && player.name.equals(partner))
				{
					player.UndoStep += name + ";";
					if (iLang == 0) LogMessage(formatter.format(d.getTime()) + " amoba " + "Undo request added from " + name + " to " + player.name);
					else LogMessage(formatter.format(d.getTime()) + " amoba " + "Visszavonás kérelem " + name + " -> " + player.name);
				}
			}
		}

		return sRes;
	}

}