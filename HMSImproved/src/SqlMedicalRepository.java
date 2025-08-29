import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

public final class SqlMedicalRepository implements MedicalRepository {
    private final DataSource ds;
    public SqlMedicalRepository(DataSource ds) { this.ds = ds; }

    @Override
    public Optional<Medical> findByName(String name) {
        String sql = "SELECT name,manufacturer,expiry_date,cost,count FROM medical WHERE name=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Medical(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getInt(4), rs.getInt(5)
                ));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Medical> findAll() {
        String sql = "SELECT name,manufacturer,expiry_date,cost,count FROM medical ORDER BY name";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Medical> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Medical(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getInt(4), rs.getInt(5)
                ));
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean insert(Medical m) {
        String sql = "INSERT INTO medical(name,manufacturer,expiry_date,cost,count) VALUES(?,?,?,?,?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getManufacturer());
            ps.setString(3, m.getExpiryDate());
            ps.setInt(4, m.getCost());
            ps.setInt(5, m.getCount());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean update(Medical m) {
        String sql = "UPDATE medical SET manufacturer=?,expiry_date=?,cost=?,count=? WHERE name=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getManufacturer());
            ps.setString(2, m.getExpiryDate());
            ps.setInt(3, m.getCost());
            ps.setInt(4, m.getCount());
            ps.setString(5, m.getName());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean delete(String name) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM medical WHERE name=?")) {
            ps.setString(1, name);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }
}
