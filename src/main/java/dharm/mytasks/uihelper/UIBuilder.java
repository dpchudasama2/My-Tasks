package dharm.mytasks.uihelper;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

/** Create panel with gridbag layout and handle accordingly like show form/show dialog etc*/
public class UIBuilder {
	private List<Object[]> dataList = new ArrayList<>(); // [info{0:line,1:center,2:pair}, label, component[]]
	private int maxCol = 1;
	private List<Integer> heightList = new ArrayList<>(); //0 means default
	
	private GridBagLayout layout = new GridBagLayout();
//	layout.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
//	layout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
/*	public UIBuilder(int[] columnWidths, int[] rowHeights){
		layout.columnWidths = columnWidths;
		layout.rowHeights = rowHeights;
	}
*/
	/** @see GridBagLayout#columnWidths */
	public UIBuilder(int...columnWidths){
		layout.columnWidths = columnWidths;
	}

	/** @see GridBagLayout#columnWidths */
	public UIBuilder setWidths(int...columnWidths){
		layout.columnWidths = columnWidths;
		return this;
	}

	
	/** @param height height for constrain, 0=default
	 * @param data [info{0:fillRow,1:center,2:pair} [,label,[component[]]]] */
	private UIBuilder addRec(int height, Object...data){
		heightList.add(height);
		dataList.add(data);
		return this;
	}

	/** @param text label text or html code for label
	 * @return current object
	 * @see #addCenter(JComponent...)
	 */
	public UIBuilder addCenter(int height, String text){
		return addCenter(height, new JLabel(text));
	}
	public UIBuilder addCenter(int height, JComponent... components){
		return addRow(height, groupPane(FlowLayout.CENTER, components));
/*		if(components.length==1)
			addRec(1, components[0]);
		else if (components.length > 1)
			addRec(1, groupPane(FlowLayout.CENTER, components));
		return this;*/
	}
	
	/** append with fill (no alignment) */
	public UIBuilder addRow(int height, JComponent component){
		return addRec(height, 0, component);
	}
	
	public UIBuilder addLine(){
		return addRow(0, new JSeparator());
	}
	
	/** Form data pair
	 * @param label form label
	 * @param components form control components
	 * @return current instance
	 */
	public UIBuilder addPair(int height, String label, JComponent... components){
		maxCol = Math.max(maxCol, components.length + 1);
		label = label + " :";
		return addRec(height, 2, label, components);
	}

	public JPanel buildPanel(){
//		GridBagLayout layout = new GridBagLayout();
//		layout.columnWidths = new int[]{0, 49, 64, 0};
		layout.rowHeights = heightList.stream().mapToInt(Integer::intValue).toArray();//new int[]{0, 0, 0, 0, 0};
//		layout.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
//		layout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		final JPanel panel = new JPanel(layout);
		panel.setBorder(new EmptyBorder(30, 30, 30, 30));

//		GridBagConstraints defaultConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		GridBagConstraints fillConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 0), 0, 0);
		GridBagConstraints centerConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0);

