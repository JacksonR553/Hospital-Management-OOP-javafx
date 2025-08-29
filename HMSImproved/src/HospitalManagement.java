import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import javafx.application.Application;
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
	private Doctor[] doctors = new Doctor[25];
	private Patient[] patients = new Patient[100];
    private Lab[] labs = new Lab[20];
    private Facility[] facilities = new Facility[20];
    private Medical[] medicals = new Medical[100];
    private Staff[] staffs = new Staff[100];
	// --- App layout constants ---
	private static final double APP_WIDTH  = 1000;
	private static final double APP_HEIGHT = 700;
	// persistence
	private PatientRepository patientRepo;
	private DoctorRepository doctorRepo;
	private StaffRepository staffRepo;
	private MedicalRepository medicalRepo;
	private LabRepository labRepo;
	
	private static String ns(String s) { return (s == null) ? "" : s; }
	
    private void initializeStaffs() {
    	staffs[0] = new Staff("412", "Lim Boon Chong", "Nurse", "Female", 3000);
    	staffs[1] = new Staff("348", "Teoh Ming Xue", "Receptionist", "Male", 2500);
    	staffs[2] = new Staff("934", "Lim Jie Yew", "Janitor", "Male", 2000);
    }
    
	public void initializeDoctors() {
        doctors[0] = new Doctor("412", "Dr. Lim Boon Chong", "Surgeon", "8-11AM", "MBBS,MD", 11);
        doctors[1] = new Doctor("348", "Dr. Teoh Ming Xue", "Physician", "10-3AM", "MBBS,MS", 45);
        doctors[2] = new Doctor("934", "Dr. Lim Jie Yew", "Surgeon", "7-11AM", "MBBS,MD", 8);
    }
	
	private void initializePatients() {
    	patients[0] = new Patient("412", "Lim Boon Chong", "Fever", "Female", "Admitted", 30);
        patients[1] = new Patient("348", "Teoh Ming Xue", "Broken Arm", "Male", "Admitted", 25);
        patients[2] = new Patient("934", "Lim Jie Yew", "Headache", "Female", "Discharged", 40);
    }

	private void initializeMedicals() {
		medicals[0] = new Medical("Aspirin", "Pfizer", "2023-12-31", 5, 100);
		medicals[1] = new Medical("Ibuprofen", "Novartis", "2023-11-30", 8, 150);
		medicals[2] = new Medical("Amoxicillin", "Roche", "2024-02-28", 12, 80);
	}
	
    private void initializeLabs() {
    	labs[0] = new Lab("Lab A", 2000);
        labs[1] = new Lab("Lab B", 1500);
        labs[2] = new Lab("Lab C", 1800);
    }

    private void initializeFacilities() {
    	facilities[0] = new Facility("MRI Room");
        facilities[1] = new Facility("X-ray Room");
        facilities[2] = new Facility("Surgery Theater");
    }

    private void initialize() {
        initializeDoctors();
        initializePatients();
        initializeLabs();
        initializeFacilities();
        initializeMedicals();
        initializeStaffs();
    }
    
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


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		initialize();
		
		// database
		Db.bootstrap();
		patientRepo = new SqlPatientRepository(Db.get());
		doctorRepo = new SqlDoctorRepository(Db.get());
		staffRepo = new SqlStaffRepository(Db.get());
		medicalRepo = new SqlMedicalRepository(Db.get());
		labRepo = new SqlLabRepository(Db.get());
		
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
		
		addStaff.setPrefWidth(100);
		showStaff.setPrefWidth(100);
		returnTo1.setPrefWidth(100);
		addStaff.setMinHeight(50);
		showStaff.setMinHeight(50);
		returnTo1.setMinHeight(50);
		
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
		
		staffV1.getChildren().addAll(addStaff, showStaff, returnTo1);
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
		});

		
		showStaff.setOnAction(e -> {
		    staffV2.getChildren().clear();

		    Label columnHeaderLabel = new Label("  ID        Name                 Designation         Sex     Salary");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    ListView<String> staffListView = new ListView<>();
		    staffListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    staffListView.getItems().clear();
		    for (Staff s : staffRepo.findAll()) {
		        String row = String.format("%-10s%-20s%-20s%-8s%8d",
		                s.getId(), s.getName(), s.getDesignation(), s.getSex(), s.getSalary());
		        staffListView.getItems().add(row);
		    }

		    staffListView.setPlaceholder(new Label("No staff found."));
		    staffListView.setPrefHeight(550);
		    staffListView.setPrefWidth(700);

		    staffV2.getChildren().addAll(columnHeaderLabel, staffListView);
		});

		
		// ----------------------------------------------------------------------------------
		// Doctor Menu
		// Buttons
		Button addDoctor = new Button("Add Doctor");
		Button showDoctor = new Button("Show Doctor");
		Button returnTo2 = new Button("Return");
		
		addDoctor.setPrefWidth(100);
		showDoctor.setPrefWidth(100);
		addDoctor.setMinHeight(50);
		showDoctor.setMinHeight(50);
		returnTo2.setPrefWidth(100);
		returnTo2.setMinHeight(50);
		
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
		
		doctorV1.getChildren().addAll(addDoctor, showDoctor, returnTo2);
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
		});

		
		showDoctor.setOnAction(e -> {
		    doctorV2.getChildren().clear();

		    // Monospace header so rows align
		    Label columnHeaderLabel = new Label("  ID        Name                 Specialist           Work Time   Qualification     Room");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // List view in monospace as well
		    ListView<String> doctorListView = new ListView<>();
		    doctorListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // Load from repository (DB) instead of the doctors[] array
		    doctorListView.getItems().clear();
		    for (Doctor d : doctorRepo.findAll()) {
		        String row = String.format("%-10s%-20s%-22s%-12s%-18s%6d",
		                d.getId(),
		                d.getName(),
		                d.getSpecialist(),
		                d.getWorkTime(),
		                d.getQualification(),
		                d.getRoom());
		        doctorListView.getItems().add(row);
		    }

		    doctorListView.setPrefHeight(550);
		    doctorListView.setPrefWidth(700); // wider to fit columns nicely
		    doctorV2.getChildren().addAll(columnHeaderLabel, doctorListView);
		});

		
		// ----------------------------------------------------------------------------------
		// Patient Menu
		
		// Buttons
		Button addPatient = new Button("Add Patient");
		Button showPatient = new Button("Show Patient");
		Button returnTo3 = new Button("Return");
		
		addPatient.setPrefWidth(100);
		showPatient.setPrefWidth(100);
		addPatient.setMinHeight(50);
		showPatient.setMinHeight(50);
		returnTo3.setPrefWidth(100);
		returnTo3.setMinHeight(50);
		
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
		
		patientV1.getChildren().addAll(addPatient, showPatient, returnTo3);
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
		});

			
		showPatient.setOnAction(e -> {
		    patientV2.getChildren().clear();

		    // Monospace header so columns line up
		    Label columnHeaderLabel = new Label("  ID        Name                 Disease             Sex    Admit Status      Age");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    ListView<String> patientListView = new ListView<>();
		    patientListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    // Load from repository (DB)
		    patientListView.getItems().clear();
		    for (Patient p : patientRepo.findAll()) {
		        String row = String.format("%-10s%-20s%-22s%-8s%-18s%5d",
		                ns(p.getId()),
		                ns(p.getName()),
		                ns(p.getDisease()),
		                ns(p.getSex()),
		                ns(p.getAdmitStatus()),
		                p.getAge());
		        patientListView.getItems().add(row);
		    }

		    patientListView.setPlaceholder(new Label("No patients found."));
		    patientListView.setPrefHeight(550);
		    patientListView.setPrefWidth(700); // wider so all columns fit

		    patientV2.getChildren().addAll(columnHeaderLabel, patientListView);
		});

				
		// ----------------------------------------------------------------------------------
		// Medical menu
		
		// Buttons
		Button addMedical = new Button("Add Medical");
		Button showMedical = new Button("Show Medical");
		Button returnTo4 = new Button("Return");
		
		addMedical.setPrefWidth(100);
		showMedical.setPrefWidth(100);
		addMedical.setMinHeight(50);
		showMedical.setMinHeight(50);
		returnTo4.setPrefWidth(100);
		returnTo4.setMinHeight(50);
		
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
		
		medicalV1.getChildren().addAll(addMedical, showMedical, returnTo4);
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
		});
		
		showMedical.setOnAction(e -> {
		    medicalV2.getChildren().clear();

		    Label columnHeaderLabel = new Label("  Name                 Manufacturer         Expiry       Cost   Count");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    ListView<String> medicalListView = new ListView<>();
		    medicalListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    medicalListView.getItems().clear();
		    for (Medical m : medicalRepo.findAll()) {
		        String row = String.format("%-22s%-22s%-13s%7d%7d",
		                ns(m.getName()),
		                ns(m.getManufacturer()),
		                ns(m.getExpiryDate()),
		                m.getCost(),
		                m.getCount());
		        medicalListView.getItems().add(row);
		    }

		    medicalListView.setPlaceholder(new Label("No medicines found."));
		    medicalListView.setPrefHeight(550);
		    medicalListView.setPrefWidth(700);

		    medicalV2.getChildren().addAll(columnHeaderLabel, medicalListView);
		});
		
		// ----------------------------------------------------------------------------------
		// Lab Menu
		// Buttons
		Button addLab = new Button("Add Lab");
		Button showLab = new Button("Show Lab");
		Button returnTo5 = new Button("Return");
		
		addLab.setPrefWidth(100);
		showLab.setPrefWidth(100);
		addLab.setMinHeight(50);
		showLab.setMinHeight(50);
		returnTo5.setPrefWidth(100);
		returnTo5.setMinHeight(50);
		
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
		
		labV1.getChildren().addAll(addLab, showLab, returnTo5);
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
		});

		showLab.setOnAction(e -> {
		    labV2.getChildren().clear();

		    Label columnHeaderLabel = new Label("  Lab/Test Name              Cost");
		    columnHeaderLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    ListView<String> labListView = new ListView<>();
		    labListView.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12;");

		    labListView.getItems().clear();
		    for (Lab l : labRepo.findAll()) {
		        String row = String.format("%-28s%8d",
		                (l.getLab() == null ? "" : l.getLab()),
		                l.getCost());
		        labListView.getItems().add(row);
		    }

		    labListView.setPlaceholder(new Label("No labs/tests found."));
		    labListView.setPrefHeight(550);
		    labListView.setPrefWidth(500);

		    labV2.getChildren().addAll(columnHeaderLabel, labListView);
		});
	
		// ----------------------------------------------------------------------------------
		// Facility menu
		// Buttons
		Button addFacility = new Button("Add Facility");
		Button showFacility = new Button("Show Facility");
		Button returnTo6 = new Button("Return");
		
		addFacility.setPrefWidth(100);
		showFacility.setPrefWidth(100);
		addFacility.setMinHeight(50);
		showFacility.setMinHeight(50);
		returnTo6.setPrefWidth(100);
		returnTo6.setMinHeight(50);
		
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
		
		facilityV1.getChildren().addAll(addFacility, showFacility, returnTo6);
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
		    facilityTf2.setText("");

		    final String name = facilityTf1.getText().trim();
		    if (name.isEmpty()) {
		        facilityTf2.setText("Please enter a facility name.");
		        return;
		    }

		    // duplicate by facility name
		    for (Facility f : facilities) {
		        if (f != null && name.equalsIgnoreCase(f.getFacility())) {
		            facilityTf2.setText("A facility with this name already exists.");
		            return;
		        }
		    }

		    boolean inserted = false;
		    Facility newFacility = new Facility(name);
		    for (int i = 0; i < facilities.length; i++) {
		        if (facilities[i] == null) { facilities[i] = newFacility; inserted = true; break; }
		    }

		    if (!inserted) { facilityTf2.setText("Facility list is full. Unable to add."); return; }

		    facilityTf2.setText("New FACILITY added successfully.");
		    facilityTf1.clear();
		});

		
		showFacility.setOnAction(e -> {
			facilityV2.getChildren().clear();
			Label columnHeaderLabel = new Label(" Facility");
			ListView<String> facilityListView = new ListView<>();
			facilityListView.setStyle("-fx-font-family: 'Courier New';");
			columnHeaderLabel.setStyle("-fx-font-family: 'Courier New';");
			for(Facility facility: facilities) {
				if(facility != null) {
					facilityListView.getItems().add(facility.showFacility());
				}
			}
			facilityListView.setPrefHeight(550);
			facilityListView.setPrefWidth(470);
			facilityV2.getChildren().addAll(columnHeaderLabel, facilityListView);
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
			primaryStage.setTitle("Lab Menu");
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
