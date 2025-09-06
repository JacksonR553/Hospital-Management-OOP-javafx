import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlAuditLogRepository implements AuditLogRepository {
    private final DataSource ds;
    public SqlAuditLogRepository(DataSource ds) { this.ds = ds; }

    private static AuditLog map(ResultSet rs) throws SQLException {
        return new AuditLog(
            rs.getLong("id"),
            rs.getString("ts"),
            rs.getString("table_name"),
            rs.getString("action"),
            rs.getString("entity_id"),
            rs.getString("old_values"),
            rs.getString("new_values")
        );
    }

    @Override public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT * FROM audit_log ORDER BY id DESC LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditLog> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<AuditLog> findByTable(String tableName, int limit) {
        String sql = "SELECT * FROM audit_log WHERE table_name=? ORDER BY id DESC LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditLog> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<AuditLog> findByEntity(String tableName, String entityId, int limit) {
        String sql = "SELECT * FROM audit_log WHERE table_name=? AND entity_id=? ORDER BY id DESC LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, entityId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditLog> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
