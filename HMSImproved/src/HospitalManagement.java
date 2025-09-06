import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SelectionMode;
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
import javafx.scene.chart.*;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.stage.Modality;

public class HospitalManagement extends Application {
	// ===== Theme constants (black & white with optional subtle greys) =====
	private static final String BG_WHITE = "#FFFFFF";
	private static final String FG_BLACK = "#000000";
	private static final String GREY_BG  = "#F2F2F2"; // allowed subtle grey for list background
	private static final String FONT_FAMILY = "'Inter', 'Roboto', 'Arial', sans-serif";

	private static final double BTN_MIN_W = 160;
	private static final double BTN_MIN_H = 38;
	private static final double TF_MIN_W  = 300;
	private static final double TF_MIN_H  = 32;
	private static final double SPACING   = 10;
	private static final Insets PAD       = new Insets(16);

	private Stage primaryStage;
	private Scene mainScene;

	// DataSource / repositories (created once)
	private DataSource ds;

	private SqlPatientRepository patientRepo;
	private SqlDoctorRepository doctorRepo;
	private SqlStaffRepository staffRepo;
	private SqlMedicalRepository medicalRepo;
	private SqlLabRepository labRepo;
	private SqlFacilityRepository facilityRepo;
	private AuditLogRepository auditRepo;
	
	// ===== Consistent colors by entity (shared across charts) =====
	private static final java.util.Map<String, String> ENTITY_COLORS = java.util.Map.of(
	    "Patients",   "#1f77b4",
	    "Doctors",    "#ff7f0e",
	    "Staff",      "#2ca02c",
	    "Medical",    "#d62728",
	    "Labs",       "#9467bd",
	    "Facilities", "#8c564b"
	);

	// ===== Shared UI state used by multiple handlers =====
	// --- Toast/alerts infra ---
	private StackPane mainRoot;              // main menu root so we can overlay toasts
	private VBox toastArea;                  // bottom-right toast stack
	private final java.util.ArrayDeque<Runnable> toastQueue = new java.util.ArrayDeque<>();
	// serialize lightweightChecks so it never runs concurrently
	private final java.util.concurrent.locks.ReentrantLock checkLock = new java.util.concurrent.locks.ReentrantLock();
	private boolean goToMedicalListAfterOpen = false;  // navigation flag after opening Medical UI

	// A tiny DTO for notifications we fetch from DB
	private static final class NotificationRow {
	    final int id; final String severity; final String title; final String detail;
	    NotificationRow(int id, String sev, String t, String d) { this.id = id; this.severity = sev; this.title = t; this.detail = d; }
	}
	
	// Patient section state (IDs selected from ListView etc.)
	private ListView<String> patientListView;
	private String selectedPatientId;

	// Staff section
	private ListView<String> staffListView;
	private String selectedStaffId;

	// Doctor section
	private ListView<String> doctorListView;
	private String selectedDoctorId;

	// Medical section
	private ListView<String> medicalListView;
	private String selectedMedicalId;

	// Lab section
	private ListView<String> labListView;
	private String selectedLabId;

	// Facility section
	private ListView<String> facilityListView;
	private String selectedFacilityId;

	// Keep original "Add" handlers so we can restore after "Update" flow swaps
	private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddPatientHandler;
	private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddStaffHandler;
	private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddDoctorHandler;
	private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddMedicalHandler;
	private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddLabHandler;
	private javafx.event.EventHandler<javafx.event.ActionEvent> originalAddFacilityHandler;

	private static String ns(String s) { return s == null ? "" : s; }

	// ===== Shared helpers =====
	private static Label titleLabel(String text) {
	    Label l = new Label(text);
	    l.setStyle("-fx-text-fill: " + FG_BLACK + "; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: " + FONT_FAMILY + ";");
	    return l;
	}

	private static Button menuButton(String text) {
	    Button b = new Button(text);
	    b.setMinWidth(BTN_MIN_W);
	    b.setMinHeight(BTN_MIN_H);
	    b.setStyle(
	        "-fx-background-color: " + BG_WHITE + ";" +
	        "-fx-text-fill: " + FG_BLACK + ";" +
	        "-fx-border-color: " + FG_BLACK + ";" +
	        "-fx-border-width: 1px;" +
	        "-fx-font-family: " + FONT_FAMILY + ";" +
	        "-fx-font-size: 13px;"
	    );
	    return b;
	}

	private static TextField textField(String prompt) {
	    TextField tf = new TextField();
	    tf.setPromptText(prompt);
	    tf.setMinWidth(TF_MIN_W);
	    tf.setMinHeight(TF_MIN_H);
	    tf.setStyle(
	        "-fx-background-color: " + BG_WHITE + ";" +
	        "-fx-text-fill: " + FG_BLACK + ";" +
	        "-fx-border-color: " + FG_BLACK + ";" +
	        "-fx-border-width: 1px;" +
	        "-fx-font-family: " + FONT_FAMILY + ";" +
	        "-fx-font-size: 13px;"
	    );
	    return tf;
	}

	private static String getCurrentDateTime() {
	    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm:ss");
	    return LocalDateTime.now().format(dtf);
	}

	private ListView<String> makeBWListView() {
	    ListView<String> lv = new ListView<>();
	    lv.setStyle(
	        "-fx-control-inner-background: " + GREY_BG + ";" +
	        "-fx-font-family: 'Courier New', monospace;" +
	        "-fx-text-fill: " + FG_BLACK + ";" +
	        "-fx-border-color: " + FG_BLACK + ";" +
	        "-fx-border-width: 1px;"
	    );
	    return lv;
	}

	private boolean confirm(String title, String message) {
	    // If you already have a shared confirm(...) for Medical, reuse that and delete this.
	    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
	    a.setTitle(title);
	    a.setHeaderText(title);
	    a.setContentText(message);
	    var res = a.showAndWait();
	    return res.isPresent() && res.get() == javafx.scene.control.ButtonType.OK;
	}
	
	private void showAuditLogPopup() {
	    // small, read-only window with recent log lines
	    ListView<String> lv = makeBWListView();
	    lv.setMinHeight(260);
	    lv.setStyle(lv.getStyle() + "-fx-font-size: 12px;"); // compact

	    Label header = new Label("TIMESTAMP                                             TABLE              ACTION           ENTITY");
	    header.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold;");
	    BorderPane.setMargin(header, new Insets(8));

	    lv.getItems().clear();
	    if (auditRepo != null) {
	        var logs = auditRepo.findRecent(120); // last 120 entries
	        for (AuditLog a : logs) {
	            String ts = ns(a.getTs());
	            if (ts.length() > 23) ts = ts.substring(0, 23);
	            String row = String.format("%-30s %-10s %-10s %-10s",
	                    ts,
	                    ns(a.getTableName()),
	                    ns(a.getAction()),
	                    ns(a.getEntityId()));
	            lv.getItems().add(row);
	        }
	    }

	    BorderPane pane = new BorderPane();
	    pane.setStyle("-fx-background-color: #FFFFFF;");
	    pane.setTop(header);
	    pane.setCenter(lv);

	    Stage s = new Stage();
	    s.initOwner(primaryStage);
	    s.initModality(Modality.NONE);
	    s.setResizable(false);
	    s.setTitle("Audit Log");
	    s.setScene(new Scene(pane, 520, 320));
	    s.show();
	}

	// ----------------------------------------------------------------------------------
	// Utility to help parse fixed-width rows shown in the ListView
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
	
