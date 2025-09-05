import java.nio.file.*;
import java.sql.*;
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;

public final class Db {
    private static SQLiteDataSource ds;

    private Db() {}

    public static DataSource get() {
        if (ds != null) return ds;
        try {
            Path dbDir = Paths.get(System.getProperty("user.home"), ".hms");
            Files.createDirectories(dbDir);
            String url = "jdbc:sqlite:" + dbDir.resolve("hms.db").toString();
            ds = new SQLiteDataSource();
            ds.setUrl(url);
            return ds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void bootstrap() {
        try (Connection c = get().getConnection(); Statement s = c.createStatement()) {
            // existing tables (unchanged)
            s.execute("CREATE TABLE IF NOT EXISTS patient(" +
                      "id TEXT PRIMARY KEY," +
                      "name TEXT NOT NULL," +
                      "disease TEXT NOT NULL," +
                      "sex TEXT NOT NULL," +
                      "admit_status TEXT NOT NULL," +
                      "age INTEGER NOT NULL)");

            s.execute("CREATE TABLE IF NOT EXISTS doctor(" +
                      "id TEXT PRIMARY KEY," +
                      "name TEXT NOT NULL," +
                      "specialist TEXT NOT NULL," +
                      "work_time TEXT NOT NULL," +
                      "qualification TEXT NOT NULL," +
                      "room INTEGER NOT NULL)");

            s.execute("CREATE TABLE IF NOT EXISTS staff(" +
                      "id TEXT PRIMARY KEY," +
                      "name TEXT NOT NULL," +
                      "designation TEXT NOT NULL," +
                      "sex TEXT NOT NULL," +
                      "salary INTEGER NOT NULL)");

            // Fresh schema if table does not exist
            s.execute("CREATE TABLE IF NOT EXISTS medical(" +
                      "id TEXT PRIMARY KEY," +
                      "name TEXT NOT NULL UNIQUE," +
                      "manufacturer TEXT NOT NULL," +
                      "expiry_date TEXT NOT NULL," +
                      "cost INTEGER NOT NULL," +
                      "count INTEGER NOT NULL)");

            // If an old "medical" already existed *without* id → migrate it once
            migrateAddIdTextPk(c, "medical",
                "CREATE TABLE medical_new(" +
                "id TEXT PRIMARY KEY," +
                "name TEXT NOT NULL UNIQUE," +
                "manufacturer TEXT NOT NULL," +
                "expiry_date TEXT NOT NULL," +
                "cost INTEGER NOT NULL," +
                "count INTEGER NOT NULL)",
                // copy rows; set id = name for the legacy data
                "INSERT INTO medical_new(id,name,manufacturer,expiry_date,cost,count) " +
                "SELECT name, name, manufacturer, expiry_date, cost, count FROM medical");
            
            // === Facility table (aligns with Medical-style PK id as TEXT; UI enforces numeric) ===
            s.execute("CREATE TABLE IF NOT EXISTS facility(" +
                      "id TEXT PRIMARY KEY," +
                      "name TEXT NOT NULL UNIQUE," +
                      "description TEXT NOT NULL," +
                      "status TEXT NOT NULL," +
                      "capacity INTEGER NOT NULL)");
            
            // === Lab table ===
            s.execute("CREATE TABLE IF NOT EXISTS lab(" +
                      "id TEXT PRIMARY KEY," +
                      "name TEXT NOT NULL UNIQUE," +
                      "status TEXT NOT NULL," +
                      "result TEXT)");
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void migrateAddIdTextPk(Connection c, String table,
                                           String createNewSql, String copySql) throws SQLException {
        if (!tableExists(c, table)) return;      // brand-new install: nothing to migrate
        if (hasColumn(c, table, "id")) return;   // already has id: nothing to do
        try (Statement s = c.createStatement()) {
            s.execute("BEGIN");
            s.execute(createNewSql);
            s.execute(copySql);
            s.execute("DROP TABLE " + table);
            s.execute("ALTER TABLE " + table + "_new RENAME TO " + table);
            s.execute("COMMIT");
        }
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        try (ResultSet rs = c.getMetaData().getTables(null, null, table, null)) {
            return rs.next();
        }
    }

    private static boolean hasColumn(Connection c, String table, String col) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) if (col.equalsIgnoreCase(rs.getString("name"))) return true;
            return false;
        }
    }

}
