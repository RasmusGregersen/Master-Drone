package controller;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import de.yadrone.base.IARDrone;
import de.yadrone.base.command.LEDAnimation;
import de.yadrone.base.video.ImageListener;
import imgManagement.Circle;
import imgManagement.CircleFinder;
import imgManagement.TagListener;
import utils.WallCoordinatesReader;

/**
 * Main drone controller.
 * Translates read tags into commands for the drone.
 * @author Nichlas N. Pilemand
 *
 */
public class MainDroneController extends AbstractController implements TagListener, ImageListener {

	private final static int SPEED = 5;
	private final static int SLEEP = 500;
	
	private long imageCount = 0;
	private final int frameSkip = 2; // Skip every n frames. Must be > 0. 1 == no skip.

	/*
	 * This list holds tag-IDs for all tags which have successfully been visited
	 */
	private ArrayList<String> tagVisitedList = new ArrayList<String>();

	private Result tag;
	private float tagOrientation;
	private ArrayList<String> ports = new ArrayList<String>();
	private HashMap<String, Point> wallMarks;
	private Circle[] circles;
	private int nextPort = 0;

	public MainDroneController(IARDrone drone) {
		super(drone);
		// Init ports list
		for (int i = 0; i <= 7; i++)
			ports.add("P.0" + i);
		wallMarks = WallCoordinatesReader.read();
	}

