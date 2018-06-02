package sembscope;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.fazecast.jSerialComm.SerialPort;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.SwingConstants;
import javax.swing.JToggleButton;

public class Graph {

	static SerialPort chosenPort;
	static int x = 0;
	static int nrLevels = 256;
	static float fullScale = (float) 5.0;
	static int nrSamples = 1000;
	static int nrDiv = 10;
	static final int TRIGGER_MIN = 0;
	static final int TRIGGER_MAX = 50;
	static final int TRIGGER_INIT = 25;    //initial value of slider
	static float triggerValue = (float)2.5;

	public static void main(String[] args) {
		// create and configure the window
		JFrame window = new JFrame();
		window.setTitle("SEMBscope");
		window.setBounds(350, 150, 600, 450);
		window.getContentPane().setLayout(new BorderLayout());
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// create drop-down list and button
		JComboBox<String> portList = new JComboBox<String>();
		JButton connect = new JButton("Connect");
		JPanel top = new JPanel();
		top.add(portList);
		top.add(connect);
		window.getContentPane().add(top, BorderLayout.NORTH);


		// populate drop-down list
		SerialPort[] portNames = SerialPort.getCommPorts();
		for(int i = 0; i < portNames.length; i++) {
			portList.addItem(portNames[i].getSystemPortName());
		}

		//create trigger slider and panel
		JPanel east = new JPanel();
		window.getContentPane().add(east, BorderLayout.EAST);
		GridBagLayout gbl_east = new GridBagLayout();
		gbl_east.columnWidths = new int[]{60, 0, 0};
		gbl_east.rowHeights = new int[]{50, 50, 99, 50, 118, 0, 0};
		gbl_east.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_east.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		east.setLayout(gbl_east);
		JSlider trigger = new JSlider(JSlider.VERTICAL, TRIGGER_MIN, TRIGGER_MAX, TRIGGER_INIT);
		trigger.setMinorTickSpacing(1); 
		trigger.setMajorTickSpacing(2); 
		trigger.setPaintTicks(true);
		trigger.setPaintLabels(true);
		GridBagConstraints gbc_trigger = new GridBagConstraints();
		gbc_trigger.gridheight = 4;
		gbc_trigger.insets = new Insets(0, 0, 5, 5);
		gbc_trigger.fill = GridBagConstraints.BOTH;
		gbc_trigger.gridx = 0;
		gbc_trigger.gridy = 1;
		east.add(trigger, gbc_trigger);
		trigger.setBackground(new Color(238, 238, 238));
		trigger.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				triggerValue = (float)trigger.getValue()/10;
				//System.out.println(triggerValue);
			}
		});
		Hashtable<Integer, JLabel> triggerTable = new Hashtable<Integer, JLabel>();
		triggerTable.put( new Integer( 0 ), new JLabel("0.0") );
		triggerTable.put( new Integer( 5 ), new JLabel("0.5") );
		triggerTable.put( new Integer( 10 ), new JLabel("1.0") );
		triggerTable.put( new Integer( 15 ), new JLabel("1.5") );
		triggerTable.put( new Integer( 20 ), new JLabel("2.0") );
		triggerTable.put( new Integer( 25 ), new JLabel("2.5") );
		triggerTable.put( new Integer( 30 ), new JLabel("3.0") );
		triggerTable.put( new Integer( 35 ), new JLabel("3.5") );
		triggerTable.put( new Integer( 40 ), new JLabel("4.0") );
		triggerTable.put( new Integer( 45 ), new JLabel("4.5") );
		triggerTable.put( new Integer( 50 ), new JLabel("5.0") );
		trigger.setLabelTable( triggerTable );

		//create trigger label
		JLabel triggerLabel = new JLabel("Trigger");
		triggerLabel.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_triggerLabel = new GridBagConstraints();
		gbc_triggerLabel.insets = new Insets(0, 0, 5, 5);
		gbc_triggerLabel.fill = GridBagConstraints.BOTH;
		gbc_triggerLabel.gridx = 0;
		gbc_triggerLabel.gridy = 0;
		east.add(triggerLabel, gbc_triggerLabel);
		triggerLabel.setLabelFor(trigger);
		
		/*JToggleButton toggleTrigger = new JToggleButton("ASC/DESC");
		GridBagConstraints gbc_toggleTrigger = new GridBagConstraints();
		gbc_toggleTrigger.insets = new Insets(0, 0, 5, 0);
		gbc_toggleTrigger.gridx = 1;
		gbc_toggleTrigger.gridy = 2;
		east.add(toggleTrigger, gbc_toggleTrigger);
		toggleTrigger.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg1) {
		});*/


		//create the line graph
		XYSeries channel1 = new XYSeries("Channel 1", true, false);
		XYSeriesCollection data = new XYSeriesCollection(channel1);
		JFreeChart chart = ChartFactory.createXYLineChart("Oscilloscope", "", "", data);
		window.getContentPane().add(new ChartPanel(chart), BorderLayout.CENTER);
		XYPlot xyPlot = (XYPlot) chart.getPlot();
		NumberAxis domain = (NumberAxis) xyPlot.getDomainAxis();
		domain.setRange(0.00, (float)nrDiv);
		domain.setTickUnit(new NumberTickUnit(1.0));
		NumberAxis range = (NumberAxis) xyPlot.getRangeAxis();
		range.setRange(0.0, 5.0);
		range.setTickUnit(new NumberTickUnit(1.0));

		//configure connect button
		//use other thread to listen for data
		connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(connect.getText().equals("Connect")) {
					channel1.clear();

					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					chosenPort.setBaudRate(115200);
					if(chosenPort.openPort()) {
						System.out.println(chosenPort.getBaudRate());
						connect.setText("Disconnect");
						portList.setEnabled(false);
					}
					Thread thread = new Thread() {
						@Override public void run() {

							Scanner scanner = new Scanner(chosenPort.getInputStream());
							float current = (float) 0.0;
							float next = (float) 0.0;
							String line = null;
							Integer number = 0;

							if(scanner.hasNextLine()) {
								line = scanner.nextLine();
								current = (float)(number * fullScale / nrLevels);
							}


							while(scanner.hasNextLine()) {
								try {
									line = scanner.nextLine();
									number = Integer.parseInt(line);
									next = (float) number * fullScale / nrLevels;
									System.out.println(current);
									System.out.println(triggerValue);
									if(current > triggerValue*0.98 && current < triggerValue*1.02 && next > current) {
										System.out.println("FEZ TRIGGER");
										x = 0;
									}

									channel1.addOrUpdate(x*0.9, current);
									x++;

									if(x > nrSamples) { 
										x = 0;
									}

									current = next;
								} catch(Exception e) {

								}
							}
							scanner.close();
						}
					};
					thread.start();
				} else {
					chosenPort.closePort();
					portList.setEnabled(true);
					connect.setText("Connect");
					x = 0;
				}
			}
		});

		window.setVisible(true);
	}
}
