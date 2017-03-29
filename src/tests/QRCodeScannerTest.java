package tests;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import de.yadrone.base.video.ImageListener;
import imgManagement.QRCodeScanner;
import imgManagement.TagListener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

/**
 * Controller Mock class
 * @author Nichlas N. Pilemand
 *
 */
class Controller implements TagListener {
	public Controller(){
		
	}
	public void onTag(Result result, float orientation){
		if (result == null) {
			System.out.println("No tag result!");
			return; 
		}
		System.out.print("orientation: "+orientation + ". ");
		System.out.println("Tag found: "+ result.getText());			
	}
}

public class QRCodeScannerTest {
	
	public static void main(String[] args) {
		QRCodeScanner scanner = new QRCodeScanner();
		Controller controller = new Controller();
		scanner.addListener(controller);
		
		// Send some images
		BufferedImage img = null;
		String[] images = {
				"assets/qr.jpg",
				"assets/qr2.jpg",
				"assets/qr3.jpg",
				"assets/qr4.jpg",
				"assets/qr5.jpg",
				"assets/demo1.jpg",
				"assets/demo2.jpg",
				"assets/demo3.jpg",
				"assets/demo4.jpg",
				"assets/ny_demo1.jpg",
				"assets/ny_demo2.jpg"};
		long time = System.currentTimeMillis();
		for (String file : images) {
			try {
				System.out.print(file + ": ");
			    img = ImageIO.read(new File(file));
			    scanner.imageUpdated(img);
			    System.out.println();
			} catch (IOException e) {
			}
		}
		System.out.println("Time taken: " + (System.currentTimeMillis() - time) + " ms.");
		
	}
}