		GridBagConstraints formLabelConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 0.25, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0);
		GridBagConstraints formControlConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 0.75, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0);

		int rowNo = 0;
		for (Object[] data : dataList) {
			int col = 0;

			int info = (int) data[0]; //info{1:center,2:pair}
			switch (info) {
				case 0: { //append filled row
					JComponent component = (JComponent) data[1];
					add(panel, fillConstraints, col, rowNo, maxCol, component);
					break;
				}

				case 1: { //center: data for center
					JComponent component = (JComponent) data[1];
					add(panel, centerConstraints, col, rowNo, maxCol, component);
					break;
				}
				
				case 2: { //pair: label, component[]
					//label
					String label = (String) data[1];
					col = add(panel, formLabelConstraints, col, rowNo, 1, new JLabel(label));
					
					//components
					JComponent[] components = (JComponent[]) data[2];
					int len = components.length;
					if(len!=0){
						int width = (maxCol - 1/*remove label col*/) / len;
						for (JComponent comp : components)
							col = (comp==null) ? col+1 : add(panel, formControlConstraints, col, rowNo, width, comp);
					}
					break;
				}
			}
			rowNo++;
		}

		return panel;
	}
	
	/**
	 * @param panel panel where to add
	 * @param constraints
	 * @param x col (base=0)
	 * @param y rowNo (base=0)
	 * @param width def=1, colspan
	 * @param component component to add
	 * @return next col, param x for next call
	 */
	private int add(JPanel panel, GridBagConstraints constraints, int x, int y, int width, JComponent component){
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.gridwidth = width;
		panel.add(component, constraints);
		return x + width;
	}
	
	/** @return new panel with flow layout */
	public static JPanel groupPane(int flowLayoutAlign, JComponent...components){
		JPanel p = new JPanel(new FlowLayout(flowLayoutAlign));
		for (JComponent comp : components) p.add(comp);
		return p;
	}
	
	/**
	 * @param maxLength default=0
	 * @param keyValidator default=<code>null</code>
	 * @return created text field
	 */
	public static JTextField createTextField(int maxLength, Function<Character, Boolean> keyValidator){
		final JTextField textField = new JTextField();
		if(maxLength>0)
			invalidKeyBlocker(textField, c->textField.getText().length()<maxLength);
		if(keyValidator != null)
			invalidKeyBlocker(textField, keyValidator);
		return textField;
	}

	public static JTextField createTextField(final BiFunction<JTextField, Character, Boolean> validator){
		final JTextField textField = new JTextField();
		if(validator != null)
			invalidKeyBlocker(textField, c->validator.apply(textField, c));
		return textField;
	}
	
	public static void invalidKeyBlocker(final JTextComponent textComponent, final Function<Character, Boolean> keyValidator){
		textComponent.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				if(!keyValidator.apply(e.getKeyChar()))
					e.consume();
			}
		});
	}

	
	/** @return new button with action */
	public static JButton createButton(String text, ActionListener clickHandler){
		final JButton button = new JButton(text, null);
		if (clickHandler != null)
			button.addActionListener(clickHandler);
		return button;
	}
	
	public void showDialog(JFrame callerFrame, String title, Function<JDialog, Boolean> okClickHandler){
		showDialog(callerFrame, title, this, okClickHandler);
	}

	/**Show dialog box
	 * @param callerFrame may <code>null</code>
	 * @param title
	 * @param okClickHandler call on click ok button, returns <code>true</code>=operation performed(valid), hide/dispose dialog, <code>false</code>=may some err, so don't hide
	 */
	public static void showDialog(JFrame callerFrame, String title, UIBuilder builder, Function<JDialog, Boolean> okClickHandler){
		final JDialog dialog = new JDialog(callerFrame, title, true);
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		builder.addLine();
		
		//button pane
		{
			JButton okButton = createButton(
				"OK",
				e->{ if(okClickHandler.apply(dialog)) dialog.dispose(); }
			);
			dialog.getRootPane().setDefaultButton(okButton);
	
			builder.addRow(0, groupPane(FlowLayout.TRAILING,
				okButton,
				createButton("Cancel", e->dialog.dispose())
			));
		}
		
		JPanel panel = builder.buildPanel();
		panel.setBorder(new EmptyBorder(20, 30, 20, 30));
		dialog.setContentPane(panel);

		dialog.setResizable(false);
		dialog.pack();
		dialog.setLocationRelativeTo(callerFrame);

//		GuiUtil.setFonts(dialog);
		dialog.setVisible(true);
		dialog.requestFocus();
	}

	
	public void showFrame(String title, String submitBtnLabel, Consumer<JFrame> submitClickHandler) {
		showFrame(null, title, (frame, builder)->{
			JButton submitButton = createButton(submitBtnLabel, e->submitClickHandler.accept(frame));

			Dimension size = submitButton.getPreferredSize();
			double h = size.getHeight();
			if(h<40) submitButton.setPreferredSize(new Dimension(size.width, 40));
			
			frame.getRootPane().setDefaultButton(submitButton); //set default button
			builder.addCenter(0, submitButton);
//			builder.addRow(submitButton);
		}, frame->System.exit(0));
	}

	public FrameHandler showFrame(JFrame callerFrame, String title, BiConsumer<JFrame, UIBuilder> frameFinalShapper, Consumer<JFrame> closeHandler) {
		final JFrame frame = new JFrame(title);
//		frame.setIconImage(GuiUtil.getIconImage());
//		frame.setResizable(false);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setSize(487, 130);
		frame.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				if (closeHandler != null)
					closeHandler.accept(frame);
				else
					System.exit(0);
			}
		});

		if (frameFinalShapper != null) {
			this.addLine();
			frameFinalShapper.accept(frame, this);
		}

		final JPanel panel = this.buildPanel();
		panel.setBorder(new EmptyBorder(20, 40, 20, 40));
		frame.setContentPane(panel);

		frame.pack();
		frame.setLocationRelativeTo(callerFrame);
