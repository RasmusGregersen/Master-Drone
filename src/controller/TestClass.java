package controller;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.FlyingMode;
import de.yadrone.base.command.VideoBitRateMode;
import de.yadrone.base.command.VideoChannel;
import de.yadrone.base.command.VideoCodec;
import de.yadrone.base.navdata.AttitudeListener;
import imgManagement.CircleFinder;
import imgManagement.QRCodeScanner;
import org.opencv.core.Core;

import java.io.IOException;

/**
 * Created by TheDave on 12/06/2017.
 */
public class TestClass extends Thread {
    public final static int IMAGE_WIDTH = 1280 / 2;
    public final static int IMAGE_HEIGHT = 720 / 2;
    private IARDrone drone = null;
    private QRCodeScanner scanner = null;


    public final static int TOLERANCE = 40;


    public TestClass() throws IOException, InterruptedException {
        drone = new ARDrone();
        drone.start();
        drone.getCommandManager().setConfigurationIds().setVideoCodec(VideoCodec.H264_360P);
        drone.getCommandManager().setVideoBitrateControl(VideoBitRateMode.MANUAL);
        drone.getCommandManager().setVideoBitrate(1024);
        drone.getCommandManager().setVideoChannel(VideoChannel.HORI);
        drone.getCommandManager().setNavDataDemo(true);
        drone.getNavDataManager().addAttitudeListener(new AttitudeListener() {
            public void attitudeUpdated(float pitch, float roll, float yaw){
                //droneYaw = yaw/1000;
            }
            @Override
            public void attitudeUpdated(float pitch, float roll) {}

            @Override
            public void windCompensation(float pitch, float roll) {}
        });

        KeyboardController keyboardController = new KeyboardController(drone);
        keyboardController.start();
        GUI gui = new GUI(drone, this);
        scanner = new QRCodeScanner();
        scanner.addListener(gui);
        drone.getVideoManager().addImageListener(gui);
        drone.getVideoManager().addImageListener(scanner);
        CircleFinder cf = new CircleFinder();
        drone.getVideoManager().addImageListener(cf);

        cf.addListener(gui);

        drone.getCommandManager().setFlyingMode(FlyingMode.HOVER_ON_TOP_OF_ROUNDEL);
        run();
    }

    public void run() {

       /* try {
          *//*  drone.takeOff();
            Thread.sleep(5000);
            drone.getCommandManager().hover().doFor(5000);*//*

        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }


    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load OpenCV
        try {
            new TestClass().start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
