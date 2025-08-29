import java.util.*;

public interface MedicalRepository {
    Optional<Medical> findByName(String name);
    List<Medical> findAll();
    boolean insert(Medical m);
    boolean update(Medical m);
    boolean delete(String name);
}
