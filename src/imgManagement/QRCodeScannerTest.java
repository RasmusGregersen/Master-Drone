package imgManagement;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import de.yadrone.base.video.ImageListener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

class Controller implements TagListener {
	public Controller(){
		
	}
	public void onTag(Result result, float orientation){
		if (result == null) {
			System.out.println("No tag result!");
			return; 
		}
		System.out.println("Tag found: "+ result.getText());			
	}
}

public class QRCodeScannerTest implements ImageListener
{
	private ArrayList<TagListener> listener = new ArrayList<TagListener>();
	private Result scanResult;
	private long imageCount = 0;
	
	public static void main(String[] args) {
		QRCodeScannerTest scanner = new QRCodeScannerTest();
		Controller controller = new Controller();
		scanner.addListener(controller);
		
		// Send some images
		BufferedImage img = null;
		String[] images = {"assets/qr.jpg","assets/qr2.jpg","assets/qr3.jpg","assets/qr4.jpg","assets/qr5.jpg"};
		for (String file : images) {
			try {
				System.out.print(file + ": ");
			    img = ImageIO.read(new File(file));
			    scanner.imageUpdated(img);
			    System.out.println();
			} catch (IOException e) {
			}
		}
		
	}

	public void imageUpdated(BufferedImage image)
	{
		// This check is ment to skip every other frame from a video stream
//		if ((++imageCount % 2) == 0)
//			return;

		// try to detect QR code
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

		// decode the barcode (if only QR codes are used, the QRCodeReader might be a better choice)
		MultiFormatReader reader = new MultiFormatReader();

		double theta = Double.NaN;
		try
		{
			scanResult = reader.decode(bitmap);

			ResultPoint[] points = scanResult.getResultPoints();
			ResultPoint a = points[1]; // top-left
			ResultPoint b = points[2]; // top-right

			// Find the degree of the rotation (needed e.g. for auto control)

			double z = Math.abs(a.getX() - b.getX());
			double x = Math.abs(a.getY() - b.getY());
			theta = Math.atan(x / z); // degree in rad (+- PI/2)

			theta = theta * (180 / Math.PI); // convert to degree

			if ((b.getX() < a.getX()) && (b.getY() > a.getY()))
			{ // code turned more than 90� clockwise
				theta = 180 - theta;
			}
			else if ((b.getX() < a.getX()) && (b.getY() < a.getY()))
			{ // code turned more than 180� clockwise
				theta = 180 + theta;
			}
			else if ((b.getX() > a.getX()) && (b.getY() < a.getY()))
			{ // code turned more than 270 clockwise
				theta = 360 - theta;
			}
		}
		catch (ReaderException e)
		{
			// no code found.
			scanResult = null;
		}

		// inform all listener
		for (int i=0; i < listener.size(); i++)
		{
			listener.get(i).onTag(scanResult, (float)theta);
		}
	}

	public void addListener(TagListener listener)
	{
		this.listener.add(listener);
	}

	public void removeListener(TagListener listener)
	{
		this.listener.remove(listener);
	}
}
