package boardViews;

import java.io.Serializable;

/**
 * Serializable
 * @author mojo
 *
 */
public class Board implements Serializable
{
	private static final long serialVersionUID = -4562588052983968452L;
	private String board;
	private byte ws_board;

	public String getBoard()
	{
		return board;
	}

	public byte getWs_board()
	{
		return ws_board;
	}
	
	/**
	 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
	 */
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof Board)
		{
			if(obj == this)
			{
				return true;
			}
			return board.equals( ((Board)obj).board );
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
}