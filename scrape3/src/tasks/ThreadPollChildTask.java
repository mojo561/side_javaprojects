package tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import boardViews.Board;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import log.LogType;
import log.LogWriter;
import scrape3.FileResources;
import tasks.ImageDownloadTask.ImageThumbnailType;
import threadViews.ThreadView;
import threadViews.ThreadView.Post;
import utils.FreemarkerResources;
import utils.WebResources;

/**
 * Serializable
 * @author mojo
 *
 */
public class ThreadPollChildTask implements Callable<TaskState>, Serializable
{
	public static enum ChildType
	{
		/**
		 * Each image download will be delayed by 30 seconds between each consecutive download
		 */
		SLOW,
		/**
		 * Images will be downloaded 5 at a time as fast as possible. Call() will block until all new images per update have been downloaded
		 * (or have attempted to download)
		 */
		FAST
	};
	
	private static final long serialVersionUID = -1391259619460833240L;
	private final int SLOW_IMGTASK_DELAY_SECONDS = 30;
	private final short[] BASE_DELAY_VALUES = new short[] { 15, 20, 30, 60, 90, 120, 180, 240, 300, 301, 302, 303, 304,
			305, 306, 307, 308, 309, 600, 600, 600, 900, 900, 900, 1800, 3600, 300, 600, 900, 900, 900, 1800, 3600, 300,
			900, 1800, 3600, 7200, 10800, 21600, 32400, 32401, 32403, 32404, 32767, 32767, 32767, 32767, 32767 };
	private final int TIMEOUT_DELAY_INDEX = 18;
	private TaskState state;
	private URL threadURL;
	private transient int delayIndex;
	private transient int lastResponseCode;
	private long lastModified;
	private transient LinkedHashSet<Post> postSet;
	private transient HashSet<ImageDownloadTask> imageTaskSet;
	private HashSet<String> ytLinkSet;
	private String threadName;
	private transient String updateSchedule;
	private String board;
	private ChildType type;
	private String domain;
	
	public ThreadPollChildTask(URL threadURL, String board, ChildType type)
	{
		state = TaskState.STOPPED;
		this.threadURL = threadURL;
		this.board = board;
		threadName = threadURL.toString();
		postSet = new LinkedHashSet<Post>();
		ytLinkSet = new HashSet<String>();
		imageTaskSet = new HashSet<ImageDownloadTask>();
		this.type = type;

		Board b = WebResources.getInstance().getBoardList().getBoardCollection().getBoardsAsMap().get(board);
		if(b != null && b.getWs_board() == 1)
		{
			domain = "boards.4channel.org";
		}
		else
		{
			domain = "boards.4chan.org";
		}
		
	}
	
	public synchronized TaskState getState()
	{
		return state;
	}

	@Override
	public TaskState call() throws Exception
	{
		SimpleDateFormat updateDateFormat = new SimpleDateFormat("EEE, h:mm:ss a");
		SimpleDateFormat archiveDateFormat = new SimpleDateFormat("YYYY.MM.dd hh:mm:ss a");
		boolean poll = true;
		do
		{
			lastResponseCode = 0;
			StringBuilder sb = null;
			ThreadView thread = null;
			HttpURLConnection con = null;
			ExecutorService imageSrcPool = null;
			if(type == ChildType.FAST)
			{
				imageSrcPool = Executors.newFixedThreadPool(5);
			}
			state = TaskState.RUNNING;

			try
			{
				WebResources.getInstance().getWebResourceLock().acquire();
				con = (HttpURLConnection)threadURL.openConnection();
				con.setRequestProperty("User-Agent", WebResources.USER_AGENT);
				con.setRequestProperty("Accept", "application/json");
				con.setRequestProperty("Accept-Encoding", "gzip");
				con.setRequestMethod("GET");
				if(lastModified != 0)
				{
					con.setIfModifiedSince(lastModified);
				}
				con.connect();
				lastResponseCode = con.getResponseCode();
			}
			catch(IOException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't complete connection operation to %s (%s)\n%s\n",
						threadURL.toString(),
						threadName,
						e.getMessage()));
				//WebResources.getInstance().getWebResourceLock().release();
				//state = TaskState.FAILED;
				//poll = false;
				//continue;
			}
			catch (InterruptedException e)
			{
				//WebResources.getInstance().getWebResourceLock().release();
				state = TaskState.FAILED;
				throw new IllegalStateException(String.format("Child %s interrupted & cancelled before creating connection (should not happen!)",
						toString()));
			}

