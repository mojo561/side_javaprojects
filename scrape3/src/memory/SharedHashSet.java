package memory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SharedHashSet<V> implements Serializable
{
	private static final long serialVersionUID = 6966462906894370260L;
	private HashSet<V> set;
	
	public SharedHashSet()
	{
		set = new HashSet<V>();
	}
	
	public synchronized boolean ins(V value)
	{
		return set.add(value);
	}
	
	public synchronized boolean del(V value)
	{
		return set.remove(value);
	}
	
	public synchronized boolean contains(V value)
	{
		return set.contains(value);
	}
	
	public synchronized Set<V> getValueSet()
	{
		HashSet<V> rval = new HashSet<V>();
		for(V value : set)
		{
			rval.add(value);
		}
		return rval;
	}
	
	public synchronized List<V> getValuesAsList()
	{
		ArrayList<V> rval = new ArrayList<V>();
		for(V value : set)
		{
			rval.add(value);
		}
		return rval;
	}
	
	/**
	 * Synchronized...worky?
	 */
	@Override
	public synchronized String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(V value : set)
		{
			sb.append(value.toString());
			sb.append('\n');
		}
		if(sb.length() > 0)
		{
			return sb.substring(0, sb.length() - 1);
		}
		return sb.toString();
	}
}