package controller;

import java.util.Timer;
import java.util.TimerTask;

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
	
	private Timer timer;


	private int nextPort = 0; // Consider handling this in MainDronController
	private final int maxPorts = 5;

	public StateController(MainDroneController mc, IARDrone drone, CommandManager cmd) {
		this.controller = mc;
		this.cmd = cmd;
		this.drone = drone;
		this.currentMode = Mode.Normal;
		this.timer = new Timer();
	}

	public void commands(Command command) throws InterruptedException {
		if (System.currentTimeMillis() - this.controller.latestImgTime > 10) {
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
		state = Command.Hover;
	}

	public void hover() throws InterruptedException {
		// Hover method
		System.out.println("State: Hover");
		drone.hover();
		controller.sleep(100);
		// Check conditions and transit to next state
		state = Command.QRSearch;
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
		int doFor = 150;
		
		flyToHeight(1000);

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
			cmd.goRight(4).doFor(40);
			lostMode = 2;
			break;
		case 2:
			System.out.println("Look left");
			cmd.goLeft(4).doFor(40);
			lostMode = 1;
			break;
		}
		Thread.currentThread().sleep(200);
	}

	private boolean firstTag = false;

	public void qRValidate() throws InterruptedException {
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
		Thread.currentThread().sleep(10);
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
			System.out.println("Tag size: " + controller.getTagSize());
		
			synchronized (tag) {
				points = tag.getResultPoints();
				a = controller.getQRRelativeAngle(tag);
				System.out.println("Angle: " + a);
			}

			int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
			int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

			float x = (points[1].getX() + points[2].getX()) / 2; // True middle
			float y = points[1].getY();	
			a = a / 3 / 100f;
			
			
			float leftRightSpeed = (float) ((x - imgCenterX) / 45) / 100.0f;
			float upDownSpeed = (float) ((imgCenterY - y) / 6) / 100.0f;
			leftRightSpeed = limit(leftRightSpeed, -0.15f, 0.15f);
			upDownSpeed = limit(upDownSpeed, -0.15f, 0.15f);
			
			System.out.println("adjusting: " + leftRightSpeed + ", 0f, " + upDownSpeed + ", " + (float)a);
			// Left spin needs more speed
			if (a < 0)
				spin((float) (a * 5), 50);
			else
				spin((float)a, 50);
			System.out.println("Spinning");
			MainDroneController.sleep(300);
			
			if (controller.getTagSize() > MasterDrone.IMAGE_WIDTH / 14) {
				// We don't want to get too close to the tag
				System.out.println("too close, going back");
				cmd.backward(6).doFor(150);
				controller.sleep(100);
				cmd.hover();
				return;
				
			}
			System.out.println("Correcting position");				
			move(leftRightSpeed, 0f, upDownSpeed, 0f, 80).waitFor(20).hover();
			MainDroneController.sleep(300);
			
		} else {
			System.out.println("AutoController: Tag centered");
			cmd.hover();
			// ADJUSTING TO CIRCLE HEIGHT
			flyToHeight(1750);
			Thread.currentThread().sleep(800);

			this.state = Command.SearchForCircle;
		}

	}
	
	Circle lastCircle;
	int lastCircleCount = 0;

	public void searchForCircle() throws InterruptedException {
		// Increase altitude and search for the circle
		System.out.print("State: SearchForCircle - ");
		if (controller.getCircles().length >= 1) {
			for (Circle c : controller.getCircles()) {
				if (c.r > MasterDrone.IMAGE_HEIGHT / 4) {
					System.out.println("Circle found!");
					this.state = Command.Centralize;
					lastCircle = c;
					lastCircleCount = 0;
				}
			}
			
		} else {
			if (lastCircle != null) {
				if (++lastCircleCount > 10) {
					System.out.println("lastCircle for the 10th time!!!");
					state = Command.QRSearch;
					Thread.currentThread().sleep(200);
					lastCircleCount = 0;
					return;
				}
			} else {
				System.out.println("No lastCircle?!");
				this.state = Command.QRSearch;
				controller.sleep(200);
				return;
			}

			Thread.currentThread().sleep(200);

			this.state = Command.SearchForCircle;
		}
	}

	String currentCorrect = "";
	String midlertidlig = "";
	int circleCentralizeState = 0;
	int numCentralized = 0;

	public void centralize() throws InterruptedException {
		// Centralize drone in front of circle
		System.out.print("State: Centralize - ");
		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
		Circle[] circles = controller.getCircles();
		// We have more than one circle, figure out which one is correct
		if (circles.length > 0) {
			// We might have more than one circle, figure out which one is correct
			for (Circle c : circles) {
				if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 4) {
					if (controller.isCircleCentered()) {
						if (++numCentralized > 1) {
							System.out.println("CENTERED!");
							cmd.hover().doFor(100);
							this.state = Command.FlyThrough;
							numCentralized = 0;
							return;
						}
						System.out.println("Centralized #" + numCentralized);
					}
					if (circleCentralizeState == 0) { //spin
						circleCentralizeState = 1;
						double a = 92;
						double degPerPx = a /MasterDrone.IMAGE_WIDTH;
						double spin = ((c.x - imgCenterX) * degPerPx) / 2 / 100;
						spin = limit((float)spin, -0.15f, 0.15f);
						if (spin < 0)
							spin((float)(spin * 5), 30);
						else
							spin((float)spin, 30);
						controller.sleep(300);
						cmd.hover();
						return;
					}
					circleCentralizeState = 0;
					float leftRightSpeed = (float) ((c.x - imgCenterX) / 20) / 100.0f;

					int forwardSpeed =   (int) (((c.r - controller.circleRadius)) / 4);

					float upDownSpeed = (float) ((imgCenterY - c.y) / 2) / 100.0f;
					leftRightSpeed = limit(leftRightSpeed, -0.2f, 0.2f);
					forwardSpeed = limit(forwardSpeed, -20, 20);
					upDownSpeed = limit(upDownSpeed, -0.2f, 0.2f);
					
					
					midlertidlig = leftRightSpeed + ", " + forwardSpeed + ", " + upDownSpeed;
					if (currentCorrect.equals(midlertidlig)) {
						System.out.println("Picture FUCKS");
						cmd.hover().doFor(500);
						return;
					} else
						currentCorrect = midlertidlig;

					System.out.println(
							"Correcting position, " + leftRightSpeed + ", " + forwardSpeed + ", " + upDownSpeed );
					
					if (forwardSpeed < 0)
						cmd.forward(-forwardSpeed).doFor(40);
					else
						cmd.backward(forwardSpeed).doFor(40);
					controller.sleep(300);
					move(leftRightSpeed, 0f, upDownSpeed, 0f, 130);
					cmd.hover().doFor(200);
					break;
				}
			}
		} else {
			System.out.println("No circles");
			state = Command.SearchForCircle;
		}
	}

	public void flyThrough() throws InterruptedException {
		System.out.print("State: flyThrough - ");
		System.out.println("AutoController: Going through port " + nextPort);
		cmd.forward(16).doFor(1800);
		System.out.println("Returning to Hover State");
		controller.tag = null; // Reset to avoid reading while flying through
		firstTag = false;
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
	 * reached. Has a tolerance of 5 cms in each direction.
	 * 
	 * @param height
	 *            The requested height in millimeters.
	 */
	private void flyToHeight(int height) {
		System.out.println("StateController: flyToHeight: " + height);
		while (true) {
			if (height + 50 < controller.getAltitude()) { // fly down
				cmd.down(30).doFor(30).hover();
				// Sleep maybe?
			} else if (height - 50 > controller.getAltitude()) { // fly up
				cmd.up(30).doFor(30).hover();
			} else {
				System.out.println("Reached height: " + controller.getAltitude()); // done
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
	
	/**
	 * A method to translate spins as a float into movements,
	 * as the CommandManager's spin methods only takes integers.
	 * @param speed Speed of the command, [-1, 1]
	 * @param doFor How many milliseconds to run the command for
	 * @return {@link CommandManager} for chaining commands
	 */
	private CommandManager spin(float speed, int doFor) {
		return cmd.move(0f, 0f, 0f, speed).doFor(doFor);
	}
	
	/**
	 * A method to limit values.
	 * @param i The value
	 * @param min
	 * @param max
	 * @return The limited value.
	 */
	private int limit(int i, int min, int max) {
		return (i > max ? max : (i < min ? min : i));
	}

	// See above
	private float limit(float f, float min, float max) {
		return (f > max ? max : (f < min ? min : f));
	}
	
	/**
	 * Attempt at emulating sticky commands.
	 */
	private CommandManager move(float lrtilt, float fbtilt, float vspeed, float aspeed, int doFor) throws InterruptedException {
		for (int i = 1; i < doFor/7; i++) {
			cmd.move(lrtilt, fbtilt, vspeed, aspeed).doFor(25);
		}
		return cmd.move(lrtilt, fbtilt, vspeed, aspeed);
	}
	
}
