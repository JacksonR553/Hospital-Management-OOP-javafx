import java.util.Objects;
import java.util.Scanner;

public class Doctor {
    private String id;
    private String name;
    private String specialist;
    private String workTime;
    private String qualification;
    private int room;

    public Doctor() { }

    public Doctor(String id, String name, String specialist, String workTime, String qualification, int room) {
        this.id = requireNonBlank(id, "id");
        this.name = requireNonBlank(name, "name");
        this.specialist = requireNonBlank(specialist, "specialist");
        this.workTime = requireNonBlank(workTime, "workTime");
        this.qualification = requireNonBlank(qualification, "qualification");
        setRoom(room);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }

    public int getRoom() { return room; }
    public void setRoom(int room) {
        if (room < 0) throw new IllegalArgumentException("room must be >= 0");
        this.room = room;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = requireNonBlank(id, "id"); }

    public String getName() { return name; }
    public void setName(String name) { this.name = requireNonBlank(name, "name"); }

    public String getSpecialist() { return specialist; }
    public void setSpecialist(String specialist) { this.specialist = requireNonBlank(specialist, "specialist"); }

    public String getWorkTime() { return workTime; }
    public void setWorkTime(String workTime) { this.workTime = requireNonBlank(workTime, "workTime"); }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = requireNonBlank(qualification, "qualification"); }

    /**
     * Kept for backwards compatibility, but safer:
     * - Accept a Scanner provided by the caller (DON'T close System.in here).
     * - Also captures 'room'.
     */
    @Deprecated
    public void newDoctor(Scanner input) {
        System.out.print("Enter doctor ID: ");
        this.id = input.nextLine();
        System.out.print("Enter doctor name: ");
        this.name = input.nextLine();
        System.out.print("Enter doctor specialist: ");
        this.specialist = input.nextLine();
        System.out.print("Enter doctor work time: ");
        this.workTime = input.nextLine();
        System.out.print("Enter doctor qualification: ");
        this.qualification = input.nextLine();
        System.out.print("Enter doctor room: ");
        String roomStr = input.nextLine();
        this.room = Integer.parseInt(roomStr.trim()); // throws NumberFormatException if invalid
    }

    /** Matches current list header (no room yet, to avoid UI breakage). */
    public String showDoctorInfo() {
        return String.format("%-8s%-20s%-14s%-12s%-8s", id, name, specialist, workTime, qualification);
    }

    /** Optional: when you decide to extend the UI header to show room. */
    public String showDoctorInfoWithRoom() {
        return String.format("%-8s%-20s%-14s%-12s%-12s%-5d", id, name, specialist, workTime, qualification, room);
    }

    @Override public String toString() { return showDoctorInfo(); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Doctor)) return false;
        Doctor other = (Doctor) o;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
