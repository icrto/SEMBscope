package sembscope;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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


public class Graph {
	static final byte CHANNEL1 = 0;
	static final byte CHANNEL2 = 1;
	static final byte BOTH = 2;
	static final byte NONE = 3;
	static final byte ASC = 4;
	static final byte DESC = 5;

	static SerialPort chosenPort;
	static int x = 0;
	static int nrLevels = 256; //ADC 8 bit resolution
	static float fullScale = (float) 5.0;
	static int nrSamples = 512;
	static int toggleCount = 0;
	static int index = 0;
	static int triggerIndex = 0;
	static int selectedTriggerMode = ASC;
	static boolean triggerFlag = true;
	static int[] bufferChannel1 = new int[nrSamples];
	static int[] bufferChannel2 = new int[nrSamples];
	static int selectedChannel = CHANNEL1;
	static int selectedTriggerChannel = CHANNEL1;

	static int nrDiv = 10;
	static final int TRIGGER_MIN = 0;
	static final int TRIGGER_MAX = 80;
	static final int TRIGGER_INIT = 40;    //initial value of slider
	static float triggerValue = (float)4.0;

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

	public static void main(String[] args) {
		// create and configure the window
		JFrame window = new JFrame();
		window.setTitle("SEMBscope");
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
		gbl_east.columnWidths = new int[]{0, 0, 0, 0, 11, 60, 18, 0};
		gbl_east.rowHeights = new int[]{32, 24, 41, 20, 37, 12, 36, 30, 35, 29, 0, 14, 38, 0, 0};
		gbl_east.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_east.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		east.setLayout(gbl_east);
		
		Component horizontalStrut_1 = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_1 = new GridBagConstraints();
		gbc_horizontalStrut_1.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_1.gridx = 0;
		gbc_horizontalStrut_1.gridy = 0;
		east.add(horizontalStrut_1, gbc_horizontalStrut_1);

		Component verticalStrut = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut = new GridBagConstraints();
		gbc_verticalStrut.gridheight = 14;
		gbc_verticalStrut.insets = new Insets(0, 0, 0, 5);
		gbc_verticalStrut.gridx = 4;
		gbc_verticalStrut.gridy = 0;
		east.add(verticalStrut, gbc_verticalStrut);

		//create trigger panel
		JPanel triggerPanel = new JPanel();
		GridBagConstraints gbc_triggerPanel = new GridBagConstraints();
		gbc_triggerPanel.insets = new Insets(0, 0, 0, 5);
		gbc_triggerPanel.gridheight = 14;
		gbc_triggerPanel.fill = GridBagConstraints.BOTH;
		gbc_triggerPanel.gridx = 5;
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
		JSlider trigger = new JSlider(JSlider.VERTICAL, TRIGGER_MIN, TRIGGER_MAX, TRIGGER_INIT);
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
				triggerValue = (float)trigger.getValue()/10;
				//System.out.println(triggerValue);
			}
		});
		
		triggerLabel.setLabelFor(trigger);

		JToggleButton tglbtnAsc = new JToggleButton("ASC");
		tglbtnAsc.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JToggleButton aux = (JToggleButton) e.getSource();
				if(aux.isSelected()) {
					//TODO button is selected --> change trigger to dsc
					aux.setText("DSC");
				}
				else {
					//TODO button is unselected --> change trigger to asc
					aux.setText("ASC");
				}
			}
		});
		tglbtnAsc.setFocusable(false);
		GridBagConstraints gbc_tglbtnAsc = new GridBagConstraints();
		gbc_tglbtnAsc.gridx = 0;
		gbc_tglbtnAsc.gridy = 8;
		triggerPanel.add(tglbtnAsc, gbc_tglbtnAsc);

		Component horizontalStrut = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut = new GridBagConstraints();
		gbc_horizontalStrut.insets = new Insets(0, 0, 5, 0);
		gbc_horizontalStrut.gridx = 6;
		gbc_horizontalStrut.gridy = 1;
		east.add(horizontalStrut, gbc_horizontalStrut);

		JLabel lblVerticalAxis = new JLabel("Vertical axis");
		GridBagConstraints gbc_lblVerticalAxis = new GridBagConstraints();
		gbc_lblVerticalAxis.fill = GridBagConstraints.VERTICAL;
		gbc_lblVerticalAxis.gridwidth = 2;
		gbc_lblVerticalAxis.insets = new Insets(0, 0, 5, 5);
		gbc_lblVerticalAxis.gridx = 1;
		gbc_lblVerticalAxis.gridy = 2;
		east.add(lblVerticalAxis, gbc_lblVerticalAxis);

		JButton buttonMinusVertical = new JButton("-");
		buttonMinusVertical.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//TODO
				//reduce the vertical scale
				//min is between 0 and 5 V
			}
		});
		
		GridBagConstraints gbc_buttonMinusVertical = new GridBagConstraints();
		gbc_buttonMinusVertical.insets = new Insets(0, 0, 5, 5);
		gbc_buttonMinusVertical.gridx = 1;
		gbc_buttonMinusVertical.gridy = 3;
		east.add(buttonMinusVertical, gbc_buttonMinusVertical);

		JButton buttonPlusVertical = new JButton("+");
		buttonPlusVertical.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO
				//increase vertical scale
				//check max with JOptionPane?
			}
		});
		buttonPlusVertical.setFocusable(false);
		GridBagConstraints gbc_buttonPlusVertical = new GridBagConstraints();
		gbc_buttonPlusVertical.insets = new Insets(0, 0, 5, 5);
		gbc_buttonPlusVertical.gridx = 2;
		gbc_buttonPlusVertical.gridy = 3;
		east.add(buttonPlusVertical, gbc_buttonPlusVertical);

		JLabel lblVoltsdiv = new JLabel("Volts/div");
		// TODO lblVoltsdiv.setText(value + " Volts/div");
		//TODO remove next line
		lblVoltsdiv.setVisible(false);
		GridBagConstraints gbc_lblVoltsdiv = new GridBagConstraints();
		gbc_lblVoltsdiv.gridwidth = 2;
		gbc_lblVoltsdiv.insets = new Insets(0, 0, 5, 5);
		gbc_lblVoltsdiv.gridx = 1;
		gbc_lblVoltsdiv.gridy = 4;
		east.add(lblVoltsdiv, gbc_lblVoltsdiv);

		Component verticalGlue = Box.createVerticalGlue();
		GridBagConstraints gbc_verticalGlue = new GridBagConstraints();
		gbc_verticalGlue.fill = GridBagConstraints.VERTICAL;
		gbc_verticalGlue.insets = new Insets(0, 0, 5, 5);
		gbc_verticalGlue.gridx = 1;
		gbc_verticalGlue.gridy = 5;
		east.add(verticalGlue, gbc_verticalGlue);

		JLabel lblHorizontalAxis = new JLabel("Horizontal axis");
		GridBagConstraints gbc_lblHorizontalAxis = new GridBagConstraints();
		gbc_lblHorizontalAxis.fill = GridBagConstraints.VERTICAL;
		gbc_lblHorizontalAxis.gridwidth = 2;
		gbc_lblHorizontalAxis.insets = new Insets(0, 0, 5, 5);
		gbc_lblHorizontalAxis.gridx = 1;
		gbc_lblHorizontalAxis.gridy = 6;
		east.add(lblHorizontalAxis, gbc_lblHorizontalAxis);

		JButton buttonMinusHorizontal = new JButton("-");
		buttonMinusHorizontal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO reduce the time scale
				//minimum is what 1024 * 13 u ?
			}
		});
		buttonMinusHorizontal.setFocusable(false);
		GridBagConstraints gbc_buttonMinusHorizontal = new GridBagConstraints();
		gbc_buttonMinusHorizontal.insets = new Insets(0, 0, 5, 5);
		gbc_buttonMinusHorizontal.gridx = 1;
		gbc_buttonMinusHorizontal.gridy = 7;
		east.add(buttonMinusHorizontal, gbc_buttonMinusHorizontal);

		JButton buttonPlusHorizontal = new JButton("+");
		buttonPlusHorizontal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO increase the time scale
				//maximum 15u per division ? cause the adc
			}
		});
		buttonPlusHorizontal.setFocusable(false);
		GridBagConstraints gbc_buttonPlusHorizontal = new GridBagConstraints();
		gbc_buttonPlusHorizontal.insets = new Insets(0, 0, 5, 5);
		gbc_buttonPlusHorizontal.gridx = 2;
		gbc_buttonPlusHorizontal.gridy = 7;
		east.add(buttonPlusHorizontal, gbc_buttonPlusHorizontal);

		JLabel lblUsdiv = new JLabel();
		// TODO lblUsdiv.setText(value + "us/div");
		// TODO remove next line
		lblUsdiv.setVisible(false);
		GridBagConstraints gbc_lblUsdiv = new GridBagConstraints();
		gbc_lblUsdiv.gridwidth = 2;
		gbc_lblUsdiv.insets = new Insets(0, 0, 5, 5);
		gbc_lblUsdiv.gridx = 1;
		gbc_lblUsdiv.gridy = 8;
		east.add(lblUsdiv, gbc_lblUsdiv);

		Component verticalGlue_1 = Box.createVerticalGlue();
		GridBagConstraints gbc_verticalGlue_1 = new GridBagConstraints();
		gbc_verticalGlue_1.insets = new Insets(0, 0, 5, 5);
		gbc_verticalGlue_1.gridx = 1;
		gbc_verticalGlue_1.gridy = 9;
		east.add(verticalGlue_1, gbc_verticalGlue_1);

		JToggleButton tglbtnHrm = new JToggleButton("HRM");
		tglbtnHrm.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				//TODO check whats the button state
				/*
				 * if (both mode) JoptionPane with error
				 * else high resolution mode on selected channel
				 * 
				 */
			}
		});
		tglbtnHrm.setFocusable(false);
		GridBagConstraints gbc_tglbtnHrm = new GridBagConstraints();
		gbc_tglbtnHrm.gridwidth = 2;
		gbc_tglbtnHrm.insets = new Insets(0, 0, 5, 5);
		gbc_tglbtnHrm.gridx = 1;
		gbc_tglbtnHrm.gridy = 10;
		east.add(tglbtnHrm, gbc_tglbtnHrm);

		Component verticalGlue_2 = Box.createVerticalGlue();
		GridBagConstraints gbc_verticalGlue_2 = new GridBagConstraints();
		gbc_verticalGlue_2.insets = new Insets(0, 0, 5, 5);
		gbc_verticalGlue_2.gridx = 1;
		gbc_verticalGlue_2.gridy = 11;
		east.add(verticalGlue_2, gbc_verticalGlue_2);

		JLabel lblChannelSelection = new JLabel("Channel Selection");
		GridBagConstraints gbc_lblChannelSelection = new GridBagConstraints();
		gbc_lblChannelSelection.gridwidth = 2;
		gbc_lblChannelSelection.insets = new Insets(0, 0, 5, 5);
		gbc_lblChannelSelection.gridx = 1;
		gbc_lblChannelSelection.gridy = 12;
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
		gbc_tglbtnCH1.gridy = 13;
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
		gbc_tglbtnCH2.gridy = 13;
		east.add(tglbtnCH2, gbc_tglbtnCH2);



		//create the line graph
		channel1 = new XYSeries("Channel 1", true, false);
		channel2 = new XYSeries("Channel 2", true, false);

		data1 = new XYSeriesCollection();
		data2 = new XYSeriesCollection();


		chart = ChartFactory.createXYLineChart("Oscilloscope", "", "", data1);

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
		range.setRange(0.0, 8.0);
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
		gbl_south.columnWidths = new int[]{0, 114, 40, 80, 60, 80, 60, 80, 61, 80, 0, 0, 0, 0, 0};
		gbl_south.rowHeights = new int[]{11, 0, 35, 27, 0, 0};
		gbl_south.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_south.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
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

		JLabel lblFrequency = new JLabel("Frequency");
		GridBagConstraints gbc_lblFrequency = new GridBagConstraints();
		gbc_lblFrequency.insets = new Insets(0, 0, 5, 5);
		gbc_lblFrequency.gridx = 3;
		gbc_lblFrequency.gridy = 1;
		south.add(lblFrequency, gbc_lblFrequency);

		Component horizontalGlue_2 = Box.createHorizontalGlue();
		GridBagConstraints gbc_horizontalGlue_2 = new GridBagConstraints();
		gbc_horizontalGlue_2.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalGlue_2.gridx = 4;
		gbc_horizontalGlue_2.gridy = 1;
		south.add(horizontalGlue_2, gbc_horizontalGlue_2);

		JLabel lblMin = new JLabel("Min");
		GridBagConstraints gbc_lblMin = new GridBagConstraints();
		gbc_lblMin.insets = new Insets(0, 0, 5, 5);
		gbc_lblMin.gridx = 5;
		gbc_lblMin.gridy = 1;
		south.add(lblMin, gbc_lblMin);

		Component horizontalGlue = Box.createHorizontalGlue();
		GridBagConstraints gbc_horizontalGlue = new GridBagConstraints();
		gbc_horizontalGlue.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalGlue.gridx = 6;
		gbc_horizontalGlue.gridy = 1;
		south.add(horizontalGlue, gbc_horizontalGlue);

		JLabel lblMax = new JLabel("Max");
		GridBagConstraints gbc_lblMax = new GridBagConstraints();
		gbc_lblMax.insets = new Insets(0, 0, 5, 5);
		gbc_lblMax.gridx = 7;
		gbc_lblMax.gridy = 1;
		south.add(lblMax, gbc_lblMax);

		Component horizontalGlue_1 = Box.createHorizontalGlue();
		GridBagConstraints gbc_horizontalGlue_1 = new GridBagConstraints();
		gbc_horizontalGlue_1.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalGlue_1.gridx = 8;
		gbc_horizontalGlue_1.gridy = 1;
		south.add(horizontalGlue_1, gbc_horizontalGlue_1);

		JLabel lblAvg = new JLabel("AVG");
		GridBagConstraints gbc_lblAvg = new GridBagConstraints();
		gbc_lblAvg.insets = new Insets(0, 0, 5, 5);
		gbc_lblAvg.gridx = 9;
		gbc_lblAvg.gridy = 1;
		south.add(lblAvg, gbc_lblAvg);

		JLabel lblCH1 = new JLabel("CH1:");
		GridBagConstraints gbc_lblCH1 = new GridBagConstraints();
		gbc_lblCH1.insets = new Insets(0, 0, 5, 5);
		gbc_lblCH1.gridx = 1;
		gbc_lblCH1.gridy = 2;
		south.add(lblCH1, gbc_lblCH1);

		JLabel labelFreqCH1 = new JLabel("?");
		/* if( channel 1 is off)
		 * 		label.setText("off");
		 * else if( channel 1 not triggered)
		 * 			label.setText("?");
		 * else
		 * 		label.setText(frequencyCH1); 
		 */
		GridBagConstraints gbc_labelFreqCH1 = new GridBagConstraints();
		gbc_labelFreqCH1.insets = new Insets(0, 0, 5, 5);
		gbc_labelFreqCH1.gridx = 3;
		gbc_labelFreqCH1.gridy = 2;
		south.add(labelFreqCH1, gbc_labelFreqCH1);

		JLabel labelMinCH1 = new JLabel("?");
		/* if( channel 1 is off)
		 * 		label.setText("off");
		 * else if( channel 1 not triggered)
		 * 			label.setText("?");
		 * else
		 * 		label.setText(minCH1); 
		 */
		GridBagConstraints gbc_labelMinCH1 = new GridBagConstraints();
		gbc_labelMinCH1.insets = new Insets(0, 0, 5, 5);
		gbc_labelMinCH1.gridx = 5;
		gbc_labelMinCH1.gridy = 2;
		south.add(labelMinCH1, gbc_labelMinCH1);

		JLabel labelMaxCH1 = new JLabel("?");
		/* if( channel 1 is off)
		 * 		label.setText("off");
		 * else if( channel 1 not triggered)
		 * 			label.setText("?");
		 * else
		 * 		label.setText(maxCH1); 
		 */
		GridBagConstraints gbc_labelMaxCH1 = new GridBagConstraints();
		gbc_labelMaxCH1.insets = new Insets(0, 0, 5, 5);
		gbc_labelMaxCH1.gridx = 7;
		gbc_labelMaxCH1.gridy = 2;
		south.add(labelMaxCH1, gbc_labelMaxCH1);

		JLabel labelAVGCH1 = new JLabel("?");
		/* if( channel 1 is off)
		 * 		label.setText("off");
		 * else if( channel 1 not triggered)
		 * 			label.setText("?");
		 * else
		 * 		label.setText(avgCH1); 
		 */
		GridBagConstraints gbc_labelAVGCH1 = new GridBagConstraints();
		gbc_labelAVGCH1.insets = new Insets(0, 0, 5, 5);
		gbc_labelAVGCH1.gridx = 9;
		gbc_labelAVGCH1.gridy = 2;
		south.add(labelAVGCH1, gbc_labelAVGCH1);

		JLabel lblCH2 = new JLabel("CH2:");
		GridBagConstraints gbc_lblCH2 = new GridBagConstraints();
		gbc_lblCH2.insets = new Insets(0, 0, 5, 5);
		gbc_lblCH2.gridx = 1;
		gbc_lblCH2.gridy = 3;
		south.add(lblCH2, gbc_lblCH2);

		JLabel labelFreqCH2 = new JLabel("?");
		/* if( channel 2 is off)
		 * 		label.setText("off");
		 * else if( channel 2 not triggered)
		 * 			label.setText("?");
		 * else
		 * 		label.setText(frequencyCH2); 
		 */
		GridBagConstraints gbc_labelFreqCH2 = new GridBagConstraints();
		gbc_labelFreqCH2.insets = new Insets(0, 0, 5, 5);
		gbc_labelFreqCH2.gridx = 3;
		gbc_labelFreqCH2.gridy = 3;
		south.add(labelFreqCH2, gbc_labelFreqCH2);

		JLabel labelMinCH2 = new JLabel("?");
		/* if( channel 2 is off)
		 * 		label.setText("off");
		 * else if( channel 2 not triggered)
		 * 			label.setText("?");
		 * else
		 * 		label.setText(minCH2); 
		 */
		GridBagConstraints gbc_labelMinCH2 = new GridBagConstraints();
		gbc_labelMinCH2.insets = new Insets(0, 0, 5, 5);
		gbc_labelMinCH2.gridx = 5;
		gbc_labelMinCH2.gridy = 3;
		south.add(labelMinCH2, gbc_labelMinCH2);

		JLabel labelMaxCH2 = new JLabel("?");
		/* if( channel 2 is off)
		 * 		label.setText("off");
		 * else if( channel 2 not triggered)
		 * 			label.setText("?");
		 * else
		 * 		label.setText(maxCH2); 
		 */
		GridBagConstraints gbc_labelMaxCH2 = new GridBagConstraints();
		gbc_labelMaxCH2.insets = new Insets(0, 0, 5, 5);
		gbc_labelMaxCH2.gridx = 7;
		gbc_labelMaxCH2.gridy = 3;
		south.add(labelMaxCH2, gbc_labelMaxCH2);

		JLabel labelAVGCH2 = new JLabel("?");
		/* if( channel 2 is off)
		 * 		label.setText("off");
		 * else if( channel 2 not triggered)
		 * 			label.setText("?");
		 * else
		 * 		label.setText(avgCH2); 
		 */
		GridBagConstraints gbc_labelAVGCH2 = new GridBagConstraints();
		gbc_labelAVGCH2.insets = new Insets(0, 0, 5, 5);
		gbc_labelAVGCH2.gridx = 9;
		gbc_labelAVGCH2.gridy = 3;
		south.add(labelAVGCH2, gbc_labelAVGCH2);

		Component verticalStrut_2 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_2 = new GridBagConstraints();
		gbc_verticalStrut_2.insets = new Insets(0, 0, 0, 5);
		gbc_verticalStrut_2.gridx = 1;
		gbc_verticalStrut_2.gridy = 4;
		south.add(verticalStrut_2, gbc_verticalStrut_2);


		//configure connect button
		//use other thread to listen for data
		connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(connect.getText().equals("Connect")) {

					tglbtnCH1.setSelected(true);
					tglbtnCH2.setSelected(false);
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					chosenPort.setBaudRate(230400);
					if(chosenPort.openPort()) {
						System.out.println(chosenPort.getBaudRate());
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
										bufferChannel1[(index++) % nrSamples] = next;
										trigger(current, next, index, selectedTriggerMode);
										if(index == nrSamples) {	// buffer is full, restart
											triggerFlag = true;
											index = 0;
											drawChannel(CHANNEL1, triggerIndex);
										}
										current = next;
										break;
									case CHANNEL2:
										bufferChannel2[(index++) % nrSamples] = next;
										trigger(current, next, index, selectedTriggerMode);
										if(index == nrSamples) {	// buffer is full, restart
											triggerFlag = true;
											index = 0;
											drawChannel(CHANNEL2, triggerIndex);
										}
										current = next;
										break;
									case BOTH: 
										if(state == 0) {
											index = 0;
											if(next == 256 || next == 257) { //flag
												//System.out.println(next);
												receiving  = next;
												state = 1;
											}
										}
										else if(state == 1) {
											if(receiving == 256) {
												bufferChannel1[(index++) % (nrSamples)] = next;
												if(selectedTriggerChannel == CHANNEL1) {
													trigger(current, next, index, selectedTriggerMode);
												}
											}
											else if(receiving == 257){
												bufferChannel2[(index++) % (nrSamples)] = next;
												if(selectedTriggerChannel == CHANNEL2) {
													trigger(current, next, index, selectedTriggerMode);
												}
											}

											if(index == nrSamples - 1) {
												state = 0;
												triggerFlag = true;
												if(toggleCount == 1) {
													toggleCount = 0;
													drawChannel(BOTH, triggerIndex); //draws 1 wave at a time
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
				}
			}
		});

		window.setVisible(true);
	}
	static void checkChannelInput() {
		if(tglbtnCH1.isSelected() && tglbtnCH2.isSelected()) {
			selectedChannel = BOTH;
			state = 0;
		}
		else if(tglbtnCH1.isSelected()){
			selectedChannel = CHANNEL1;
		}
		else if(tglbtnCH2.isSelected()) {
			selectedChannel = CHANNEL2;
		}
		else {
			selectedChannel = NONE;
			channel1.clear();
			channel2.clear();
			return;
		}
		try {
			System.out.println(selectedChannel);
			chosenPort.getOutputStream().write(selectedChannel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		channel1.clear();
		channel2.clear();
	}
	static void drawChannel(int channelID, int triggerIndex) {
		//channelID 0 -> channel1 / 1 -> channel2 / 2 -> both channels
		int x = 0;
		for(int i = triggerIndex; i <= triggerIndex + nrSamples/2; i++) {
			switch(channelID) {
			case CHANNEL1: channel1.addOrUpdate((x++) * (nrDiv * 1) / (nrSamples/2.0), bufferChannel1[i]  * fullScale / nrLevels); break;
			case CHANNEL2: channel2.addOrUpdate((x++) * (nrDiv * 1) / (nrSamples/2.0), bufferChannel2[i] * fullScale / nrLevels); break;
			case BOTH: channel1.addOrUpdate((x) * (nrDiv * 1) / (nrSamples/2.0), bufferChannel1[i]  * fullScale / nrLevels); 
			channel2.addOrUpdate((x++) * (nrDiv * 1) / (nrSamples/2.0), bufferChannel2[i] * fullScale / nrLevels); 
			break;
			}
		}
	}
	static void trigger(int current, int next, int index, int mode) {
		switch(mode) {
		case ASC:
			//System.out.println("Tentou triggar");
			if(triggerFlag && current > (int)(triggerValue * nrLevels / fullScale) - 2 && current < (int)(triggerValue * nrLevels / fullScale) + 2 &&  next > current) {
				triggerIndex = index - 1;
				triggerFlag = false;
				//System.out.println("Trigou");
			}
			break;
		case DESC:
			if(triggerFlag && current > (int)(triggerValue * nrLevels / fullScale) - 2 && current < (int)(triggerValue * nrLevels / fullScale) + 2 &&  next < current) {
				triggerIndex = index - 1;
				triggerFlag = false;
			}
			break;
		}
		
	}
}
