package utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import boardViews.BoardCollection;
import memory.SharedHashMap;
import memory.SharedHashSet;
import tasks.CatalogPollTask;
import tasks.ThreadPollParentTask;
import tasks.ThreadPollChildTask.ChildType;
import threadViews.CatalogPollType;

public final class WebResources
{
	private static volatile WebResources instance;
	/**
	 * Get information about the different boards
	 */
	private BoardCollection boardCollection;
	/**
	 * Use this to cancel tasks
	 */
	private SharedHashMap<ThreadPollParentTask, Future<?>> activeThreadMap;
	/**
	 * Use this to store inactive tasks
	 */
	private SharedHashSet<ThreadPollParentTask> inactiveThreadSet;
	/**
	 * Use this to cancel active catalog tasks
	 */
	private SharedHashMap<CatalogPollTask, Future<?>> activeCatalogMap;
	/**
	 * Use this to block other threads from accessing a web resource. Will guarantee first-in first-out granting of permits under contention
	 */
	private Semaphore WEB_RESOURCE_LOCK;
	/**
	 * Fixed thread pool - use this to submit new SLOW parent poll tasks for threads. The pool is fixed to {@link WebResources#MAX_SLOWTHREADPOOL_THREADS}
	 */
	private ExecutorService SLOWTHREAD_POOL;
	/**
	 * Fixed thread pool - use this to submit new FAST parent poll tasks for threads. The pool is fixed to {@link WebResources#MAX_FASTTHREADPOOL_THREADS}
	 */
	private ExecutorService FASTTHREAD_POOL;
	/**
	 * Fixed thread pool - use this to submit new parent poll tasks for catalog searches. The pool is fixed to {@link WebResources#MAX_CATALOGPOOL_THREADS}
	 */
	private ScheduledExecutorService CATALOG_POOL;
	/**
	 * Fixed thread pool - use this for the image download task scheduler, which allows for delayed execution of image download tasks
	 */
	private ExecutorService IMGTASKSCHEDULER_POOL;
	/**
	 * Use this to cancel the image download task scheduler
	 */
	private Future<?> IMGTASKSCHEDULER_FUTURE;
	public static final ImageDownloadTaskScheduler IMAGEDOWNLOAD_DELAYEDTASK_QUEUE = new ImageDownloadTaskScheduler();
	/**
	 * The max number of slow thread pool threads that can be run concurrently
	 */
	public static final int MAX_SLOWTHREADPOOL_THREADS = 1024;
	/**
	 * The max number of slow thread pool threads that can be run concurrently
	 */
	public static final int MAX_FASTTHREADPOOL_THREADS = 50;
	/**
	 * The max number of catalog pool threads that can be run concurrently
	 */
	public static final int MAX_CATALOGPOOL_THREADS = 72;
	public static final int CATALOGPOOL_DELAY_SLOW = 5;
	public static final int CATALOGPOOL_DELAY_FAST = 2;
	/**
	 * {@link TimeUnit#HOURS }
	 */
	public static final TimeUnit CATALOGPOOL_DELAY_UNIT = TimeUnit.HOURS;
	public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 7.0; Pixel C Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/52.0.2743.98 Safari/537.36";
	/**
	 * Store encoded image MD5 hashes here
	 */
	public static final SharedHashSet<String> IMAGEDL_RECORD = new SharedHashSet<String>();
	public static final Search THREAD_URL_MATCH = new Search("(?<=\\/)([a-z]+)\\/thread\\/([0-9]+)", Pattern.CASE_INSENSITIVE);

