package tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import log.LogType;
import log.LogWriter;
import tasks.ThreadPollChildTask.ChildType;
import threadViews.CatalogMultiPageView;
import threadViews.CatalogMultiPageView.OPost;
import threadViews.CatalogPollType;
import threadViews.CatalogSinglePageView;
import threadViews.CatalogSinglePageView.CatalogThread;
import threadViews.CatalogSinglePageView.Post;
import utils.Search;
import utils.WebResources;

/**
 * Serializable
 * @author mojo
 *
 */
public class CatalogPollTask implements Runnable, Serializable
{
	private static final long serialVersionUID = 1707432076770850710L;
	private HashSet<Search> searchSet;
	private URL fullCatalogURL;
	private URL partialCatalogURL;
	private String board;
	private CatalogPollType type;
	private transient int evenOrOdd;
	private TaskState state;
	private long lastModified;
	private transient int lastResponseCode;
	
	public CatalogPollTask(String board, CatalogPollType type)
	{
		state = TaskState.STOPPED;
		searchSet = new HashSet<Search>();
		this.board = board;
		this.type = type;
		try
		{
			fullCatalogURL = new URL(String.format("http://a.4cdn.org/%s/catalog.json", board));
			if(type == CatalogPollType.VARIABLE)
			{
				partialCatalogURL = new URL(String.format("http://a.4cdn.org/%s/1.json", board));
			}
		}
		catch (MalformedURLException e)
		{
			LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't create catalog URLs properly\n%s\n", e.getMessage()));
		}
	}
	
	private boolean ranEvenTimes()
	{
		return (evenOrOdd == 0);
	}
	
	public boolean addSearch(Search search)
	{
		synchronized(searchSet)
		{
			return searchSet.add(search);
		}
	}
	
	public boolean deleteSearch(Search search)
	{
		synchronized(searchSet)
		{
			return searchSet.remove(search);
		}
	}
	
	public Search[] getSearches()
	{
		return searchSet.toArray(new Search[0]);
	}
	
	public CatalogPollType getType()
	{
		return type;
	}
	
	public String getBoard()
	{
		return board;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Board: %s\n", board));
		if(type == CatalogPollType.CONSTANT)
		{
			sb.append("Speed: Constant\n");
		}
		else if(type == CatalogPollType.VARIABLE)
		{
			sb.append("Speed: Variable\n");
		}
		sb.append("Search patterns:\n");
		for(Search search : searchSet)
		{
			sb.append( String.format("%s\"%s\"\n", search.isCaseInsensitive() ? "(i) " : "", search.toString()) );
		}
		if(sb.length() > 0)
		{
			return sb.substring(0, sb.length());
		}
		return sb.toString();
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof CatalogPollTask)
		{
			if(obj == this)
			{
				return true;
			}
			String l = ((CatalogPollTask)obj).board;
			return this.board.equals(l);
		}
		return false;
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public int hashCode()
	{
		return board.hashCode();
	}

