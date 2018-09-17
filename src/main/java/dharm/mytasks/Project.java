package dharm.mytasks;

import javax.swing.JPanel;

public class Project {
	private int projectId;
	private String title;
	private JPanel panel;
	private boolean initialisedPanel = false;
	
	public Project(int projectId, String title) {
		this.projectId = projectId;
		this.title = title;
	}

	public JPanel getPanel() {
		return panel;
	}

	public void setPanel(JPanel panel) {
		this.panel = panel;
	}

	public boolean isInitialisedPanel() {
		return initialisedPanel;
	}

	public void setInitialisedPanel(boolean initialisedPanel) {
		this.initialisedPanel = initialisedPanel;
	}

	public int getProjectId() {
		return projectId;
	}

	@Override
	public String toString() {
		return title;
	}
	
}
