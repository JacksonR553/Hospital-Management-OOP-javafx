import java.util.*;

import javafx.scene.control.ButtonType;

public interface StaffRepository {
    Optional<Staff> findById(String id);
    List<Staff> findAll();
    boolean insert(Staff s);
    boolean update(Staff s);
    boolean delete(String id);
}