	@Override
	public void run()
	{
		if(fullCatalogURL == null)
		{
			state = TaskState.FAILED;
			LogWriter.getInstance().write(LogType.ERROR, String.format("Tried to execute a catalog poll task with a null full URL (board /%s/)\n", board));
			return;
		}
		if(type == CatalogPollType.VARIABLE && partialCatalogURL == null)
		{
			state = TaskState.FAILED;
			LogWriter.getInstance().write(LogType.ERROR, String.format("Tried to execute a catalog poll task with a null partial URL (board /%s/)\n", board));
			return;
		}
		if(searchSet.isEmpty())
		{
			state = TaskState.FAILED;
			LogWriter.getInstance().write(LogType.ERROR, String.format("Tried to execute a catalog poll task with an empty search set (board %s)\n", board));
			return;
		}
		
		state = TaskState.RUNNING;
		lastResponseCode = 0;
		HttpURLConnection con = null;
		StringBuilder sb = null;
		
		try
		{
			WebResources.getInstance().getWebResourceLock().acquire();
			LogWriter.getInstance().write(LogType.DEBUG, String.format("Running %s type search (%s) on /%s/",
					(type == CatalogPollType.CONSTANT ? "constant" : "variable"),
					(type == CatalogPollType.VARIABLE && ranEvenTimes() ? "partial" : "full"),
					board ));
			if(type == CatalogPollType.VARIABLE && ranEvenTimes())
			{
				con = (HttpURLConnection)partialCatalogURL.openConnection();
			}
			else
			{
				con = (HttpURLConnection)fullCatalogURL.openConnection();
			}
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
		catch (InterruptedException e)
		{
			LogWriter.getInstance().write(LogType.DEBUG, String.format("Catalog task for /%s/ is now cancelled", board));
			state = TaskState.FINISHED;
			WebResources.getInstance().getWebResourceLock().release();
			return;
		}
		catch (IOException e)
		{
			LogWriter.getInstance().write(LogType.ERROR, String.format("Catalog task for /%s/ failed while establishing connection\n%s\n",
					board,
					e.getMessage()));
			state = TaskState.FAILED;
		}
		
		if(lastResponseCode == HttpURLConnection.HTTP_OK)
		{
			lastModified = con.getLastModified();
			try(	InputStream inputstream = con.getInputStream();
					GZIPInputStream gz = new GZIPInputStream(inputstream);
					InputStreamReader inputstreamreader = new InputStreamReader(gz);
					BufferedReader reader = new BufferedReader(inputstreamreader))
			{
				sb = new StringBuilder(reader.readLine());
			}
			catch(IOException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, String.format("Processing incomming network data for catalog task (board /%s/) failed\n%s\n",
						board,
						e.getMessage()));
				state = TaskState.FAILED;
			}
			catch(NullPointerException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, String.format("Processing incomming network data for catalog task (board /%s/) failed (Nullptr!)\n%s\n",
						board,
						e.getMessage()));
				state = TaskState.FAILED;
			}
		}
		else if(lastResponseCode == 522) //cloudflare connection timeout
		{
			LogWriter.getInstance().write(LogType.ERROR, String.format("HTTP 522, Connection timed out for catalog task (board /%s/)\n", board));
			state = TaskState.FAILED;
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
						StringBuilder ein = new StringBuilder(ebr.readLine());
						LogWriter.getInstance().write(LogType.ERROR, String.format("Catalog retrieval failed with HTTP %d for board /%s/\n%s\n",
								lastResponseCode,
								board,
								ein.toString()));
						state = TaskState.FAILED;
					}
					catch(IOException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't build error message (HTTP %d, catalog task for board /%s/)\n%s\n",
								lastResponseCode,
								board,
								e.getMessage()));
						state = TaskState.FAILED;
					}
					catch(NullPointerException e)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Catalog task for board /%s/ failed while reading incomming network data\n%s\n",
								board,
								e.getMessage()));
						state = TaskState.FAILED;
					}
				}
				else
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Catalog task for board /%s/ failed while reading incomming network data. HTTP %d\n",
							board,
							lastResponseCode));
					state = TaskState.FAILED;
				}

			}
			else if(lastResponseCode == HttpURLConnection.HTTP_NOT_FOUND)
			{
				LogWriter.getInstance().write(LogType.ERROR, String.format("Catalog task for board /%s/ failed. HTTP %d!\n",
						board,
						lastResponseCode));
				state = TaskState.FAILED;
			}
			else
			{
				LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't scrape %s (http %d)", board, lastResponseCode));
				state = TaskState.FAILED;
			}
		}
		
		if(con != null)
		{
			con.disconnect();
		}
		
		if(state != TaskState.FAILED && lastResponseCode == HttpURLConnection.HTTP_OK && sb != null)
		{
			if(type == CatalogPollType.VARIABLE && ranEvenTimes())
			{
				//partial
				CatalogSinglePageView cspv = null;
				try
				{
					Gson g = new GsonBuilder().create();
					cspv = g.fromJson(sb.toString(), CatalogSinglePageView.class);
				}
				catch(JsonSyntaxException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't parse incomming JSON data for single page catalog task (board %s)\n%s\n",
							board,
							e.getMessage()));
					state = TaskState.FAILED;
				}
				if(cspv != null)
				{
					if(cspv.getThreads() != null)
					{
						synchronized(searchSet)
						{
							for(CatalogThread ct : cspv.getThreads())
							{
								if(ct.getPosts() != null && ct.getPosts().length > 0)
								{
									Post op = ct.getPosts()[0];
									
									String haystacks[] = new String[]{
											op.getCom(),
											op.getSub(),
											op.getTag(),
											op.getSemantic_url(),
											op.getName(),
											op.getFilename(),
											op.getMd5(),
											op.getTrip()};
									
									boolean matchfound = false;
									out: for(Search search : searchSet)
									{
										for(String haystack : haystacks)
										{
											if(search.in(haystack))
											{
												matchfound = true;
												break out; //stop iterating through haystacks, searchSet, and continue
											}
										}
									}
									if(matchfound)
									{
										ThreadPollParentTask tppt = null;
										if(type == CatalogPollType.CONSTANT)
										{
											tppt = new ThreadPollParentTask(board, op.getNo(), ChildType.SLOW);
										}
										else if(type == CatalogPollType.VARIABLE)
										{
											tppt = new ThreadPollParentTask(board, op.getNo(), ChildType.FAST);
										}
										if(WebResources.getInstance().submitAndInsertNewThreadPollTask(tppt))
										{
											LogWriter.getInstance().write(String.format("\"%s\" in /%s/ (p1, %dr, %di) found. Watching.",
													op.getSub() != null ? op.getSub() : op.getSemantic_url(),
													board,
													op.getReplies(),
													op.getImages()));
										}
									}
								}
							}
						} // synchronized
					}
					else
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Could not find catalog page threads for board /%s/", board));
						state = TaskState.FAILED;
					}
				}
			}
			else
			{
				//full
				CatalogMultiPageView cmpv[] = null;
				try
				{
					Gson g = new GsonBuilder().create();
					cmpv = g.fromJson(sb.toString(), CatalogMultiPageView[].class);
				}
				catch(JsonSyntaxException e)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't parse incomming JSON data for multi-page catalog task (board %s)\n%s\n",
							board,
							e.getMessage()));
					state = TaskState.FAILED;
				}
				if(cmpv != null)
				{
					synchronized(searchSet)
					{
						for(CatalogMultiPageView page : cmpv)
						{
							OPost opcollection[] = page.getThreads();
							for(OPost post : opcollection)
							{
								String haystacks[] = new String[]{
										post.getCom(),
										post.getSub(),
										post.getTag(),
										post.getSemantic_url(),
										post.getName(),
										post.getFilename(),
										post.getMd5(),
										post.getTrip()};
								boolean matchfound = false;
								out: for(Search search : searchSet)
								{
									for(String haystack : haystacks)
									{
										if(search.in(haystack))
										{
											matchfound = true;
											break out; //stop iterating through haystacks, searchSet, and continue
										}
									}
								}
								if(matchfound)
								{
									ThreadPollParentTask tppt = null;
									if(type == CatalogPollType.CONSTANT)
									{
										tppt = new ThreadPollParentTask(board, post.getNo(), ChildType.SLOW);
									}
									else if(type == CatalogPollType.VARIABLE)
									{
										tppt = new ThreadPollParentTask(board, post.getNo(), ChildType.FAST);
									}
									if(WebResources.getInstance().submitAndInsertNewThreadPollTask(tppt))
									{
										LogWriter.getInstance().write(String.format("\"%s\" in /%s/ (p%d, %dr, %di) found. Watching",
												post.getSub() != null ? post.getSub() : post.getSemantic_url(),
												board,
												page.getPage(),
												post.getReplies(),
												post.getImages()));
									}
								}
							}
						}
					} // synchronized
				}
			}
		}
		
		if(state != TaskState.FAILED)
		{
			state = TaskState.IDLE;
			try
			{
				Thread.sleep(10000);
			}
			catch (InterruptedException e){ ; }
			state = TaskState.FINISHED;
			evenOrOdd = (evenOrOdd + 1) % 2;
		}
		
		WebResources.getInstance().getWebResourceLock().release();
	}
}