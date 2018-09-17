package dharm.mytasks.component;

import java.util.Map;
import java.util.Vector;

import javax.swing.JComboBox;

public class Record<T> {
	private String label;
	private T value;

	public Record(String label, T value) {
		this.label = label;
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return label;
	}

	
	public static <T1> JComboBox<Record<T1>> createCombo(Map<String, T1> map){
		Vector<Record<T1>> vector = createRecordVector(map);
		return new JComboBox<Record<T1>>(vector);
	}

	public static <T1> Vector<Record<T1>> createRecordVector(Map<String, T1> map) {
		Vector<Record<T1>> vector = new Vector<>(map.size());
		map.forEach((label,value)->vector.add(new Record<T1>(label, value)));
		return vector;
	}
}
