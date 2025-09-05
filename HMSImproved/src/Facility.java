import java.util.Objects;

public class Facility {
    private String id;           // numeric string (PK)
    private String name;         // unique
    private String description;  // free text
    private String status;       // e.g., "Operational", "Maintenance"
    private int capacity;        // e.g., number of beds/equipment
    
    public Facility() {}

    public Facility(String id, String name, String description, String status, int capacity) {
        this.id = req(id, "id"); // required for id/name to be filled
        this.name = req(name, "name");
        this.description = description;
        this.status = status;
        this.capacity = capacity;
    }
    
    public static Facility of(String id, String name, String description, String status, int capacity) {
    	return new Facility(id, name, description, status, capacity);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = req(id, "id"); }

    public String getName() { return name; }
    public void setName(String name) { this.name = req(name, "name"); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    
    private static String req(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " is required");
        return v.trim();
    }
    
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Facility)) return false;
        return id.equals(((Facility) o).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        // formatted for Show list (monospace-friendly)
        return String.format("%-10s\t%-20s\t%-14s\t%8d", id, name, status, capacity);
    }
}
