package utils;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Serializable
 * @author mojo
 *
 */
public class Search implements Serializable
{
	private static final long serialVersionUID = 8056216165085548111L;
	private Pattern p;
	private transient Matcher match;
	private String needle;
	private int flags;

	public Search(String needle) throws PatternSyntaxException
	{
		this.needle = needle;
		p = Pattern.compile(needle);
	}
	
	public boolean isCaseInsensitive()
	{
		return (flags & Pattern.CASE_INSENSITIVE) == Pattern.CASE_INSENSITIVE;
	}
	
	/**
	 * 
	 * @param needle
	 * @param flags {@link Pattern} flag bitfield
	 * @throws PatternSyntaxException
	 */
	public Search(String needle, int flags) throws PatternSyntaxException
	{
		this.needle = needle;
		this.flags = flags;
		p = Pattern.compile(needle, flags);
	}
	
	/**
	 * Run regex search against haystack
	 * @param haystack
	 * @return true if a match was found, false if no match or if haystack is null
	 */
	public boolean in(String haystack)
	{
		if(haystack == null)
		{
			return false;
		}
		if(match != null)
		{
			match.reset();
		}
		match = p.matcher(haystack);
		return match.find();
	}
	
	public String[] getHits()
	{
		if(match == null)
		{
			return null;
		}
		LinkedHashSet<String> matches = new LinkedHashSet<String>();
		
		if(match.find(0))
		{
			do
			{
				for(int i = 0; i < match.groupCount(); ++i)
				{
					matches.add(match.group(i + 1));
				}
				matches.add(match.group());
			} while(match.find());
		}
		
		return matches.toArray(new String[3]);
	}
	
	public String getPattern()
	{
		return p.pattern();
	}
	
	/**
	 * Use this for testing regex patterns
	 * @param regex The regex to be tested by {@link Pattern#compile(String)}
	 * @return An error description if the regex is invalid, or null if there are no errors
	 */
	public static String validate(String regex)
	{
		String rval = null;
		try
		{
			Pattern.compile(regex);
		}
		catch(PatternSyntaxException e)
		{
			rval = e.getMessage();
		}
		return rval;
	}
	
	@Override
	public String toString()
	{
		return needle;
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof Search)
		{
			return this.needle.equals(((Search)obj).needle);
		}
		return false;
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public int hashCode()
	{
		return needle.hashCode();
	}
}