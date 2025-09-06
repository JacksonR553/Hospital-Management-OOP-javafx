public class AuditLog {
    private final long id;
    private final String ts;
    private final String tableName;
    private final String action;
    private final String entityId;
    private final String oldValues; // JSON
    private final String newValues; // JSON

    public AuditLog(long id, String ts, String tableName, String action,
                    String entityId, String oldValues, String newValues) {
        this.id = id;
        this.ts = ts;
        this.tableName = tableName;
        this.action = action;
        this.entityId = entityId;
        this.oldValues = oldValues;
        this.newValues = newValues;
    }

    public long getId() { return id; }
    public String getTs() { return ts; }
    public String getTableName() { return tableName; }
    public String getAction() { return action; }
    public String getEntityId() { return entityId; }
    public String getOldValues() { return oldValues; }
    public String getNewValues() { return newValues; }
}
