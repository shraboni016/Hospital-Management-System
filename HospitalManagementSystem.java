package HospitalManagementSystem;

import java.sql.*;
import java.util.Scanner;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDate;

public class HospitalManagementSystem {
    private static final String url = "jdbc:mysql://localhost:3306/hospital";
    private static final String username = "root";
    private static final String password = "P@ssword050803";

    public static void main(String[] args) {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        Scanner scanner = new Scanner(System.in);
        try{
            Connection connection = DriverManager.getConnection(url, username, password);
            Patient patient = new Patient(connection, scanner);
            Doctor doctor = new Doctor(connection);
            while(true){
                System.out.println("HOSPITAL MANAGEMENT SYSTEM ");
                System.out.println("1. Add Patient");
                System.out.println("2. View Patients");
                System.out.println("3. View Doctors");
                System.out.println("4. Book Appointment");
                System.out.println("5. View Appointment");
                System.out.println("6. Exit");
                System.out.println("Enter your choice: ");
                int choice = scanner.nextInt();

                switch(choice){
                    case 1:
                        patient.addPatient();
                        System.out.println();
                        break;
                    case 2:
                        patient.viewPatients();
                        System.out.println();
                        break;
                    case 3:
                        doctor.viewDoctors();
                        System.out.println();
                        break;
                    case 4:
                        bookAppointment(patient, doctor, connection, scanner);
                        System.out.println();
                        break;
                    case 5:
                        viewAppointmentsMenu(connection, scanner);
                        System.out.println();
                        break;
                    case 6:
                        System.out.println("THANK YOU! FOR USING HOSPITAL MANAGEMENT SYSTEM!!");
                        return;
                    default:
                        System.out.println("Enter valid choice!!!");
                        break;
                }

            }

        }catch (SQLException e){
            e.printStackTrace();
        }
    }


