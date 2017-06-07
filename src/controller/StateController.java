package controller;

import de.yadrone.base.IARDrone;

/**
 * Created by Dave on 07/06/2017.
 */
public class StateController {

    private IARDrone drone;

    public enum command{
        TakeOff,Hover,QRSearch,QRFound,QRValidate,SearchForCircle,CircleFound,Centralize,FlyThrough,UpdateGate,Finish
    }

    public command state;


    private void commands(command command){
        switch(command){
            case TakeOff: TakeOff();
                break;
            case Hover: Hover();
                break;
            case QRSearch: QRSearch();
                break;
            case QRFound: QRFound();
                break;
            case QRValidate: QRValidate();
                break;
            case SearchForCircle: SearchForCircle();
                break;
            case CircleFound: CircleFound();
                break;
            case Centralize: Centralize();
                break;
            case FlyThrough: FlyThrough();
                break;
            case UpdateGate: UpdateGate();
                break;
            case Finish: Finish();
                break;
        }
    }


    public void TakeOff(){
        System.out.println("TakeOff");
        drone.takeOff();

    }


    public void Hover() {
        System.out.println("Hover");
        drone.hover();
    }


    public void QRSearch() {
        System.out.println("QRSearch");

    }

    public void QRFound() {
        System.out.println("QRSearch");
    }

    public void QRValidate() {
        System.out.println("QRValidate");
    }

    public void SearchForCircle() {
        System.out.println("SearchForCircle");
    }

    public void CircleFound() {
        System.out.println("CircleFound");
    }

    public void Centralize() {
        System.out.println("Centralize");
    }

    public void FlyThrough() {
        System.out.println("FlyThrough");
    }

    public void UpdateGate() {
        System.out.println("UpdateGate");
    }

    public void Finish() {
        System.out.println("Finish");
       drone.landing();

    }
}
