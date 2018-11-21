package tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import log.LogType;
import log.LogWriter;
import utils.WebResources;

/**
 * Serializable
 * @author mojo
 *
 */
public class ImageDownloadTask implements Runnable, Serializable
{
	private static final long serialVersionUID = -7816636670346049877L;
	private TaskState state;
	private URL url;
	private String dstFileName;
	private String fileExt;
	private String fileMD5;
	private ImageThumbnailType thumbtype;
	private int lastResponseCode;
	private transient int retryCount;
	public static final int MAX_RETRIES = 2;
	
	static enum ImageThumbnailType
	{
		NONE,
		SMALL,
		LARGE
	};
	
	public ImageDownloadTask(long srcFileID, String dstFileName, String fileExt, String board, String fileMD5)
	{
		this(srcFileID, dstFileName, fileExt, board, fileMD5, ImageThumbnailType.NONE);
	}
	
	public ImageDownloadTask(long srcFileID, String dstFileName, String fileExt, String board, String fileMD5, ImageThumbnailType thumbtype)
	{
		this.dstFileName = dstFileName;
		this.fileExt = fileExt;
		this.fileMD5 = fileMD5;
		this.thumbtype = thumbtype;
		state = TaskState.STOPPED;
		try
		{
			url = new URL( String.format("http://i.4cdn.org/%s/%d%s", board, srcFileID, fileExt) );
		}
		catch (MalformedURLException e)
		{
			LogWriter.getInstance().write(LogType.ERROR, String.format("MalformedURLException (src: \"%s\" dst: \"%s\" ext: \"%s\")\n%s\n",
					srcFileID,
					dstFileName,
					fileExt,
					e.getMessage()));
		}
	}
	
	/**
	 * Sets the state to TaskState.IDLE
	 */
	public void rearm()
	{
		state = TaskState.IDLE;
	}
	
	public TaskState getState()
	{
		return state;
	}
	
	public int getLastResponseCode()
	{
		return lastResponseCode;
	}
	
	public boolean exceededRetries()
	{
		return (retryCount >= MAX_RETRIES);
	}
	
	public String getFileMD5()
	{
		return fileMD5;
	}
	
