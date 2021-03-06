package OCR;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import config.Config;
import config.Species;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class OCR{

	private Tesseract tesseract;
	private String language = "eng";
	
	//Used for the purposes of cleaning up the screen capture image for OCR.
	private int colorSimilarityTreshold=15;
	
	private ArrayList<Rectangle> OCRViewports;
	Species speciesList;
	
	//Frame locations for OCR
	private double frame1PercentageLeft;
	private double frame1PercentageTop;
	private double frame2PercentageLeft;
	private double frame2PercentageTop;
	private double frameWidthPercentage;
	private double frameHeightPercentage;
	
	
	public OCR(Config config, Species speciesList){
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		this.frame1PercentageLeft = config.frame1PercentageLeft;
		this.frame1PercentageTop = config.frame1PercentageTop;
		
		this.frame2PercentageLeft = config.frame2PercentageLeft;
		this.frame2PercentageTop = config.frame2PercentageTop;
		
		this.frameWidthPercentage = config.frameWidthPercentage;
		this.frameHeightPercentage = config.frameHeightPercentage;
		
		Dimension frameSize = new Dimension((int) Math.ceil(screenSize.getWidth() * frameWidthPercentage), (int) Math.ceil(screenSize.getHeight() * frameHeightPercentage));
		Point frame1Location = new Point((int) Math.ceil(screenSize.getWidth() * frame1PercentageLeft), (int) Math.ceil(screenSize.getHeight() * frame1PercentageTop));
		Point frame2Location = new Point((int) Math.ceil(screenSize.getWidth() * frame2PercentageLeft), (int) Math.ceil(screenSize.getHeight() * frame2PercentageTop));
		
		this.OCRViewports = new ArrayList<Rectangle>();
		
		OCRViewports.add(new Rectangle(frame1Location, frameSize));
		OCRViewports.add(new Rectangle(frame2Location, frameSize));
		
		this.speciesList = speciesList;
		tesseract = new Tesseract();
		tesseract.setDatapath("./");
		tesseract.setLanguage(language);
	}
	
	//Does the actual OCR operation and puts the generated text in the label
	public ArrayList<String> doOCR() {
		ArrayList<String> results = new ArrayList<String>();
		try {
			Robot robot = new Robot();
			ArrayList<BufferedImage> textCaps = new ArrayList<BufferedImage>();
			for(Rectangle viewport: OCRViewports) {
				textCaps.add(robot.createScreenCapture(viewport));
			}
			for(BufferedImage textCap : textCaps) {
				String text;
				try {
					//File outputFile = new File("screenCap" + System.currentTimeMillis() + ".jpg");
					//ImageIO.write(textCap, "jpg", outputFile);
					
					processImage(textCap);
					
					//outputFile = new File("screenCap_postProc" + System.currentTimeMillis() + ".jpg");
					//ImageIO.write(textCap, "jpg", outputFile);
					
					text = tesseract.doOCR(textCap);
					if(text.length()<=3) {
						//Don't do string comparison if the output is empty
						continue;
					}
					//System.out.println("OCR reading: " + text);
					text = StringSimilarityComparer.compare(text, speciesList.species);
					results.add(text);
				} catch (TesseractException e) {
					e.printStackTrace();
				} 
			}
		} catch (AWTException e) {
			e.printStackTrace();
		}
		return results;
	}
	
	private void processImage(BufferedImage inputImage){
		int imageHeight = inputImage.getHeight();
		int imageWidth = inputImage.getWidth();
		
		for(int i=0;i<imageWidth;i++) {
			for(int j=0;j<imageHeight;j++) {
				int pixel = inputImage.getRGB(i, j);
				int red = (0xFF & (pixel>>16));
				int green = (0xFF & (pixel>>8));
				int blue = (0xFF & pixel);
				
				if(Math.abs(red-green)<=colorSimilarityTreshold 
						&& Math.abs(green-blue)<=colorSimilarityTreshold
						&& Math.abs(red-blue)<=colorSimilarityTreshold) {
					//Invert color
					inputImage.setRGB(i,j, (255-red)<<16 | (255-green)<<8 | (255-blue));
				}
				else {
					//Set color to white
					inputImage.setRGB(i, j, 0xffffffff);
				}
			}
		}
	}
	
}
