package log;

public enum LogType
{
	INFO ("Info"),
	DEBUG ("DEBUG"),
	ERROR ("Error");
	
	private String str;
	
	LogType(String str)
	{
		this.str = str;
	}
	
	@Override
	public String toString()
	{
		return str;
	}
}