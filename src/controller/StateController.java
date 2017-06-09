package controller;

import de.yadrone.base.IARDrone;
import imgManagement.Circle;

import javax.xml.bind.SchemaOutputResolver;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

/**
 * Created by Dave on 07/06/2017.
 */
public class StateController {


    public enum Command {
        ReadyForTakeOff,Hover,QRSearching,QRCheck,QRValidated,QRCentralized,CircleFound,DroneCentralized,FlownThrough, Finished
    }

    public Command state;
    
    private IARDrone drone;
    private MainDroneController controller;
    
    private int nextPort = 0; // Consider handling this in MainDronController
    private final int maxPorts = 5;
    
    public StateController(MainDroneController mc, IARDrone drone) {
    	this.controller = mc;
    	this.drone = drone;
    }


    public void commands(Command command) throws InterruptedException {
        switch(command){
            case ReadyForTakeOff: takeOff();
                break;
            case Hover: hover();
                break;
            case QRSearching: qRSearch(); // Hannibal
                break;
            case QRCheck: qRValidate(); // Nichlas
                break;
            case QRValidated: qRCentralizing(); // Nichlas
                break;
            case QRCentralized: searchForCircle();
                break;
            case CircleFound: centralize(); // Lars
                break;
            case DroneCentralized: flyThrough(); // David
                break;
            case FlownThrough: updateGate();
                break;
            case Finished: finish();
                break;
        }
    }


    public void takeOff(){
        //Takeoff
        System.out.println("ReadyForTakeOff");
        drone.takeOff();
        
        //Check conditions and transit to next state
        state = Command.Hover;
    }


    public void hover() {
        //Hover method
        System.out.println("Hover");
        drone.hover();
        //Check conditions and transit to next state
        state = Command.QRSearching;
    }


    public void qRSearch() {
        //Searching method
        System.out.println("QRSearch");
        //TODO: Implement qRSearch method

        //Check conditions
        //TODO: Implement check to see if QR tag is found. Keep looking or transit state
    }
    
    
    
    

    public void qRValidate() {
    	System.out.print("State: QRValidate: ");
    	Result tag = controller.getTag();
    	synchronized(tag) {
	    	if (tag == null ) {
	    		System.out.println("no tag");
	    		this.state = Command.QRSearching;
	    	}
	    	// The scanned QR is the next port we need
	    	if (controller.getPorts().get(nextPort).equals(tag.getText())) {
	    		System.out.println("Validated port: " + tag.getText());
	    		this.state = Command.QRValidated;    		
	    	} else {
	    		System.out.println("Not validated port: " + tag.getText());
	    		this.state = Command.QRSearching;
	    	}
    	}
    }
    
    
    
    
    

    public void qRCentralizing() throws InterruptedException {
        //Centralize QR tag method
        System.out.print("State: QRcentralizing - ");
        Result tag = controller.getTag();
        if (tag == null) {
        	System.out.println("no tag, back to searching");
        	this.state = Command.QRSearching;
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
			Thread.currentThread();
			Thread.sleep(SLEEP);				
		} else if (x > (imgCenterX + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go right");
			drone.getCommandManager().goRight(SPEED).doFor(30);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (y < (imgCenterY - MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go up");
			drone.getCommandManager().up(SPEED * 2).doFor(60);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else if (y > (imgCenterY + MasterDrone.TOLERANCE)) {
			System.out.println("AutoController: Center Tag: Go down");
			drone.getCommandManager().down(SPEED * 2).doFor(60);
			Thread.currentThread();
			Thread.sleep(SLEEP);
		} else {
			System.out.println("AutoController: Tag centered");
			drone.getCommandManager().setLedsAnimation(LEDAnimation.BLINK_GREEN, 10, 5);
			this.state = Command.QRCentralized;
		}
    }

    public void searchForCircle() {
        //Increase altitude and search for the circle
        System.out.println("SearchForCircle");
        //TODO: Implement method to find circle

        //Check if circle found and transit state
        //TODO: Check if circle is found and transit state, otherwise back to search.
    }

    public void centralize() throws InterruptedException {
        //Centralize drone in front of circle
        System.out.println("Centralize");
        int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
        int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
        if (controller.getCircles().length > 0) {
            // We have more than one circle, figure out which one is correct
            for (Circle c : controller.getCircles()){
                float leftRightSpeed = (float) ((imgCenterX-c.x)/10+5)/100.0f;
                float forwardSpeed = (float) ((c.r-150)/10)/100.0f;
                float upDownSpeed = (float) ((c.y-imgCenterY)/10+5)/100.0f;
                drone.getCommandManager().move(leftRightSpeed, forwardSpeed, upDownSpeed, 0f).doFor(30);
                Thread.currentThread().sleep(30);
                if ((c.x > (imgCenterX - MasterDrone.TOLERANCE))
                        && (c.x < (imgCenterX + MasterDrone.TOLERANCE))
                        && (c.y > (imgCenterY - MasterDrone.TOLERANCE))
                        && (c.y < (imgCenterY + MasterDrone.TOLERANCE))
                        && (c.r >= 160)) {
                    state = Command.DroneCentralized;
                }
            }
        }
        else {
            state = Command.QRCentralized;
        }
    }

    public void flyThrough() {
        //Flying through the ring
        System.out.println("FlyThrough");
        //TODO: Implement flythrough

        //Updating port to search for and transit state
        //TODO: Transit state
    }

    public void updateGate() {
        //Changing which QR tag to search for next
    	nextPort++;
    	
        System.out.println("UpdateGate: next port is " + nextPort);

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
        System.out.println("Finish");
       drone.landing();
    }
}
