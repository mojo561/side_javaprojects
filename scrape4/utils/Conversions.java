package utils;

import log.LogType;
import log.LogWriter;

public class Conversions
{
	private Conversions(){ ; }
	
	public static long tryParse(String str)
	{
		long l = 0;
		try
		{
			l = Long.parseLong(str);
		}
		catch(NumberFormatException e)
		{
			LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
		}
		return l;
	}
}