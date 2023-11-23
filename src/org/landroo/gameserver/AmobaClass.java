// Amoba package
package org.landroo.gameserver;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.Log;

public class AmobaClass
{
	private static final String TAG = "AmobaClass";
	
	public int miRectSize = 0; // square size
	public int miRectMaxX = 0; // paper width
	public int miRectMaxY = 0; // paper height

	private int[][] maTable = null; // playground fields
	private int[][] maValue = null; // fields values

	private int[][] maPattern = null; // pattern lines
	private int[] maPatternVal = null; // next values

	public int xOff = 0; // offset x
	public int yOff = 0; // offset y
	public int miWidth = 0; // width
	public int miHeight = 0; // height

	private int[] miEndLine = null; // draw line
	public int[] miLast1 = null; // last step
	public int[] miLast2 = null;

	public int miBackColor = 0xFF000000;
	public int miLastColor = 0xFFFFFF00;
	public int miBorderColor = 0xFFFFFFFF;
	public int miGridColor = 0xFFFFFFFF;

	public int miXColor = 0xFF00FF00;
	public int miOColor = 0xFFFF0000;
	public int miRColor = 0xFF0000FF;
	public int miTColor = 0xFF00FFFF;

	public int miLieWidth = 6;

	public String playerName;
	public boolean newEvent;
	public String saveGame = "";
	public int[] iWin = new int[4];
	public int myTurn = 1;

//	private BorderLine border;

	// constructor
	public AmobaClass(int iRectSize)
	{
		miRectSize = iRectSize;
		maPattern = new int[12][9];
		maPatternVal = new int[12];
		for (int i = 0; i < 12; i++) maPatternVal[i] = 0;
		miEndLine = new int[4];
		miLast1 = new int[3];
		miLast2 = new int[3];

		for (int i = 0; i < 4; i++) iWin[i] = 0;
	}

	// set field
	public void setTableField(int i, int j, int iPlayer)
	{
		maTable[i][j] = iPlayer;

		miLast2[0] = miLast1[0];
		miLast2[1] = miLast1[1];
		miLast2[2] = miLast1[2];

		miLast1[0] = i;
		miLast1[1] = j;
		miLast1[2] = iPlayer;

		return;
	}

	public boolean setField(int i, int j, int iPlayer)
	{
		if (i > miRectMaxX || j > miRectMaxY) return false;

		boolean bOK = false;
		if (maTable[i][j] == 0)
		{
			maTable[i][j] = iPlayer;
			
			bOK = true;

			miLast2[0] = miLast1[0];
			miLast2[1] = miLast1[1];
			miLast2[2] = miLast1[2];

			miLast1[0] = i;
			miLast1[1] = j;
			miLast1[2] = iPlayer;
		}

		return bOK;
	}

	// draw grid
	public void initGame(int width, int height)
	{
		miWidth = width;
		miHeight = height;

		int w = miWidth;
		int h = miHeight;

		w = w - (w % miRectSize);
		h = h - (h % miRectSize);

		xOff = (miWidth % miRectSize) / 2;
		yOff = (miHeight % miRectSize) / 2;

		miRectMaxX = w / miRectSize;
		miRectMaxY = h / miRectSize;

		maTable = new int[miRectMaxX][miRectMaxY];
		maValue = new int[miRectMaxX][miRectMaxY];

		for (int i = 0; i < miLast1.length; i++)
			miLast1[i] = 0;
		for (int i = 0; i < miLast2.length; i++)
			miLast2[i] = 0;
	}

	// undo last step
	public void undo()
	{
		if (miLast1[2] > 0 && miLast2[2] > 0)
		{
			maTable[miLast1[0]][miLast1[1]] = 0;
			maTable[miLast2[0]][miLast2[1]] = 0;
			
			miLast1[2] = 0;
			miLast2[2] = 0;
		}
	}

	// check game end
	public int endGame()
	{
		int iRes = 0;
		for (int i = 0; i < miRectMaxX; i++)
		{
			for (int j = 0; j < miRectMaxY; j++)
			{
				if (maTable[i][j] == 1 || maTable[i][j] == 2) iRes = checkCell(i, j, 0);
				if (iRes != 0)
				{
					getAllFields();
					//drawBorder();
					//drawLine(miEndLine[0], miEndLine[1], miEndLine[2], miEndLine[3]);

					return iRes;
				}
			}
		}

		return iRes;
	}

