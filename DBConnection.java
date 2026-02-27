import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBConnection {
    
    // Updated to new school management database - using ultimate_driving_school
    private static final String URL = "jdbc:postgresql://localhost:5432/ultimate_driving_school";
    private static final String USER = "postgres";
    private static final String PASSWORD = "secret123";
    private static Connection connection = null;
    
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Attempting to connect silently
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database connection failed!");
            e.printStackTrace();
        }
        return connection;
    }
    
    /**
     * Hash password using SHA-256
     */
    public static String hashPassword(String password) {
        if (password == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    // ==================== LOCATIONS CRUD ====================
    
    public static List<Map<String, Object>> getAllLocations() {
        List<Map<String, Object>> locations = new ArrayList<>();
        String sql = "SELECT * FROM locations WHERE is_active = true ORDER BY name";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                locations.add(mapLocation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return locations;
    }
    
    public static Map<String, Object> getLocationById(int id) {
        String sql = "SELECT * FROM locations WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapLocation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static boolean updateLocationById(int locationId, String name, String address, String phone, String email, boolean isActive) {
        String sql = "UPDATE locations SET name = ?, address = ?, phone = ?, email = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, address);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            stmt.setBoolean(5, isActive);
            stmt.setInt(6, locationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static int createLocation(String name, String address, String phone, String email) {
        String sql = "INSERT INTO locations (name, address, phone, email) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, address);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    private static Map<String, Object> mapLocation(ResultSet rs) throws SQLException {
        Map<String, Object> loc = new HashMap<>();
        loc.put("id", rs.getInt("id"));
        loc.put("name", rs.getString("name"));
        loc.put("address", rs.getString("address"));
        loc.put("phone", rs.getString("phone"));
        loc.put("email", rs.getString("email"));
        loc.put("manager_id", rs.getObject("manager_id"));
        loc.put("is_active", rs.getBoolean("is_active"));
        return loc;
    }
    
    // ==================== DEPARTMENTS CRUD ====================
    
    public static List<Map<String, Object>> getAllDepartments() {
        List<Map<String, Object>> departments = new ArrayList<>();
        String sql = "SELECT * FROM departments WHERE is_active = true ORDER BY name";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                departments.add(mapDepartment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return departments;
    }
    
    private static Map<String, Object> mapDepartment(ResultSet rs) throws SQLException {
        Map<String, Object> dept = new HashMap<>();
        dept.put("id", rs.getInt("id"));
        dept.put("name", rs.getString("name"));
        dept.put("description", rs.getString("description"));
        dept.put("head_id", rs.getObject("head_id"));
        dept.put("location_id", rs.getObject("location_id"));
        dept.put("is_active", rs.getBoolean("is_active"));
        return dept;
    }
    
    public static Map<String, Object> getDepartmentById(int id) {
        String sql = "SELECT * FROM departments WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapDepartment(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static int createDepartment(String name, String description, Integer headId, Integer locationId) {
        String sql = "INSERT INTO departments (name, description, head_id, location_id) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            if (headId != null) {
                stmt.setInt(3, headId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            if (locationId != null) {
                stmt.setInt(4, locationId);
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static boolean updateDepartment(int id, String name, String description) {
        String sql = "UPDATE departments SET name = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean deleteDepartment(int id) {
        String sql = "UPDATE departments SET is_active = false, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== USER CRUD (Enhanced for School Management) ====================
    
    public static int createUser(String email, String password, String firstName, String lastName, 
                                  String phone, String role) {
        String sql = "INSERT INTO users (email, password_hash, first_name, last_name, full_name, phone, role) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password); // Already hashed by caller
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, firstName + " " + lastName);
            stmt.setString(6, phone);
            stmt.setString(7, role);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static int registerUser(String email, String password, String firstName, String lastName, 
                                   String phone, String role) {
        return createUser(email, password, firstName, lastName, phone, role);
    }
    
    public static int registerStudent(String email, String password, String firstName, String lastName, 
                                      String phone, String admissionNumber) {
        String sql = "INSERT INTO users (email, password_hash, first_name, last_name, full_name, phone, " +
                     "role, admission_number, enrollment_date) VALUES (?, ?, ?, ?, ?, ?, 'student', ?, CURRENT_DATE) " +
                     "RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, firstName + " " + lastName);
            stmt.setString(6, phone);
            stmt.setString(7, admissionNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static int registerStaff(String email, String password, String firstName, String lastName, 
                                     String phone, String position, int departmentId, int locationId) {
        String sql = "INSERT INTO users (email, password_hash, first_name, last_name, full_name, phone, " +
                     "role, position, department_id, location_id, hire_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'staff', ?, ?, ?, CURRENT_DATE) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, firstName + " " + lastName);
            stmt.setString(6, phone);
            stmt.setString(7, position);
            stmt.setInt(8, departmentId);
            stmt.setInt(9, locationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static boolean emailExists(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static Map<String, Object> getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static Map<String, Object> getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    public static List<Map<String, Object>> getUsersByRole(String role) {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, role);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    public static List<Map<String, Object>> getStudents() {
        return getUsersByRole("student");
    }
    
    public static List<Map<String, Object>> getStaff() {
        return getUsersByRole("staff");
    }
    
    public static List<Map<String, Object>> getUsersByRoles(String[] roles) {
        List<Map<String, Object>> users = new ArrayList<>();
        if (roles == null || roles.length == 0) return users;
        
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE role IN (");
        for (int i = 0; i < roles.length; i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(") ORDER BY created_at DESC");
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < roles.length; i++) {
                stmt.setString(i + 1, roles[i]);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    public static List<Map<String, Object>> getStudentsByLocation(String location) {
        List<Map<String, Object>> users = new ArrayList<>();
        // Get users with role user or applicant whose location_id matches (exclude role=student)
        String sql = "SELECT * FROM users WHERE role IN (?, ?) AND (location_id::text = ? OR training_location = ?) ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, "user");
            stmt.setString(2, "applicant");
            stmt.setString(3, location);
            stmt.setString(4, location);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    // Get students with their active enrollments/applications for attendance dropdown
    public static List<Map<String, Object>> getStudentsWithEnrollmentsForAttendance(String location) {
        List<Map<String, Object>> students = new ArrayList<>();
        // Get all users except admin
        String sql = "SELECT u.id as user_id, u.first_name, u.last_name, u.full_name, u.email, u.role, " +
                    "cr.name as course_name, cr.category as course_category, cr.id as course_id " +
                    "FROM users u " +
                    "LEFT JOIN enrollments e ON u.id = e.student_id AND e.status = 'active' " +
                    "LEFT JOIN classes c ON e.class_id = c.id " +
                    "LEFT JOIN courses cr ON c.course_id = cr.id " +
                    "WHERE u.role != 'admin' " +
                    "ORDER BY u.full_name";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> student = new HashMap<>();
                student.put("user_id", rs.getInt("user_id"));
                student.put("first_name", rs.getString("first_name"));
                student.put("last_name", rs.getString("last_name"));
                student.put("full_name", rs.getString("full_name"));
                student.put("email", rs.getString("email"));
                student.put("role", rs.getString("role"));
                student.put("course_name", rs.getString("course_name"));
                student.put("course_category", rs.getString("course_category"));
                student.put("course_id", rs.getObject("course_id"));
                students.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }
    
    private static Map<String, Object> mapUser(ResultSet rs) throws SQLException {
        Map<String, Object> user = new HashMap<>();
        user.put("id", rs.getInt("id"));
        user.put("email", rs.getString("email"));
        user.put("first_name", rs.getString("first_name"));
        user.put("last_name", rs.getString("last_name"));
        user.put("full_name", rs.getString("full_name"));
        user.put("phone", rs.getString("phone"));
        user.put("alternative_phone", rs.getString("alternative_phone"));
        user.put("date_of_birth", rs.getString("date_of_birth"));
        user.put("gender", rs.getString("gender"));
        user.put("id_number", rs.getString("id_number"));
        user.put("role", rs.getString("role"));
        user.put("status", rs.getString("status"));
        user.put("department_id", rs.getObject("department_id"));
        user.put("location_id", rs.getObject("location_id"));
        user.put("training_location", rs.getString("training_location"));
        user.put("position", rs.getString("position"));
        user.put("admission_number", rs.getString("admission_number"));
        user.put("enrollment_date", rs.getString("enrollment_date"));
        user.put("guardian_name", rs.getString("guardian_name"));
        user.put("guardian_phone", rs.getString("guardian_phone"));
        user.put("guardian_email", rs.getString("guardian_email"));
        user.put("emergency_contact_name", rs.getString("emergency_contact_name"));
        user.put("emergency_contact_phone", rs.getString("emergency_contact_phone"));
        user.put("emergency_contact_relation", rs.getString("emergency_contact_relation"));
        user.put("medical_conditions", rs.getString("medical_conditions"));
        user.put("address", rs.getString("address"));
        user.put("dob", rs.getString("date_of_birth"));
        user.put("created_at", rs.getTimestamp("created_at"));
        user.put("updated_at", rs.getTimestamp("updated_at"));
        return user;
    }
    

    
    public static boolean updateUserRole(int userId, String role) {
        String sql = "UPDATE users SET role = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean updateUserProfile(int userId, String fullName, String email, 
                                           String phone, String password, String trainingLocation) {
        // Parse full name into first and last name
        String firstName = fullName;
        String lastName = "";
        if (fullName != null && fullName.contains(" ")) {
            int lastSpaceIndex = fullName.lastIndexOf(' ');
            firstName = fullName.substring(0, lastSpaceIndex).trim();
            lastName = fullName.substring(lastSpaceIndex + 1).trim();
        }
        
        // Convert training_location ID to name if it's a number, and also get the ID
        String trainingLocationName = trainingLocation;
        Integer locationId = null;
        if (trainingLocation != null && !trainingLocation.isEmpty()) {
            try {
                locationId = Integer.parseInt(trainingLocation);
                Map<String, Object> location = getLocationById(locationId);
                if (location != null && location.get("name") != null) {
                    trainingLocationName = (String) location.get("name");
                }
            } catch (NumberFormatException e) {
                // Not a number, assume it's already a name
                trainingLocationName = trainingLocation;
            }
        }
        
        StringBuilder sql = new StringBuilder("UPDATE users SET updated_at = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();
        
        if (firstName != null) { sql.append(", first_name = ?"); params.add(firstName); }
        if (lastName != null && !lastName.isEmpty()) { sql.append(", last_name = ?"); params.add(lastName); }
        if (fullName != null) { sql.append(", full_name = ?"); params.add(fullName); }
        if (email != null) { sql.append(", email = ?"); params.add(email); }
        if (phone != null) { sql.append(", phone = ?"); params.add(phone); }
        if (password != null && !password.isEmpty()) { sql.append(", password_hash = ?"); params.add(hashPassword(password)); }
        if (locationId != null) { 
            sql.append(", location_id = ?"); params.add(locationId); 
        }
        if (trainingLocationName != null && !trainingLocationName.isEmpty()) { 
            sql.append(", training_location = ?"); params.add(trainingLocationName); 
        }
        
        sql.append(" WHERE id = ?");
        params.add(userId);
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean updateUserExtendedProfile(int userId, String fullName, String email, 
            String phone, String idno, String gender, String dob, String address, 
            String emergencyContactName, String emergencyContactPhone) {
        // Parse full name into first and last name
        String firstName = fullName;
        String lastName = "";
        if (fullName != null && fullName.contains(" ")) {
            int lastSpaceIndex = fullName.lastIndexOf(' ');
            firstName = fullName.substring(0, lastSpaceIndex).trim();
            lastName = fullName.substring(lastSpaceIndex + 1).trim();
        }
        
        StringBuilder sql = new StringBuilder("UPDATE users SET updated_at = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();
        
        if (firstName != null) { sql.append(", first_name = ?"); params.add(firstName); }
        if (lastName != null && !lastName.isEmpty()) { sql.append(", last_name = ?"); params.add(lastName); }
        if (fullName != null) { sql.append(", full_name = ?"); params.add(fullName); }
        if (email != null) { sql.append(", email = ?"); params.add(email); }
        if (phone != null) { sql.append(", phone = ?"); params.add(phone); }
        if (idno != null) { sql.append(", id_number = ?"); params.add(idno); }
        if (gender != null) { sql.append(", gender = ?"); params.add(gender); }
        if (dob != null) { sql.append(", date_of_birth = ?"); params.add(dob); }
        if (address != null) { sql.append(", address = ?"); params.add(address); }
        if (emergencyContactName != null) { sql.append(", emergency_contact_name = ?"); params.add(emergencyContactName); }
        if (emergencyContactPhone != null) { sql.append(", emergency_contact_phone = ?"); params.add(emergencyContactPhone); }
        
        sql.append(" WHERE id = ?");
        params.add(userId);
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Update user login (email and/or password)
    public static boolean updateUserLogin(int userId, String email, String newPassword) {
        StringBuilder sql = new StringBuilder("UPDATE users SET updated_at = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();
        
        if (email != null && !email.isEmpty()) {
            sql.append(", email = ?");
            params.add(email);
        }
        if (newPassword != null && !newPassword.isEmpty()) {
            sql.append(", password_hash = ?");
            params.add(hashPassword(newPassword));
        }
        
        sql.append(" WHERE id = ?");
        params.add(userId);
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean updateUserStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean deleteUser(int userId) {
        // First delete all mpesa_messages for this user's applications
        deleteMpesaMessagesByUserId(userId);
        
        // Then delete all applications for this user
        deleteApplicationsByUserId(userId);
        
        // Then delete all JWT tokens for this user
        deleteUserTokens(userId);
        
        // Now delete the user
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Delete all mpesa_messages for applications belonging to a user
    public static void deleteMpesaMessagesByUserId(int userId) {
        String sql = "DELETE FROM mpesa_messages WHERE application_id IN (SELECT id FROM applications WHERE user_id = ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Delete all applications for a specific user
    public static void deleteApplicationsByUserId(int userId) {
        String sql = "DELETE FROM applications WHERE user_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Delete all JWT tokens for a specific user
    public static void deleteUserTokens(int userId) {
        String sql = "DELETE FROM jwt_tokens WHERE user_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static Map<String, Object> validateUser(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND status = 'active'";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String inputHash = hashPassword(password);
                if (storedHash != null && storedHash.equals(inputHash)) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static Map<String, Object> authenticateUser(String email, String password) {
        return validateUser(email, password);
    }
    
    // ==================== COURSES CRUD ====================
    
    public static int createCourse(String code, String name, String description, String category,
                                   int durationHours, double price, String requirements) {
        String sql = "INSERT INTO courses (code, name, description, category, duration_hours, price, requirements) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setString(4, category);
            stmt.setInt(5, durationHours);
            stmt.setDouble(6, price);
            stmt.setString(7, requirements);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static Map<String, Object> getCourseById(int id) {
        String sql = "SELECT * FROM courses WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapCourse(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static List<Map<String, Object>> getAllCourses() {
        List<Map<String, Object>> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE is_active = true ORDER BY name";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                courses.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }
    
    // Get courses by category
    public static List<Map<String, Object>> getCoursesByCategory(String category) {
        List<Map<String, Object>> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE is_active = true AND category = ? ORDER BY name";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                courses.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }
    
    // Get courses by multiple categories
    public static List<Map<String, Object>> getCoursesByCategories(String[] categories) {
        List<Map<String, Object>> courses = new ArrayList<>();
        if (categories == null || categories.length == 0) {
            return courses;
        }
        
        // Build IN clause
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < categories.length; i++) {
            placeholders.append(i > 0 ? ",?" : "?");
        }
        
        String sql = "SELECT * FROM courses WHERE is_active = true AND category IN (" + placeholders.toString() + ") ORDER BY name, category";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < categories.length; i++) {
                stmt.setString(i + 1, categories[i].trim());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                courses.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }
    
    // Get course ID by name (for application submission)
    public static Integer getCourseIdByName(String courseName) {
        if (courseName == null || courseName.isEmpty()) return null;
        String sql = "SELECT id FROM courses WHERE name = ? AND is_active = true";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, courseName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // Get location ID by name (for application submission)
    public static Integer getLocationIdByName(String locationName) {
        if (locationName == null || locationName.isEmpty()) return null;
        // Try exact match first
        String sql = "SELECT id FROM locations WHERE name = ? AND is_active = true";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, locationName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Try partial match (contains)
        sql = "SELECT id FROM locations WHERE name LIKE ? AND is_active = true";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%" + locationName + "%");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static List<Map<String, Object>> getDrivingCourses() {
        return getCoursesByCategory("driving");
    }
    
    public static List<Map<String, Object>> getComputerCourses() {
        return getCoursesByCategory("computer");
    }
    
    private static Map<String, Object> mapCourse(ResultSet rs) throws SQLException {
        Map<String, Object> course = new HashMap<>();
        course.put("id", rs.getInt("id"));
        course.put("code", rs.getString("code"));
        course.put("name", rs.getString("name"));
        course.put("description", rs.getString("description"));
        course.put("category", rs.getString("category"));
        
        // Format duration field for frontend compatibility
        int durationHours = rs.getInt("duration_hours");
        int durationWeeks = rs.getInt("duration_weeks");
        course.put("duration_hours", durationHours);
        course.put("duration_weeks", durationWeeks);
        
        // Create formatted duration string
        String duration;
        if (durationHours > 0 && durationWeeks > 0) {
            duration = durationHours + " hours (" + durationWeeks + " weeks)";
        } else if (durationHours > 0) {
            duration = durationHours + " hours";
        } else if (durationWeeks > 0) {
            duration = durationWeeks + " weeks";
        } else {
            duration = "Contact for duration";
        }
        course.put("duration", duration);
        
        course.put("price", rs.getDouble("price"));
        course.put("requirements", rs.getString("requirements"));
        course.put("course_level", rs.getString("course_level"));
        course.put("is_licensed", rs.getBoolean("is_licensed"));
        course.put("license_type", rs.getString("license_type"));
        course.put("is_active", rs.getBoolean("is_active"));
        course.put("is_featured", rs.getBoolean("is_featured"));
        return course;
    }
    
    public static boolean updateCourse(int id, String name, String description, double price) {
        String sql = "UPDATE courses SET name = ?, description = ?, price = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setDouble(3, price);
            stmt.setInt(4, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean deleteCourse(int id) {
        String sql = "UPDATE courses SET is_active = false WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== CLASSES CRUD ====================
    
    public static int createClass(int courseId, String name, String code, int locationId,
                                  String startDate, String endDate, String startTime, String endTime,
                                  String daysOfWeek, int instructorId, int maxStudents, int currentStudents, double classFee,
                                  String description, String status) {
        String sql = "INSERT INTO classes (course_id, name, code, location_id, start_date, end_date, " +
                     "start_time, end_time, days_of_week, instructor_id, max_students, current_students, class_fee, description, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            stmt.setString(2, name);
            stmt.setString(3, code);
            stmt.setInt(4, locationId);
            
            // Handle dates safely
            if (startDate != null && !startDate.isEmpty()) {
                try { stmt.setDate(5, java.sql.Date.valueOf(startDate)); } catch (Exception e) { stmt.setDate(5, null); }
            } else { stmt.setDate(5, null); }
            
            if (endDate != null && !endDate.isEmpty()) {
                try { stmt.setDate(6, java.sql.Date.valueOf(endDate)); } catch (Exception e) { stmt.setDate(6, null); }
            } else { stmt.setDate(6, null); }
            
            // Handle times safely - convert HH:MM to HH:MM:SS format
            if (startTime != null && !startTime.isEmpty()) {
                try { 
                    String timeStr = startTime.length() == 5 ? startTime + ":00" : startTime;
                    stmt.setTime(7, java.sql.Time.valueOf(timeStr)); 
                } catch (Exception e) { stmt.setTime(7, null); }
            } else { stmt.setTime(7, null); }
            
            if (endTime != null && !endTime.isEmpty()) {
                try { 
                    String timeStr = endTime.length() == 5 ? endTime + ":00" : endTime;
                    stmt.setTime(8, java.sql.Time.valueOf(timeStr)); 
                } catch (Exception e) { stmt.setTime(8, null); }
            } else { stmt.setTime(8, null); }
            
            stmt.setString(9, daysOfWeek);
            stmt.setInt(10, instructorId);
            stmt.setInt(11, maxStudents);
            stmt.setInt(12, currentStudents);
            stmt.setDouble(13, classFee);
            stmt.setString(14, description);
            stmt.setString(15, status != null && !status.isEmpty() ? status : "scheduled");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static List<Map<String, Object>> getAllClasses() {
        List<Map<String, Object>> classes = new ArrayList<>();
        String sql = "SELECT c.*, COALESCE(cr.name, '') as course_name, COALESCE(l.name, '') as location_name, " +
                     "COALESCE(u.full_name, '') as instructor_name FROM classes c " +
                     "LEFT JOIN courses cr ON c.course_id = cr.id " +
                     "LEFT JOIN locations l ON c.location_id = l.id " +
                     "LEFT JOIN users u ON c.instructor_id = u.id " +
                     "ORDER BY c.start_date DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(mapClass(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classes;
    }
    
    public static List<Map<String, Object>> getActiveClasses() {
        List<Map<String, Object>> classes = new ArrayList<>();
        String sql = "SELECT c.*, COALESCE(cr.name, '') as course_name, COALESCE(l.name, '') as location_name, " +
                     "COALESCE(u.full_name, '') as instructor_name FROM classes c " +
                     "LEFT JOIN courses cr ON c.course_id = cr.id " +
                     "LEFT JOIN locations l ON c.location_id = l.id " +
                     "LEFT JOIN users u ON c.instructor_id = u.id " +
                     "WHERE c.status IN ('open', 'in_progress', 'scheduled') " +
                     "ORDER BY c.start_date DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(mapClass(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classes;
    }
    
    public static List<Map<String, Object>> getClassesByInstructor(int instructorId) {
        List<Map<String, Object>> classes = new ArrayList<>();
        String sql = "SELECT c.*, cr.name as course_name, l.name as location_name " +
                     "FROM classes c " +
                     "JOIN courses cr ON c.course_id = cr.id " +
                     "JOIN locations l ON c.location_id = l.id " +
                     "WHERE c.instructor_id = ? " +
                     "ORDER BY c.start_date DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(mapClass(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classes;
    }
    
    public static Map<String, Object> getClassById(int id) {
        String sql = "SELECT c.*, cr.name as course_name, l.name as location_name, " +
                     "u.full_name as instructor_name FROM classes c " +
                     "JOIN courses cr ON c.course_id = cr.id " +
                     "JOIN locations l ON c.location_id = l.id " +
                     "LEFT JOIN users u ON c.instructor_id = u.id " +
                     "WHERE c.id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapClass(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static Map<String, Object> mapClass(ResultSet rs) throws SQLException {
        Map<String, Object> cls = new HashMap<>();
        cls.put("id", rs.getInt("id"));
        cls.put("course_id", rs.getInt("course_id"));
        cls.put("name", rs.getString("name"));
        cls.put("code", rs.getString("code"));
        cls.put("description", rs.getString("description"));
        cls.put("location_id", rs.getInt("location_id"));
        cls.put("start_date", rs.getString("start_date"));
        cls.put("end_date", rs.getString("end_date"));
        cls.put("start_time", rs.getString("start_time"));
        cls.put("end_time", rs.getString("end_time"));
        cls.put("days_of_week", rs.getString("days_of_week"));
        cls.put("instructor_id", rs.getInt("instructor_id"));
        cls.put("max_students", rs.getInt("max_students"));
        cls.put("current_students", rs.getInt("current_students"));
        cls.put("status", rs.getString("status"));
        cls.put("class_fee", rs.getDouble("class_fee"));
        cls.put("course_name", rs.getString("course_name"));
        cls.put("location_name", rs.getString("location_name"));
        cls.put("instructor_name", rs.getString("instructor_name"));
        return cls;
    }
    
    public static boolean updateClassStatus(int classId, String status) {
        String sql = "UPDATE classes SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, classId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean updateClass(int classId, int courseId, String name, String code, int locationId, 
            String startDate, String endDate, String startTime, String endTime, String daysOfWeek,
            int instructorId, int maxStudents, double classFee, String description, String status) {
        String sql = "UPDATE classes SET course_id = ?, name = ?, code = ?, location_id = ?, " +
                     "start_date = ?, end_date = ?, start_time = ?, end_time = ?, days_of_week = ?, " +
                     "instructor_id = ?, max_students = ?, class_fee = ?, description = ?, status = ?, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            stmt.setString(2, name);
            stmt.setString(3, code);
            stmt.setInt(4, locationId);
            
            // Handle dates safely
            if (startDate != null && !startDate.isEmpty()) {
                try { stmt.setDate(5, java.sql.Date.valueOf(startDate)); } catch (Exception e) { stmt.setDate(5, null); }
            } else { stmt.setDate(5, null); }
            
            if (endDate != null && !endDate.isEmpty()) {
                try { stmt.setDate(6, java.sql.Date.valueOf(endDate)); } catch (Exception e) { stmt.setDate(6, null); }
            } else { stmt.setDate(6, null); }
            
            // Handle times safely - convert HH:MM to HH:MM:SS format
            if (startTime != null && !startTime.isEmpty()) {
                try { 
                    String timeStr = startTime.length() == 5 ? startTime + ":00" : startTime;
                    stmt.setTime(7, java.sql.Time.valueOf(timeStr)); 
                } catch (Exception e) { stmt.setTime(7, null); }
            } else { stmt.setTime(7, null); }
            
            if (endTime != null && !endTime.isEmpty()) {
                try { 
                    String timeStr = endTime.length() == 5 ? endTime + ":00" : endTime;
                    stmt.setTime(8, java.sql.Time.valueOf(timeStr)); 
                } catch (Exception e) { stmt.setTime(8, null); }
            } else { stmt.setTime(8, null); }
            
            stmt.setString(9, daysOfWeek);
            stmt.setInt(10, instructorId);
            stmt.setInt(11, maxStudents);
            stmt.setDouble(12, classFee);
            stmt.setString(13, description);
            stmt.setString(14, status != null && !status.isEmpty() ? status : "scheduled");
            stmt.setInt(15, classId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== ENROLLMENTS CRUD ====================
    
    public static int enrollStudent(int studentId, int classId, String enrollmentNumber, double feeAmount) {
        String sql = "INSERT INTO enrollments (student_id, class_id, enrollment_number, enrollment_date, " +
                     "fee_amount, status) VALUES (?, ?, ?, CURRENT_DATE, ?, 'enrolled') RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, classId);
            stmt.setString(3, enrollmentNumber);
            stmt.setDouble(4, feeAmount);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Update class current students count
                updateClassStudentCount(classId);
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    private static void updateClassStudentCount(int classId) {
        String sql = "UPDATE classes SET current_students = (SELECT COUNT(*) FROM enrollments WHERE class_id = ?) " +
                     "WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, classId);
            stmt.setInt(2, classId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static List<Map<String, Object>> getEnrollmentsByStudent(int studentId) {
        List<Map<String, Object>> enrollments = new ArrayList<>();
        String sql = "SELECT e.*, c.name as class_name, c.code as class_code, cr.name as course_name, " +
                     "l.name as location_name FROM enrollments e " +
                     "JOIN classes c ON e.class_id = c.id " +
                     "JOIN courses cr ON c.course_id = cr.id " +
                     "JOIN locations l ON c.location_id = l.id " +
                     "WHERE e.student_id = ? ORDER BY e.enrollment_date DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                enrollments.add(mapEnrollment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return enrollments;
    }
    
    public static List<Map<String, Object>> getAllEnrollments() {
        List<Map<String, Object>> enrollments = new ArrayList<>();
        String sql = "SELECT e.*, u.full_name as student_name, u.email as student_email, u.admission_number, " +
                     "c.name as class_name, c.code as class_code, cr.name as course_name, " +
                     "l.name as location_name FROM enrollments e " +
                     "JOIN users u ON e.student_id = u.id " +
                     "JOIN classes c ON e.class_id = c.id " +
                     "JOIN courses cr ON c.course_id = cr.id " +
                     "JOIN locations l ON c.location_id = l.id " +
                     "ORDER BY e.enrollment_date DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                enrollments.add(mapEnrollment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return enrollments;
    }
    
    private static Map<String, Object> mapEnrollment(ResultSet rs) throws SQLException {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("id", rs.getInt("id"));
        enrollment.put("student_id", rs.getInt("student_id"));
        enrollment.put("class_id", rs.getInt("class_id"));
        enrollment.put("enrollment_number", rs.getString("enrollment_number"));
        enrollment.put("enrollment_date", rs.getString("enrollment_date"));
        enrollment.put("status", rs.getString("status"));
        enrollment.put("fee_amount", rs.getDouble("fee_amount"));
        enrollment.put("fee_paid", rs.getDouble("fee_paid"));
        enrollment.put("fee_balance", rs.getDouble("fee_balance"));
        enrollment.put("payment_status", rs.getString("payment_status"));
        enrollment.put("progress_percentage", rs.getDouble("progress_percentage"));
        enrollment.put("student_name", rs.getString("student_name"));
        enrollment.put("student_email", rs.getString("student_email"));
        enrollment.put("admission_number", rs.getString("admission_number"));
        enrollment.put("class_name", rs.getString("class_name"));
        enrollment.put("class_code", rs.getString("class_code"));
        enrollment.put("course_name", rs.getString("course_name"));
        enrollment.put("location_name", rs.getString("location_name"));
        return enrollment;
    }
    
    public static boolean updateEnrollmentFees(int enrollmentId, double feePaid) {
        String sql = "UPDATE enrollments SET fee_paid = ?, fee_balance = fee_amount - ?, " +
                     "payment_status = CASE WHEN fee_amount - ? <= 0 THEN 'paid' " +
                     "WHEN fee_amount - ? > 0 AND fee_paid > 0 THEN 'partial' ELSE 'unpaid' END, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setDouble(1, feePaid);
            stmt.setDouble(2, feePaid);
            stmt.setDouble(3, feePaid);
            stmt.setDouble(4, feePaid);
            stmt.setInt(5, enrollmentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean updateEnrollmentStatus(int enrollmentId, String status) {
        String sql = "UPDATE enrollments SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, enrollmentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== ATTENDANCE CRUD ====================
    
    public static boolean markAttendance(int studentId, int classId, String date, String status) {
        String sql = "INSERT INTO attendance (student_id, class_id, date, status) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (student_id, class_id, date) DO UPDATE SET status = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, classId);
            stmt.setDate(3, java.sql.Date.valueOf(date));
            stmt.setString(4, status);
            stmt.setString(5, status);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static List<Map<String, Object>> getAttendanceByClass(int classId, String date) {
        List<Map<String, Object>> attendance = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as student_name, u.admission_number " +
                     "FROM attendance a JOIN users u ON a.student_id = u.id " +
                     "WHERE a.class_id = ? AND a.date = ? ORDER BY u.full_name";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, classId);
            stmt.setDate(2, java.sql.Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", rs.getInt("id"));
                record.put("student_id", rs.getInt("student_id"));
                record.put("class_id", rs.getInt("class_id"));
                record.put("date", rs.getString("date"));
                record.put("status", rs.getString("status"));
                record.put("student_name", rs.getString("student_name"));
                record.put("admission_number", rs.getString("admission_number"));
                attendance.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendance;
    }
    
    public static List<Map<String, Object>> getAttendanceByStudent(int studentId) {
        List<Map<String, Object>> attendance = new ArrayList<>();
        String sql = "SELECT a.*, c.name as class_name FROM attendance a " +
                     "JOIN classes c ON a.class_id = c.id " +
                     "WHERE a.student_id = ? ORDER BY a.date DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", rs.getInt("id"));
                record.put("class_id", rs.getInt("class_id"));
                record.put("date", rs.getString("date"));
                record.put("status", rs.getString("status"));
                record.put("class_name", rs.getString("class_name"));
                attendance.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendance;
    }
    
    // ==================== GRADES CRUD ====================
    
    public static int createAssessment(int classId, String name, String type, double maxScore, 
                                       double passingScore, String assessmentDate) {
        String sql = "INSERT INTO assessments (class_id, name, assessment_type, max_score, passing_score, " +
                     "assessment_date) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, classId);
            stmt.setString(2, name);
            stmt.setString(3, type);
            stmt.setDouble(4, maxScore);
            stmt.setDouble(5, passingScore);
            stmt.setDate(6, java.sql.Date.valueOf(assessmentDate));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static boolean gradeStudent(int studentId, int assessmentId, int enrollmentId, 
                                      double score, String grade) {
        String sql = "INSERT INTO grades (student_id, assessment_id, enrollment_id, score, grade) " +
                     "VALUES (?, ?, ?, ?, ?) ON CONFLICT (student_id, assessment_id) " +
                     "DO UPDATE SET score = ?, grade = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, assessmentId);
            stmt.setInt(3, enrollmentId);
            stmt.setDouble(4, score);
            stmt.setString(5, grade);
            stmt.setDouble(6, score);
            stmt.setString(7, grade);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static List<Map<String, Object>> getGradesByStudent(int studentId) {
        List<Map<String, Object>> grades = new ArrayList<>();
        String sql = "SELECT g.*, a.name as assessment_name, a.assessment_type, a.max_score, " +
                     "c.name as class_name FROM grades g " +
                     "JOIN assessments a ON g.assessment_id = a.id " +
                     "JOIN classes c ON a.class_id = c.id " +
                     "WHERE g.student_id = ? ORDER BY a.assessment_date DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", rs.getInt("id"));
                record.put("score", rs.getDouble("score"));
                record.put("grade", rs.getString("grade"));
                record.put("assessment_name", rs.getString("assessment_name"));
                record.put("assessment_type", rs.getString("assessment_type"));
                record.put("max_score", rs.getDouble("max_score"));
                record.put("class_name", rs.getString("class_name"));
                grades.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grades;
    }
    
    // ==================== FEE PAYMENTS CRUD ====================
    
    public static int recordPayment(int studentId, int enrollmentId, double amount, 
                                    String paymentMethod, String referenceNumber) {
        String sql = "INSERT INTO fee_payments (student_id, enrollment_id, amount, payment_method, " +
                     "payment_date, reference_number, status) VALUES (?, ?, ?, ?, CURRENT_DATE, ?, 'completed') " +
                     "RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, enrollmentId);
            stmt.setDouble(3, amount);
            stmt.setString(4, paymentMethod);
            stmt.setString(5, referenceNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Update enrollment
                updateEnrollmentFees(enrollmentId, amount);
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static List<Map<String, Object>> getPaymentsByStudent(int studentId) {
        List<Map<String, Object>> payments = new ArrayList<>();
        String sql = "SELECT fp.*, e.enrollment_number FROM fee_payments fp " +
                     "JOIN enrollments e ON fp.enrollment_id = e.id " +
                     "WHERE fp.student_id = ? ORDER BY fp.payment_date DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", rs.getInt("id"));
                record.put("amount", rs.getDouble("amount"));
                record.put("payment_method", rs.getString("payment_method"));
                record.put("payment_date", rs.getString("payment_date"));
                record.put("reference_number", rs.getString("reference_number"));
                record.put("status", rs.getString("status"));
                record.put("enrollment_number", rs.getString("enrollment_number"));
                payments.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }
    
    // ==================== M-PESA TRANSACTIONS CRUD ====================
    
    public static int recordMpesaTransaction(String transactionId, String phoneNumber, 
                                                double amount, String rawMessage) {
        String sql = "INSERT INTO mpesa_messages (mpesa_code, phone, amount, message, status) " +
                     "VALUES (?, ?, ?, ?, 'pending') RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, transactionId);
            stmt.setString(2, phoneNumber);
            stmt.setDouble(3, amount);
            stmt.setString(4, rawMessage);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static boolean updateMpesaStatus(int transactionId, String status) {
        String sql = "UPDATE mpesa_messages SET status = ?, " +
                     "processed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, transactionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== APPLICATIONS CRUD ====================
    
    public static int createApplication(Map<String, Object> data) {
        String sql = "INSERT INTO applications (" +
            "user_id, first_name, last_name, email, phone, date_of_birth, id_number, " +
            "address, city, postal_code, course_id, license_type, driving_course, computer_course, " +
            "location_id, training_location, transmission, preferred_schedule, preferred_start, " +
            "driving_experience, previous_driving_experience, emergency_contact_name, emergency_contact_phone, emergency_contact_relation, " +
            "medical_conditions, comments, school_fees, fees_paid, fees_balance) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, data.get("user_id") != null ? data.get("user_id") : null);
            stmt.setString(2, (String) data.get("first_name"));
            stmt.setString(3, (String) data.get("last_name"));
            stmt.setString(4, (String) data.get("email"));
            stmt.setString(5, (String) data.get("phone"));
            stmt.setObject(6, data.get("date_of_birth") != null ? java.sql.Date.valueOf((String) data.get("date_of_birth")) : null);
            stmt.setString(7, (String) data.get("id_number"));
            stmt.setString(8, (String) data.get("address"));
            stmt.setString(9, (String) data.get("city"));
            stmt.setString(10, (String) data.get("postal_code"));
            stmt.setObject(11, data.get("course_id") != null ? Integer.parseInt(data.get("course_id").toString()) : null);
            stmt.setString(12, (String) data.get("license_type"));
            stmt.setString(13, (String) data.get("driving_course"));
            stmt.setString(14, (String) data.get("computer_course"));
            stmt.setObject(15, data.get("location_id") != null ? Integer.parseInt(data.get("location_id").toString()) : null);
            stmt.setString(16, (String) data.get("training_location"));
            stmt.setString(17, (String) data.get("transmission"));
            stmt.setString(18, (String) data.get("preferred_schedule"));
            stmt.setObject(19, data.get("preferred_start") != null ? java.sql.Date.valueOf((String) data.get("preferred_start")) : null);
            stmt.setString(20, (String) data.get("driving_experience"));
            stmt.setBoolean(21, data.get("previous_driving_experience") != null ? (Boolean) data.get("previous_driving_experience") : false);
            stmt.setString(22, (String) data.get("emergency_contact_name"));
            stmt.setString(23, (String) data.get("emergency_contact_phone"));
            stmt.setString(24, (String) data.get("emergency_contact_relation"));
            stmt.setString(25, (String) data.get("medical_conditions"));
            stmt.setString(26, (String) data.get("comments"));
            stmt.setDouble(27, data.get("school_fees") != null ? Double.parseDouble(data.get("school_fees").toString()) : 0.0);
            stmt.setDouble(28, data.get("fees_paid") != null ? Double.parseDouble(data.get("fees_paid").toString()) : 0.0);
            stmt.setDouble(29, data.get("fees_balance") != null ? Double.parseDouble(data.get("fees_balance").toString()) : 0.0);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static List<Map<String, Object>> getAllApplications() {
        List<Map<String, Object>> applications = new ArrayList<>();
        String sql = "SELECT a.*, c.name as course_name, l.name as location " +
                     "FROM applications a " +
                     "LEFT JOIN courses c ON a.course_id = c.id " +
                     "LEFT JOIN locations l ON a.location_id = l.id " +
                     "ORDER BY a.created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                applications.add(mapApplication(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return applications;
    }
    
    public static boolean updateApplicationStatus(int applicationId, String status) {
        String sql = "UPDATE applications SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, applicationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private static Map<String, Object> mapApplication(ResultSet rs) throws SQLException {
        Map<String, Object> app = new HashMap<>();
        app.put("id", rs.getInt("id"));
        app.put("user_id", rs.getObject("user_id"));
        app.put("first_name", rs.getString("first_name"));
        app.put("last_name", rs.getString("last_name"));
        app.put("email", rs.getString("email"));
        app.put("phone", rs.getString("phone"));
        app.put("date_of_birth", rs.getString("date_of_birth"));
        app.put("id_number", rs.getString("id_number"));
        app.put("address", rs.getString("address"));
        app.put("city", rs.getString("city"));
        app.put("postal_code", rs.getString("postal_code"));
        app.put("course_id", rs.getObject("course_id"));
        app.put("license_type", rs.getString("license_type"));
        app.put("driving_course", rs.getString("driving_course"));
        app.put("computer_course", rs.getString("computer_course"));
        app.put("location", rs.getString("location"));
        app.put("location_id", rs.getObject("location_id"));
        app.put("training_location", rs.getString("training_location"));
        app.put("transmission", rs.getString("transmission"));
        app.put("preferred_schedule", rs.getString("preferred_schedule"));
        app.put("preferred_start", rs.getString("preferred_start"));
        app.put("driving_experience", rs.getString("driving_experience"));
        app.put("previous_driving_experience", rs.getBoolean("previous_driving_experience"));
        app.put("emergency_contact_name", rs.getString("emergency_contact_name"));
        app.put("emergency_contact_phone", rs.getString("emergency_contact_phone"));
        app.put("emergency_contact_relation", rs.getString("emergency_contact_relation"));
        app.put("emergency_contact_relation", rs.getString("emergency_contact_relation"));
        app.put("medical_conditions", rs.getString("medical_conditions"));
        app.put("comments", rs.getString("comments"));
        app.put("school_fees", rs.getDouble("school_fees"));
        app.put("fees_paid", rs.getDouble("fees_paid"));
        app.put("fees_balance", rs.getDouble("fees_balance"));
        app.put("payment_status", rs.getString("payment_status"));
        app.put("payment_method", rs.getString("payment_method"));
        app.put("status", rs.getString("status"));
        app.put("staff_id", rs.getObject("staff_id"));
        app.put("course_name", rs.getString("course_name"));
        app.put("location_name", rs.getString("location"));
        app.put("created_at", rs.getTimestamp("created_at"));
        app.put("updated_at", rs.getTimestamp("updated_at"));
        return app;
    }
    
    // ==================== CONTACT MESSAGES CRUD ====================
    
    public static int createContactMessage(String name, String email, String phone, 
                                          String subject, String message) {
        String sql = "INSERT INTO contact_messages (name, email, phone, subject, message) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, subject);
            stmt.setString(5, message);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static List<Map<String, Object>> getAllContactMessages() {
        List<Map<String, Object>> messages = new ArrayList<>();
        String sql = "SELECT * FROM contact_messages ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("id", rs.getInt("id"));
                msg.put("name", rs.getString("name"));
                msg.put("email", rs.getString("email"));
                msg.put("phone", rs.getString("phone"));
                msg.put("subject", rs.getString("subject"));
                msg.put("message", rs.getString("message"));
                msg.put("status", rs.getString("status"));
                msg.put("created_at", rs.getTimestamp("created_at"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    
    public static boolean updateContactStatus(int messageId, String status) {
        String sql = "UPDATE contact_messages SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, messageId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean deleteContactMessage(int messageId) {
        String sql = "DELETE FROM contact_messages WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== JWT TOKENS ====================
    
    public static void saveToken(String token, int userId) {
        String sql = "INSERT INTO jwt_tokens (token, user_id, expires_at) VALUES (?, ?, " +
                     "(CURRENT_TIMESTAMP + INTERVAL '24 hours'))";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void revokeToken(String token) {
        String sql = "UPDATE jwt_tokens SET is_revoked = true, revoked_at = CURRENT_TIMESTAMP WHERE token = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static boolean isTokenValid(String token) {
        String sql = "SELECT id FROM jwt_tokens WHERE token = ? AND is_revoked = false AND expires_at > CURRENT_TIMESTAMP";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== DASHBOARD STATISTICS ====================
    
    public static Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total students
        stats.put("total_students", getUsersByRole("student").size());
        
        // Total staff
        stats.put("total_staff", getUsersByRole("staff").size());
        
        // Total courses
        stats.put("total_courses", getAllCourses().size());
        
        // Active classes
        stats.put("active_classes", getActiveClasses().size());
        
        // Active enrollments
        stats.put("active_enrollments", getAllEnrollments().size());
        
        // Pending applications
        List<Map<String, Object>> pendingApps = getAllApplications().stream()
            .filter(a -> "pending".equals(a.get("status")))
            .toList();
        stats.put("pending_applications", pendingApps.size());
        
        // New messages
        List<Map<String, Object>> newMessages = getAllContactMessages().stream()
            .filter(m -> "new".equals(m.get("status")))
            .toList();
        stats.put("new_messages", newMessages.size());
        
        return stats;
    }
    
    // ==================== BACKWARD COMPATIBILITY METHODS =====================
    
    // Alias for getAllCourses (old method name)
    public static List<Map<String, Object>> getCourses() {
        return getAllCourses();
    }
    
    // Alias for getUsersByRole("staff")
    public static List<Map<String, Object>> getAllStaff() {
        return getUsersByRole("staff");
    }
    
    // Alias for getApplicationsByUserId
    public static List<Map<String, Object>> getUserEnrollmentsByStudent(int studentId) {
        return getEnrollmentsByStudent(studentId);
    }
    
    // Get application by ID
    public static Map<String, Object> getApplicationById(int id) {
        String sql = "SELECT a.*, c.name as course_name, l.name as location, " +
                     "u.id_number, u.date_of_birth, u.city, u.address, u.full_name " +
                     "FROM applications a " +
                     "LEFT JOIN courses c ON a.course_id = c.id " +
                     "LEFT JOIN locations l ON a.location_id = l.id " +
                     "LEFT JOIN users u ON a.user_id = u.id " +
                     "WHERE a.id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapApplication(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // Get applications by user ID (alias for enrollments)
    public static List<Map<String, Object>> getApplicationsByUserId(int userId) {
        return getEnrollmentsByStudent(userId);
    }
    
    // Get actual applications from applications table for a user
    public static List<Map<String, Object>> getApplicationsDataByUserId(int userId) {
        List<Map<String, Object>> applications = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name, u.email, u.phone, u.training_location, l.name as location_name " +
                     "FROM applications a " +
                     "JOIN users u ON a.user_id = u.id " +
                     "LEFT JOIN locations l ON a.location_id = l.id " +
                     "WHERE a.user_id = ? " +
                     "ORDER BY a.created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> app = new HashMap<>();
                app.put("id", rs.getInt("id"));
                app.put("user_id", rs.getInt("user_id"));
                app.put("first_name", rs.getString("first_name"));
                app.put("last_name", rs.getString("last_name"));
                app.put("full_name", rs.getString("full_name"));
                app.put("email", rs.getString("email"));
                app.put("phone", rs.getString("phone"));
                app.put("id_number", rs.getString("id_number"));
                app.put("date_of_birth", rs.getString("date_of_birth"));
                app.put("address", rs.getString("address"));
                app.put("city", rs.getString("city"));
                app.put("postal_code", rs.getString("postal_code"));
                app.put("training_location", rs.getString("training_location"));
                app.put("location_id", rs.getObject("location_id"));
                app.put("location", rs.getString("location_name"));
                app.put("driving_course", rs.getString("driving_course"));
                app.put("computer_course", rs.getString("computer_course"));
                app.put("license_type", rs.getString("license_type"));
                app.put("transmission", rs.getString("transmission"));
                app.put("preferred_start", rs.getString("preferred_start"));
                app.put("preferred_schedule", rs.getString("preferred_schedule"));
                app.put("emergency_contact_name", rs.getString("emergency_contact_name"));
                app.put("emergency_contact_phone", rs.getString("emergency_contact_phone"));
                app.put("emergency_contact_relation", rs.getString("emergency_contact_relation"));
                app.put("emergency_contact_relation", rs.getString("emergency_contact_relation"));
                app.put("status", rs.getString("status"));
                app.put("school_fees", rs.getDouble("school_fees"));
                app.put("fees_paid", rs.getDouble("fees_paid"));
                app.put("fees_balance", rs.getDouble("fees_balance"));
                app.put("payment_status", rs.getString("payment_status"));
                app.put("created_at", rs.getString("created_at"));
                app.put("updated_at", rs.getString("updated_at"));
                applications.add(app);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return applications;
    }
    
    // Get user location - checks both location_id and training_location
    public static String getUserLocation(int userId) {
        Map<String, Object> user = getUserById(userId);
        if (user != null) {
            // First check location_id (INTEGER reference to locations table)
            if (user.get("location_id") != null) {
                try {
                    Map<String, Object> location = getLocationById((Integer) user.get("location_id"));
                    if (location != null && location.get("name") != null) {
                        return (String) location.get("name");
                    }
                } catch (Exception e) {
                    // Ignore - location_id might not be valid
                }
            }
            // Fall back to training_location (VARCHAR direct value)
            if (user.get("training_location") != null) {
                String trainingLocation = (String) user.get("training_location");
                if (!trainingLocation.isEmpty()) {
                    return trainingLocation;
                }
            }
        }
        return "";
    }
    
    // Update staff location
    public static boolean updateStaffLocation(int userId, String location) {
        // Find location ID by name
        List<Map<String, Object>> locations = getAllLocations();
        Integer locationId = null;
        for (Map<String, Object> loc : locations) {
            if (location.equals(loc.get("name"))) {
                locationId = (Integer) loc.get("id");
                break;
            }
        }
        if (locationId == null) return false;
        
        String sql = "UPDATE users SET location_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, locationId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Update user location by location_id directly
    public static boolean updateUserLocationById(int userId, int locationId) {
        String sql = "UPDATE users SET location_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, locationId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Calculate fees (simplified)
    public static double calculateFees(String drivingCourse, String computerCourse) {
        double fees = 0.0;
        if (drivingCourse != null && !drivingCourse.isEmpty()) {
            fees += 15000.0; // Default driving course fee
        }
        if (computerCourse != null && !computerCourse.isEmpty()) {
            fees += 5000.0; // Default computer course fee
        }
        return fees;
    }
    
    // Update user fees
    public static boolean updateUserFees(int userId, double schoolFees, double feesPaid, double feesBalance, String paymentStatus) {
        String sql = "UPDATE users SET school_fees = ?, fees_paid = ?, fees_balance = ?, payment_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setDouble(1, schoolFees);
            stmt.setDouble(2, feesPaid);
            stmt.setDouble(3, feesBalance);
            stmt.setString(4, paymentStatus);
            stmt.setInt(5, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Get locations as strings (for backward compatibility)
    public static List<String> getAllLocationsList() {
        List<String> locations = new ArrayList<>();
        List<Map<String, Object>> locs = getAllLocations();
        for (Map<String, Object> loc : locs) {
            locations.add((String) loc.get("name"));
        }
        return locations;
    }
    
    // Get applications by location
    public static List<Map<String, Object>> getApplicationsByLocation(String location) {
        // Find location ID
        List<Map<String, Object>> locations = getAllLocations();
        Integer locationId = null;
        for (Map<String, Object> loc : locations) {
            if (location.equals(loc.get("name"))) {
                locationId = (Integer) loc.get("id");
                break;
            }
        }
        
        if (locationId == null) return new ArrayList<>();
        
        List<Map<String, Object>> apps = new ArrayList<>();
        String sql = "SELECT a.*, u.id as user_id, u.id_number, u.date_of_birth, u.city, u.address, u.full_name, u.email as user_email, " +
                     "u.emergency_contact_name, u.emergency_contact_phone, u.emergency_contact_relation, " +
                     "c.name as course_name " +
                     "FROM applications a " +
                     "LEFT JOIN courses c ON a.course_id = c.id " +
                     "LEFT JOIN users u ON a.user_id = u.id " +
                     "WHERE a.location_id = ? ORDER BY a.created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, locationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> app = new HashMap<>();
                app.put("id", rs.getInt("id"));
                app.put("user_id", rs.getInt("user_id"));
                app.put("first_name", rs.getString("first_name"));
                app.put("last_name", rs.getString("last_name"));
                app.put("full_name", rs.getString("full_name"));
                app.put("email", rs.getString("user_email"));
                app.put("phone", rs.getString("phone"));
                app.put("id_number", rs.getString("id_number"));
                app.put("date_of_birth", rs.getString("date_of_birth"));
                app.put("city", rs.getString("city"));
                app.put("address", rs.getString("address"));
                app.put("course_name", rs.getString("course_name"));
                app.put("status", rs.getString("status"));
                app.put("school_fees", rs.getDouble("school_fees"));
                app.put("fees_paid", rs.getDouble("fees_paid"));
                app.put("fees_balance", rs.getDouble("fees_balance"));
                app.put("payment_status", rs.getString("payment_status"));
                app.put("driving_course", rs.getString("driving_course"));
                app.put("computer_course", rs.getString("computer_course"));
                app.put("license_type", rs.getString("license_type"));
                app.put("transmission", rs.getString("transmission"));
                app.put("training_location", location);
                // Emergency contact fields from users table
                app.put("emergency_contact_name", rs.getString("emergency_contact_name"));
                app.put("emergency_contact_phone", rs.getString("emergency_contact_phone"));
                app.put("emergency_contact_relation", rs.getString("emergency_contact_relation"));
                app.put("preferred_schedule", rs.getString("preferred_schedule"));
                app.put("created_at", rs.getString("created_at"));
                app.put("updated_at", rs.getString("updated_at"));
                apps.add(app);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apps;
    }
    
    // Submit application (wrapper for createApplication)
    public static int submitApplication(Map<String, Object> data) {
        return createApplication(data);
    }
    
    // Update application
    public static boolean updateApplication(Map<String, Object> data) {
        Integer id = (Integer) data.get("id");
        if (id == null) return false;
        
        // Build dynamic SET clause and collect params with their types
        StringBuilder sql = new StringBuilder("UPDATE applications SET updated_at = CURRENT_TIMESTAMP");
        List<String> paramNames = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        
        // Add all fields that are provided
        String[] stringFields = {"first_name", "last_name", "email", "phone", "address", "city", 
            "postal_code", "id_number", "license_type", "driving_course", "computer_course", 
            "training_location", "transmission", "emergency_contact_name", "emergency_contact_phone", 
            "emergency_contact_relation", "comments", "medical_conditions"};
        
        for (String field : stringFields) {
            if (data.containsKey(field)) {
                sql.append(", ").append(field).append(" = ?");
                paramNames.add(field);
                params.add(data.get(field));
            }
        }
        
        // Date fields (only date_of_birth is a date type) - use CAST for PostgreSQL
        if (data.containsKey("date_of_birth")) {
            sql.append(", date_of_birth = CAST(? AS DATE)");
            paramNames.add("date_of_birth");
            params.add(data.get("date_of_birth"));
        }
        
        // preferred_schedule is now VARCHAR (Morning, Noon, Evening)
        if (data.containsKey("preferred_schedule")) {
            sql.append(", preferred_schedule = ?");
            paramNames.add("preferred_schedule");
            params.add(data.get("preferred_schedule"));
        }
        
        // preferred_start is a DATE field
        if (data.containsKey("preferred_start")) {
            sql.append(", preferred_start = CAST(? AS DATE)");
            paramNames.add("preferred_start");
            params.add(data.get("preferred_start"));
        }
        
        // Boolean field
        if (data.containsKey("previous_driving_experience")) {
            sql.append(", previous_driving_experience = ?");
            paramNames.add("previous_driving_experience");
            params.add(data.get("previous_driving_experience"));
        }
        
        // Numeric fields
        String[] numericFields = {"school_fees", "fees_paid", "fees_balance", "location_id"};
        for (String field : numericFields) {
            if (data.containsKey(field)) {
                sql.append(", ").append(field).append(" = ?");
                paramNames.add(field);
                params.add(data.get(field));
            }
        }
        
        // String enum fields
        String[] enumFields = {"payment_status", "payment_method", "status"};
        for (String field : enumFields) {
            if (data.containsKey(field)) {
                sql.append(", ").append(field).append(" = ?");
                paramNames.add(field);
                params.add(data.get(field));
            }
        }
        
        sql.append(" WHERE id = ?");
        paramNames.add("id");
        params.add(id);
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                String paramName = paramNames.get(i);
                
                if (param == null) {
                    stmt.setNull(i + 1, java.sql.Types.VARCHAR);
                } else if (param instanceof Boolean) {
                    stmt.setBoolean(i + 1, (Boolean) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (paramName.equals("date_of_birth")) {
                    // Handle date_of_birth - PostgreSQL CAST will convert string to date
                    stmt.setString(i + 1, param.toString());
                } else {
                    stmt.setString(i + 1, param.toString());
                }
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Update application fees
    public static boolean updateApplicationFees(int applicationId, String schoolFees, String feesPaid, 
                                                 String feesBalance, String paymentStatus, String paymentMethod) {
        String sql = "UPDATE applications SET school_fees = ?, fees_paid = ?, fees_balance = ?, " +
                     "payment_status = ?, payment_method = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setDouble(1, Double.parseDouble(schoolFees));
            stmt.setDouble(2, Double.parseDouble(feesPaid));
            stmt.setDouble(3, Double.parseDouble(feesBalance));
            stmt.setString(4, paymentStatus);
            stmt.setString(5, paymentMethod);
            stmt.setInt(6, applicationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Update student details
    public static boolean updateStudentDetails(Map<String, Object> data) {
        Integer userId = (Integer) data.get("user_id");
        if (userId == null) return false;
        
        String sql = "UPDATE users SET " +
                     "first_name = ?, last_name = ?, phone = ?, alternative_phone = ?, " +
                     "date_of_birth = CAST(? AS date), gender = ?, id_number = ?, guardian_name = ?, " +
                     "guardian_phone = ?, guardian_email = ?, emergency_contact_name = ?, " +
                     "emergency_contact_phone = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, (String) data.get("first_name"));
            stmt.setString(2, (String) data.get("last_name"));
            stmt.setString(3, (String) data.get("phone"));
            stmt.setString(4, (String) data.get("alternative_phone"));
            stmt.setString(5, (String) data.get("date_of_birth"));
            stmt.setString(6, (String) data.get("gender"));
            stmt.setString(7, (String) data.get("id_number"));
            stmt.setString(8, (String) data.get("guardian_name"));
            stmt.setString(9, (String) data.get("guardian_phone"));
            stmt.setString(10, (String) data.get("guardian_email"));
            stmt.setString(11, (String) data.get("emergency_contact_name"));
            stmt.setString(12, (String) data.get("emergency_contact_phone"));
            stmt.setInt(13, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Update application location
    public static boolean updateApplicationLocation(int applicationId, String location) {
        // Find location ID (case-insensitive match)
        List<Map<String, Object>> locations = getAllLocations();
        Integer locationId = null;
        for (Map<String, Object> loc : locations) {
            String locName = (String) loc.get("name");
            if (locName != null && locName.equalsIgnoreCase(location)) {
                locationId = (Integer) loc.get("id");
                break;
            }
        }
        
        // Update both location_id (if found) and training_location (always)
        String sql = "UPDATE applications SET training_location = ?";
        if (locationId != null) {
            sql += ", location_id = ?";
        }
        sql += ", updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, location);
            if (locationId != null) {
                stmt.setInt(paramIndex++, locationId);
            }
            stmt.setInt(paramIndex, applicationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Delete application by ID
    public static boolean deleteApplication(int applicationId) {
        String sql = "DELETE FROM applications WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Delete all applications by location ID
    public static int deleteApplicationsByLocationId(int locationId) {
        String sql = "DELETE FROM applications WHERE location_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, locationId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    // Get fees summary by location
    public static Map<String, Object> getFeesSummaryByLocation(String location) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("total_fees", 0.0);
        summary.put("total_paid", 0.0);
        summary.put("total_balance", 0.0);
        summary.put("students_count", 0);
        
        // Find location ID
        List<Map<String, Object>> locations = getAllLocations();
        Integer locationId = null;
        for (Map<String, Object> loc : locations) {
            if (location.equals(loc.get("name"))) {
                locationId = (Integer) loc.get("id");
                break;
            }
        }
        
        if (locationId == null) return summary;
        
        String sql = "SELECT COUNT(*) as count, COALESCE(SUM(school_fees), 0) as total_fees, " +
                     "COALESCE(SUM(fees_paid), 0) as total_paid, " +
                     "COALESCE(SUM(fees_balance), 0) as total_balance " +
                     "FROM applications WHERE location_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, locationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                summary.put("students_count", rs.getInt("count"));
                summary.put("total_fees", rs.getDouble("total_fees"));
                summary.put("total_paid", rs.getDouble("total_paid"));
                summary.put("total_balance", rs.getDouble("total_balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }
    
    // Get staff assigned applications
    public static List<Map<String, Object>> getStaffAssignedApplications(int staffId) {
        return getApplicationsByStaffId(staffId);
    }
    
    // Get applications by staff ID
    public static List<Map<String, Object>> getApplicationsByStaffId(int staffId) {
        Map<String, Object> staff = getUserById(staffId);
        if (staff == null || staff.get("location_id") == null) {
            return new ArrayList<>();
        }
        
        String locationId = staff.get("location_id").toString();
        return getApplicationsByLocation(locationId);
    }
    
    // Revoke all user tokens
    public static void revokeAllUserTokens(int userId) {
        String sql = "UPDATE jwt_tokens SET is_revoked = true, revoked_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // M-Pesa message methods - uses mpesa_messages table from UltimateServer.java
    public static int addMpesaMessage(int applicationId, String message, String phone, double amount, String mpesaCode) {
        String sql = "INSERT INTO mpesa_messages (mpesa_code, phone, amount, message, application_id, status) " +
                     "VALUES (?, ?, ?, ?, ?, 'pending') RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, mpesaCode != null ? mpesaCode : "TXN" + System.currentTimeMillis());
            stmt.setString(2, phone);
            stmt.setDouble(3, amount);
            stmt.setString(4, message);
            stmt.setInt(5, applicationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public static List<Map<String, Object>> getMpesaMessagesByUserId(int userId) {
        return getPaymentsByStudent(userId);
    }
    
    public static List<Map<String, Object>> getMpesaMessagesByAppId(int applicationId) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String sql = "SELECT * FROM mpesa_messages WHERE application_id = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("id", rs.getInt("id"));
                msg.put("transaction_id", rs.getString("mpesa_code"));  // Use mpesa_code column
                msg.put("phone_number", rs.getString("phone"));        // Use phone column
                msg.put("amount", rs.getDouble("amount"));
                msg.put("raw_message", rs.getString("raw_message"));
                msg.put("status", rs.getString("status"));
                msg.put("created_at", rs.getTimestamp("created_at"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    
    public static List<Map<String, Object>> getMpesaMessagesByPhone(String phone) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String sql = "SELECT * FROM mpesa_messages WHERE phone = ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("id", rs.getInt("id"));
                msg.put("transaction_id", rs.getString("mpesa_code"));
                msg.put("amount", rs.getDouble("amount"));
                msg.put("status", rs.getString("status"));
                msg.put("created_at", rs.getTimestamp("created_at"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    
    public static List<Map<String, Object>> getMpesaMessagesByLocation(String location, String status) {
        // This is a simplified implementation
        List<Map<String, Object>> messages = new ArrayList<>();
        String sql = "SELECT m.* FROM mpesa_messages m " +
                     "JOIN applications a ON m.application_id = a.id " +
                     "JOIN users u ON a.user_id = u.id " +
                     "WHERE 1=1";
        
        if (status != null && !status.isEmpty()) {
            sql += " AND m.status = '" + status + "'";
        }
        
        sql += " ORDER BY m.created_at DESC LIMIT 100";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("id", rs.getInt("id"));
                msg.put("transaction_id", rs.getString("mpesa_code"));
                msg.put("phone_number", rs.getString("phone"));
                msg.put("amount", rs.getDouble("amount"));
                msg.put("status", rs.getString("status"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    
    public static boolean verifyMpesaMessage(int messageId, boolean verified) {
        String sql = "UPDATE mpesa_messages SET status = ?, " +
                     "processed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, verified ? "completed" : "failed");
            stmt.setInt(2, messageId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Contact message backward compatibility
    public static boolean isDuplicateContactMessage(String email, String message) {
        String sql = "SELECT id FROM contact_messages WHERE email = ? AND message LIKE ? AND created_at > (CURRENT_TIMESTAMP - INTERVAL '1 hour')";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, "%" + message.substring(0, Math.min(50, message.length())) + "%");
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static int saveContactMessage(String name, String email, String phone, String subject, String message) {
        return createContactMessage(name, email, phone, subject, message);
    }
    
    public static boolean updateContactMessageStatus(int messageId, String status) {
        return updateContactStatus(messageId, status);
    }
    
    // Course methods with old signatures
    public static int createCourse(String name, String description, String duration, double price, String requirements) {
        // Default values for missing fields
        String code = "CRS" + System.currentTimeMillis();
        String category = "general";
        int durationHours = 20;
        
        try {
            durationHours = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
        } catch (Exception e) {}
        
        return createCourse(code, name, description, category, durationHours, price, requirements);
    }
    
    public static boolean updateCourse(int courseId, String name, String description, String duration, double price, String requirements) {
        // Parse duration string to extract hours and weeks
        int durationHours = 0;
        int durationWeeks = 0;
        
        if (duration != null && !duration.isEmpty()) {
            // Try to extract hours (e.g., "30 hours" or "30")
            String hoursLower = duration.toLowerCase();
            if (hoursLower.contains("hour")) {
                try {
                    String hoursPart = hoursLower.split("hour")[0].trim();
                    // Find the number in the string
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(hoursPart);
                    if (matcher.find()) {
                        durationHours = Integer.parseInt(matcher.group(1));
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }
            
            // Try to extract weeks (e.g., "4 weeks" or "(4 weeks)")
            if (hoursLower.contains("week")) {
                try {
                    String weeksPart = hoursLower.split("week")[0].trim();
                    // Find the number in parentheses or before
                    if (weeksPart.endsWith("(")) {
                        weeksPart = weeksPart + ")";
                    }
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(weeksPart);
                    if (matcher.find()) {
                        durationWeeks = Integer.parseInt(matcher.group(1));
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }
            
            // If no hours/weeks found, try to parse as a simple number
            if (durationHours == 0 && durationWeeks == 0) {
                try {
                    durationHours = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }
        }
        
        String sql = "UPDATE courses SET name = ?, description = ?, duration_hours = ?, duration_weeks = ?, price = ?, requirements = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, durationHours);
            stmt.setInt(4, durationWeeks);
            stmt.setDouble(5, price);
            stmt.setString(6, requirements);
            stmt.setInt(7, courseId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // ==================== MPESA TRANSACTIONS ====================
    
    /**
     * Insert a new M-Pesa transaction
     */
    public static int insertMpesaTransaction(String merchantRequestId, String checkoutRequestId,
                                             String phoneNumber, double amount, String transactionType,
                                             Integer applicationId, String status) {
        String sql = "INSERT INTO mpesa_transactions (merchant_request_id, checkout_request_id, " +
                     "phone_number, amount, transaction_type, application_id, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, merchantRequestId);
            stmt.setString(2, checkoutRequestId);
            stmt.setString(3, phoneNumber);
            stmt.setDouble(4, amount);
            stmt.setString(5, transactionType);
            if (applicationId != null) {
                stmt.setInt(6, applicationId);
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }
            stmt.setString(7, status);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    /**
     * Update M-Pesa transaction with M-Pesa response
     */
    public static boolean updateMpesaTransaction(int transactionId, String merchantRequestId,
                                                   String checkoutRequestId, String resultCode,
                                                   String resultDesc) {
        String sql = "UPDATE mpesa_transactions SET merchant_request_id = ?, checkout_request_id = ?, " +
                     "result_code = ?, result_desc = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, merchantRequestId);
            stmt.setString(2, checkoutRequestId);
            stmt.setString(3, resultCode);
            stmt.setString(4, resultDesc);
            stmt.setInt(5, transactionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Update M-Pesa transaction status
     */
    public static boolean updateMpesaTransactionStatus(int transactionId, String status) {
        String sql = "UPDATE mpesa_transactions SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, transactionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Update M-Pesa transaction result
     */
    public static boolean updateMpesaTransactionResult(int transactionId, int resultCode,
                                                         String resultDesc, String mpesaReceipt,
                                                         String transactionDate) {
        String sql = "UPDATE mpesa_transactions SET result_code = ?, result_desc = ?, " +
                     "mpesa_receipt = ?, transaction_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, resultCode);
            stmt.setString(2, resultDesc);
            if (mpesaReceipt != null) {
                stmt.setString(3, mpesaReceipt);
            } else {
                stmt.setNull(3, java.sql.Types.VARCHAR);
            }
            if (transactionDate != null) {
                stmt.setTimestamp(4, Timestamp.valueOf(transactionDate));
            } else {
                stmt.setNull(4, java.sql.Types.TIMESTAMP);
            }
            stmt.setInt(5, transactionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get M-Pesa transaction by ID
     */
    public static Map<String, Object> getMpesaTransactionById(int transactionId) {
        String sql = "SELECT * FROM mpesa_transactions WHERE id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapMpesaTransaction(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get M-Pesa transaction by merchant request ID
     */
    public static Map<String, Object> getMpesaTransactionByMerchantId(String merchantRequestId) {
        String sql = "SELECT * FROM mpesa_transactions WHERE merchant_request_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, merchantRequestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapMpesaTransaction(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get M-Pesa transaction by checkout request ID
     */
    public static Map<String, Object> getMpesaTransactionByCheckoutId(String checkoutRequestId) {
        String sql = "SELECT * FROM mpesa_transactions WHERE checkout_request_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, checkoutRequestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapMpesaTransaction(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get M-Pesa transactions with optional filtering
     */
    public static List<Map<String, Object>> getMpesaTransactions(String status, String phone,
                                                                  String startDate, String endDate,
                                                                  int limit, int offset) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder("SELECT * FROM mpesa_transactions WHERE 1=1");
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
        }
        if (phone != null && !phone.isEmpty()) {
            sql.append(" AND phone_number LIKE ?");
        }
        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" AND created_at >= ?");
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND created_at <= ?");
        }
        
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            int paramIndex = 1;
            
            if (status != null && !status.isEmpty()) {
                stmt.setString(paramIndex++, status);
            }
            if (phone != null && !phone.isEmpty()) {
                stmt.setString(paramIndex++, "%%" + phone + "%%");
            }
            if (startDate != null && !startDate.isEmpty()) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(startDate + " 00:00:00"));
            }
            if (endDate != null && !endDate.isEmpty()) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(endDate + " 23:59:59"));
            }
            
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex++, offset);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(mapMpesaTransaction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }
    
    /**
     * Get M-Pesa transactions by phone number
     */
    public static List<Map<String, Object>> getMpesaTransactionsByPhone(String phoneNumber) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        String sql = "SELECT * FROM mpesa_transactions WHERE phone_number LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%%" + phoneNumber + "%%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(mapMpesaTransaction(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }
    
    /**
     * Count M-Pesa transactions with optional filtering
     */
    public static int countMpesaTransactions(String status, String phone, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM mpesa_transactions WHERE 1=1");
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
        }
        if (phone != null && !phone.isEmpty()) {
            sql.append(" AND phone_number LIKE ?");
        }
        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" AND created_at >= ?");
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND created_at <= ?");
        }
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            int paramIndex = 1;
            
            if (status != null && !status.isEmpty()) {
                stmt.setString(paramIndex++, status);
            }
            if (phone != null && !phone.isEmpty()) {
                stmt.setString(paramIndex++, "%%" + phone + "%%");
            }
            if (startDate != null && !startDate.isEmpty()) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(startDate + " 00:00:00"));
            }
            if (endDate != null && !endDate.isEmpty()) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(endDate + " 23:59:59"));
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Get M-Pesa transaction summary
     */
    public static Map<String, Object> getMpesaTransactionSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        String sql = "SELECT " +
                     "COUNT(*) as total_transactions, " +
                     "SUM(amount) as total_amount, " +
                     "COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed_count, " +
                     "SUM(CASE WHEN status = 'completed' THEN amount ELSE 0 END) as completed_amount, " +
                     "COUNT(CASE WHEN status = 'pending' THEN 1 END) as pending_count, " +
                     "COUNT(CASE WHEN status = 'failed' THEN 1 END) as failed_count " +
                     "FROM mpesa_transactions";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                summary.put("total_transactions", rs.getInt("total_transactions"));
                summary.put("total_amount", rs.getDouble("total_amount"));
                summary.put("completed_count", rs.getInt("completed_count"));
                summary.put("completed_amount", rs.getDouble("completed_amount"));
                summary.put("pending_count", rs.getInt("pending_count"));
                summary.put("failed_count", rs.getInt("failed_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }
    
    /**
     * Map ResultSet to M-Pesa transaction map
     */
    private static Map<String, Object> mapMpesaTransaction(ResultSet rs) throws SQLException {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("id", rs.getInt("id"));
        transaction.put("transaction_id", rs.getString("transaction_id"));
        transaction.put("phone_number", rs.getString("phone_number"));
        transaction.put("amount", rs.getDouble("amount"));
        transaction.put("status", rs.getString("status"));
        transaction.put("merchant_request_id", rs.getString("merchant_request_id"));
        transaction.put("checkout_request_id", rs.getString("checkout_request_id"));
        transaction.put("result_code", rs.getString("result_code"));
        transaction.put("result_desc", rs.getString("result_desc"));
        transaction.put("mpesa_receipt", rs.getString("mpesa_receipt"));
        transaction.put("transaction_type", rs.getString("transaction_type"));
        transaction.put("transaction_date", rs.getTimestamp("transaction_date"));
        transaction.put("application_id", rs.getObject("application_id"));
        transaction.put("created_at", rs.getTimestamp("created_at"));
        transaction.put("updated_at", rs.getTimestamp("updated_at"));
        return transaction;
    }
}

