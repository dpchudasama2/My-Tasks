package dharm.mytasks.service;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CommandExecutor {

	private final Map<String, Supplier<Object>> commonVarFetcher;

	public CommandExecutor(Map<String, Supplier<Object>> commonVarFetcher) {
		this.commonVarFetcher = commonVarFetcher;
	}
	
	public void execCommand(String command) {
		EventQueue.invokeLater(()->{
			List<String> cmd = splitt(command);
			try {
				Process process = new ProcessBuilder(cmd).start();
				Thread.sleep(800);
				if(!process.isAlive()){
					int exitValue = process.exitValue();
					if(exitValue != 0){
						System.err.println("Can't execute command: "+cmd+", exitValue="+exitValue);
					}
				}
			} catch (IOException | InterruptedException e) {
				System.err.println("Can't execute command: "+cmd);
			}
		});
	}
	
	private List<String> splitt(String command){
		char[] arr = normalizeOuterCommand(command).toCharArray(); //normalizing and converting

		List<String> commandList = new ArrayList<String>(10);
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			switch (c) {
				case '\\':
					buf.append(arr[i++]);
					break;
					
				case '|':
					commandList.add(buf.toString());
					buf = new StringBuilder();
					break;
	
				default:
					buf.append(c);
					break;
			}
		}

		commandList.add(buf.toString());
		return commandList;
	}

	private String normalizeOuterCommand(String command) {
		char[] arr = command.toCharArray();

		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			switch (c) {
				case '\\':
					buf.append(arr[i++]);
					break;
					
				case '$' :
						if(arr[i+1]=='{')
							i = normalizeInnerCommand(arr, i+2, val->buf.append(val));
						else buf.append(c);
					break;
	
				default:
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	private int normalizeInnerCommand(char[] arr, int from, Consumer<String> valConsumer) {
		StringBuilder buf = new StringBuilder();
		for (int i = from; i < arr.length; i++) {
			char c = arr[i];
			switch (c) {
				case '\\':
					buf.append(arr[i++]);
					break;
					
				case '$' :
						if(arr[i]=='{')
							i = normalizeInnerCommand(arr, i+1, val->buf.append(val));
						else buf.append(c);
					break;

				case '}' :
					String varName = buf.toString();
					String val = getVarVal(varName);
					String normalizedVal = normalizeOuterCommand(val); //recursive resolver
					valConsumer.accept(normalizedVal);
					return i;
	
				default:
					buf.append(c);
					break;
			}
		}

		throw new IllegalArgumentException("Syntax error !");
	}
	
//	private final Supplier<?> nullSupplier = ()->null;
	public String getVarVal(String varName){
//		Project proj = (Project) commonVarFetcher.get(CommonConstant.CUR_PROJ).get();
		/*if (proj != null) {
			JPanel panel = proj.getPanel();

			String prefix = "click.";
			if(varName.startsWith(prefix)){
				String compName = varName.substring(prefix.length());

				Optional<JButton> btnOpt = 
				Stream.of(panel.getComponents())
				.filter(c->c instanceof JButton).map(c->(JButton) c)
				.filter(b->b.getActionCommand().equals(compName))
				.findAny();
				if(btnOpt.isPresent()){
					btnOpt.get().doClick();
				}
			}
		}*/
		
		String val = (String) commonVarFetcher.getOrDefault(varName, ()->'!'+varName+'!').get();
		return val;
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
