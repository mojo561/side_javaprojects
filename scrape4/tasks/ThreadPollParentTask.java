package tasks;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import log.LogType;
import log.LogWriter;
import tasks.ThreadPollChildTask.ChildType;
import utils.WebResources;

/**
 * Serializable
 * @author mojo
 *
 */
public class ThreadPollParentTask implements Runnable, Serializable, Comparable<ThreadPollParentTask>
{
	private static final long serialVersionUID = 4400398747057754751L;
	private long threadID;
	private String board;
	private ThreadPollChildTask child;
	private TaskState state;
	private ChildType childType;

	public ThreadPollParentTask(String board, long threadID)
	{
		state = TaskState.STOPPED;
		this.board = board;
		this.threadID = threadID;
		childType = ChildType.FAST;
	}
	
	public ThreadPollParentTask(String board, long threadID, ChildType childType)
	{
		this(board, threadID);
		this.childType = childType;
	}
	
	public TaskState getState()
	{
		return state;
	}
	
	public ChildType getChildType()
	{
		return childType;
	}

	@Override
	public void run()
	{
		ExecutorService service = Executors.newSingleThreadExecutor();
		
		URL url = null;
		try
		{
			url = new URL(String.format("http://a.4cdn.org/%s/thread/%d.json", board, threadID));
			child = new ThreadPollChildTask(url, board, childType);
		}
		catch (MalformedURLException e)
		{
			state = TaskState.FAILED;
			LogWriter.getInstance().write(LogType.ERROR, String.format("Board, thread ID: %s %d\n%s", board, threadID, e.getMessage()));
			return;
		}
		
		Future<TaskState> rval = service.submit(child);
		
		try
		{
			state = TaskState.RUNNING;
			state = rval.get();
		}
		catch (InterruptedException e)
		{
			//this.currentThread's interrupted status is implicitly reset
			//1. wait for child task to become idle
			//2. cancel the child task
			while(child.getState() == TaskState.RUNNING)
			{
				try
				{
					LogWriter.getInstance().write(LogType.DEBUG, String.format("Parent waiting for child to exit running state (child: %s)", child.toString()));
					Thread.sleep(500); //hopefully the child isn't set to delay for less than half a second
				}
				catch (InterruptedException e1)
				{
					state = TaskState.FAILED;
					LogWriter.getInstance().write(LogType.ERROR, String.format("Parent exception (InterruptedException (should never happen!), child: %s)\n%s", child.toString(), e1.getMessage()));
				}
			}
			if(rval.cancel(true))
			{
				LogWriter.getInstance().write(LogType.DEBUG, String.format("Parent cancelled child successfully (child: %s)", child.toString()));
				state = TaskState.FINISHED;
			}
			else
			{
				if(rval.isDone())
				{
					state = TaskState.FINISHED;
					LogWriter.getInstance().write(LogType.DEBUG, String.format("Parent cancelled child successfully (child, done: %s)", child.toString()));
				}
				else
				{
					state = TaskState.FAILING;
					LogWriter.getInstance().write(LogType.ERROR, String.format("Parent couldn't cancel child (child: %s)", child.toString()));
					//TODO: child couldn't be cancelled and is still running - what to do in this case?
				}
			}
		}
		catch (ExecutionException e)
		{
			state = TaskState.FAILED;
			LogWriter.getInstance().write(LogType.ERROR, String.format("Parent exception (ExecutionException, child: %s)\n%s", child.toString(), e.getMessage()));
		}
		finally
		{
			WebResources.getInstance().getActiveThreadMap().del(this);
			WebResources.getInstance().getInactiveThreadSet().ins(this);
			service.shutdown();
			while(!service.isTerminated());
		}
	}
	
	public long getThreadID()
	{
		return threadID;
	}
	
	@Override
	public String toString()
	{
		if(child != null)
		{
			return child.toString();
		}
		return String.format("Childless! [%s] Board: %s, Thread ID: %d", state, board, threadID);
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof ThreadPollParentTask)
		{
			long rid = ((ThreadPollParentTask)obj).threadID;
			return this.threadID == rid;
		}
		return false;
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public int hashCode()
	{
		return Long.hashCode(threadID);
	}

	@Override
	public int compareTo(ThreadPollParentTask right)
	{
		return this.board.compareTo(right.board);
	}
}