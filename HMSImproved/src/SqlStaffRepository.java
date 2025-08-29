import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

public final class SqlStaffRepository implements StaffRepository {
    private final DataSource ds;
    public SqlStaffRepository(DataSource ds) { this.ds = ds; }

    @Override
    public Optional<Staff> findById(String id) {
        String sql = "SELECT id,name,designation,sex,salary FROM staff WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Staff(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getInt(5)
                ));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Staff> findAll() {
        String sql = "SELECT id,name,designation,sex,salary FROM staff ORDER BY name";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Staff> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Staff(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getInt(5)
                ));
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean insert(Staff s) {
        String sql = "INSERT INTO staff(id,name,designation,sex,salary) VALUES(?,?,?,?,?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getDesignation());
            ps.setString(4, s.getSex());
            ps.setInt(5, s.getSalary());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean update(Staff s) {
        String sql = "UPDATE staff SET name=?,designation=?,sex=?,salary=? WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getDesignation());
            ps.setString(3, s.getSex());
            ps.setInt(4, s.getSalary());
            ps.setString(5, s.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean delete(String id) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM staff WHERE id=?")) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }
}
