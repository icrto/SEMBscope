package sembscope;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


import com.fazecast.jSerialComm.SerialPort;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


public class Graph {
	static final byte CHANNEL1 = 0;
	static final byte CHANNEL2 = 1;
	static final byte BOTH = 2;
	static final byte NONE = 3;
	static final byte ASC = 4;
	static final byte DSC = 5;
	static final byte STOP = 6;
	static final byte START = 7;
	static final byte HRM  = 8;
	static final byte NORMAL = 9;
	static final double verticalMAX = 4.0;
	static final double verticalMIN = 0.25;
	static final int horizontalMAX = 15;
	static final int horizontalMIN = 1;
	static int tickPeriod = 100;
	static int nrTicks = 3;
	static int periodPerSample = tickPeriod * nrTicks; //in us 


	static SerialPort chosenPort;
	static int x = 0;
	static int nrLevels = 256; //ADC 8 bit resolution
	static float fullScale = (float) 5.0;
	static int nrSamples = 512;
	static int nrSamplesToDraw = 166;
	static int toggleCount = 0;
	static int index = 0;
	static int triggerIndex = 0;
	static int selectedTriggerMode = ASC;
	static int run = START;
	static boolean triggerFlag = true;
	static int[] bufferChannel1 = new int[nrSamples];
	static int[] bufferChannel2 = new int[nrSamples];
	static int[] bufferChannel1HRM = new int[nrSamples*3];
	static int[] bufferChannel2HRM = new int[nrSamples*3];
	static int selectedChannel = CHANNEL1;
	static int selectedTriggerChannel = CHANNEL1;

	static int nrDiv = 10;
	static int Amplitude = 8;
	static double voltPerDivCH1 = 1.0;
	static double voltPerDivCH2 = 1.0;
	static int secondsPerDiv = 5;
	static double factorVertical = 2.0;
	static double factorHoriontal = 2.0;
	static final int TRIGGER_MIN = 0;
	static final int TRIGGER_MAX = 80;
	static final int TRIGGER_INIT = 40;    //initial value of slider
	static float triggerValue = (float)4.0;
	static final int channel1Min = 0;
	static final int channel1Max = 80;
	static float ch1PosValue = (float) 4.0;
	static int channel2Min = 0;
	static int channel2Max = 80;
	static float ch2PosValue = (float) 4.0;

	static int valueChannel;

	static XYSeries channel1;
	static XYSeries channel2;

	static XYSeriesCollection data1;
	static XYSeriesCollection data2;


	static JFreeChart chart;

	static int current = 0;
	static int next = 0; 
	static int receiving = -1; //256 -> channel 1 / 257 -> channel 2
	static int state = 0;

	static JToggleButton tglbtnCH1;
	static JToggleButton tglbtnCH2;
	static JToggleButton tglbtnHrm;

	static JSlider sliderCH1Pos;
	static JSlider sliderCH2Pos;
	static JSlider trigger;
	static JLabel labelMinCH1;
	static JLabel labelMinCH2;
	static JLabel labelMaxCH1;
	static JLabel labelMaxCH2;
	static JLabel labelAVGCH1;
	static JLabel labelAVGCH2;




