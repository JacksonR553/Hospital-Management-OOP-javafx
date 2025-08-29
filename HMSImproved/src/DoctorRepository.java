import java.util.*;

public interface DoctorRepository {
    Optional<Doctor> findById(String id);
    List<Doctor> findAll();
    boolean insert(Doctor d);
    boolean update(Doctor d);
    boolean delete(String id);
}
