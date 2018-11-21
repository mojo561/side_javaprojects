package tasks;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedTask<T> implements Delayed
{
	private long initNanos;
	private long delay;
	private T task;
	
	public DelayedTask(T task, long delay, TimeUnit unit)
	{
		this.task = task;
		initNanos = System.nanoTime();
		this.delay = unit.toNanos(delay);
	}
	
	public T getTask()
	{
		return task;
	}
	
	/**
	 * Not to be confused with {@link DelayedTask#getDelay(TimeUnit)}, which retrieves the <b>remaining</b> time ticks left until this
	 * task is active instead of the fixed delay value. This method will instead just return the delay value
	 * as provided in the DelayedTask constructor converted to the provided TimeUnit
	 * @param unit - the TimeUnit to convert the internal delay to
	 * @return the delay value as provided in the DelayedTask constructor converted to TimeUnit unit
	 */
	public long getOriginalDelay(TimeUnit unit)
	{
		if(unit == TimeUnit.NANOSECONDS)
		{
			return delay;
		}
		return unit.convert(delay, TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Not to be confused with {@link DelayedTask#getDelay(TimeUnit)}, which retrieves the <b>remaining</b> time ticks left until this
	 * task is active instead of the fixed delay value. This method will instead just return the delay value
	 * as provided in the DelayedTask constructor in nanoseconds
	 * @return the delay value in nanoseconds
	 */
	public long getOriginalDelay()
	{
		return delay;
	}
	
	@Override
	public int compareTo(Delayed right)
	{
		if(this == right)
		{
			return 0;
		}
		long dl = this.getDelay(TimeUnit.NANOSECONDS);
		long dr = right.getDelay(TimeUnit.NANOSECONDS);
		long diff = dl - dr;
		if(diff < 0)
		{
			return -1;
		}
		if(diff == 0)
		{
			return 0;
		}
		return 1;
	}

	@Override
	public long getDelay(TimeUnit unit)
	{
		return delay - (System.nanoTime() - initNanos);
	}
}