package dharm.mytasks.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author dchudasama */
public class Property {
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("project");

	/** property from project.properties file
	 * @param key
	 * @return property value
	 */
	public static String get(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
//			return '!' + key + '!';
			throw e;
		}
	}

	public static String get(String key, Function<String, String> noFound) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return noFound.apply(key);
		}
	}

	/** @param projName
	 * @param biConsumer call with each properties in sequence with key & value
	 */
	public static void forEachProperty(String projName, BiConsumer<String, String> biConsumer){
		try {
			(new Properties(){ private static final long serialVersionUID = -3242304116070641955L;
				public synchronized Object put(Object key, Object value) {
					biConsumer.accept(key.toString(), value.toString().trim());
					return null;
				};
			}).load(new FileInputStream(new File(get("propdir"), projName.trim()+".properties")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** @param projName
	 * @param valueGetterFunc may null or function which call with $key and replace it's value
	 * @return map
	 */
	public static LinkedHashMap<String, String> getPropertiesInSequence(String projName, Function<String,String> valueGetterFunc){
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		if (valueGetterFunc == null)
			forEachProperty(projName, map::put);
		else
			forEachProperty(projName, (k,v)-> map.put(k, replaceValues(v, valueGetterFunc)));
		return map;
	}
	

	/** @param line line to check (variables identified as ${A-z or 0-9}, ex: ${NAME})
	 * @param valueGetterFunc passed key as argument and get value for it
	 * @return replaced string
	 */
	public static String replaceValues(String line, Function<String, String> valueGetterFunc) {
        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("\\$\\{([A-Z|_|a-z|0-9]+)\\}").matcher(line);
        while (m.find()) {
            String key = m.group(1);
            String replacement = valueGetterFunc.apply(key);
            if (replacement == null)
				replacement = m.group();
			m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);

        return sb.toString();
    }
  
}
