package controller;

import java.util.ArrayList;
import java.util.HashMap;
import org.opencv.core.Point;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import controller.StateController.Command;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.LEDAnimation;
import imgManagement.Circle;
import imgManagement.CircleFinder;
import imgManagement.CircleListener;
import imgManagement.TagListener;
import utils.WallCoordinatesReader;

/**
 * Main drone controller.
 * Translates read tags into commands for the drone.
 * @author Nichlas N. Pilemand
 *
 */
public class MainDroneController extends AbstractController implements TagListener, CircleListener {

	private final static int SPEED = 4;
	private final static int SLEEP = 500;
	private final static int doFor = 20; // How long (ms) to run commands for.
	private final static int maxHeight = 3000; // Maximum height in millimeters
	private static int leftRightDiv = 10;
	private static int leftRightAdd = 5;
	


	/*
	 * This list holds tag-IDs for all tags which have successfully been visited
	 */
	private ArrayList<String> tagVisitedList = new ArrayList<String>();

	private Result tag;
	private Result lastTag;
	private float tagOrientation;
	private ArrayList<String> ports = new ArrayList<String>();
	private HashMap<String, Point> wallMarks;
	private Circle[] circles;
	private int nextPort = 3;
	private StateController sc;

	public MainDroneController(IARDrone drone) {
		super(drone);
		sc = new StateController(this, drone);
		drone.getCommandManager().setMaxAltitude(maxHeight);
		// Init ports list
		for (int i = 0; i <= 7; i++)
			ports.add("P.0" + i);
		wallMarks = WallCoordinatesReader.read();
	}

