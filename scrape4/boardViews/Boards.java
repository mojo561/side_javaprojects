package boardViews;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Serializable
 * @author mojo
 *
 */
public class Boards implements Serializable
{
	private static final long serialVersionUID = -370012739417170154L;
	private HashSet<Board> boards;
	
	//hmm...
	public HashMap<String, Board> getBoardsAsMap()
	{
		HashMap<String, Board> rval = new HashMap<String, Board>();
		for(Board b : boards)
		{
			rval.put(b.getBoard(), b);
		}
		return rval;
	}
}