	/**
	 * Tested against:
	 * <ul>
		<li>https://www.youtube.com/watch?v=WY6&lt;wbr&gt;oLgoPsy8&amp;frags=pl%2Cwn</li>
		<li>https://m.youtube.com/watch?feature&lt;wbr&gt;=youtu.be&amp;v=0cxq6Js4tU8</li>
		<li>https://www.youtube.com/watch?v=DtwD-c9hn58&feature=youtu.be&time_continue=42m42s</li>
		<li>https://www.youtube.com/watch?time_continue=1m42s&v=6bT&lt;wbr&gt;CwqqKGq4</li>
		<li>https://www.youtube.com/watch?v=6bTCwqqKGq4&time_continue=10m42s</li>
		<li>https://youtu.be/6bTCwqqKGq4?t=643</li>
		<li>iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/6bTCwqqKGq4?start=180" frameborder=\"0\" allow=\"autoplay; encrypted-media\" allowfullscreen> iframe</li>
		<li>https://www.youtube.com/watch?v=6bTCwqqKGq4&start=180</li>
		<li>https://www.youtube.com/watch?start=240&v=6bTCwqqKGq4</li>
		<li>https://www.youtube.com/watch?v=DtwD-c9hn58</li>
		<li>https://youtu.be/DtwD-c9hn58</li>
		<li>https://www.youtube.com/embed/DtwD-c9hn58</li>
		<li>https://youtu.be/DtwD-c9hn58?time_continue=42m42s</li>
		<li>https://www.youtube.com/embed/fwbY7qmefRo?rel=0&autoplay=1</li>
		<li>https://youtu.be/6bTCwqqKGq4?time_continue=1m3s</li>
		<li>https://www.youtube.com/watch?time_continue=1m42s&<wbr>amp;v=6bT&<wbr>amp;&lt;wbr&gt;CwqqKGq4&lt;br&gt;okiedokie</li>
		<li>&lt;br&gt;https://m.youtube.com/watch?v=JgM4a&lt;wbr&gt;uRw2Tc&<wbr>amp;index=2&<wbr>amp;list=PL4-Rb5wk4OX4ZS&lt;wbr&gt;Dng-dQbGdlxNiiWUEME&<wbr>amp;t=0s&<wbr>amp;layout=mob&lt;wbr&gt;ile&<wbr>amp;client=mv-google&<wbr>amp;skipcontrinter&lt;wbr&gt;=1&lt;br&gt;</li>
		</ul>
	 */
	public static final Search YOUTUBE_URL_MATCH = new Search("(?:youtube\\.com\\/(?:v\\/|embed\\/|watch\\?)[a-z0-9_\\-=\\&;\\?\\<\\>\\.%]+?(?=[\\s\"]|<[^w]|$)|youtu\\.be\\/[a-z0-9_\\-=\\&;\\?\\<\\>]+?(?=[\\s\"]|<[^w]|$))", Pattern.CASE_INSENSITIVE);
	
	private WebResources()
	{
		boardCollection = new BoardCollection();
		activeThreadMap = new SharedHashMap<ThreadPollParentTask, Future<?>>();
		activeCatalogMap = new SharedHashMap<CatalogPollTask, Future<?>>();
		inactiveThreadSet = new SharedHashSet<ThreadPollParentTask>();
		WEB_RESOURCE_LOCK = new Semaphore(1, true);
		SLOWTHREAD_POOL = Executors.newFixedThreadPool(MAX_SLOWTHREADPOOL_THREADS);
		FASTTHREAD_POOL = Executors.newFixedThreadPool(MAX_FASTTHREADPOOL_THREADS);
		CATALOG_POOL = Executors.newScheduledThreadPool(MAX_CATALOGPOOL_THREADS);
		IMGTASKSCHEDULER_POOL = Executors.newSingleThreadExecutor();
		IMGTASKSCHEDULER_FUTURE = IMGTASKSCHEDULER_POOL.submit(IMAGEDOWNLOAD_DELAYEDTASK_QUEUE);
	}
	
	public BoardCollection getBoardList()
	{
		return boardCollection;
	}
	
