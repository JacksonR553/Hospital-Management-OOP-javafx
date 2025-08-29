import java.util.Objects;
import java.util.Scanner;

public class Facility {
    private String facility;

    public Facility() { }

    public Facility(String facility) {
        this.facility = requireNonBlank(facility, "facility");
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }

    public String getFacility() { return facility; }
    public void setFacility(String facility) { this.facility = requireNonBlank(facility, "facility"); }

    @Deprecated
    public void newFacility(Scanner input) {
        System.out.print("Enter facility: ");
        this.facility = input.nextLine();
    }

    @Deprecated
    public void newFacility() {
        newFacility(new Scanner(System.in));
    }

    public String showFacility() {
        return facility;
    }

    @Override public String toString() { return showFacility(); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Facility)) return false;
        Facility other = (Facility) o;
        return Objects.equals(facility, other.facility);
    }
    @Override public int hashCode() { return Objects.hash(facility); }
}
