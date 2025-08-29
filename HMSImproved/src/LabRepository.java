import java.util.*;

public interface LabRepository {
    Optional<Lab> findByName(String name);
    List<Lab> findAll();
    boolean insert(Lab l);
    boolean update(Lab l);
    boolean delete(String name);
}
