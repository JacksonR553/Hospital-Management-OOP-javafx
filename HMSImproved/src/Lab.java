import java.util.Objects;
import java.util.Scanner;

public class Lab {
    private String lab;
    private int cost;

    public Lab() { }

    public Lab(String lab, int cost) {
        this.lab = requireNonBlank(lab, "lab");
        setCost(cost);
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }

    public String getLab() { return lab; }
    public void setLab(String lab) { this.lab = requireNonBlank(lab, "lab"); }

    public int getCost() { return cost; }
    public void setCost(int cost) {
        if (cost < 0) throw new IllegalArgumentException("cost must be >= 0");
        this.cost = cost;
    }

    @Deprecated
    public void newLab(Scanner input) {
        System.out.print("Enter lab: ");              // fixed prompt (was "facility" before)
        this.lab = input.nextLine();
        System.out.print("Enter cost: ");
        String c = input.nextLine();
        this.cost = Integer.parseInt(c.trim());
    }

    @Deprecated
    public void newLab() {
        newLab(new Scanner(System.in));
    }

    public String labList() {
        return lab + "\t" + cost;
    }

    @Override public String toString() { return labList(); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lab)) return false;
        Lab other = (Lab) o;
        return Objects.equals(lab, other.lab);
    }
    @Override public int hashCode() { return Objects.hash(lab); }
}
