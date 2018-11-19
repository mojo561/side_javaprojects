package test;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 * @author Josh Monague
 */
public class MiscTests
{
	@Test
	public final void test()
	{
		int[] values = new int[]{ 0x8C, 0x2D, 0x99, 0x87, 0x2A, 0x1C, 0xDC, 0x33, 0xA0, 0x02, 0x75, 0xEC, 0x95, 0xFA, 0xA8, 0xDE, 0x60, 0x8C, 0x04,
				0x91, 0x4C, 0x01};
		int fauxCounter = 4; //to skip first pixel...
		
		int prevValue = 0;
		int prevRemainder = 0;
		int bitWidth = 3;
		
		for(int rawByte : values)
		{
			int i = 0;
			
			if(prevRemainder > 0)
			{
				i = prevRemainder;
				int add = getLEBits(rawByte, prevRemainder);
				add = (add << (bitWidth - prevRemainder)) ^ prevValue;
				System.out.printf("%d\n", add); //if(add != 4) fauxCounter++; if (fauxCounter >= ((1 << bitWidth) - 1)) bitWidth++;
				prevValue = rawByte >> prevRemainder;
			}
			else
			{
				prevValue = rawByte;
			}
			while(8 - i >= bitWidth)
			{
				int add = getLEBits(prevValue, bitWidth);
				System.out.printf("%d\n", add);
				prevValue = prevValue >> bitWidth;
				i += bitWidth; //if(add != 4) fauxCounter++; if (fauxCounter >= ((1 << bitWidth) - 1)) bitWidth++;
			}
			
			if(i != 8)
				prevRemainder = bitWidth - (8 - i);
			else
				prevRemainder = 0;
		}
	}
	
	//returns the last n bits of rawByte... trust me
	private int getLEBits(int rawByte, int n)
	{
		if(n <= 0)
			return 0;
		return rawByte & (~(~(1 << n) << n ^ (1 << (n * 2))));
	}
}