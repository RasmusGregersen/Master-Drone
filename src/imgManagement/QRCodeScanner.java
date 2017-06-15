package imgManagement;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import de.yadrone.base.video.ImageListener;

/**
 * Scanner class for scanning QR codes.
 * Register {@link TagListener}s with addListener() to receive updates.
 * 
 * @author Nichlas N. Pilemand
 * @see QRCodeScanner#addListener addListener()
 */
public class QRCodeScanner implements ImageListener {
	
	private ArrayList<TagListener> listener = new ArrayList<TagListener>();
	private Result scanResult;
	private long imageCount = 0;
	private int frameSkip = 2; // Skip every n frames. Must be > 0. 1 == no skip.
	

	/**
	 * Called by AR Drone API
	 */
	public void imageUpdated(BufferedImage image) {
		// This check is meant to skip every n'th frame from a video stream
		if ((imageCount++ % frameSkip) != 0)
			return;

		// try to detect QR code
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

		// decode the QR code
		QRCodeReader reader = new QRCodeReader();
		
		Map<DecodeHintType,BarcodeFormat> readerHint = new HashMap();
		readerHint.put(DecodeHintType.valueOf("POSSIBLE_FORMATS"), BarcodeFormat.QR_CODE);

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
			{ // code turned more than 90 deg clockwise
				theta = 180 - theta;
			}
			else if ((b.getX() < a.getX()) && (b.getY() < a.getY()))
			{ // code turned more than 180 deg clockwise
				theta = 180 + theta;
			}
			else if ((b.getX() > a.getX()) && (b.getY() < a.getY()))
			{ // code turned more than 270 deg clockwise
				theta = 360 - theta;
			}
		}
		catch (ReaderException e)
		{
			// no code found.
			scanResult = null;
		}
		
		// inform all listeners
		for (int i=0; i < listener.size(); i++)
			listener.get(i).onTag(scanResult, (float)theta);
				
	}
	
	/**
	 * Registers a {@link TagListener}.
	 * @param listener The TagListener object to register on.
	 * @see TagListener#onTag(Result, float) TagListener.onTag() for more.
	 */
	public void addListener(TagListener listener) {
		this.listener.add(listener);
	}

	/**
	 * Unregisters a TagListener.
	 * @param listener The TagListener object to unregister from.
	 */
	public void removeListener(TagListener listener) {
		this.listener.remove(listener);
	}

}
