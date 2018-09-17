package dharm.mytasks;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import dharm.mytasks.util.DatabaseUtil;

/** access and update configuration with local database
 * @author dharmendrac
 */
public enum AppConfig {
	LOOK_AND_FEEL_CLASS,
	ROOT_PROJ_ID,
	CUR_PROJ_ID,
	DEFAULT_HOST,
//	/** Path of Driver (DSC token Driver(s))*/DRIVERPATH, 
//	/** [true, false] */ FILE_RENAME_FLAG, FILE_RENAME_PREFIX,
	;
	
	public String getKey() {
		return name();
	}
	
	public boolean get(final boolean def){
		return get(getKey(), def);
	}
	
	public void set(boolean newVal){
		set(getKey(), newVal);
	}
	
	/**
	 * @param def default value for returns if value not found in db
	 * @return value or param def
	 */
	public String get(final String def) {
		return get(getKey(), def);
	}

	/** @param newVal new value which want to set, should not null */
	public void set(final String newVal) {
		set(getKey(), newVal);
	}

	//======================= DYNAMIC DATA COMPLETED ===========================

	/** Data Map for reduce local db operation overhead */
	private static final Map<String, String> dataMap = getLoadDataMap();
	
	public static Map<String, String> getDatamap() {
		return dataMap;
	}

	/** Load data from DB
	 * @return loaded data map
	 */
	private static final Map<String, String> getLoadDataMap() {
		return DatabaseUtil.select("SELECT Key, Val FROM AppConfig", null, DatabaseUtil::toMap);
	}

	public static final boolean get(final String key, final boolean def){
		String ret = get(key, def ? "T" : "F");
		return ret.equals("T");
	}
	
	public static final void set(final String key, final boolean newVal){
		set(key, newVal ? "T" : "F");
	}
	
	public static final String get(final String key, final String def) {
		return dataMap.getOrDefault(key, def);
	}
	
	private static final AtomicBoolean isUpdate = new AtomicBoolean();
	public static final void set(final String key, final String newVal) {
		dataMap.compute(key, (k,oldVal)->{
			if(!newVal.equals(oldVal)) isUpdate.set(true);
			return newVal;
		});
	}

	/** Update data in DB */
	public static void saveData() {
/*		holdedDataSupporterMap.forEach((k,arr)->{
			@SuppressWarnings("unchecked")
			Supplier<String> saveDataSupplier = (Supplier<String>) arr[2];
			set(k, saveDataSupplier.get());
		});
*/		
		if(!isUpdate.get()) return; //ignore if no field updated

		Map<String, String> newDataMap = dataMap;
		Map<String, String> oldDataMap = getLoadDataMap();

		System.out.println("saving data");
		newDataMap.forEach((key,newVal)->{
			String oldVal = oldDataMap.get(key);
			if(newVal.equals(oldVal)) return; //already same value

			if(oldVal!=null){//update
				DatabaseUtil.query("UPDATE AppConfig SET Val = ? WHERE Key = ?", newVal, key);
				System.out.println("Updated for key="+key);
			}
			else{//insert because not found
				DatabaseUtil.query("INSERT INTO AppConfig (Key, Val) VALUES (?, ?)", key, newVal);
				System.out.println("New val inserted for key="+key);
			}
		});
		System.out.println("saved");
		isUpdate.set(false);
	}
	
/*	private static final Map<String, Object[]> holdedDataSupporterMap = new HashMap<>();
	public static void requestHoldDataState(String key, String defVal, Consumer<String> loadDataConsumer, Supplier<String> saveDataSupplier){
		Object ret = holdedDataSupporterMap.put(key, new Object[]{defVal, loadDataConsumer, saveDataSupplier});
		if(ret!=null) throw new IllegalArgumentException("Data listeners already in hold for key = "+key);
	}

	*//** call consumer for set data which holded <br/>
	 * fetch from db and set values
	 * @see #requestHoldDataState(String, Consumer, Supplier)
	 * @see #saveData()
	 *//*
	public static void triggerLoadConfigData(){
		holdedDataSupporterMap.forEach((k,arr)->{
			final String defVal = (String) arr[0];
			@SuppressWarnings("unchecked")
			final Consumer<String> loadDataConsumer = (Consumer<String>) arr[1];

			loadDataConsumer.accept(get(k, defVal));
		});
	}
*/
//	public static void requestHoldDataState(String key, String defVal, Consumer<String> loadDataConsumer, Supplier<String> saveDataSupplier){}
//	public static void triggerLoadConfigData(){}
}