	@Override
	public void run() {
		sc.state = Command.ReadyForTakeOff;
		while (!doStop) // control loop
		{
			try {
				// reset if too old (and not updated)
				if ((tag != null) && (System.currentTimeMillis() - tag.getTimestamp() > 500)){
					tag = null;
				}
				sc.commands(sc.state);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

//			try {
//				if (circles.length > 0) {
//					if (!isCircleCentered())						
//						centerCircle();
//					else
//						goThroughPort();
//				} else {
//						Thread.currentThread();
//						Thread.sleep(SLEEP);
//				}
//				if (tag != null && lastTag != null && ports.get(nextPort).equals(lastTag.getText())) { // We haven't gone through this port
//					// Check for circles
//					if (circles.length > 0) {
//						if (isCircleCentered())
//							goThroughPort();
//						else
//							centerCircle();
//						
//					} else if (!isTagCentered()) { // tag visible, but not centered
//						centerTag();
//					}
//					else
//					{ // Try to reduce the angle to the tag
//						// TODO
//						System.out.println("AutoController.centerCircle: Go up");
//						drone.getCommandManager().up(SPEED).doFor(doFor * 2);
//						Thread.currentThread();
//						Thread.sleep(SLEEP);
//						//strayAround();
//					}
//				} else if ((tag == null) || hasTagBeenVisited()) {
//					strayAround();
//				} else if (tag != null && wallMarks.containsKey(tag.getText())) {
//					// We found a wall mark tag, continue looking around
//					strayAround();
//				
//				} else {
//					System.out.println("AutoController: I do not know what to do ...");
//					drone.getCommandManager().doFor(doFor);
//					Thread.currentThread();
//					Thread.sleep(SLEEP);
//				}
//			} catch (Exception exc) {
//				exc.printStackTrace();
//			}
		}

	}

	ArrayList<String> getTagVisitedList() {
		return tagVisitedList;
	}

	Result getTag() {
		return tag;
	}

	ArrayList<String> getPorts() {
		return ports;
	}

	HashMap<String, Point> getWallMarks() {
		return wallMarks;
	}

	Circle[] getCircles() {
		return circles;
	}


	public void onTag(Result result, float orientation) {
		if (result == null) // ToDo: do not call if no tag is present
			return;

		//System.out.println("AutoController: Tag found " + result.getText() + ", " + orientation);

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


	private void centerTag() throws InterruptedException {
		String tagText;
		ResultPoint[] points;

		synchronized (tag) {
			points = tag.getResultPoints();
			tagText = tag.getText();
		}

		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

		float x = (points[1].getX() + points[2].getX()) / 2; // True middle
		//float x = points[1].getX();
		float y = points[1].getY();

//		if ((tagOrientation > 10) && (tagOrientation < 180)) {
//			System.out.println("AutoController: Spin left");
//			drone.getCommandManager().spinLeft(SPEED * 2);
//			Thread.currentThread();
//			Thread.sleep(SLEEP);
//		} else if ((tagOrientation < 350) && (tagOrientation > 180)) {
//			System.out.println("AutoController: Spin right");
//			drone.getCommandManager().spinRight(SPEED * 2);
//			Thread.currentThread();
//			Thread.sleep(SLEEP);
		//} else 
			if (x < (imgCenterX - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go left");
			drone.getCommandManager().goLeft(SPEED).doFor(doFor);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (x > (imgCenterX + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go right");
			drone.getCommandManager().goRight(SPEED).doFor(doFor);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (y < (imgCenterY - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go up");
			drone.getCommandManager().up(SPEED).doFor(doFor * 2);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (y > (imgCenterY + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go down");
			drone.getCommandManager().down(SPEED).doFor(doFor);
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
		if (circles.length > 0) { 
			// We have more than one circle, figure out which one is correct
			for (Circle c : circles){
				// Assume the radius has to be at least 10% of the image height to be valid
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10){
					// Now do the centering
					if (c.x < (imgCenterX - MasterDrone.TOLERANCE)){
						System.out.println("AutoController.centerCircle: Go left " + Math.abs(c.x- imgCenterX + MasterDrone.TOLERANCE)/leftRightDiv + leftRightAdd);
						drone.getCommandManager().goLeft(SPEED).doFor((long) (Math.abs(c.x- imgCenterX + MasterDrone.TOLERANCE)/leftRightDiv + leftRightAdd));
						drone.getCommandManager().hover().doFor(100);
						Thread.currentThread();
						Thread.sleep(SLEEP);
					} else if (c.x > (imgCenterX + MasterDrone.TOLERANCE)) {
						System.out.println("AutoController.centerCircle: Go right " + Math.abs(c.x- imgCenterX + MasterDrone.TOLERANCE)/leftRightDiv + leftRightAdd);
						drone.getCommandManager().goRight(SPEED).doFor((long) (Math.abs(c.x- imgCenterX + MasterDrone.TOLERANCE)/leftRightDiv + leftRightAdd));
						drone.getCommandManager().hover().doFor(100);
						Thread.currentThread();
						Thread.sleep(SLEEP);
					} else if (c.y < (imgCenterY - MasterDrone.TOLERANCE)){
						System.out.println("AutoController.centerCircle: Go up");
						drone.getCommandManager().up(SPEED).doFor(doFor * 3);
						Thread.currentThread();
						Thread.sleep(SLEEP);
					} else if (c.y > (imgCenterY + MasterDrone.TOLERANCE)){
						System.out.println("AutoController.centerCircle: Go down");
						drone.getCommandManager().down(SPEED).doFor(doFor * 3);
						Thread.currentThread();
						Thread.sleep(SLEEP);
					} else if (c.r < 160) { // Fly closer
						System.out.println("AutoController.centerCircle: Go forward, radius: " + c.r);
						drone.getCommandManager().forward(SPEED).doFor(doFor);
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
		
		if (circles.length > 0)  // Same deal as centerCircle()
			for (Circle c : circles)
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10) 
					return ret = ((c.x > (imgCenterX - MasterDrone.TOLERANCE))
							&& (c.x < (imgCenterX + MasterDrone.TOLERANCE))
							&& (c.y > (imgCenterY - MasterDrone.TOLERANCE))
							&& (c.y < (imgCenterY + MasterDrone.TOLERANCE))
							&& (c.r >= 160));
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
		if (circles.length > 0)
			for (Circle c: circles)
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10) {
					System.out.println("Radius: " + c.r);
					break;
				}
//		while(true) {
//			if (!isCircleCentered())
//				break;
//			drone.getCommandManager().forward(SPEED);
//			Thread.currentThread();
//			Thread.sleep(SLEEP);
//		}
//		// TODO Here we assume we're so close to the circle that we no longer see it
//		// so fly forward
		drone.getCommandManager().forward(SPEED*4).doFor(doFor*2);
		Thread.currentThread();
		Thread.sleep(SLEEP);
	}

	/**
	 * Takes circles from {@link CircleFinder}.
	 */
	@Override
	public void circlesUpdated(Circle[] circles) {
		this.circles = circles;		
	}
}
