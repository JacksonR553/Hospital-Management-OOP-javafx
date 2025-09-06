import java.util.List;

public interface AuditLogRepository {
    List<AuditLog> findRecent(int limit);
    List<AuditLog> findByTable(String tableName, int limit);
    List<AuditLog> findByEntity(String tableName, String entityId, int limit);
}
