package def;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

/**
 * ..######...####.########.########.####.##.......########
 * .##....##...##..##.......##........##..##.......##......
 * .##.........##..##.......##........##..##.......##......
 * .##...####..##..######...######....##..##.......######..
 * .##....##...##..##.......##........##..##.......##......
 * .##....##...##..##.......##........##..##.......##......
 * ..######...####.##.......##.......####.########.########
 * @author josh
 *
 */
public class GifFile
{
	private final int EXTENSION_INTRODUCER = 0x21;
	private final int GRAPHICS_CONTROL_LABEL = 0xF9;
	private final int PLAIN_TEXT_LABEL = 0x01;
	private final int APPLICATION_EXTENSION_LABEL = 0xFF;
	private final int COMMENT_LABEL = 0xFE;
	private final int IMAGE_SEPERATOR = 0x2C;
	private final int TRAILER_MARKER = 0x3B;
	private String signature;
	private String comment;
	private String fileName;
	private RandomAccessFile raf;
	private LogicalScreenDescriptor lsd;
	private GraphicsControlExtension gce;
	private ApplicationExtension appext;
	private ArrayList<Color[]> globalColorTable;
	private ArrayList<GIFImage> gifimgs;
	public GifFile(String fileName) throws FileNotFoundException
	{
		raf = new RandomAccessFile(fileName, "r");
		this.fileName = fileName;
		gifimgs = new ArrayList<GifFile.GIFImage>();
		try
		{
			determineFileSignature();
			lsd = new LogicalScreenDescriptor();
			if(lsd.hasGlobalColorTable)
			{
				buildGlobalColorTable();
			}
			else
			{
				System.err.println("uh oh... no global color table!");
			}
			continueLoading();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(raf != null)
					raf.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public String getFileName()
	{
		return fileName;
	}
	
	private void continueLoading() throws IOException
	{
		int nextByte = raf.read();
		if(nextByte == EXTENSION_INTRODUCER)
		{	//we're in extension land
			nextByte = raf.read();
			switch (nextByte)
			{
			case GRAPHICS_CONTROL_LABEL:
				gce = new GraphicsControlExtension();
				System.out.println(GCEToString());
				continueLoading();
				break;
				
			case APPLICATION_EXTENSION_LABEL:
				appext = new ApplicationExtension();
				System.out.println(AppExtToString());
				continueLoading();
				break;
				
			case COMMENT_LABEL:
				comment = readComment();
				System.out.println(comment);
				continueLoading();
				break;
				
			case PLAIN_TEXT_LABEL:
				System.out.println("enter plaintext ext");
				break;

			default:
				System.err.println("Unknown extension!");
				break;
			}
		}
		else if(nextByte == IMAGE_SEPERATOR)
		{	//we're in image data land
			System.out.println(getSignature());
			System.out.println(LSDToString());
			if(gce != null && gce.hasTransparentColor)
			{
				gifimgs.add(new GIFImage(gce.transparentColorIndex));
//				System.out.println(GCEToString());
			}
			else
			{
				gifimgs.add(new GIFImage());
				System.out.println("image added...");
			}
			continueLoading();
		}
		else
		{
			if(nextByte != TRAILER_MARKER)
			{
				System.err.printf("I dunno what to do! File pointer is: %d\n", raf.getFilePointer());
			}
		}
	}
	
	private String readComment() throws IOException
	{
		int bytesToRead = raf.read();
		StringBuilder sb = new StringBuilder(bytesToRead + 1);
		for(int i = 0; i < bytesToRead; i++)
		{
			sb.append((char)raf.read());
		}
		raf.read();
		return sb.toString();
	}
	
	private void determineFileSignature() throws IOException
	{
		byte[] sb = new byte[6];
		raf.read(sb);
		signature = new String(sb);
	}
	
	private void buildGlobalColorTable() throws IOException
	{
		globalColorTable = new ArrayList<Color[]>(lsd.globalColorTableSize);
		Color[] ca;
		for(int i = 0; i < lsd.globalColorTableSize; i++)
		{
			ca = new Color[]{ new Color(raf.read(), raf.read(), raf.read()) };
			globalColorTable.add(ca);
		}
	}
	
	public String getSignature() { return signature; }
	public int getGlobalWidth() { return lsd.width; }
	public int getGlobalHeight() { return lsd.height; }
	public boolean hasGlobalColorTable() { return lsd.hasGlobalColorTable; }
	public int getGlobalColorResolution() { return lsd.colorResolution; }
	public boolean isGlobalColorTableSorted() { return lsd.isGlobalColorTableSorted; }
	public int getGlobalColorTableSize() { return lsd.globalColorTableSize; }
	public int getBackgroundColorIndex() { return lsd.backgroundColorIndex; }
	public int getPixelAspectRatio() { return lsd.pixelAspectRatio; }
	public BufferedImage getImageAt(int i) throws IndexOutOfBoundsException { return gifimgs.get(i).img; }
	public int getImageCount() { return gifimgs.size(); }
	
	public String LSDToString()
	{
		return String.format(	"Dimensions: %dx%d\n" +
								"Has color table: %s\n" +
								"Color resolution: %d\n" +
								"Color table is sorted: %s\n" +
								"Size of color table: %d\n" +
								"Background color index: %d\n" +
								"Pixel aspect ratio: %d",
								lsd.width,
								lsd.height,
								lsd.hasGlobalColorTable,
								lsd.colorResolution,
								lsd.isGlobalColorTableSorted,
								lsd.globalColorTableSize,
								lsd.backgroundColorIndex,
								lsd.pixelAspectRatio);
	}
	
	/**
	 * .##........######..########.
	 * .##.......##....##.##.....##
	 * .##.......##.......##.....##
	 * .##........######..##.....##
	 * .##.............##.##.....##
	 * .##.......##....##.##.....##
	 * .########..######..########.
	 * @author josh
	 *
	 */
	private class LogicalScreenDescriptor
	{
		private int width;
		private int height;
		private boolean hasGlobalColorTable;
		private int colorResolution;
		private boolean isGlobalColorTableSorted;
		private int globalColorTableSize;
		private int backgroundColorIndex;
		private int pixelAspectRatio;
		
		private LogicalScreenDescriptor() throws IOException
		{
			int[] lsdBuffer = new int[7];
			for(int i = 0; i < lsdBuffer.length; i++)
			{
				lsdBuffer[i] = raf.read();
			}
			width = (lsdBuffer[1] << 8) ^ lsdBuffer[0];
			height = (lsdBuffer[3] << 8) ^ lsdBuffer[2];
			hasGlobalColorTable = (128 & lsdBuffer[4]) == 128;
			isGlobalColorTableSorted = ((lsdBuffer[4] & 8) != 0);
			colorResolution = (lsdBuffer[4] & 112) >> 4;
			globalColorTableSize = (int)Math.pow(2, (lsdBuffer[4] & 7) + 1);
			String s = String.format("%x", lsdBuffer[5]);
			backgroundColorIndex = Integer.parseInt(s, 16);
			s = String.format("%x", lsdBuffer[6]);
			pixelAspectRatio = Integer.parseInt(s, 16);
		}
	}
	
	public String GCEToString()
	{
		return String.format(	"GCE block size: %d\n" +
								"GCE disposal method: %d\n" +
								"GCE user input: %s\n" +
								"GCE transparent color: %s\n" +
								"GCE delay time: %d\n" +
								"GCE transparent color index: %d\n",
								gce.blockSize,
								gce.disposalMethod,
								gce.handlesUserInput,
								gce.hasTransparentColor,
								gce.delayTime,
								gce.transparentColorIndex);
	}
	
	/**
	 * ..######....######..########
	 * .##....##..##....##.##......
	 * .##........##.......##......
	 * .##...####.##.......######..
	 * .##....##..##.......##......
	 * .##....##..##....##.##......
	 * ..######....######..########
	 * @author josh
	 *
	 */
	private class GraphicsControlExtension
	{
		private int blockSize;
		private int disposalMethod;
		private boolean handlesUserInput;
		private boolean hasTransparentColor;
		private int delayTime;
		private int transparentColorIndex;
		
		private GraphicsControlExtension() throws IOException
		{
			//we have read 2 bytes before this point (21 and F9), 8 - 2 = 6
			//last byte will always be the block terminator, so let's ignore it to make array length 5
			int[] gceBuffer = new int[5];
			for(int i = 0; i < gceBuffer.length; i++)
			{
				gceBuffer[i] = raf.read();
			}
			String s = String.format("%x", gceBuffer[0]);
			blockSize = Integer.parseInt(s, 16);
			disposalMethod = (gceBuffer[1] & 28) >> 2;
			handlesUserInput = (gceBuffer[1] & 2) == 2;
			hasTransparentColor = (gceBuffer[1] & 1) == 1;
			s = String.format("%x%x", gceBuffer[3], gceBuffer[2]);
			delayTime = Integer.parseInt(s, 16);
			s = String.format("%x", gceBuffer[4]);
			transparentColorIndex = Integer.parseInt(s, 16);
			//skip!
			raf.read();
		}
	}
	
	public String AppExtToString()
	{
		return String.format(		"Application ID block length: %d\n" +
									"Applcation identifier: %s\n" +
									"Authentication code: %s\n" +
									"Subblock length: %d\n" +
									"Total loop number: %d",
									appext.appBlockLength,
									appext.appIdentifier,
									appext.authCode,
									appext.dataSubBlockLength,
									appext.totalLoops);
	}
	
	/**
	 * ....###....########..########..########.##.....##.########
	 * ...##.##...##.....##.##.....##.##........##...##.....##...
	 * ..##...##..##.....##.##.....##.##.........##.##......##...
	 * .##.....##.########..########..######......###.......##...
	 * .#########.##........##........##.........##.##......##...
	 * .##.....##.##........##........##........##...##.....##...
	 * .##.....##.##........##........########.##.....##....##...
	 * @author josh
	 *
	 */
	private class ApplicationExtension
	{
		private int appBlockLength;
		private String appIdentifier;
		private String authCode;
		private int dataSubBlockLength;
		private int totalLoops;
		
		private ApplicationExtension() throws IOException
		{
			//we have read 2 bytes at this point: 21 and FF
			appBlockLength = raf.read();
			byte buffer[] = new byte[appBlockLength];
			raf.read(buffer);
			String s = new String(buffer);
			appIdentifier = s.substring(0, 8); //first 8 bytes
			//assume(?) that the next 3 bytes represent the authentication code
			authCode = s.substring(8);
			dataSubBlockLength = raf.read();
			buffer = new byte[dataSubBlockLength];
			raf.read(buffer);
			//"The first value is always the byte 01" (http://giflib.sourceforge.net/whatsinagif/animation_and_transparency.html)
			totalLoops = (buffer[1] << 8) + buffer[2];
			//the next byte /should/ be 0
			int next = raf.read();
			if(next != 0)
			{
				System.err.println("ApplicationExtension: expected null byte but got something else! File pointer is: " + raf.getFilePointer());
				//let's assume 'next' is the remaining bytes required to read:
				buffer = new byte[next];
				raf.read(buffer);
				raf.read(); //again to read past block terminator
			}
		}
	}
	
	/**
	 * ..######...####.########.####.##.....##....###.....######...########
	 * .##....##...##..##........##..###...###...##.##...##....##..##......
	 * .##.........##..##........##..####.####..##...##..##........##......
	 * .##...####..##..######....##..##.###.##.##.....##.##...####.######..
	 * .##....##...##..##........##..##.....##.#########.##....##..##......
	 * .##....##...##..##........##..##.....##.##.....##.##....##..##......
	 * ..######...####.##.......####.##.....##.##.....##..######...########
	 * @author josh
	 *
	 */
	private class GIFImage
	{
		private ImageDescriptor imgdesc;
		private int lzwMinCodeSize;
		private final int MAX_CODE_SIZE = 12;
		private BufferedImage img;
		private int transparentColorIndex;
		private boolean hasTransparentIndex;
		private int local;
		private int eoi;
		private int clearCode;
		private int codeWidth;
		
		private GIFImage() throws IOException
		{
			imgdesc = new ImageDescriptor();
			img = new BufferedImage(imgdesc.width, imgdesc.height, BufferedImage.TYPE_INT_RGB);
			buildImage();
		}
		
		private GIFImage(int transparentColorIndex) throws IOException
		{
			hasTransparentIndex = true;
			this.transparentColorIndex = transparentColorIndex;
			imgdesc = new ImageDescriptor();
			img = new BufferedImage(imgdesc.width, imgdesc.height, BufferedImage.TYPE_INT_ARGB);
			buildImage();
		}
		
		private void resetCodeList(ArrayList<Color[]> clist)
		{
			codeWidth = lzwMinCodeSize + 1;
			if(imgdesc.hasLocalColorTable)
			{
				//TODO: parse local color table
				System.err.println("local color table not implemented!");
				return;
			}
			else
			{
				clist.clear();
				clist.addAll(globalColorTable);
			}
			//continue adding elements to the list until it can fit EOI index
			//additional codes that are added to clist during decoding will now have their appropriate indices
			while(clist.size() <= eoi)
			{
				clist.add(null);
			}
		}
		
		private Color[] combine(Color[] left, Color right)
		{
			Color[] rval = new Color[left.length + 1];
			int i = 0;
			for(; i < left.length; i++)
			{
				rval[i] = left[i];
			}
			rval[i] = right;
			return rval;
		}
		
		private void colorImage(ArrayList<Color> colorArray)
		{
			int k = 0;
			for(int i = 0; i < img.getHeight(); i++)
			{
				for(int j = 0; j < img.getWidth(); j++)
				{
					img.setRGB(j, i, colorArray.get(k).getRGB());
					k = (k + 1) % colorArray.size();
				}
			}
		}
		
		private void buildImage() throws IOException
		{
			ArrayList<Color> colorArray = new ArrayList<Color>(imgdesc.width * imgdesc.height);
			ArrayList<Color[]> clist = new ArrayList<Color[]>();
			String s = String.format("%x", raf.read());
			lzwMinCodeSize = Integer.parseInt(s, 16);
			codeWidth = lzwMinCodeSize + 1;
			clearCode = 1 << lzwMinCodeSize;
			eoi = clearCode + 1;
			int currentMaxCodeSize = (int)(Math.pow(2, codeWidth) - 1);
			System.out.printf("lzw mincode: %d\nclear: %d\nEOI: %d\n", lzwMinCodeSize, clearCode, eoi);
			System.out.println(imgdesc.toString());
			if(hasTransparentIndex)
				System.out.printf("transparent color index: %d\n", transparentColorIndex);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BitSet bs;
			int next = raf.read();
			while(next != 0)
			{
				s = String.format("%x", next);
				int bytesToRead = Integer.parseInt(s, 16);
				byte imgDataBuffer[] = new byte[bytesToRead];
				raf.read(imgDataBuffer);
				
				for(byte b : imgDataBuffer)
					baos.write(b);

				next = raf.read();
			}
			bs = BitSet.valueOf(baos.toByteArray());
			
			boolean firstPixel = false;
			for(int i = codeWidth;; i += codeWidth)
			{	
				int code = getBitSlice(bs, i, codeWidth);
				if(code == eoi)
				{
					break;
				}
				else if(code == clearCode)
				{
					resetCodeList(clist);
					firstPixel = true;
					codeWidth = lzwMinCodeSize + 1;
					currentMaxCodeSize = (int)(Math.pow(2, codeWidth) - 1);
				}
				else
				{
					if(code < clist.size() && clist.get(code) != null)
					{	//code found in table
						Color found[] = clist.get(code);
						//first 'output' found code
						colorArray.addAll(Arrays.asList(found));
						if(!firstPixel)
						{	//then add previous+found color to existing code list
							clist.add(combine(clist.get(local),found[0]));
						}
						else
						{	//else:  first pixel
							firstPixel = false;
						}
					}
					else
					{	//code not found in table
						//get the colors at previous index, and add the first color of the same index to code list
						clist.add(combine(clist.get(local), clist.get(local)[0]));
						//then 'output'
						colorArray.addAll(Arrays.asList(clist.get(code)));
					}
					local = code;
					if(clist.size() > currentMaxCodeSize)
					{
						if(codeWidth < MAX_CODE_SIZE)
						{
							++codeWidth;
							currentMaxCodeSize = (int)(Math.pow(2, codeWidth) - 1);
						}//else: don't touch code size, the next code /should/ be the clear code
					}
				}
			}
			
			System.out.println("finished main loop gracefully");
			colorImage(colorArray);
		}
		
		private int getBitSlice(BitSet set, int start, int bitsToRead)
		{
			int rval = 0;
			BitSet tmp;
			try
			{
				if(start - bitsToRead < 0)
					tmp = set.get(start - (bitsToRead + (start - bitsToRead)), start);
				else
					tmp = set.get(start - bitsToRead, start);
			}
			catch(IndexOutOfBoundsException e)
			{
				e.printStackTrace();
				return 0;
			}
			for(int i = 0; i < tmp.length(); ++i)
				if(tmp.get(i))
					rval += 1 << i;
			return rval;
		}
	}

	/**
	 * .####.##.....##..######...########..########..######...######.
	 * ..##..###...###.##....##..##.....##.##.......##....##.##....##
	 * ..##..####.####.##........##.....##.##.......##.......##......
	 * ..##..##.###.##.##...####.##.....##.######....######..##......
	 * ..##..##.....##.##....##..##.....##.##.............##.##......
	 * ..##..##.....##.##....##..##.....##.##.......##....##.##....##
	 * .####.##.....##..######...########..########..######...######.
	 * @author josh
	 *
	 */
	private class ImageDescriptor
	{
		private int left;
		private int top;
		private int width;
		private int height;
		private boolean hasLocalColorTable;
		private boolean isInterlaced;
		private boolean isColorTableSorted;
		private int localColorTableSize;
		
		private ImageDescriptor() throws IOException
		{
			//we have read 1 byte so far (2C), 10 - 1 = 9
			int[] imgdescBuffer = new int[9];
			for(int i = 0; i < imgdescBuffer.length; i++)
			{
				imgdescBuffer[i] = raf.read();
			}
			left = (imgdescBuffer[1] << 8) ^ imgdescBuffer[0];
			top = (imgdescBuffer[3] << 8) ^ imgdescBuffer[2];
			width = (imgdescBuffer[5] << 8) ^ imgdescBuffer[4];
			height = (imgdescBuffer[7] << 8) ^ imgdescBuffer[6];
			hasLocalColorTable = (imgdescBuffer[8] & 128) == 128;
			isInterlaced = ((imgdescBuffer[8] << 1) & 128) == 128;
			isColorTableSorted = ((imgdescBuffer[8] << 2) & 128) == 128;
			localColorTableSize = 0;
			if (hasLocalColorTable)
				localColorTableSize = ((int)Math.pow(2, ((imgdescBuffer[8] << 5) & 224) + 1)) * 3;
		}
		
		@Override
		public String toString()
		{
			return String.format(	"imgdesc left: %d\n" +
									"imgdesc top: %d\n" +
									"imgdesc width: %d\n" +
									"imgdesc height: %d\n" +
									"imgdesc has colortable: %s\n" +
									"imgdesc is interlaced: %s\n" +
									"imgdesc is colortable sorted: %s\n" +
									"imgdesc colortable size: %d\n",
									left,
									top,
									width,
									height,
									hasLocalColorTable,
									isInterlaced,
									isColorTableSorted,
									localColorTableSize);
		}
	}
}