import java.util.*;

public interface MedicalRepository {
    Optional<Medical> findById(String id);
    Optional<Medical> findByName(String name);       // convenience if you still use it in UI
    List<Medical> findAll();                         // MUST return ID-ascending

    boolean insert(Medical m);                       // requires user-provided id
    boolean update(Medical m);                       // WHERE id=?
    boolean deleteById(String id);

    // Legacy convenience (ok to keep for now if other parts still call it)
    boolean delete(String name);
}
