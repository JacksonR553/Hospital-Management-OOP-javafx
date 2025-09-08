# 🏥 Hospital Management System (HMSImproved)

A **JavaFX-based Hospital Management System** built with **Java 22** and **SQLite**, designed to provide an intuitive interface for managing hospital operations such as **Staff, Doctors, Patients, Medical Inventory, Laboratories, and Facilities**.  
The system follows a **repository pattern** with CRUD operations and includes **audit logging** and a **real-time dashboard**.

## ✨ Features
- 👨‍⚕️ **Staff, Patient, Doctor, Medical, Lab & Facility Management**  
  CRUD operations (Add, Show, Update, Delete) for all core hospital entities.
- 📊 **Dashboard Overview**  
  Displays entity counts, distribution pie charts, low stock alerts, and expiry notifications.
- 📝 **Audit Logging**  
  Tracks every database change (INSERT, UPDATE, DELETE) with timestamped records.
- ⚡ **Low-Stock & Expire Alerts for Medicine Entity**  
  Real-time medicine notifications (e.g., Insulin stock and expire warning).
- 🎨 **Minimalist Black & White UI**  
  Clean interface for easy navigation and professional appearance.
- 💾 **SQLite Integration**  
  Repository pattern with SQL-based persistence.

## 📸 Screenshots

<p align="center">

  <img src="screenshots/dashboard.jpg" width="70%" alt="Dashboard Overview"/>
</p>

<p align="center">
  <img src="screenshots/staff_list.png" width="70%" alt="Staff List View"/>
</p>

<p align="center">
  <img src="screenshots/staff_form.png" width="70%" alt="Staff Form"/>
</p>

<p align="center">
  <img src="screenshots/audit_log.png" width="70%" alt="Audit Log"/>
</p>

## 🛠️ Tech Stack
- **Language:** Java 22 ☕
- **UI Framework:** JavaFX 🎨
- **Database:** SQLite 💾
- **Architecture:** Repository Pattern

## 🚀 Getting Started

### 📋 Prerequisites
- **JDK 22** (or compatible version)  
- **Eclipse IDE** (or IntelliJ IDEA with JavaFX support)

### ⚙️ Installation
```bash
# Clone the repository
git clone https://github.com/<your-username>/HMSImproved.git

# Open the project in Eclipse
# Make sure JDK 22 is configured

# Run the main file
HospitalManagement.java
````

## 🎯 Usage

1. Launch the application from `HospitalManagement.java`.
2. Use the main menu to navigate between (**CRUD operations**):
   * Staff
   * Doctor
   * Patient
   * Medical
   * Lab
   * Facility
3. Access **Dashboard** for statistics & notifications.
4. Check **Audit Log** for recorded database actions.

## 📜 Concluding Notes

Started this project back when I am taking Object-Oriented Programming during University Time. Special Thanks for @BoonChong and @JieYew for the basic work. I enhanced this project further more by adding dashboard, audit logging, automated alert via notification and integrated SQLite. Peace ✌