	// ---- Simple HMS scheduler ----
	private final java.util.concurrent.ScheduledExecutorService exec =
	    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
	        Thread t = new Thread(r, "hms-scheduler");
	        t.setDaemon(true);
	        return t;
	    });

	private void startJobs() {
	    // Every 60s: run low-stock and expiring-med checks
	    exec.scheduleAtFixedRate(this::lightweightChecks, 30, 60, java.util.concurrent.TimeUnit.SECONDS);
	}

	private void lightweightChecks() {
	    if (!checkLock.tryLock()) {
	        // a previous run is still in progress; skip this tick
	        return;
	    }
	    try (var c = Db.get().getConnection()) {
	        // avoid immediate SQLITE_BUSY
	        try (var st = c.createStatement()) {
	            st.execute("PRAGMA busy_timeout = 3000"); // 3s
	        }

	        c.setAutoCommit(false);
	        try (
	            var sLow = c.createStatement();
	            var sExp = c.createStatement();
	            var psIns = c.prepareStatement(
	                "INSERT INTO notification(severity,title,detail) " +
	                "SELECT ?, ?, ? WHERE NOT EXISTS (" +
	                "  SELECT 1 FROM notification WHERE severity=? AND title=? AND detail=? AND seen=0" +
	                ")"
	            )
	        ) {
	            // Low stock
	            try (var rs = sLow.executeQuery("SELECT id,name,count FROM medical WHERE count <= 10")) {
	                while (rs.next()) {
	                    String id = rs.getString("id");
	                    String name = rs.getString("name");
	                    int count = rs.getInt("count");
	                    String title  = "Low stock: " + name;
	                    String detail = "ID " + id + ", count=" + count;

	                    psIns.setString(1, "WARN");
	                    psIns.setString(2, title);
	                    psIns.setString(3, detail);
	                    // de-dupe same unseen notification
	                    psIns.setString(4, "WARN");
	                    psIns.setString(5, title);
	                    psIns.setString(6, detail);
	                    psIns.addBatch();
	                }
	            }

	            // Expiring within 30 days
	            try (var rs = sExp.executeQuery(
	                "SELECT id,name,expiry_date FROM medical " +
	                "WHERE date(expiry_date) <= date('now','+30 day')"
	            )) {
	                while (rs.next()) {
	                    String id = rs.getString("id");
	                    String name = rs.getString("name");
	                    String expiry = rs.getString("expiry_date");
	                    String title  = "Expiring soon: " + name;
	                    String detail = "ID " + id + ", expires " + expiry;

	                    psIns.setString(1, "INFO");
	                    psIns.setString(2, title);
	                    psIns.setString(3, detail);
	                    psIns.setString(4, "INFO");
	                    psIns.setString(5, title);
	                    psIns.setString(6, detail);
	                    psIns.addBatch();
	                }
	            }

	            psIns.executeBatch();
	            c.commit();
	        } catch (Exception e) {
	            try { c.rollback(); } catch (Exception ignore) {}
	            throw e;
	        }
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    } finally {
	        checkLock.unlock();
	    }
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;

		// Initialize Database
		Db.bootstrap();
	    patientRepo  = new SqlPatientRepository(Db.get());
	    doctorRepo   = new SqlDoctorRepository(Db.get());
	    staffRepo    = new SqlStaffRepository(Db.get());
	    medicalRepo  = new SqlMedicalRepository(Db.get());
	    labRepo      = new SqlLabRepository(Db.get());
	    facilityRepo = new SqlFacilityRepository(Db.get());
	    this.auditRepo = new SqlAuditLogRepository(Db.get());

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
		        "-fx-background-color: #FFFFFF;" +
		        "-fx-text-fill: #000000;" +
		        "-fx-border-color: #000000;" +
		        "-fx-border-width: 1px;" +
		        "-fx-font-size: 16px;" +
		        "-fx-font-weight: 700;" +
		        "-fx-background-radius: 12;" +
		        "-fx-border-radius: 12;" +
		        "-fx-cursor: hand;"
		    );
		    b.setOnMouseEntered(e -> b.setStyle(
		        "-fx-background-color: #000000;" +
		        "-fx-text-fill: #FFFFFF;" +
		        "-fx-border-color: #000000;" +
		        "-fx-border-width: 1px;" +
		        "-fx-font-size: 16px;" +
		        "-fx-font-weight: 700;" +
		        "-fx-background-radius: 12;" +
		        "-fx-border-radius: 12;" +
		        "-fx-cursor: hand;"
		    ));
		    b.setOnMouseExited(e -> b.setStyle(
		        "-fx-background-color: #FFFFFF;" +
		        "-fx-text-fill: #000000;" +
		        "-fx-border-color: #000000;" +
		        "-fx-border-width: 1px;" +
		        "-fx-font-size: 16px;" +
		        "-fx-font-weight: 700;" +
		        "-fx-background-radius: 12;" +
		        "-fx-border-radius: 12;" +
		        "-fx-cursor: hand;"
		    ));
		}

		// Title + live time
		Text mainTitle = new Text("Hospital Management System");
		mainTitle.setStyle("-fx-fill: #000000; -fx-font-size: 24px; -fx-font-weight: 800; -fx-font-family: " + FONT_FAMILY + ";");

		Text timeTxt = new Text(getCurrentDateTime());
		timeTxt.setStyle("-fx-fill: #000000; -fx-font-size: 13px;"); // black for minimal theme

		// live clock
		Timeline clock = new Timeline(
		    new KeyFrame(Duration.ZERO, e -> timeTxt.setText(getCurrentDateTime())),
		    new KeyFrame(Duration.seconds(1))
		);
		clock.setCycleCount(Timeline.INDEFINITE);
		clock.play();

		// Layout: plain white background + centered "card"
		StackPane root = new StackPane();
		root.setStyle(
		    "-fx-background-color: #FFFFFF;" // plain white
		);

		VBox card = new VBox(24);
		card.setAlignment(Pos.CENTER);
		card.setStyle(
		    "-fx-background-color: #FFFFFF;" +
		    "-fx-background-radius: 16;" +
		    "-fx-border-color: #000000;" +
		    "-fx-border-width: 1px;" +
		    "-fx-border-radius: 16;"
		);
		card.setPadding(new Insets(32));

		// Header row
		HBox header = new HBox(16);
		header.setAlignment(Pos.CENTER);
		header.getChildren().addAll(mainTitle, timeTxt);

		// Main grid (2 columns x 3 rows of buttons)
		GridPane grid = new GridPane();
		grid.setHgap(16);
		grid.setVgap(16);
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
		
		// ---- tiny Audit Log button in the bottom-right corner of the main menu ----
		Button auditBtn = new Button("Audit Log");
		auditBtn.setMinWidth(100);
		auditBtn.setMinHeight(32);
		auditBtn.setStyle(
		    "-fx-background-color: #FFFFFF;" +
		    "-fx-text-fill: #000000;" +
		    "-fx-border-color: #000000;" +
		    "-fx-border-width: 1px;" +
		    "-fx-font-size: 12px;" +
		    "-fx-font-weight: 600;" +
		    "-fx-background-radius: 8;" +
		    "-fx-border-radius: 8;"
		);
		auditBtn.setOnAction(e -> showAuditLogPopup());

		// place it in the corner
		root.getChildren().add(auditBtn);
		StackPane.setAlignment(auditBtn, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(auditBtn, new Insets(0, 12, 12, 0));
		
		// ---- tiny Dashboard button in the bottom-left corner (same style as Audit Log) ----
		Button dashboardBtn = new Button("Dashboard");
		dashboardBtn.setMinWidth(100);
		dashboardBtn.setMinHeight(32);
		dashboardBtn.setStyle(
		    "-fx-background-color: #FFFFFF;" +
		    "-fx-text-fill: #000000;" +
		    "-fx-border-color: #000000;" +
		    "-fx-border-width: 1px;" +
		    "-fx-font-size: 12px;" +
		    "-fx-font-weight: 600;" +
		    "-fx-background-radius: 8;" +
		    "-fx-border-radius: 8;"
		);
		dashboardBtn.setOnAction(e -> showDashboardPopup());

		root.getChildren().add(dashboardBtn);
		StackPane.setAlignment(dashboardBtn, Pos.BOTTOM_LEFT);
		StackPane.setMargin(dashboardBtn, new Insets(0, 0, 12, 12));
		
		// --- toast overlay mount (bottom-right queue) ---
		this.mainRoot = root;

		toastArea = new VBox(8);
		toastArea.setPickOnBounds(false);
		toastArea.setMouseTransparent(false);
		toastArea.setPadding(new Insets(0, 12, 12, 0));
		toastArea.setMaxWidth(320);

		root.getChildren().add(toastArea);
		StackPane.setAlignment(toastArea, Pos.BOTTOM_RIGHT);
		// make sure it's on top of everything that was just added
		toastArea.toFront();

		// Scene
		Scene mainMenu = new Scene(root, 900, 650);
		this.mainScene = mainMenu;

		// Wire mainMenu navigation — each button opens its respective section
		mainBt1.setOnAction(e -> showStaffMenu());
		mainBt2.setOnAction(e -> showDoctorMenu());
		mainBt3.setOnAction(e -> showPatientMenu());
		mainBt4.setOnAction(e -> showMedicalMenu());
		mainBt5.setOnAction(e -> showLabMenu());
		mainBt6.setOnAction(e -> showFacilityMenu());

		primaryStage.setScene(mainMenu);
		primaryStage.setTitle("Hospital Management System");
		primaryStage.show();
		
		startJobs();
		// Run checks once immediately on startup
		lightweightChecks();
		// Try to show any unseen notifications on app open
		showAlertsOnStartup();
	}

	// ----------------------------------------------------------------------------------
	// Staff Menu

	private void showStaffMenu() {

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
		staffTxt.setFill(Color.BLACK);
		staffTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));

		// Text Fields
		TextField staffTf1 = new TextField();
		TextField staffTf2 = new TextField();
		TextField staffTf3 = new TextField();
		TextField staffTf4 = new TextField();
		TextField staffTf5 = new TextField();
		TextField staffTf6 = new TextField();

		// HBox (Top banner)
		HBox staffH1 = new HBox();
		staffH1.getChildren().add(staffTxt);
		staffH1.setAlignment(Pos.CENTER);
		staffH1.setSpacing(20);
		staffH1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		staffH1.setPrefSize(50, 50);

		// VBox (Left menu + content panel)
		VBox staffV1 = new VBox();
		VBox staffV2 = new VBox();
		
		staffV1.getChildren().addAll(addStaff, showStaff, returnTo1, updateStaff, deleteStaff);
		staffV1.setAlignment(Pos.BASELINE_CENTER);
		staffV1.setSpacing(30);
		staffV1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		staffV1.setPrefSize(130, 600);
		
		staffV2.setAlignment(Pos.TOP_LEFT);
		staffV2.setSpacing(10);
		staffV2.setPrefSize(470, 600);
		staffV2.setPadding(new Insets(20));

		// textfields
		staffTf1.setMinSize(300, 30);
		staffTf2.setMinSize(300, 30);
		staffTf3.setMinSize(300, 30);
		staffTf4.setMinSize(300, 30);
		staffTf5.setMinSize(300, 30);
		staffTf6.setMinSize(300, 30);
		
		staffTf1.setPromptText("Staff ID");
		staffTf2.setPromptText("Staff Name");
		staffTf3.setPromptText("Staff Designation");
		staffTf4.setPromptText("Staff Sex");
		staffTf5.setPromptText("Staff Salary");
		staffTf6.setPromptText("Status");

		// Fonts
		staffTf1.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		staffTf2.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		staffTf3.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		staffTf4.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		staffTf5.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		staffTf6.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));

		// Styles (black & white minimal)
		staffTf1.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		staffTf2.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		staffTf3.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		staffTf4.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		staffTf5.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		staffTf6.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");

		addStaff.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		showStaff.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		returnTo1.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		addStaffTo.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		updateStaff.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		deleteStaff.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");

		// VBox alignment and spacing for form and status feedback
		VBox staffV3 = new VBox();
		staffV3.getChildren().addAll(staffTf1, staffTf2, staffTf3, staffTf4, staffTf5, addStaffTo, staffTf6);
		staffV3.setAlignment(Pos.TOP_LEFT);
		staffV3.setSpacing(10);
		staffV3.setPrefSize(400, 600);
		staffV3.setPadding(new Insets(20));

		// ListView (read)
		staffListView = makeBWListView();
		staffListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		staffListView.setMinHeight(470);
		staffListView.setOnMouseClicked(ev -> {
		    String row = staffListView.getSelectionModel().getSelectedItem();
		    selectedStaffId = leading(row, 10); // assuming id width 10 in formatted row
		    staffTf6.setText("Selected Staff ID: " + ns(selectedStaffId));
		});
		
		// BorderPane layout
		BorderPane main3 = new BorderPane();
		main3.setTop(staffH1);
		main3.setLeft(staffV1);
		main3.setCenter(staffV2);

		// Add event handlers
		addStaff.setOnAction(e -> {
			staffV2.getChildren().clear();
			staffV2.getChildren().add(staffV3);
			
			// reset form fields
			staffTf1.clear(); staffTf2.clear(); staffTf3.clear();
			staffTf4.clear(); staffTf5.clear();
			staffTf6.setText("");
			staffTf1.setEditable(true);
			addStaffTo.setText("Add");
			
			if (originalAddStaffHandler != null) addStaffTo.setOnAction(originalAddStaffHandler);
		});
		
		returnTo1.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});

		addStaffTo.setOnAction(e -> {
			// add or update (when swapped) depends on handler state
	        String id = staffTf1.getText();
	        String name = staffTf2.getText();
	        String designation = staffTf3.getText();
	        String sex = staffTf4.getText();
	        String salary = staffTf5.getText();

	        // basic validation & call repository
	        if (id.isBlank()) { staffTf6.setText("Staff ID cannot be empty"); return; }
	        if (name.isBlank()) { staffTf6.setText("Name cannot be empty"); return; }

	        Staff s = new Staff(id, name, designation, sex, salary == null ? 0 : Integer.parseInt(salary));
	        boolean ok = staffRepo.insert(s);
	        staffTf6.setText(ok ? "Added Staff successfully" : "Failed to add Staff");
		});

		showStaff.setOnAction(e -> {
			staffV2.getChildren().clear();

			Label staffHeader = new Label("   ID             NAME                   DESIGNATION         SEX      SALARY");
			staffHeader.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 15));
			staffHeader.setStyle("-fx-text-fill: #000000;");

			refreshStaffList();
			staffListView.getSelectionModel().clearSelection();

			staffV2.getChildren().addAll(staffHeader, staffListView);
		});
		
		updateStaff.setOnAction(e -> {
		    staffTf6.setText("");

		    // 1) ensure a row is selected
		    String id = selectedStaffId;
		    if ((id == null || id.isBlank()) && staffListView != null) {
		        String row = staffListView.getSelectionModel().getSelectedItem();
		        if (row != null) {
		            id = leading(row, 10);
		        }
		    }
		    if (id == null || id.isBlank()) {
		        staffTf6.setText("Please select a staff from the list first.");
		        return;
		    }

		    // 1.5) fetch current record
		    var opt = staffRepo.findById(id);
		    if (opt.isEmpty()) {
		        staffTf6.setText("Selected staff no longer exists.");
		        // refresh list to reflect current db
		        if (staffListView != null) {
		            staffListView.getItems().clear();
		            List<Staff> all = new ArrayList<>(staffRepo.findAll());
		            all.sort((a,b) -> cmpId(a.getId(), b.getId()));
		            for (Staff ss : all) {
		                String row = String.format("%-10s%-20s%-22s%-8s%-10s",
		                    ns(ss.getId()), ns(ss.getName()), ns(ss.getDesignation()), ss.getSex(), ss.getSalary());
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
		    staffTf6.setText("Update mode: editing staff " + s.getId());

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

		        if (name.isEmpty()) { staffTf6.setText("Name cannot be empty."); return; }
		        int salary = 0;
		        try { salary = Integer.parseInt(salaryStr); }
		        catch (Exception ex) { staffTf6.setText("Salary must be an integer."); return; }

		        Staff updated = new Staff(selectedId, name, designation, sex, salary);
		        boolean ok = staffRepo.update(updated);
		        staffTf6.setText(ok ? "Updated staff " + selectedId : "Failed to update " + selectedId);

		        // restore UI back to normal Add mode
		        staffTf1.setEditable(true);
		        addStaffTo.setText("Add");
		        if (originalAddStaffHandler != null) addStaffTo.setOnAction(originalAddStaffHandler);

		        // Optionally refresh list if currently showing it
		        if (staffListView != null && !staffListView.getItems().isEmpty()) {
		            staffListView.getItems().clear();
		            List<Staff> all = new ArrayList<>(staffRepo.findAll());
		            all.sort((a,b) -> cmpId(a.getId(), b.getId()));
		            for (Staff ss : all) {
		                String row = String.format("%-10s%-20s%-22s%-8s%-10s",
		                    ns(ss.getId()), ns(ss.getName()), ns(ss.getDesignation()), ss.getSex(), ss.getSalary());
		                staffListView.getItems().add(row);
		            }
		        }
		    });
		});
		
		deleteStaff.setOnAction(e -> {
		    String id = selectedStaffId;
		    if ((id == null || id.isBlank()) && staffListView != null) {
		        String row = staffListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        showInfo("Delete Staff", "Please select a staff to delete.");
		        return;
		    }
		    if (!confirm("Delete Staff", "Are you sure you want to delete staff " + id + "?")) return;

		    boolean ok = staffRepo.delete(id);  // <-- changed from deleteById(id) to delete(id)
		    showInfo("Delete Staff", ok ? "Deleted staff " + id : "Failed to delete staff " + id);

		    // refresh list
		    if (staffListView != null) {
		        staffListView.getItems().clear();
		        List<Staff> all = new ArrayList<>(staffRepo.findAll());
		        all.sort((a,b) -> cmpId(a.getId(), b.getId()));
		        for (Staff ss : all) {
		            String row = String.format("%-10s%-20s%-22s%-8s%-10s",
		                ns(ss.getId()), ns(ss.getName()), ns(ss.getDesignation()), ss.getSex(), ss.getSalary());
		            staffListView.getItems().add(row);
		        }
		    }
		});

		Scene sc1 = new Scene(main3, 900, 650);
		primaryStage.setScene(sc1);
		primaryStage.show();
	}

	// ----------------------------------------------------------------------------------
	// Doctor Menu

	private void showDoctorMenu() {

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
		doctorTxt.setFill(Color.BLACK);
		doctorTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));

		// Text Fields
		TextField doctorTf1 = new TextField();
		TextField doctorTf2 = new TextField();
		TextField doctorTf3 = new TextField();
		TextField doctorTf4 = new TextField();
		TextField doctorTf5 = new TextField();
		TextField doctorTf6 = new TextField();

		// HBox
		HBox doctorH1 = new HBox();
		doctorH1.getChildren().add(doctorTxt);
		doctorH1.setAlignment(Pos.CENTER);
		doctorH1.setSpacing(20);
		doctorH1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		doctorH1.setPrefSize(50, 50);
		
		// VBox
		VBox doctorV1 = new VBox();
		VBox doctorV2 = new VBox();
		
		doctorV1.getChildren().addAll(addDoctor, showDoctor, returnTo2, updateDoctor, deleteDoctor);
		doctorV1.setAlignment(Pos.BASELINE_CENTER);
		doctorV1.setSpacing(30);
		doctorV1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		doctorV1.setPrefSize(130, 600);
		
		doctorV2.setAlignment(Pos.TOP_LEFT);
		doctorV2.setSpacing(10);
		doctorV2.setPrefSize(470, 600);
		doctorV2.setPadding(new Insets(20));

		// textfields
		doctorTf1.setMinSize(300, 30);
		doctorTf2.setMinSize(300, 30);
		doctorTf3.setMinSize(300, 30);
		doctorTf4.setMinSize(300, 30);
		doctorTf5.setMinSize(300, 30);
		doctorTf6.setMinSize(300, 30);
		
		doctorTf1.setPromptText("Doctor ID");
		doctorTf2.setPromptText("Doctor Name");
		doctorTf3.setPromptText("Doctor Specialist");
		doctorTf4.setPromptText("Doctor WorkTime");
		doctorTf5.setPromptText("Doctor Qualifications");
		doctorTf6.setPromptText("Status");

		// Fonts
		doctorTf1.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		doctorTf2.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		doctorTf3.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		doctorTf4.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		doctorTf5.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		doctorTf6.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));

		// Styles
		doctorTf1.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		doctorTf2.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		doctorTf3.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		doctorTf4.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		doctorTf5.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		doctorTf6.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");

		addDoctor.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		showDoctor.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		returnTo2.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		addDoctorTo.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		updateDoctor.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		deleteDoctor.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");

		VBox doctorV3 = new VBox();
		doctorV3.getChildren().addAll(doctorTf1, doctorTf2, doctorTf3, doctorTf4, doctorTf5, addDoctorTo, doctorTf6);
		doctorV3.setAlignment(Pos.TOP_LEFT);
		doctorV3.setSpacing(10);
		doctorV3.setPrefSize(400, 600);
		doctorV3.setPadding(new Insets(20));

		// ListView (read)
		doctorListView = makeBWListView();
		doctorListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		doctorListView.setMinHeight(470);
		doctorListView.setOnMouseClicked(ev -> {
		    String row = doctorListView.getSelectionModel().getSelectedItem();
		    selectedDoctorId = leading(row, 10);
		    doctorTf6.setText("Selected Doctor ID: " + ns(selectedDoctorId));
		});

		// BorderPane layout
		BorderPane main4 = new BorderPane();
		main4.setTop(doctorH1);
		main4.setLeft(doctorV1);
		main4.setCenter(doctorV2);

		// Handlers
		addDoctor.setOnAction(e -> {
			doctorV2.getChildren().clear();
			doctorV2.getChildren().add(doctorV3);
			
			doctorTf1.clear(); doctorTf2.clear(); doctorTf3.clear(); doctorTf4.clear();
			doctorTf5.clear(); 
			doctorTf6.setText("");
			doctorTf1.setEditable(true);
			addDoctorTo.setText("Add");
			
			if (originalAddDoctorHandler != null) addDoctorTo.setOnAction(originalAddDoctorHandler);
		});

		addDoctorTo.setOnAction(e -> {
		    String id = doctorTf1.getText();
		    String name = doctorTf2.getText();
		    String specialist = doctorTf3.getText();
		    String workTime = doctorTf4.getText();       
		    String qualification = doctorTf5.getText();  
		    int room = 0;                                 // placeholder (no room field in UI)

		    if (id.isBlank()) { doctorTf6.setText("Doctor ID cannot be empty"); return; }
		    if (name.isBlank()) { doctorTf6.setText("Name cannot be empty"); return; }

		    Doctor d = new Doctor(id, name, specialist, workTime, qualification, room);
		    boolean ok = doctorRepo.insert(d); 
		    doctorTf6.setText(ok ? "Added Doctor successfully" : "Failed to add Doctor");
		});

		showDoctor.setOnAction(e -> {
			doctorV2.getChildren().clear();
			
			Label doctorHeader = new Label("   ID                 NAME                SPECIALIST           WORKTIME      QUALIFICATION");
			doctorHeader.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 15));
			doctorHeader.setStyle("-fx-text-fill: #000000;");

			refreshDoctorList();
			doctorListView.getSelectionModel().clearSelection();

			doctorV2.getChildren().addAll(doctorHeader, doctorListView);
		});

		returnTo2.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});

		updateDoctor.setOnAction(e -> {
		    doctorTf6.setText("");

		    String id = selectedDoctorId;
		    if ((id == null || id.isBlank()) && doctorListView != null) {
		        String row = doctorListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        doctorTf6.setText("Please select a doctor from the list first.");
		        return;
		    }

		    var opt = doctorRepo.findById(id);
		    if (opt.isEmpty()) {
		        doctorTf6.setText("Selected doctor no longer exists.");
		        refreshDoctorList();
		        return;
		    }

		    // switch to add view and prefill
		    addDoctor.fire();

		    Doctor d = opt.get();
		    doctorTf1.setText(d.getId());
		    doctorTf2.setText(ns(d.getName()));
		    doctorTf3.setText(ns(d.getSpecialist()));
		    doctorTf4.setText(ns(d.getWorkTime()));
		    doctorTf5.setText(String.valueOf(d.getQualification()));
		    doctorTf6.setText("Update mode: editing " + d.getId());
		    doctorTf1.setEditable(false);
		    addDoctorTo.setText("Update Record");

		    if (originalAddDoctorHandler == null) {
		        originalAddDoctorHandler = addDoctorTo.getOnAction();
		    }
		    
		    final String selectedId = id;
		    addDoctorTo.setOnAction(ev -> {
		        doctorTf6.setText("");
		        final String name = doctorTf2.getText().trim();
		        final String spec = doctorTf3.getText().trim();
		        final String worktime = doctorTf4.getText().trim();
		        final String qualification = doctorTf5.getText().trim();
		        final int room = 0; // placeholder

		        if (name.isEmpty()) { doctorTf6.setText("Name cannot be empty."); return; }

		        Doctor updated = new Doctor(selectedId, name, spec, worktime, qualification, room);
		        boolean ok = doctorRepo.update(updated);
		        doctorTf6.setText(ok ? "Updated " + selectedId : "Failed to update " + selectedId);

		        doctorTf1.setEditable(true);
		        addDoctorTo.setText("Add");
		        if (originalAddDoctorHandler != null) addDoctorTo.setOnAction(originalAddDoctorHandler);
		        refreshDoctorList();
		    });
		});

		deleteDoctor.setOnAction(e -> {
		    String id = selectedDoctorId;
		    if ((id == null || id.isBlank()) && doctorListView != null) {
		        String row = doctorListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        showInfo("Delete Doctor", "Please select a doctor to delete.");
		        return;
		    }
		    if (!confirm("Delete Doctor", "Are you sure you want to delete doctor " + id + "?")) return;

		    boolean ok = doctorRepo.delete(id);  // <-- changed from deleteById(id)
		    showInfo("Delete Doctor", ok ? "Deleted doctor " + id : "Failed to delete doctor " + id);
		    refreshDoctorList();
		});

		Scene sc2 = new Scene(main4, 900, 650);
		primaryStage.setScene(sc2);
		primaryStage.show();
	}

	private void refreshDoctorList() {
	    if (doctorListView == null) return;
	    doctorListView.getItems().clear();
	    java.util.List<Doctor> all = new java.util.ArrayList<>(doctorRepo.findAll());
	    all.sort((d1, d2) -> cmpId(d1.getId(), d2.getId()));
	    for (Doctor dd : all) {
	        String row = String.format("%-10s%-20s%-15s%-15s%-15s",
	            ns(dd.getId()), ns(dd.getName()), ns(dd.getSpecialist()), ns(dd.getWorkTime()), dd.getQualification());
	        doctorListView.getItems().add(row);
	    }
	}

	// ----------------------------------------------------------------------------------
	// Patient Menu

	private void showPatientMenu() {

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
		patientTxt.setFill(Color.BLACK);
		patientTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));

		// Text Fields
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
		patientH1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		patientH1.setPrefSize(50, 50);
		
		// VBox
		VBox patientV1 = new VBox();
		VBox patientV2 = new VBox();
		
		patientV1.getChildren().addAll(addPatient, showPatient, returnTo3, updatePatient, deletePatient);
		patientV1.setAlignment(Pos.BASELINE_CENTER);
		patientV1.setSpacing(30);
		patientV1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		patientV1.setPrefSize(130, 600);
		
		patientV2.setAlignment(Pos.TOP_LEFT);
		patientV2.setSpacing(10);
		patientV2.setPrefSize(470, 600);
		patientV2.setPadding(new Insets(20));
		
		// textfields
		patientTf1.setMinSize(300, 30);
		patientTf2.setMinSize(300, 30);
		patientTf3.setMinSize(300, 30);
		patientTf4.setMinSize(300, 30);
		patientTf5.setMinSize(300, 30);
		patientTf6.setMinSize(300, 30);
		patientTf7.setMinSize(300, 30);
		
		patientTf1.setPromptText("Patient ID");
		patientTf2.setPromptText("Patient Name");
		patientTf3.setPromptText("Patient Disease");
		patientTf4.setPromptText("Patient Sex");
		patientTf5.setPromptText("Patient Age");
		patientTf6.setPromptText("Patient Admit Status");
		patientTf7.setPromptText("Status");
		
		// Fonts
		patientTf1.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		patientTf2.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		patientTf3.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		patientTf4.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		patientTf5.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		patientTf6.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		patientTf7.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		
		// Styles
		patientTf1.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		patientTf2.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		patientTf3.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		patientTf4.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		patientTf5.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		patientTf6.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		patientTf7.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		
		addPatient.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		showPatient.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		returnTo3.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		addPatientTo.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		updatePatient.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		deletePatient.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");

		VBox patientV3 = new VBox();
		patientV3.getChildren().addAll(patientTf1, patientTf2, patientTf3, patientTf4, patientTf5, patientTf6, addPatientTo, patientTf7);
		patientV3.setAlignment(Pos.TOP_LEFT);
		patientV3.setSpacing(10);
		patientV3.setPrefSize(400, 600);
		patientV3.setPadding(new Insets(20));

		// ListView (read)
		patientListView = makeBWListView();
		patientListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		patientListView.setMinHeight(470);
		patientListView.setOnMouseClicked(ev -> {
		    String row = patientListView.getSelectionModel().getSelectedItem();
		    selectedPatientId = leading(row, 10); // uses fixed column layout
		    patientTf7.setText("Selected Patient ID: " + ns(selectedPatientId));
		});

		// BorderPane layout
		BorderPane main5 = new BorderPane();
		main5.setTop(patientH1);
		main5.setLeft(patientV1);
		main5.setCenter(patientV2);
		
		// Handlers		
		addPatient.setOnAction(e -> {
		    patientV2.getChildren().clear();   // clear the center area
		    patientV2.getChildren().add(patientV3);  // show the form in the center

		    // reset form fields
		    patientTf1.clear(); patientTf2.clear(); patientTf3.clear();
		    patientTf4.clear(); patientTf5.clear(); patientTf6.clear();
		    patientTf7.setText("");           
		    patientTf1.setEditable(true);
		    addPatientTo.setText("Add");
		    if (originalAddPatientHandler != null) addPatientTo.setOnAction(originalAddPatientHandler);
		});

		addPatientTo.setOnAction(e -> {
		    String id = patientTf1.getText();
		    String name = patientTf2.getText();
		    String disease = patientTf3.getText();
		    String sex = patientTf4.getText();
		    String age = patientTf5.getText();
		    String admitStatus = patientTf6.getText();

		    if (id.isBlank()) { patientTf7.setText("Patient ID cannot be empty"); return; }
		    if (name.isBlank()) { patientTf7.setText("Name cannot be empty"); return; }

		    int ageVal = (age == null || age.isBlank()) ? 0 : Integer.parseInt(age.trim());

		    Patient p = new Patient(id, name, disease, sex, admitStatus, ageVal); // <-- order fixed
		    boolean ok = patientRepo.insert(p); 
		    patientTf7.setText(ok ? "Added Patient successfully" : "Failed to add Patient");
		});
		
		returnTo3.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});

		showPatient.setOnAction(e -> {
		    patientV2.getChildren().clear();   // clear the center

		    Label patientHeader = new Label(
		        "   ID               NAME                       DISEASE                        SEX      ADMIT STATUS       AGE"
		    );
		    patientHeader.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 15));
		    patientHeader.setStyle("-fx-text-fill: #000000;");

		    refreshPatientList();
		    patientListView.getSelectionModel().clearSelection();

		    patientV2.getChildren().addAll(patientHeader, patientListView);  // show the list in the center
		});
		
		updatePatient.setOnAction(e -> {
		    patientTf7.setText("");

		    // 1) make sure a row is selected
		    String id = selectedPatientId;
		    if ((id == null || id.isBlank()) && patientListView != null) {
		        String row = patientListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        patientTf7.setText("Please select a patient from the list first.");
		        return;
		    }

		    // 1.5) Fetch the current record to update
		    var opt = patientRepo.findById(id);
		    if (opt.isEmpty()) {
		        patientTf7.setText("Selected patient no longer exists.");
		        refreshPatientList();
		        return;
		    }

		    // 2) switch to the Add Patient view (reuse your existing builder)
		    addPatient.fire();

		    // 3) prefill form, lock ID, and convert Add button into "Update Record"
		    Platform.runLater(() -> {
		        Patient p = opt.get();
		        selectedPatientId = p.getId();
		        patientTf1.setText(p.getId());
		        patientTf2.setText(ns(p.getName()));
		        patientTf3.setText(ns(p.getDisease()));
		        patientTf4.setText(ns(p.getSex()));
		        patientTf5.setText(String.valueOf(p.getAge()));
		        patientTf6.setText(ns(p.getAdmitStatus()));
		        patientTf7.setText("Update mode: editing " + p.getId());

		        patientTf1.setEditable(false);
		        addPatientTo.setText("Update Record");

		        if (originalAddPatientHandler == null) {
		            originalAddPatientHandler = addPatientTo.getOnAction();
		        }

		        addPatientTo.setOnAction(ev -> {
		            // 4) validate and call repo.update(...)
		            String name = patientTf2.getText().trim();
		            String disease = patientTf3.getText().trim();
		            String sex = patientTf4.getText().trim();
		            String ageStr = patientTf5.getText().trim();
		            String admit = patientTf6.getText().trim();

		            if (name.isEmpty()) { patientTf7.setText("Name cannot be empty."); return; }
		            int ageVal = 0;
		            try { ageVal = Integer.parseInt(ageStr); }
		            catch (Exception ex) { patientTf7.setText("Age must be an integer."); return; }

		            // ---- FIX: admitStatus before age ----
		            Patient updated = new Patient(selectedPatientId, name, disease, sex, admit, ageVal);

		            boolean ok = patientRepo.update(updated);
		            patientTf7.setText(ok ? "Updated " + selectedPatientId : "Failed to update " + selectedPatientId);
		            
		            // 5) restore "Add" mode UI and handler
		            patientTf1.setEditable(true);
		            addPatientTo.setText("Add");
		            if (originalAddPatientHandler != null) addPatientTo.setOnAction(originalAddPatientHandler);

		            // 6) refresh list if visible
		            refreshPatientList();
		        });
		    });
		});
		
		deletePatient.setOnAction(e -> {
		    String id = selectedPatientId;
		    if ((id == null || id.isBlank()) && patientListView != null) {
		        String row = patientListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        showInfo("Delete Patient", "Please select a patient to delete.");
		        return;
		    }
		    if (!confirm("Delete Patient", "Are you sure you want to delete patient " + id + "?")) return;
		    boolean ok = patientRepo.delete(id);
		    showInfo("Delete Patient", ok ? "Deleted patient " + id : "Failed to delete patient " + id);
		    refreshPatientList();
		});

		Scene sc3 = new Scene(main5, 900, 650);
		primaryStage.setScene(sc3);
		primaryStage.show();
	}

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
	    all.sort((p1, p2) -> cmpId(p1.getId(), p2.getId()));
	    for (Staff s : all) {
	        String row = String.format("%-10s%-20s%-15s%-10s%-15s",
	            ns(s.getId()), ns(s.getName()), ns(s.getDesignation()),
	            ns(s.getSex()), ns(String.valueOf(s.getSalary())));
	        staffListView.getItems().add(row);
	    }
	}

	// ----------------------------------------------------------------------------------
	// Medical Menu

	private void showMedicalMenu() {

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

	    // Title
	    Text medicalTxt = new Text("Medical");
	    medicalTxt.setFill(Color.BLACK);
	    medicalTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));

	    // Text Fields (id, name, manufacturer, expiryDate, cost, count, status)
	    TextField medicalTf1 = new TextField(); // id
	    TextField medicalTf2 = new TextField(); // name
	    TextField medicalTf3 = new TextField(); // manufacturer
	    TextField medicalTf4 = new TextField(); // expiryDate YYYY-MM-DD
	    TextField medicalTf5 = new TextField(); // cost (int)
	    TextField medicalTf6 = new TextField(); // count (int)
	    TextField medicalTf7 = new TextField(); // status / feedback (read-only)
	    medicalTf7.setEditable(false);

	    // Header HBox
	    HBox medicalH1 = new HBox();
	    medicalH1.getChildren().add(medicalTxt);
	    medicalH1.setAlignment(Pos.CENTER);
	    medicalH1.setSpacing(20);
	    medicalH1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
	    medicalH1.setPrefSize(50, 50);

	    // Side + Center containers
	    VBox medicalV1 = new VBox();
	    VBox medicalV2 = new VBox();

	    medicalV1.getChildren().addAll(addMedical, showMedical, returnTo4, updateMedical, deleteMedical);
	    medicalV1.setAlignment(Pos.BASELINE_CENTER);
	    medicalV1.setSpacing(30);
	    medicalV1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
	    medicalV1.setPrefSize(130, 600);

	    medicalV2.setAlignment(Pos.TOP_LEFT);
	    medicalV2.setSpacing(10);
	    medicalV2.setPrefSize(470, 600);
	    medicalV2.setPadding(new Insets(20));

	    // Sizing
	    medicalTf1.setMinSize(300, 30);
	    medicalTf2.setMinSize(300, 30);
	    medicalTf3.setMinSize(300, 30);
	    medicalTf4.setMinSize(300, 30);
	    medicalTf5.setMinSize(300, 30);
	    medicalTf6.setMinSize(300, 30);
	    medicalTf7.setMinSize(300, 30);

	    // Prompts aligned with Medical.java
	    medicalTf1.setPromptText("ID");
	    medicalTf2.setPromptText("Name");
	    medicalTf3.setPromptText("Manufacturer");
	    medicalTf4.setPromptText("Expiry Date (YYYY-MM-DD)");
	    medicalTf5.setPromptText("Cost (integer)");
	    medicalTf6.setPromptText("Count (integer)");
	    medicalTf7.setPromptText("Status");

	    // Fonts
	    medicalTf1.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
	    medicalTf2.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
	    medicalTf3.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
	    medicalTf4.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
	    medicalTf5.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
	    medicalTf6.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
	    medicalTf7.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));

	    // Minimal black & white styles
	    String bw = "-fx-border-color: #000000; -fx-border-width: 1px;";
	    medicalTf1.setStyle(bw);
	    medicalTf2.setStyle(bw);
	    medicalTf3.setStyle(bw);
	    medicalTf4.setStyle(bw);
	    medicalTf5.setStyle(bw);
	    medicalTf6.setStyle(bw);
	    medicalTf7.setStyle(bw);

	    String bwBtn = "-fx-border-color: #000000; -fx-background-color: #FFFFFF;";
	    addMedical.setStyle(bwBtn);
	    showMedical.setStyle(bwBtn);
	    returnTo4.setStyle(bwBtn);
	    addMedicalTo.setStyle(bwBtn);
	    updateMedical.setStyle(bwBtn);
	    deleteMedical.setStyle(bwBtn);

	    // Right-side form stack (includes status at bottom)
	    VBox medicalV3 = new VBox();
	    medicalV3.getChildren().addAll(medicalTf1, medicalTf2, medicalTf3, medicalTf4, medicalTf5, medicalTf6, addMedicalTo, medicalTf7);
	    medicalV3.setAlignment(Pos.TOP_LEFT);
	    medicalV3.setSpacing(10);
	    medicalV3.setPrefSize(400, 600);
	    medicalV3.setPadding(new Insets(20));

	    // ListView (read)
	    medicalListView = makeBWListView();
	    medicalListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
	    medicalListView.setMinHeight(470);
	    medicalListView.setOnMouseClicked(ev -> {
	        String row = medicalListView.getSelectionModel().getSelectedItem();
	        // First column is ID, width 10
	        selectedMedicalId = leading(row, 10); // NOTE: reusing existing variable name to avoid introducing new fields; it now stores the ID
	        medicalTf7.setText("Selected ID: " + ns(selectedMedicalId));
	    });

	    // BorderPane layout
	    BorderPane main6 = new BorderPane();
	    main6.setTop(medicalH1);
	    main6.setLeft(medicalV1);
	    main6.setCenter(medicalV2);

	    // Handlers
	    addMedical.setOnAction(e -> {
	        medicalV2.getChildren().clear();

	        medicalV2.getChildren().add(medicalV3);  // show the form in the center

	        // reset form fields
	        medicalTf1.clear(); medicalTf2.clear(); medicalTf3.clear();
	        medicalTf4.clear(); medicalTf5.clear(); medicalTf6.clear();
	        medicalTf7.setText("");           
	        medicalTf1.setEditable(true);
	        addMedicalTo.setText("Add");
	        if (originalAddMedicalHandler != null) addMedicalTo.setOnAction(originalAddMedicalHandler);
	    });

	    addMedicalTo.setOnAction(e -> {
	        String id = ns(medicalTf1.getText());
	        String name = ns(medicalTf2.getText());
	        String manufacturer = ns(medicalTf3.getText());
	        String expiryDate = ns(medicalTf4.getText());
	        String costStr = ns(medicalTf5.getText());
	        String countStr = ns(medicalTf6.getText());

	        if (id.isBlank()) { medicalTf7.setText("Medical ID cannot be empty"); return; }
	        if (name.isBlank()) { medicalTf7.setText("Name cannot be empty"); return; }
	        if (manufacturer.isBlank()) { medicalTf7.setText("Manufacturer cannot be empty"); return; }
	        if (expiryDate.isBlank()) { medicalTf7.setText("Expiry date cannot be empty"); return; }

	        int cost, count;
	        try {
	            cost = costStr.isBlank() ? 0 : Integer.parseInt(costStr);
	            count = countStr.isBlank() ? 0 : Integer.parseInt(countStr);
	        } catch (NumberFormatException ex) {
	            medicalTf7.setText("Cost and Count must be integers.");
	            return;
	        }

	        Medical m = new Medical(id, name, manufacturer, expiryDate, cost, count);
	        boolean ok = medicalRepo.insert(m); // repository uses insert(...)
	        medicalTf7.setText(ok ? "Added Medical successfully" : "Failed to add Medical");
	    });

	    showMedical.setOnAction(e -> {
	        medicalV2.getChildren().clear();

	        Label medicalHeader = new Label("   ID              NAME            MANUFACTURER      EXPIRY DATE   COST   COUNT");
	        medicalHeader.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 15));
	        medicalHeader.setStyle("-fx-text-fill: #000000;");

	        medicalListView.getItems().clear();
	        java.util.List<Medical> all = new java.util.ArrayList<>(medicalRepo.findAll());
	        // Already ordered by ID in repo; stable here as well if needed
	        for (Medical mm : all) {
	            String row = String.format("%-10s%-18s%-18s%-14s%-7s%-6s",
	                ns(mm.getId()),
	                ns(mm.getName()),
	                ns(mm.getManufacturer()),
	                ns(mm.getExpiryDate()),
	                String.valueOf(mm.getCost()),
	                String.valueOf(mm.getCount())
	            );
	            medicalListView.getItems().add(row);
	        }
	        medicalListView.getSelectionModel().clearSelection();
	        medicalV2.getChildren().addAll(medicalHeader, medicalListView);
	    });

	    returnTo4.setOnAction(e -> {
	        primaryStage.setScene(mainScene);
	    });

	    updateMedical.setOnAction(e -> {
	        medicalTf7.setText("");

	        String id = selectedMedicalId; // stores ID from selection
	        if ((id == null || id.isBlank()) && medicalListView != null) {
	            String row = medicalListView.getSelectionModel().getSelectedItem();
	            if (row != null) id = leading(row, 10);
	        }
	        if (id == null || id.isBlank()) {
	            medicalTf7.setText("Please select a medical record from the list first.");
	            return;
	        }

	        var opt = medicalRepo.findById(id);
	        if (opt.isEmpty()) {
	            medicalTf7.setText("Selected medical record no longer exists.");
	            showMedical.fire();
	            return;
	        }

	        addMedical.fire();

	        Medical m = opt.get();
	        medicalTf1.setText(ns(m.getId()));
	        medicalTf2.setText(ns(m.getName()));
	        medicalTf3.setText(ns(m.getManufacturer()));
	        medicalTf4.setText(ns(m.getExpiryDate()));
	        medicalTf5.setText(String.valueOf(m.getCost()));
	        medicalTf6.setText(String.valueOf(m.getCount()));
	        medicalTf7.setText("Update mode: editing ID " + m.getId());

	        medicalTf1.setEditable(false);      // lock ID during update
	        addMedicalTo.setText("Update Record");

	        if (originalAddMedicalHandler == null) {
	            originalAddMedicalHandler = addMedicalTo.getOnAction();
	        }

	        final String key = id; // immutable id for WHERE clause

	        addMedicalTo.setOnAction(ev -> {
	            medicalTf7.setText("");

	            String name = ns(medicalTf2.getText());
	            String manufacturer = ns(medicalTf3.getText());
	            String expiryDate = ns(medicalTf4.getText());
	            String costStr = ns(medicalTf5.getText());
	            String countStr = ns(medicalTf6.getText());

	            if (name.isBlank()) { medicalTf7.setText("Name cannot be empty."); return; }
	            if (manufacturer.isBlank()) { medicalTf7.setText("Manufacturer cannot be empty."); return; }
	            if (expiryDate.isBlank()) { medicalTf7.setText("Expiry date cannot be empty."); return; }

	            int cost, count;
	            try {
	                cost = costStr.isBlank() ? 0 : Integer.parseInt(costStr);
	                count = countStr.isBlank() ? 0 : Integer.parseInt(countStr);
	            } catch (NumberFormatException ex) {
	                medicalTf7.setText("Cost and Count must be integers.");
	                return;
	            }

	            Medical updated = new Medical(key, name, manufacturer, expiryDate, cost, count);
	            boolean ok = medicalRepo.update(updated);
	            medicalTf7.setText(ok ? "Updated ID " + key : "Failed to update ID " + key);

	            // restore "Add" mode and handler
	            medicalTf1.setEditable(true);
	            addMedicalTo.setText("Add");
	            if (originalAddMedicalHandler != null) addMedicalTo.setOnAction(originalAddMedicalHandler);

	            showMedical.fire(); // refresh
	        });
	    });

	    deleteMedical.setOnAction(e -> {
	        String id = selectedMedicalId; // stores ID
	        if ((id == null || id.isBlank()) && medicalListView != null) {
	            String row = medicalListView.getSelectionModel().getSelectedItem();
	            if (row != null) id = leading(row, 10);
	        }
	        if (id == null || id.isBlank()) {
	            showInfo("Delete Medical", "Please select a medical record to delete.");
	            return;
	        }
	        if (!confirm("Delete Medical", "Are you sure you want to delete ID " + id + "?")) return;

	        boolean ok = medicalRepo.deleteById(id);
	        showInfo("Delete Medical", ok ? "Deleted ID " + id : "Failed to delete ID " + id);
	        showMedical.fire();
	    });

	    // set scene
	    Scene medicalScene = new Scene(main6, 1000, 600);
	    primaryStage.setScene(medicalScene);
	    
	    // If a toast asked us to jump straight to the list:
	    if (goToMedicalListAfterOpen) {
	        goToMedicalListAfterOpen = false;
	        Platform.runLater(() -> {
	            try {
	                // Find and fire the "Show Medical" button we created above
	                VBox left = (VBox) ((BorderPane) medicalScene.getRoot()).getLeft();
	                for (javafx.scene.Node n : left.getChildren()) {
	                    if (n instanceof Button b && "Show Medical".equals(b.getText())) {
	                        b.fire();
	                        break;
	                    }
	                }
	            } catch (Exception ignored) {}
	        });
	    }
	}

	// ----------------------------------------------------------------------------------
	// Lab Menu

	private void showLabMenu() {

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
		labTxt.setFill(Color.BLACK);
		labTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));

		// Text Field
		TextField labTf1 = new TextField();
		TextField labTf2 = new TextField();
		TextField labTf3 = new TextField();
		TextField labTf4 = new TextField();
		TextField labTf5 = new TextField();

		// HBox
		HBox labH1 = new HBox();
		labH1.getChildren().add(labTxt);
		labH1.setAlignment(Pos.CENTER);
		labH1.setSpacing(20);
		labH1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		labH1.setPrefSize(50, 50);

		// VBox
		VBox labV1 = new VBox();
		VBox labV2 = new VBox();

		labV1.getChildren().addAll(addLab, showLab, returnTo5, updateLab, deleteLab);
		labV1.setAlignment(Pos.BASELINE_CENTER);
		labV1.setSpacing(30);
		labV1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		labV1.setPrefSize(130, 600);

		labV2.setAlignment(Pos.TOP_LEFT);
		labV2.setSpacing(10);
		labV2.setPrefSize(470, 600);
		labV2.setPadding(new Insets(20));

		// textfields
		labTf1.setMinSize(300, 30);
		labTf2.setMinSize(300, 30);
		labTf3.setMinSize(300, 30);
		labTf4.setMinSize(300, 30);
		labTf5.setMinSize(300, 30);

		labTf1.setPromptText("Lab ID");
		labTf2.setPromptText("Lab Name");
		labTf3.setPromptText("Lab Status");
		labTf4.setPromptText("Lab Result");
		labTf5.setPromptText("Status");

		// Fonts
		labTf1.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		labTf2.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		labTf3.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		labTf4.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		labTf5.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));

		// Styles
		labTf1.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		labTf2.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		labTf3.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		labTf4.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		labTf5.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");

		addLab.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		showLab.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		returnTo5.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		addLabTo.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		updateLab.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		deleteLab.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		
		VBox labV3 = new VBox();
		labV3.getChildren().addAll(labTf1, labTf2, labTf3, labTf4, addLabTo, labTf5);
		labV3.setAlignment(Pos.TOP_LEFT);
		labV3.setSpacing(10);
		labV3.setPrefSize(400, 600);
		labV3.setPadding(new Insets(20));

		// ListView (read)
		labListView = makeBWListView();
		labListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		labListView.setMinHeight(470);
		labListView.setOnMouseClicked(ev -> {
		    String row = labListView.getSelectionModel().getSelectedItem();
		    selectedLabId = leading(row, 10);
		    labTf5.setText("Selected Lab ID: " + ns(selectedLabId));
		});

		// BorderPane layout
		BorderPane main7 = new BorderPane();
		main7.setTop(labH1);
		main7.setLeft(labV1);
		main7.setCenter(labV2);

		// Handlers
		addLab.setOnAction(e -> {
			labV2.getChildren().clear();
			labV2.getChildren().add(labV3);
			
			// reset form fields
			labTf1.clear(); labTf2.clear(); labTf3.clear(); labTf4.clear();
			labTf5.setText("");
			labTf1.setEditable(true);
			addLabTo.setText("Add");
			if(originalAddLabHandler != null) addLabTo.setOnAction(originalAddLabHandler);
		});

		addLabTo.setOnAction(e -> {
		    String id = labTf1.getText();
		    String name = labTf2.getText();     // "Lab Name"
		    String status = labTf3.getText();   // "Lab Status"
		    String result = labTf4.getText();   // "Lab Result"

		    if (id.isBlank()) { labTf5.setText("Lab ID cannot be empty"); return; }
		    if (name.isBlank()) { labTf5.setText("Name cannot be empty"); return; }

		    Lab l = new Lab(id, name, status, result);
		    boolean ok = labRepo.insert(l); 
		    labTf5.setText(ok ? "Added Lab successfully" : "Failed to add Lab");
		});

		showLab.setOnAction(e -> {
		    labV2.getChildren().clear();

		    Label labHeader = new Label("   ID                NAME                       STATUS               RESULT");
		    labHeader.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 15));
		    labHeader.setStyle("-fx-text-fill: #000000;");

		    labListView.getItems().clear();
		    java.util.List<Lab> all = new java.util.ArrayList<>(labRepo.findAll());
		    all.sort((a, b) -> cmpId(a.getId(), b.getId()));
		    for (Lab ll : all) {
		        String row = String.format("%-10s%-20s%-20s%-20s",
		            ns(ll.getId()), ns(ll.getName()), ns(ll.getStatus()), ns(ll.getResult()));
		        labListView.getItems().add(row);
		    }
		    labListView.getSelectionModel().clearSelection();
		    labV2.getChildren().addAll(labHeader, labListView);
		});
		
		returnTo5.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});
		
		updateLab.setOnAction(e -> {
		    labTf5.setText("");

		    String id = selectedLabId;
		    if ((id == null || id.isBlank()) && labListView != null) {
		        String row = labListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        labTf5.setText("Please select a lab from the list first.");
		        return;
		    }

		    var opt = labRepo.findById(id);
		    if (opt.isEmpty()) {
		        labTf5.setText("Selected lab no longer exists.");
		        showLab.fire();
		        return;
		    }

		    addLab.fire();
		    
		    Lab l = opt.get();
		    labTf1.setText(ns(l.getId()));          // ID (locked)
		    labTf2.setText(ns(l.getName()));        // Name
		    labTf3.setText(ns(l.getStatus()));      // Status
		    labTf4.setText(ns(l.getResult()));      // Result
		    labTf5.setText("");                     // (optional/unused)
		    labTf5.setText("Update mode: editing " + l.getId());

		    labTf1.setEditable(false);
		    addLabTo.setText("Update Record");

		    if (originalAddLabHandler == null) {
		        originalAddLabHandler = addLabTo.getOnAction();
		    }
		    final String selectedId = id;

		    addLabTo.setOnAction(ev -> {
		        labTf5.setText("");
		        String name = labTf2.getText().trim();
		        String status = labTf3.getText().trim();
		        String result = labTf4.getText().trim();

		        if (name.isEmpty()) { labTf2.setText("Name cannot be empty."); return; }

		        Lab updated = new Lab(selectedId, name, status, result);
		        boolean ok = labRepo.update(updated);
		        labTf5.setText(ok ? "Updated " + selectedId : "Failed to update " + selectedId);

		        labTf1.setEditable(true);
		        addLabTo.setText("Add");
		        if (originalAddLabHandler != null) addLabTo.setOnAction(originalAddLabHandler);
		        showLab.fire();
		    });
		});
		
		deleteLab.setOnAction(e -> {
		    String id = selectedLabId;
		    if ((id == null || id.isBlank()) && labListView != null) {
		        String row = labListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        showInfo("Delete Lab", "Please select a lab to delete.");
		        return;
		    }
		    if (!confirm("Delete Lab", "Are you sure you want to delete lab " + id + "?")) return;
		    boolean ok = labRepo.deleteById(id);
		    showInfo("Delete Lab", ok ? "Deleted lab " + id : "Failed to delete lab " + id);
		    showLab.fire();
		});

		Scene sc5 = new Scene(main7, 900, 650);
		primaryStage.setScene(sc5);
		primaryStage.show();
	}

	// ----------------------------------------------------------------------------------
	// Facility Menu

	private void showFacilityMenu() {

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
		facilityTxt.setFill(Color.BLACK);
		facilityTxt.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 20));

		// Text Field
		TextField facilityTf1 = new TextField();
		TextField facilityTf2 = new TextField();
		TextField facilityTf3 = new TextField();
		TextField facilityTf4 = new TextField();
		TextField facilityTf5 = new TextField();
		TextField facilityTf6 = new TextField();

		// HBox
		HBox facilityH1 = new HBox();
		facilityH1.getChildren().add(facilityTxt);
		facilityH1.setAlignment(Pos.CENTER);
		facilityH1.setSpacing(20);
		facilityH1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		facilityH1.setPrefSize(50, 50);

		// VBox
		VBox facilityV1 = new VBox();
		VBox facilityV2 = new VBox();

		facilityV1.getChildren().addAll(addFacility, showFacility, returnTo6, updateFacility, deleteFacility);
		facilityV1.setAlignment(Pos.BASELINE_CENTER);
		facilityV1.setSpacing(30);
		facilityV1.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), Insets.EMPTY)));
		facilityV1.setPrefSize(130, 600);

		facilityV2.setAlignment(Pos.TOP_LEFT);
		facilityV2.setSpacing(10);
		facilityV2.setPrefSize(470, 600);
		facilityV2.setPadding(new Insets(20));

		// textfields
		facilityTf1.setMinSize(300, 30);
		facilityTf2.setMinSize(300, 30);
		facilityTf3.setMinSize(300, 30);
		facilityTf4.setMinSize(300, 30);
		facilityTf5.setMinSize(300, 30);
		facilityTf6.setMinSize(300, 30);

		facilityTf1.setPromptText("Facility ID");
		facilityTf2.setPromptText("Facility Name");
		facilityTf3.setPromptText("Facility Description");
		facilityTf4.setPromptText("Facility Status");
		facilityTf5.setPromptText("Facility Capacity");
		facilityTf6.setPromptText("Status");

		// Fonts
		facilityTf1.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		facilityTf2.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		facilityTf3.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		facilityTf4.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		facilityTf5.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));
		facilityTf6.setFont(Font.font("Poppins", FontWeight.NORMAL, FontPosture.REGULAR, 15));

		// Styles
		facilityTf1.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		facilityTf2.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		facilityTf3.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		facilityTf4.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		facilityTf5.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");
		facilityTf6.setStyle("-fx-border-color: #000000; -fx-border-width: 1px;");

		addFacility.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		showFacility.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		returnTo6.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		addFacilityTo.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		updateFacility.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		deleteFacility.setStyle("-fx-border-color: #000000; -fx-background-color: #FFFFFF;");
		
		VBox facilityV3 = new VBox();
		facilityV3.getChildren().addAll(facilityTf1, facilityTf2, facilityTf3, facilityTf4, facilityTf5, addFacilityTo, facilityTf6);
		facilityV3.setAlignment(Pos.TOP_LEFT);
		facilityV3.setSpacing(10);
		facilityV3.setPrefSize(400, 600);
		facilityV3.setPadding(new Insets(20));

		// ListView (read)
		facilityListView = makeBWListView();
		facilityListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		facilityListView.setMinHeight(470);
		facilityListView.setOnMouseClicked(ev -> {
		    String row = facilityListView.getSelectionModel().getSelectedItem();
		    selectedFacilityId = leading(row, 10);
		    facilityTf6.setText("Selected Facility ID: " + ns(selectedFacilityId));
		});

		// BorderPane layout
		BorderPane main8 = new BorderPane();
		main8.setTop(facilityH1);
		main8.setLeft(facilityV1);
		main8.setCenter(facilityV2);

		// Handlers
		addFacility.setOnAction(e -> {
			facilityV2.getChildren().clear();
			facilityV2.getChildren().add(facilityV3);
			
			// reset form fields
			facilityTf1.clear(); facilityTf2.clear(); facilityTf3.clear();
			facilityTf4.clear(); facilityTf5.clear();
			facilityTf6.setText("");
			facilityTf1.setEditable(true);
			addFacilityTo.setText("Add");
			if (originalAddFacilityHandler != null) addFacilityTo.setOnAction(originalAddFacilityHandler);
		});

		addFacilityTo.setOnAction(e -> {
	        String id = facilityTf1.getText();
	        String name = facilityTf2.getText();
	        String description = facilityTf3.getText();
	        String status = facilityTf4.getText();
	        String capacity = facilityTf5.getText();

	        if (id.isBlank()) { facilityTf6.setText("Facility ID cannot be empty"); return; }
	        if (name.isBlank()) { facilityTf6.setText("Name cannot be empty"); return; }

	        Facility f = new Facility(id, name, description, status, capacity == null ? 0 : Integer.parseInt(capacity));
	        boolean ok = facilityRepo.insert(f);
	        facilityTf6.setText(ok ? "Added Facility successfully" : "Failed to add Facility");
		});

		showFacility.setOnAction(e -> {
			facilityV2.getChildren().clear();

			Label facilityHeader = new Label("   ID            NAME                           DESCRIPTION                                   STATUS             CAPACITY");
			facilityHeader.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 15));
			facilityHeader.setStyle("-fx-text-fill: #000000;");

			facilityListView.getItems().clear();
			java.util.List<Facility> all = new java.util.ArrayList<>(facilityRepo.findAll());
			all.sort((a, b) -> cmpId(a.getId(), b.getId()));
			for (Facility ff : all) {
				String row = String.format("%-10s%-20s%-35s%-15s%-10s",
						ns(ff.getId()), ns(ff.getName()), ns(ff.getDescription()), ns(ff.getStatus()), ns(String.valueOf(ff.getCapacity())));
				facilityListView.getItems().add(row);
			}
			facilityListView.getSelectionModel().clearSelection();
			
			facilityV2.getChildren().addAll(facilityHeader, facilityListView);
		});
		
		returnTo6.setOnAction(e -> {
			primaryStage.setScene(mainScene);
		});
		
		updateFacility.setOnAction(e -> {
		    facilityTf6.setText("");

		    String id = selectedFacilityId;
		    if ((id == null || id.isBlank()) && facilityListView != null) {
		        String row = facilityListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        facilityTf6.setText("Please select a facility from the list first.");
		        return;
		    }

		    var opt = facilityRepo.findById(id);
		    if (opt.isEmpty()) {
		        facilityTf6.setText("Selected facility no longer exists.");
		        showFacility.fire();
		        return;
		    }

		    addFacility.fire();

		    Facility f = opt.get();
		    facilityTf1.setText(ns(f.getId()));
		    facilityTf2.setText(ns(f.getName()));
		    facilityTf3.setText(ns(f.getDescription()));
		    facilityTf4.setText(ns(f.getStatus()));
		    facilityTf5.setText(String.valueOf(f.getCapacity()));
		    facilityTf6.setText("Update mode: editing " + f.getId());
		    facilityTf1.setEditable(false);
		    addFacilityTo.setText("Update Record");

		    if (originalAddFacilityHandler == null) {
		        originalAddFacilityHandler = addFacilityTo.getOnAction();
		    }
		    final String selectedId = id;
		    addFacilityTo.setOnAction(ev -> {
		        facilityTf6.setText("");
		        Facility updated = new Facility(
		            selectedId,
		            facilityTf2.getText().trim(),
		            facilityTf3.getText().trim(),
		            facilityTf4.getText().trim(),
		            Integer.parseInt(facilityTf5.getText().trim())
		        );
		        boolean ok = facilityRepo.update(updated);
		        facilityTf6.setText(ok ? "Updated " + selectedId : "Failed to update " + selectedId);

		        facilityTf1.setEditable(true);
		        addFacilityTo.setText("Add");
		        if (originalAddFacilityHandler != null) addFacilityTo.setOnAction(originalAddFacilityHandler);
		        showFacility.fire();
		    });
		});
		
		deleteFacility.setOnAction(e -> {
		    String id = selectedFacilityId;
		    if ((id == null || id.isBlank()) && facilityListView != null) {
		        String row = facilityListView.getSelectionModel().getSelectedItem();
		        if (row != null) id = leading(row, 10);
		    }
		    if (id == null || id.isBlank()) {
		        showInfo("Delete Facility", "Please select a facility to delete.");
		        return;
		    }
		    if (!confirm("Delete Facility", "Are you sure you want to delete facility " + id + "?")) return;
		    boolean ok = facilityRepo.deleteById(id);
		    showInfo("Delete Facility", ok ? "Deleted facility " + id : "Failed to delete " + id);
		    showFacility.fire();
		});

		Scene sc6 = new Scene(main8, 900, 650);
		primaryStage.setScene(sc6);
		primaryStage.show();
	}

	// ----------------------------------------------------------------------------------
	// Helpers
	
	// Tiny rounded square used as a color chip next to labels
	private static javafx.scene.layout.Region colorChip(String hex) {
	    javafx.scene.layout.Region r = new javafx.scene.layout.Region();
	    r.setMinSize(10, 10);
	    r.setPrefSize(10, 10);
	    r.setMaxSize(10, 10);
	    r.setStyle(
	        "-fx-background-color: " + hex + ";" +
	        "-fx-background-radius: 2;" +
	        "-fx-border-color: #000000;" +
	        "-fx-border-width: 1;" +
	        "-fx-border-radius: 2;"
	    );
	    return r;
	}

	// Robustly apply a pie slice color (works whether node exists yet or not)
	private static void applyPieSliceColor(javafx.scene.chart.PieChart.Data d, String hex) {
	    if (d.getNode() != null) {
	        d.getNode().setStyle("-fx-pie-color: " + hex + ";");
	    }
	    d.nodeProperty().addListener((obs, oldN, newN) -> {
	        if (newN != null) newN.setStyle("-fx-pie-color: " + hex + ";");
	    });
	}
	
	// === Notification table safety: ensure columns exist (id, severity, title, detail, seen, created_at)
	private void ensureNotificationTableColumns() {
	    try (var c = Db.get().getConnection(); var s = c.createStatement()) {
	        // Add columns if they don't exist (SQLite-friendly)
	        try { s.executeUpdate("ALTER TABLE notification ADD COLUMN seen INTEGER DEFAULT 0"); } catch (Exception ignore) {}
	        try { s.executeUpdate("ALTER TABLE notification ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignore) {}
	    } catch (Exception ignore) {}
	}

	// === Fetch unseen notifications (oldest first)
	private java.util.List<NotificationRow> fetchUnseenNotifications() {
	    ensureNotificationTableColumns();
	    java.util.List<NotificationRow> out = new java.util.ArrayList<>();
	    try (var c = Db.get().getConnection();
	         var ps = c.prepareStatement(
	             "SELECT id,severity,title,detail " +
	             "FROM notification WHERE COALESCE(seen,0)=0 " +
	             "ORDER BY COALESCE(created_at,datetime('now')) ASC, id ASC")) {
	        try (var rs = ps.executeQuery()) {
	            while (rs.next()) {
	                out.add(new NotificationRow(
	                    rs.getInt("id"),
	                    ns(rs.getString("severity")),
	                    ns(rs.getString("title")),
	                    ns(rs.getString("detail"))
	                ));
	            }
	        }
	    } catch (Exception ignore) {}
	    return out;
	}

	private void markSeen(int id) {
	    try (var c = Db.get().getConnection();
	         var ps = c.prepareStatement("UPDATE notification SET seen=1 WHERE id=?")) {
	        ps.setInt(1, id);
	        ps.executeUpdate();
	    } catch (Exception ignore) {}
	}

	// === Toast queue driver ===
	private void showAlertsOnStartup() {
	    var rows = fetchUnseenNotifications();
	    if (rows.isEmpty()) return;

	    for (var r : rows) {
	        toastQueue.add(() -> showToast(r, () -> {
	            handleToastNavigation(r);  // click → go to the right list
	            markSeen(r.id);
	        }, () -> {
	            // closed (X)
	            markSeen(r.id);
	        }));
	    }
	    playNextToast();
	}

	private void playNextToast() {
	    var job = toastQueue.pollFirst();
	    if (job != null) job.run();
	}

	// === Toast UI ===
	private void showToast(NotificationRow r, Runnable onClick, Runnable onClose) {
	    HBox box = new HBox(12);
	    box.setAlignment(Pos.CENTER_LEFT);
	    box.setStyle(
	        "-fx-background-color: #FFFFFF;" +
	        "-fx-border-color: #000000;" +
	        "-fx-border-width: 1px;" +
	        "-fx-background-radius: 10;" +
	        "-fx-border-radius: 10;" +
	        "-fx-padding: 10;"
	    );

	    Label dot = new Label("●");
	    dot.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

	    VBox text = new VBox(2);
	    Label title = new Label(r.title);
	    title.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 13px;");
	    Label detail = new Label(r.detail == null ? "" : r.detail);
	    detail.setStyle("-fx-text-fill: #000000; -fx-font-size: 12px;");
	    text.getChildren().addAll(title, detail);

	    var spacer = new StackPane();
	    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

	    Button close = new Button("✕");
	    close.setFocusTraversable(false);
	    close.setStyle("-fx-background-color: transparent; -fx-text-fill: #000000; -fx-font-size: 12px; -fx-cursor: hand;");

	    box.getChildren().addAll(dot, text, spacer, close);

	    // Click: navigate; Close: dismiss
	    box.setOnMouseClicked(e -> {
	        if (e.getTarget() != close) {
	            onClick.run();
	            dismissToast(box, this::playNextToast);
	        }
	    });
	    close.setOnAction(e -> {
	        onClose.run();
	        dismissToast(box, this::playNextToast);
	    });

	    // Add + animate
	    toastArea.getChildren().add(box);
	    toastArea.toFront();
	    box.setOpacity(0);
	    box.setTranslateY(20);

	    var fadeIn = new javafx.animation.FadeTransition(Duration.millis(180), box);
	    fadeIn.setFromValue(0); fadeIn.setToValue(1);

	    var slideIn = new javafx.animation.TranslateTransition(Duration.millis(180), box);
	    slideIn.setFromY(20); slideIn.setToY(0);

	    var showTime = new javafx.animation.PauseTransition(Duration.seconds(6));

	    var fadeOut = new javafx.animation.FadeTransition(Duration.millis(160), box);
	    fadeOut.setFromValue(1); fadeOut.setToValue(0);

	    var seq = new javafx.animation.SequentialTransition(
	        new javafx.animation.ParallelTransition(fadeIn, slideIn),
	        showTime,
	        fadeOut
	    );
	    seq.setOnFinished(e -> {
	        toastArea.getChildren().remove(box);
	        playNextToast();
	    });
	    seq.play();
	}

	private void dismissToast(javafx.scene.Node n, Runnable after) {
	    var fade = new javafx.animation.FadeTransition(Duration.millis(120), n);
	    fade.setFromValue(1); fade.setToValue(0);
	    fade.setOnFinished(e -> {
	        toastArea.getChildren().remove(n);
	        if (after != null) after.run();
	    });
	    fade.play();
	}

	// === Where a toast should send the user ===
	// Today: all your automated alerts are for Medical (low stock, expiring).
	// Extend this to route other types later (e.g., if r.title startsWith("Overdue bill:") → showPatientMenu()).
	private void handleToastNavigation(NotificationRow r) {
	    goToMedicalListAfterOpen = true;
	    showMedicalMenu();
	}
	
	private void notifyInfo(String title, String detail) { insertNotification("INFO", title, detail); }
	private void notifyWarn(String title, String detail) { insertNotification("WARN", title, detail); }

	private void insertNotification(String severity, String title, String detail) {
	    try (var c = Db.get().getConnection();
	         var ps = c.prepareStatement(
	            "INSERT INTO notification(severity,title,detail) VALUES(?,?,?)")) {
	        ps.setString(1, severity);
	        ps.setString(2, title);
	        ps.setString(3, detail);
	        ps.executeUpdate();
	    } catch (Exception ignored) {}
	}

	private int cmpId(String a, String b) {
		if (a == null && b == null) return 0;
		if (a == null) return -1;
		if (b == null) return 1;
		try {
			int ai = Integer.parseInt(a.trim());
			int bi = Integer.parseInt(b.trim());
			return Integer.compare(ai, bi);
		} catch (Exception e) {
			return a.compareToIgnoreCase(b);
		}
	}

	private void showInfo(String title, String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
	
	// Format a whole-number percentage like "28%"
	private static String pct(int part, int total) {
	    if (total <= 0) return "0%";
	    long r = Math.round(part * 100.0 / total);
	    return r + "%";
	}

	// Strip any " (…)" suffix so color lookup stays stable
	private static String baseName(String name) {
	    if (name == null) return "";
	    int i = name.indexOf(" (");
	    return i >= 0 ? name.substring(0, i) : name;
	}
	
	// === Compact Dashboard popup (bottom-left button opens this) ===
	// === Dashboard popup with visuals ===
	private void showDashboardPopup() {
		// ----- 1) Legend-style entity summary (swatches match the pie chart) -----
		Label patientsCount   = new Label();
		Label doctorsCount    = new Label();
		Label staffCount      = new Label();
		Label medicalCount    = new Label();
		Label labsCount       = new Label();
		Label facilitiesCount = new Label();
		Label notifsCount     = new Label();

		// Common styling
		java.util.function.Consumer<Label> styleCount = l ->
		    l.setStyle("-fx-text-fill:#000000; -fx-font-size:13px; -fx-font-family: " + FONT_FAMILY + ";");
		java.util.function.Function<String, Label> nameLabel = txt -> {
		    Label l = new Label(txt);
		    l.setStyle("-fx-text-fill:#000000; -fx-font-size:13px; -fx-font-weight:700; -fx-font-family: " + FONT_FAMILY + ";");
		    return l;
		};

		// Build tidy rows: [chip] [Name] ..... [Count]
		java.util.function.BiFunction<String, Label, javafx.scene.layout.HBox> row = (key, countLabel) -> {
		    String hex = ENTITY_COLORS.getOrDefault(key, "#999999");
		    var chip = colorChip(hex);
		    var name = nameLabel.apply(key);
		    var spacer = new javafx.scene.layout.Region();
		    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
		    styleCount.accept(countLabel);
		    var h = new javafx.scene.layout.HBox(8, chip, name, spacer, countLabel);
		    h.setAlignment(Pos.CENTER_LEFT);
		    return h;
		};

		var summaryTitle = new Label("Overview");
		summaryTitle.setStyle("-fx-text-fill:#000000; -fx-font-size:14px; -fx-font-weight:800; -fx-font-family: " + FONT_FAMILY + ";");

		VBox summaryBox = new VBox(
		    6,
		    summaryTitle,
		    row.apply("Patients",   patientsCount),
		    row.apply("Doctors",    doctorsCount),
		    row.apply("Staff",      staffCount),
		    row.apply("Medical",    medicalCount),
		    row.apply("Labs",       labsCount),
		    row.apply("Facilities", facilitiesCount),
		    new Label(""), // small spacer line before notifications
		    row.apply("Notifications", notifsCount)  // (keeps layout consistent)
		);
		summaryBox.setPadding(new Insets(8));

		// Update counts (numbers only, the label text is the left name)
		Runnable refreshCounts = () -> {
		    try {
		        patientsCount.setText(String.valueOf(patientRepo.findAll().size()));
		        doctorsCount.setText(String.valueOf(doctorRepo.findAll().size()));
		        staffCount.setText(String.valueOf(staffRepo.findAll().size()));
		        medicalCount.setText(String.valueOf(medicalRepo.findAll().size()));
		        labsCount.setText(String.valueOf(labRepo.findAll().size()));
		        facilitiesCount.setText(String.valueOf(facilityRepo.findAll().size()));

		        ensureNotificationTableColumns();
		        int unseen = 0;
		        try (var c = Db.get().getConnection();
		             var rs = c.createStatement().executeQuery(
		                 "SELECT COUNT(*) FROM notification WHERE COALESCE(seen,0)=0")) {
		            if (rs.next()) unseen = rs.getInt(1);
		        }
		        notifsCount.setText(String.valueOf(unseen));
		    } catch (Exception e) {
		        patientsCount.setText("?"); doctorsCount.setText("?"); staffCount.setText("?");
		        medicalCount.setText("?");  labsCount.setText("?");   facilitiesCount.setText("?");
		        notifsCount.setText("?");
		    }
		};

//	    VBox summaryBox = new VBox(6, patientsCount, doctorsCount, staffCount,
//	                               medicalCount, labsCount, facilitiesCount, notifsCount);
//	    summaryBox.setPadding(new Insets(8));
//	    for (Node n : summaryBox.getChildren()) {
//	        if (n instanceof Label l) {
//	            l.setStyle("-fx-text-fill:#000000; -fx-font-size:13px; -fx-font-weight:bold;");
//	        }
//	    }

	    // ----- 2) Visualization: Pie chart for entity distribution -----
	    PieChart distributionChart = new PieChart();
	    distributionChart.setLabelsVisible(true);
	    distributionChart.setLegendVisible(true);
	    distributionChart.setTitle("Entity Distribution");

	    Runnable refreshChart = () -> {
	        distributionChart.getData().clear();

	        int cPatients   = patientRepo.findAll().size();
	        int cDoctors    = doctorRepo.findAll().size();
	        int cStaff      = staffRepo.findAll().size();
	        int cMedical    = medicalRepo.findAll().size();
	        int cLabs       = labRepo.findAll().size();
	        int cFacilities = facilityRepo.findAll().size();

	        int total = cPatients + cDoctors + cStaff + cMedical + cLabs + cFacilities;

	        // Display names now include percentages (legend & labels will show them)
	        PieChart.Data d1 = new PieChart.Data("Patients"   + " (" + pct(cPatients,   total) + ")", cPatients);
	        PieChart.Data d2 = new PieChart.Data("Doctors"    + " (" + pct(cDoctors,    total) + ")", cDoctors);
	        PieChart.Data d3 = new PieChart.Data("Staff"      + " (" + pct(cStaff,      total) + ")", cStaff);
	        PieChart.Data d4 = new PieChart.Data("Medical"    + " (" + pct(cMedical,    total) + ")", cMedical);
	        PieChart.Data d5 = new PieChart.Data("Labs"       + " (" + pct(cLabs,       total) + ")", cLabs);
	        PieChart.Data d6 = new PieChart.Data("Facilities" + " (" + pct(cFacilities, total) + ")", cFacilities);

	        distributionChart.getData().addAll(d1, d2, d3, d4, d5, d6);

	        // Apply consistent colors using the base entity name (without % suffix)
	        for (PieChart.Data d : distributionChart.getData()) {
	            String key = baseName(d.getName()); // e.g., "Patients"
	            String color = ENTITY_COLORS.getOrDefault(key, "#999999");
	            // Node might be null until rendered; run later to be safe
	            Platform.runLater(() -> {
	                if (d.getNode() != null) d.getNode().setStyle("-fx-pie-color: " + color + ";");
	            });
	        }

	        // (Optional but nice) Show a tooltip with absolute count and percent
	        for (PieChart.Data d : distributionChart.getData()) {
	            String key = baseName(d.getName());
	            int count = (int) d.getPieValue();
	            String tip  = key + ": " + count + " (" + pct(count, total) + ")";
	            javafx.scene.control.Tooltip.install(d.getNode(), new javafx.scene.control.Tooltip(tip));
	        }
	    };

	    // ----- 3) Visualization: Bar chart for medical stock -----
	    CategoryAxis xAxis = new CategoryAxis();
	    NumberAxis yAxis = new NumberAxis();
	    BarChart<String, Number> stockChart = new BarChart<>(xAxis, yAxis);
	    stockChart.setTitle("Low Stock Medicines");
	    xAxis.setLabel("Medical ID");
	    yAxis.setLabel("Count");
	    stockChart.setLegendVisible(false);

	    Runnable refreshStock = () -> {
	        stockChart.getData().clear();
	        XYChart.Series<String, Number> series = new XYChart.Series<>();
	        try (var c = Db.get().getConnection();
	             var rs = c.createStatement().executeQuery(
	                 "SELECT id,count FROM medical ORDER BY count ASC LIMIT 5")) {
	            while (rs.next()) {
	                series.getData().add(new XYChart.Data<>(rs.getString("id"), rs.getInt("count")));
	            }
	        } catch (Exception ignored) {}
	        stockChart.getData().add(series);

	        // Apply consistent bar color (Medical = red)
	        for (XYChart.Data<String, Number> data : series.getData()) {
	            Node node = data.getNode();
	            if (node != null) {
	                node.setStyle("-fx-bar-fill: " + ENTITY_COLORS.get("Medical") + ";");
	            }
	        }
	    };

	    // ----- 4) Lists: Low stock + Expiring soon -----
	    ListView<String> lvLowStock  = makeBWListView();
	    ListView<String> lvExpSoon   = makeBWListView();
	    lvLowStock.setMinHeight(140);
	    lvExpSoon.setMinHeight(140);

	    Runnable refreshLists = () -> {
	        lvLowStock.getItems().clear();
	        lvExpSoon.getItems().clear();
	        try (var c = Db.get().getConnection()) {
	            try (var rs = c.createStatement().executeQuery(
	                    "SELECT id,name,count FROM medical WHERE count <= 10 ORDER BY count ASC LIMIT 10")) {
	                while (rs.next()) {
	                    lvLowStock.getItems().add(
	                        String.format("%-8s %-18s count=%d",
	                                      ns(rs.getString("id")), ns(rs.getString("name")), rs.getInt("count")));
	                }
	            }
	            try (var rs = c.createStatement().executeQuery(
	                    "SELECT id,name,expiry_date FROM medical " +
	                    "WHERE date(expiry_date) <= date('now','+30 day') ORDER BY date(expiry_date) ASC LIMIT 10")) {
	                while (rs.next()) {
	                    lvExpSoon.getItems().add(
	                        String.format("%-8s %-18s expires %s",
	                                      ns(rs.getString("id")), ns(rs.getString("name")), rs.getString("expiry_date")));
	                }
	            }
	        } catch (Exception ignored) {}
	    };

	    // ----- 5) Refresh button -----
	    Button refreshBtn = new Button("Refresh");
	    refreshBtn.setStyle("-fx-background-color:#FFFFFF; -fx-text-fill:#000000; -fx-border-color:#000000;");
	    refreshBtn.setOnAction(e -> {
	        refreshCounts.run();
	        refreshChart.run();
	        refreshStock.run();
	        refreshLists.run();
	    });

	    // ----- Layout -----
	    BorderPane root = new BorderPane();
	    root.setStyle("-fx-background-color:#FFFFFF;");

	    // Top row (title + refresh)
	    Label title = new Label("Dashboard");
	    title.setStyle("-fx-text-fill:#000000; -fx-font-size:16px; -fx-font-weight:bold;");
	    HBox topBar = new HBox(12, title, refreshBtn);
	    topBar.setPadding(new Insets(8));
	    root.setTop(topBar);

	    // Center area: 2 columns
	    GridPane grid = new GridPane();
	    grid.setHgap(20);
	    grid.setVgap(20);
	    grid.setPadding(new Insets(12));

	    grid.add(summaryBox, 0, 0);
	    grid.add(distributionChart, 1, 0);
	    grid.add(stockChart, 0, 1);
	    grid.add(new VBox(new Label("Low Stock"), lvLowStock), 1, 1);
	    grid.add(new VBox(new Label("Expiring Soon"), lvExpSoon), 0, 2, 2, 1);

	    root.setCenter(grid);

	    // Show stage
	    Stage s = new Stage();
	    s.initOwner(primaryStage);
	    s.setTitle("Dashboard");
	    s.setScene(new Scene(root, 850, 650));
	    refreshBtn.fire(); // auto-load
	    s.show();
	}
}
