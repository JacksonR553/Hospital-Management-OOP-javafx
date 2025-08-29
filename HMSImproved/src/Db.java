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
            s.execute("CREATE TABLE IF NOT EXISTS medical(" +
                    "name TEXT PRIMARY KEY," +
                    "manufacturer TEXT NOT NULL," +
                    "expiry_date TEXT NOT NULL," +   // store as YYYY-MM-DD string
                    "cost INTEGER NOT NULL," +
                    "count INTEGER NOT NULL)");
            // add other tables later (doctor, staff, etc.)
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
