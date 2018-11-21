package scrape3;

import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class FileResources
{
	private static volatile FileResources instance;
	private HashSet<PosixFilePermission> imageDirPermissions;
	private HashSet<PosixFilePermission> threadDirPermissions;
	private HashSet<PosixFilePermission> logDirPermissions;
	private HashSet<PosixFilePermission> localFilePermissions;
	private HashSet<PosixFilePermission> webFilePermissions;
	private HashSet<PosixFilePermission> webFileDirPermissions;
	
	private FileResources()
	{
		imageDirPermissions = new HashSet<PosixFilePermission>();
		imageDirPermissions.add(PosixFilePermission.OWNER_READ);
		imageDirPermissions.add(PosixFilePermission.OWNER_WRITE);
		imageDirPermissions.add(PosixFilePermission.OWNER_EXECUTE);
		imageDirPermissions.add(PosixFilePermission.GROUP_READ);
		imageDirPermissions.add(PosixFilePermission.GROUP_WRITE);
		imageDirPermissions.add(PosixFilePermission.GROUP_EXECUTE);
		
		threadDirPermissions = new HashSet<PosixFilePermission>();
		threadDirPermissions.add(PosixFilePermission.OWNER_READ);
		threadDirPermissions.add(PosixFilePermission.OWNER_WRITE);
		threadDirPermissions.add(PosixFilePermission.OWNER_EXECUTE);
		threadDirPermissions.add(PosixFilePermission.GROUP_READ);
		threadDirPermissions.add(PosixFilePermission.GROUP_WRITE);
		threadDirPermissions.add(PosixFilePermission.GROUP_EXECUTE);
		
		localFilePermissions = new HashSet<PosixFilePermission>();
		localFilePermissions.add(PosixFilePermission.OWNER_READ);
		localFilePermissions.add(PosixFilePermission.OWNER_WRITE);
		
		logDirPermissions = new HashSet<PosixFilePermission>();
		logDirPermissions.add(PosixFilePermission.OWNER_READ);
		logDirPermissions.add(PosixFilePermission.OWNER_WRITE);
		logDirPermissions.add(PosixFilePermission.OWNER_EXECUTE);
		
		webFilePermissions = new HashSet<PosixFilePermission>();
		webFilePermissions.add(PosixFilePermission.OWNER_READ);
		webFilePermissions.add(PosixFilePermission.OWNER_WRITE);
		webFilePermissions.add(PosixFilePermission.GROUP_READ);
		
		webFileDirPermissions = new HashSet<PosixFilePermission>();
		webFileDirPermissions.add(PosixFilePermission.OWNER_READ);
		webFileDirPermissions.add(PosixFilePermission.OWNER_WRITE);
		webFileDirPermissions.add(PosixFilePermission.OWNER_EXECUTE);
		webFileDirPermissions.add(PosixFilePermission.GROUP_READ);
		webFileDirPermissions.add(PosixFilePermission.GROUP_EXECUTE);
	}
	
	public static FileResources getInstance()
	{
		if(instance == null)
		{
			synchronized(FileResources.class)
			{
				if(instance == null)
				{
					instance = new FileResources();
				}
			}
		}
		return instance;
	}
	
	/**
	 * drwx rwx ---
	 */
	public Set<PosixFilePermission> getImageDirPermissions() { return imageDirPermissions; }
	/**
	 * drwx rwx ---
	 */
	public Set<PosixFilePermission> getThreadDirPermissions() { return threadDirPermissions; }
	/**
	 * drwx --- ---
	 */
	public Set<PosixFilePermission> getLogDirPermissions() { return logDirPermissions; }
	/**
	 * -rw- --- ---
	 */
	public Set<PosixFilePermission> getLocalFilePermissions() { return localFilePermissions; }
	/**
	 * -rw- r-- ---
	 */
	public Set<PosixFilePermission> getWebFilePermissions() { return webFilePermissions; }
	/**
	 * drwx r-x ---
	 */
	public Set<PosixFilePermission> getWebFileDirPermissions() { return webFileDirPermissions; }
}