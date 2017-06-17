package imgManagement;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import de.yadrone.base.video.ImageListener;

/**
 * Finds circles in an image.
 * @author Nichlas N. Pilemand 
 */
public class CircleFinder implements ImageListener {
	
	private static final double DP = 1.05; // Basicly tolerence
	private static final int MIN_DIST = 50; // Minimum distance between center points
	private static final int BLUR = 9; // Blur amount, removes noise - must be uneven
	
	private long imageCount = 0;
	private final int frameSkip = 5; // Only check every n frames. Must be > 0. 1 == no skip.
	
	private ArrayList<CircleListener> listeners = new ArrayList<CircleListener>();
	
	public CircleFinder() {
		// Probably not needed
	}

	
	/**
	 * Detects circles in an image
	 * @param image The image to detect circles on and display.
	 * @return {@link Circle}[] An array containing the data from the found circles.
	 */
	public static Circle[] findCircles(Mat image) {
		Size imgSize = new Size(0,0);
		if (image.size().height > 1200)
			Imgproc.resize(image, image, imgSize, 0.5,0.5,1);
		//System.out.println(image.size());
		Mat gray = image.clone();
		// Get the gray img
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		
		// Detect circles
		Mat circles = new Mat();
		Size s = new Size(BLUR,BLUR);
		Imgproc.GaussianBlur(gray, gray, s, 2);
		Imgproc.HoughCircles(gray, circles, Imgproc.CV_HOUGH_GRADIENT, DP, MIN_DIST);
		
		Circle[] ret = new Circle[circles.cols()];
		double[] data;
		
		for (int i = 0; i < circles.cols(); i++) {
			data = circles.get(0, i);
			ret[i] = new Circle(data[0], data[1], data[2]);
		}
		
		return ret;
	}
	
	/**
	 * Same as {@link CircleFinder#findCircles}, except it takes a BufferedImage as argument.
	 * @param bi {@link BufferedImage}
	 * @return {@link Circle}[] Same as {@link CircleFinder#findCircles}
	 */
	public static Circle[] findCircles(BufferedImage bi){
		 Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		  byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		  mat.put(0, 0, data);
		  return findCircles(mat);
	}
	
	/**
	 *	Loads an image based on imgLoc
	 *	@param imgLoc Location of the image
	 *	@return The image loaded of type Mat
	*/
	public static Mat loadImg(String imgLoc){
		Mat image;
		image = Imgcodecs.imread(imgLoc, Imgcodecs.IMREAD_COLOR);
		if (image.empty()){
			System.out.println("Couldn't load image!");
			return null;
		} else
		return image;
	}


	@Override
	public void imageUpdated(BufferedImage img) {
		// We don't need to find circles in every frame
		//System.out.println("Image"+System.currentTimeMillis());
		if ((imageCount++ % frameSkip) != 0)
			return;
		Circle[] circles = findCircles(img);
		for (CircleListener listener : listeners)
			listener.circlesUpdated(circles);
	}
	
	public void addListener(CircleListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeListener(CircleListener listener) {
		this.listeners.remove(listener);
	}

}