	// check the state of a cell
	private int checkCell(int x, int y, int st)
	{
		int i, j;

		for (i = 0; i < 12; i++)
		{
			maPatternVal[i] = 0;
			for (j = 0; j < 9; j++)
				maPattern[i][j] = 0;
		}

		cereatePattern(x, y, 0, st, 1, 0); // right
		cereatePattern(x, y, 1, st, -1, 0); // left
		cereatePattern(x, y, 2, st, 0, -1); // up
		cereatePattern(x, y, 3, st, 0, 1); // down
		cereatePattern(x, y, 4, st, 1, -1); // right up
		cereatePattern(x, y, 5, st, 1, 1); // right down
		cereatePattern(x, y, 6, st, -1, -1); // left up
		cereatePattern(x, y, 7, st, -1, 1); // left down

		j = 0;
		for (i = 4 - st; i >= 0; i--)
		{
			maPattern[8][j] = maPattern[1][i];
			maPattern[9][j] = maPattern[2][i];
			maPattern[10][j] = maPattern[6][i];
			maPattern[11][j] = maPattern[7][i];
			j++;
		}
		addPattern(j + 1, 8, 0); // left right
		addPattern(j + 1, 9, 3); // up down
		addPattern(j + 1, 10, 5); // left-up right-down
		addPattern(j + 1, 11, 4); // left-down right-up

		int iMul = 0;
		int iRet = 0;
		for (i = 0; i < 8; i++)
		{
			iMul = mulPattern(maPattern[i], 0);
			if (iMul == 511111) iRet = setEndLine(x, y, i, 1);
			if (iMul == 522222) iRet = setEndLine(x, y, i, 2);
			if (iMul == 533333) iRet = setEndLine(x, y, i, 3);
			if (iMul == 544444) iRet = setEndLine(x, y, i, 4);
		}

		return iRet;
	}

	private int setEndLine(int x, int y, int i, int ply)
	{
		miEndLine[0] = x;
		miEndLine[1] = y;
		miEndLine[2] = i;
		miEndLine[3] = ply;

		return ply;
	}

	// add new element to the end of a pattern
	private void addPattern(int st, int dest, int src)
	{
		for (int i = 0; i < 9 - st; i++)
			maPattern[dest][i + st] = maPattern[src][i];
	}

	// create pattern
	private void cereatePattern(int x, int y, int cnt, int st, int rx, int ry)
	{
		int j = 0;
		for (int i = st; i < 5; i++)
		{
			// if inside
			if (x + (i * rx) >= 0 && x + (i * rx) < miRectMaxX && y + (i * ry) >= 0 && y + (i * ry) < miRectMaxY)
			{
				maPattern[cnt][j++] = maTable[x + (i * rx)][y + (i * ry)];
				// if the cell is used increase the it's value
				if (maTable[x + (i * rx)][y + (i * ry)] > 0) maPatternVal[cnt] += 5 - i;
			}
		}

		return;
	}

	// amoba AI
	public int[] amobaAI(int iPlayer)
	{
		int[] iRes = new int[2];

		int x = 0;
		int y = 0;

		for (x = 0; x < miRectMaxX; x++)
		{
			for (y = 0; y < miRectMaxY; y++)
			{
				if (maTable[x][y] == 0)
				{
					// fill pattern
					checkCell(x, y, 1);

					// process pattern
					maValue[x][y] = processPattern(iPlayer);
				}
			}
		}

		int c = 0;
		for (x = 0; x < miRectMaxX; x++)
		{
			for (y = 0; y < miRectMaxY; y++)
			{
				if (maValue[x][y] >= c && maTable[x][y] == 0)
				{
					if (maValue[x][y] > c)
					{
						iRes[0] = x;
						iRes[1] = y;
						c = maValue[x][y];
					}
					else if (random(0, 9, 1) > 4)
					{
						iRes[0] = x;
						iRes[1] = y;
					}
				}
			}
		}

		return iRes;
	}

