import com.google.zxing.Result;
import de.yadrone.base.IARDrone;

public abstract class AbstractController extends Thread implements TagListener
{
	protected boolean doStop = false;

	protected IARDrone drone;
	
	public AbstractController(IARDrone drone)
	{
		this.drone = drone;
	}

	public abstract void run();
	
	public void onTag(Result result, float orientation)
	{

	}
	
	public void stopController()
	{
		doStop = true;
	}
}