	/**
	 * cancel all futures in internal maps, then terminate internal pools
	 */
	public void terminateAll()
	{
		for(Future<?> f : activeCatalogMap.getValueSet())
		{
			if(!f.isDone())
			{
				f.cancel(false);
			}
		}
		CATALOG_POOL.shutdown();
		while(!CATALOG_POOL.isTerminated()){;}
		
		for(Future<?> f : activeThreadMap.getValueSet())
		{
			if(!f.isDone())
			{
				f.cancel(true);
			}
		}
		SLOWTHREAD_POOL.shutdown();
		while(!SLOWTHREAD_POOL.isTerminated()){;}
		FASTTHREAD_POOL.shutdown();
		while(!FASTTHREAD_POOL.isTerminated()){;}
		
		IMGTASKSCHEDULER_FUTURE.cancel(true);
		IMGTASKSCHEDULER_POOL.shutdown();
		while(!IMGTASKSCHEDULER_POOL.isTerminated()){;}
	}
	
	public boolean terminateTask(ThreadPollParentTask task)
	{
		if(task == null)
		{
			return false;
		}
		
		if(activeThreadMap.contains(task))
		{
			Future<?> f = activeThreadMap.get(task);
			if(f != null && !f.isDone())
			{
				return f.cancel(true);
			}
		}
		return false;
	}
	
	public static WebResources getInstance()
	{
		//double-checked locking - check if instance is null twice (2nd time within a synchronized block) to prevent accidental multiple instantiations
		//https://en.wikipedia.org/wiki/Singleton_pattern#Lazy_initialization
		if(instance == null)
		{
			synchronized(WebResources.class)
			{
				if(instance == null)
				{
					instance = new WebResources();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Convenience method. Tasks are inserted only if the internal active AND inactive task maps do not already contain the task.
	 * @param task The ThreadPollParentTask to be inserted into the internal active thread map
	 * @return true iff the task was inserted successfully
	 */
	public boolean submitAndInsertNewThreadPollTask(ThreadPollParentTask task)
	{
		if(task == null || activeThreadMap.contains(task) || inactiveThreadSet.contains(task))
		{
			return false;
		}
		if(task.getChildType() == ChildType.SLOW)
		{
			return activeThreadMap.ins(task, SLOWTHREAD_POOL.submit(task));
		}
		return activeThreadMap.ins(task, FASTTHREAD_POOL.submit(task));
	}
	
	/**
	 * Convenience method for submitting net catalog tasks. Tasks are inserted iff the task does not exist already in the internal active map.
	 * @param task The CatalogPollTask to be inserted into the internal active catalog map
	 * @return true iff the task was inserted successfully
	 */
	public boolean submitAndInsertNewCatalogPollTask(CatalogPollTask task)
	{
		if(activeCatalogMap.contains(task))
		{
			return false;
		}
		else if(task.getType() == CatalogPollType.VARIABLE)
		{
			return activeCatalogMap.ins(task, CATALOG_POOL.scheduleAtFixedRate(task, 0, CATALOGPOOL_DELAY_FAST, CATALOGPOOL_DELAY_UNIT));
		}
		return activeCatalogMap.ins(task, CATALOG_POOL.scheduleAtFixedRate(task, 0, CATALOGPOOL_DELAY_SLOW, CATALOGPOOL_DELAY_UNIT));
	}
	
	public boolean removeCatalogPollTask(CatalogPollTask task)
	{
		Future<?> f = activeCatalogMap.get(task); 
		if(f != null)
		{
			if(!f.isDone())
			{
				f.cancel(false);
			}
			return activeCatalogMap.del(task);
		}
		return false;
	}
	
	public Set<String> getCatalogBoardSet()
	{
		HashSet<String> rval = new HashSet<String>();
		for(CatalogPollTask cpt : activeCatalogMap.getKeySet())
		{
			rval.add(cpt.getBoard());
		}
		return rval;
	}
	
	public SharedHashMap<CatalogPollTask, Future<?>> getActiveCatalogMap()
	{
		return activeCatalogMap;
	}
	
	public SharedHashMap<ThreadPollParentTask, Future<?>> getActiveThreadMap()
	{
		return activeThreadMap;
	}
	
	public SharedHashSet<ThreadPollParentTask> getInactiveThreadSet()
	{
		return inactiveThreadSet;
	}
	
	public synchronized Semaphore getWebResourceLock()
	{
		return WEB_RESOURCE_LOCK;
	}
}
