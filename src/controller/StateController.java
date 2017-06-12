package controller;

import de.yadrone.base.IARDrone;
import imgManagement.Circle;

import javax.xml.bind.SchemaOutputResolver;
import java.util.Random;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

/**
 * Created by Dave on 07/06/2017..
 */
public class StateController {


    public enum Command {
        TakeOff,Hover,QRSearch,QRValidate,QRCentralizing,SearchForCircle,Centralize,FlyThrough,UpdateGate, Finished
    }

    public Command state;
    
    private IARDrone drone;
    private MainDroneController controller;
    
    private int nextPort = 1; // Consider handling this in MainDronController
    private final int maxPorts = 5;
    
    public StateController(MainDroneController mc, IARDrone drone) {
    	this.controller = mc;
    	this.drone = drone;
    }


	public void commands(Command command) throws InterruptedException {
        switch(command){
            case TakeOff: takeOff();
                break;
            case Hover: hover();
                break;
            case QRSearch: this.state = Command.Centralize;//qRSearch(); // Hannibal
                break;
            case QRValidate: qRValidate(); // Nichlas
                break;
            case QRCentralizing: qRCentralizing(); // Nichlas
                break;
            case SearchForCircle: searchForCircle(); //David
                break;
            case Centralize: centralize(); // Lars
                break;
            case FlyThrough: flyThrough(); // David
                break;
            case UpdateGate: updateGate();
                break;
            case Finished: finish();
                break;
        }
    }


    public void takeOff() throws InterruptedException{
        //Takeoff
        System.out.println("State: ReadyForTakeOff");
        drone.takeOff();
        MainDroneController.sleep(3000);
        drone.getCommandManager().forward(1).doFor(10);     
        //Check conditions and transit to next state
        state = Command.Hover;
    }


    public void hover() throws InterruptedException {
        //Hover method
        System.out.println("State: Hover");
        drone.getCommandManager().hover().doFor(500);
		MainDroneController.sleep(550);
        //Check conditions and transit to next state
        state = Command.QRSearch;
    }

    int strayMode = 0;

    public void qRSearch() throws InterruptedException {

        int SPEEDSpin = 10;
        int SPEEDMove = 4;
        int doFor = 20;

        //Searching method
        System.out.println("State: QRSearch");
        //TODO: Implement qRSearch method

        switch(strayMode) {
            case 0:
                System.out.println("AutoController: Stray Around: Spin right, Case: 0");
                drone.getCommandManager().spinRight(SPEEDSpin * 3).doFor(doFor);
                strayMode++;
                break;
            case 1:
                System.out.println("AutoController: Stray Around: Go up, Case: 1");
                drone.getCommandManager().up(SPEEDMove).doFor(doFor);
                strayMode++;
                break;
            case 2:
                System.out.println("AutoController: Stray Around: Spin right, Case: 2");
                drone.getCommandManager().spinRight(SPEEDSpin * 3).doFor(doFor);
                strayMode++;
                break;
            case 3:
                System.out.println("AutoController: Stray Around: Go down, Case: 3");
                drone.getCommandManager().down(SPEEDMove).doFor(doFor);
                strayMode = 0;
                break;
        }
        MainDroneController.sleep(200);
        state = Command.QRValidate;
    }

    

    public void qRValidate() {
    	System.out.print("State: QRValidate: ");
    	Result tag = controller.getTag();
    	if (tag == null ) {
    		System.out.println("no tag");
    		this.state = Command.QRSearch;
    		return;
    	}
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
        //Centralize QR tag method
        System.out.print("State: QRcentralizing - ");
        Result tag = controller.getTag();
        if (tag == null) {
        	System.out.println("no tag, back to searching");
        	this.state = Command.QRSearch;
        	return;
        }
        final int SPEED = 5;
        final int SLEEP = 500;
        ResultPoint[] points;
		synchronized (tag) {
			points = tag.getResultPoints();
		}

		int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
		int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;

		float x = (points[1].getX() + points[2].getX()) / 2; // True middle
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
			drone.getCommandManager().goLeft(SPEED).doFor(30);
			MainDroneController.sleep(SLEEP);				
		} else if (x > (imgCenterX + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go right");
			drone.getCommandManager().goRight(SPEED).doFor(30);
			MainDroneController.sleep(SLEEP);
		} else if (y < (imgCenterY - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go up");
			drone.getCommandManager().up(SPEED * 2).doFor(60);
			MainDroneController.sleep(SLEEP);
		} else if (y > (imgCenterY + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go down");
			drone.getCommandManager().down(SPEED * 2).doFor(60);
			MainDroneController.sleep(SLEEP);
		} else {
			System.out.println("AutoController: Tag centered");
			this.state = Command.SearchForCircle;
		}
    }

    public void searchForCircle() throws InterruptedException {
        //Increase altitude and search for the circle
        System.out.print("State: SearchForCircle - ");
        if (controller.getCircles().length >= 1) {
            System.out.println("Circle found!");
            this.state = Command.Centralize;
        }
        else {
        	// TODO Needs to actually FIND the circle
            System.out.println("Returning TO QR SEARCH!");
            this.state = Command.QRSearch;
            MainDroneController.sleep(200);
        }
    }
    
	private float limit(float f, float min, float max) {
		return (f > max ? max : (f < min ? min : f));
	}

    public void centralize() throws InterruptedException {
        //Centralize drone in front of circle
        System.out.print("State: Centralize - ");
        int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
        int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
        if (controller.getCircles().length > 0) {
            // We have more than one circle, figure out which one is correct
            for (Circle c : controller.getCircles()){
            	if (c.getRadius() >= MasterDrone.IMAGE_HEIGHT / 10) {
            		if (controller.isCircleCentered()) {
            			System.out.println("CENTERED!");
            			this.state = Command.FlyThrough;
            			return;
            		}
	                float leftRightSpeed = (float) ((c.x - imgCenterX) / 10) / 100.0f;
	                float forwardSpeed = (float) ((c.r - 160) / 6 ) / 100.0f;
	                float upDownSpeed = (float) ((imgCenterY - c.y) / 10) / 100.0f;
	                leftRightSpeed = limit(leftRightSpeed, -0.1f, 0.1f);
	                System.out.println("Correcting position, " + leftRightSpeed +", " + forwardSpeed +", " + upDownSpeed);
	                drone.getCommandManager().move(leftRightSpeed, forwardSpeed, upDownSpeed, 0f).doFor(30);
	                drone.hover();
	                MainDroneController.sleep(300);
	                break;
            	}
            }
        }
        else {
            state = Command.SearchForCircle;
        }
    }

    public void flyThrough() throws InterruptedException {
    	System.out.print("State: flyThrough - ");
        System.out.println("AutoController: Going through port " + nextPort);
        drone.getCommandManager().forward(16).doFor(1200);
        MainDroneController.sleep(1500);
        drone.getCommandManager().hover();
        System.out.println("Returning to Hover State");
        state = Command.UpdateGate;
    }

    public void updateGate() {
    	System.out.print("State: updateGate: ");
        //Changing which QR tag to search for next
    	nextPort++;
    	
        System.out.println("Next port is " + nextPort);

        //Changing state to finishing or searching for next tag depending on port state
        if(nextPort>maxPorts) {
        	System.out.println("setting state to finish");
        	this.state=Command.Finished;
        }
        else {
        	System.out.println("setting state hover");
        	this.state=Command.Hover;
        }
    }

    public void finish() {
       System.out.println("State: Finish");
       drone.landing();
       controller.stopController();
    }
}
