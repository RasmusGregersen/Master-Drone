package controller;

import java.util.ArrayList;
import java.util.Random;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import de.yadrone.base.IARDrone;
import de.yadrone.base.command.LEDAnimation;
import imgManagement.TagListener;

/**
 * Main drone controller.
 * Translates read tags into commands for the drone.
 * @author Nichlas N. Pilemand
 *
 */
public class MainDroneController extends AbstractController implements TagListener {

	private final static int SPEED = 5;
	private final static int SLEEP = 500;

	/*
	 * This list holds tag-IDs for all tags which have successfully been visited
	 */
	private ArrayList<String> tagVisitedList = new ArrayList<String>();

	private Result tag;
	private float tagOrientation;

	public MainDroneController(IARDrone drone) {
		super(drone);
	}

	@Override
	public void run() {
		while (!doStop) // control loop
		{
			try {
				// reset if too old (and not updated)
				if ((tag != null) && (System.currentTimeMillis() - tag.getTimestamp() > 500))
					tag = null;

				if ((tag == null) || hasTagBeenVisited()) {
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

		System.out.println("AutoController: Tag found");

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
			drone.getCommandManager().forward(SPEED);
			System.out.println("AutoController: Stray Around: FORWARD");
			break;
		case 1:
			drone.getCommandManager().backward(SPEED);
			System.out.println("AutoController: Stray Around: BACKWARD");
			break;
		case 2:
			drone.getCommandManager().goLeft(SPEED);
			System.out.println("AutoController: Stray Around: LEFT");
			break;
		case 3:
			drone.getCommandManager().goRight(SPEED);
			System.out.println("AutoController: Stray Around: RIGHT");
			break;
		}

		Thread.currentThread().sleep(SLEEP);
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
			Thread.currentThread().sleep(SLEEP);
		} else if ((tagOrientation < 350) && (tagOrientation > 180)) {
			System.out.println("AutoController: Spin right");
			drone.getCommandManager().spinRight(SPEED * 2);
			Thread.currentThread().sleep(SLEEP);
		} else if (x < (imgCenterX - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Go left");
			drone.getCommandManager().goLeft(SPEED);
			Thread.currentThread().sleep(SLEEP);
		} else if (x > (imgCenterX + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Go right");
			drone.getCommandManager().goRight(SPEED);
			Thread.currentThread().sleep(SLEEP);
		} else if (y < (imgCenterY - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Go forward");
			drone.getCommandManager().forward(SPEED);
			Thread.currentThread().sleep(SLEEP);
		} else if (y > (imgCenterY + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Go backward");
			drone.getCommandManager().backward(SPEED);
			Thread.currentThread().sleep(SLEEP);
		} else {
			System.out.println("AutoController: Tag centered");
			drone.getCommandManager().setLedsAnimation(LEDAnimation.BLINK_GREEN, 10, 5);

			tagVisitedList.add(tagText);
		}
	}
}
