package controller;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import de.yadrone.base.IARDrone;
import de.yadrone.base.navdata.BatteryListener;
import de.yadrone.base.video.ImageListener;
import imgManagement.Circle;
import imgManagement.CircleListener;
import imgManagement.TagListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class GUI extends JFrame implements ImageListener, TagListener, CircleListener {
	private MasterDrone main;
	private TestClass test;
	private IARDrone drone;

	private BufferedImage image = null;
	private Result result;
	private String orientation;
	private Circle[] circles;

	private JPanel videoPanel;
	private int batterypercentage;
	private int imgScale = 1; // Scale the preset width/height with this factor

	public GUI(final IARDrone drone, MasterDrone main) {
		super("Master Drone");

		this.main = main;
		this.drone = drone;

		batteryListener();

		createMenuBar();

        setSize(MasterDrone.IMAGE_WIDTH * imgScale, MasterDrone.IMAGE_HEIGHT * imgScale);
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
        pack(); 
	}
	public GUI(final IARDrone drone, TestClass main) {
		super("Master Drone");

		this.test = main;
		this.drone = drone;

		batteryListener();

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
		pack();
	}

	private void createMenuBar() {
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

	private JPanel createVideoPanel() {
		videoPanel = new JPanel() {

			/**
			 *
			 */
			private static final long serialVersionUID = 1L;
			private Font tagFont = new Font("SansSerif", Font.BOLD, 14*imgScale/2);
			private Font timeFont = new Font("SansSerif", Font.BOLD, 18*imgScale/2);
			private Font gameOverFont = new Font("SansSerif", Font.BOLD, 36*imgScale/2);

        	public void paint(Graphics g)
        	{
        		if (image != null)
        		{
        			// now draw the camera image
        			Image img = image.getScaledInstance(MasterDrone.IMAGE_WIDTH * imgScale, MasterDrone.IMAGE_HEIGHT * imgScale, Image.SCALE_DEFAULT);
        			g.drawImage(img, 0, 0, MasterDrone.IMAGE_WIDTH * imgScale, MasterDrone.IMAGE_HEIGHT * imgScale, null);

        			// draw the battery percentage
					if(batterypercentage>50) {
						g.setColor(Color.GREEN);
						g.setFont(tagFont);
					}
					else {
						g.setColor(Color.RED);
						g.setFont(tagFont);
					}

					g.drawString("Battery: "+batterypercentage+"%", 0, 15);

					/*// draw current state
					if (main.getDroneController().getSc() != null)
						g.drawString("State: " + main.getDroneController().getSc().state.toString(), 0, 40);
					else
						g.drawString("State: Waiting for AutoController...", 0, 40);

					// draw circle status
					g.drawString("Circles remaining: " + main.getDroneController().getPorts().size(), 0, 65);*/

        			// draw tolerance field (rectangle)
        			g.setColor(Color.RED);

    				int imgCenterX = MasterDrone.IMAGE_WIDTH * imgScale / 2;
    				int imgCenterY = MasterDrone.IMAGE_HEIGHT * imgScale / 2;
    				int tolerance = MasterDrone.TOLERANCE * imgScale;

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

        				g.drawPolygon(new int[] {(int)a.getX()*imgScale,(int)b.getX()*imgScale,(int)d.getX()*imgScale,(int)c.getX()*imgScale},
  						      new int[] {(int)a.getY()*imgScale,(int)b.getY()*imgScale,(int)d.getY()*imgScale,(int)c.getY()*imgScale}, 4);

        				g.setColor(Color.RED);
        				g.setFont(tagFont);
        				g.drawString(result.getText(), (int)a.getX()*imgScale, (int)a.getY()*imgScale);
        				g.drawString(orientation, (int)a.getX()*imgScale, (int)a.getY()*imgScale + 20);

        				if ((System.currentTimeMillis() - result.getTimestamp()) > 1000)
        				{
        					result = null;
        				}
        			}

        			// Draw circles
					if (circles != null)
						for(Circle c : circles){
							g.setColor(Color.RED);
							g.drawRect((int)c.x*imgScale, (int)c.y*imgScale, 10, 10);
							g.setColor(Color.BLUE);
							g.drawOval((int)(c.x - c.r)*imgScale, (int) (c.y - c.r)*imgScale, (int)(2*c.r)*imgScale, (int)(2*c.r)*imgScale);
						}
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
        videoPanel.setSize(MasterDrone.IMAGE_WIDTH * imgScale, MasterDrone.IMAGE_HEIGHT * imgScale);
        videoPanel.setMinimumSize(new Dimension(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT));
        videoPanel.setPreferredSize(new Dimension(MasterDrone.IMAGE_WIDTH * imgScale, MasterDrone.IMAGE_HEIGHT * imgScale));
        videoPanel.setMaximumSize(new Dimension(MasterDrone.IMAGE_WIDTH, MasterDrone.IMAGE_HEIGHT));
        
        return videoPanel;
	}
	
	private long imageCount = 0;

	private void batteryListener() {
		drone.getNavDataManager().addBatteryListener(new BatteryListener() {

			public void batteryLevelChanged(int percentage)
			{
				batterypercentage = percentage;
			}

			@Override
			public void voltageChanged(int vbat_raw) {
			}
		});
	}

	public void imageUpdated(BufferedImage newImage) {
	/*	if ((++imageCount % 1) == 0)
			return;*/
		
    	image = newImage;
		SwingUtilities.invokeLater(new Runnable() {
			public void run()
			{
				videoPanel.repaint();
			}
		});
    }

	@Override
	public void onTag(Result result, float v) {
		if (result != null) {
			this.result = result;
			this.orientation = v + " deg";
		}
	}

	@Override
	public void circlesUpdated(Circle[] circles) {
		this.circles = circles;		
	}
}