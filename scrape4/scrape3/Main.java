package scrape3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Set;

import log.LogType;
import log.LogWriter;
import memory.SharedHashSet;
import tasks.CatalogPollTask;
import tasks.ThreadPollChildTask.ChildType;
import tasks.ThreadPollParentTask;
import threadViews.CatalogPollType;
import utils.Conversions;
import utils.Search;
import utils.WebResources;

public class Main
{
	private static final String imageHashListSerialFileName = "imagehashlist.ser";
	private static final String activeThreadSerialFileName = "activeThreads.ser";
	private static final String catalogListSerialFileName = "catalogList.ser";
	
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			for(String s : args)
			{
				if(!init(s))
				{
					return;
				}
			}
		}
		else
		{
			if(!init())
			{
				return;
			}
		}
		Scanner scanner = new Scanner(System.in);
		String input = "";
		while(!input.toLowerCase().equals("quit"))
		{
			input = "";
			System.out.print(String.format("Quit: exit program\n1: %s\n2: %s\n> ", "Catalog search options", "Thread specific options"));
			if(scanner.hasNextLine())
			{
				input = scanner.nextLine();
				switch(input)
				{
				case "1":
					catalogDialogPrompt(scanner);
					break;
				case "2":
					threadDialogPrompt(scanner);
					break;
				}
			}
		}
		scanner.close();
		
		serializeStuff();
		
		System.out.print("terminating . . . ");
		WebResources.getInstance().terminateAll();
		System.out.println("OK");
	}
	
	private static boolean init()
	{
		File imgdir = new File("images");
		if(!imgdir.exists())
		{
			try
			{
				Set<PosixFilePermission> posixperms = FileResources.getInstance().getImageDirPermissions();
				Files.createDirectory(imgdir.toPath(), PosixFilePermissions.asFileAttribute(posixperms));
				Files.setPosixFilePermissions(imgdir.toPath(), posixperms);
			}
			catch (IOException e2)
			{
				LogWriter.getInstance().write(LogType.ERROR, e2.getMessage());
				return false;
			}
		}
		
		if(!dumpResourceFile("webfiles/index.php", "index.php"))
		{
			return false;
		}
		if(!dumpResourceFile("webfiles/deleteafile.php", "deleteafile.php"))
		{
			return false;
		}
		if(!dumpResourceFile("webfiles/deletefiles.php", "deletefiles.php"))
		{
			return false;
		}
		if(!dumpResourceFile("webfiles/scripts/script.js", "scripts", "script.js"))
		{
			return false;
		}
		if(!dumpResourceFile("webfiles/styles/grid.css", "styles", "grid.css"))
		{
			return false;
		}
		if(!dumpResourceFile("webfiles/styles/vert.css", "styles", "vert.css"))
		{
			return false;
		}
		
		if(!WebResources.getInstance().getBoardList().init("http://a.4cdn.org/boards.json"))
		{
			return false;
		}
		
		File f = new File(imageHashListSerialFileName);
		if(f.exists())
		{
			try(FileInputStream fis = new FileInputStream(f); ObjectInputStream ois = new ObjectInputStream(fis))
			{
				@SuppressWarnings("unchecked")
				SharedHashSet<String> tmp = (SharedHashSet<String>)ois.readObject();
				for(String s : tmp.getValueSet())
				{
					WebResources.IMAGEDL_RECORD.ins(s);
				}
			}
			catch (ClassNotFoundException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
				return false;
			}
			catch (FileNotFoundException e1)
			{
				LogWriter.getInstance().write(LogType.ERROR, e1.getMessage());
				return false;
			}
			catch (IOException e1)
			{
				LogWriter.getInstance().write(LogType.ERROR, e1.getMessage());
				return false;
			}
		}
		
		f = new File(activeThreadSerialFileName);
		if(f.exists())
		{
			try(FileInputStream fis = new FileInputStream(f); ObjectInputStream ois = new ObjectInputStream(fis))
			{
				ThreadPollParentTask deserializedActiveList[] = (ThreadPollParentTask[])ois.readObject();
				for(ThreadPollParentTask tppt : deserializedActiveList)
				{
					WebResources.getInstance().submitAndInsertNewThreadPollTask(tppt);
				}
			}
			catch (ClassNotFoundException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
				return false;
			}
			catch (FileNotFoundException e1)
			{
				LogWriter.getInstance().write(LogType.ERROR, e1.getMessage());
				return false;
			}
			catch (IOException e1)
			{
				LogWriter.getInstance().write(LogType.ERROR, e1.getMessage());
				return false;
			}
		}
		
		f = new File(catalogListSerialFileName);
		if(f.exists())
		{
			try(FileInputStream fis = new FileInputStream(f); ObjectInputStream ois = new ObjectInputStream(fis))
			{
				CatalogPollTask deserializedCatalogList[] = (CatalogPollTask[])ois.readObject();
				for(CatalogPollTask task : deserializedCatalogList)
				{
					WebResources.getInstance().submitAndInsertNewCatalogPollTask(task);
				}
			}
			catch (ClassNotFoundException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
				return false;
			}
			catch (FileNotFoundException e1)
			{
				LogWriter.getInstance().write(LogType.ERROR, e1.getMessage());
				return false;
			}
			catch (IOException e1)
			{
				LogWriter.getInstance().write(LogType.ERROR, e1.getMessage());
				return false;
			}
		}

		return true;
	}
	
	private static boolean dumpResourceFile(String resourcepath, String filename)
	{
		ClassLoader cl = Main.class.getClassLoader();
		File f = new File(filename); 
		if(!f.exists())
		{
			try(	InputStream is = cl.getResourceAsStream(resourcepath);
					InputStreamReader sr = new InputStreamReader(is);
					BufferedReader reader = new BufferedReader(sr);
					FileWriter fw = new FileWriter(f))
			{
				String line;
				while((line = reader.readLine()) != null)
				{
					fw.write(line);
					fw.write('\n');
				}
			}
			catch (IOException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
				return false;
			}
			try
			{
				Files.setPosixFilePermissions(f.toPath(), FileResources.getInstance().getWebFilePermissions());
			}
			catch (IOException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
				return false;
			}
		}
		return true;
	}
	
	private static boolean dumpResourceFile(String resourcepath, String parentdir, String filename)
	{
		File pd = new File(parentdir);
		if(!pd.exists())
		{
			Set<PosixFilePermission> posixperms = FileResources.getInstance().getWebFileDirPermissions();
			try
			{
				Files.createDirectory(pd.toPath(), PosixFilePermissions.asFileAttribute(posixperms));
				Files.setPosixFilePermissions(pd.toPath(), posixperms);
			}
			catch (IOException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
				return false;
			}
		}
		return dumpResourceFile(resourcepath, String.format("%s/%s", parentdir, filename));
	}
	
	private static boolean init(String imgdir)
	{
		Path path = Paths.get(imgdir);
		File test = path.toFile();
		if(!test.exists() || !test.isDirectory() || !test.canRead())
		{
			LogWriter.getInstance().write(LogType.ERROR, String.format("Couldn't access %s", imgdir));
			return false;
		}
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			Encoder e = Base64.getEncoder();
			File filelist[] = path.toFile().listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					return !name.endsWith("s.jpg"); //ignore files that follow thumbnail naming convention
				}
			});
			if(filelist.length > 0)
			{
				String msg = String.format("Parsing files in dir \"%s\", this could take a while...", imgdir);
				System.out.println(msg);
				for(File f : filelist)
				{
					Path fileloc = Paths.get(f.getAbsolutePath());
					md.update(Files.readAllBytes(fileloc));
					WebResources.IMAGEDL_RECORD.ins(e.encodeToString(md.digest()));
				}
				for(int i = 0; i < msg.length(); ++i)
				{
					System.out.print('\b');
				}
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			LogWriter.getInstance().write(LogType.ERROR, String.format("%s\n%s", imgdir, e.getMessage()));
			return false;
		}
		catch (IOException e1)
		{
			LogWriter.getInstance().write(LogType.ERROR, String.format("%s\n%s", imgdir, e1.getMessage()));
			return false;
		}

		return init();
	}
	
	public static boolean serializeStuff()
	{
		try(FileOutputStream fos = new FileOutputStream(imageHashListSerialFileName); ObjectOutputStream oos = new ObjectOutputStream(fos);)
		{
			oos.writeObject(WebResources.IMAGEDL_RECORD);
			Files.setPosixFilePermissions(Paths.get(imageHashListSerialFileName), FileResources.getInstance().getLocalFilePermissions());
		}
		catch(IOException e)
		{
			LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
			return false;
		}
		
		Set<ThreadPollParentTask> activelist = WebResources.getInstance().getActiveThreadMap().getKeySet();
		if(activelist.size() > 0)
		{
			ThreadPollParentTask serializableActiveList[] = activelist.toArray(new ThreadPollParentTask[activelist.size()]);
			try(FileOutputStream fos = new FileOutputStream(activeThreadSerialFileName); ObjectOutputStream oos = new ObjectOutputStream(fos);)
			{
				oos.writeObject(serializableActiveList);
				Files.setPosixFilePermissions(Paths.get(activeThreadSerialFileName), FileResources.getInstance().getLocalFilePermissions());
			}
			catch(IOException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
				return false;
			}
		}

		Set<CatalogPollTask> catalogList = WebResources.getInstance().getActiveCatalogMap().getKeySet();
		if(catalogList.size() > 0)
		{
			CatalogPollTask serializableCatalogList[] = catalogList.toArray(new CatalogPollTask[catalogList.size()]);
			try(FileOutputStream fos = new FileOutputStream(catalogListSerialFileName); ObjectOutputStream oos = new ObjectOutputStream(fos);)
			{
				oos.writeObject(serializableCatalogList);
				Files.setPosixFilePermissions(Paths.get(catalogListSerialFileName), FileResources.getInstance().getLocalFilePermissions());
			}
			catch(IOException e)
			{
				LogWriter.getInstance().write(LogType.ERROR, e.getMessage());
				return false;
			}
		}

		return true;
	}
	
	private static void catalogDialogPrompt(Scanner cdpscanner)
	{
		String cdpinput;
		do
		{
			cdpinput = "";
			System.out.print(String.format("0: %s\n1: %s\n2: %s\n3: %s\n4: %s\n> ",
					"Go back to main menu",
					"List active catalog tasks",
					"Add new catalog task",
					"Modify catalog task search patterns",
					"Delete a catalog task"));
			if(cdpscanner.hasNextLine())
			{
				cdpinput = cdpscanner.nextLine();
			}
			
			if(cdpinput.equals("0"))
			{
				continue;
			}
			
			switch(cdpinput)
			{
			case "1":
				for(CatalogPollTask cpt : WebResources.getInstance().getActiveCatalogMap().getKeySet())
				{
					System.out.println(cpt);
					System.out.print('\n');
				}
				break;
			case "2":
				String opt2input = "";
				Set<String> existingBoardSet = WebResources.getInstance().getCatalogBoardSet();
				System.out.print("Enter board (blank to cancel):");
				if(cdpscanner.hasNextLine())
				{
					opt2input = cdpscanner.nextLine();
				}
				if(opt2input.trim().length() > 0)
				{
					if(!existingBoardSet.contains(opt2input))
					{
						String board = opt2input;
						String freq = "";
						CatalogPollTask catTask = null;
						do
						{
							System.out.print("Select frequency:\n1: Constant (default)\n2: Variable\n[1]: ");
							opt2input = "";
							if(cdpscanner.hasNextLine())
							{
								opt2input = cdpscanner.nextLine();
							}
							if(opt2input.trim().length() == 0)
							{
								freq = "1";
								break;
							}
							else
							{
								freq = opt2input;
								if(freq.equals("1"))
									break;
								if(freq.equals("2"))
									break;
							}
						} while (true);
						
						if(freq.equals("1"))
						{
							catTask = new CatalogPollTask(board, CatalogPollType.CONSTANT);
						}
						else
						{
							catTask = new CatalogPollTask(board, CatalogPollType.VARIABLE);
						}
						
						do
						{
							String searchstr = "";
							String err = null;
							System.out.print("Enter search term (blank to continue): ");
							if(cdpscanner.hasNextLine())
							{
								searchstr = cdpscanner.nextLine();
							}
							if(searchstr.trim().length() == 0)
							{
								break;
							}
							
							err = Search.validate(searchstr);
							if(err == null)
							{
								String conf = "n";
								do
								{
									System.out.print("Case insensitive? (Y/N): ");
									if(cdpscanner.hasNextLine())
									{
										conf = cdpscanner.nextLine().toLowerCase();
									}
									if(conf.equals("y") || conf.equals("n"))
									{
										break;
									}
								} while(true);
								if(conf.equals("y"))
								{
									catTask.addSearch(new Search(searchstr, Pattern.CASE_INSENSITIVE));
								}
								else if(conf.equals("n"))
								{
									catTask.addSearch(new Search(searchstr));
								}
							}
							else
							{
								System.out.println(err);
							}
							
						} while(true);
						System.out.println(String.format("\n%s\n", catTask.toString()));
						do
						{
							cdpinput = "";
							System.out.print("Is this ok? (Y/N): ");
							if(cdpscanner.hasNextLine())
							{
								cdpinput = cdpscanner.nextLine();
							}
							if(cdpinput.toLowerCase().equals("y"))
							{
								System.out.println("\nAdded\n");
								WebResources.getInstance().submitAndInsertNewCatalogPollTask(catTask);
								break;
							}
							else if(cdpinput.toLowerCase().equals("n"))
							{
								System.out.println("\nCancelled\n");
								break;
							}
						} while(true);
					}
					else
					{
						System.out.println(String.format("Already watching %s, cancelling creation process", opt2input));
					}
				}
				else
				{
					System.out.println("\nCancelled\n");
				}
				break;
			case "3":
				HashMap<String, CatalogPollTask> map = new HashMap<String, CatalogPollTask>();
				for(CatalogPollTask cpt : WebResources.getInstance().getActiveCatalogMap().getKeySet())
				{
					System.out.println(cpt.getBoard());
					map.put(cpt.getBoard(), cpt);
				}
				if(map.size() > 0)
				{
					CatalogPollTask chosen = null;
					do
					{
						String choice = "";
						System.out.print("Which would you like to modify? (blank to cancel): ");
						if(cdpscanner.hasNextLine())
						{
							choice = cdpscanner.nextLine();
						}
						if(choice.trim().length() == 0)
						{
							break;
						}
						if(map.containsKey(choice))
						{
							chosen = map.get(choice);
							break;
						}
					} while(true);
					if(chosen != null)
					{
						boolean modprompt = true;
						do
						{
							String choice = "";
							System.out.print(String.format("0: %s\n1: %s\n2: %s\n3: %s\n> ",
									"Go back",
									"Add more search patterns",
									"Delete a search pattern",
									"List search patterns"));
							if(cdpscanner.hasNextLine())
							{
								choice = cdpscanner.nextLine();
							}
							switch(choice)
							{
							case "0":
								modprompt = false;
								break;
							case "1":
								do
								{
									String searchstr = "";
									String err = null;
									System.out.print("Enter search term (blank to continue): ");
									if(cdpscanner.hasNextLine())
									{
										searchstr = cdpscanner.nextLine();
									}
									if(searchstr.trim().length() == 0)
									{
										break;
									}
									err = Search.validate(searchstr);
									if(err == null)
									{
										String conf = "n";
										do
										{
											System.out.print("Case insensitive? (Y/N): ");
											if(cdpscanner.hasNextLine())
											{
												conf = cdpscanner.nextLine().toLowerCase();
											}
											if(conf.equals("y") || conf.equals("n"))
											{
												break;
											}
										} while(true);
										if(conf.equals("y"))
										{
											chosen.addSearch(new Search(searchstr, Pattern.CASE_INSENSITIVE));
										}
										else if(conf.equals("n"))
										{
											chosen.addSearch(new Search(searchstr));
										}
										System.out.println("Added");
									}
									else
									{
										System.out.println(err);
									}
								} while (true);
								break;
							case "2":
								if(chosen.getSearches().length > 0)
								{
									do
									{
										String modopt2choice = "";
										int delindex = -1;
										Search searchArray[] = chosen.getSearches();
										for(int i = 0; i < searchArray.length; ++i)
										{
											if(searchArray[i].isCaseInsensitive())
											{
												System.out.println(String.format("%d (i) \"%s\"", i, searchArray[i].toString()));
											}
											else
											{
												System.out.println(String.format("%d \"%s\"", i, searchArray[i].toString()));
											}
										}
										
										System.out.print("Enter a number to delete the corresponding search (or blank to go back): ");
										if(cdpscanner.hasNextLine())
										{
											modopt2choice = cdpscanner.nextLine();
										}
										if(modopt2choice.trim().length() == 0)
										{
											break;
										}
										if(modopt2choice.equals("0"))
										{
											delindex = 0;
										}
										else
										{
											delindex = (int)Conversions.tryParse(modopt2choice);
											if(delindex == 0)
											{
												continue;
											}
										}
										if(delindex < searchArray.length)
										{
											System.out.println(String.format("Deleting pattern \"%s\"", searchArray[delindex]));
											chosen.deleteSearch(searchArray[delindex]);
										}
										else
										{
											System.out.println(String.format("%d is out of bounds", delindex));
										}
									} while(chosen.getSearches().length > 0);
								}
								else
								{
									System.out.println("\nNo searches to delete\n");
								}
								break;
							case "3":
								Search searchArray[] = chosen.getSearches();
								if(searchArray.length > 0)
								{
									System.out.print('\n');
									for(Search s : searchArray)
									{
										System.out.println(String.format("\"%s\"", s.toString()));
									}
									System.out.print('\n');
								}
								else
								{
									System.out.println("\nNo searches\n");
								}
								break;
							}
						} while(modprompt);
					}
					else
					{
						System.out.println("\nCancelled\n");
					}
				}
				else
				{
					System.out.println("\nNo catalog tasks to modify\n");
				}
				break;
			case "4":
				HashMap<String, CatalogPollTask> map2 = new HashMap<String, CatalogPollTask>();
				for(CatalogPollTask cpt : WebResources.getInstance().getActiveCatalogMap().getKeySet())
				{
					System.out.println(cpt.getBoard());
					map2.put(cpt.getBoard(), cpt);
				}
				if(map2.size() > 0)
				{
					do
					{
						String choice = "";
						System.out.print("Enter the board of the task to be deleted (blank to cancel): ");
						if(cdpscanner.hasNextLine())
						{
							choice = cdpscanner.nextLine();
						}
						if(choice.trim().length() == 0)
						{
							System.out.println("\nCancelled\n");
							break;
						}
						if(map2.containsKey(choice))
						{
							CatalogPollTask chosen = map2.get(choice);
							if(WebResources.getInstance().removeCatalogPollTask(chosen))
							{
								System.out.println(String.format("\nDeleted %s\n", choice));
							}
							else
							{
								System.out.println(String.format("\nCould NOT Delete %s (should not happen!)\n", choice));
							}
							break;
						}
					} while(true);
				}
				break;
			}
			
		}while(!cdpinput.equals("0"));
	}
	
	private static void threadDialogPrompt(Scanner tdpscanner)
	{
		String tdpinput;
		do
		{
			tdpinput = "";
			System.out.print(String.format("0: %s\n1: %s\n2: %s\n3: %s\n4: %s\n5: %s\n6: %s\n> ",
					"Go back to main menu",
					"List active threads",
					"Add new thread",
					"List inactive threads",
					"Cancel an active task",
					"Delete an inactive record",
					"Delete all inactive records"));

			if(tdpscanner.hasNextLine())
			{
				tdpinput = tdpscanner.nextLine();
			}
			
			if(tdpinput.equals("0"))
			{
				continue;
			}
			
			switch (tdpinput)
			{
			case "1":
				List<ThreadPollParentTask> activelist = WebResources.getInstance().getActiveThreadMap().getKeySetAsList();
				Collections.sort(activelist);
				for(ThreadPollParentTask parent : activelist)
				{
					System.out.println(parent);
				}
				
				int total = WebResources.IMAGEDOWNLOAD_DELAYEDTASK_QUEUE.getTotalQueuedTasks();
				if(total == 0)
				{
					System.out.println("\n0 queued image tasks\n");
				}
				else if (total == 1)
				{
					System.out.println("\nThere is 1 queued image task\n");
				}
				else
				{
					System.out.println(String.format("\nThere are %d queued image tasks\n", total));
				}
				break;
			case "2":
				/* ------------------------------------------- */
				String opt2input = "";
				System.out.println("Enter URL (blank to cancel and go back to main menu)");
				if(tdpscanner.hasNextLine())
				{
					opt2input = tdpscanner.nextLine();
				}
				if(opt2input.trim().length() > 0)
				{
					if(WebResources.THREAD_URL_MATCH.in(opt2input))
					{
						System.out.print("How fast should images be downloaded?\n0. Cancel\n1. Fast (default)\n2. Slow\n[1]: ");
						if(tdpscanner.hasNextLine())
						{
							opt2input = tdpscanner.nextLine();
						}
						else
						{
							opt2input = "0";
						}
						
						if(!opt2input.equals("0"))
						{
							String matches[] = WebResources.THREAD_URL_MATCH.getHits();
							ThreadPollParentTask task = null;
							if(opt2input.equals("2"))
							{
								task = new ThreadPollParentTask(matches[0], Conversions.tryParse(matches[1]), ChildType.SLOW);
								System.out.println("[selected slow]");
							}
							else
							{
								task = new ThreadPollParentTask(matches[0], Conversions.tryParse(matches[1]), ChildType.FAST);
								System.out.println("[selected fast]");
							}
							if(task != null && WebResources.getInstance().submitAndInsertNewThreadPollTask(task))
							{
								System.out.println(String.format("\nAdded: %s\n", matches[2]));
							}
							else
							{
								//TODO: check if it's actually a duplicate...
								System.out.println(String.format("\nCouldn't add %s (duplicate)\n", matches[2]));
							}
						}
						else
						{
							System.out.println("\nCancelled\n");
						}
					}
					else
					{
						System.out.println("\nInvalid input, could not add URL\n");
					}
				}
				else
				{
					System.out.println("\nCancelled\n");
				}
				/* ------------------------------------------- */
				break;
			case "3":
				
				List<ThreadPollParentTask> inactivelist = WebResources.getInstance().getInactiveThreadSet().getValuesAsList();
				Collections.sort(inactivelist);
				for(ThreadPollParentTask parent : inactivelist)
				{
					System.out.println(parent);
				}
				System.out.print('\n');
				break;
			case "4":
				String opt4input = "";
				Set<ThreadPollParentTask> tasklist = WebResources.getInstance().getActiveThreadMap().getKeySet();
				if(tasklist.size() > 0)
				{
					for(ThreadPollParentTask tppt : tasklist)
					{
						System.out.println(String.format("%d\t%s", tppt.getThreadID(), tppt.toString()));
					}
					System.out.println("Enter a thread ID (blank to cancel and go back to main menu): ");
					if(tdpscanner.hasNextLine())
					{
						opt4input = tdpscanner.nextLine();
					}
					if(opt4input.trim().length() > 0)
					{
						long l = Conversions.tryParse(opt4input);
						ThreadPollParentTask kos = null;
						for(ThreadPollParentTask tppt : tasklist)
						{
							if(tppt.getThreadID() == l)
							{
								kos = tppt;
								break;
							}
						}
						if(WebResources.getInstance().terminateTask(kos))
						{
							System.out.println("\nCancelled task\n");
						}
						else
						{
							System.out.println("\nCouldn't cancel task (did it finish?)\n");
						}
					}
					else
					{
						System.out.println("\nCancelled\n");
					}
				}
				else
				{
					System.out.println("\nNo active tasks to cancel\n");
				}
				break;
			case "5":
				String opt5input = "";
				Set<ThreadPollParentTask> inactiveTaskList = WebResources.getInstance().getInactiveThreadSet().getValueSet();
				if(inactiveTaskList.size() > 0)
				{
					for(ThreadPollParentTask tppt : inactiveTaskList)
					{
						System.out.println(String.format("%d\t%s", tppt.getThreadID(), tppt.toString()));
					}
					System.out.println("Enter a thread ID (blank to cancel and go back to main menu): ");
					if(tdpscanner.hasNextLine())
					{
						opt5input = tdpscanner.nextLine();
					}
					if(opt5input.trim().length() > 0)
					{
						long l = Conversions.tryParse(opt5input);
						ThreadPollParentTask dos = null;
						for(ThreadPollParentTask tppt : inactiveTaskList)
						{
							if(tppt.getThreadID() == l)
							{
								dos = tppt;
								break;
							}
						}
						if(dos != null && WebResources.getInstance().getInactiveThreadSet().del(dos))
						{
							System.out.println("\nDeleted the task\n");
						}
						else
						{
							System.out.println("\nCould not delete the task\n");
						}
					}
					else
					{
						System.out.println("\nCancelled\n");
					}
				}
				else
				{
					System.out.println("\nNo inactive tasks to delete\n");
				}
				break;
			case "6":
				String opt6input = "";
				Set<ThreadPollParentTask> inactiveTaskList2 = WebResources.getInstance().getInactiveThreadSet().getValueSet();
				if(inactiveTaskList2.size() > 0)
				{
					for(ThreadPollParentTask tppt : inactiveTaskList2)
					{
						System.out.println(String.format("%d\t%s", tppt.getThreadID(), tppt.toString()));
					}
					System.out.print("Really delete all inactive tasks? (Y/N): ");
					if(tdpscanner.hasNextLine())
					{
						opt6input = tdpscanner.nextLine();
					}
					if(opt6input.toLowerCase().equals("y"))
					{
						for(ThreadPollParentTask tppt : inactiveTaskList2)
						{
							if(WebResources.getInstance().getInactiveThreadSet().del(tppt))
							{
								System.out.println(String.format("Deleted %d", tppt.getThreadID()));
							}
							else
							{
								System.out.println(String.format("Could not delete %s!", tppt.toString()));
							}
						}
					}
					else
					{
						System.out.println("\nCancelled\n");
					}
				}
				break;
			}
		}while(!tdpinput.equals("0"));
	}
}