package boardViews;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import log.LogType;
import log.LogWriter;
import scrape3.FileResources;
import utils.WebResources;

public class BoardCollection implements Serializable
{
	private static final long serialVersionUID = -2986580908991200924L;
	private volatile Boards boardcollection;
	private final String boardSerializedFileName = "boardList.ser";
	
	public Boards getBoardCollection()
	{
		return boardcollection;
	}
	
	public boolean init(String refURLString)
	{
		if(boardcollection == null)
		{
			File serlist = new File(boardSerializedFileName);
			if(serlist.exists())
			{
				try(FileInputStream fis = new FileInputStream(serlist); ObjectInputStream ois = new ObjectInputStream(fis))
				{
					boardcollection = (Boards)ois.readObject();
					return true;
				}
				catch (ClassNotFoundException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Could not load serialized board list\n%s\n\n", e.getMessage()));
				}
				catch (FileNotFoundException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Could not load serialized board list\n%s\n\n", e.getMessage()));
				}
				catch (IOException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Could not load serialized board list\n%s\n\n", e.getMessage()));
				}
			}
			
			synchronized(this)
			{
				if(boardcollection == null)
				{
					URL url = null;
					HttpURLConnection con = null;
					StringBuilder sb = null;
					long lastResponseCode = 0;
					
					try
					{
						url = new URL(refURLString);
						WebResources.getInstance().getWebResourceLock().acquire();
					}
					catch (MalformedURLException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Can't use %s\n%s\n\n", refURLString, e.getMessage()));
						return false;
					}
					catch (InterruptedException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Interrupted before completing necessary work!\n%s\n\n", e.getMessage()));
						return false;
					}
					
					try
					{
						con = (HttpURLConnection)url.openConnection();
						con.setRequestProperty("User-Agent", WebResources.USER_AGENT);
						con.setRequestProperty("Accept", "application/json");
						con.setRequestProperty("Accept-Encoding", "gzip");
						con.setRequestMethod("GET");
						con.connect();
						lastResponseCode = con.getResponseCode();
					}
					catch (IOException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Connection error\n%s\n\n", e.getMessage()));
						WebResources.getInstance().getWebResourceLock().release();
						return false;
					}
					
					if(lastResponseCode == HttpURLConnection.HTTP_OK)
					{
						try(	InputStream inputstream = con.getInputStream();
								GZIPInputStream gz = new GZIPInputStream(inputstream);
								InputStreamReader inputstreamreader = new InputStreamReader(gz);
								BufferedReader reader = new BufferedReader(inputstreamreader))
						{
							sb = new StringBuilder(reader.readLine());
							Gson g = new GsonBuilder().create();
							boardcollection = g.fromJson(sb.toString(), Boards.class);
						}
						catch(IOException e)
						{
							LogWriter.getInstance().write(LogType.ERROR, String.format("Could not read connection data\n%s\n\n", e.getMessage()));
							WebResources.getInstance().getWebResourceLock().release();
							return false;
						}
						catch(JsonSyntaxException e)
						{
							LogWriter.getInstance().write(LogType.ERROR, String.format("Could not parse JSON properly\n%s\n\n", e.getMessage()));
							WebResources.getInstance().getWebResourceLock().release();
							return false;
						}
						catch(NullPointerException e)
						{
							LogWriter.getInstance().write(LogType.ERROR, String.format("Null Pointer!\n%s\n\n", e.getMessage()));
							WebResources.getInstance().getWebResourceLock().release();
							return false;
						}
					}
					con.disconnect();
					if(boardcollection == null)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Could not read connection data - HTTP %d\n\n", lastResponseCode));
						WebResources.getInstance().getWebResourceLock().release();
						return false;
					}
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, "BoardCollection Interrupted before thread's timeout");
					}
					
					WebResources.getInstance().getWebResourceLock().release();

					try(	FileOutputStream fos = new FileOutputStream(boardSerializedFileName);
							ObjectOutputStream oos = new ObjectOutputStream(fos))
					{
						oos.writeObject(boardcollection);
						Files.setPosixFilePermissions(Paths.get(boardSerializedFileName), FileResources.getInstance().getLocalFilePermissions());
					}
					catch (IOException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Could not serialize board list\n%s\n\n", e.getMessage()));
					}
				}
			}
		}
		return true;
	}
}