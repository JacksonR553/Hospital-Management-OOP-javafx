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
            
            // === Audit table ===
            s.execute("""
            CREATE TABLE IF NOT EXISTS audit_log(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              ts TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
              table_name TEXT NOT NULL,
              action TEXT NOT NULL,         -- INSERT | UPDATE | DELETE
              entity_id TEXT,               -- the row's PK value
              old_values TEXT,              -- JSON of OLD.* (for UPDATE/DELETE)
              new_values TEXT               -- JSON of NEW.* (for INSERT/UPDATE)
            )""");

            s.execute("CREATE INDEX IF NOT EXISTS idx_audit_table ON audit_log(table_name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_log(table_name, entity_id)");

            // ---------------- Patient triggers ----------------
            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_patient_ai
            AFTER INSERT ON patient
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('patient','INSERT', NEW.id, NULL,
                json_object('id',NEW.id,'name',NEW.name,'disease',NEW.disease,'sex',NEW.sex,'admit_status',NEW.admit_status,'age',NEW.age)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_patient_au
            AFTER UPDATE ON patient
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('patient','UPDATE', NEW.id,
                json_object('id',OLD.id,'name',OLD.name,'disease',OLD.disease,'sex',OLD.sex,'admit_status',OLD.admit_status,'age',OLD.age),
                json_object('id',NEW.id,'name',NEW.name,'disease',NEW.disease,'sex',NEW.sex,'admit_status',NEW.admit_status,'age',NEW.age)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_patient_ad
            AFTER DELETE ON patient
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('patient','DELETE', OLD.id,
                json_object('id',OLD.id,'name',OLD.name,'disease',OLD.disease,'sex',OLD.sex,'admit_status',OLD.admit_status,'age',OLD.age),
                NULL
              );
            END;""");

            // ---------------- Doctor triggers ----------------
            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_doctor_ai
            AFTER INSERT ON doctor
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('doctor','INSERT', NEW.id, NULL,
                json_object('id',NEW.id,'name',NEW.name,'specialist',NEW.specialist,'work_time',NEW.work_time,'qualification',NEW.qualification,'room',NEW.room)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_doctor_au
            AFTER UPDATE ON doctor
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('doctor','UPDATE', NEW.id,
                json_object('id',OLD.id,'name',OLD.name,'specialist',OLD.specialist,'work_time',OLD.work_time,'qualification',OLD.qualification,'room',OLD.room),
                json_object('id',NEW.id,'name',NEW.name,'specialist',NEW.specialist,'work_time',NEW.work_time,'qualification',NEW.qualification,'room',NEW.room)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_doctor_ad
            AFTER DELETE ON doctor
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('doctor','DELETE', OLD.id,
                json_object('id',OLD.id,'name',OLD.name,'specialist',OLD.specialist,'work_time',OLD.work_time,'qualification',OLD.qualification,'room',OLD.room),
                NULL
              );
            END;""");

            // ---------------- Staff triggers ----------------
            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_staff_ai
            AFTER INSERT ON staff
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('staff','INSERT', NEW.id, NULL,
                json_object('id',NEW.id,'name',NEW.name,'designation',NEW.designation,'sex',NEW.sex,'salary',NEW.salary)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_staff_au
            AFTER UPDATE ON staff
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('staff','UPDATE', NEW.id,
                json_object('id',OLD.id,'name',OLD.name,'designation',OLD.designation,'sex',OLD.sex,'salary',OLD.salary),
                json_object('id',NEW.id,'name',NEW.name,'designation',NEW.designation,'sex',NEW.sex,'salary',NEW.salary)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_staff_ad
            AFTER DELETE ON staff
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('staff','DELETE', OLD.id,
                json_object('id',OLD.id,'name',OLD.name,'designation',OLD.designation,'sex',OLD.sex,'salary',OLD.salary),
                NULL
              );
            END;""");

            // ---------------- Medical triggers ----------------
            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_medical_ai
            AFTER INSERT ON medical
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('medical','INSERT', NEW.id, NULL,
                json_object('id',NEW.id,'name',NEW.name,'manufacturer',NEW.manufacturer,'expiry_date',NEW.expiry_date,'cost',NEW.cost,'count',NEW.count)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_medical_au
            AFTER UPDATE ON medical
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('medical','UPDATE', NEW.id,
                json_object('id',OLD.id,'name',OLD.name,'manufacturer',OLD.manufacturer,'expiry_date',OLD.expiry_date,'cost',OLD.cost,'count',OLD.count),
                json_object('id',NEW.id,'name',NEW.name,'manufacturer',NEW.manufacturer,'expiry_date',NEW.expiry_date,'cost',NEW.cost,'count',NEW.count)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_medical_ad
            AFTER DELETE ON medical
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('medical','DELETE', OLD.id,
                json_object('id',OLD.id,'name',OLD.name,'manufacturer',OLD.manufacturer,'expiry_date',OLD.expiry_date,'cost',OLD.cost,'count',OLD.count),
                NULL
              );
            END;""");

            // ---------------- Facility triggers ----------------
            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_facility_ai
            AFTER INSERT ON facility
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('facility','INSERT', NEW.id, NULL,
                json_object('id',NEW.id,'name',NEW.name,'description',NEW.description,'status',NEW.status,'capacity',NEW.capacity)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_facility_au
            AFTER UPDATE ON facility
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('facility','UPDATE', NEW.id,
                json_object('id',OLD.id,'name',OLD.name,'description',OLD.description,'status',OLD.status,'capacity',OLD.capacity),
                json_object('id',NEW.id,'name',NEW.name,'description',NEW.description,'status',NEW.status,'capacity',NEW.capacity)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_facility_ad
            AFTER DELETE ON facility
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('facility','DELETE', OLD.id,
                json_object('id',OLD.id,'name',OLD.name,'description',OLD.description,'status',OLD.status,'capacity',OLD.capacity),
                NULL
              );
            END;""");

            // ---------------- Lab triggers ----------------
            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_lab_ai
            AFTER INSERT ON lab
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('lab','INSERT', NEW.id, NULL,
                json_object('id',NEW.id,'name',NEW.name,'status',NEW.status,'result',NEW.result)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_lab_au
            AFTER UPDATE ON lab
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('lab','UPDATE', NEW.id,
                json_object('id',OLD.id,'name',OLD.name,'status',OLD.status,'result',OLD.result),
                json_object('id',NEW.id,'name',NEW.name,'status',NEW.status,'result',NEW.result)
              );
            END;""");

            s.execute("""
            CREATE TRIGGER IF NOT EXISTS trg_lab_ad
            AFTER DELETE ON lab
            BEGIN
              INSERT INTO audit_log(table_name, action, entity_id, old_values, new_values)
              VALUES ('lab','DELETE', OLD.id,
                json_object('id',OLD.id,'name',OLD.name,'status',OLD.status,'result',OLD.result),
                NULL
              );
            END;""");
            
            s.execute("CREATE TABLE IF NOT EXISTS notification(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "created_at TEXT NOT NULL DEFAULT (datetime('now'))," +
                    "severity TEXT NOT NULL," +
                    "title TEXT NOT NULL," +
                    "detail TEXT," +
                    "seen INTEGER NOT NULL DEFAULT 0)");
            
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