	@Override
	public String toString()
	{
		if(url != null)
		{
			return String.format( "%s (%s)", fileMD5, url.toString() );
		}
		return String.format( "%s (%s)", fileMD5, "NO URL!" );
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public boolean equals(Object obj)
	{
		//TODO: is this ok?
		if(obj instanceof ImageDownloadTask)
		{
			if(obj == this)
			{
				return true;
			}
			
			return this.fileMD5.equals(((ImageDownloadTask)obj).fileMD5);
			//String l = ((ImageDownloadTask)obj).url.toString();
			//return this.url.toString().equals(l);
		}
		return false;
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public int hashCode()
	{
		//TODO: is this ok?
		//return url.toString().hashCode();
		return fileMD5.hashCode();
	}

	@Override
	public void run()
	{
		if(url == null)
		{
			state = TaskState.FAILED;
			LogWriter.getInstance().write(LogType.ERROR, "URL was set to NULL");
			return;
		}
		/**
		 * Don't continuously attempt connections
		 */
		if(exceededRetries())
		{
			state = TaskState.FINISHED;
			LogWriter.getInstance().write(LogType.ERROR, String.format("Connection to %s aborted, previous number of connection attempts exceed max of %d\n",
					url.toString(),
					MAX_RETRIES));
			return;
		}

		state = TaskState.RUNNING;
		long bytesWritten = 0;
		File f = new File( String.format("images/%s%s", dstFileName, fileExt) );
		HttpURLConnection connection = null;
		try
		{
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("User-Agent", WebResources.USER_AGENT);
			connection.setRequestMethod("GET");
			lastResponseCode = connection.getResponseCode();
			
			if(lastResponseCode == HttpURLConnection.HTTP_OK)
			{
				try(	ReadableByteChannel readc = Channels.newChannel(connection.getInputStream());
						FileOutputStream fos = new FileOutputStream(f);)
				{
					FileChannel filec = fos.getChannel();
					bytesWritten = filec.transferFrom(readc, 0, Long.MAX_VALUE);
				}
				catch(IOException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("%s, bytes written: %d\n%s", url, bytesWritten, e.getMessage()));
					state = TaskState.FAILED;
				}

				if(thumbtype != ImageThumbnailType.NONE && state != TaskState.FAILED)
				{
					if(fileExt.equals(".webm"))
					{
						String quality = "-q:v 8";
						String res = "scale=125:125:force_original_aspect_ratio=decrease";
						if(thumbtype == ImageThumbnailType.LARGE)
						{
							quality = "-q:v 1";
							res = "scale=250:250:force_original_aspect_ratio=decrease";
						}
						//TODO: how to handle the -n option -> if a thumbnail already exists, the source file itself must have already been processed
						//hopefully this means that the file hasn't already been redownloaded, but just incase let's come up with a warning
						String cmd = String.format("/usr/bin/ffmpeg -i images/%s%s -loglevel quiet -n %s -vf %s -frames:v 1 images/%ss.jpg", dstFileName, fileExt, quality, res, dstFileName);
						if(Runtime.getRuntime().exec(cmd).getErrorStream().available() != 0)
						{
							try(	InputStream erroris = Runtime.getRuntime().exec(cmd).getErrorStream();
									InputStreamReader ereader = new InputStreamReader(erroris);
									BufferedReader ebr = new BufferedReader(ereader))
							{
								//if an error occurred, print it out
								StringBuilder ein = new StringBuilder(ebr.readLine());
								LogWriter.getInstance().write(LogType.ERROR, String.format("%s%s (%s)\ncommand: %s\n%s\n", dstFileName, fileExt, url.toString(), cmd, ein.toString()));
								state = TaskState.FAILED;
							}
							catch(IOException e)
							{
								LogWriter.getInstance().write(LogType.ERROR, String.format("%s%s (%s)\ncommand: %s\n%s\n", dstFileName, fileExt, url.toString(), cmd, e.getMessage()));
								state = TaskState.FAILED;
							}
							catch(NullPointerException e)
							{
								LogWriter.getInstance().write(LogType.ERROR, String.format("%s%s (%s)\ncommand: %s\nNullptr!\n%s\n", dstFileName, fileExt, url.toString(), cmd, e.getMessage()));
								state = TaskState.FAILED;
							}
						}
					}
					else if(fileExt.equals(".gif"))
					{
						String quality = "-quality 62";
						String res = "125x125>";
						if(thumbtype == ImageThumbnailType.LARGE)
						{
							quality = "-quality 100";
							res = "250x250>";
						}
						String cmd = String.format("/usr/bin/convert ./images/%s%s[0] -thumbnail %s %s -background white -alpha remove -alpha off ./images/%ss.jpg", dstFileName, fileExt, res, quality, dstFileName);
						if(Runtime.getRuntime().exec(cmd).getErrorStream().available() != 0)
						{
							try(	InputStream erroris = Runtime.getRuntime().exec(cmd).getErrorStream();
									InputStreamReader ereader = new InputStreamReader(erroris);
									BufferedReader ebr = new BufferedReader(ereader))
							{
								//if an error occurred, print it out
								StringBuilder ein = new StringBuilder(ebr.readLine());
								LogWriter.getInstance().write(LogType.ERROR, String.format("%s%s (%s)\ncommand: %s\n%s", dstFileName, fileExt, url.toString(), cmd, ein.toString()));
								state = TaskState.FAILED;
							}
							catch(IOException e)
							{
								LogWriter.getInstance().write(LogType.ERROR, String.format("%s%s (%s)\ncommand: %s\n%s\n", dstFileName, fileExt, url.toString(), cmd, e.getMessage()));
								state = TaskState.FAILED;
							}
							catch(NullPointerException e)
							{
								LogWriter.getInstance().write(LogType.ERROR, String.format("%s%s (%s)\ncommand: %s\nNullptr!\n%s\n", dstFileName, fileExt, url.toString(), cmd, e.getMessage()));
								state = TaskState.FAILED;
							}
						}
					}
				}
			}
			else if(lastResponseCode == HttpURLConnection.HTTP_NOT_FOUND)
			{
				LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't download from %s (HTTP %d)", url.toString(), lastResponseCode));
				state = TaskState.FINISHED;
			}
			else
			{
				LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't download from %s (HTTP %d)", url.toString(), lastResponseCode));
				state = TaskState.FAILED;
			}
			connection.disconnect();
			if(state != TaskState.FAILED)
			{
				//if we haven't failed at this point, then we're finished
				state = TaskState.FINISHED;
			}
		}
		catch (IOException e)
		{
			LogWriter.getInstance().write(LogType.ERROR, String.format("%s, bytes written: %d\n%s", url, bytesWritten, e.getMessage()));
			state = TaskState.FAILED;
		}
		finally
		{
			++retryCount;
		}
	}
}