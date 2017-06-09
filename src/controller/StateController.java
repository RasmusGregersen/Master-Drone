package controller;

import de.yadrone.base.IARDrone;
import imgManagement.Circle;

import javax.xml.bind.SchemaOutputResolver;

/**
 * Created by Dave on 07/06/2017.
 */
public class StateController {


    public enum Command {
        ReadyForTakeOff,Hover,QRSearching,QRFound,QRValidated,QRCentralized,CircleFound,DroneCentralized,FlownThrough, Finished
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
            case QRFound: qRValidate(); // Nichlas
                break;
            case QRValidated: qRCentralizing();
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
    	if (controller.getTag() == null ) {
    		System.out.println("no tag");
    		this.state = Command.QRSearching;
    	}
    	// The scanned QR is the next port we need
    	if (controller.getPorts().get(nextPort).equals(controller.getTag().getText())) {
    		System.out.println("Validated port: " + nextPort);
    		this.state = Command.QRValidated;    		
    	}
    }
    
    
    
    
    

    public void qRCentralizing() {
        //Centralize QR tag method
        System.out.println("QRcentralizing");
        //TODO: Implement method to centralize QR tag

        //If succesfully centralized, transit otherwise move back to searching
        //TODO: Implement check otherwise move back to search
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
                Thread.sleep(30);
                if ((c.x > (imgCenterX - MasterDrone.TOLERANCE))
                        && (c.x < (imgCenterX + MasterDrone.TOLERANCE))
                        && (c.y > (imgCenterY - MasterDrone.TOLERANCE))
                        && (c.y < (imgCenterY + MasterDrone.TOLERANCE))
                        && (c.r >= 160)) {
                    state = Command.DroneCentralized;
                }
            }
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
