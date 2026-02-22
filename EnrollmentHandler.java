import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enrollment Handler
 * Handles student enrollment operations for courses/classes
 */
class EnrollmentHandler extends ApiHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            // Route: POST /api/enrollments - Create new enrollment (admin/staff only)
            if (path.equals("/api/enrollments") && "POST".equals(method)) {
                handleCreateEnrollment(exchange);
                return;
            }

            // Route: GET /api/enrollments - List all enrollments (admin/staff only)
            if (path.equals("/api/enrollments") && "GET".equals(method)) {
                handleGetEnrollments(exchange);
                return;
            }

            // Route: GET /api/enrollments/{id} - Get single enrollment
            if (path.startsWith("/api/enrollments/") && "GET".equals(method)) {
                String idStr = path.substring("/api/enrollments/".length());
                try {
                    int enrollmentId = Integer.parseInt(idStr);
                    handleGetEnrollmentById(exchange, enrollmentId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid enrollment ID");
                }
                return;
            }

            // Route: PUT /api/enrollments/{id} - Update enrollment (admin only)
            if (path.startsWith("/api/enrollments/") && "PUT".equals(method)) {
                String idStr = path.substring("/api/enrollments/".length());
                try {
                    int enrollmentId = Integer.parseInt(idStr);
                    handleUpdateEnrollment(exchange, enrollmentId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid enrollment ID");
                }
                return;
            }

            // Route: DELETE /api/enrollments/{id} - Delete enrollment (admin only)
            if (path.startsWith("/api/enrollments/") && "DELETE".equals(method)) {
                String idStr = path.substring("/api/enrollments/".length());
                try {
                    int enrollmentId = Integer.parseInt(idStr);
                    handleDeleteEnrollment(exchange, enrollmentId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid enrollment ID");
                }
                return;
            }

            // Route: GET /api/students/{studentId}/enrollments - Get student's enrollments
            if (path.startsWith("/api/students/") && path.endsWith("/enrollments") && "GET".equals(method)) {
                String studentIdStr = path.substring("/api/students/".length(), path.length() - "/enrollments".length());
                try {
                    int studentId = Integer.parseInt(studentIdStr);
                    handleGetStudentEnrollments(exchange, studentId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid student ID");
                }
                return;
            }

            // 404 for other routes
            sendErrorResponse(exchange, 404, "Endpoint not found");

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    /**
     * Create a new enrollment (admin/staff only)
     */
    private void handleCreateEnrollment(HttpExchange exchange) throws IOException {
        // Check authentication
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }

        Map<String, Object> payload = JWTUtil.validateToken(token);
        if (payload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role) && !"staff".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin or staff access required");
            return;
        }

        // Parse request body
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);

        String studentIdStr = params.get("student_id");
        String courseIdStr = params.get("course_id");
        String enrollmentDate = params.get("enrollment_date");
        String status = params.get("status");
        String startTime = params.get("start_time");
        String feeAmountStr = params.get("fee_amount");
        String feePaidStr = params.get("fee_paid");
        String feeBalanceStr = params.get("fee_balance");
        String paymentStatus = params.get("payment_status");
        String notes = params.get("notes");

        // Validate required fields
        if (studentIdStr == null || studentIdStr.isEmpty()) {
            sendErrorResponse(exchange, 400, "Student ID is required");
            return;
        }
        if (courseIdStr == null || courseIdStr.isEmpty()) {
            sendErrorResponse(exchange, 400, "Course ID is required");
            return;
        }

        int studentId;
        int courseId;
        double feeAmount = 0;
        double feePaid = 0;
        double feeBalance = 0;
        try {
            studentId = Integer.parseInt(studentIdStr);
            courseId = Integer.parseInt(courseIdStr);
            if (feeAmountStr != null && !feeAmountStr.isEmpty()) {
                feeAmount = Double.parseDouble(feeAmountStr);
            }
            if (feePaidStr != null && !feePaidStr.isEmpty()) {
                feePaid = Double.parseDouble(feePaidStr);
            }
            if (feeBalanceStr != null && !feeBalanceStr.isEmpty()) {
                feeBalance = Double.parseDouble(feeBalanceStr);
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid student or course ID");
            return;
        }

        // Validate student exists
        if (DBConnection.getUserById(studentId) == null) {
            sendErrorResponse(exchange, 404, "Student not found");
            return;
        }

        // Validate course exists
        if (DBConnection.getCourseById(courseId) == null) {
            sendErrorResponse(exchange, 404, "Course not found");
            return;
        }

        // Check if already enrolled
        if (isAlreadyEnrolled(studentId, courseId)) {
            sendErrorResponse(exchange, 409, "Student is already enrolled in this course");
            return;
        }

        // Create enrollment
        int enrollmentId = createEnrollment(studentId, courseId, enrollmentDate, status, startTime, feeAmount, feePaid, feeBalance, paymentStatus, notes);
        if (enrollmentId > 0) {
            String json = "{\"success\": true, \"message\": \"Enrollment created successfully\", \"id\": " + enrollmentId + "}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to create enrollment");
        }
    }

    /**
     * Get all enrollments (admin/staff only) with optional filtering
     */
    private void handleGetEnrollments(HttpExchange exchange) throws IOException {
        // Check authentication
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }

        Map<String, Object> payload = JWTUtil.validateToken(token);
        if (payload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = JWTUtil.getRole(token);
        // Allow admin, staff, student, applicant, and user to view enrollments
        if (!"admin".equals(role) && !"staff".equals(role) && !"student".equals(role) && !"applicant".equals(role) && !"user".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin, staff, student, applicant or user access required");
            return;
        }
        
        // Parse query parameters for filtering
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());
        
        // For students/applicants/users, only show their own enrollments
        int userId = JWTUtil.getUserId(token);
        String studentIdFilter = queryParams.get("student_id");
        if ("student".equals(role) || "applicant".equals(role) || "user".equals(role)) {
            studentIdFilter = String.valueOf(userId);
        }
        String courseIdFilter = queryParams.get("course_id");
        String statusFilter = queryParams.get("status");
        String dateFilter = queryParams.get("date");

        List<Map<String, Object>> enrollments = getEnrollments(studentIdFilter, courseIdFilter, statusFilter, dateFilter);
        String json = "{\"success\": true, \"enrollments\": " + listMapToJson(enrollments) + ", \"count\": " + enrollments.size() + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get enrollment by ID (admin/staff/student own)
     */
    private void handleGetEnrollmentById(HttpExchange exchange, int enrollmentId) throws IOException {
        // Check authentication
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }

        Map<String, Object> payload = JWTUtil.validateToken(token);
        if (payload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = JWTUtil.getRole(token);
        int userId = JWTUtil.getUserId(token);

        // Get enrollment
        Map<String, Object> enrollment = getEnrollmentById(enrollmentId);
        if (enrollment == null) {
            sendErrorResponse(exchange, 404, "Enrollment not found");
            return;
        }

        // Check authorization - admin/staff can view any, students/applicants/users can only view their own
        if ("student".equals(role) || "applicant".equals(role) || "user".equals(role)) {
            int enrollmentStudentId = ((Number) enrollment.get("student_id")).intValue();
            if (enrollmentStudentId != userId) {
                sendErrorResponse(exchange, 403, "You can only view your own enrollments");
                return;
            }
        }

        String json = "{\"success\": true, \"enrollment\": " + mapToJson(enrollment) + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Update enrollment (admin only)
     */
    private void handleUpdateEnrollment(HttpExchange exchange, int enrollmentId) throws IOException {
        // Check authentication
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }

        Map<String, Object> payload = JWTUtil.validateToken(token);
        if (payload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }

        // Check if enrollment exists
        if (!enrollmentExists(enrollmentId)) {
            sendErrorResponse(exchange, 404, "Enrollment not found");
            return;
        }

        // Parse request body
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);

        String status = params.get("status");
        String startTime = params.get("start_time");
        String feeAmountStr = params.get("fee_amount");
        String feePaidStr = params.get("fee_paid");
        String feeBalanceStr = params.get("fee_balance");
        String paymentStatus = params.get("payment_status");
        String notes = params.get("notes");
        String completionDate = params.get("completion_date");
        String certificateNumber = params.get("certificate_number");

        double feeAmount = 0;
        double feePaid = 0;
        double feeBalance = 0;
        try {
            if (feeAmountStr != null && !feeAmountStr.isEmpty()) {
                feeAmount = Double.parseDouble(feeAmountStr);
            }
            if (feePaidStr != null && !feePaidStr.isEmpty()) {
                feePaid = Double.parseDouble(feePaidStr);
            }
            if (feeBalanceStr != null && !feeBalanceStr.isEmpty()) {
                feeBalance = Double.parseDouble(feeBalanceStr);
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid fee amount");
            return;
        }

        // Validate status if provided
        if (status != null && !status.isEmpty()) {
            if (!isValidStatus(status)) {
                sendErrorResponse(exchange, 400, "Invalid status. Must be: enrolled, active, completed, dropped, suspended");
                return;
            }
        }

        // Update enrollment
        boolean updated = updateEnrollment(enrollmentId, status, startTime, feeAmount, feePaid, feeBalance, paymentStatus, notes, completionDate, certificateNumber);
        if (updated) {
            String json = "{\"success\": true, \"message\": \"Enrollment updated successfully\"}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to update enrollment");
        }
    }

    /**
     * Delete enrollment (admin only)
     */
    private void handleDeleteEnrollment(HttpExchange exchange, int enrollmentId) throws IOException {
        // Check authentication
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }

        Map<String, Object> payload = JWTUtil.validateToken(token);
        if (payload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }

        // Check if enrollment exists
        if (!enrollmentExists(enrollmentId)) {
            sendErrorResponse(exchange, 404, "Enrollment not found");
            return;
        }

        // Delete enrollment
        boolean deleted = deleteEnrollment(enrollmentId);
        if (deleted) {
            String json = "{\"success\": true, \"message\": \"Enrollment deleted successfully\"}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete enrollment");
        }
    }

    /**
     * Get enrollments for a specific student
     */
    private void handleGetStudentEnrollments(HttpExchange exchange, int studentId) throws IOException {
        // Check authentication
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }

        Map<String, Object> payload = JWTUtil.validateToken(token);
        if (payload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = JWTUtil.getRole(token);
        int userId = JWTUtil.getUserId(token);

        // Students can only view their own enrollments
        if ("student".equals(role) && userId != studentId) {
            sendErrorResponse(exchange, 403, "You can only view your own enrollments");
            return;
        }

        // Validate student exists
        if (DBConnection.getUserById(studentId) == null) {
            sendErrorResponse(exchange, 404, "Student not found");
            return;
        }

        List<Map<String, Object>> enrollments = getEnrollmentsByStudentId(studentId);
        String json = "{\"success\": true, \"enrollments\": " + listMapToJson(enrollments) + ", \"count\": " + enrollments.size() + "}";
        sendJsonResponse(exchange, 200, json);
    }

    // ==================== Database Methods ====================

    /**
     * Check if student is already enrolled in a course
     */
    private boolean isAlreadyEnrolled(int studentId, int courseId) {
        String sql = "SELECT id FROM enrollments WHERE student_id = ? AND class_id = ? AND status NOT IN ('completed', 'cancelled')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create a new enrollment
     */
    private int createEnrollment(int studentId, int courseId, String enrollmentDate, String status, String startTime, 
            double feeAmount, double feePaid, double feeBalance, String paymentStatus, String notes) {
        // Generate enrollment number
        String enrollmentNumber = "ENR-" + System.currentTimeMillis();
        // Use provided values or defaults - cast date/timestamp fields in SQL
        String sql = "INSERT INTO enrollments (student_id, course_id, enrollment_number, enrollment_date, status, start_time, fee_amount, fee_paid, fee_balance, payment_status, notes) " +
                     "VALUES (?, ?, ?, CAST(? AS DATE), ?, CAST(? AS TIMESTAMP), ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            stmt.setString(3, enrollmentNumber);
            // Handle enrollment_date - use current date if not provided
            if (enrollmentDate != null && !enrollmentDate.isEmpty()) {
                stmt.setString(4, enrollmentDate);
            } else {
                stmt.setString(4, null); // Let DEFAULT handle it
            }
            stmt.setString(5, status != null && !status.isEmpty() ? status : "enrolled");
            stmt.setString(6, startTime != null && !startTime.isEmpty() ? startTime : null);
            stmt.setDouble(7, feeAmount);
            stmt.setDouble(8, feePaid);
            stmt.setDouble(9, feeBalance);
            stmt.setString(10, paymentStatus != null && !paymentStatus.isEmpty() ? paymentStatus : "unpaid");
            stmt.setString(11, notes);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get enrollments with optional filtering
     */
    private List<Map<String, Object>> getEnrollments(String studentIdFilter, String courseIdFilter, String statusFilter, String dateFilter) {
        List<Map<String, Object>> enrollments = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.*, u.first_name as student_first_name, u.last_name as student_last_name, u.email as student_email, ");
        sql.append("c.name as course_name, c.code as course_code, c.duration_hours, c.price ");
        sql.append("FROM enrollments e ");
        sql.append("LEFT JOIN users u ON e.student_id = u.id ");
        sql.append("LEFT JOIN courses c ON e.course_id = c.id ");
        sql.append("WHERE 1=1 ");
        
        if (studentIdFilter != null && !studentIdFilter.isEmpty()) {
            sql.append("AND e.student_id = ").append(studentIdFilter).append(" ");
        }
        if (courseIdFilter != null && !courseIdFilter.isEmpty()) {
            sql.append("AND e.course_id = ").append(courseIdFilter).append(" ");
        }
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND e.status = '").append(statusFilter).append("' ");
        }
        if (dateFilter != null && !dateFilter.isEmpty()) {
            sql.append("AND DATE(e.enrollment_date) = '").append(dateFilter).append("' ");
        }
        sql.append("ORDER BY e.created_at DESC");
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                enrollments.add(mapEnrollment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return enrollments;
    }

    /**
     * Get enrollment by ID
     */
    private Map<String, Object> getEnrollmentById(int enrollmentId) {
        String sql = "SELECT e.*, u.first_name as student_first_name, u.last_name as student_last_name, u.email as student_email, " +
                     "c.name as course_name, c.code as course_code, c.duration_hours, c.price " +
                     "FROM enrollments e " +
                     "LEFT JOIN users u ON e.student_id = u.id " +
                     "LEFT JOIN courses c ON e.course_id = c.id " +
                     "WHERE e.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapEnrollment(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get enrollments by student ID
     */
    private List<Map<String, Object>> getEnrollmentsByStudentId(int studentId) {
        List<Map<String, Object>> enrollments = new ArrayList<>();
        String sql = "SELECT e.*, c.course_name, c.course_code, c.duration, c.price " +
                     "FROM enrollments e " +
                     "LEFT JOIN classes cl ON e.class_id = cl.id " +
                     "LEFT JOIN courses c ON cl.course_id = c.id " +
                     "WHERE e.student_id = ? " +
                     "ORDER BY e.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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

    /**
     * Check if enrollment exists
     */
    private boolean enrollmentExists(int enrollmentId) {
        String sql = "SELECT id FROM enrollments WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if status is valid
     */
    private boolean isValidStatus(String status) {
        return "enrolled".equals(status) || "active".equals(status) || 
               "completed".equals(status) || "dropped".equals(status) || "suspended".equals(status);
    }

    /**
     * Update enrollment
     */
    private boolean updateEnrollment(int enrollmentId, String status, String startTime, double feeAmount, 
            double feePaid, double feeBalance, String paymentStatus, String notes, String completionDate, String certificateNumber) {
        StringBuilder sql = new StringBuilder("UPDATE enrollments SET updated_at = CURRENT_TIMESTAMP");
        
        if (status != null && !status.isEmpty()) {
            sql.append(", status = '").append(status).append("'");
        }
        if (startTime != null && !startTime.isEmpty()) {
            sql.append(", start_time = '").append(startTime).append("'");
        }
        if (feeAmount > 0) {
            sql.append(", fee_amount = ").append(feeAmount);
        }
        if (feePaid > 0) {
            sql.append(", fee_paid = ").append(feePaid);
        }
        if (feeBalance > 0 || (feeAmount > 0 && feeBalance == 0)) {
            sql.append(", fee_balance = ").append(feeBalance);
        }
        if (paymentStatus != null && !paymentStatus.isEmpty()) {
            sql.append(", payment_status = '").append(paymentStatus).append("'");
        }
        if (notes != null) {
            sql.append(", notes = '").append(notes.replace("'", "''")).append("'");
        }
        if (completionDate != null && !completionDate.isEmpty()) {
            sql.append(", completion_date = '").append(completionDate).append("'");
        }
        if (certificateNumber != null && !certificateNumber.isEmpty()) {
            sql.append(", certificate_number = '").append(certificateNumber).append("'");
        }
        
        sql.append(" WHERE id = ?");
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, enrollmentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete enrollment
     */
    private boolean deleteEnrollment(int enrollmentId) {
        String sql = "DELETE FROM enrollments WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map ResultSet to enrollment map
     */
    private Map<String, Object> mapEnrollment(ResultSet rs) throws SQLException {
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("id", rs.getInt("id"));
        enrollment.put("student_id", rs.getInt("student_id"));
        enrollment.put("course_id", rs.getInt("course_id"));
        enrollment.put("enrollment_number", rs.getString("enrollment_number"));
        enrollment.put("enrollment_date", rs.getDate("enrollment_date"));
        enrollment.put("status", rs.getString("status"));
        enrollment.put("start_time", rs.getTimestamp("start_time"));
        enrollment.put("completion_date", rs.getDate("completion_date"));
        enrollment.put("certificate_number", rs.getString("certificate_number"));
        enrollment.put("fee_amount", rs.getDouble("fee_amount"));
        enrollment.put("fee_paid", rs.getDouble("fee_paid"));
        enrollment.put("fee_balance", rs.getDouble("fee_balance"));
        enrollment.put("payment_status", rs.getString("payment_status"));
        enrollment.put("notes", rs.getString("notes"));
        enrollment.put("created_at", rs.getTimestamp("created_at"));
        enrollment.put("updated_at", rs.getTimestamp("updated_at"));
        
        // Include student info if available
        try {
            enrollment.put("student_first_name", rs.getString("student_first_name"));
            enrollment.put("student_last_name", rs.getString("student_last_name"));
            enrollment.put("student_email", rs.getString("student_email"));
        } catch (SQLException e) {
            // Student info not available in this query
        }
        
        // Include course info if available
        try {
            enrollment.put("course_name", rs.getString("course_name"));
            enrollment.put("course_code", rs.getString("course_code"));
            enrollment.put("duration", rs.getString("duration"));
            enrollment.put("price", rs.getDouble("price"));
        } catch (SQLException e) {
            // Course info not available in this query
        }
        
        return enrollment;
    }
}
