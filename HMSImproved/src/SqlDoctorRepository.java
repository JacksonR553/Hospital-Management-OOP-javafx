import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

public final class SqlDoctorRepository implements DoctorRepository {
    private final DataSource ds;
    public SqlDoctorRepository(DataSource ds) { this.ds = ds; }

    @Override
    public Optional<Doctor> findById(String id) {
        String sql = "SELECT id,name,specialist,work_time,qualification,room FROM doctor WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Doctor(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getInt(6)
                ));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Doctor> findAll() {
        String sql = "SELECT id,name,specialist,work_time,qualification,room FROM doctor ORDER BY name";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Doctor> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Doctor(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getInt(6)
                ));
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean insert(Doctor d) {
        String sql = "INSERT INTO doctor(id,name,specialist,work_time,qualification,room) VALUES(?,?,?,?,?,?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getName());
            ps.setString(3, d.getSpecialist());
            ps.setString(4, d.getWorkTime());
            ps.setString(5, d.getQualification());
            ps.setInt(6, d.getRoom());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean update(Doctor d) {
        String sql = "UPDATE doctor SET name=?,specialist=?,work_time=?,qualification=?,room=? WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getSpecialist());
            ps.setString(3, d.getWorkTime());
            ps.setString(4, d.getQualification());
            ps.setInt(5, d.getRoom());
            ps.setString(6, d.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean delete(String id) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM doctor WHERE id=?")) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }
}
