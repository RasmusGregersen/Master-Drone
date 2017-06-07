import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.H264;
import de.yadrone.base.command.VideoChannel;
import de.yadrone.base.command.VideoCodec;

public class MasterDrone
{
	public final static int IMAGE_WIDTH = 640;
	public final static int IMAGE_HEIGHT = 360;
	
	public final static int TOLERANCE = 40;
	
	private IARDrone drone = null;
	private AutoController autoController;
	private QRCodeScanner scanner = null;
	
	public MasterDrone()
	{
		drone = new ARDrone();
		drone.start();
		drone.getCommandManager().setVideoChannel(VideoChannel.HORI);

		GUI gui = new GUI(drone, this);
		
		// keyboard controller is always enabled and cannot be disabled (for safety reasons)
		KeyboardController keyboardController = new KeyboardController(drone);
		keyboardController.start();
		
		// auto controller is instantiated, but not started
		autoController = new AutoController(drone);
		
		scanner = new QRCodeScanner();
		scanner.addListener(gui);
		
		drone.getVideoManager().addImageListener(gui);
		drone.getVideoManager().addImageListener(scanner);
	}
	
	public void enableAutoControl(boolean enable)
	{
		if (enable)
		{
			scanner.addListener(autoController);
			autoController.start();
		}
		else
		{
			autoController.stopController();
			scanner.removeListener(autoController); // only auto autoController registers as TagListener
		}
	}
	
	public static void main(String[] args)
	{
		new MasterDrone();
	}
	
}