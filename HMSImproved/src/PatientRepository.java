import java.util.*;

public interface PatientRepository {
    Optional<Patient> findById(String id);
    List<Patient> findAll();
    boolean insert(Patient p);
    boolean update(Patient p);
    boolean delete(String id);
}
