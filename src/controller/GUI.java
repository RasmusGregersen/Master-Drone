package controller;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import de.yadrone.base.IARDrone;
import de.yadrone.base.navdata.ControlState;
import de.yadrone.base.navdata.DroneState;
import de.yadrone.base.navdata.StateListener;
import de.yadrone.base.video.ImageListener;
import imgManagement.TagListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;

public class GUI extends JFrame implements ImageListener, TagListener
{
	private MasterDrone main;
	private IARDrone drone;

	private BufferedImage image = null;
	private Result result;
	private String orientation;

	private String[] ringsToFind = new String[] {"Ring 1", "Ring 2"};
	private boolean[] ringsFound = new boolean[] {false, false};

	private JPanel videoPanel;

	private Timer timer = new Timer();
	private long gameStartTimestamp = System.currentTimeMillis();
	private String gameTime = "0:00";

	private boolean gameOver = false;

	public GUI(final IARDrone drone, MasterDrone main)
	{
		super("Master Drone");

		this.main = main;
		this.drone = drone;

		createMenuBar();

        setSize(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT);
        setVisible(true);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				drone.stop();
				System.exit(0);
			}
		});

        setLayout(new GridBagLayout());
        
        add(createVideoPanel(), new GridBagConstraints(0, 0, 1, 2, 1, 1, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));

        // add listener to be notified once the drone takes off so that the game timer counter starts
        drone.getNavDataManager().addStateListener(new StateListener() {

			public void stateChanged(DroneState state)
			{
				if (state.isFlying())
				{
					startGameTimeCounter();
					drone.getNavDataManager().removeStateListener(this);
				}
			}

			public void controlStateChanged(ControlState state) { }
		});
        pack(); 
	}

	private void createMenuBar()
	{
		JMenu options = new JMenu("Options");

		final JCheckBoxMenuItem autoControlMenuItem = new JCheckBoxMenuItem("Start autonomous flight");
		autoControlMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				main.enableAutoControl(autoControlMenuItem.isSelected());
			}
		});

		options.add(autoControlMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(options);

		setJMenuBar(menuBar);
	}

	private JPanel createVideoPanel()
	{
		videoPanel = new JPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private Font tagFont = new Font("SansSerif", Font.BOLD, 14);
			private Font timeFont = new Font("SansSerif", Font.BOLD, 18);
			private Font gameOverFont = new Font("SansSerif", Font.BOLD, 36);

        	public void paint(Graphics g)
        	{
        		if (image != null)
        		{
        			// now draw the camera image
        			g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

        			// draw "Shreds to find"
    				g.setColor(Color.RED);
    				g.setFont(tagFont);
    				g.drawString("Rings to find", 10, 20);
    				for (int i = 0; i < ringsToFind.length; i++)
    				{
    					if (ringsFound[i])
    						g.setColor(Color.GREEN.darker());
    					else
    						g.setColor(Color.RED);
    					g.drawString(ringsToFind[i], 30, 40 + (i*20));
    				}

        			// draw tolerance field (rectangle)
        			g.setColor(Color.RED);

    				int imgCenterX = MasterDrone.IMAGE_WIDTH / 2;
    				int imgCenterY = MasterDrone.IMAGE_HEIGHT / 2;
    				int tolerance = MasterDrone.TOLERANCE;

    				g.drawPolygon(new int[] {imgCenterX-tolerance, imgCenterX+tolerance, imgCenterX+tolerance, imgCenterX-tolerance},
						      		  new int[] {imgCenterY-tolerance, imgCenterY-tolerance, imgCenterY+tolerance, imgCenterY+tolerance}, 4);

    				// draw triangle if tag is visible
        			if (result != null)
        			{
        				ResultPoint[] points = result.getResultPoints();
        				ResultPoint a = points[1]; // top-left
        				ResultPoint b = points[2]; // top-right
        				ResultPoint c = points[0]; // bottom-left
        				ResultPoint d = points.length == 4 ? points[3] : points[0]; // alignment point (bottom-right)

        				g.setColor(Color.GREEN);

        				g.drawPolygon(new int[] {(int)a.getX(),(int)b.getX(),(int)d.getX(),(int)c.getX()},
  						      new int[] {(int)a.getY(),(int)b.getY(),(int)d.getY(),(int)c.getY()}, 4);

        				g.setColor(Color.RED);
        				g.setFont(tagFont);
        				g.drawString(result.getText(), (int)a.getX(), (int)a.getY());
        				g.drawString(orientation, (int)a.getX(), (int)a.getY() + 20);

        				if ((System.currentTimeMillis() - result.getTimestamp()) > 1000)
        				{
        					result = null;
        				}
        			}

        			// draw "Congrats" if all tags have been detected
        			if (gameOver)
        			{
        				String str = "All rings found!";

        				g.setColor(Color.GREEN.darker());
        				g.setFont(gameOverFont);

        				FontMetrics metrics = g.getFontMetrics(gameOverFont);
        				int hgt = metrics.getHeight();
        				int adv = metrics.stringWidth(str);

        				g.drawString(str, (getWidth() / 2) - (adv / 2), (getHeight() / 2) - (hgt / 2) - 50); // draw text centered
        			}

        			// draw the time
    				g.setColor(Color.RED);
    				g.setFont(timeFont);
    				g.drawString(gameTime, getWidth() - 50, 20);
        		}
        		else
        		{
        			// draw "Waiting for video"
        			g.setColor(Color.RED);
    				g.setFont(tagFont);
        			g.drawString("Waiting for VideoRecognition ...", 10, 20);
        		}
        	}
        };

        // a click on the video shall toggle the camera (from vertical to horizontal and vice versa)
		videoPanel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				drone.toggleCamera();
			}
		});

        videoPanel.setSize(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT);
        videoPanel.setMinimumSize(new Dimension(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT));
        videoPanel.setPreferredSize(new Dimension(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT));
        videoPanel.setMaximumSize(new Dimension(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT));
        
        return videoPanel;
	}
	
	private long imageCount = 0;
	
	public void imageUpdated(BufferedImage newImage)
    {
		if ((++imageCount % 2) == 0)
			return;
		
    	image = newImage;
		SwingUtilities.invokeLater(new Runnable() {
			public void run()
			{
				videoPanel.repaint();
			}
		});
    }
	
	public void onTag(Result result, float orientation)
	{
		if (result != null)
		{
			this.result = result;
			this.orientation = orientation + "�";
			
			// check if that's a tag (shred) which has not be seen before and mark it as 'found'
			for (int i = 0; i < ringsToFind.length; i++)
			{
				if (ringsToFind[i].equals(result.getText()))
				{
					ringsToFind[i] = ringsToFind[i] + " - " + gameTime;
					ringsFound[i] = true;
				}
			}
			
			// now check if all shreds have been found and if so, set the gameOver flag
			boolean isGameOver = true;
			for (int i = 0; i < ringsFound.length; i++)
			{
				if (ringsFound[i] == false)
					isGameOver = false;
			}
			
			if (isGameOver) // all shreds found ?
			{
				gameOver = true;
				stopGameTimeCounter();
			}
		}
	}
	
	private void startGameTimeCounter()
	{
		gameStartTimestamp = System.currentTimeMillis();
		
		TimerTask timerTask = new TimerTask() {

			public void run()
			{
				long time = System.currentTimeMillis() - gameStartTimestamp;
				
				int minutes = (int)(time / (60 * 1000));
				int seconds = (int)((time / 1000) % 60);
				gameTime = String.format("%d:%02d", minutes, seconds);
			}
		};
		
		timer.schedule(timerTask, 0, 1000);		
	}
	
	private void stopGameTimeCounter()
	{
		timer.cancel();
	}
}
