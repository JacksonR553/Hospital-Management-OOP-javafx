import java.util.Objects;
import java.util.Scanner;

public class Medical {
    private String name;
    private String manufacturer;
    private String expiryDate; // keep as String for now (UI expects it)
    private int cost;
    private int count;

    public Medical() { }

    public Medical(String name, String manufacturer, String expiryDate, int cost, int count) {
        this.name = requireNonBlank(name, "name");
        this.manufacturer = requireNonBlank(manufacturer, "manufacturer");
        this.expiryDate = requireNonBlank(expiryDate, "expiryDate");
        setCost(cost);
        setCount(count);
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = requireNonBlank(name, "name"); }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = requireNonBlank(manufacturer, "manufacturer"); }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = requireNonBlank(expiryDate, "expiryDate"); }

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

    @Deprecated
    public void newMedical(Scanner input) {
        System.out.print("Enter medicine name: ");
        this.name = input.nextLine();
        System.out.print("Enter medicine manufacturer: ");
        this.manufacturer = input.nextLine();
        System.out.print("Enter medicine expiry date: ");
        this.expiryDate = input.nextLine();
        System.out.print("Enter medicine cost: ");
        String c = input.nextLine();
        this.cost = Integer.parseInt(c.trim());
        System.out.print("Enter medicine number of unit: ");
        String n = input.nextLine();
        this.count = Integer.parseInt(n.trim());
    }

    @Deprecated
    public void newMedical() {
        newMedical(new Scanner(System.in));
    }

    /** Keep original display formatting. */
    public String findMedical() {
        return String.format("%-20s%-20s%-17s$%d", name, manufacturer, expiryDate, cost);
    }

    @Override public String toString() { return findMedical(); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medical)) return false;
        Medical other = (Medical) o;
        return Objects.equals(name, other.name);
    }
    @Override public int hashCode() { return Objects.hash(name); }
}
