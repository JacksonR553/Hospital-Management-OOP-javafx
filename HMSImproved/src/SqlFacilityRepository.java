import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

public final class SqlFacilityRepository implements FacilityRepository {
    private final DataSource ds;
    public SqlFacilityRepository(DataSource ds) { this.ds = ds; }

    @Override
    public Optional<Facility> findByName(String name) {
        String sql = "SELECT name FROM facility WHERE name=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Facility(rs.getString(1)));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Facility> findAll() {
        String sql = "SELECT name FROM facility ORDER BY name";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Facility> out = new ArrayList<>();
            while (rs.next()) out.add(new Facility(rs.getString(1)));
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean insert(Facility f) {
        String sql = "INSERT INTO facility(name) VALUES(?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, f.getFacility());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean update(Facility f) {
        // Optional: rename facility (old name -> new name). Provide a setter in UI if needed.
        // For now, just return true; or implement when you add an "Edit" flow.
        return true;
    }

    @Override
    public boolean delete(String name) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM facility WHERE name=?")) {
            ps.setString(1, name);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }
}