    public static void bookAppointment(Patient patient, Doctor doctor, Connection connection, Scanner scanner){
        System.out.println("Available Doctors:");
        doctor.viewDoctors();
        System.out.println();
        System.out.print("Enter Patient Id: ");
        int patientId = scanner.nextInt();
        System.out.print("Enter Doctor Id: ");
        int doctorId = scanner.nextInt();
        showDoctorAppointments(doctorId, connection);

        String appointmentDate;
        while (true) {
            System.out.print("Enter appointment date (YYYY-MM-DD): ");
            appointmentDate = scanner.next();
            try {
                LocalDate date = LocalDate.parse(appointmentDate);
                LocalDate today = LocalDate.now();
                if (date.isBefore(today)) {
                    System.out.println("Error: Date must be today or in the future");
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println("Invalid date format! Please use YYYY-MM-DD format (e.g., 2023-12-31)");
            }
        }

        String appointmentTime;
        while (true) {
            System.out.print("Enter appointment time (HH:MM): ");
            appointmentTime = scanner.next();
            try {
                LocalTime time = LocalTime.parse(appointmentTime);
                if (time.isBefore(LocalTime.of(8, 0))) {
                    System.out.println("Error: Clinic opens at 8:00 AM");
                    continue;
                }
                if (time.isAfter(LocalTime.of(16, 0))) {
                    System.out.println("Error: Clinic closes at 4:00 PM");
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println("Invalid time format! Please use HH:MM format (e.g., 09:30)");
            }
        }

        if(patient.getPatientById(patientId) && doctor.getDoctorById(doctorId)){
            if(checkDoctorAvailability(doctorId, appointmentDate, appointmentTime, connection)){
                String appointmentQuery = "INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time) VALUES(?, ?, ?, ?)";
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement(appointmentQuery);
                    preparedStatement.setInt(1, patientId);
                    preparedStatement.setInt(2, doctorId);
                    preparedStatement.setString(3, appointmentDate);
                    preparedStatement.setString(4, appointmentTime);
                    int rowsAffected = preparedStatement.executeUpdate();
                    if(rowsAffected>0){
                        System.out.println("Appointment Booked!");
                    }else{
                        System.out.println("Failed to Book Appointment!");
                    }
                }catch (SQLException e){
                    e.printStackTrace();
                }
            }else{
                System.out.println("Doctor not available at this time!!");
                showAvailableSlots(doctorId, appointmentDate, connection);
            }
        }else{
            System.out.println("Either doctor or patient doesn't exist!!!");
        }
    }

    public static void showDoctorAppointments(int doctorId, Connection connection) {
        String query = "SELECT appointment_date, appointment_time FROM appointments WHERE doctor_id = ? ORDER BY appointment_date, appointment_time";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, doctorId);
            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("Doctor's Existing Appointments:");
            while(resultSet.next()) {
                System.out.println("Date: " + resultSet.getString("appointment_date") +
                        " Time: " + resultSet.getString("appointment_time"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkDoctorAvailability(int doctorId, String appointmentDate, String appointmentTime, Connection connection) {

        String countQuery = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ?";
        String timeQuery = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ?";

        try {
            PreparedStatement timeStatement = connection.prepareStatement(timeQuery);
            timeStatement.setInt(1, doctorId);
            timeStatement.setString(2, appointmentDate);
            timeStatement.setString(3, appointmentTime);
            ResultSet timeResult = timeStatement.executeQuery();
            if(timeResult.next() && timeResult.getInt(1) > 0) {
                return false;
            }

            PreparedStatement countStatement = connection.prepareStatement(countQuery);
            countStatement.setInt(1, doctorId);
            countStatement.setString(2, appointmentDate);
            ResultSet countResult = countStatement.executeQuery();
            if(countResult.next() && countResult.getInt(1) >= 5) {
                return false;
            }

            LocalTime time = LocalTime.parse(appointmentTime);
            if(time.isBefore(LocalTime.of(8, 0)) || time.isAfter(LocalTime.of(16, 0))) {
                return false;
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void showAvailableSlots(int doctorId, String appointmentDate, Connection connection) {
        System.out.println("Available time slots for doctor " + doctorId + " on " + appointmentDate + ":");

        String query = "SELECT appointment_time FROM appointments WHERE doctor_id = ? AND appointment_date = ?";
        Set<LocalTime> takenSlots = new HashSet<>();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, doctorId);
            preparedStatement.setString(2, appointmentDate);
            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()) {
                takenSlots.add(resultSet.getTime("appointment_time").toLocalTime());
            }

            LocalTime start = LocalTime.of(8, 0);
            LocalTime end = LocalTime.of(16, 0);
            int availableCount = 0;

            while(start.isBefore(end) && availableCount < (5 - takenSlots.size())) {
                if(!takenSlots.contains(start)) {
                    System.out.println(start);
                    availableCount++;
                }
                start = start.plusHours(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void viewAppointmentsMenu(Connection connection, Scanner scanner) {
        while(true) {
            System.out.println("\nAPPOINTMENT VIEWING OPTIONS");
            System.out.println("1. View All Appointments");
            System.out.println("2. View Doctor's Appointments");
            System.out.println("3. View Patient's Appointments");
            System.out.println("4. Exit to Main Menu");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();

            switch(choice) {
                case 1:
                    viewAllAppointments(connection);
                    break;
                case 2:
                    viewDoctorAppointments(connection, scanner);
                    break;
                case 3:
                    viewPatientAppointments(connection, scanner);
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    public static void viewAllAppointments(Connection connection) {
        String query = "SELECT a.id, p.name AS patient_name, d.name AS doctor_name, " +
                "a.appointment_date, a.appointment_time " +
                "FROM appointments a " +
                "JOIN patients p ON a.patient_id = p.id " +
                "JOIN doctors d ON a.doctor_id = d.id " +
                "ORDER BY a.appointment_date, a.appointment_time";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("\nALL APPOINTMENTS");
            System.out.println("+----+------------------+------------------+------------------+------------------+");
            System.out.println("| ID | Patient Name     | Doctor Name      | Date             | Time             |");
            System.out.println("+----+------------------+------------------+------------------+------------------+");

            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String patientName = resultSet.getString("patient_name");
                String doctorName = resultSet.getString("doctor_name");
                String date = resultSet.getString("appointment_date");
                String time = resultSet.getString("appointment_time");

                System.out.printf("| %-2d | %-16s | %-16s | %-16s | %-16s |\n",
                        id, patientName, doctorName, date, time);
            }

            System.out.println("+----+------------------+------------------+------------------+------------------+");
        } catch (SQLException e) {
            System.out.println("Error viewing appointments: " + e.getMessage());
        }
    }

    public static void viewDoctorAppointments(Connection connection, Scanner scanner) {
        System.out.println("\nAvailable Doctors:");
        String doctorQuery = "SELECT id, name FROM doctors";
        try {
            PreparedStatement doctorStmt = connection.prepareStatement(doctorQuery);
            ResultSet doctorRs = doctorStmt.executeQuery();

            System.out.println("+----+------------------+");
            System.out.println("| ID | Doctor Name      |");
            System.out.println("+----+------------------+");

            while(doctorRs.next()) {
                System.out.printf("| %-2d | %-16s |\n",
                        doctorRs.getInt("id"),
                        doctorRs.getString("name"));
            }
            System.out.println("+----+------------------+");
        } catch (SQLException e) {
            System.out.println("Error fetching doctors: " + e.getMessage());
            return;
        }

        System.out.print("Enter Doctor ID: ");
        int doctorId = scanner.nextInt();

        String query = "SELECT a.id, p.name AS patient_name, a.appointment_date, a.appointment_time " +
                "FROM appointments a " +
                "JOIN patients p ON a.patient_id = p.id " +
                "WHERE a.doctor_id = ? " +
                "ORDER BY a.appointment_date, a.appointment_time";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, doctorId);
            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("\nAPPOINTMENTS FOR DOCTOR ID " + doctorId);
            System.out.println("+----+------------------+------------------+------------------+");
            System.out.println("| ID | Patient Name     | Date             | Time             |");
            System.out.println("+----+------------------+------------------+------------------+");

            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String patientName = resultSet.getString("patient_name");
                String date = resultSet.getString("appointment_date");
                String time = resultSet.getString("appointment_time");

                System.out.printf("| %-2d | %-16s | %-16s | %-16s |\n",
                        id, patientName, date, time);
            }

            System.out.println("+----+------------------+------------------+------------------+");
        } catch (SQLException e) {
            System.out.println("Error viewing doctor appointments: " + e.getMessage());
        }
    }

    public static void viewPatientAppointments(Connection connection, Scanner scanner) {
        System.out.println("\nAvailable Patients:");
        String patientQuery = "SELECT id, name FROM patients";
        try {
            PreparedStatement patientStmt = connection.prepareStatement(patientQuery);
            ResultSet patientRs = patientStmt.executeQuery();

            System.out.println("+----+------------------+");
            System.out.println("| ID | Patient Name     |");
            System.out.println("+----+------------------+");

            while(patientRs.next()) {
                System.out.printf("| %-2d | %-16s |\n",
                        patientRs.getInt("id"),
                        patientRs.getString("name"));
            }
            System.out.println("+----+------------------+");
        } catch (SQLException e) {
            System.out.println("Error fetching patients: " + e.getMessage());
            return;
        }

        System.out.print("Enter Patient ID: ");
        int patientId = scanner.nextInt();

        String query = "SELECT a.id, d.name AS doctor_name, a.appointment_date, a.appointment_time " +
                "FROM appointments a " +
                "JOIN doctors d ON a.doctor_id = d.id " +
                "WHERE a.patient_id = ? " +
                "ORDER BY a.appointment_date, a.appointment_time";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, patientId);
            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("\nAPPOINTMENTS FOR PATIENT ID " + patientId);
            System.out.println("+----+------------------+------------------+------------------+");
            System.out.println("| ID | Doctor Name      | Date             | Time             |");
            System.out.println("+----+------------------+------------------+------------------+");

            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String doctorName = resultSet.getString("doctor_name");
                String date = resultSet.getString("appointment_date");
                String time = resultSet.getString("appointment_time");

                System.out.printf("| %-2d | %-16s | %-16s | %-16s |\n",
                        id, doctorName, date, time);
            }

            System.out.println("+----+------------------+------------------+------------------+");
        } catch (SQLException e) {
            System.out.println("Error viewing patient appointments: " + e.getMessage());
        }
    }

}