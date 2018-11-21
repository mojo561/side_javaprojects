package tasks;

public enum TaskState
{
	/**
	 * In this state, the task hasn't started yet (default state in other words)
	 */
	STOPPED ("Stopped"),
	/**
	 * In this state, the task is currently executing
	 */
	RUNNING ("Running"),
	/**
	 * In this state, the task is sleeping / waiting
	 */
	IDLE ("Idle"),
	/**
	 * In this state, the task finished without error and is no longer executing or sleeping / waiting
	 */
	FINISHED ("Finished"),
	/**
	 * In this state, the task failed and is no longer executing or sleeping / waiting
	 */
	FAILED ("Failed"),
	/**
	 * In this state, something went wrong and the task is still executing
	 */
	FAILING("Failing");
	
	private String estr;
	
	TaskState(String estr)
	{
		this.estr = estr;
	}
	
	@Override
	public String toString()
	{
		return estr;
	}
}