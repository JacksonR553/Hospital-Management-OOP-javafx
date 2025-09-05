import java.util.Objects;

public class Medical {
    private String id;               // user-defined primary key (TEXT)
    private String name;
    private String manufacturer;
    private String expiryDate;       // YYYY-MM-DD
    private int cost;
    private int count;

    public Medical() {}

    public Medical(String id, String name, String manufacturer, String expiryDate, int cost, int count) {
        this.id = req(id, "id");
        this.name = req(name, "name");
        this.manufacturer = req(manufacturer, "manufacturer");
        this.expiryDate = req(expiryDate, "expiryDate");
        if (cost < 0) throw new IllegalArgumentException("cost must be >= 0");
        if (count < 0) throw new IllegalArgumentException("count must be >= 0");
        this.cost = cost;
        this.count = count;
    }

    public static Medical of(String id, String name, String manufacturer, String expiryDate, int cost, int count) {
        return new Medical(id, name, manufacturer, expiryDate, cost, count);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = req(id, "id"); }

    public String getName() { return name; }
    public void setName(String name) { this.name = req(name, "name"); }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = req(manufacturer, "manufacturer"); }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = req(expiryDate, "expiryDate"); }

    public int getCost() { return cost; }
    public void setCost(int cost) {
        if (cost < 0) throw new IllegalArgumentException("cost must be >= 0");
        this.cost = cost;
    }

    public int getCount() { return count; }
    public void setCount(int count) {
        if (count < 0) throw new IllegalArgumentException("count must be >= 0");
        this.count = count;
    }

    private static String req(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " is required");
        return v.trim();
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medical)) return false;
        return id.equals(((Medical) o).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return "Medical{id='" + id + "', name='" + name + "'}";
    }
}