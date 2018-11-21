package threadViews;

public class ThreadView
{
	private Post posts[];
	
	public Post[] getPosts()
	{
		return posts;
	}
	
	/**
	 * Representation of a single post in a specific thread
	 * @author mojo
	 */
	public class Post
	{
		private byte closed;
		private byte archived;
		private short w;
		private short h;
		private short tn_w;
		private short tn_h;
		private int fsize;
		//private int replies;
		//private int images;
		private long no;
		private long tim;
		//private long time;
		private long resto;
		private long archived_on;
		private String now;
		private String name;
		private String trip;
		private String sub;
		private String com;
		private String filename;
		private String ext;
		private String semantic_url;
		private String md5;
		private String tag;
		private String filesize;
		private String sanitizedMD5;
		private String shortfilename;
		
//		public byte closed() {
//			return closed;
//		}
//		
//		public byte archived() {
//			return archived;
//		}
		
		/**
		 * @return true iff the thread was closed
		 */
		public boolean isClosed() {
			return (closed != 0);
		}

		/**
		 * @return true iff the thread is archived
		 */
		public boolean isArchived() {
			return (archived != 0);
		}
		
		/**
		 * @return the source image width
		 */
		public short getW() {
			return w;
		}

		/**
		 * @return the source image height
		 */
		public short getH() {
			return h;
		}

		/**
		 * @return thumbnail width
		 */
		public short getTn_w() {
			return tn_w;
		}

		/**
		 * @return thumbnail height
		 */
		public short getTn_h() {
			return tn_h;
		}

		/**
		 * 0-10485760
		 * @return the file size of the source image (in bytes)
		 */
		public int getFsize() {
			return fsize;
		}

		/**
		 * UNIX timestamp + milliseconds. ex: "1344402680740"
		 * @return the renamed source filename
		 */
		public long getTim() {
			return tim;
		}

		/**
		 * @return the UNIX timestamp of when the post was submitted
		 */
//		public long getTime() {
//			return time;
//		}

		/**
		 * 0 is a thread OP
		 * @return the thread ID (OP post ID) that this post is replying to
		 */
		public long getResto() {
			return resto;
		}
		
		/**
		 * @return the UNIX timestamp of when the thread was archived
		 */
		public long getArchived_on() {
			return archived_on;
		}

		/**
		 * MM/DD/YY(Day)HH:MM (:SS on some boards), EST/EDT timezone
		 * @return the timestamp of when the post was submitted
		 */
		public String getNow() {
			return now;
		}

		/**
		 * .jpg, .png, .gif, .pdf, .swf, .webm
		 * @return the file's extension
		 */
		public String getExt() {
			return ext;
		}

		/**
		 * # replies total
		 * @return the total number of replies
		 */
//		public int getReplies() {
//			return replies;
//		}

		/**
		 * # images total
		 * @return the total number of images
		 */
//		public int getImages() {
//			return images;
//		}
		
		/**
		 * Post number 1-9999999999999
		 * @return the post ID
		 */
		public long getNo() {
			return no;
		}

		/**
		 * ex: "moot"
		 * @return the poster's name, or null
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
		 * 24 character, packed base64 MD5 hash. Ex: NOetrLVnES3jUn1x5ZPVAg==
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
		
		/**
		 * @return formatted source file size in bytes, kilobytes, or megabytes
		 */
		public String getFilesize()
		{
			if(filesize == null)
			{
				if(fsize >= 0 && fsize < 1024)
					filesize = String.format("%d B", fsize);
				else if(fsize >= 1024 && fsize < 1048576)
					filesize = String.format("%.2f KB", (double)fsize / 1024);
				else if(fsize >= 1048576)
					filesize = String.format("%.2f MB", (double)fsize / 1024 / 1024);
			}
			return filesize;
		}
		
		public String getSanitizedMD5()
		{
			if(sanitizedMD5 == null && md5 != null)
			{
				StringBuilder sb = new StringBuilder(60);
				for(byte b : md5.getBytes())
				{
					sb.append(String.format("%d", b));
				}
				sanitizedMD5 = sb.toString();
			}
			return sanitizedMD5;
		}
		
		public String getShortfilename()
		{
			if(shortfilename == null && filename != null)
			{
				if(filename.length() > 24)
				{
					shortfilename = filename.substring(0, 25) + "(...)";
				}
				else
				{
					shortfilename = filename;
				}
			}
			return shortfilename;
		}
		
		/**
		 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
		 */
		@Override
		public boolean equals(Object obj)
		{
			if(obj instanceof Post)
			{
				if(obj == this)
				{
					return true;
				}
				return (this.no == ((Post)obj).no);
			}
			return false;
		}
		
		/**
		 * Need to override .equals and hashCode before use in Hashed data structures such as HashSet or LinkedHashSet
		 */
		@Override
		public int hashCode()
		{
			return Long.hashCode(no);
		}
	}
}