package controller;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import imgManagement.Circle;

/**
 * Created by Dave on 07/06/2017..
 */
public class StateController {

	private enum Mode {
		Normal, Continous
	}

	private Mode currentMode;

	public enum Command {
		TakeOff, Hover, QRSearch, QRLost, QRValidate, QRCentralizing, SearchForCircle, Centralize, FlyThrough, UpdateGate, Finished
	}

	public Command state;

	private IARDrone drone;
	private CommandManager cmd;
	private MainDroneController controller;
	int strayModeCircle = 0;

	private int nextPort = 1; // Consider handling this in MainDronController
	private final int maxPorts = 5;

	public StateController(MainDroneController mc, IARDrone drone, CommandManager cmd) {
		this.controller = mc;
		this.cmd = cmd;
		this.drone = drone;
		this.currentMode = Mode.Continous;
	}

	public void commands(Command command) throws InterruptedException {
		if (System.currentTimeMillis() - this.controller.latestImgTime < 150) {
			System.out.println("Image lag, delaying commands...");
			drone.hover();
			MainDroneController.sleep(150);
		}
		switch (command) {
		case TakeOff:
			takeOff();
			break;
		case Hover:
			hover();
			break;
		case QRSearch:
			qRSearch();// qRSearch(); // Hannibal
			break;
		case QRLost:
			qRLost(); // Rasmus og Lars
			break;
		case QRValidate:
			qRValidate(); // Nichlas
			break;
		case QRCentralizing:
			qRCentralizing(); // Nichlas
			break;
		case SearchForCircle:
			searchForCircle(); // David
			break;
		case Centralize:
			centralize(); // Lars
			break;
		case FlyThrough:
			flyThrough(); // David
			break;
		case UpdateGate:
			updateGate();
			break;
		case Finished:
			finish();
			break;
		}
	}

	public void takeOff() throws InterruptedException {
		// Takeoff
		System.out.println("State: ReadyForTakeOff");
		cmd.takeOff();
		flyToHeight(1000);
		Thread.currentThread().sleep(500);
		// //MainDroneController.sleep(1250);
		// cmd.up(15).doFor(1250);
		// MainDroneController.sleep(1250);
		// cmd.landing();
		// MainDroneController.sleep(3000);
		// Check conditions and transit to next state

		state = Command.Hover;
	}

	public void hover() throws InterruptedException {
		// Hover method
		System.out.println("State: Hover");
		cmd.hover().doFor(500);
		// Thread.currentThread().sleep(5000);
		// MainDroneController.sleep(550);
		// Check conditions and transit to next state
		state = Command.QRValidate;
	}

	int strayMode = 0;

	public void qRSearch() throws InterruptedException {

		// Searching method
		System.out.print("State: QRSearch - ");

		int SPEEDSpin = 80;
		int SPEEDMove = 4;
		int doFor = 30;

		Result tag = controller.getTag();
		if (tag != null) {
			System.out.println("Tag found");
			this.state = Command.QRValidate;
			return;
		}
		flyToHeight(1000);
		System.out.println("Spin right");
		spinRight(SPEEDSpin, doFor);
		controller.sleep(800);
	}

	int lostMode = 0;

	public void qRLost() throws InterruptedException {
		System.out.print("State: QR Lost: ");

		int Speed = 5;
		int doFor = 200;

		Result tag = controller.getTag();
		if (tag != null) {
			System.out.println("Tag found");
			this.state = Command.QRValidate;
			return;
		}
		switch (lostMode) {
		case 0:
			System.out.println("Fly backwards");
			cmd.backward(Speed).doFor(doFor);
			lostMode = 1;
			break;
		case 1:
			System.out.println("Look right");
			spinRight(Speed, doFor);
			lostMode = 2;
			break;
		case 2:
			System.out.println("Look left");
			spinLeft(Speed*2, doFor);
			lostMode = 1;
			break;
		}
		Thread.currentThread().sleep(100);
	}

	private boolean firstTag = false;

	public void qRValidate() {
		System.out.print("State: QRValidate: ");
		Result tag = controller.getTag();
		if (tag == null) {
			if (firstTag) {
				System.out.println("Tag Lost");
				firstTag = false;
				this.state = Command.QRLost;
				return;
			}
			this.state = Command.QRSearch;
			return;

		}
		firstTag = true;
		// The scanned QR is the next port we need
		if (controller.getPorts().get(nextPort).equals(tag.getText())) {
			System.out.println("Validated port: " + tag.getText());
			this.state = Command.QRCentralizing;
		} else {
			System.out.println("Not validated port: " + tag.getText());
			this.state = Command.QRSearch;
		}
	}

	public void qRCentralizing() throws InterruptedException {
		// Centralize QR tag method
		System.out.print("State: QRcentralizing - ");
		Result tag = controller.getTag();
		if (tag == null) {
			System.out.println("no tag, back to searching");
			this.state = Command.QRLost;
			return;
		}

		if (!controller.isTagCentered()) { // Inverted for readability, lol.
			ResultPoint[] points;
			double a;
			synchronized (tag) {
				points = tag.getResultPoints();
				a = controller.getQRRelativeAngle(tag);
			}

			int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
			int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

			float x = (points[1].getX() + points[2].getX()) / 2; // True middle
			float y = points[1].getY();	
			a = a / 12 / 100f;
			
			
			
			float leftRightSpeed = (float) ((x - imgCenterX) / 25) / 100.0f;
			float upDownSpeed = (float) ((imgCenterY - y) / 5) / 100.0f;
			leftRightSpeed = limit(leftRightSpeed, -0.15f, 0.15f);
			upDownSpeed = limit(upDownSpeed, -0.15f, 0.15f);
			System.out.println("adjusting: " + leftRightSpeed + ", 0f, " + upDownSpeed + ", " + (float)a);
			cmd.move(0f,0f,0f,(float)a).doFor(30);
			cmd.move(leftRightSpeed, 0f, upDownSpeed, 0f).doFor(40);
			MainDroneController.sleep(100);
			
		} else {
			System.out.println("AutoController: Tag centered");

			// ADJUSTING TO CIRCLE HEIGHT
			flyToHeight(1700);
			Thread.currentThread().sleep(200);

			this.state = Command.SearchForCircle;
		}

	}