	@Override
	public void run() {
		while (!doStop) // control loop
		{
			try {
				// reset if too old (and not updated)
				if ((tag != null) && (System.currentTimeMillis() - tag.getTimestamp() > 500))
					tag = null;
				
				if (tag != null && ports.get(nextPort).equals(tag.getText())) { // We haven't gone through this port
					// Check for circles
					if (circles.length > 0)
						if (isCircleCentered())
							goThroughPort();
						else
							centerCircle();
					else { // Try to reduce the angle to the tag
						// TODO
					}
				}

				else if ((tag == null) || hasTagBeenVisited()) {
					strayAround();
				} else if (tag != null && wallMarks.containsKey(tag.getText())) {
					// We found a wall mark tag, continue looking around
					strayAround();
				
				} else if (!isTagCentered()) { // tag visible, but not centered
					centerTag();
				} else {
					System.out.println("AutoController: I do not know what to do ...");
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

	}

	public void onTag(Result result, float orientation) {
		if (result == null) // ToDo: do not call if no tag is present
			return;

		System.out.println("AutoController: Tag found" + result.getText() + ", " + orientation);

		tag = result;
		tagOrientation = orientation;
	}

	private boolean isTagCentered() {
		if (tag == null)
			return false;

		// a tag is centered if it is
		// 1. if "Point 1" (on the tag the upper left point) is near the center
		// of the camera
		// 2. orientation is between 350 and 10 degrees

		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

		ResultPoint[] points = tag.getResultPoints();
		boolean isCentered = ((points[1].getX() > (imgCenterX - MasterDrone.TOLERANCE))
				&& (points[1].getX() < (imgCenterX + MasterDrone.TOLERANCE))
				&& (points[1].getY() > (imgCenterY - MasterDrone.TOLERANCE))
				&& (points[1].getY() < (imgCenterY + MasterDrone.TOLERANCE)));

		boolean isOriented = ((tagOrientation < 10) || (tagOrientation > 350));

		System.out.println("AutoController: Tag centered ? " + isCentered + " Tag oriented ? " + isOriented);

		return isCentered && isOriented;
	}

	private boolean hasTagBeenVisited() {
		synchronized (tag) {
			for (int i = 0; i < tagVisitedList.size(); i++) {
				if (tag.getText().equals(tagVisitedList.get(i)))
					return true;
			}
		}

		return false;
	}

	private void strayAround() throws InterruptedException {
		int direction = new Random().nextInt() % 4;
		switch (direction) {
		case 0:
			System.out.println("AutoController: Stray Around: FORWARD");
			drone.getCommandManager().forward(SPEED);
			break;
		case 1:
			System.out.println("AutoController: Stray Around: BACKWARD");
			drone.getCommandManager().backward(SPEED);
			break;
		case 2:
			System.out.println("AutoController: Stray Around: LEFT");
			drone.getCommandManager().goLeft(SPEED);
			break;
		case 3:
			System.out.println("AutoController: Stray Around: RIGHT");
			drone.getCommandManager().goRight(SPEED);
			break;
		}

		Thread.currentThread();
		Thread.sleep(SLEEP);
	}

	private void centerTag() throws InterruptedException {
		String tagText;
		ResultPoint[] points;

		synchronized (tag) {
			points = tag.getResultPoints();
			tagText = tag.getText();
		}

		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

		float x = points[1].getX();
		float y = points[1].getY();

		if ((tagOrientation > 10) && (tagOrientation < 180)) {
			System.out.println("AutoController: Spin left");
			drone.getCommandManager().spinLeft(SPEED * 2);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if ((tagOrientation < 350) && (tagOrientation > 180)) {
			System.out.println("AutoController: Spin right");
			drone.getCommandManager().spinRight(SPEED * 2);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (x < (imgCenterX - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Go left");
			drone.getCommandManager().goLeft(SPEED);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (x > (imgCenterX + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Go right");
			drone.getCommandManager().goRight(SPEED);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (y < (imgCenterY - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Go forward");
			drone.getCommandManager().forward(SPEED);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (y > (imgCenterY + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Go backward");
			drone.getCommandManager().backward(SPEED);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else {
			System.out.println("AutoController: Tag centered");
			drone.getCommandManager().setLedsAnimation(LEDAnimation.BLINK_GREEN, 10, 5);

			tagVisitedList.add(tagText);
		}
	}
	
	/**
	 * Centers a circle's midpoint
	 */
	void centerCircle() throws InterruptedException {
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
		if (circles.length > 1) { 
			// We have more than one circle, figure out which one is correct
			for (Circle c : circles){
				// Assume the radius has to be at least 10% of the image height to be valid
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10){
					// Now do the centering
					if (c.x < (imgCenterX - MasterDrone.TOLERANCE)){
						System.out.println("AutoController.centerCircle: Go left");
						drone.getCommandManager().goLeft(SPEED);
						Thread.currentThread();
						Thread.sleep(SLEEP);
					} else if (c.x > (imgCenterX + MasterDrone.TOLERANCE)) {
						System.out.println("AutoController.centerCircle: Go right");
						drone.getCommandManager().goRight(SPEED);
						Thread.currentThread();
						Thread.sleep(SLEEP);
					} else if (c.y < (imgCenterY - MasterDrone.TOLERANCE)){
						System.out.println("AutoController.centerCircle: Go up");
						drone.getCommandManager().up(SPEED);
						Thread.currentThread();
						Thread.sleep(SLEEP);
					} else if (c.y > (imgCenterY + MasterDrone.TOLERANCE)){
						System.out.println("AutoController.centerCircle: Go down");
						drone.getCommandManager().down(SPEED);
						Thread.currentThread();
						Thread.sleep(SLEEP);
					} else {
						System.out.println("AutoController.centerCircle: Circle centered.");
					}
				}
			}
		}
	}
	
	Boolean isCircleCentered() {
		Boolean ret = false;
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
		
		if (circles.length > 1)  // Same deal as centerCircle()
			for (Circle c : circles)
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10)
					ret = ((c.x > (imgCenterX - MasterDrone.TOLERANCE))
							&& (c.x < (imgCenterX + MasterDrone.TOLERANCE))
							&& (c.y > (imgCenterY - MasterDrone.TOLERANCE))
							&& (c.y < (imgCenterY + MasterDrone.TOLERANCE)));		
		return ret;
	}
	
	/**
	 * Fly's the drone through the currently centered port.
	 * This just flies the drone forward until the circle is no longer visible,
	 * i.e. until we're through
	 * @throws InterruptedException 
	 */
	void goThroughPort() throws InterruptedException{
		System.out.println("AutoController: Going through port " + nextPort);
		while(true) {
			if (!isCircleCentered())
				break;
			drone.getCommandManager().forward(SPEED);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		}
		// TODO Here we assume we're so close to the circle that we no longer see it
		// so fly forward
		drone.getCommandManager().forward(SPEED * 2);
		Thread.currentThread();
		Thread.sleep(SLEEP);
	}

	/**
	 * Takes images from the drone and feeds them to {@link CircleFinder}.
	 */
	public void imageUpdated(BufferedImage image) {
		// This check is meant to skip every n'th frame from a video stream
		if ((imageCount++ % frameSkip) != 0)
			return;
		circles = CircleFinder.findCircles(image);
//		for(int i = 0; i < circles.length; i++){
//			System.out.printf("Circle %d: (%f,%f) r = %f. %n", i, circles[i].x, circles[i].y, circles[i].getRadius());
//		}
		
	}
	/**
	 * Same as above, used for testing
	 * @param image
	 */
	public void imageUpdated(Mat image) {		
		if ((imageCount++ % frameSkip) != 0)
			return;
		circles = CircleFinder.findCircles(image);
		for(int i = 0; i < circles.length; i++){
			System.out.printf("Circle %d: (%d,%d) r = %f.\n", i, circles[i].x, circles[i].y, circles[i].getRadius());
		}
	}
}
