import java.util.*;

public interface FacilityRepository {
	Optional<Facility> findById(String id);
	Optional<Facility> findByName(String name);
	List<Facility> findAll(); // sorted by numeric id ascending
	
    boolean insert(Facility facility);
    boolean update(Facility facility);
    boolean deleteById(String id);
    
    boolean delete(String name);
}