//		GuiUtil.setFonts(frame);
		frame.setVisible(true);
		frame.requestFocus();
		
		return new FrameHandler(frame);
	}
	
	public static class FrameHandler {
		private JFrame frame;
		private FrameHandler(JFrame frame){
			this.frame = frame;
		}
		
		public JFrame getFrame(){
			return frame;
		}
		
		public void show(){
			frame.setVisible(true);
		}
		
		public void setDefaultButton(JButton defaultButton){
			defaultButton.setPreferredSize(new Dimension(80, 35));
			getFrame().getRootPane().setDefaultButton(defaultButton); //set default button
		}
		
		public void showDialog(String title, UIBuilder builder, Function<JDialog, Boolean> okClickHandler){
			UIBuilder.showDialog(getFrame(), title, builder, okClickHandler);
		}
	}
	
	/** Demo */
	public static void main(String[] args) {
		JTextField firstName = UIBuilder.createTextField(15, null);
		JTextField lastName = UIBuilder.createTextField(15, null);

		new UIBuilder(5, 0, 0)
		.addPair(0, "Firstname", firstName)
		.addPair(0, "Lastname", lastName)
		.addPair(0, "Gender", new JRadioButton("Male"), new JRadioButton("Female"))
		.addPair(2, "Subjects", new JScrollPane(new JList<String>(new String[]{"Gujarati","Hindi","English","Sanskrit"})))
		.showFrame("Personal info", "Open dialog", frame->{
			new UIBuilder(10,10,10)
			.addPair(0, "Firstname", new JLabel(firstName.getText()))
			.addPair(0, "Lastname", new JLabel(lastName.getText()))
			.showDialog(frame, "Info", ok->true);
		});

/*
		new UIBuilder(5)
//			.addRow(titlePane)
			.addCenter("<html><body><h1><u>Hello</u></h1></body></html>")
			.addPair("Firstname", new JTextField("Enter your first name"))
			.addPair("Lastname", new JTextField("Enter your last name"))
			.addPair("Sex", new JRadioButton("Male", true), new JRadioButton("Female"), null, null)
			.addLine()
			.addPair("<html>Language<br/>knowledge :</html>", new JCheckBox("Gujarati"), new JCheckBox("Hindi"), new JCheckBox("English"), new JCheckBox("Sanskrit"))
			.addPair("Can", new JCheckBox("Read"), new JCheckBox("Write"), new JCheckBox("A"))
			.addPair("List", new JList<>(new String[]{"HTTP","HTTPS","SOCKS"}))
			.showFrame("Test", "Sign", frame->{
				System.err.println("Sign");
			});
*/
//		JOptionPane.showMessageDialog(null, panel);
/*		GuiUtil.showDialog(null, "Test", panel, dialog->{
			return false;
		});*/
//		JOptionPane.showMessageDialog(null, panel);
	}

}
