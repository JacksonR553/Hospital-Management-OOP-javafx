import java.util.Objects;
import java.util.Scanner;

public class Patient {
    private String id;
    private String name;
    private String disease;
    private String sex;
    private String admitStatus;
    private int age;

    public Patient() { }

    public Patient(String id, String name, String disease, String sex, String admitStatus, int age) {
        this.id = requireNonBlank(id, "id");
        this.name = requireNonBlank(name, "name");
        this.disease = requireNonBlank(disease, "disease");
        this.sex = requireNonBlank(sex, "sex");
        this.admitStatus = requireNonBlank(admitStatus, "admitStatus");
        setAge(age);
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = requireNonBlank(id, "id"); }

    public String getName() { return name; }
    public void setName(String name) { this.name = requireNonBlank(name, "name"); }

    public String getDisease() { return disease; }
    public void setDisease(String disease) { this.disease = requireNonBlank(disease, "disease"); }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = requireNonBlank(sex, "sex"); }

    public String getAdmitStatus() { return admitStatus; }
    public void setAdmitStatus(String admitStatus) { this.admitStatus = requireNonBlank(admitStatus, "admitStatus"); }

    public int getAge() { return age; }
    public void setAge(int age) {
        if (age < 0) throw new IllegalArgumentException("age must be >= 0");
        this.age = age;
    }

    /** Safer console capture: caller supplies Scanner; do NOT close System.in here. */
    @Deprecated
    public void newPatient(Scanner input) {
        System.out.print("Enter patient ID: ");
        this.id = input.nextLine();
        System.out.print("Enter patient name: ");
        this.name = input.nextLine();
        System.out.print("Enter patient disease: ");
        this.disease = input.nextLine();
        System.out.print("Enter patient sex: ");
        this.sex = input.nextLine();
        System.out.print("Enter patient admit status: ");
        this.admitStatus = input.nextLine();
        System.out.print("Enter patient age: ");
        String ageStr = input.nextLine();
        this.age = Integer.parseInt(ageStr.trim());
    }

    /** Backward-compatible shim (does not close System.in). */
    @Deprecated
    public void newPatient() {
        newPatient(new Scanner(System.in));
    }

    /** Keep existing column widths from your UI lists. */
    public String showPatientInfo() {
        return String.format("%-8s%-20s%-15s%-10s%-8s", id, name, disease, sex, admitStatus);
    }

    @Override public String toString() { return showPatientInfo(); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient)) return false;
        Patient other = (Patient) o;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
