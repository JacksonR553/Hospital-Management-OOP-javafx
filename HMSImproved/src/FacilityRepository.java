import java.util.*;

public interface FacilityRepository {
    Optional<Facility> findByName(String name);
    List<Facility> findAll();
    boolean insert(Facility f);
    boolean update(Facility f);   // renaming support (optional)
    boolean delete(String name);
}
