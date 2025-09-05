import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class SqlLabRepository implements LabRepository {
    private final DataSource ds;

    public SqlLabRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public boolean insert(Lab lab) {
        final String sql = "INSERT INTO lab(id,name,status,result) VALUES(?,?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, lab.getId());
            ps.setString(2, lab.getName());
            ps.setString(3, lab.getStatus());
            ps.setString(4, lab.getResult());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Lab lab) {
        final String sql = "UPDATE lab SET name=?, status=?, result=? WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, lab.getName());
            ps.setString(2, lab.getStatus());
            ps.setString(3, lab.getResult());
            ps.setString(4, lab.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteById(String id) {
        final String sql = "DELETE FROM lab WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Optional<Lab> findById(String id) {
        final String sql = "SELECT id,name,status,result FROM lab WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Lab> findByName(String name) {
        final String sql = "SELECT id,name,status,result FROM lab WHERE name=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Lab> findAll() {
        final String sql = "SELECT id,name,status,result FROM lab ORDER BY CAST(id AS INTEGER) ASC";
        List<Lab> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    private static Lab map(ResultSet rs) throws SQLException {
        return new Lab(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("status"),
            rs.getString("result")
        );
    }
}
