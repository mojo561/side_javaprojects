package threadViews;

public enum CatalogPollType
{
	/**
	 * The task will scan the first page every time it has ran an even number of times, and the entire catalog
	 * every time it has ran an odd number of times.
	 */
	VARIABLE,
	/**
	 * The task will scan the entire catalog every time when it runs.
	 */
	CONSTANT
}