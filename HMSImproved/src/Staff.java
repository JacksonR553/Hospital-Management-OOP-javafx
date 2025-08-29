import java.util.Objects;
import java.util.Scanner;

public class Staff {
    private String id;
    private String name;
    private String designation;
    private String sex;
    private int salary;

    public Staff() { }

    public Staff(String id, String name, String designation, String sex, int salary) {
        this.id = requireNonBlank(id, "id");
        this.name = requireNonBlank(name, "name");
        this.designation = requireNonBlank(designation, "designation");
        this.sex = requireNonBlank(sex, "sex");
        setSalary(salary);
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = requireNonBlank(id, "id"); }

    public String getName() { return name; }
    public void setName(String name) { this.name = requireNonBlank(name, "name"); }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = requireNonBlank(designation, "designation"); }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = requireNonBlank(sex, "sex"); }

    public int getSalary() { return salary; }
    public void setSalary(int salary) {
        if (salary < 0) throw new IllegalArgumentException("salary must be >= 0");
        this.salary = salary;
    }

    @Deprecated
    public void newStaff(Scanner input) {
        System.out.print("Enter staff ID: ");
        this.id = input.nextLine();
        System.out.print("Enter staff name: ");
        this.name = input.nextLine();
        System.out.print("Enter staff designation: ");
        this.designation = input.nextLine();
        System.out.print("Enter staff sex: ");
        this.sex = input.nextLine();
        System.out.print("Enter staff salary: ");
        String s = input.nextLine();
        this.salary = Integer.parseInt(s.trim());
    }

    @Deprecated
    public void newStaff() {
        newStaff(new Scanner(System.in));
    }

    public String showStaffInfo() {
        return String.format("%-8s%-20s%-20s%-8s$%d", id, name, designation, sex, salary);
    }

    @Override public String toString() { return showStaffInfo(); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Staff)) return false;
        Staff other = (Staff) o;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