	public static void main(String[] args) {
		// create and configure the window
		JFrame window = new JFrame();
		window.setTitle("SEMBScope");
		window.setBounds(150, 50, 1000, 685);
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
		portList.setSelectedIndex(2);
		//create trigger slider and panel
		JPanel east = new JPanel();
		window.getContentPane().add(east, BorderLayout.EAST);
		GridBagLayout gbl_east = new GridBagLayout();
		gbl_east.columnWidths = new int[]{0, 0, 0, 0, 11, 60, 18, 0, 0};
		gbl_east.rowHeights = new int[]{32, 24, 41, 20, 37, 12, 36, 30, 35, 29, 0, 14, 38, 0, 0};
		gbl_east.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_east.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		east.setLayout(gbl_east);

		Component horizontalStrut_1 = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_1 = new GridBagConstraints();
		gbc_horizontalStrut_1.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_1.gridx = 0;
		gbc_horizontalStrut_1.gridy = 0;
		east.add(horizontalStrut_1, gbc_horizontalStrut_1);

		JLabel lblCH1Resolution = new JLabel("Volt/div CH1");
		GridBagConstraints gbc_lblCH1Resolution = new GridBagConstraints();
		gbc_lblCH1Resolution.fill = GridBagConstraints.VERTICAL;
		gbc_lblCH1Resolution.gridwidth = 2;
		gbc_lblCH1Resolution.insets = new Insets(0, 0, 5, 5);
		gbc_lblCH1Resolution.gridx = 1;
		gbc_lblCH1Resolution.gridy = 0;
		east.add(lblCH1Resolution, gbc_lblCH1Resolution);

		JPanel panelScaleCH1 = new JPanel();
		GridBagConstraints gbc_panelScaleCH1 = new GridBagConstraints();
		gbc_panelScaleCH1.gridheight = 15;
		gbc_panelScaleCH1.insets = new Insets(0, 0, 5, 5);
		gbc_panelScaleCH1.fill = GridBagConstraints.BOTH;
		gbc_panelScaleCH1.gridx = 4;
		gbc_panelScaleCH1.gridy = 0;
		east.add(panelScaleCH1, gbc_panelScaleCH1);
		GridBagLayout gbl_panelScaleCH1 = new GridBagLayout();
		gbl_panelScaleCH1.columnWidths = new int[]{0, 0};
		gbl_panelScaleCH1.rowHeights = new int[]{20, 0, 0, 0, 0, 0, 0, 0};
		gbl_panelScaleCH1.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panelScaleCH1.rowWeights = new double[]{0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panelScaleCH1.setLayout(gbl_panelScaleCH1);

		JLabel lblPositionCH1 = new JLabel("POS CH1");
		GridBagConstraints gbc_lblPositionCH1 = new GridBagConstraints();
		gbc_lblPositionCH1.insets = new Insets(0, 0, 5, 0);
		gbc_lblPositionCH1.gridx = 0;
		gbc_lblPositionCH1.gridy = 0;
		panelScaleCH1.add(lblPositionCH1, gbc_lblPositionCH1);

		sliderCH1Pos = new JSlider(JSlider.VERTICAL, channel1Min, channel1Max, (int) channel1Max/2);
		sliderCH1Pos.setMinorTickSpacing(5);
		sliderCH1Pos.setMajorTickSpacing(10);
		sliderCH1Pos.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				ch1PosValue = (float)(sliderCH1Pos.getValue()/10.0);
				trigger(selectedTriggerChannel, current, next, index, selectedTriggerMode);
			}
		});
		GridBagConstraints gbc_sliderCH1Pos = new GridBagConstraints();
		gbc_sliderCH1Pos.fill = GridBagConstraints.VERTICAL;
		gbc_sliderCH1Pos.gridheight = 6;
		gbc_sliderCH1Pos.insets = new Insets(0, 0, 5, 0);
		gbc_sliderCH1Pos.gridx = 0;
		gbc_sliderCH1Pos.gridy = 1;
		panelScaleCH1.add(sliderCH1Pos, gbc_sliderCH1Pos);
		sliderCH1Pos.setMajorTickSpacing(10);
		sliderCH1Pos.setMinorTickSpacing(5);

		JPanel panelScaleCH2 = new JPanel();
		GridBagConstraints gbc_panelScaleCH2 = new GridBagConstraints();
		gbc_panelScaleCH2.gridheight = 15;
		gbc_panelScaleCH2.insets = new Insets(0, 0, 5, 5);
		gbc_panelScaleCH2.fill = GridBagConstraints.BOTH;
		gbc_panelScaleCH2.gridx = 5;
		gbc_panelScaleCH2.gridy = 0;
		east.add(panelScaleCH2, gbc_panelScaleCH2);
		GridBagLayout gbl_panelScaleCH2 = new GridBagLayout();
		gbl_panelScaleCH2.columnWidths = new int[]{0, 0};
		gbl_panelScaleCH2.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		gbl_panelScaleCH2.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panelScaleCH2.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panelScaleCH2.setLayout(gbl_panelScaleCH2);

		JLabel lblPositionCH2 = new JLabel("POS CH2");
		GridBagConstraints gbc_lblPositionCH2 = new GridBagConstraints();
		gbc_lblPositionCH2.insets = new Insets(0, 0, 5, 0);
		gbc_lblPositionCH2.gridx = 0;
		gbc_lblPositionCH2.gridy = 0;
		panelScaleCH2.add(lblPositionCH2, gbc_lblPositionCH2);

		sliderCH2Pos = new JSlider(JSlider.VERTICAL, channel2Min, channel2Max, (int) channel2Max/2);
		sliderCH2Pos.setMinorTickSpacing(5);
		sliderCH2Pos.setMajorTickSpacing(10);
		sliderCH2Pos.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				ch2PosValue = (float)(sliderCH2Pos.getValue()/10.0);
				trigger(selectedTriggerChannel, current, next, index, selectedTriggerMode);
			}
		});
		GridBagConstraints gbc_sliderCH2Pos = new GridBagConstraints();
		gbc_sliderCH2Pos.fill = GridBagConstraints.VERTICAL;
		gbc_sliderCH2Pos.gridheight = 6;
		gbc_sliderCH2Pos.insets = new Insets(0, 0, 5, 0);
		gbc_sliderCH2Pos.gridx = 0;
		gbc_sliderCH2Pos.gridy = 1;
		panelScaleCH2.add(sliderCH2Pos, gbc_sliderCH2Pos);

		//create trigger panel
		JPanel triggerPanel = new JPanel();
		GridBagConstraints gbc_triggerPanel = new GridBagConstraints();
		gbc_triggerPanel.insets = new Insets(0, 0, 5, 5);
		gbc_triggerPanel.gridheight = 15;
		gbc_triggerPanel.fill = GridBagConstraints.BOTH;
		gbc_triggerPanel.gridx = 6;
		gbc_triggerPanel.gridy = 0;
		east.add(triggerPanel, gbc_triggerPanel);
		GridBagLayout gbl_triggerPanel = new GridBagLayout();
		gbl_triggerPanel.columnWidths = new int[]{0, 0};
		gbl_triggerPanel.rowHeights = new int[]{0, 33, 48, 44, 28, 55, 0, 0, 0, 0};
		gbl_triggerPanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_triggerPanel.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		triggerPanel.setLayout(gbl_triggerPanel);

		//create trigger label
		JLabel triggerLabel = new JLabel("Trigger");
		GridBagConstraints gbc_triggerLabel = new GridBagConstraints();
		gbc_triggerLabel.insets = new Insets(0, 0, 5, 0);
		gbc_triggerLabel.gridx = 0;
		gbc_triggerLabel.gridy = 0;
		triggerPanel.add(triggerLabel, gbc_triggerLabel);
		triggerLabel.setHorizontalAlignment(SwingConstants.CENTER);
		trigger = new JSlider(JSlider.VERTICAL, TRIGGER_MIN, TRIGGER_MAX, TRIGGER_INIT);
		GridBagConstraints gbc_trigger = new GridBagConstraints();
		gbc_trigger.fill = GridBagConstraints.VERTICAL;
		gbc_trigger.gridheight = 7;
		gbc_trigger.insets = new Insets(0, 0, 5, 0);
		gbc_trigger.gridx = 0;
		gbc_trigger.gridy = 1;
		triggerPanel.add(trigger, gbc_trigger);
		trigger.setMinorTickSpacing(5); 
		trigger.setMajorTickSpacing(10); 
		trigger.setPaintTicks(false);
		trigger.setPaintLabels(false);
		trigger.setBackground(new Color(238, 238, 238));
		trigger.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				if(selectedTriggerChannel == CHANNEL1) valueChannel = sliderCH1Pos.getValue()/10;
				else valueChannel = sliderCH2Pos.getValue()/10;
				triggerValue = (float) (trigger.getValue()/10.0-((trigger.getMaximum()/10.0)/2)+((channel1Max/10.0)/2-valueChannel));
				trigger(selectedTriggerChannel, current, next, index, selectedTriggerMode);
			}
		});

		JLabel lblVoltsDivCH1 = new JLabel();
		lblVoltsDivCH1.setText(voltPerDivCH1 + " V/div");

		JButton buttonMinusCH1 = new JButton("-");
		buttonMinusCH1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFrame frame = (JFrame) SwingUtilities.getRoot((JButton)arg0.getSource());
				if(voltPerDivCH1 == verticalMAX) {
					JOptionPane.showMessageDialog(frame, "Volts/Div at limit", "Alert", JOptionPane.WARNING_MESSAGE);
				}
				else {
					voltPerDivCH1 *= factorVertical;
					lblVoltsDivCH1.setText(1.0/voltPerDivCH1 + " V/div");
					if(selectedTriggerChannel == CHANNEL1)
						trigger.setMaximum((int)((1.0/voltPerDivCH1)*Amplitude*10));
					else if(selectedTriggerChannel == CHANNEL2)
						trigger.setMaximum((int) ((1.0/voltPerDivCH2)*Amplitude*10));
				}
			}
		});
		GridBagConstraints gbc_buttonMinusCH1 = new GridBagConstraints();
		gbc_buttonMinusCH1.anchor = GridBagConstraints.NORTH;
		gbc_buttonMinusCH1.insets = new Insets(0, 0, 5, 5);
		gbc_buttonMinusCH1.gridx = 1;
		gbc_buttonMinusCH1.gridy = 1;
		east.add(buttonMinusCH1, gbc_buttonMinusCH1);



		JButton buttonPlusCH1 = new JButton("+");
		buttonPlusCH1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frame = (JFrame) SwingUtilities.getRoot((JButton)e.getSource());
				if(voltPerDivCH1 == verticalMIN) {
					JOptionPane.showMessageDialog(frame, "Volts/Div at limit", "Alert", JOptionPane.WARNING_MESSAGE);
				}
				else {
					voltPerDivCH1 /= factorVertical;
					lblVoltsDivCH1.setText(1.0/voltPerDivCH1 + " V/div");
					if(selectedTriggerChannel == CHANNEL1)
						trigger.setMaximum((int)((1.0/voltPerDivCH1)*Amplitude*10));
					else if(selectedTriggerChannel == CHANNEL2)
						trigger.setMaximum((int) ((1.0/voltPerDivCH2)*Amplitude*10));
				}
			}
		});
		buttonPlusCH1.setFocusable(false);
		GridBagConstraints gbc_buttonPlusCH1 = new GridBagConstraints();
		gbc_buttonPlusCH1.anchor = GridBagConstraints.NORTH;
		gbc_buttonPlusCH1.insets = new Insets(0, 0, 5, 5);
		gbc_buttonPlusCH1.gridx = 2;
		gbc_buttonPlusCH1.gridy = 1;
		east.add(buttonPlusCH1, gbc_buttonPlusCH1);
		GridBagConstraints gbc_lblVoltsDivCH1 = new GridBagConstraints();
		gbc_lblVoltsDivCH1.gridwidth = 2;
		gbc_lblVoltsDivCH1.insets = new Insets(0, 0, 5, 5);
		gbc_lblVoltsDivCH1.gridx = 1;
		gbc_lblVoltsDivCH1.gridy = 2;
		east.add(lblVoltsDivCH1, gbc_lblVoltsDivCH1);

		JLabel lblUsdiv = new JLabel("μs/div");

		lblUsdiv.setVisible(true);

		JLabel lblCH2Resolution = new JLabel();
		lblCH2Resolution.setText("Volt/div CH2");
		lblCH2Resolution.setVisible(true);

		GridBagConstraints gbc_lblCH2Resolution = new GridBagConstraints();
		gbc_lblCH2Resolution.gridwidth = 2;
		gbc_lblCH2Resolution.insets = new Insets(0, 0, 5, 5);
		gbc_lblCH2Resolution.gridx = 1;
		gbc_lblCH2Resolution.gridy = 4;
		east.add(lblCH2Resolution, gbc_lblCH2Resolution);

		JLabel lblVoltsDivCH2 = new JLabel();
		lblVoltsDivCH2.setText(voltPerDivCH2 + " V/div");
		GridBagConstraints gbc_lblVoltsDivCH2 = new GridBagConstraints();
		gbc_lblVoltsDivCH2.gridwidth = 2;
		gbc_lblVoltsDivCH2.insets = new Insets(0, 0, 5, 5);
		gbc_lblVoltsDivCH2.gridx = 1;
		gbc_lblVoltsDivCH2.gridy = 6;
		east.add(lblVoltsDivCH2, gbc_lblVoltsDivCH2);

		JButton buttonMinusCH2 = new JButton("-");
		buttonMinusCH2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frame = (JFrame) SwingUtilities.getRoot((JButton)e.getSource());
				if(voltPerDivCH2 == verticalMAX) {
					JOptionPane.showMessageDialog(frame, "Volts/Div at limit", "Alert", JOptionPane.WARNING_MESSAGE);
				}
				else {
					voltPerDivCH2 *= factorVertical;
					lblVoltsDivCH2.setText(1.0/voltPerDivCH2 + " V/div");
					if(selectedTriggerChannel == CHANNEL1)
						trigger.setMaximum((int)((1.0/voltPerDivCH1)*Amplitude*10));
					else if(selectedTriggerChannel == CHANNEL2)
						trigger.setMaximum((int) ((1.0/voltPerDivCH2)*Amplitude*10));
				}
			}
		});
		GridBagConstraints gbc_buttonMinusCH2 = new GridBagConstraints();
		gbc_buttonMinusCH2.insets = new Insets(0, 0, 5, 5);
		gbc_buttonMinusCH2.gridx = 1;
		gbc_buttonMinusCH2.gridy = 5;
		east.add(buttonMinusCH2, gbc_buttonMinusCH2);




		JButton buttonPlusCH2 = new JButton("+");
		buttonPlusCH2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frame = (JFrame) SwingUtilities.getRoot((JButton)e.getSource());
				if(voltPerDivCH2 == verticalMIN) {
					JOptionPane.showMessageDialog(frame, "Volts/Div at limit", "Alert", JOptionPane.WARNING_MESSAGE);
				}
				else {
					voltPerDivCH2 /= factorVertical;
					lblVoltsDivCH2.setText(1.0/voltPerDivCH2 + " V/div");
					if(selectedTriggerChannel == CHANNEL1)
						trigger.setMaximum((int)((1.0/voltPerDivCH1)*Amplitude*10));
					else if(selectedTriggerChannel == CHANNEL2)
						trigger.setMaximum((int) ((1.0/voltPerDivCH2)*Amplitude*10));
				}
			}
		});
		GridBagConstraints gbc_buttonPlusCH2 = new GridBagConstraints();
		gbc_buttonPlusCH2.insets = new Insets(0, 0, 5, 5);
		gbc_buttonPlusCH2.gridx = 2;
		gbc_buttonPlusCH2.gridy = 5;
		east.add(buttonPlusCH2, gbc_buttonPlusCH2);



		Component verticalStrut = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut = new GridBagConstraints();
		gbc_verticalStrut.insets = new Insets(0, 0, 5, 5);
		gbc_verticalStrut.gridx = 1;
		gbc_verticalStrut.gridy = 7;
		east.add(verticalStrut, gbc_verticalStrut);

		JLabel lblHorizontalAxis = new JLabel("Horizontal axis");
		GridBagConstraints gbc_lblHorizontalAxis = new GridBagConstraints();
		gbc_lblHorizontalAxis.fill = GridBagConstraints.VERTICAL;
		gbc_lblHorizontalAxis.gridwidth = 2;
		gbc_lblHorizontalAxis.insets = new Insets(0, 0, 5, 5);
		gbc_lblHorizontalAxis.gridx = 1;
		gbc_lblHorizontalAxis.gridy = 8;
		east.add(lblHorizontalAxis, gbc_lblHorizontalAxis);

		JButton buttonMinusHorizontal = new JButton("-");
		buttonMinusHorizontal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frame = (JFrame) SwingUtilities.getRoot((JButton)e.getSource());
				if(secondsPerDiv == 1) {
					JOptionPane.showMessageDialog(frame, "Seconds/Div at min", "Alert", JOptionPane.WARNING_MESSAGE);
				}
				else if(secondsPerDiv == 2) {
					secondsPerDiv = 1;
				}
				else if(secondsPerDiv == 5) {
					secondsPerDiv = 2;
				}
				else if(secondsPerDiv == 10) {
					secondsPerDiv = 5;
				}
				nrSamplesToDraw = (int)( (double)(secondsPerDiv * nrDiv) / (periodPerSample / 1000.0));
				lblUsdiv.setText(secondsPerDiv + " ms/div");
				channel1.clear();
				channel2.clear();
			}
		});
		buttonMinusHorizontal.setFocusable(false);
		GridBagConstraints gbc_buttonMinusHorizontal = new GridBagConstraints();
		gbc_buttonMinusHorizontal.insets = new Insets(0, 0, 5, 5);
		gbc_buttonMinusHorizontal.gridx = 1;
		gbc_buttonMinusHorizontal.gridy = 9;
		east.add(buttonMinusHorizontal, gbc_buttonMinusHorizontal);

		JButton buttonPlusHorizontal = new JButton("+");
		buttonPlusHorizontal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frame = (JFrame) SwingUtilities.getRoot((JButton)e.getSource());
				if(secondsPerDiv == 10) {
					JOptionPane.showMessageDialog(frame, "Seconds/Div at max", "Alert", JOptionPane.WARNING_MESSAGE);
				}
				else if(secondsPerDiv == 5) {
					secondsPerDiv = 10;
				}
				else if(secondsPerDiv == 2) {
					secondsPerDiv = 5;
				}
				else if(secondsPerDiv == 1) {
					secondsPerDiv = 2;
				}
				nrSamplesToDraw = (int)( (double)(secondsPerDiv * nrDiv) / (periodPerSample / 1000.0));
				lblUsdiv.setText(secondsPerDiv + " ms/div");
				channel1.clear();
				channel2.clear();
			}
		});
		buttonPlusHorizontal.setFocusable(false);
		GridBagConstraints gbc_buttonPlusHorizontal = new GridBagConstraints();
		gbc_buttonPlusHorizontal.insets = new Insets(0, 0, 5, 5);
		gbc_buttonPlusHorizontal.gridx = 2;
		gbc_buttonPlusHorizontal.gridy = 9;
		east.add(buttonPlusHorizontal, gbc_buttonPlusHorizontal);
		GridBagConstraints gbc_lblUsdiv = new GridBagConstraints();
		gbc_lblUsdiv.anchor = GridBagConstraints.NORTH;
		gbc_lblUsdiv.gridwidth = 2;
		gbc_lblUsdiv.insets = new Insets(0, 0, 5, 5);
		gbc_lblUsdiv.gridx = 1;
		gbc_lblUsdiv.gridy = 10;
		east.add(lblUsdiv, gbc_lblUsdiv);

		JLabel lblChannelSelection = new JLabel("Channel Selection");
		GridBagConstraints gbc_lblChannelSelection = new GridBagConstraints();
		gbc_lblChannelSelection.gridwidth = 2;
		gbc_lblChannelSelection.insets = new Insets(0, 0, 5, 5);
		gbc_lblChannelSelection.gridx = 1;
		gbc_lblChannelSelection.gridy = 13;
		east.add(lblChannelSelection, gbc_lblChannelSelection);

		tglbtnCH1 = new JToggleButton("CH1");
		tglbtnCH1.setSelected(true);
		tglbtnCH1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				checkChannelInput();
			}
		});
		tglbtnCH1.setFocusable(false);
		GridBagConstraints gbc_tglbtnCH1 = new GridBagConstraints();
		gbc_tglbtnCH1.insets = new Insets(0, 0, 0, 5);
		gbc_tglbtnCH1.gridx = 1;
		gbc_tglbtnCH1.gridy = 14;
		east.add(tglbtnCH1, gbc_tglbtnCH1);

		tglbtnCH2 = new JToggleButton("CH2");
		tglbtnCH2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				checkChannelInput();
			}
		});
		tglbtnCH2.setFocusable(false);
		GridBagConstraints gbc_tglbtnCH2 = new GridBagConstraints();
		gbc_tglbtnCH2.insets = new Insets(0, 0, 0, 5);
		gbc_tglbtnCH2.gridx = 2;
		gbc_tglbtnCH2.gridy = 14;
		east.add(tglbtnCH2, gbc_tglbtnCH2);

		//create the line graph
		channel1 = new XYSeries("Channel 1", true, false);
		channel2 = new XYSeries("Channel 2", true, false);

		data1 = new XYSeriesCollection();
		data2 = new XYSeriesCollection();


		chart = ChartFactory.createXYLineChart("SEMBScope", "", "", data1);

		data1.addSeries(channel1);
		data2.addSeries(channel2);

		chart.getXYPlot().setDataset(0, data1);
		chart.getXYPlot().setDataset(1, data2);

		XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer(); 
		XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(); 
		chart.getXYPlot().setRenderer(0, renderer0); 
		renderer0.setBaseShapesVisible(false);
		chart.getXYPlot().setRenderer(1, renderer1);
		renderer1.setBaseShapesVisible(false);
		chart.getXYPlot().getRendererForDataset(chart.getXYPlot().getDataset(0)).setSeriesPaint(0, Color.red); 	
		chart.getXYPlot().getRendererForDataset(chart.getXYPlot().getDataset(1)).setSeriesPaint(0, Color.blue); 	


		window.getContentPane().add(new ChartPanel(chart), BorderLayout.CENTER);
		XYPlot xyPlot = (XYPlot) chart.getPlot();
		NumberAxis domain = (NumberAxis) xyPlot.getDomainAxis();
		domain.setRange(0.00, (float)nrDiv);
		domain.setTickUnit(new NumberTickUnit(1.0));
		domain.setVisible(false);
		NumberAxis range = (NumberAxis) xyPlot.getRangeAxis();
		range.setRange(0.0, (float)Amplitude);
		range.setTickUnit(new NumberTickUnit(1.0));
		range.setVisible(false);


		final Marker horizontalLine = new ValueMarker(4.0, Color.BLACK, new BasicStroke(
				0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 
				10.0f, new float[] {10.0f, 10.0f}, 1.0f
				));
		final Marker verticalLine = new ValueMarker(5.0, Color.BLACK, new BasicStroke(
				0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 
				10.0f, new float[] {10.0f, 10.0f}, 1.0f
				));
		xyPlot.addRangeMarker(horizontalLine);
		xyPlot.addDomainMarker(verticalLine);

		JPanel south = new JPanel();
		south.setBounds(new Rectangle(0, 0, 0, 31));
		window.getContentPane().add(south, BorderLayout.SOUTH);
		GridBagLayout gbl_south = new GridBagLayout();
		gbl_south.columnWidths = new int[]{0, 114, 40, 60, 80, 60, 80, 61, 80, 62, 120, 0, 0, 0};
		gbl_south.rowHeights = new int[]{11, 0, 35, 27, 28, 0, 0};
		gbl_south.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_south.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		south.setLayout(gbl_south);

		Component horizontalStrut_2 = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_2 = new GridBagConstraints();
		gbc_horizontalStrut_2.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_2.gridx = 0;
		gbc_horizontalStrut_2.gridy = 0;
		south.add(horizontalStrut_2, gbc_horizontalStrut_2);

		Component verticalStrut_1 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_1 = new GridBagConstraints();
		gbc_verticalStrut_1.insets = new Insets(0, 0, 5, 5);
		gbc_verticalStrut_1.gridx = 1;
		gbc_verticalStrut_1.gridy = 0;
		south.add(verticalStrut_1, gbc_verticalStrut_1);

		Component horizontalGlue_2 = Box.createHorizontalGlue();
		GridBagConstraints gbc_horizontalGlue_2 = new GridBagConstraints();
		gbc_horizontalGlue_2.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalGlue_2.gridx = 3;
		gbc_horizontalGlue_2.gridy = 1;
		south.add(horizontalGlue_2, gbc_horizontalGlue_2);

		JLabel lblMin = new JLabel("Min");
		GridBagConstraints gbc_lblMin = new GridBagConstraints();
		gbc_lblMin.insets = new Insets(0, 0, 5, 5);
		gbc_lblMin.gridx = 4;
		gbc_lblMin.gridy = 1;
		south.add(lblMin, gbc_lblMin);

		Component horizontalGlue = Box.createHorizontalGlue();
		GridBagConstraints gbc_horizontalGlue = new GridBagConstraints();
		gbc_horizontalGlue.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalGlue.gridx = 5;
		gbc_horizontalGlue.gridy = 1;
		south.add(horizontalGlue, gbc_horizontalGlue);

		JLabel lblMax = new JLabel("Max");
		GridBagConstraints gbc_lblMax = new GridBagConstraints();
		gbc_lblMax.insets = new Insets(0, 0, 5, 5);
		gbc_lblMax.gridx = 6;
		gbc_lblMax.gridy = 1;
		south.add(lblMax, gbc_lblMax);

		Component horizontalGlue_1 = Box.createHorizontalGlue();
		GridBagConstraints gbc_horizontalGlue_1 = new GridBagConstraints();
		gbc_horizontalGlue_1.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalGlue_1.gridx = 7;
		gbc_horizontalGlue_1.gridy = 1;
		south.add(horizontalGlue_1, gbc_horizontalGlue_1);

		JLabel lblAvg = new JLabel("AVG");
		GridBagConstraints gbc_lblAvg = new GridBagConstraints();
		gbc_lblAvg.insets = new Insets(0, 0, 5, 5);
		gbc_lblAvg.gridx = 8;
		gbc_lblAvg.gridy = 1;
		south.add(lblAvg, gbc_lblAvg);

		JLabel lblTriggerSource = new JLabel("Trigger source");
		GridBagConstraints gbc_lblTriggerSource = new GridBagConstraints();
		gbc_lblTriggerSource.insets = new Insets(0, 0, 5, 5);
		gbc_lblTriggerSource.gridx = 11;
		gbc_lblTriggerSource.gridy = 1;
		south.add(lblTriggerSource, gbc_lblTriggerSource);

		JLabel lblCH1 = new JLabel("CH1:");
		GridBagConstraints gbc_lblCH1 = new GridBagConstraints();
		gbc_lblCH1.insets = new Insets(0, 0, 5, 5);
		gbc_lblCH1.gridx = 1;
		gbc_lblCH1.gridy = 2;
		south.add(lblCH1, gbc_lblCH1);

		labelMinCH1 = new JLabel("?");
		GridBagConstraints gbc_labelMinCH1 = new GridBagConstraints();
		gbc_labelMinCH1.insets = new Insets(0, 0, 5, 5);
		gbc_labelMinCH1.gridx = 4;
		gbc_labelMinCH1.gridy = 2;
		south.add(labelMinCH1, gbc_labelMinCH1);

		labelMaxCH1 = new JLabel("?");
		GridBagConstraints gbc_labelMaxCH1 = new GridBagConstraints();
		gbc_labelMaxCH1.insets = new Insets(0, 0, 5, 5);
		gbc_labelMaxCH1.gridx = 6;
		gbc_labelMaxCH1.gridy = 2;
		south.add(labelMaxCH1, gbc_labelMaxCH1);

		labelAVGCH1 = new JLabel("?");
		GridBagConstraints gbc_labelAVGCH1 = new GridBagConstraints();
		gbc_labelAVGCH1.insets = new Insets(0, 0, 5, 5);
		gbc_labelAVGCH1.gridx = 8;
		gbc_labelAVGCH1.gridy = 2;
		south.add(labelAVGCH1, gbc_labelAVGCH1);

		Component horizontalStrut = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut = new GridBagConstraints();
		gbc_horizontalStrut.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut.gridx = 9;
		gbc_horizontalStrut.gridy = 2;
		south.add(horizontalStrut, gbc_horizontalStrut);

		tglbtnHrm = new JToggleButton("HRM");

		GridBagConstraints gbc_tglbtnHrm = new GridBagConstraints();
		gbc_tglbtnHrm.insets = new Insets(0, 0, 5, 5);
		gbc_tglbtnHrm.gridx = 10;
		gbc_tglbtnHrm.gridy = 2;
		south.add(tglbtnHrm, gbc_tglbtnHrm);

		tglbtnHrm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changetoHRM();
			}
		});
		tglbtnHrm.setFocusable(false);


		JRadioButton rdbtnCH1 = new JRadioButton("CH1");
		rdbtnCH1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				labelMinCH2.setText("?");
				labelMaxCH2.setText("?");
				labelAVGCH2.setText("?");
				
				selectedTriggerChannel = CHANNEL1;
				triggerFlag = true;
				trigger.setMaximum((int)((1.0/voltPerDivCH1)*Amplitude*10));
			}
		});
		rdbtnCH1.setFocusable(false);
		rdbtnCH1.setSelected(true);
		GridBagConstraints gbc_rdbtnCH1 = new GridBagConstraints();
		gbc_rdbtnCH1.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnCH1.gridx = 11;
		gbc_rdbtnCH1.gridy = 2;
		south.add(rdbtnCH1, gbc_rdbtnCH1);

		JLabel lblCH2 = new JLabel("CH2:");
		GridBagConstraints gbc_lblCH2 = new GridBagConstraints();
		gbc_lblCH2.insets = new Insets(0, 0, 5, 5);
		gbc_lblCH2.gridx = 1;
		gbc_lblCH2.gridy = 3;
		south.add(lblCH2, gbc_lblCH2);

		JToggleButton tglbtnRun = new JToggleButton("STOP");
		tglbtnRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(((JToggleButton) arg0.getSource()).isSelected())	{
					((JToggleButton) arg0.getSource()).setText("RUN");
					run = STOP;
				}
				else {
					((JToggleButton) arg0.getSource()).setText("STOP");
					run = START;
				}
			}
		});
		tglbtnRun.setFocusable(false);
		GridBagConstraints gbc_tglbtnRun = new GridBagConstraints();
		gbc_tglbtnRun.gridheight = 2;
		gbc_tglbtnRun.insets = new Insets(0, 0, 5, 5);
		gbc_tglbtnRun.gridx = 10;
		gbc_tglbtnRun.gridy = 3;
		south.add(tglbtnRun, gbc_tglbtnRun);

		JRadioButton rdbtnCH2 = new JRadioButton("CH2");
		rdbtnCH2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				labelMinCH1.setText("?");
				labelMaxCH1.setText("?");
				labelAVGCH1.setText("?");
				
				selectedTriggerChannel = CHANNEL2;
				triggerFlag = true;
				trigger.setMaximum((int) ((1.0/voltPerDivCH2)*Amplitude*10));
			}
		});
		rdbtnCH2.setFocusable(false);
		GridBagConstraints gbc_rdbtnCH2 = new GridBagConstraints();
		gbc_rdbtnCH2.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnCH2.gridx = 11;
		gbc_rdbtnCH2.gridy = 3;
		south.add(rdbtnCH2, gbc_rdbtnCH2);

		//Group the radio buttons.
		ButtonGroup triggerSource = new ButtonGroup();
		triggerSource.add(rdbtnCH1);
		triggerSource.add(rdbtnCH2);

		labelMinCH2 = new JLabel("?");
		GridBagConstraints gbc_labelMinCH2 = new GridBagConstraints();
		gbc_labelMinCH2.insets = new Insets(0, 0, 5, 5);
		gbc_labelMinCH2.gridx = 4;
		gbc_labelMinCH2.gridy = 3;
		south.add(labelMinCH2, gbc_labelMinCH2);

		labelMaxCH2 = new JLabel("?");
		GridBagConstraints gbc_labelMaxCH2 = new GridBagConstraints();
		gbc_labelMaxCH2.insets = new Insets(0, 0, 5, 5);
		gbc_labelMaxCH2.gridx = 6;
		gbc_labelMaxCH2.gridy = 3;
		south.add(labelMaxCH2, gbc_labelMaxCH2);

		labelAVGCH2 = new JLabel("?");
		GridBagConstraints gbc_labelAVGCH2 = new GridBagConstraints();
		gbc_labelAVGCH2.insets = new Insets(0, 0, 5, 5);
		gbc_labelAVGCH2.gridx = 8;
		gbc_labelAVGCH2.gridy = 3;
		south.add(labelAVGCH2, gbc_labelAVGCH2);



		Component verticalStrut_2 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_2 = new GridBagConstraints();
		gbc_verticalStrut_2.insets = new Insets(0, 0, 5, 5);
		gbc_verticalStrut_2.gridx = 1;
		gbc_verticalStrut_2.gridy = 4;
		south.add(verticalStrut_2, gbc_verticalStrut_2);

		JToggleButton tglbtnAsc = new JToggleButton("ASC");
		GridBagConstraints gbc_tglbtnAsc = new GridBagConstraints();
		gbc_tglbtnAsc.insets = new Insets(0, 0, 5, 5);
		gbc_tglbtnAsc.gridx = 11;
		gbc_tglbtnAsc.gridy = 4;
		south.add(tglbtnAsc, gbc_tglbtnAsc);
		tglbtnAsc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JToggleButton aux = (JToggleButton) e.getSource();
				if(aux.isSelected()) {
					//button is selected --> change trigger to dsc
					aux.setText("DSC");
					selectedTriggerMode = DSC;
					triggerFlag = true;
				}
				else {
					//button is unselected --> change trigger to asc
					aux.setText("ASC");
					selectedTriggerMode = ASC;
					triggerFlag = true;
				}
			}
		});
		tglbtnAsc.setFocusable(false);

		Component verticalStrut_3 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_3 = new GridBagConstraints();
		gbc_verticalStrut_3.insets = new Insets(0, 0, 0, 5);
		gbc_verticalStrut_3.gridx = 5;
		gbc_verticalStrut_3.gridy = 5;
		south.add(verticalStrut_3, gbc_verticalStrut_3);

		buttonMinusCH1.setEnabled(false);
		buttonPlusCH1.setEnabled(false);
		buttonMinusCH2.setEnabled(false);
		buttonPlusCH2.setEnabled(false);
		buttonMinusHorizontal.setEnabled(false);
		buttonPlusHorizontal.setEnabled(false);
		tglbtnAsc.setEnabled(false);
		tglbtnHrm.setEnabled(false);
		tglbtnRun.setEnabled(false);
		rdbtnCH1.setEnabled(false);
		rdbtnCH2.setEnabled(false);
		trigger.setEnabled(false);
		sliderCH1Pos.setEnabled(false);
		sliderCH2Pos.setEnabled(false);
		tglbtnCH1.setEnabled(false);
		tglbtnCH2.setEnabled(false);
		lblUsdiv.setText(secondsPerDiv + " ms/div");



		//configure connect button
		//use other thread to listen for data
		connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(connect.getText().equals("Connect")) {
					buttonMinusCH1.setEnabled(true);
					buttonPlusCH1.setEnabled(true);
					buttonMinusCH2.setEnabled(true);
					buttonPlusCH2.setEnabled(true);
					buttonMinusHorizontal.setEnabled(true);
					buttonPlusHorizontal.setEnabled(true);
					trigger.setValue(40);
					sliderCH1Pos.setValue((int)channel1Max/2);
					sliderCH2Pos.setValue((int)channel2Max/2);
					trigger.setEnabled(true);
					rdbtnCH1.setEnabled(true);
					rdbtnCH2.setEnabled(true);
					tglbtnAsc.setEnabled(true);
					tglbtnHrm.setEnabled(true);
					tglbtnRun.setEnabled(true);
					sliderCH1Pos.setEnabled(true);
					sliderCH2Pos.setEnabled(true);
					tglbtnCH1.setEnabled(true);
					tglbtnCH2.setEnabled(true);
					tglbtnCH1.setSelected(true);
					tglbtnCH2.setSelected(false);
					secondsPerDiv = 5;
					voltPerDivCH1 = 1;
					voltPerDivCH2 = 1;
					lblVoltsDivCH1.setText(voltPerDivCH1 + " V/div");
					lblVoltsDivCH2.setText(voltPerDivCH2 + " V/div");
					lblUsdiv.setText(secondsPerDiv + " ms/div");
					selectedTriggerChannel = CHANNEL1;
					rdbtnCH1.setSelected(true);
					rdbtnCH2.setSelected(false);
					selectedChannel = CHANNEL1;
					triggerFlag = true;
					triggerIndex = 0;
					selectedTriggerMode = ASC;
					nrSamplesToDraw = 166;
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					chosenPort.setBaudRate(230400);
					if(chosenPort.openPort()) {
						connect.setText("Disconnect");
						portList.setEnabled(false);
						checkChannelInput();

					}
					Thread thread = new Thread() {
						@Override public void run() {

							Scanner scanner = new Scanner(chosenPort.getInputStream());

							String line = null;


							try {
								if(scanner.hasNextLine()) {
									line = scanner.nextLine();
									current = Integer.parseInt(line);
									bufferChannel1[(index++) % nrSamples] = current;
								}
							}
							catch(Exception e1){

							}


							while(scanner.hasNextLine()) {
								try {
									line = scanner.nextLine();
									next = Integer.parseInt(line); 

									switch(selectedChannel) {
									case CHANNEL1:
										if(tglbtnHrm.isSelected()) {
											bufferChannel1HRM[(index++) % nrSamples] = next;

										}
										else {
											bufferChannel1[(index++) % nrSamples] = next;
										}
										if(selectedTriggerChannel == CHANNEL1) {
											trigger(CHANNEL1, current, next, index, selectedTriggerMode);
										}
										if(index == nrSamples) {	// buffer is full, restart
											triggerFlag = true;
											index = 0;
											if(run == START) drawChannel(CHANNEL1, triggerIndex);
										}
										current = next;
										break;
									case CHANNEL2:
										if(tglbtnHrm.isSelected()) {
											bufferChannel2HRM[(index++) % nrSamples] = next;

										}
										else {
											bufferChannel2[(index++) % nrSamples] = next;
										}
										if(selectedTriggerChannel == CHANNEL2) { 
											trigger(CHANNEL2, current, next, index, selectedTriggerMode);
										}
										if(index == nrSamples) {	// buffer is full, restart
											triggerFlag = true;
											index = 0;
											if(run == START) drawChannel(CHANNEL2, triggerIndex);
										}
										current = next;
										break;
									case BOTH: 
										if(state == 0) {
											index = 0;
											if(next == 256 || next == 257) { //flag
												receiving  = next;
												state = 1;
											}
										}
										else if(state == 1) {
											if(receiving == 256) {
												bufferChannel1[(index++) % (nrSamples)] = next;
												if(selectedTriggerChannel == CHANNEL1)
													trigger(CHANNEL1, current, next, index, selectedTriggerMode);
											}
											else if(receiving == 257){
												bufferChannel2[(index++) % (nrSamples)] = next;
												if(selectedTriggerChannel == CHANNEL2)
													trigger(CHANNEL2, current, next, index, selectedTriggerMode);
											}

											if(index == nrSamples - 1) {
												state = 0;
												triggerFlag = true;
												if(toggleCount == 1) {
													toggleCount = 0;
													if(run == START) drawChannel(BOTH, triggerIndex); //draws 1 wave at a time
													continue;
												}
												toggleCount ++;	
												//drawChannel(BOTH, triggerIndex); //draws both waves at once
											}
											current = next;
										}
										break;
									default:
										break;
									}
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
					tglbtnCH1.setSelected(false);
					tglbtnCH2.setSelected(false);
					channel1.clear();
					channel2.clear();
					x = 0;
					buttonMinusCH1.setEnabled(false);
					buttonPlusCH1.setEnabled(false);
					buttonMinusCH2.setEnabled(false);
					buttonPlusCH2.setEnabled(false);
					buttonMinusHorizontal.setEnabled(false);
					buttonPlusHorizontal.setEnabled(false);
					sliderCH1Pos.setEnabled(false);
					sliderCH2Pos.setEnabled(false);
					tglbtnAsc.setEnabled(false);
					tglbtnHrm.setEnabled(false);
					tglbtnRun.setEnabled(false);
					tglbtnCH1.setEnabled(false);
					tglbtnCH2.setEnabled(false);
					rdbtnCH1.setEnabled(false);
					rdbtnCH2.setEnabled(false);
					trigger.setEnabled(false);
				}
			}
		});

		window.setVisible(true);
	}
	static void checkChannelInput() {
		if(tglbtnCH1.isSelected() && tglbtnCH2.isSelected()) {
			tglbtnHrm.setEnabled(false);
			selectedChannel = BOTH;
			state = 0;
		}
		else if(tglbtnCH1.isSelected()){
			tglbtnHrm.setEnabled(true);
			selectedChannel = CHANNEL1;
		}
		else if(tglbtnCH2.isSelected()) {
			tglbtnHrm.setEnabled(true);
			selectedChannel = CHANNEL2;
		}
		else {
			tglbtnHrm.setEnabled(false);
			selectedChannel = NONE;
			channel1.clear();
			channel2.clear();
			return;
		}
		try {
			chosenPort.getOutputStream().write(selectedChannel);
		} catch (IOException e) {
			e.printStackTrace();
		}
		channel1.clear();
		channel2.clear();
		index = 0;
		triggerFlag = true;
	}
	static void changetoHRM() {
		try {
			if(tglbtnHrm.isSelected()) {
				if(tglbtnCH1.isSelected()) {
					tglbtnCH2.setEnabled(false);
				}
				else if(tglbtnCH2.isSelected()) {
					tglbtnCH1.setEnabled(false);
				}
				chosenPort.getOutputStream().write(HRM);
				nrSamples = nrSamples * 3;
				nrTicks = nrTicks / 3;
				periodPerSample = tickPeriod * nrTicks;
				nrSamplesToDraw = (int)( (double)(secondsPerDiv * nrDiv) / (periodPerSample / 1000.0));
			}
			else {
				tglbtnCH1.setEnabled(true);
				tglbtnCH2.setEnabled(true);
				nrSamples = nrSamples / 3;
				nrTicks = nrTicks * 3;

				periodPerSample = tickPeriod * nrTicks;
				nrSamplesToDraw = (int)( (double)(secondsPerDiv * nrDiv) / (periodPerSample / 1000.0));
				chosenPort.getOutputStream().write(NORMAL);
			}
			channel1.clear();
			channel2.clear();
			index = 0;
			triggerFlag = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	static void drawChannel(int channelID, int triggerIndex) {
		//channelID 0 -> channel1 / 1 -> channel2 / 2 -> both channels
		int x = 0;
		for(int i = triggerIndex; i <= triggerIndex + nrSamplesToDraw; i++) {
			switch(channelID) {
			case CHANNEL1:
				if(tglbtnHrm.isSelected()) {
					channel1.addOrUpdate((x++) * (float) nrDiv / (nrSamplesToDraw), (bufferChannel1HRM[i] * voltPerDivCH1 * fullScale / nrLevels) + ch1PosValue ); 
				}
				else {
					channel1.addOrUpdate((x++) * (float) nrDiv / (nrSamplesToDraw), (bufferChannel1[i] * voltPerDivCH1 * fullScale / nrLevels) + ch1PosValue ); 

				}
				break;
			case CHANNEL2:
				if(tglbtnHrm.isSelected()) {
					channel2.addOrUpdate((x++) * (float) nrDiv / (nrSamplesToDraw), (bufferChannel2HRM[i] * voltPerDivCH2 * fullScale / nrLevels) + ch2PosValue ); 
				}
				else {
					channel2.addOrUpdate((x++) * (float) nrDiv / (nrSamplesToDraw), (bufferChannel2[i] * voltPerDivCH2 * fullScale / nrLevels) + ch2PosValue ); 

				}
				break;
			case BOTH: 
				channel1.addOrUpdate((x) * (float) nrDiv / (nrSamplesToDraw), (bufferChannel1[i] * voltPerDivCH1 * fullScale / nrLevels + ch1PosValue)); 
				channel2.addOrUpdate((x++) * (float) nrDiv / (nrSamplesToDraw), (bufferChannel2[i] * voltPerDivCH2 * fullScale / nrLevels + ch2PosValue)); 
				break;
			}
		}
	}
	static void trigger(int channelID, int current, int next, int index, int mode) {
		switch(mode) {
		case ASC:
			if(triggerFlag && current > (int)((triggerValue * nrLevels / fullScale)) - 2 && current < (int)(triggerValue * nrLevels / fullScale) + 2 &&  next > current) {
				triggerIndex = index - 1;
				triggerFlag = false;
				min(channelID, triggerIndex);
				max(channelID, triggerIndex);
				avg(channelID, triggerIndex);
			}
			break;
		case DSC:

			if(triggerFlag && current > (int)(triggerValue * nrLevels / fullScale) - 2 && current < (int)(triggerValue * nrLevels / fullScale) + 2 &&  next < current) {
				triggerIndex = index - 1;
				triggerFlag = false;
				min(channelID, triggerIndex);
				max(channelID, triggerIndex);
				avg(channelID, triggerIndex);
			}
			else {
				labelMinCH1.setText("?");
				labelMaxCH1.setText("?");
				labelAVGCH1.setText("?");
				labelMinCH2.setText("?");
				labelMaxCH2.setText("?");
				labelAVGCH2.setText("?");
				}
			break;
		}
	}
	static void min(int channelID, int triggerIndex) {
		int min;
		if(channelID == CHANNEL1) {
			min = bufferChannel1[triggerIndex];
			for(int i = triggerIndex + 1; i <= triggerIndex + nrSamplesToDraw; i++) {
				if(bufferChannel1[i] < min) {
					min = bufferChannel1[i];
				}
			}
			labelMinCH1.setText(String.format("%.2f V", min * fullScale / nrLevels));
		}
		else if(channelID == CHANNEL2) {
			min = bufferChannel2[triggerIndex];
			for(int i = triggerIndex + 1; i <= triggerIndex + nrSamplesToDraw; i++) {
				if(bufferChannel2[i] < min) {
					min = bufferChannel2[i];
				}
			}			
			labelMinCH2.setText(String.format("%.2f V", min * fullScale / nrLevels));
		}
	}
	static void max(int channelID, int triggerIndex) {
		int max;
		if(channelID == CHANNEL1) {
			max = bufferChannel1[triggerIndex];
			for(int i = triggerIndex + 1; i <= triggerIndex + nrSamplesToDraw; i++) {
				if(bufferChannel1[i] > max) {
					max = bufferChannel1[i];
				}
			}
			labelMaxCH1.setText(String.format("%.2f V", max * fullScale / nrLevels));
		}
		else if(channelID == CHANNEL2) {
			max = bufferChannel2[triggerIndex];
			for(int i = triggerIndex + 1; i <= triggerIndex + nrSamplesToDraw; i++) {
				if(bufferChannel2[i] > max) {
					max = bufferChannel2[i];
				}
			}
			labelMaxCH2.setText(String.format("%.2f V", max * fullScale / nrLevels));
		}
	}
	static void avg(int channelID, int triggerIndex) {
		int sum = 0;
		if(channelID == CHANNEL1) {
			for(int i = triggerIndex; i <= triggerIndex + nrSamplesToDraw; i++) {
				sum += bufferChannel1[i];
			}
			labelAVGCH1.setText(String.format("%.2f V", ((float)sum / nrSamplesToDraw) * fullScale / nrLevels));
		}
		else if(channelID == CHANNEL2) {
			for(int i = triggerIndex; i <= triggerIndex + nrSamplesToDraw; i++) {
				sum += bufferChannel2[i];
			}
			labelAVGCH2.setText(String.format("%.2f V", ((float)sum / nrSamplesToDraw) * fullScale / nrLevels));
		}
	}

}
