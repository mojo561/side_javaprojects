package def;

import java.awt.Color;
import java.awt.Graphics;
import java.io.FileNotFoundException;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 
 * @author Josh Monague
 */
@SuppressWarnings("serial")
public class Main3 extends JPanel
{
	private GifFile gif;
	public int width;
	public int height;
	public Main3()
	{
		String[] files = new String[]{	                                                                            "GifSample2.gif",			//0
										                                                                            "GifSample5.gif",			//1
										                                                                            "GifSample.gif",			//2
										                                                                            "sample_1.gif",				//3
										                                                                            "firststeps48.gif",			//4
										                                                                            "GifSample3.gif",			//5
										                                                                            "sample_1_enlarged.gif",	//6
										"GifSample4.gif",			//	7 Index: 4352, Size: 4097
										"giphy_s.gif",				//	8 Index: 338, Size: 263
										"GifSample6.gif",			//	9 Index: 4160, Size: 4097
										                                                                            "GifSample7.gif",			//10
										                                                                            "GifSample8.gif",			//11
										"GifSample9.gif",			//	12 Index: 385, Size: 259
										"GifSample6_resized.gif",	//	13 Index: 192, Size: 130
										"GifSample9_anim.gif"		//	14
									};
		try
		{
			gif = new GifFile(files[14]);//[7]);
			width = gif.getGlobalWidth();
			height = gif.getGlobalHeight();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	public String getFileName()
	{
		return gif.getFileName();
	}
	
	@Override
	public void paint(Graphics g)
	{
		g.drawImage(gif.getImageAt(0), 0, 0, new Color(255,0,255), null);
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		Main3 m = new Main3();
		JFrame frame = new JFrame(m.getFileName());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.add(m);
	    frame.setSize(m.width+50, m.height+50);
	    frame.setLocationRelativeTo(null);
	    frame.setVisible(true);
	}
}