			if(lastResponseCode == HttpURLConnection.HTTP_OK)
			{
				lastModified = con.getLastModified();
				delayIndex = 0;
				
				try(	InputStream inputstream = con.getInputStream();
						GZIPInputStream gz = new GZIPInputStream(inputstream);
						InputStreamReader inputstreamreader = new InputStreamReader(gz);
						BufferedReader reader = new BufferedReader(inputstreamreader))
				{
					sb = new StringBuilder(reader.readLine());
					Gson g = new GsonBuilder().create();
					thread = g.fromJson(sb.toString(), ThreadView.class);
				}
				catch(IOException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Child %s failed while reading incomming network data\n%s\n",
							toString(),
							e.getMessage()));
					WebResources.getInstance().getWebResourceLock().release();
					state = TaskState.FAILED;
					poll = false;
					continue;
				}
				catch(JsonSyntaxException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Child %s failed while parsing incomming JSON\n%s\n",
							toString(),
							e.getMessage()));
					WebResources.getInstance().getWebResourceLock().release();
					state = TaskState.FAILED;
					poll = false;
					continue;
				}
				catch(NullPointerException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Child %s failed while reading incomming network data\n%s\n",
							toString(),
							e.getMessage()));
					WebResources.getInstance().getWebResourceLock().release();
					state = TaskState.FAILED;
					poll = false;
					continue;
				}
			}
			else if(lastResponseCode == 522) //cloudflare connection timeout
			{
				delayIndex = TIMEOUT_DELAY_INDEX;
				LogWriter.getInstance().write(String.format("Connection timed out (%s), retry in %ds", threadName, BASE_DELAY_VALUES[delayIndex]));
			}
			else if(lastResponseCode != HttpURLConnection.HTTP_NOT_MODIFIED)
			{
				if(lastResponseCode >= HttpURLConnection.HTTP_BAD_REQUEST && lastResponseCode != HttpURLConnection.HTTP_NOT_FOUND)
				{
					if(con.getErrorStream() != null) // && con.getErrorStream().available() != 0) TODO: needed?
					{
						try(	InputStream eis = con.getErrorStream();
								InputStreamReader ereader = new InputStreamReader(eis);
								BufferedReader ebr = new BufferedReader(ereader))
						{
							state = TaskState.FAILED;
							StringBuilder ein = new StringBuilder(ebr.readLine());
							LogWriter.getInstance().write(LogType.ERROR, String.format("%d %s\n%s", lastResponseCode, threadName, ein.toString()));
						}
						catch(IOException e) //TODO: fail on exception instead?
						{
							LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't build error message (HTTP %d, %s)\n%s",
									lastResponseCode,
									threadName,
									e.getMessage()));
						}
						catch(NullPointerException e) //TODO: fail on exception instead?
						{
							LogWriter.getInstance().write(LogType.ERROR, String.format("Child %s failed while reading incomming network data\n%s\n",
									toString(),
									e.getMessage()));
						}
					}

				}
				else if(lastResponseCode == HttpURLConnection.HTTP_NOT_FOUND)
				{
					LogWriter.getInstance().write(String.format("Thread [%s] %d'd", threadName, lastResponseCode));
					state = TaskState.FINISHED;
				}
				else
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't scrape %s (http response %d)", threadName, lastResponseCode));
					state = TaskState.FAILED;
				}
			}

			if(con != null)
			{
				con.disconnect();
			}

			//thread will not be null if connection succeeded and JSON was properly parsed
			if(thread != null && thread.getPosts() != null && thread.getPosts().length > 0)
			{
				File dfile = new File(board);
				if(!dfile.exists())
				{
					Set<PosixFilePermission> posixperms = FileResources.getInstance().getThreadDirPermissions();
					try
					{
						Files.createDirectory(dfile.toPath(), PosixFilePermissions.asFileAttribute(posixperms));
						Files.setPosixFilePermissions(dfile.toPath(), posixperms);
					}
					catch(IOException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Child %s could not create directory %s or it couldn't set permissions correctly\n%s\n",
								toString(),
								dfile.toString(),
								e.getMessage()));
						WebResources.getInstance().getWebResourceLock().release();
						state = TaskState.FAILED;
						poll = false;
						continue;
					}
				}
				
				Post opost = thread.getPosts()[0];
				if(threadName.equals(threadURL.toString()))
				{
					//thread name is considered not set yet
					if(opost.getSub() != null && opost.getSub().trim().length() > 0)
					{
						threadName = opost.getSub();
					}
					else if(opost.getSemantic_url() != null && opost.getSemantic_url().trim().length() > 0)
					{
						threadName = opost.getSemantic_url();
					}
					else if(opost.getNo() > 0)
					{
						threadName = Long.toString(opost.getNo());
					}
					else
					{
						WebResources.getInstance().getWebResourceLock().release();
						state = TaskState.FAILED;
						throw new Exception("Subject and semantic URL are both null / empty, and opost ID is <= 0, something has gone wrong");
					}
				}
				//rare case: It is possible for threads to 'disappear', or in otherwords, previously active threads begin returning JSON
				//similar to the following: {"posts":[{"resto":0,"replies":0,"images":0,"unique_ips":1,"semantic_url":null}]}
				//Assuming this is an unrecoverable error. For now let's just throw an exception and stop updating.
				if(opost.getSemantic_url() == null)
				{
					WebResources.getInstance().getWebResourceLock().release();
					state = TaskState.FAILED;
					throw new Exception(String.format("Received thread post count: %d\nStored post count: %d\nJSON: \"%s\"",
							thread.getPosts().length,
							postSet.size(),
							sb.toString()));
				}
				
				for(Post p : thread.getPosts())
				{
					if(!postSet.add(p))
					{
						continue;
					}
					if(p.getMd5() != null)
					{
						if(!WebResources.IMAGEDL_RECORD.contains(p.getMd5()))
						{
							if(!WebResources.IMAGEDL_RECORD.ins(p.getMd5()))
							{
								LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't add %d%s (%s) to image hashset list", p.getTim(), p.getExt(), p.getMd5()));
							}
							else
							{
								ImageDownloadTask imgt;
								if(p.getExt().equals(".webm") || p.getExt().equals(".gif"))
								{
									if(p.equals(opost))
									{
										imgt = new ImageDownloadTask(p.getTim(), p.getSanitizedMD5(), p.getExt(), board, p.getMd5(), ImageThumbnailType.LARGE);
									}
									else
									{
										imgt = new ImageDownloadTask(p.getTim(), p.getSanitizedMD5(), p.getExt(), board, p.getMd5(), ImageThumbnailType.SMALL);
									}
								}
								else
								{
									imgt = new ImageDownloadTask(p.getTim(), p.getSanitizedMD5(), p.getExt(), board, p.getMd5());
								}
								imageTaskSet.add(imgt);
								if(type == ChildType.FAST)
								{
									imageSrcPool.submit(imgt);
								}
								else if(type == ChildType.SLOW)
								{
									DelayedTask<ImageDownloadTask> imgtask = new DelayedTask<ImageDownloadTask>(imgt, SLOW_IMGTASK_DELAY_SECONDS, TimeUnit.SECONDS);
									WebResources.IMAGEDOWNLOAD_DELAYEDTASK_QUEUE.addImageDownloadTask(imgtask);
								}
							}
						}
					}
					if(WebResources.YOUTUBE_URL_MATCH.in(p.getCom()))
					{
						String matches[] = WebResources.YOUTUBE_URL_MATCH.getHits();
						for(String link : matches)
						{
							if(link != null)
							{
								doYTLinkInsert(link.replaceAll("<wbr>", ""));
							}
						}
					}
					if(WebResources.YOUTUBE_URL_MATCH.in(p.getSub()))
					{
						String matches[] = WebResources.YOUTUBE_URL_MATCH.getHits();
						for(String link : matches)
						{
							if(link != null)
							{
								doYTLinkInsert(link.replaceAll("<wbr>", ""));
							}
						}
					}
				}
				
				if(opost.isClosed())
				{
					state = TaskState.FINISHED;
					if(opost.isArchived())
					{
						Date d = new Date(opost.getArchived_on() * 1000);
						LogWriter.getInstance().write(String.format("Thread %s was archived (%s)", threadName, archiveDateFormat.format(d)));
					}
					else
					{
						LogWriter.getInstance().write(String.format("Thread %s was closed", threadName));
					}
				}
				
				//map of all the things that will be written out to the php file
				HashMap<String, Object> input = new HashMap<String, Object>();
				input.put("domain", domain);
				input.put("urllist", ytLinkSet);
				if(opost.getSemantic_url().trim().length() > 0)
				{
					input.put("title", opost.getSemantic_url());
				}
				else
				{
					input.put("title", String.format("/%s/thread/%d", board, opost.getNo()));
				}
				if(opost.getSub() != null)
				{
					input.put("header", opost.getSub());
				}
				else if(opost.getSemantic_url().trim().length() > 0)
				{
					input.put("header", opost.getSemantic_url());
				}
				else
				{
					input.put("header", String.format("/%s/thread/%d", board, opost.getNo()));
				}
				input.put("livethreadurl", String.format("https://%s/%s/thread/%d", domain, board, opost.getNo()));
				input.put("firstpost", opost);
				input.put("board", board);
				input.put("posts", postSet);
				input.put("lastmodified", updateDateFormat.format(new Date(lastModified)));
				
				//write to output file...
				String fileName;
				
				if(opost.getSemantic_url() != null && opost.getSemantic_url().trim().length() > 0)
				{
					fileName = String.format("%s/%d_%s.php", board, opost.getNo(), opost.getSemantic_url());
				}
				else
				{
					fileName = String.format("%s/%d.php", board, opost.getNo());
				}
				
				Template tmpl = null;
				
				try
				{
					tmpl = FreemarkerResources.getInstance().getConfiguration().getTemplate(FreemarkerResources.THREAD_TEMPLATE_FILENAME);
				}
				catch(IOException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Error retrieving template\n%s\n", e.getMessage()));
				}
				
				if(tmpl != null)
				{
					try(FileWriter fileWriter = new FileWriter(fileName, false))
					{
						tmpl.process(input, fileWriter);
					}
					catch(IOException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Error processing template\n%s\n", e.getMessage()));
					}
					catch (TemplateException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Error processing template\n%s\n", e.getMessage()));
					}
				}
			}
			
			/**
			 * new: sometimes image d/l will fail (HTTP 504).
			 * for-each image in the list, if it's state == finished, remove it from the list, otherwise try again for download.
			 * ImageDownloadTask objects keep track of the number of connection attempts it makes. If the number of previous attempts
			 * exceed ImageDownloadTask.MAX_RETRIES, then the task will not attempt the connection and will report as finished.
			 */
			HashSet<ImageDownloadTask> tmp = new HashSet<ImageDownloadTask>();
			for(ImageDownloadTask idt : imageTaskSet)
			{
				//construct this because next block will potentially modify imageTaskSet
				if(idt != null)
				{
					tmp.add(idt);
				}
			}
			
			if(type == ChildType.FAST)
			{
				//TODO: let's move this to a dedicated scheduler of some kind that handles all of this instead of having this child class
				//responsible
				for(ImageDownloadTask idt : tmp)
				{
					if(idt.getState() == TaskState.FINISHED)
					{
						if(idt.exceededRetries() || idt.getLastResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
						{
							//we couldn't download the image, perhaps it will turn up elsewhere in the future
							//remove the image md5 from the master record
							String md5 = idt.getFileMD5();
							LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't download %s, removing %s from master record",
									idt.toString(),
									md5));
							WebResources.IMAGEDL_RECORD.del(md5);
						}
						imageTaskSet.remove(idt);
					}
					else if(idt.getState() == TaskState.FAILED)
					{
						//this is done to allow for a period of sleep() to occur before reattempting to download the image again
						LogWriter.getInstance().write(LogType.DEBUG, String.format("Rearming image task for future downloading (%s)", idt.toString()));
						idt.rearm();
					}
					else if(idt.getState() == TaskState.IDLE)
					{
						LogWriter.getInstance().write(LogType.DEBUG, String.format("Retry download from %s", idt.toString()));
						imageSrcPool.submit(idt);
					}
				}

				imageSrcPool.shutdown();
				while(!imageSrcPool.isTerminated()){;}
			}
				
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				LogWriter.getInstance().write(LogType.DEBUG, String.format("Quitting child is now releasing lock early (%s)", toString()));
				state = TaskState.FINISHED;
			}

			WebResources.getInstance().getWebResourceLock().release();

			try
			{
				if(state != TaskState.FINISHED && state != TaskState.FAILED)
				{
					state = TaskState.IDLE;
					updateScheduleString(BASE_DELAY_VALUES[delayIndex], updateDateFormat);

					Thread.sleep((long)BASE_DELAY_VALUES[delayIndex] * 1000);

					delayIndex = (delayIndex + 1) % BASE_DELAY_VALUES.length;
				}
				else
				{
					poll = false;
				}
			}
			catch(InterruptedException e)
			{
				LogWriter.getInstance().write(LogType.DEBUG, String.format("Waking up child for cancellation (%s)", toString()));
				state = TaskState.FINISHED;
				poll = false;
			}
		} while(poll);
		
		//before completion, clear out any unfinished image tasks and remove their encoded md5 hashes from the master image record
		//TODO: should we attempt to retry? What if thread was just closed / archived?
		//TODO: child shouldn't be responsible for this
		if(type == ChildType.FAST)
		{
			for(ImageDownloadTask idt : imageTaskSet)
			{
				String md5 = idt.getFileMD5();
				if(idt.getState() == TaskState.FINISHED)
				{
					if(idt.exceededRetries() || idt.getLastResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
					{
						LogWriter.getInstance().write(String.format("Couldn't download %s (max retries or 404), removing %s from master record",
								idt.toString(),
								md5));
						WebResources.IMAGEDL_RECORD.del(md5);
					}
				}
				else if(idt.getState() == TaskState.FAILED)
				{
					LogWriter.getInstance().write(String.format("Couldn't download %s (general failure), removing %s from master record",
							idt.toString(),
							md5));
					WebResources.IMAGEDL_RECORD.del(md5);
				}
				else if(idt.getState() == TaskState.IDLE)
				{
					LogWriter.getInstance().write(String.format("%s was awaiting redownload, removing %s from master record because unfinished tasks are being purged",
							idt.toString(),
							md5));
					WebResources.IMAGEDL_RECORD.del(md5);
				}
			}
		}
		
		return state;
	}

	public String getUpdateSchedule()
	{
		return updateSchedule;
	}
	
	private void doYTLinkInsert(String str)
	{
		if(str.length() > 7)
		{
			str = str.replaceAll("<wbr>", "");
			if(str.substring(0, 7).equals("youtube"))
			{
				ytLinkSet.add(String.format("www.%s", str));
			}
			else
			{
				ytLinkSet.add(str);
			}
		}
	}
	
	private void updateScheduleString(int addTicks, SimpleDateFormat datefmt)
	{
		Calendar c = Calendar.getInstance();
		c.add(Calendar.SECOND, addTicks);
		updateSchedule = datefmt.format(new Date(c.getTimeInMillis()));
	}
	
	@Override
	public String toString()
	{
		if(state == TaskState.IDLE)
		{
			if(type == ChildType.SLOW)
			{
				return String.format("[%s]* /%s/ %s (update @%s)", state, board, threadName, updateSchedule);
			}
			return String.format("[%s] /%s/ %s (update @%s)", state, board, threadName, updateSchedule);
		}
		return String.format("[%s] /%s/ %s", state, board, threadName);
	}
}