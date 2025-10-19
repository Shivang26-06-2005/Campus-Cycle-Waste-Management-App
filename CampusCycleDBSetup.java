import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CampusCycleDBSetup {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/";
        String user = "root";
        String password = "Shivi26@";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE DATABASE IF NOT EXISTS CampusCycleDB");
            stmt.execute("USE CampusCycleDB");
 
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(100) UNIQUE NOT NULL," +
                    "password VARCHAR(100) NOT NULL," +
                    "role ENUM('user','staff','admin') NOT NULL," +
                    "phone VARCHAR(15)," +
                    "dob DATE," +
                    "address VARCHAR(255)," +
                    "profile_picture VARCHAR(255)," +
                    "status ENUM('active','inactive','banned') DEFAULT 'active')");

            stmt.execute("CREATE TABLE IF NOT EXISTS bins (" +
                    "bin_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "location_lat DOUBLE NOT NULL," +
                    "location_lng DOUBLE NOT NULL," +
                    "status ENUM('empty','half','full') DEFAULT 'empty'," +
                    "bin_type ENUM('recyclable','non-recyclable','general') DEFAULT 'general'," +
                    "capacity INT DEFAULT 100," +
                    "fill_level INT DEFAULT 0," +
                    "zone VARCHAR(50)," +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS complaints (" +
                    "complaint_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT NOT NULL," +
                    "description TEXT," +
                    "status ENUM('pending','in-progress','resolved') DEFAULT 'pending'," +
                    "priority ENUM('low','medium','high') DEFAULT 'medium'," +
                    "staff_id INT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY (staff_id) REFERENCES users(user_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS hotspots (" +
                    "hotspot_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "location_lat DOUBLE NOT NULL," +
                    "location_lng DOUBLE NOT NULL," +
                    "risk_level ENUM('low','medium','high') DEFAULT 'low'," +
                    "hotspot_type ENUM('bin','safety','maintenance','traffic') DEFAULT 'bin'," +
                    "predicted_time TIMESTAMP," +
                    "predicted_cleanup TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                    "notification_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT," +
                    "message TEXT NOT NULL," +
                    "notification_type ENUM('info','alert','reminder') DEFAULT 'info'," +
                    "is_read BOOLEAN DEFAULT FALSE," +
                    "sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS bin_maintenance (" +
                    "maintenance_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "bin_id INT NOT NULL," +
                    "staff_id INT," +
                    "action VARCHAR(255)," +
                    "action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (bin_id) REFERENCES bins(bin_id)," +
                    "FOREIGN KEY (staff_id) REFERENCES users(user_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS bin_history (" +
                    "history_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "bin_id INT NOT NULL," +
                    "fill_level INT," +
                    "status ENUM('empty','half','full')," +
                    "recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (bin_id) REFERENCES bins(bin_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS complaint_history (" +
                    "history_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "complaint_id INT NOT NULL," +
                    "status ENUM('pending','in-progress','resolved')," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_by INT," +
                    "FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id)," +
                    "FOREIGN KEY (updated_by) REFERENCES users(user_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_activity (" +
                    "activity_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT NOT NULL," +
                    "action VARCHAR(255)," +
                    "action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id))");

            stmt.execute("INSERT INTO users (name, email, password, role, phone, dob, address) VALUES" +
                    "('Rahul Singh', 'rahul@example.com', '12345', 'user', '9876543210', '2000-01-01', 'Campus Address')," +
                    "('Admin Staff', 'admin@example.com', 'adminpass', 'admin', '9123456780', '1995-05-05', 'Admin Office')");

            stmt.execute("INSERT INTO bins (location_lat, location_lng, status, bin_type, capacity, fill_level, zone) VALUES" +
                    "(28.6139, 77.2090, 'half', 'general', 100, 50, 'Zone A')," +
                    "(28.7041, 77.1025, 'empty', 'recyclable', 100, 0, 'Zone B')");

            stmt.execute("INSERT INTO complaints (user_id, description, priority) VALUES" +
                    "(1, 'Bin near main gate is overflowing.', 'high')," +
                    "(1, 'Bin in hostel block needs repair.', 'medium')");

            stmt.execute("INSERT INTO hotspots (location_lat, location_lng, risk_level, hotspot_type, predicted_time, predicted_cleanup) VALUES" +
                    "(28.7041, 77.1025, 'high', 'bin', NOW(), NOW())," +
                    "(28.5355, 77.3910, 'medium', 'maintenance', NOW(), NOW())");

            stmt.execute("INSERT INTO notifications (user_id, message, notification_type) VALUES" + 
                    "(1, 'Your complaint has been registered successfully.', 'info')," +
                    "(2, 'System update completed.', 'alert')");

            stmt.execute("INSERT INTO bin_maintenance (bin_id, staff_id, action) VALUES" +
                    "(1, 2, 'Emptied bin')," +
                    "(2, 2, 'Checked bin')");

            stmt.execute("INSERT INTO bin_history (bin_id, fill_level, status) VALUES" +
                    "(1, 50, 'half')," +
                    "(2, 0, 'empty')");

            stmt.execute("INSERT INTO complaint_history (complaint_id, status, updated_by) VALUES" +
                    "(1, 'in-progress', 2)," +
                    "(2, 'pending', 2)");

            stmt.execute("INSERT INTO user_activity (user_id, action) VALUES" +
                    "(1, 'Submitted complaint')," +
                    "(2, 'Resolved complaint')");

            System.out.println("Database created and sample data inserted.\n");

            String[] tables = {"users", "bins", "complaints", "hotspots", "notifications", "bin_maintenance", "bin_history", "complaint_history", "user_activity"};
            for (String table : tables) {
                System.out.println("=== " + table.toUpperCase() + " ===");
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        System.out.print(rs.getMetaData().getColumnName(i) + ": " + rs.getString(i) + " | ");
                    }
                    System.out.println();
                }
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}