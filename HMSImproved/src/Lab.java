public class Lab {
    private String id;
    private String name;
    private String status;
    private String result;

    public Lab(String id, String name, String status, String result) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.result = result;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getResult() { return result; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStatus(String status) { this.status = status; }
    public void setResult(String result) { this.result = result; }

    @Override
    public String toString() {
        return String.format("%-10s\t%-20s\t%-12s\t%-20s",
            id, name, status, result == null ? "" : result);
    }
}
