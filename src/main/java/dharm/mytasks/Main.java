package dharm.mytasks;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import dharm.mytasks.service.CommandExecutor;
import dharm.mytasks.service.DBConfigService;
import dharm.mytasks.service.ProjPanelHandler;
import dharm.mytasks.util.DatabaseUtil;

public class Main implements TreeSelectionListener {
    
    public static void main(String[] args) {
    	DBConfigService.validateDBConnection();
        /* Use an appropriate Look and Feel */
        try {
        	String clazz = AppConfig.LOOK_AND_FEEL_CLASS.get((String) null);
        	if(clazz!=null && !clazz.isEmpty())
        		UIManager.setLookAndFeel(clazz); //"javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception ex) {
        	System.err.println("Can't load look and feel");
        	AppConfig.LOOK_AND_FEEL_CLASS.set("");
        	AppConfig.saveData();
        }
        /* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        EventQueue.invokeLater(()->createAndShowGUI());
    }
    
	/**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("My Tasks");
//        frame.setPreferredSize(new Dimension(500, 450));
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        Main main = new Main();
        main.commonVarFetcher.put(CommonConstant.MAIN_FRAME, ()->frame);
        frame.setJMenuBar(new MainFrameMenuBar(frame));
        main.addComponentToPane(frame.getContentPane());
        frame.addWindowListener(new WindowAdapter() {
        	@Override public void windowClosing(WindowEvent e) {
        		main.tearDown();
        	}
		});
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
    
    
	private final Map<String, Supplier<Object>> commonVarFetcher = new HashMap<>();
	private final CommandExecutor commandExecutor = new CommandExecutor(commonVarFetcher);
	private final ProjPanelHandler projPanelHandler = new ProjPanelHandler(commonVarFetcher, commandExecutor);
	private JTree tree;
	private JPanel cards;
    
    public void addComponentToPane(Container pane){
    	cards = new JPanel(new CardLayout());
    	cards.setPreferredSize(new Dimension(400, 0));
    	
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode("Projects");
		DatabaseUtil.execute(conn -> {
			fillChildNodes(conn, root, 1);
			return null;
		});
    	this.tree = new JTree(root);
    	tree.setPreferredSize(new Dimension(150, 0));
//    	tree.setRootVisible(false);
    	tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setExpandsSelectedPaths(true);
    	tree.addTreeSelectionListener(this);
    	commonVarFetcher.put(CommonConstant.CUR_PROJ, ()->null);
    	
//    	pane.add(header, BorderLayout.PAGE_START);
    	pane.add(new JScrollPane(tree), BorderLayout.WEST);
    	pane.add(new JScrollPane(cards), BorderLayout.CENTER);
//    	pane.add(footer, BorderLayout.PAGE_END);
    }
    
    public void tearDown(){
    	try {
    		Project proj = (Project) commonVarFetcher.get(CommonConstant.CUR_PROJ).get();
    		if (proj != null) {
				DatabaseUtil.query("UPDATE PROJECT SET LASTUSEDON = NOW() WHERE PROJECTID = ?", proj.getProjectId());
				AppConfig.CUR_PROJ_ID.set(""+proj.getProjectId());
			}
    		AppConfig.saveData();
    	} finally {
    		DatabaseUtil.closeAllConnections();
    		System.exit(0);
    	}
    }

	private void fillChildNodes(Connection conn, DefaultMutableTreeNode parentNode, int parentProjID) {
		System.out.println("Filling for "+parentNode);
		try {
			String query = "SELECT PROJECTID, TITLE, CHILDCOUNT<>0 AS HASCHILD FROM PROJECT WHERE PARENTPROJECTID = ?";// ORDER BY LASTUSEDON DESC";
			List<Map<String, Object>> list = DatabaseUtil.select(conn, query, new Object[]{parentProjID}, DatabaseUtil::toMapList);
			
			for (Map<String, Object> map : list) {
				int projectId = (int)(long) map.get("PROJECTID");
				String title = (String) map.get("TITLE");
				boolean hasChild = (boolean) map.get("HASCHILD");
				
				Project proj = new Project(projectId, title);
		    	DefaultMutableTreeNode node = new DefaultMutableTreeNode(proj);
		    	if(hasChild)
					fillChildNodes(conn, node, projectId);
	    		parentNode.add(node);
	    		
	    		int lastProjID = Integer.parseInt(AppConfig.CUR_PROJ_ID.get("0"));
	    		if(lastProjID == projectId)
	    			EventQueue.invokeLater(()->tree.setSelectionPath(new TreePath(node.getPath())));
			}
		} catch (Exception ex) {
			System.err.println("Can't load children nodes for parentProjID = "+parentProjID);
			ex.printStackTrace(System.err);
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreePath selectedPath = e.getNewLeadSelectionPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
		if(node == null || !node.isLeaf()) return;

		Project proj = (Project) node.getUserObject();
    	commonVarFetcher.put(CommonConstant.CUR_PROJ, ()->proj);
		String panelName = "projectId-"+proj.getProjectId();
		if(!proj.isInitialisedPanel()){
			try {
	    		JPanel panel = projPanelHandler.createProjPane(proj);
	    		proj.setPanel(panel);
				proj.setInitialisedPanel(true);
				cards.add(panel, panelName);
			} catch (Exception ex) {
				System.err.println("Can't fill/initialise panel for project="+proj);
				ex.printStackTrace(System.err);
			}
		}
		
		CardLayout cl = (CardLayout) cards.getLayout();
		cl.show(cards, panelName);
	}

}
