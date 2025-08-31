import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class HospitalManagement extends Application{
	// --- App layout constants ---
	private static final double APP_WIDTH  = 1000;
	private static final double APP_HEIGHT = 700;
	// persistence
	private PatientRepository patientRepo;
	private DoctorRepository doctorRepo;
	private StaffRepository staffRepo;
	private MedicalRepository medicalRepo;
	private LabRepository labRepo;
	private FacilityRepository facilityRepo;
    
    private String getCurrentDateTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return currentDateTime.format(formatter);
    }
    
    private static Label monospaceHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");
        return l;
    }
    
    // ==== Patient update workflow state ====
    private ListView<String> patientListView;        // list shown in "Show Patient" view
    private String selectedPatientId = null;         // PK of selected row for update
    private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddPatientHandler; // to restore after Update mode
    
 // ==== Staff update workflow state ====
    private ListView<String> staffListView;
    private String selectedStaffId = null;
    private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddStaffHandler;

    // ==== Doctor update workflow state ====
    private ListView<String> doctorListView;
    private String selectedDoctorId = null;
    private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddDoctorHandler;

    // ==== Medical update workflow state (PK = name) ====
    private ListView<String> medicalListView;
    private String selectedMedicalName = null;
    private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddMedicalHandler;

    // ==== Lab update workflow state (PK = name) ====
    private ListView<String> labListView;
    private String selectedLabName = null;
    private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddLabHandler;

    // ==== Facility update workflow state (PK = name) ====
    private ListView<String> facilityListView;
    private String selectedFacilityName = null;
    private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddFacilityHandler;
    
    private static String ns(String s) { return s == null ? "" : s; }
    
    private static boolean confirm(String title, String content) {
        var a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        var res = a.showAndWait();
        return res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK;
    }
    
 // === Sorting helpers ===
    private static int cmpId(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        try {
            return Integer.compare(Integer.parseInt(a.trim()), Integer.parseInt(b.trim()));
        } catch (NumberFormatException ex) {
            return a.compareToIgnoreCase(b); // fallback if IDs aren't pure numbers
        }
    }
    private static int cmpName(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.compareToIgnoreCase(b);
    }

    // === Sorted refreshers for each ListView ===
    private void refreshPatientList() {
        if (patientListView == null) return;
        patientListView.getItems().clear();
        java.util.List<Patient> all = new java.util.ArrayList<>(patientRepo.findAll());
        all.sort((p1, p2) -> cmpId(p1.getId(), p2.getId()));
        for (Patient p : all) {
            String row = String.format("%-10s%-20s%-22s%-8s%-18s%5d",
                ns(p.getId()), ns(p.getName()), ns(p.getDisease()),
                ns(p.getSex()), ns(p.getAdmitStatus()), p.getAge());
            patientListView.getItems().add(row);
        }
    }
    private void refreshStaffList() {
        if (staffListView == null) return;
        staffListView.getItems().clear();
        java.util.List<Staff> all = new java.util.ArrayList<>(staffRepo.findAll());
        all.sort((s1, s2) -> cmpId(s1.getId(), s2.getId()));
        for (Staff s : all) {
            String row = String.format("%-10s%-20s%-20s%-8s%8d",
                ns(s.getId()), ns(s.getName()), ns(s.getDesignation()), ns(s.getSex()), s.getSalary());
            staffListView.getItems().add(row);
        }
    }
    private void refreshDoctorList() {
        if (doctorListView == null) return;
        doctorListView.getItems().clear();
        java.util.List<Doctor> all = new java.util.ArrayList<>(doctorRepo.findAll());
        all.sort((d1, d2) -> cmpId(d1.getId(), d2.getId()));
        for (Doctor d : all) {
            String row = String.format("%-10s%-20s%-22s%-12s%-20s%6d",
                ns(d.getId()), ns(d.getName()), ns(d.getSpecialist()),
                ns(d.getWorkTime()), ns(d.getQualification()), d.getRoom());
            doctorListView.getItems().add(row);
        }
    }
    private void refreshMedicalList() {
        if (medicalListView == null) return;
        medicalListView.getItems().clear();
        java.util.List<Medical> all = new java.util.ArrayList<>(medicalRepo.findAll());
        all.sort((m1, m2) -> cmpName(m1.getName(), m2.getName())); // PK = name
        for (Medical m : all) {
            String row = String.format("%-22s%-22s%-13s%7d%7d",
                ns(m.getName()), ns(m.getManufacturer()), ns(m.getExpiryDate()), m.getCost(), m.getCount());
            medicalListView.getItems().add(row);
        }
    }
    private void refreshLabList() {
        if (labListView == null) return;
        labListView.getItems().clear();
        java.util.List<Lab> all = new java.util.ArrayList<>(labRepo.findAll());
        all.sort((l1, l2) -> cmpName(l1.getLab(), l2.getLab())); // PK = name
        for (Lab l : all) {
            String row = String.format("%-28s%8d", ns(l.getLab()), l.getCost());
            labListView.getItems().add(row);
        }
    }
    private void refreshFacilityList() {
        if (facilityListView == null) return;
        facilityListView.getItems().clear();
        java.util.List<Facility> all = new java.util.ArrayList<>(facilityRepo.findAll());
        all.sort((f1, f2) -> cmpName(f1.showFacility(), f2.showFacility())); // PK = name
        for (Facility f : all) {
            facilityListView.getItems().add(ns(f.showFacility())); // or f.getFacility()
        }
    }
    
    private static String leading(String row, int width) {
        return row == null ? "" : row.substring(0, Math.min(width, row.length())).trim();
    }
    
    private static String beforeTabOrEnd(String row) {
        if (row == null) return "";
        int i = row.indexOf('\t');
        return i >= 0 ? row.substring(0, i).trim() : row.trim();
    }

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		
		// database
		Db.bootstrap();
		patientRepo = new SqlPatientRepository(Db.get());
		doctorRepo = new SqlDoctorRepository(Db.get());
		staffRepo = new SqlStaffRepository(Db.get());
		medicalRepo = new SqlMedicalRepository(Db.get());
		labRepo = new SqlLabRepository(Db.get());
		facilityRepo = new SqlFacilityRepository(Db.get());
		
		// ----------------------------------------------------------------------------------
		// MAIN MENU (modern, minimal, larger)

		// Buttons
		Button mainBt1 = new Button("Staff");
		Button mainBt2 = new Button("Doctor");
		Button mainBt3 = new Button("Patient");
		Button mainBt4 = new Button("Medical");
		Button mainBt5 = new Button("Lab");
		Button mainBt6 = new Button("Facility");

		// unified button styling
		List<Button> mainButtons = Arrays.asList(mainBt1, mainBt2, mainBt3, mainBt4, mainBt5, mainBt6);
		for (Button b : mainButtons) {
		    b.setMinWidth(240);
		    b.setMinHeight(56);
		    b.setStyle(
		        "-fx-background-color: #0ea5e9;" +          // sky-500
		        "-fx-text-fill: white;" +
		        "-fx-font-size: 16px;" +
		        "-fx-font-weight: 700;" +
		        "-fx-background-radius: 12;" +
		        "-fx-cursor: hand;"
		    );
		    b.setOnMouseEntered(e -> b.setStyle(
		        "-fx-background-color: #0284c7;" +          // sky-600 hover
		        "-fx-text-fill: white;" +
		        "-fx-font-size: 16px;" +
		        "-fx-font-weight: 700;" +
		        "-fx-background-radius: 12;" +
		        "-fx-cursor: hand;"
		    ));
		    b.setOnMouseExited(e -> b.setStyle(
		        "-fx-background-color: #0ea5e9;" +
		        "-fx-text-fill: white;" +
		        "-fx-font-size: 16px;" +
		        "-fx-font-weight: 700;" +
		        "-fx-background-radius: 12;" +
		        "-fx-cursor: hand;"
		    ));
		}

		// Title + live time
		Text mainTxt = new Text("Hospital Management System");
		mainTxt.setStyle("-fx-fill: #0f172a; -fx-font-size: 36px; -fx-font-weight: 800;"); // slate-900

		Text timeTxt = new Text(getCurrentDateTime());
		timeTxt.setStyle("-fx-fill: #334155; -fx-font-size: 13px;"); // slate-600

		// live clock
		Timeline clock = new Timeline(
		    new KeyFrame(Duration.ZERO, e -> timeTxt.setText(getCurrentDateTime())),
		    new KeyFrame(Duration.seconds(1))
		);
		clock.setCycleCount(Timeline.INDEFINITE);
		clock.play();

		// Layout: gradient background + centered "card"
		StackPane root = new StackPane();
		root.setStyle(
		    "-fx-background-color: linear-gradient(to bottom right, #eef2ff, #e0f2fe);" // indigo-50 → sky-100
		);

		VBox card = new VBox(24);
		card.setAlignment(Pos.CENTER);
		card.setStyle(
		    "-fx-background-color: rgba(255,255,255,0.92);" +
		    "-fx-background-radius: 20;" +
		    "-fx-padding: 40 48;" +
		    "-fx-effect: dropshadow(gaussian, rgba(2,6,23,0.18), 30, 0.2, 0, 10);"
		);

		// header
		VBox header = new VBox(8);
		header.setAlignment(Pos.CENTER);
		header.getChildren().addAll(mainTxt, timeTxt);

		// 2×3 grid of menu buttons
		GridPane grid = new GridPane();
		grid.setHgap(20);
		grid.setVgap(20);
		grid.setAlignment(Pos.CENTER);
		grid.add(mainBt1, 0, 0);
		grid.add(mainBt2, 1, 0);
		grid.add(mainBt3, 0, 1);
		grid.add(mainBt4, 1, 1);
		grid.add(mainBt5, 0, 2);
		grid.add(mainBt6, 1, 2);

		// assemble card
		card.getChildren().addAll(header, grid);
		StackPane.setMargin(card, new Insets(40, 40, 40, 40));
		root.getChildren().add(card);

		// Scene
		Scene mainScene = new Scene(root, APP_WIDTH, APP_HEIGHT);
		primaryStage.setTitle("Hospital Management System");
		primaryStage.setScene(mainScene);
		primaryStage.setMinWidth(APP_WIDTH);
		primaryStage.setMinHeight(APP_HEIGHT);
		primaryStage.show();
		
		// ----------------------------------------------------------------------------------
		// Staff Menu
		
		// Buttons
		Button addStaff = new Button("Add Staff");
		Button showStaff = new Button("Show Staff");
		Button returnTo1 = new Button("Return");
		Button updateStaff = new Button("Update");
		Button deleteStaff = new Button("Delete");
		
		addStaff.setPrefWidth(100);
		showStaff.setPrefWidth(100);
		returnTo1.setPrefWidth(100);
		addStaff.setMinHeight(50);
		showStaff.setMinHeight(50);
		returnTo1.setMinHeight(50);
		updateStaff.setPrefWidth(100);
		deleteStaff.setPrefWidth(100);
		updateStaff.setMinHeight(50);
		deleteStaff.setMinHeight(50);
		
		Button addStaffTo = new Button("Add");
		
		addStaffTo.setPrefWidth(100);
		addStaffTo.setMinHeight(30);
		
		
		// Text
		Text staffTxt = new Text("Staff");
		staffTxt.setFill(Color.WHITE);
		staffTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));
		
		
		// Text Field
		TextField staffTf1 = new TextField();
		TextField staffTf2 = new TextField();
		TextField staffTf3 = new TextField();
		TextField staffTf4 = new TextField();
		TextField staffTf5= new TextField();
		TextField staffTf6 = new TextField();
		
		// HBox
		HBox staffH1 = new HBox();
		
		staffH1.getChildren().add(staffTxt);
		staffH1.setAlignment(Pos.CENTER);
		staffH1.setSpacing(20);
		staffH1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		staffH1.setPrefSize(50, 50);
		
		// VBox
		VBox staffV1 = new VBox();
		VBox staffV2 = new VBox();
		
		staffV1.getChildren().addAll(addStaff, showStaff, returnTo1, updateStaff, deleteStaff);
		staffV1.setAlignment(Pos.BASELINE_CENTER);
		staffV1.setSpacing(30);
		staffV1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		staffV1.setPrefSize(130, 600);
		
		staffV2.setAlignment(Pos.TOP_LEFT);
		staffV2.setSpacing(10);
		staffV2.setPrefSize(470, 600);
		staffV2.setPadding(new Insets(20));
		
		// BorderPane
		BorderPane staffPane = new BorderPane();
		staffPane.setTop(staffH1);
		staffPane.setLeft(staffV1);
		staffPane.setCenter(staffV2);
		
		// Scene
		Scene staffScene = new Scene(staffPane, 700, 600);
		
		// Staff Button Functions 
		addStaff.setOnAction(e -> {
			staffV2.getChildren().clear();
			staffV2.getChildren().addAll(
					new Label("ID: "),staffTf1, 
					new Label("Name: "), staffTf2, 
					new Label("Designation: "), staffTf3, 
					new Label("Sex: "), staffTf4, 
					new Label("Salary: "), staffTf5, addStaffTo, staffTf6);
			VBox.setMargin(addStaffTo, new Insets(0, 0, 30, 200));
			VBox.setMargin(staffTf6, new Insets(0, 0, 30, 0));
		});
		
		addStaffTo.setOnAction(e -> {
		    staffTf6.setText("");

		    final String id = staffTf1.getText().trim();
		    final String name = staffTf2.getText().trim();
		    final String designation = staffTf3.getText().trim();
		    final String sex = staffTf4.getText().trim();
		    final String salaryStr = staffTf5.getText().trim();

		    if (id.isEmpty() || name.isEmpty() || designation.isEmpty() || sex.isEmpty() || salaryStr.isEmpty()) {
		        staffTf6.setText("Please fill in all fields.");
		        return;
		    }

		    final int salary;
		    try {
		        salary = Integer.parseInt(salaryStr);
		        if (salary < 0) throw new NumberFormatException("negative");
		    } catch (NumberFormatException nfe) {
		        staffTf6.setText("Salary must be a non-negative number.");
		        return;
		    }

		    if (staffRepo.findById(id).isPresent()) {
		        staffTf6.setText("A staff member with this ID already exists.");
		        return;
		    }

		    boolean ok = staffRepo.insert(new Staff(id, name, designation, sex, salary));
		    if (!ok) { staffTf6.setText("Failed to add staff."); return; }

		    staffTf6.setText("New STAFF added successfully.");
		    staffTf1.clear(); staffTf2.clear(); staffTf3.clear();
		    staffTf4.clear(); staffTf5.clear();
		    
		    if (staffListView != null) refreshStaffList();

		});

		
		showStaff.setOnAction(e -> {
			// At the start, reset selection
			selectedPatientId = null;
			
		    // Header
		    Label columnHeaderLabel = new Label("  ID        Name                 Designation         Sex     Salary");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // Use the class field (NOT a local variable)
		    staffListView = new ListView<>();
		    staffListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // Selection tracking (same as Patient: selecting a row sets the selected PK)
		    staffListView.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, row) -> {
		        selectedStaffId = (row == null) ? null : leading(row, 10); // first column is width 10
		    });
		    
		    // Populate list
		    staffListView.getItems().clear();
		    staffListView.getSelectionModel().clearSelection();
		    
		    staffListView.setPlaceholder(new Label("No staff found."));
		    staffListView.setPrefHeight(550);
		    staffListView.setPrefWidth(700);
		    
		    refreshStaffList();

		    staffV2.getChildren().clear();
		    staffV2.getChildren().addAll(columnHeaderLabel, staffListView);
		});
		
		updateStaff.setOnAction(e -> {
		    staffTf6.setText("");

		    // 1) Resolve selected ID (prefer selected row; fallback to typed field)
		    String id = selectedStaffId;
		    if ((id == null || id.isBlank()) && staffListView != null) {
		        String sel = staffListView.getSelectionModel().getSelectedItem();
		        if (sel != null) id = leading(sel, 10);
		    }
		    if (id == null || id.isBlank()) {
		        staffTf6.setText("Select a staff in the list first.");
		        return;
		    }

		    var opt = staffRepo.findById(id);
		    if (opt.isEmpty()) {
		        staffTf6.setText("Selected staff no longer exists.");
		        // optional: if you're already on Show, refresh it
		        if (staffListView != null) {
		            staffListView.getItems().clear();
		            for (Staff ss : staffRepo.findAll()) {
		                String row = String.format("%-10s%-20s%-20s%-8s%8d",
		                        ss.getId(), ss.getName(), ss.getDesignation(), ss.getSex(), ss.getSalary());
		                staffListView.getItems().add(row);
		            }
		        }
		        return;
		    }
		    Staff s = opt.get();

		    // 2) Switch to Add view (reuse your existing builder)
		    try { addStaff.fire(); } catch (Exception ignore) {}

		    // 3) Pre-fill form with the selected record
		    staffTf1.setText(s.getId());
		    staffTf2.setText(s.getName() == null ? "" : s.getName());
		    staffTf3.setText(s.getDesignation() == null ? "" : s.getDesignation());
		    staffTf4.setText(s.getSex() == null ? "" : s.getSex());
		    staffTf5.setText(String.valueOf(s.getSalary()));

		    // 4) Lock PK and change main button to "Update Record"
		    staffTf1.setEditable(false);
		    addStaffTo.setText("Update Record");

		    // 5) Swap handler (remember original to restore later)
		    if (originalAddStaffHandler == null) {
		        originalAddStaffHandler = addStaffTo.getOnAction();
		    }

		    final String selectedId = id; // effectively final for inner handler
		    addStaffTo.setOnAction(ev -> {
		        staffTf6.setText("");

		        final String name = staffTf2.getText().trim();
		        final String designation = staffTf3.getText().trim();
		        final String sex = staffTf4.getText().trim();
		        final String salaryStr = staffTf5.getText().trim();

		        if (name.isEmpty() || designation.isEmpty() || sex.isEmpty() || salaryStr.isEmpty()) {
		            staffTf6.setText("Please fill in all fields.");
		            return;
		        }

		        final int salary;
		        try {
		            salary = Integer.parseInt(salaryStr);
		            if (salary < 0) throw new NumberFormatException("negative");
		        } catch (NumberFormatException ex) {
		            staffTf6.setText("Salary must be a non-negative number.");
		            return;
		        }

		        // 6) Persist update
		        boolean ok = staffRepo.update(new Staff(selectedId, name, designation, sex, salary));
		        staffTf6.setText(ok ? "Staff updated." : "Failed to update staff.");

		        // 7) Restore Add mode & unlock PK
		        addStaffTo.setText("Add");
		        addStaffTo.setOnAction(originalAddStaffHandler);
		        staffTf1.setEditable(true);

		        // 8) Clear form fields
		        staffTf1.clear(); staffTf2.clear(); staffTf3.clear(); staffTf4.clear(); staffTf5.clear();

		        // 9) Return to Show + refresh list (like Patient)
		        try { showStaff.fire(); } catch (Exception ignore) {}

		        if (staffListView != null) {
		            staffListView.getItems().clear();
		            refreshStaffList();
		        }

		        // 10) Reset selection
		        selectedStaffId = null;
		    });
		});

		deleteStaff.setOnAction(e -> {
		    staffTf6.setText("");

		    String id = (selectedStaffId != null) ? selectedStaffId : staffTf1.getText().trim();
		    if ((id == null || id.isEmpty()) && staffListView != null) {
		        String sel = staffListView.getSelectionModel().getSelectedItem();
		        if (sel != null) id = leading(sel, 10);
		    }
		    if (id == null || id.isEmpty()) { staffTf6.setText("Select a staff or enter an ID."); return; }
		    if (staffRepo.findById(id).isEmpty()) { staffTf6.setText("No staff with this ID exists."); return; }

		    if (!confirm("Delete Staff", "Are you sure you want to delete staff ID: " + id + "?")) return;

		    boolean ok = staffRepo.delete(id);
		    if (!ok) { staffTf6.setText("Failed to delete staff."); return; }

		    staffTf6.setText("Staff deleted.");

		    // Clear form fields
		    staffTf1.clear(); staffTf2.clear(); staffTf3.clear(); staffTf4.clear(); staffTf5.clear();

		    // Refresh list (if showing)
		    if (staffListView != null) {
		        staffListView.getItems().clear();
		        refreshStaffList();
		    }

		    // Keep parity with Patient by returning to Show
		    try { showStaff.fire(); } catch (Exception ignore) {}

		    selectedStaffId = null;
		});

		
		// ----------------------------------------------------------------------------------
		// Doctor Menu
		// Buttons
		Button addDoctor = new Button("Add Doctor");
		Button showDoctor = new Button("Show Doctor");
		Button returnTo2 = new Button("Return");
		Button updateDoctor = new Button("Update");
		Button deleteDoctor = new Button("Delete");
		
		addDoctor.setPrefWidth(100);
		showDoctor.setPrefWidth(100);
		addDoctor.setMinHeight(50);
		showDoctor.setMinHeight(50);
		returnTo2.setPrefWidth(100);
		returnTo2.setMinHeight(50);
		updateDoctor.setPrefWidth(100);
		updateDoctor.setMinHeight(50);
		deleteDoctor.setPrefWidth(100);
		deleteDoctor.setMinHeight(50);
		
		Button addDoctorTo = new Button("Add");
		
		addDoctorTo.setPrefWidth(100);
		addDoctorTo.setMinHeight(30);
		
		// Text
		Text doctorTxt = new Text("Doctor");
		doctorTxt.setFill(Color.WHITE);
		doctorTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));
		
		
		// Text Field
		TextField doctorTf1 = new TextField();
		TextField doctorTf2 = new TextField();
		TextField doctorTf3 = new TextField();
		TextField doctorTf4 = new TextField();
		TextField doctorTf5= new TextField();
		TextField doctorTf6 = new TextField();
		TextField doctorTf7 = new TextField();
		
		// HBox
		HBox doctorH1 = new HBox();
		
		doctorH1.getChildren().add(doctorTxt);
		doctorH1.setAlignment(Pos.CENTER);
		doctorH1.setSpacing(20);
		doctorH1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		doctorH1.setPrefSize(50, 50);
		
		// VBox
		VBox doctorV1 = new VBox();
		VBox doctorV2 = new VBox();
		
		doctorV1.getChildren().addAll(addDoctor, showDoctor, returnTo2, updateDoctor, deleteDoctor);
		doctorV1.setAlignment(Pos.BASELINE_CENTER);
		doctorV1.setSpacing(30);
		doctorV1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		doctorV1.setPrefSize(130, 600);
		
		doctorV2.setAlignment(Pos.TOP_LEFT);
		doctorV2.setSpacing(10);
		doctorV2.setPrefSize(470, 600);
		doctorV2.setPadding(new Insets(20));
		
		// BorderPane
		BorderPane doctorPane = new BorderPane();
		doctorPane.setTop(doctorH1);
		doctorPane.setLeft(doctorV1);
		doctorPane.setCenter(doctorV2);
		
		// Scene
		Scene doctorScene = new Scene(doctorPane, 700, 600);
		
		// Doctor Button Functions 
		addDoctor.setOnAction(e -> {
			doctorV2.getChildren().clear();
			doctorV2.getChildren().addAll(
					new Label("ID: "),doctorTf1, 
					new Label("Name: "), doctorTf2, 
					new Label("Specialist: "), doctorTf3, 
					new Label("Work time: "), doctorTf4, 
					new Label("Qualification: "), doctorTf5, 
					new Label("Room: "), doctorTf6, addDoctorTo, doctorTf7);
			VBox.setMargin(addDoctorTo, new Insets(0, 0, 30, 200));
			VBox.setMargin(doctorTf7, new Insets(0, 0, 30, 0));
		});
		
		addDoctorTo.setOnAction(e -> {
		    doctorTf7.setText("");

		    final String id = doctorTf1.getText().trim();
		    final String name = doctorTf2.getText().trim();
		    final String specialist = doctorTf3.getText().trim();
		    final String workTime = doctorTf4.getText().trim();
		    final String qualification = doctorTf5.getText().trim();
		    final String roomStr = doctorTf6.getText().trim();

		    if (id.isEmpty() || name.isEmpty() || specialist.isEmpty() || workTime.isEmpty() || qualification.isEmpty() || roomStr.isEmpty()) {
		        doctorTf7.setText("Please fill in all fields.");
		        return;
		    }

		    final int room;
		    try {
		        room = Integer.parseInt(roomStr);
		        if (room < 0) throw new NumberFormatException("negative");
		    } catch (NumberFormatException nfe) {
		        doctorTf7.setText("Room must be a non-negative number.");
		        return;
		    }

		    if (doctorRepo.findById(id).isPresent()) {
		        doctorTf7.setText("A doctor with this ID already exists.");
		        return;
		    }

		    boolean ok = doctorRepo.insert(new Doctor(id, name, specialist, workTime, qualification, room));
		    if (!ok) { doctorTf7.setText("Failed to add doctor."); return; }

		    doctorTf7.setText("New DOCTOR added successfully.");
		    doctorTf1.clear(); doctorTf2.clear(); doctorTf3.clear();
		    doctorTf4.clear(); doctorTf5.clear(); doctorTf6.clear();
		    
		    if (doctorListView != null) refreshDoctorList();

		});

		
		showDoctor.setOnAction(e -> {
			// reset selection view
			selectedDoctorId = null;
			
		    doctorV2.getChildren().clear();

		    Label columnHeaderLabel = new Label("  ID        Name                 Specialist            Work Time   Qualification        Room");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // Use the CLASS FIELD, not a local variable
		    doctorListView = new ListView<>();
		    doctorListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");
		    doctorListView.setPlaceholder(new Label("No doctors found."));
		    doctorListView.setPrefHeight(550);
		    doctorListView.setPrefWidth(700);

		    // Populate
		    doctorListView.getItems().clear();
		    doctorListView.getSelectionModel().clearSelection();

		    // Track selection (first column width = 10)
		    doctorListView.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, row) -> {
		        selectedDoctorId = (row == null) ? null : leading(row, 10);
		    });
		    
		    refreshDoctorList();

		    doctorV2.getChildren().addAll(columnHeaderLabel, doctorListView);
		});

		updateDoctor.setOnAction(e -> {
		    doctorTf7.setText("");

		    // Resolve selected ID
		    String id = selectedDoctorId;
		    if ((id == null || id.isBlank()) && doctorListView != null) {
		        String sel = doctorListView.getSelectionModel().getSelectedItem();
		        if (sel != null) id = leading(sel, 10);
		    }
		    if (id == null || id.isBlank()) { doctorTf7.setText("Select a doctor in the list first."); return; }

		    var opt = doctorRepo.findById(id);
		    if (opt.isEmpty()) { doctorTf7.setText("Selected doctor no longer exists."); try { showDoctor.fire(); } catch (Exception ignore) {} return; }
		    Doctor d = opt.get();

		    // Switch to Add view
		    try { addDoctor.fire(); } catch (Exception ignore) {}

		    // Prefill
		    doctorTf1.setText(d.getId());
		    doctorTf2.setText(ns(d.getName()));
		    doctorTf3.setText(ns(d.getSpecialist()));
		    doctorTf4.setText(ns(d.getWorkTime()));
		    doctorTf5.setText(ns(d.getQualification()));
		    doctorTf6.setText(String.valueOf(d.getRoom()));

		    // Lock PK, flip main button
		    doctorTf1.setEditable(false);
		    addDoctorTo.setText("Update Record");

		    // Save original handler once
		    if (originalAddDoctorHandler == null) originalAddDoctorHandler = addDoctorTo.getOnAction();

		    final String selectedId = id;
		    addDoctorTo.setOnAction(ev -> {
		        doctorTf7.setText("");

		        final String name = doctorTf2.getText().trim();
		        final String specialist = doctorTf3.getText().trim();
		        final String workTime = doctorTf4.getText().trim();
		        final String qualification = doctorTf5.getText().trim();
		        final String roomStr = doctorTf6.getText().trim();

		        if (name.isEmpty() || specialist.isEmpty() || workTime.isEmpty() || qualification.isEmpty() || roomStr.isEmpty()) {
		            doctorTf7.setText("Please fill in all fields.");
		            return;
		        }
		        final int room;
		        try { room = Integer.parseInt(roomStr); if (room < 0) throw new NumberFormatException(); }
		        catch (NumberFormatException ex) { doctorTf7.setText("Room must be a non-negative number."); return; }

		        boolean ok = doctorRepo.update(new Doctor(selectedId, name, specialist, workTime, qualification, room));
		        doctorTf7.setText(ok ? "Doctor updated." : "Failed to update doctor.");

		        // Restore Add mode
		        addDoctorTo.setText("Add");
		        addDoctorTo.setOnAction(originalAddDoctorHandler);
		        doctorTf1.setEditable(true);

		        // Clear
		        doctorTf1.clear(); doctorTf2.clear(); doctorTf3.clear(); doctorTf4.clear(); doctorTf5.clear(); doctorTf6.clear();

		        // Refresh list
		        try { showDoctor.fire(); } catch (Exception ignore) {}
		        if (doctorListView != null) {
		            doctorListView.getItems().clear();
		            refreshDoctorList();
		        }

		        selectedDoctorId = null;
		    });
		});
		
		deleteDoctor.setOnAction(e -> {
		    doctorTf7.setText("");

		    // Resolve ID: tracked selection → text field → list selection (first 10 chars)
		    String id = (selectedDoctorId != null) ? selectedDoctorId : doctorTf1.getText().trim();
		    if (id.isEmpty() && doctorListView != null) {
		        String sel = doctorListView.getSelectionModel().getSelectedItem();
		        if (sel != null) id = sel.substring(0, Math.min(10, sel.length())).trim();
		    }

		    if (id.isEmpty()) { doctorTf7.setText("Select a doctor or enter an ID."); return; }
		    if (doctorRepo.findById(id).isEmpty()) { doctorTf7.setText("No doctor with this ID exists."); return; }
		    if (!confirm("Delete Doctor", "Delete doctor " + id + "?")) return;

		    boolean ok = doctorRepo.delete(id); // use deleteById(id) if your repo uses that name
		    if (!ok) { doctorTf7.setText("Failed to delete doctor."); return; }

		    doctorTf7.setText("Doctor deleted.");

		    // Clear form and selection; unlock ID for new entries
		    doctorTf1.clear(); doctorTf2.clear(); doctorTf3.clear();
		    doctorTf4.clear(); doctorTf5.clear(); doctorTf6.clear();
		    doctorTf1.setEditable(true);
		    selectedDoctorId = null;

		    // Refresh list
		    if (doctorListView != null) {
		        doctorListView.getSelectionModel().clearSelection();
		        doctorListView.getItems().clear();
		        refreshDoctorList();
		    }
		});
		
		// ----------------------------------------------------------------------------------
		// Patient Menu
		
		// Buttons
		Button addPatient = new Button("Add Patient");
		Button showPatient = new Button("Show Patient");
		Button returnTo3 = new Button("Return");
		Button updatePatient = new Button("Update");
		Button deletePatient = new Button("Delete");

		
		addPatient.setPrefWidth(100);
		showPatient.setPrefWidth(100);
		addPatient.setMinHeight(50);
		showPatient.setMinHeight(50);
		returnTo3.setPrefWidth(100);
		returnTo3.setMinHeight(50);
		updatePatient.setPrefWidth(100);
		updatePatient.setMinHeight(50);
		deletePatient.setPrefWidth(100);
		deletePatient.setMinHeight(50);
		
		Button addPatientTo = new Button("Add");
		
		addPatientTo.setPrefWidth(100);
		addPatientTo.setMinHeight(30);
		
		
		// Text
		Text patientTxt = new Text("Patient");
		patientTxt.setFill(Color.WHITE);
		patientTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));
		
		
		// Text Field
		TextField patientTf1 = new TextField();
		TextField patientTf2 = new TextField();
		TextField patientTf3 = new TextField();
		TextField patientTf4 = new TextField();
		TextField patientTf5= new TextField();
		TextField patientTf6 = new TextField();
		TextField patientTf7 = new TextField();
		
		// HBox
		HBox patientH1 = new HBox();
		
		patientH1.getChildren().add(patientTxt);
		patientH1.setAlignment(Pos.CENTER);
		patientH1.setSpacing(20);
		patientH1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		patientH1.setPrefSize(50, 50);
		
		// VBox
		VBox patientV1 = new VBox();
		VBox patientV2 = new VBox();
		
		patientV1.getChildren().addAll(addPatient, showPatient, returnTo3, updatePatient, deletePatient);
		patientV1.setAlignment(Pos.BASELINE_CENTER);
		patientV1.setSpacing(30);
		patientV1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		patientV1.setPrefSize(130, 600);
		
		patientV2.setAlignment(Pos.TOP_LEFT);
		patientV2.setSpacing(10);
		patientV2.setPrefSize(470, 600);
		patientV2.setPadding(new Insets(20));
		
		// BorderPane
		BorderPane 	patientPane = new BorderPane();
		patientPane.setTop(patientH1);
		patientPane.setLeft(patientV1);
		patientPane.setCenter(patientV2);
		
		// Scene
		Scene patientScene = new Scene(patientPane, 700, 600);
		
		// Doctor Button Functions 
		addPatient.setOnAction(e -> {
			// Whenever user explicitly opens the Add Patient form, ensure the button is in normal Add mode
			if (originalAddPatientHandler != null) {
			    addPatientTo.setText("Add");
			    addPatientTo.setOnAction(originalAddPatientHandler);
			}
			patientTf1.setEditable(true);
			patientTf7.setText("");
			
			patientV2.getChildren().clear();
			patientV2.getChildren().addAll(
					new Label("ID: "),patientTf1, 
					new Label("Name: "), patientTf2, 
					new Label("Disease: "), patientTf3, 
					new Label("Sex: "), patientTf4, 
					new Label("Admit Status: "), patientTf5, 
					new Label("Age: "), patientTf6, addPatientTo, patientTf7);
			VBox.setMargin(addPatientTo, new Insets(0, 0, 30, 200));
			VBox.setMargin(patientTf7, new Insets(0, 0, 30, 0));
		});
		
		addPatientTo.setOnAction(e -> {
		    patientTf7.setText("");

		    final String id = patientTf1.getText().trim();
		    final String name = patientTf2.getText().trim();
		    final String disease = patientTf3.getText().trim();
		    final String sex = patientTf4.getText().trim();
		    final String admitStatus = patientTf5.getText().trim();
		    final String ageStr = patientTf6.getText().trim();

		    if (id.isEmpty() || name.isEmpty() || disease.isEmpty() || sex.isEmpty() || admitStatus.isEmpty() || ageStr.isEmpty()) {
		        patientTf7.setText("Please fill in all fields.");
		        return;
		    }

		    final int age;
		    try {
		        age = Integer.parseInt(ageStr);
		        if (age < 0) throw new NumberFormatException("negative");
		    } catch (NumberFormatException nfe) {
		        patientTf7.setText("Age must be a non-negative number.");
		        return;
		    }

		    // duplicate check via repository
		    if (patientRepo.findById(id).isPresent()) {
		        patientTf7.setText("A patient with this ID already exists.");
		        return;
		    }

		    boolean ok = patientRepo.insert(new Patient(id, name, disease, sex, admitStatus, age));
		    if (!ok) {
		        patientTf7.setText("Failed to add patient.");
		        return;
		    }

		    patientTf7.setText("New PATIENT added successfully.");
		    patientTf1.clear(); patientTf2.clear(); patientTf3.clear();
		    patientTf4.clear(); patientTf5.clear(); patientTf6.clear();
		    
		    if (patientListView != null) refreshPatientList();
		});

			
		showPatient.setOnAction(e -> {
			// reset List view
			selectedPatientId = null;
			
		    patientV2.getChildren().clear();

		    Label patientHeader = new Label("  ID        Name                 Disease             Sex    Admit Status      Age");
		    patientHeader.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    patientListView = new ListView<>();
		    patientListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");
		    patientListView.setPlaceholder(new Label("No patients found."));
		    patientListView.setPrefHeight(550);
		    patientListView.setPrefWidth(700);

		    // Capture the selected patient's ID (first column)
		    patientListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
		        if (sel == null) { selectedPatientId = null; return; }
		        selectedPatientId = sel.substring(0, Math.min(10, sel.length())).trim();
		    });
		    
		    refreshPatientList();
		    patientListView.getSelectionModel().clearSelection();

		    patientV2.getChildren().addAll(patientHeader, patientListView);
		});
		
		updatePatient.setOnAction(e -> {
		    patientTf7.setText("");

		    // 1) make sure a row is selected
		    String id = selectedPatientId;
		    if ((id == null || id.isBlank()) && patientListView != null) {
		        String sel = patientListView.getSelectionModel().getSelectedItem();
		        if (sel != null) id = sel.substring(0, Math.min(10, sel.length())).trim();
		    }
		    if (id == null || id.isBlank()) {
		        patientTf7.setText("Select a patient in the list first.");
		        return;
		    }

		    var opt = patientRepo.findById(id);
		    if (opt.isEmpty()) {
		        patientTf7.setText("Selected patient no longer exists.");
		        refreshPatientList();
		        return;
		    }

		    // 2) switch to the Add Patient view (reuse your existing builder)
		    addPatient.fire();

		    // 3) prefill form, lock ID, and convert Add button into "Update Record"
		    javafx.application.Platform.runLater(() -> {
		        Patient p = opt.get();
		        selectedPatientId = p.getId();          // lock the PK we'll update

		        // prefill
		        patientTf1.setText(p.getId());
		        patientTf2.setText(p.getName());
		        patientTf3.setText(p.getDisease());
		        patientTf4.setText(p.getSex());
		        patientTf5.setText(p.getAdmitStatus());
		        patientTf6.setText(Integer.toString(p.getAge()));

		        // lock PK
		        patientTf1.setEditable(false);

		        // change button label
		        addPatientTo.setText("Update Record");

		        // store original handler (once) and replace with update handler
		        if (originalAddPatientHandler == null) {
		            originalAddPatientHandler = addPatientTo.getOnAction();
		        }

		        addPatientTo.setOnAction(ev -> {
		            patientTf7.setText("");

		            // validate
		            final String name = patientTf2.getText().trim();
		            final String disease = patientTf3.getText().trim();
		            final String sex = patientTf4.getText().trim();
		            final String admit = patientTf5.getText().trim();
		            final String ageStr = patientTf6.getText().trim();

		            if (name.isEmpty() || disease.isEmpty() || sex.isEmpty() || admit.isEmpty() || ageStr.isEmpty()) {
		                patientTf7.setText("Please fill in all fields.");
		                return;
		            }
		            final int age;
		            try { age = Integer.parseInt(ageStr); if (age < 0) throw new NumberFormatException(); }
		            catch (NumberFormatException ex) { patientTf7.setText("Age must be a non-negative number."); return; }

		            // ensure record still exists
		            if (patientRepo.findById(selectedPatientId).isEmpty()) {
		                patientTf7.setText("The patient you were editing no longer exists.");
		                // restore button and go back to list
		                addPatientTo.setText("Add");
		                addPatientTo.setOnAction(originalAddPatientHandler);
		                patientTf1.setEditable(true);
		                showPatient.fire();
		                return;
		            }

		            boolean ok = patientRepo.update(new Patient(selectedPatientId, name, disease, sex, admit, age));
		            if (!ok) {
		                patientTf7.setText("Failed to update patient.");
		                return;
		            }

		            // success
		            patientTf7.setText("Patient record updated successfully.");

		            // restore Add button behavior
		            addPatientTo.setText("Add");
		            addPatientTo.setOnAction(originalAddPatientHandler);
		            patientTf1.setEditable(true);

		            // clear form
		            patientTf1.clear(); patientTf2.clear(); patientTf3.clear();
		            patientTf4.clear(); patientTf5.clear(); patientTf6.clear();

		            // 4) switch back to Show and refresh list
		            showPatient.fire();            // navigates back to list
		            // refresh happens in Show; keep this for safety if Show is already visible
		            refreshPatientList();

		            // reset selection lock
		            selectedPatientId = null;
		        });
		    });
		});
		
		deletePatient.setOnAction(e -> {
		    patientTf7.setText("");

		    String id = (selectedPatientId != null) ? selectedPatientId : patientTf1.getText().trim();
		    if (id.isEmpty() && patientListView != null) {
		        String sel = patientListView.getSelectionModel().getSelectedItem();
		        if (sel != null) id = sel.substring(0, Math.min(10, sel.length())).trim();
		    }
		    if (id.isEmpty()) { patientTf7.setText("Select a patient or enter an ID."); return; }
		    if (patientRepo.findById(id).isEmpty()) { patientTf7.setText("No patient with this ID exists."); return; }
		    if (!confirm("Delete Patient", "Delete patient " + id + "?")) return;

		    boolean ok = patientRepo.delete(id);
		    if (!ok) {
		        patientTf7.setText("Failed to delete patient.");
		        return;
		    }

		    patientTf7.setText("Patient deleted.");

		    // Clear form and selection; unlock ID for new entries
		    patientTf1.clear(); patientTf2.clear(); patientTf3.clear();
		    patientTf4.clear(); patientTf5.clear(); patientTf6.clear();
		    patientTf1.setEditable(true);
		    selectedPatientId = null;

		    if (patientListView != null) {
		        patientListView.getSelectionModel().clearSelection();
		        patientListView.getItems().clear();
		        refreshPatientList();
		    }
		});
	
		// ----------------------------------------------------------------------------------
		// Medical menu
		
		// Buttons
		Button addMedical = new Button("Add Medical");
		Button showMedical = new Button("Show Medical");
		Button returnTo4 = new Button("Return");
		Button updateMedical = new Button("Update");
		Button deleteMedical = new Button("Delete");
		
		addMedical.setPrefWidth(100);
		showMedical.setPrefWidth(100);
		addMedical.setMinHeight(50);
		showMedical.setMinHeight(50);
		returnTo4.setPrefWidth(100);
		returnTo4.setMinHeight(50);
		updateMedical.setPrefWidth(100);
		deleteMedical.setPrefWidth(100);
		updateMedical.setMinHeight(50);
		deleteMedical.setMinHeight(50);
		
		Button addMedicalTo = new Button("Add");
		
		addMedicalTo.setPrefWidth(100);
		addMedicalTo.setMinHeight(30);
		
		
		// Text
		Text medicalTxt = new Text("Medical");
		medicalTxt.setFill(Color.WHITE);
		medicalTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));
		
		
		// Text Field
		TextField medicalTf1 = new TextField();
		TextField medicalTf2 = new TextField();
		TextField medicalTf3 = new TextField();
		TextField medicalTf4 = new TextField();
		TextField medicalTf5= new TextField();
		TextField medicalTf6 = new TextField();
		
		// HBox
		HBox medicalH1 = new HBox();
		
		medicalH1.getChildren().add(medicalTxt);
		medicalH1.setAlignment(Pos.CENTER);
		medicalH1.setSpacing(20);
		medicalH1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		medicalH1.setPrefSize(50, 50);
		
		// VBox
		VBox medicalV1 = new VBox();
		VBox medicalV2 = new VBox();
		
		medicalV1.getChildren().addAll(addMedical, showMedical, returnTo4, updateMedical, deleteMedical);
		medicalV1.setAlignment(Pos.BASELINE_CENTER);
		medicalV1.setSpacing(30);
		medicalV1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		medicalV1.setPrefSize(130, 600);
		
		medicalV2.setAlignment(Pos.TOP_LEFT);
		medicalV2.setSpacing(10);
		medicalV2.setPrefSize(470, 600);
		medicalV2.setPadding(new Insets(20));
		
		// BorderPane
		BorderPane 	medicalPane = new BorderPane();
		medicalPane.setTop(medicalH1);
		medicalPane.setLeft(medicalV1);
		medicalPane.setCenter(medicalV2);
		
		// Scene
		Scene medicalScene = new Scene(medicalPane, 700, 600);
		
		// Medical Button Functions 
		addMedical.setOnAction(e -> {
			medicalV2.getChildren().clear();
			medicalV2.getChildren().addAll(
					new Label("Name: "),medicalTf1, 
					new Label("Manufacturer: "), medicalTf2, 
					new Label("Expiry Date: "), medicalTf3, 
					new Label("Cost: "), medicalTf4, 
					new Label("Number of unit: "), medicalTf5, addMedicalTo, medicalTf6);
			VBox.setMargin(addMedicalTo, new Insets(0, 0, 30, 200));
			VBox.setMargin(medicalTf6, new Insets(0, 0, 30, 0));
		});
		
		addMedicalTo.setOnAction(e -> {
		    medicalTf6.setText("");

		    final String name = medicalTf1.getText().trim();
		    final String manufacturer = medicalTf2.getText().trim();
		    final String expiry = medicalTf3.getText().trim();   // expect YYYY-MM-DD
		    final String costStr = medicalTf4.getText().trim();
		    final String countStr = medicalTf5.getText().trim();

		    if (name.isEmpty() || manufacturer.isEmpty() || expiry.isEmpty() || costStr.isEmpty() || countStr.isEmpty()) {
		        medicalTf6.setText("Please fill in all fields.");
		        return;
		    }

		    // simple date sanity check (YYYY-MM-DD)
		    if (!expiry.matches("\\d{4}-\\d{2}-\\d{2}")) {
		        medicalTf6.setText("Expiry must be YYYY-MM-DD.");
		        return;
		    }

		    final int cost, count;
		    try {
		        cost = Integer.parseInt(costStr);
		        count = Integer.parseInt(countStr);
		        if (cost < 0 || count < 0) throw new NumberFormatException("negative");
		    } catch (NumberFormatException nfe) {
		        medicalTf6.setText("Cost and Count must be non-negative numbers.");
		        return;
		    }

		    if (medicalRepo.findByName(name).isPresent()) {
		        medicalTf6.setText("A medicine with this name already exists.");
		        return;
		    }

		    boolean ok = medicalRepo.insert(new Medical(name, manufacturer, expiry, cost, count));
		    if (!ok) { medicalTf6.setText("Failed to add medical."); return; }

		    medicalTf6.setText("New MEDICAL added successfully.");
		    medicalTf1.clear(); medicalTf2.clear(); medicalTf3.clear();
		    medicalTf4.clear(); medicalTf5.clear();
		    
		    if (medicalListView != null) refreshMedicalList();

		});
		
		showMedical.setOnAction(e -> {
		    medicalV2.getChildren().clear();

		    Label columnHeaderLabel = new Label("  Name                   Manufacturer          Expiry         Cost  Count");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // Use CLASS FIELD
		    medicalListView = new ListView<>();
		    medicalListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");
		    medicalListView.setPlaceholder(new Label("No medicines found."));
		    medicalListView.setPrefHeight(550);
		    medicalListView.setPrefWidth(700);

		    medicalListView.getItems().clear();

		    // Track selection (first column width = 22)
		    medicalListView.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, row) -> {
		        selectedMedicalName = (row == null) ? null : leading(row, 22);
		    });
		    
		    refreshMedicalList();

		    medicalV2.getChildren().addAll(columnHeaderLabel, medicalListView);
		});

		updateMedical.setOnAction(e -> {
		    medicalTf6.setText("");

		    // Resolve selected name
		    String name = selectedMedicalName;
		    if ((name == null || name.isBlank()) && medicalListView != null) {
		        String sel = medicalListView.getSelectionModel().getSelectedItem();
		        if (sel != null) name = leading(sel, 22);
		    }
		    if (name == null || name.isBlank()) { medicalTf6.setText("Select a medicine in the list first."); return; }

		    var opt = medicalRepo.findByName(name);
		    if (opt.isEmpty()) { medicalTf6.setText("Selected medicine no longer exists."); try { showMedical.fire(); } catch (Exception ignore) {} return; }
		    Medical m = opt.get();

		    // Switch to Add view
		    try { addMedical.fire(); } catch (Exception ignore) {}

		    // Prefill
		    medicalTf1.setText(ns(m.getName()));
		    medicalTf2.setText(ns(m.getManufacturer()));
		    medicalTf3.setText(ns(m.getExpiryDate()));
		    medicalTf4.setText(String.valueOf(m.getCost()));
		    medicalTf5.setText(String.valueOf(m.getCount()));

		    // Lock PK, flip button
		    medicalTf1.setEditable(false);
		    addMedicalTo.setText("Update Record");

		    if (originalAddMedicalHandler == null) originalAddMedicalHandler = addMedicalTo.getOnAction();

		    final String selectedName = name;
		    addMedicalTo.setOnAction(ev -> {
		        medicalTf6.setText("");

		        final String manufacturer = medicalTf2.getText().trim();
		        final String expiry = medicalTf3.getText().trim();
		        final String costStr = medicalTf4.getText().trim();
		        final String countStr = medicalTf5.getText().trim();

		        if (manufacturer.isEmpty() || expiry.isEmpty() || costStr.isEmpty() || countStr.isEmpty()) {
		            medicalTf6.setText("Please fill in all fields.");
		            return;
		        }
		        if (!expiry.matches("\\d{4}-\\d{2}-\\d{2}")) {
		            medicalTf6.setText("Expiry must be YYYY-MM-DD.");
		            return;
		        }
		        final int cost, count;
		        try { cost = Integer.parseInt(costStr); count = Integer.parseInt(countStr); if (cost < 0 || count < 0) throw new NumberFormatException(); }
		        catch (NumberFormatException ex) { medicalTf6.setText("Cost and Count must be non-negative numbers."); return; }

		        boolean ok = medicalRepo.update(new Medical(selectedName, manufacturer, expiry, cost, count));
		        medicalTf6.setText(ok ? "Medicine updated." : "Failed to update medicine.");

		        // Restore Add mode
		        addMedicalTo.setText("Add");
		        addMedicalTo.setOnAction(originalAddMedicalHandler);
		        medicalTf1.setEditable(true);

		        // Clear
		        medicalTf1.clear(); medicalTf2.clear(); medicalTf3.clear(); medicalTf4.clear(); medicalTf5.clear();

		        // Refresh list
		        try { showMedical.fire(); } catch (Exception ignore) {}
		        if (medicalListView != null) {
		            medicalListView.getItems().clear();
		            refreshMedicalList();
		        }

		        selectedMedicalName = null;
		    });
		});

		deleteMedical.setOnAction(e -> {
		    medicalTf6.setText("");

		    // Resolve name: tracked selection → text field → list selection (first 22 chars)
		    String name = (selectedMedicalName != null) ? selectedMedicalName : medicalTf1.getText().trim();
		    if (name.isEmpty() && medicalListView != null) {
		        String sel = medicalListView.getSelectionModel().getSelectedItem();
		        if (sel != null) name = sel.substring(0, Math.min(22, sel.length())).trim();
		    }

		    if (name.isEmpty()) { medicalTf6.setText("Select a medicine or enter a name."); return; }
		    if (medicalRepo.findByName(name).isEmpty()) { medicalTf6.setText("No medicine with this name exists."); return; }
		    if (!confirm("Delete Medicine", "Delete medicine '" + name + "'?")) return;

		    boolean ok = medicalRepo.delete(name);
		    if (!ok) { medicalTf6.setText("Failed to delete medicine."); return; }

		    medicalTf6.setText("Medicine deleted.");

		    // Clear form and selection; unlock for new entries
		    medicalTf1.clear(); medicalTf2.clear(); medicalTf3.clear(); medicalTf4.clear(); medicalTf5.clear();
		    selectedMedicalName = null;

		    // Refresh list
		    if (medicalListView != null) {
		        medicalListView.getSelectionModel().clearSelection();
		        medicalListView.getItems().clear();
		        refreshMedicalList();
		    }
		});
		
		// ----------------------------------------------------------------------------------
		// Lab Menu
		// Buttons
		Button addLab = new Button("Add Lab");
		Button showLab = new Button("Show Lab");
		Button returnTo5 = new Button("Return");
		Button updateLab = new Button("Update");
		Button deleteLab = new Button("Delete");
		
		addLab.setPrefWidth(100);
		showLab.setPrefWidth(100);
		addLab.setMinHeight(50);
		showLab.setMinHeight(50);
		returnTo5.setPrefWidth(100);
		returnTo5.setMinHeight(50);
		updateLab.setPrefWidth(100);
		deleteLab.setPrefWidth(100);
		updateLab.setMinHeight(50);
		deleteLab.setMinHeight(50);
		
		Button addLabTo = new Button("Add");
		
		addLabTo.setPrefWidth(100);
		addLabTo.setMinHeight(30);
		
		
		// Text
		Text labTxt = new Text("Lab");
		labTxt.setFill(Color.WHITE);
		labTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));
		
		
		// Text Field
		TextField labTf1 = new TextField();
		TextField labTf2 = new TextField();
		TextField labTf3 = new TextField();

		// HBox
		HBox labH1 = new HBox();
		
		labH1.getChildren().add(labTxt);
		labH1.setAlignment(Pos.CENTER);
		labH1.setSpacing(20);
		labH1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		labH1.setPrefSize(50, 50);
		
		// VBox
		VBox labV1 = new VBox();
		VBox labV2 = new VBox();
		
		labV1.getChildren().addAll(addLab, showLab, returnTo5, updateLab, deleteLab);
		labV1.setAlignment(Pos.BASELINE_CENTER);
		labV1.setSpacing(30);
		labV1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		labV1.setPrefSize(130, 600);
		
		labV2.setAlignment(Pos.TOP_LEFT);
		labV2.setSpacing(10);
		labV2.setPrefSize(470, 600);
		labV2.setPadding(new Insets(20));
		
		// BorderPane
		BorderPane 	labPane = new BorderPane();
		labPane.setTop(labH1);
		labPane.setLeft(labV1);
		labPane.setCenter(labV2);
		
		// Scene
		Scene labScene = new Scene(labPane, 700, 600);
		
		// Lab Button Functions 
		addLab.setOnAction(e -> {
			labV2.getChildren().clear();
			labV2.getChildren().addAll(
					new Label("Lab: "),labTf1,   
					new Label("Cost: "), labTf2, addLabTo, labTf3);
			VBox.setMargin(addLabTo, new Insets(0, 0, 30, 200));
			VBox.setMargin(labTf3, new Insets(0, 0, 30, 0));
		});
		
		addLabTo.setOnAction(e -> {
		    labTf3.setText(""); // status/feedback label

		    final String name = labTf1.getText().trim();   // lab name
		    final String costStr = labTf2.getText().trim(); // cost

		    if (name.isEmpty() || costStr.isEmpty()) {
		        labTf3.setText("Please fill in all fields.");
		        return;
		    }

		    final int cost;
		    try {
		        cost = Integer.parseInt(costStr);
		        if (cost < 0) throw new NumberFormatException("negative");
		    } catch (NumberFormatException nfe) {
		        labTf3.setText("Cost must be a non-negative number.");
		        return;
		    }

		    if (labRepo.findByName(name).isPresent()) {
		        labTf3.setText("A lab/test with this name already exists.");
		        return;
		    }

		    boolean ok = labRepo.insert(new Lab(name, cost));
		    if (!ok) { labTf3.setText("Failed to add lab."); return; }

		    labTf3.setText("New LAB added successfully.");
		    labTf1.clear(); labTf2.clear();
		    
		    if (labListView != null) refreshLabList();

		});

		showLab.setOnAction(e -> {
		    labV2.getChildren().clear();

		    Label columnHeaderLabel = new Label("  Lab/Test Name                 Cost");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // Class field (not local)
		    labListView = new ListView<>();
		    labListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");
		    labListView.setPlaceholder(new Label("No labs/tests found."));
		    labListView.setPrefHeight(550);
		    labListView.setPrefWidth(500);

		    // Reset stale selection + status
		    selectedLabName = null;
		    labTf3.setText("");

		    // Track selection (first column width = 28)
		    labListView.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, row) -> {
		        selectedLabName = (row == null) ? null : leading(row, 28);
		    });

		    // Populate deterministically (sorted)
		    refreshLabList();
		    labListView.getSelectionModel().clearSelection();

		    labV2.getChildren().addAll(columnHeaderLabel, labListView);
		});

		
		updateLab.setOnAction(e -> {
		    labTf3.setText("");

		    // Resolve 'name' (can be reassigned during resolution)
		    String name = selectedLabName;
		    if ((name == null || name.isBlank()) && labListView != null) {
		        String sel = labListView.getSelectionModel().getSelectedItem();
		        if (sel != null) name = leading(sel, 28);
		    }
		    if (name == null || name.isBlank()) {
		        labTf3.setText("Select a lab/test in the list first.");
		        return;
		    }

		    var opt = labRepo.findByName(name);
		    if (opt.isEmpty()) {
		        labTf3.setText("Selected lab/test no longer exists.");
		        try { showLab.fire(); } catch (Exception ignore) {}
		        return;
		    }
		    Lab l = opt.get();

		    // ✅ Make a FINAL copy BEFORE any lambda references it
		    final String resolvedName = name;

		    // Switch to Add view
		    try { addLab.fire(); } catch (Exception ignore) {}

		    // Prefill after UI is rebuilt
		    Platform.runLater(() -> {
		        labTf1.setText(ns(l.getLab()));
		        labTf2.setText(String.valueOf(l.getCost()));

		        labTf1.setEditable(false);
		        addLabTo.setText("Update Record");

		        if (originalAddLabHandler == null) originalAddLabHandler = addLabTo.getOnAction();

		        addLabTo.setOnAction(ev -> {
		            labTf3.setText("");

		            final String costStr = labTf2.getText().trim();
		            if (costStr.isEmpty()) {
		                labTf3.setText("Please fill in all fields.");
		                return;
		            }

		            final int cost;
		            try {
		                cost = Integer.parseInt(costStr);
		                if (cost < 0) throw new NumberFormatException();
		            } catch (NumberFormatException ex) {
		                labTf3.setText("Cost must be a non-negative number.");
		                return;
		            }

		            boolean ok = labRepo.update(new Lab(resolvedName, cost)); // <-- use FINAL copy
		            labTf3.setText(ok ? "Lab updated." : "Failed to update lab.");

		            // Restore Add mode
		            addLabTo.setText("Add");
		            addLabTo.setOnAction(originalAddLabHandler);
		            labTf1.setEditable(true);

		            // Clear & refresh
		            labTf1.clear(); labTf2.clear();
		            if (labListView != null) {
		                refreshLabList();
		                labListView.getSelectionModel().clearSelection();
		            }
		            selectedLabName = null;
		        });
		    });
		});
		
		deleteLab.setOnAction(e -> {
		    labTf3.setText("");

		    // Resolve name: tracked selection → text field → list selection (first 28 chars)
		    String name = (selectedLabName != null) ? selectedLabName : labTf1.getText().trim();
		    if (name.isEmpty() && labListView != null) {
		        String sel = labListView.getSelectionModel().getSelectedItem();
		        if (sel != null) name = leading(sel, 28);
		    }

		    if (name.isEmpty()) { labTf3.setText("Select a lab/test or enter a name."); return; }
		    if (labRepo.findByName(name).isEmpty()) { labTf3.setText("No lab/test with this name exists."); return; }
		    if (!confirm("Delete Lab/Test", "Delete lab/test '" + name + "'?")) return;

		    boolean ok = labRepo.delete(name);
		    if (!ok) { labTf3.setText("Failed to delete lab."); return; }

		    labTf3.setText("Lab deleted.");
		    labTf1.clear(); labTf2.clear();
		    selectedLabName = null;

		    if (labListView != null) {
		        refreshLabList();
		        labListView.getSelectionModel().clearSelection();
		    }
		});

	
		// ----------------------------------------------------------------------------------
		// Facility menu
		// Buttons
		Button addFacility = new Button("Add Facility");
		Button showFacility = new Button("Show Facility");
		Button returnTo6 = new Button("Return");
		Button updateFacility = new Button("Update");
		Button deleteFacility = new Button("Delete");
		
		addFacility.setPrefWidth(100);
		showFacility.setPrefWidth(100);
		addFacility.setMinHeight(50);
		showFacility.setMinHeight(50);
		returnTo6.setPrefWidth(100);
		returnTo6.setMinHeight(50);
		updateFacility.setPrefWidth(100);
		deleteFacility.setPrefWidth(100);
		updateFacility.setMinHeight(50);
		deleteFacility.setMinHeight(50);
		
		Button addFacilityTo = new Button("Add");
		
		addFacilityTo.setPrefWidth(100);
		addFacilityTo.setMinHeight(30);
		
		
		// Text
		Text facilityTxt = new Text("Facility");
		facilityTxt.setFill(Color.WHITE);
		facilityTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));
		
		
		// Text Field
		TextField facilityTf1 = new TextField();
		TextField facilityTf2 = new TextField();

		// HBox
		HBox facilityH1 = new HBox();
		
		facilityH1.getChildren().add(facilityTxt);
		facilityH1.setAlignment(Pos.CENTER);
		facilityH1.setSpacing(20);
		facilityH1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		facilityH1.setPrefSize(50, 50);
		
		// VBox
		VBox facilityV1 = new VBox();
		VBox facilityV2 = new VBox();
		
		facilityV1.getChildren().addAll(addFacility, showFacility, returnTo6, updateFacility, deleteFacility);
		facilityV1.setAlignment(Pos.BASELINE_CENTER);
		facilityV1.setSpacing(30);
		facilityV1.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), Insets.EMPTY)));
		facilityV1.setPrefSize(130, 600);
		
		facilityV2.setAlignment(Pos.TOP_LEFT);
		facilityV2.setSpacing(10);
		facilityV2.setPrefSize(470, 600);
		facilityV2.setPadding(new Insets(20));
		
		// BorderPane
		BorderPane facilityPane = new BorderPane();
		facilityPane.setTop(facilityH1);
		facilityPane.setLeft(facilityV1);
		facilityPane.setCenter(facilityV2);
		
		// Scene
		Scene facilityScene = new Scene(facilityPane, 700, 600);
		
		// Staff Button Functions 
		addFacility.setOnAction(e -> {
			facilityV2.getChildren().clear();
			facilityV2.getChildren().addAll(  
					new Label("Facility: "), facilityTf1, addFacilityTo, facilityTf2);
			VBox.setMargin(addFacilityTo, new Insets(0, 0, 30, 200));
			VBox.setMargin(facilityTf2, new Insets(0, 0, 30, 0));
		});
		
		addFacilityTo.setOnAction(e -> {
		    facilityTf2.setText(""); // status/feedback label

		    final String name = facilityTf1.getText().trim(); // facility name

		    if (name.isEmpty()) {
		        facilityTf2.setText("Please enter a facility name.");
		        return;
		    }

		    if (facilityRepo.findByName(name).isPresent()) {
		        facilityTf2.setText("This facility already exists.");
		        return;
		    }

		    boolean ok = facilityRepo.insert(new Facility(name));
		    if (!ok) { facilityTf2.setText("Failed to add facility."); return; }

		    facilityTf2.setText("New FACILITY added successfully.");
		    facilityTf1.clear();
		    
		    if (facilityListView != null) refreshFacilityList();

		});
	
		showFacility.setOnAction(e -> {
		    facilityV2.getChildren().clear();

		    Label columnHeaderLabel = new Label("  Facility Name");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // Use CLASS FIELD
		    facilityListView = new ListView<>();
		    facilityListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");
		    facilityListView.setPlaceholder(new Label("No facilities found."));
		    facilityListView.setPrefHeight(550);
		    facilityListView.setPrefWidth(500);

		    facilityListView.getItems().clear();
//		    for (Facility f : facilityRepo.findAll()) {
//		        facilityListView.getItems().add(ns(f.showFacility())); // f.getFacility() if you prefer
//		    }

		    // Track selection (single column; whole row is the name)
		    facilityListView.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, row) -> {
		        selectedFacilityName = (row == null) ? null : row.trim();
		    });
		    
		    refreshFacilityList();

		    facilityV2.getChildren().addAll(columnHeaderLabel, facilityListView);
		});
		
		updateFacility.setOnAction(e -> {
		    facilityTf2.setText("");

		    // Resolve selected facility name
		    String name = selectedFacilityName;
		    if ((name == null || name.isBlank()) && facilityListView != null) {
		        String sel = facilityListView.getSelectionModel().getSelectedItem();
		        if (sel != null) name = sel.trim();
		    }
		    if (name == null || name.isBlank()) {
		        facilityTf2.setText("Select a facility in the list first.");
		        return;
		    }

		    var opt = facilityRepo.findByName(name);
		    if (opt.isEmpty()) {
		        facilityTf2.setText("Selected facility no longer exists.");
		        try { showFacility.fire(); } catch (Exception ignore) {}
		        return;
		    }
		    Facility f = opt.get();

		    // ✅ FINAL copy BEFORE lambda
		    final String oldName = name;

		    // Switch to Add view
		    try { addFacility.fire(); } catch (Exception ignore) {}

		    // Prefill (ALLOW EDIT so rename is possible)
		    facilityTf1.setText(ns(f.showFacility())); // or f.getFacility()
		    facilityTf1.setEditable(true);

		    addFacilityTo.setText("Update Record");
		    if (originalAddFacilityHandler == null) originalAddFacilityHandler = addFacilityTo.getOnAction();

		    addFacilityTo.setOnAction(ev -> {
		        facilityTf2.setText("");

		        String newName = facilityTf1.getText().trim();
		        if (newName.isEmpty()) {
		            facilityTf2.setText("Facility name cannot be empty.");
		            return;
		        }
		        if (!newName.equals(oldName) && facilityRepo.findByName(newName).isPresent()) {
		            facilityTf2.setText("This facility already exists.");
		            return;
		        }

		        // Persist: if no direct update, do delete+insert
		        boolean ok = newName.equals(oldName)
		                ? true
		                : (facilityRepo.delete(oldName) && facilityRepo.insert(new Facility(newName)));

		        facilityTf2.setText(ok ? "Facility updated." : "Failed to update facility.");

		        // Restore Add mode
		        addFacilityTo.setText("Add");
		        addFacilityTo.setOnAction(originalAddFacilityHandler);
		        facilityTf1.setEditable(true);

		        // Clear & refresh
		        facilityTf1.clear();
		        if (facilityListView != null) {
		            refreshFacilityList();
		            facilityListView.getSelectionModel().clearSelection();
		        }
		        selectedFacilityName = null;
		    });
		});
		
		deleteFacility.setOnAction(e -> {
		    facilityTf2.setText("");

		    // 1) Resolve the facility name (selection → text field → list fallback)
		    String name = (selectedFacilityName != null) ? selectedFacilityName : facilityTf1.getText().trim();
		    if (name.isEmpty() && facilityListView != null) {
		        String sel = facilityListView.getSelectionModel().getSelectedItem();
		        if (sel != null) name = sel.trim();
		    }

		    if (name.isEmpty()) { facilityTf2.setText("Select a facility or enter a name."); return; }
		    if (facilityRepo.findByName(name).isEmpty()) { facilityTf2.setText("No facility with this name exists."); return; }

		    // 2) Confirm
		    if (!confirm("Delete Facility", "Delete facility '" + name + "'?")) return;

		    // 3) Delete (repo method name may differ; pick the one you have)
		    boolean ok;
		    try {
		        ok = facilityRepo.delete(name);        // or: ok = facilityRepo.deleteByName(name);
		    } catch (Exception ex) {
		        ok = false;
		    }
		    if (!ok) { facilityTf2.setText("Failed to delete facility."); return; }

		    facilityTf2.setText("Facility deleted.");

		    // 4) If we were in Update mode, restore Add mode and unlock field
		    if ("Update Record".equals(addFacilityTo.getText())) {
		        addFacilityTo.setText("Add");
		        if (originalAddFacilityHandler != null) addFacilityTo.setOnAction(originalAddFacilityHandler);
		        facilityTf1.setEditable(true);
		    }

		    // 5) Clear form + selection
		    facilityTf1.clear();
		    selectedFacilityName = null;

		    // 6) Refresh the list deterministically (sorted)
		    if (facilityListView != null) {
		        refreshFacilityList();
		        facilityListView.getSelectionModel().clearSelection();
		    }
		});

	
		// ----------------------------------------------------------------------------------
		// Button functions
		mainBt1.setOnAction(e -> {
			primaryStage.setTitle("Staff Menu");
			primaryStage.setScene(staffScene);
		});
		
		mainBt2.setOnAction(e -> {
			primaryStage.setTitle("Doctor Menu");
			primaryStage.setScene(doctorScene);
		});
		
		mainBt3.setOnAction(e -> {
			primaryStage.setTitle("Patient Menu");
			primaryStage.setScene(patientScene);
		});
		
		mainBt4.setOnAction(e -> {
			primaryStage.setTitle("Medical Menu");
			primaryStage.setScene(medicalScene);
		});
		
		mainBt5.setOnAction(e -> {
			primaryStage.setTitle("Lab Menu");
			primaryStage.setScene(labScene);
		});
		
		mainBt6.setOnAction(e -> {
		    primaryStage.setTitle("Facility Menu");
		    primaryStage.setScene(facilityScene);
		});
	
		returnTo1.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});
		
		returnTo2.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});
		
		returnTo3.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});
		
		returnTo4.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});
		
		returnTo5.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});
		
		returnTo6.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});
	}
}
