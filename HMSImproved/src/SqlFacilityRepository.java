import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class SqlFacilityRepository implements FacilityRepository {
    private final DataSource ds;

    public SqlFacilityRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public boolean insert(Facility f) {
        final String sql =
            "INSERT INTO facility(id,name,description,status,capacity) VALUES(?,?,?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, f.getId());
            ps.setString(2, f.getName());
            ps.setString(3, f.getDescription());
            ps.setString(4, f.getStatus());
            ps.setInt(5, f.getCapacity());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean update(Facility f) {
        final String sql =
            "UPDATE facility SET name=?, description=?, status=?, capacity=? WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, f.getName());
            ps.setString(2, f.getDescription());
            ps.setString(3, f.getStatus());
            ps.setInt(4, f.getCapacity());
            ps.setString(5, f.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean deleteById(String id) {
        final String sql = "DELETE FROM facility WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }
    
    // For legacy version
    @Override
    public boolean delete(String name) {
        final String sql = "DELETE FROM facility WHERE name=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Optional<Facility> findById(String id) {
        final String sql =
            "SELECT id,name,description,status,capacity FROM facility WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            // ignore
        }
        return Optional.empty();
    }

    @Override
    public Optional<Facility> findByName(String name) {
        final String sql =
            "SELECT id,name,description,status,capacity FROM facility WHERE name=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            // ignore
        }
        return Optional.empty();
    }

    @Override
    public List<Facility> findAll() {
        final String sql =
            "SELECT id,name,description,status,capacity " +
            "FROM facility " +
            "ORDER BY CAST(id AS INTEGER) ASC";
        List<Facility> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new Facility(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getInt("capacity")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    private static Facility map(ResultSet rs) throws SQLException {
        return new Facility(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getInt("capacity")
        );
    }
}
