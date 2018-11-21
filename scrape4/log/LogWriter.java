package log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.TimeZone;

import scrape3.FileResources;

public class LogWriter
{
	private static volatile LogWriter instance;
	
	private boolean hasInfoPermsSet;
	private boolean hasErrorPermsSet;
	private SimpleDateFormat sdf;
	private String infoLogFileName;
	private String errorLogFileName;
	
	private LogWriter()
	{
		sdf = new SimpleDateFormat("YY/MM/dd hh:mm:ss a");
		String magicstr = getMagicEpochString();
		errorLogFileName = String.format("log/%s_error.log", magicstr);
		infoLogFileName = String.format("log/%s_info.log", magicstr);
		
		File logDir = new File("log");
		if(!logDir.exists())
		{
			Set<PosixFilePermission> posixperms = FileResources.getInstance().getLogDirPermissions();
			try
			{
				Files.createDirectory(logDir.toPath(), PosixFilePermissions.asFileAttribute(posixperms));
				Files.setPosixFilePermissions(logDir.toPath(), posixperms);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static LogWriter getInstance()
	{
		if(instance == null)
		{
			synchronized(LogWriter.class)
			{
				if(instance == null)
				{
					instance = new LogWriter();
				}
			}
		}
		return instance;
	}
	
	private String getMagicEpochString()
	{
		Calendar c = Calendar.getInstance();
		int curyear = c.get(Calendar.YEAR);
		
		Calendar nowc = new GregorianCalendar(curyear, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		nowc.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		Calendar thenc = new GregorianCalendar(curyear, 0, 1);
		thenc.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		long diffEpoch = (nowc.getTimeInMillis() - thenc.getTimeInMillis()) / 1000;
		return String.valueOf(diffEpoch);
	}
	
	public boolean write(String message)
	{
		return write(LogType.INFO, message);
	}
	
	public boolean write(LogType type, String message)
	{
		boolean rval = true;
		String outstr = String.format("[%s] %s: %s", sdf.format(Calendar.getInstance().getTime()), type, message);
		String fileName = "";
		
		switch(type)
		{
		case ERROR:
			fileName = errorLogFileName;
			break;
		default:
			fileName = infoLogFileName;
			break;
		}
		synchronized(LogWriter.class)
		{
			File logDir = new File("log");
			if(!logDir.exists())
			{
				System.err.println(outstr);
				rval = false;
			}
			else
			{
				try(FileWriter writer = new FileWriter(fileName, true))
				{
					writer.write(outstr);
					writer.write('\n');
					if((type == LogType.INFO || type == LogType.DEBUG) && !hasInfoPermsSet)
					{
						Files.setPosixFilePermissions(Paths.get(fileName), FileResources.getInstance().getLocalFilePermissions());
						hasInfoPermsSet = true;
					}
					else if(type == LogType.ERROR && !hasErrorPermsSet)
					{
						Files.setPosixFilePermissions(Paths.get(fileName), FileResources.getInstance().getLocalFilePermissions());
						hasErrorPermsSet = true;
					}
				}
				catch (IOException e)
				{
					rval = false;
					System.err.println(outstr);
					e.printStackTrace();
				}
			}
		}
		return rval;
	}
}