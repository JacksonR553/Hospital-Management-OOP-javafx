import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

public final class SqlLabRepository implements LabRepository {
    private final DataSource ds;
    public SqlLabRepository(DataSource ds) { this.ds = ds; }

    @Override
    public Optional<Lab> findByName(String name) {
        String sql = "SELECT name, cost FROM lab WHERE name=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Lab(rs.getString(1), rs.getInt(2)));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Lab> findAll() {
        String sql = "SELECT name, cost FROM lab ORDER BY name";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Lab> out = new ArrayList<>();
            while (rs.next()) out.add(new Lab(rs.getString(1), rs.getInt(2)));
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean insert(Lab l) {
        String sql = "INSERT INTO lab(name, cost) VALUES(?, ?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, l.getLab());
            ps.setInt(2, l.getCost());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean update(Lab l) {
        String sql = "UPDATE lab SET cost=? WHERE name=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, l.getCost());
            ps.setString(2, l.getLab());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean delete(String name) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM lab WHERE name=?")) {
            ps.setString(1, name);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }
}
