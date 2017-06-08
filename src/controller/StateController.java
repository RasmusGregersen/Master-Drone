package controller;

import de.yadrone.base.IARDrone;

/**
 * Created by Dave on 07/06/2017.
 */
public class StateController {

    private IARDrone drone;

    public enum Command {
        TakeOff,Hover,QRSearch,QRFound,QRValidate,SearchForCircle,CircleFound,Centralize,FlyThrough,UpdateGate,Finish
    }

    public Command state;


    private void commands(Command command){
        switch(command){
            case TakeOff: takeOff();
                break;
            case Hover: hover();
                break;
            case QRSearch: qRSearch();
                break;
            case QRFound: qRFound();
                break;
            case QRValidate: qRValidate();
                break;
            case SearchForCircle: searchForCircle();
                break;
            case CircleFound: circleFound();
                break;
            case Centralize: centralize();
                break;
            case FlyThrough: flyThrough();
                break;
            case UpdateGate: updateGate();
                break;
            case Finish: finish();
                break;
        }
    }


    public void takeOff(){
        System.out.println("TakeOff");
        drone.takeOff();
        state = Command.Hover;
    }


    public void hover() {
        System.out.println("Hover");
        drone.hover();
    }


    public void qRSearch() {
        System.out.println("QRSearch");

    }

    public void qRFound() {
        System.out.println("QRSearch");
    }

    public void qRValidate() {
        System.out.println("QRValidate");
    }

    public void searchForCircle() {
        System.out.println("SearchForCircle");
    }

    public void circleFound() {
        System.out.println("CircleFound");
    }

    public void centralize() {
        System.out.println("Centralize");
    }

    public void flyThrough() {
        System.out.println("FlyThrough");
    }

    public void updateGate() {
        System.out.println("UpdateGate");
    }

    public void finish() {
        System.out.println("Finish");
       drone.landing();
    }
}
