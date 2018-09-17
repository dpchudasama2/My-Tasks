package dharm.mytasks.service;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import dharm.mytasks.CommonConstant;
import dharm.mytasks.Project;
import dharm.mytasks.component.Record;
import dharm.mytasks.uihelper.UIBuilder;
import dharm.mytasks.util.DatabaseUtil;

public class ProjPanelHandler {
	
	private Map<String, Supplier<Object>> commonVarFetcher;
	private final CommandExecutor commandExecutor;

	public ProjPanelHandler(Map<String, Supplier<Object>> commonVarFetcher, CommandExecutor commandExecutor) {
		this.commonVarFetcher = commonVarFetcher;
		this.commandExecutor = commandExecutor;
	}

	public JPanel createProjPane(Project proj) {
//		UIBuilder builder = new UIBuilder();
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints(GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0);
		constraints.gridy++;
		
		String query =
				"SELECT"
				+ " CONCAT(CT.COMMANDPREFIX, C.COMMAND, CT.COMMANDSUFFIX) AS COMMAND,"
				+ " C.LABEL, C.DESC, CT.COMPONENTTYPEID"
				+ " FROM COMPONENT C INNER JOIN COMPONENTTYPE CT ON CT.COMPONENTTYPEID = C.COMPONENTTYPEID"
				+ " WHERE C.PROJECTID = ? ORDER BY DISPORDER";
		
		List<Map<String, Object>> list = DatabaseUtil.select(query, new Object[]{proj.getProjectId()}, DatabaseUtil::toMapList);
		System.out.println("ProjID="+proj.getProjectId());
		
		for (Map<String, Object> map : list) {
			int componentTypeID = (int)(long) map.get("COMPONENTTYPEID");
			switch (componentTypeID) {
			case 2: //new line
				constraints.gridy++;
				break;

			default:
				String label = (String) map.get("LABEL");
				String command = (String) map.get("COMMAND");
				String desc = (String) map.get("DESC");
				
				JButton btn = new JButton(label);
				if(command != null && !command.isEmpty()) {
					btn.setToolTipText(desc+" (\""+command+"\")");
					btn.addActionListener(ae->commandExecutor.execCommand(command));
				} else {
					btn.setToolTipText(desc);
				}
				
				panel.add(btn, constraints);
				break;
			}
		}
		
		panel.addMouseListener(this.createMouseListener(proj, panel));

		return panel;
	}

	private MouseListener createMouseListener(Project proj, JPanel panel) {
		MouseListener listener = new MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if(e.getButton() != MouseEvent.BUTTON3) return; //ignore if no right click
				
				JPopupMenu popup = new JPopupMenu();

				JMenuItem addCompMenuItem = new JMenuItem("Add Component", 'A');
				addCompMenuItem.addActionListener(actEvt->EventQueue.invokeLater(()->showAddComponentDialog(proj, "Add Component")));
				popup.add(addCompMenuItem);
				
				popup.show(panel, e.getX(), e.getY());
			};
		};

		return listener;
	}
	
	private void showAddComponentDialog(Project proj, String dialogTitle){
		JFrame mainFrame = (JFrame) commonVarFetcher.getOrDefault(CommonConstant.MAIN_FRAME, ()->null).get();

		String parentQuery = "SELECT COMPONENTTYPEID, TYPENAME, COMMANDPREFIX, COMMANDSUFFIX, DESC FROM COMPONENTTYPE";
		Map<String, Map<String, Object>> parentData = new LinkedHashMap<>();
		DatabaseUtil.select(parentQuery, null, DatabaseUtil::toMapList).forEach(map->parentData.put((String)map.get("TYPENAME"), map));

		JTextField txtLabel = UIBuilder.createTextField(50, null);
		JTextField txtDesc = UIBuilder.createTextField(200, null);

		JLabel lblCmd = new JLabel();
		JTextField txtCommand = new JTextField();
		UIBuilder.invalidKeyBlocker(txtCommand, c->{
			String text = txtCommand.getText();
			lblCmd.setText(text+c);
			return text.length() < 100;
		});
		
		JLabel lblParentDesc = new JLabel();
		JLabel lblCmdPrefix = new JLabel();
		JLabel lblCmdSuffix = new JLabel();
		JComboBox<Record<Map<String, Object>>> combo = Record.createCombo(parentData);
		combo.addActionListener(ae->{
			@SuppressWarnings("unchecked")
			Map<String, Object> recordMap = ((Record<Map<String, Object>>)combo.getSelectedItem()).getValue();
			lblCmdPrefix.setText((String) recordMap.get("COMMANDPREFIX"));
			lblCmdSuffix.setText((String) recordMap.get("COMMANDSUFFIX"));
			lblParentDesc.setText((String) recordMap.get("DESC"));
		});
		EventQueue.invokeLater(()->combo.setSelectedItem(combo.getSelectedItem()));
		
		new UIBuilder(150,150,150)
		.addPair(0, "Component type", combo)
		.addPair(0, "Type Description", lblParentDesc)
		.addPair(0, "Label", txtLabel, null)
		.addPair(0, "Command", txtCommand, UIBuilder.groupPane(FlowLayout.LEADING, lblCmdPrefix, lblCmd, lblCmdSuffix))
		.addPair(0, "Description", txtDesc)
		.showDialog(mainFrame, dialogTitle, dialog->{
//			Project proj = (Project) commonVarFetcher.get(CommonConstant.CUR_PROJ).get();
			@SuppressWarnings("unchecked")
			Map<String, Object> recordMap = ((Record<Map<String, Object>>)combo.getSelectedItem()).getValue();
			
			Number compTypeID = (Number) recordMap.get("COMPONENTTYPEID");
			String label = txtLabel.getText();
			String cmd = txtCommand.getText();
			String desc = txtDesc.getText();
			
			int projID = proj.getProjectId();
			
			DatabaseUtil.execute(conn->{
				Number nextDispOrder = (Number) DatabaseUtil.get(conn, "SELECT MAX(DISPORDER)+1 FROM COMPONENT WHERE PROJECTID = ?", new Object[]{projID}, 0);
				String query = "INSERT INTO COMPONENT (LABEL, COMMAND, DESC, DISPORDER, COMPONENTTYPEID, PROJECTID) VALUES(?, ?, ?, ?, ?, ?)";
				DatabaseUtil.query(conn, query, label, cmd, desc, nextDispOrder, compTypeID, projID);
				return null;
			});
			JOptionPane.showMessageDialog(dialog, "Changes will be update on restart.");
			return true;
		});
	}

	
	
/*
	public static void main(String[] args) {
		Map<String, Supplier<Object>> map = new HashMap<>();
		map.put("name", ()->"Brother");
		map.put("message", ()->"Hello, ${name}");

		String str = "My message is, ${message}..";

		ProjPanelHandler me = new ProjPanelHandler(map);
		System.out.println(me.normalizeCommand1(str));
	}*/

}
