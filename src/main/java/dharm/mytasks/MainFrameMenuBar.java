package dharm.mytasks;

import java.awt.EventQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import dharm.mytasks.component.Record;
import dharm.mytasks.uihelper.UIBuilder;

public class MainFrameMenuBar extends JMenuBar {
	private static final long serialVersionUID = -720083909641215821L;
	private JFrame frame;

	public MainFrameMenuBar(JFrame frame){
		this.frame = frame;
		
		this.add(createConfigMenu());
		this.add(createHelpMenu());
	}
	
	private JMenu createConfigMenu(){
		JMenu menu = new JMenu("Configure");
		menu.setMnemonic('C');

		JMenuItem configLookAndFeelMenuItem = new JMenuItem("Look And Feel", 'L');
		configLookAndFeelMenuItem.addActionListener(evt->EventQueue.invokeLater(this::showConfigLookAndFeelDialog));
		menu.add(configLookAndFeelMenuItem);

		JMenuItem configPropMenuItem = new JMenuItem("Properties", 'P');
		configPropMenuItem.addActionListener(evt->EventQueue.invokeLater(this::showConfigPropDialog));
		menu.add(configPropMenuItem);

/*
		JMenuItem exitMenuItem = new JMenuItem("Exit", 'x');
		exitMenuItem.addActionListener(evt->frame.exit);
		menu.add(exitMenuItem);
*/
		return menu;
	}

	private JMenu createHelpMenu(){
		JMenu menu = new JMenu("Help");
		menu.setMnemonic('H');
		
		JMenuItem aboutMenuItem = new JMenuItem("About", 'A');
		aboutMenuItem.addActionListener(evt->{
			JOptionPane.showMessageDialog(frame, "Author: Dharmendrasinh P Chudasama");
		});
		menu.add(aboutMenuItem);
		
		return menu;
	}
	
	//================================================================
	
	private void showConfigPropDialog() {
		Map<String, String> datamap = AppConfig.getDatamap();
		HashMap<String, JTextField> fileds = new HashMap<>();

		UIBuilder builder = new UIBuilder();
		datamap.forEach((k,v)->{
			String label = k;
			JTextField component = new JTextField(v);
			fileds.put(label, component);
			builder.addPair(0, label, component);
		});
		builder.showDialog(frame, "Property Configuration", dialog->{
			fileds.forEach((l,f)->datamap.put(l,f.getText()));
			return true;
		});
	}

	private void showConfigLookAndFeelDialog() {
		String currLookAndFeelClassName = AppConfig.LOOK_AND_FEEL_CLASS.get(null);
		Record<String> currRec = null;

		Vector<Record<String>> records = new Vector<>();
		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			String className = info.getClassName();
			Record<String> rec = new Record<>(info.getName(), className);
			if(currRec==null && Objects.equals(className, currLookAndFeelClassName))
				currRec = rec;
			records.add(rec);
		}
		
		JComboBox<Record<String>> combo = new JComboBox<>(records);
		if (currRec != null)
			combo.setSelectedItem(currRec);
		
		new UIBuilder()
		.addPair(0, "Look and feel", combo)
		.showDialog(frame, "Configure Look & Feel", dialog->{
			@SuppressWarnings("unchecked")
			Record<String> selectedRec = (Record<String>) combo.getSelectedItem();
			if(selectedRec != null){
				AppConfig.LOOK_AND_FEEL_CLASS.set(selectedRec.getValue());
				AppConfig.saveData();
				JOptionPane.showMessageDialog(dialog, "Changes will be updated on restart.");
			}
			return true;
		});
	}

}