	public void searchForCircle() throws InterruptedException {
		// Increase altitude and search for the circle
		System.out.print("State: SearchForCircle - ");
		if (controller.getCircles().length >= 1) {
			System.out.println("Circle found!");
			this.state = Command.Centralize;
		} else {
			// TODO Needs to actually FIND the circle
			System.out.println("No circle found");
			int SPEEDSpin = 10;
			int SPEEDMove = 4;
			int doFor = 20;

			Thread.currentThread().sleep(500);

			this.state = Command.SearchForCircle;
		}
	}

	String currentCorrect = "";
	String midlertidlig = "";

	public void centralize() throws InterruptedException {
		// Centralize drone in front of circle
		System.out.print("State: Centralize - ");
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
		// We have more than one circle, figure out which one is correct
		if (controller.getCircles().length > 0) {
			// We have more than one circle, figure out which one is correct
			for (Circle c : controller.getCircles()) {
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10) {
					if (controller.isCircleCentered()) {
						System.out.println("CENTERED!");
						this.state = Command.FlyThrough;
						return;
					}
					float leftRightSpeed = (float) ((c.x - imgCenterX) / 15) / 100.0f;

					float forwardSpeed = (float) ((c.r - 160) / 6) / 100.0f;

					float upDownSpeed = (float) ((imgCenterY - c.y) / 5) / 100.0f;
					leftRightSpeed = limit(leftRightSpeed, -0.15f, 0.15f);
					forwardSpeed = limit(forwardSpeed, -0.15f, 0.15f);
					upDownSpeed = limit(upDownSpeed, -0.15f, 0.15f);
					
					midlertidlig = leftRightSpeed + ", " + forwardSpeed + ", " + upDownSpeed;
					if (currentCorrect.equals(midlertidlig)) {
						System.out.println("Picture FUCKS");
						cmd.hover().doFor(500);
						return;
					} else
						currentCorrect = midlertidlig;

					System.out.println(
							"Correcting position, " + leftRightSpeed + ", " + forwardSpeed + ", " + upDownSpeed);
					cmd.move(leftRightSpeed, forwardSpeed, upDownSpeed, 0f).doFor(60);
					drone.hover();
					controller.sleep(300);
					break;
				}
			}
		} else {
			state = Command.SearchForCircle;
		}
	}

	public void flyThrough() throws InterruptedException {
		System.out.print("State: flyThrough - ");
		System.out.println("AutoController: Going through port " + nextPort);
		cmd.forward(16).doFor(2000);
		// Thread.currentThread().sleep(1200);
		cmd.hover();
		System.out.println("Returning to Hover State");
		state = Command.UpdateGate;
	}

	public void updateGate() {
		System.out.print("State: updateGate: ");
		// Changing which QR tag to search for next
		nextPort++;

		// Changing state to finishing or searching for next tag depending on
		// port state
		if (nextPort > maxPorts) {
			switch (this.currentMode) {
			case Continous:
				System.out.println("\n  Starting over as mode is continous.");
				nextPort = 0;
				this.state = Command.Hover;
			case Normal:
				System.out.println("\n  Last port reached.");
				this.state = Command.Finished;
				break;
			default:
				this.state = Command.Hover; // Redundant
				break;
			}
		} else {
			this.state = Command.Hover;
		}
		System.out.println("Next port is " + nextPort);
	}

	public void finish() {
		System.out.println("State: Finish");
		cmd.landing();
		controller.stopController();
	}

	/**
	 * Puts the drone to a specific height. This will run until the height is
	 * reached. Has a tolerance of 10 cms in each direction.
	 * 
	 * @param height
	 *            The requested height in millimeters.
	 */
	private void flyToHeight(int height) {
		System.out.println("StateController: flyToHeight: " + height);
		while (true) {
			if (height + 100 < this.controller.getAltitude()) { // fly down
				cmd.down(30).doFor(15).hover();
				// Sleep maybe?
			} else if (height - 100 > this.controller.getAltitude()) { // fly up
				cmd.up(30).doFor(15).hover();
			} else {
				System.out.println("Reached height: " + this.controller.getAltitude()); // done
				return;
			}
		}
	}

	/**
	 * Separate method for spinning to compensate for stability issues.
	 * 
	 * @param speed
	 *            Int 0 - 100: Percent of max speed
	 * @param doFor
	 *            Integer: How long in ms to run the command
	 * @return {@link CommandManager} for chaining commands
	 * @see StateController#spinLeft spinLeft().
	 */
	private CommandManager spinRight(int speed, int doFor) {
		return cmd.spinRight(speed).doFor(doFor).spinLeft(1);
	}

	/**
	 * SpinLeft
	 * 
	 * @see StateController#spinRight(int, int) spinRight()
	 */
	private CommandManager spinLeft(int speed, int doFor) {
		return cmd.spinLeft(speed).doFor(doFor).spinRight(1);
	}
	
	private int limit(int i, int min, int max) {
		return (i > max ? max : (i < min ? min : i));
	}

	private float limit(float f, float min, float max) {
		return (f > max ? max : (f < min ? min : f));
	}
}
