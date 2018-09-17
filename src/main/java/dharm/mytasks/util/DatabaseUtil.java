package dharm.mytasks.util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Database utility
 * @author Dharmendrasinh Chudasama
 */
public class DatabaseUtil {
	private static final List<Connection> CONN_LIST = new LinkedList<>();
//	private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class);
	
	@FunctionalInterface public interface Consumer<T>{ void accept(T data) throws Exception; }
	@FunctionalInterface public interface Function<T,R>{ R accept(T data) throws Exception; }
	
	static {
		// will call on shutdown JVM, example on System.exit(?);
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			closeAllConnections();
		}, "Remaining connection closer thread hook"));
	}
	
	/** get db connection, for close connection use {@link #close(Connection)}, never call, conn.close()
	 * @return new database connection
	 */
	private static Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName(Property.get("db_driver"));
		final Connection conn = DriverManager.getConnection(Property.get("db_url"), Property.get("db_username"), Property.get("db_password"));
		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		CONN_LIST.add(conn);
		return conn;
	}
	
	public static void close(final Connection conn) {
		if(conn != null){
			try {
				conn.close();
				CONN_LIST.remove(conn);
			}catch (SQLException e) {
				System.err.println("Can't close db connection");
//				e.printStackTrace(System.err);
			}
				
		}
	}

	/** close all remaining connection if any 
	 * @return 0=allClosed or remaining open connection due exceptions */
	public static synchronized int closeAllConnections(){ //close connections if open by mistake
		if(!CONN_LIST.isEmpty()) {
			for (Object conn : CONN_LIST.toArray()) { //toArray() for prevent concurrent modification exception
				close((Connection) conn);
			}
		}
		return CONN_LIST.size();
	}
	
	//========================= Data access Methods ================================================
	public static <R> R execute(final Function<Connection, R> connConsumer) {
		Connection conn = null;
		try{
			conn = getConnection();
			return connConsumer.accept(conn);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			close(conn);
		}
	}

	/** Method for handle transaction
	 * @param function
	 * @return returned value from function
	 * @throws Exception 
	 */
	public static void transaction(final DatabaseUtil.Consumer<Connection> allOrNoneConnConsumer) throws Exception {
		Connection conn = getConnection();
		conn.setAutoCommit(false);
		try{
			allOrNoneConnConsumer.accept(conn);
			conn.commit();
		}catch (Exception e) {
			conn.rollback();
			throw e;
		} finally {
			close(conn);
		}
	}
	
	/** 
	 * @param conn db connection (required)
	 * @param arguments arguments for sql statement
	 * @param def default value or <code>null</code>
	 * @return first result column element, if not found def parameter value
	 */
	public static Object get(Connection conn, String sql, Object[] arguments, Object def) throws Exception {
		return select(conn, sql, arguments, rs->(rs.next() ? rs.getObject(1) : def));
	}
	public static Object get(String sql, Object[] arguments, Object def) throws Exception {
//		return get(null, sql, arguments, def);
		return execute(conn->get(conn, sql, arguments, def));
	}

	
	/**
	 * @param conn instance of Connection
	 * @param sql
	 * @param arguments arguments or <code>null</code> for not found
	 * @param func
	 * @return argument func returned value
	 */
	public static <R> R select(final Connection conn, final String sql, final Object[] arguments, final Function<ResultSet, R> func) throws Exception {
//		if(conn==null) conn = getCachedReadOnlyConnection();
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			setArgs(stmt, arguments);
			try (final ResultSet rs = stmt.executeQuery()) {
				return func.accept(rs);
			}
		}
	}

	/** example: List&lt;Map&lt;String, Object&gt;&gt; list = DatabaseUtil.select(query, null, DatabaseUtil::toList); */
	public static <R> R select(String sql, Object[] arguments, Function<ResultSet, R> func) {
//		return select(null, sql, arguments, func);
		return execute(conn->select(conn, sql, arguments, func));
	}
	
	/** execute any db query 
	 * @return return value of {@link Statement#executeUpdate(String)} */
	public static int query(final Connection conn, final String query, final Object... args) throws SQLException {
		if(args==null || args.length==0) //for better performance
			return conn.createStatement().executeUpdate(query);
		else {
			try (PreparedStatement stmt = conn.prepareStatement(query)) {
				setArgs(stmt, args);
				return stmt.executeUpdate();
			}
		}
	}

	/** Open connection, execute query, close connection
	 * @see #execute(Consumer)
	 * @see {@link #query(Connection, String, Object...)}
	 */
	public static int query(final String query, final Object... args) {
/*		AtomicInteger ret = new AtomicInteger();
		execute(conn->ret.set(query(conn, query, args)));
		return ret.get();*/
		return execute(conn->query(conn, query, args));
	}

	/** @param conn
	 * @param insertQuery "INSERT INTO ..."
	 * @param args
	 * @return generated key or 0 if not generated
	 * @author Dharmendrasinh Chudasama
	 * @throws SQLException
	 */
	public static long insertRet(final Connection conn, final String insertQuery, final Object... args) throws SQLException {
		try (PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)){
			setArgs(stmt, args);
			stmt.execute();
			ResultSet rs = stmt.getGeneratedKeys();
			return rs.next() ? rs.getLong(1) : 0l;
		}
	}
	public static long insertRet(final String insertQuery, final Object... args) {
		return execute(conn->insertRet(conn, insertQuery, args));
	}

	/** call sp only */
	public static void call(final Connection conn, final String spName, final Object... args) throws Exception {
		call(conn, spName, args, null, null, null);
	}
	/** call sp only, using transactional connection
	 * @see #call(Connection, String, Object...) 
	 * @see #transaction(Consumer)
	 */
	public static void call(final String spName, final Object... args) throws Exception {
		transaction(conn->call(conn, spName, args));
	}

	
	//========================= Data Process Helper Methods ========================================
	/** set arguments to object
	 * @param stmt {@link PreparedStatement} or {@link CallableStatement}
	 * @see {@link #setArgs(PreparedStatement, int, Object...)}
	 * */
	public static int setArgs(final PreparedStatement stmt, final Object...args) throws SQLException {
		return setArgs(stmt, 1, args);
	}

	/**
	 * @param stmt {@link PreparedStatement} or {@link CallableStatement}
	 * @param_ from base=1 (first stmt.setObject(from++, arg);)
	 * @param args 
	 * @return next index which can set in <code>stmt.setObject(?, obj)</code>
	 * @throws SQLException
	 */
	public static int setArgs(final PreparedStatement stmt, int from, final Object...args) throws SQLException {
		if(args != null && args.length != 0)
			for (Object arg : args)
				stmt.setObject(from++, arg);
		return from;
	}

	/**
	 * @param rs resultset instance
	 * @return converted list from resultset 
	 * @throws SQLException
	 */
	public static List<Map<String, Object>> toMapList(ResultSet rs) throws SQLException {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		ResultSetMetaData meta = rs.getMetaData();
		int cols = meta.getColumnCount();
		Map<String, Object> map; // = new HashMap<>(cols);

		rs.beforeFirst();
		while (rs.next()) {
			map = new LinkedHashMap<>(cols);
			for(int i=1; i<=cols; i++)
				map.put(meta.getColumnLabel(i), rs.getObject(i));
			list.add(map);
		}
		return list;
	}

	/* * first column
	 * @param rs resultset instance (should has at least 1 column)
	 * @return first column
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> toList(ResultSet rs) throws SQLException {
		List<E> list = new ArrayList<E>();
		rs.beforeFirst();
		while(rs.next()) 
			list.add((E) rs.getObject(1));
		return list;
	}

	/** 2 columns
	 * @param <K>
	 * @param <V>
	 * @param rs resultset instance (should has at least 2 columns)
	 * @return converted map<col1, col2> from resultset 
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> toMap(ResultSet rs) throws SQLException {
		Map<K,V> map = new LinkedHashMap<>();
		rs.beforeFirst();
		while(rs.next())
			map.put((K) rs.getObject(1), (V) rs.getObject(2));
		return map;
	}
	
/*	// generally in case of create excel
	public static String[] getLabelArray(final ResultSetMetaData meta) throws SQLException {
		final int cols = meta.getColumnCount();
		final String[] labels = new String[cols];

		for (int i = 0; i < cols; i++)
			labels[i] = meta.getColumnLabel(i+1);

		return labels;
	}
*/

}
