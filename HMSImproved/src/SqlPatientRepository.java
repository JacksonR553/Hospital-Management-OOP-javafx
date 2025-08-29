import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

public final class SqlPatientRepository implements PatientRepository {
    private final DataSource ds;

    public SqlPatientRepository(DataSource ds) { this.ds = ds; }

    @Override
    public Optional<Patient> findById(String id) {
        String sql = "SELECT id,name,disease,sex,admit_status,age FROM patient WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Patient(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getInt(6)));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Patient> findAll() {
        String sql = "SELECT id,name,disease,sex,admit_status,age FROM patient ORDER BY name";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Patient> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Patient(
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getInt(6)));
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean insert(Patient p) {
        String sql = "INSERT INTO patient(id,name,disease,sex,admit_status,age) VALUES(?,?,?,?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getDisease());
            ps.setString(4, p.getSex());
            ps.setString(5, p.getAdmitStatus());
            ps.setInt(6, p.getAge());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean update(Patient p) {
        String sql = "UPDATE patient SET name=?,disease=?,sex=?,admit_status=?,age=? WHERE id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDisease());
            ps.setString(3, p.getSex());
            ps.setString(4, p.getAdmitStatus());
            ps.setInt(5, p.getAge());
            ps.setString(6, p.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }

    @Override
    public boolean delete(String id) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM patient WHERE id=?")) {
            ps.setString(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { return false; }
    }
}