	// process pattern
	private int processPattern(int iPlayer)
	{
		int[] iPattern = new int[9];
		int iVal = 0;
		int iMul = 0;
		int i;

		// normal pattern
		for (i = 0; i < 8; i++)
		{
			iPattern = maPattern[i];
			iVal = maPatternVal[i];
			iMul = mulPattern(iPattern);
			if (iMul != 50000)
			{
				// increase field value +
				if (iMul == 50111 || iMul == 50222) iVal += 1; // 0111 0222
				if (iMul == 51110 || iMul == 52220) iVal += 7; // 1110 2220
				if (iMul == 51100 || iMul == 52200) iVal += 3; // 1100 2200
				if (iMul == 51000 || iMul == 52000) iVal += 1; // 1000 2000
				if (iMul == 51111 || iMul == 52222) iVal += 10; // 1111 2222

				if (iMul == 51100 && iPattern[0] == iPlayer) iVal += 1; // 1100
				if (iMul == 51110 && iPattern[0] == iPlayer) iVal += 1; // 1110
				if (iMul == 51111 && iPattern[0] == iPlayer) iVal += 9; // 1111

				if (iMul == 52200 && iPattern[0] == iPlayer) iVal += 1; // 2200
				if (iMul == 52220 && iPattern[0] == iPlayer) iVal += 1; // 2220
				if (iMul == 52222 && iPattern[0] == iPlayer) iVal += 9; // 2222

				if (iMul == 51111 && iPattern[0] != iPlayer) iVal += 10; // 1111
				if (iMul == 52222 && iPattern[0] != iPlayer) iVal += 10; // 2222

				if (iMul == 51112 && iPattern[0] == iPlayer) iVal += 7; // 1112
				if (iMul == 52221 && iPattern[0] == iPlayer) iVal += 7; // 2221

				// decrease field value -
				if (iPattern[0] != iPlayer || iPattern[1] != iPlayer) iVal -= 3;

				maPatternVal[i] = iVal;
			}
		}

		// wide pattern
		int[] iDesPatt = new int[5];
		for (i = 8; i < 12; i++)
		{
			iPattern = maPattern[i];
			for (int j = 0; j < 5; j++)
			{
				copyPattern(iPattern, iDesPatt, j, 0, 5);

				iVal = widePattern(iDesPatt, 1);
				if (iVal > 1) maPatternVal[i] += iVal * 2;

				iVal = widePattern(iDesPatt, 2);
				if (iVal > 1) maPatternVal[i] += iVal * 2;
			}

			iMul = mulPattern(iPattern, 1);
			if (iMul == 511101 || iMul == 522202) iVal = 11; // 11101 22202

			iMul = mulPattern(iPattern, 2);
			if (iMul == 511011 || iMul == 522022) iVal = 11; // 11011 22022

			iMul = mulPattern(iPattern, 3);
			if (iMul == 510111 || iMul == 520222) iVal = 11; // 10111 20222

			maPatternVal[i] += iVal;
		}

		// A legangyobb értékkel térek vissza
		int iNo1 = 0;
		for (i = 0; i < 8; i++)
			if (iNo1 < maPatternVal[i]) iNo1 = maPatternVal[i];

		int iNo2 = 0;
		for (i = 8; i < 12; i++)
			if (iNo2 < maPatternVal[i]) iNo2 = maPatternVal[i];

		return iNo1 + iNo2;
	}

	//
	private int widePattern(int[] iDesPatt, int iPly)
	{
		int iVal = 0;
		for (int k = 0; k < 5; k++)
		{
			if (iDesPatt[k] == iPly || iDesPatt[k] == 0)
			{
				if (iDesPatt[k] == iPly) iVal++;
			}
			else
			{
				iVal = 0;
				break;
			}
		}

		return iVal;
	}

	// copy patterns
	private void copyPattern(int[] iSrc, int[] iDes, int st1, int st2, int num)
	{
		for (int i = 0; i < num; i++)
			iDes[st2++] = iSrc[st1++];
	}

	// multiply pattern
	private int mulPattern(int[] iPattern)
	{
		int iRet = 50000;
		int iMul = 100;
		for (int i = 0; i < 4; i++)
		{
			iRet += iMul * 10 * iPattern[i];
			iMul /= 10;
		}
		iRet += iPattern[3];

		return iRet;
	}

	// multiply pattern
	private int mulPattern(int[] iPattern, int iBeg)
	{
		int iRet = 500000;
		int iMul = 1000;
		for (int i = iBeg; i < iBeg + 5; i++)
		{
			iRet += iMul * 10 * iPattern[i];
			iMul /= 10;
		}
		iRet += iPattern[4];

		return iRet;
	}

	// cell number
	public int getRate(int x, int y)
	{
		if (x < miRectMaxX && y < miRectMaxY && maValue[x][y] != 0) return maValue[x][y];

		return -1;
	}

