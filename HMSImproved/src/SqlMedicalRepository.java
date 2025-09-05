import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

public final class SqlMedicalRepository implements MedicalRepository {
    private final DataSource ds;

    public SqlMedicalRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Optional<Medical> findById(String id) {
        final String sql = "SELECT id,name,manufacturer,expiry_date,cost,count FROM medical WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Medical> findByName(String name) {
        final String sql = "SELECT id,name,manufacturer,expiry_date,cost,count FROM medical WHERE name=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Medical> findAll() {
        // Numeric ascending sort by ID (IDs are stored as TEXT but enforced numeric in UI)
        final String sql = "SELECT id,name,manufacturer,expiry_date,cost,count " +
                           "FROM medical " +
                           "ORDER BY CAST(id AS INTEGER) ASC";
        List<Medical> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        } catch (SQLException e) {
            // log if you want
        }
        return out;
    }

    @Override
    public boolean insert(Medical m) {
        final String sql = "INSERT INTO medical(id,name,manufacturer,expiry_date,cost,count) VALUES(?,?,?,?,?,?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getName());
            ps.setString(3, m.getManufacturer());
            ps.setString(4, m.getExpiryDate());
            ps.setInt(5, m.getCost());
            ps.setInt(6, m.getCount());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean update(Medical m) {
        final String sql = "UPDATE medical SET name=?, manufacturer=?, expiry_date=?, cost=?, count=? WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getManufacturer());
            ps.setString(3, m.getExpiryDate());
            ps.setInt(4, m.getCost());
            ps.setInt(5, m.getCount());
            ps.setString(6, m.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean deleteById(String id) {
        final String sql = "DELETE FROM medical WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean delete(String name) {
        final String sql = "DELETE FROM medical WHERE name=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    private static Medical map(ResultSet rs) throws SQLException {
        return new Medical(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("manufacturer"),
            rs.getString("expiry_date"),
            rs.getInt("cost"),
            rs.getInt("count")
        );
    }
}
