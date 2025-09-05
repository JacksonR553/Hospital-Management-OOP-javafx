import java.util.*;

public interface LabRepository {
    boolean insert(Lab lab);
    boolean update(Lab lab);
    boolean deleteById(String id);

    Optional<Lab> findById(String id);
    Optional<Lab> findByName(String name);
    List<Lab> findAll();
}