	public String getAllFields()
	{
		String sFields = "";

		// write the table field
		for (int x = 0; x < this.miRectMaxX; x++)
			for (int y = 0; y < this.miRectMaxY; y++)
				sFields += this.maTable[x][y] + ";";

		sFields += miLast1[0] + ";" + miLast1[1] + ";" + miLast1[2];
		sFields += ";" + this.miRectMaxX + ";" + this.miRectMaxY;
		sFields += ";" + iWin[0] + ";" + iWin[1] + ";" + iWin[2] + ";" + iWin[3];

		saveGame = sFields;

		//Log.i(TAG, saveGame);

		return sFields;
	}

	public boolean setAllFields(String sFields, boolean bSaved)
	{
		String[] sArr = sFields.split(";");
		int iCnt = 0;

		try
		{
			int rx = Integer.parseInt(sArr[sArr.length - 6]);
			int ry = Integer.parseInt(sArr[sArr.length - 5]);

			if (this.miRectMaxX == rx && this.miRectMaxY == ry)
			{
				for (int x = 0; x < this.miRectMaxX; x++)
				{
					for (int y = 0; y < this.miRectMaxY; y++)
					{
						this.maTable[x][y] = Integer.parseInt(sArr[iCnt++]);
					}
				}
				
				miLast1[0] = Integer.parseInt(sArr[sArr.length - 9]);
				miLast1[1] = Integer.parseInt(sArr[sArr.length - 8]);
				miLast1[2] = Integer.parseInt(sArr[sArr.length - 7]);
				
				iWin[0] = Integer.parseInt(sArr[sArr.length - 4]);
				iWin[1] = Integer.parseInt(sArr[sArr.length - 3]);
				iWin[2] = Integer.parseInt(sArr[sArr.length - 2]);
				iWin[3] = Integer.parseInt(sArr[sArr.length - 1]);
				
				return true;
			}
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.getMessage());
		}

		return false;
	}
	
	public void setName(String name)
	{
		playerName = name;
	}


	public int random(int nMinimum, int nMaximum, int nRoundToInterval)
	{
		if (nMinimum > nMaximum)
		{
			int nTemp = nMinimum;
			nMinimum = nMaximum;
			nMaximum = nTemp;
		}

		int nDeltaRange = (nMaximum - nMinimum) + (1 * nRoundToInterval);
		double nRandomNumber = Math.random() * nDeltaRange;

		nRandomNumber += nMinimum;

		int nRet = (int) (Math.floor(nRandomNumber / nRoundToInterval) * nRoundToInterval);

		return nRet;
	}
	
	public int[] stPos(int sx, int sy,int w, int h, int cnt)
	{
		int[] iRes = null;
		
		// end of recursion
		if(cnt++ > 3) return iRes;

		iRes = checkEmpty(sx, sy);
		if(iRes == null) iRes = stPos(sx, sy, w / 2, h / 2, cnt);
		else return iRes;
		
		iRes = checkEmpty(sx + w / 2, sy);
		if(iRes == null) iRes = stPos(sx + w /2, sy, w / 2, h / 2, cnt);
		else return iRes;
		
		iRes = checkEmpty(sx, sy + h / 2);
		if(iRes == null) iRes = stPos(sx, sy + h / 2, w / 2, h / 2, cnt);
		else return iRes;
		
		iRes = checkEmpty(sx + w / 2, sy + h / 2);
		if(iRes == null) iRes = stPos(sx + w / 2, sy + h / 2, w / 2, h / 2, cnt);
		else return iRes;
		
		return iRes;
	}
	
	
	private int[] checkEmpty(int x, int y)
	{
		int[] iRes = null;
		
		if(x > 1 && x < miRectMaxX - 2 && y > 1 && y < miRectMaxY - 2)
		{
			if(maTable[x][y] == 0 
					&& maTable[x + 1][y] == 0 && maTable[x + 2][y] == 0 && maTable[x - 1][y] == 0 && maTable[x - 2][y] == 0
					&& maTable[x][y + 1] == 0 && maTable[x][y + 2] == 0 && maTable[x][y - 1] == 0 && maTable[x][y - 2] == 0)
			{
				iRes = new int[2];
				iRes[0] = x;
				iRes[1] = y;
			}
		}
		
		return iRes;
	}
}
