package memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SharedHashMap<K, V>
{
	private HashMap<K, V> map;
	
	public SharedHashMap()
	{
		map = new HashMap<K, V>();
	}
	
	public synchronized boolean ins(K key, V value)
	{
		boolean rval = false;
		if(!map.containsKey(key))
		{
			rval = true;
			map.put(key, value);
		}
		return rval;
	}
	
	public synchronized boolean del(K key)
	{
		boolean rval = false;
		if(map.containsKey(key))
		{
			rval = true;
			map.remove(key);
		}
		return rval;
	}
	
	public synchronized boolean contains(K key)
	{
		return map.containsKey(key);
	}
	
	public synchronized int size()
	{
		return map.size();
	}
	
	public synchronized V get(K key)
	{
		return map.get(key);
	}
	
	public synchronized Set<K> getKeySet()
	{
		HashSet<K> rval = new HashSet<K>();
		for(K key : map.keySet())
		{
			rval.add(key);
		}
		return rval;
	}
	
	public synchronized List<K> getKeySetAsList()
	{
		ArrayList<K> rval = new ArrayList<K>();
		for(K key : map.keySet())
		{
			rval.add(key);
		}
		return rval;
	}
	
	public synchronized Set<V> getValueSet()
	{
		HashSet<V> rval = new HashSet<V>();
		for(V value : map.values())
		{
			rval.add(value);
		}
		return rval;
	}
	
	/**
	 * This is synchronized... works?
	 */
	@Override
	public synchronized String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(K key : map.keySet())
		{
			sb.append(key.toString());
			V value = map.get(key);
			if(value != null)
			{
				sb.append(": ");
				sb.append(map.get(key).toString());
			}
			sb.append("\n\n");
		}
		if(sb.length() > 2)
		{
			return sb.substring(0, sb.length() - 2);
		}
		return sb.toString();
	}
}