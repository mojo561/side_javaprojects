package threadViews;

public class CatalogMultiPageView
{
	private byte page;
	private OPost threads[];
	
	/**
	 * @return the page number
	 */
	public byte getPage() {
		return page;
	}

	/**
	 * @return the threads (for now, just an array of OP posts)
	 */
	public OPost[] getThreads() {
		return threads;
	}

	public class OPost
	{
		private int replies;
		private int images;
		private long no;
		private long last_modified;
		private String name;
		private String trip;
		private String sub;
		private String com;
		private String filename;
		private String semantic_url;
		private String md5;
		private String tag;

		/**
		 * # replies total
		 * @return the total number of replies
		 */
		public int getReplies() {
			return replies;
		}

		/**
		 * # images total
		 * @return the total number of images
		 */
		public int getImages() {
			return images;
		}
		
		/**
		 * Post number 1-9999999999999
		 * @return the post ID
		 */
		public long getNo() {
			return no;
		}
		
		/**
		 * @return the thread's last modified UNIX timestamp
		 */
		public long getLast_modified() {
			return last_modified;
		}

		/**
		 * ex: "moot"
		 * @return the poster's name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * ex: "!Ep8pui8Vw2"
		 * @return the poster's tripcode
		 */
		public String getTrip() {
			return trip;
		}

		/**
		 * ex: "This is a subject"
		 * @return the post subject
		 */
		public String getSub() {
			return sub;
		}

		/**
		 * ex: "This is a comment"
		 * @return the post comment, or null if there is no comment (includes escaped HTML).
		 */
		public String getCom() {
			return com;
		}

		/**
		 * ex: "OPisa"
		 * @return the source filename
		 */
		public String getFilename() {
			return filename;
		}

		/**
		 * ex: "daily-programming-thread"
		 * @return the thread URL slug
		 */
		public String getSemantic_url() {
			return semantic_url;
		}

		/**
		 * 24 character, packed base64 MD5 hash
		 * @return the file md5 hash
		 */
		public String getMd5() {
			return md5;
		}

		/**
		 * ex: "Loop"
		 * @return the thread tag
		 */
		public String getTag() {
			return tag;
		}
	}
}