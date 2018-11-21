package utils;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import log.LogType;
import log.LogWriter;
import tasks.DelayedTask;
import tasks.ImageDownloadTask;
import tasks.TaskState;

public class ImageDownloadTaskScheduler implements Runnable
{
	private DelayQueue<DelayedTask<ImageDownloadTask>> dq;
	
	public ImageDownloadTaskScheduler()
	{
		dq = new DelayQueue<DelayedTask<ImageDownloadTask>>();
	}
	
	public synchronized void addImageDownloadTask(DelayedTask<ImageDownloadTask> task)
	{
		if(task == null)
		{
			return;
		}
		dq.offer(task);
	}
	
	public int getTotalQueuedTasks()
	{
		int rval = 0;
		synchronized(dq)
		{
			rval = dq.size();
		}
		return rval;
	}
	
	@Override
	public void run()
	{
		boolean busy = true;
		ExecutorService taskService = Executors.newSingleThreadExecutor();
		while(busy)
		{
			ImageDownloadTask idt = null;
			try
			{
				DelayedTask<ImageDownloadTask> task = dq.take();
				idt = task.getTask();
				Future<?> f = taskService.submit(idt);
				f.get();
				if(idt.getState() == TaskState.FAILED)
				{
					LogWriter.getInstance().write(LogType.DEBUG, String.format("Re-adding delayed image task for future downloading (%s)", idt.toString()));
					DelayedTask<ImageDownloadTask> retryTask = new DelayedTask<ImageDownloadTask>(idt, task.getOriginalDelay(), TimeUnit.NANOSECONDS);
					addImageDownloadTask(retryTask);
				}
				else if(idt.getState() == TaskState.FINISHED)
				{
					if(idt.exceededRetries() || idt.getLastResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
					{
						LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't download %s, removing %s from master record",
								idt.toString(),
								idt.getFileMD5()));
						WebResources.IMAGEDL_RECORD.del(idt.getFileMD5());
					}
				}
				Thread.sleep(task.getOriginalDelay(TimeUnit.MILLISECONDS));
			}
			catch (InterruptedException e)
			{
				LogWriter.getInstance().write(LogType.DEBUG, String.format("Interrupted the image task scheduler. There are %d DelayTask(s) left",
						dq.size()));
				busy = false;
				
				//first collect all ready tasks and add remove their md5's from the master record
				//TODO: there is a chance that f.get() will still be executing when the scheduler is interrupted. Let's do what
				//ThreadPollParentTask does in that it waits until the child is idle...
				ArrayList<DelayedTask<ImageDownloadTask>> readyTasks = new ArrayList<DelayedTask<ImageDownloadTask>>();
				dq.drainTo(readyTasks);
				for(DelayedTask<ImageDownloadTask> dt : readyTasks)
				{
					String md5 = dt.getTask().getFileMD5();
					LogWriter.getInstance().write(LogType.DEBUG, String.format("Removing %s (finished delayed image task) from master image record", dt.getTask().toString()));
					WebResources.IMAGEDL_RECORD.del(md5);
				}

				for(DelayedTask<ImageDownloadTask> dt : dq)
				{
					String md5 = dt.getTask().getFileMD5();
					LogWriter.getInstance().write(LogType.DEBUG, String.format("Removing %s (unfinished delayed image task) from master image record", dt.getTask().toString()));
					WebResources.IMAGEDL_RECORD.del(md5);
				}
			}
			catch (ExecutionException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, String.format("Execution exception in image download scheduler\n%s\n", e.getMessage()));
				//TODO: is this ok?
				if(idt != null)
				{
					LogWriter.getInstance().write(LogType.ERROR, String.format("Removing %s (failed delayed image task) from master image record", idt.toString()));
					WebResources.IMAGEDL_RECORD.del(idt.getFileMD5());
				}
			}
		}
		taskService.shutdown();
		while(!taskService.isTerminated()){;}
	}
}