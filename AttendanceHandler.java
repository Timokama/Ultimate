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
 * Attendance Handler
 * Handles student attendance operations for classes/sessions
 */
class AttendanceHandler extends ApiHandler implements HttpHandler {

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
            // Route: POST /api/attendance - Create attendance record (admin/staff only)
            if (path.equals("/api/attendance") && "POST".equals(method)) {
                handleCreateAttendance(exchange);
                return;
            }

            // Route: GET /api/attendance - List all attendance records (admin/staff only)
            if (path.equals("/api/attendance") && "GET".equals(method)) {
                handleGetAllAttendance(exchange);
                return;
            }

            // Route: GET /api/attendance/{id} - Get single attendance record
            if (path.startsWith("/api/attendance/") && "GET".equals(method)) {
                String idStr = path.substring("/api/attendance/".length());
                try {
                    int attendanceId = Integer.parseInt(idStr);
                    handleGetAttendanceById(exchange, attendanceId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid attendance ID");
                }
                return;
            }

            // Route: PUT /api/attendance/{id} - Update attendance record (admin/staff only)
            if (path.startsWith("/api/attendance/") && "PUT".equals(method)) {
                String idStr = path.substring("/api/attendance/".length());
                try {
                    int attendanceId = Integer.parseInt(idStr);
                    handleUpdateAttendance(exchange, attendanceId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid attendance ID");
                }
                return;
            }

            // Route: DELETE /api/attendance/{id} - Delete attendance record (admin only)
            if (path.startsWith("/api/attendance/") && "DELETE".equals(method)) {
                String idStr = path.substring("/api/attendance/".length());
                try {
                    int attendanceId = Integer.parseInt(idStr);
                    handleDeleteAttendance(exchange, attendanceId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid attendance ID");
                }
                return;
            }

            // Route: GET /api/attendance/class/{classId} - Get attendance for a class
            if (path.startsWith("/api/attendance/class/") && "GET".equals(method)) {
                String classIdStr = path.substring("/api/attendance/class/".length());
                try {
                    int classId = Integer.parseInt(classIdStr);
                    handleGetClassAttendance(exchange, classId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid class ID");
                }
                return;
            }

            // Route: GET /api/attendance/student/{studentId} - Get student's attendance history
            if (path.startsWith("/api/attendance/student/") && "GET".equals(method)) {
                String studentIdStr = path.substring("/api/attendance/student/".length());
                try {
                    int studentId = Integer.parseInt(studentIdStr);
                    handleGetStudentAttendance(exchange, studentId);
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
     * Create attendance record (admin/staff only)
     */
    private void handleCreateAttendance(HttpExchange exchange) throws IOException {
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

        int recordedBy = JWTUtil.getUserId(token);

        // Parse request body
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);

        String studentIdStr = params.get("student_id");
        String courseIdStr = params.get("course_id");
        String classIdStr = params.get("class_id");
        String dateStr = params.get("date");
        String timeInStr = params.get("time_in");
        String timeOutStr = params.get("time_out");
        String status = params.get("status");
        String notes = params.get("notes");
        String location = params.get("location");
        
        // Course details fields
        String licenseType = params.get("license_type");
        String drivingCourse = params.get("driving_course");
        String computerCourse = params.get("computer_course");
        String transmission = params.get("transmission");
        String preferredSchedule = params.get("preferred_schedule");

        // Validate required fields
        if (studentIdStr == null || studentIdStr.isEmpty()) {
            sendErrorResponse(exchange, 400, "Student ID is required");
            return;
        }
        // Accept either course_id, class_id, or any course detail fields
        boolean hasCourseId = courseIdStr != null && !courseIdStr.isEmpty();
        boolean hasClassId = classIdStr != null && !classIdStr.isEmpty();
        boolean hasCourseDetails = (licenseType != null && !licenseType.isEmpty()) ||
                                    (drivingCourse != null && !drivingCourse.isEmpty()) ||
                                    (computerCourse != null && !computerCourse.isEmpty()) ||
                                    (transmission != null && !transmission.isEmpty());
        
        if (!hasCourseId && !hasClassId && !hasCourseDetails) {
            sendErrorResponse(exchange, 400, "Course ID, Class ID, or Course details (License Type, Driving Course, Computer Course, Transmission) is required");
            return;
        }
        
        // Use course_id if provided, otherwise use class_id
        int courseId = 0;
        int classId = 0;
        if (courseIdStr != null && !courseIdStr.isEmpty()) {
            try {
                courseId = Integer.parseInt(courseIdStr);
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid course ID");
                return;
            }
        }
        if (classIdStr != null && !classIdStr.isEmpty()) {
            try {
                classId = Integer.parseInt(classIdStr);
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid class ID");
                return;
            }
        }
        if (dateStr == null || dateStr.isEmpty()) {
            sendErrorResponse(exchange, 400, "Date is required");
            return;
        }
        if (status == null || status.isEmpty()) {
            sendErrorResponse(exchange, 400, "Status is required");
            return;
        }

        // Validate status
        if (!isValidStatus(status)) {
            sendErrorResponse(exchange, 400, "Invalid status. Must be: present, absent, late");
            return;
        }

        int studentId;
        try {
            studentId = Integer.parseInt(studentIdStr);
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid student or class ID");
            return;
        }

        // Validate student exists
        if (DBConnection.getUserById(studentId) == null) {
            sendErrorResponse(exchange, 404, "Student not found");
            return;
        }

        // Validate class exists (only if classId is provided)
        if (classId > 0 && getClassById(classId) == null) {
            sendErrorResponse(exchange, 404, "Class not found");
            return;
        }

        // Check if attendance already exists for this student/class/date
        int checkClassId = classId > 0 ? classId : -courseId;
        if (attendanceExists(studentId, checkClassId, dateStr)) {
            sendErrorResponse(exchange, 409, "Attendance record already exists for this student on this date");
            return;
        }

        // Create attendance - store course_id in class_id field for course-based attendance
        int attendanceId;
        if (courseId > 0 && classId <= 0) {
            // For course-based attendance only (no class), store negative course_id to distinguish from class_id
            attendanceId = createAttendance(studentId, -courseId, dateStr, timeInStr, timeOutStr, status, notes, recordedBy, location, licenseType, drivingCourse, computerCourse, transmission, preferredSchedule);
        } else {
            // Use class_id for class-based attendance
            attendanceId = createAttendance(studentId, classId, dateStr, timeInStr, timeOutStr, status, notes, recordedBy, location, licenseType, drivingCourse, computerCourse, transmission, preferredSchedule);
        }
        if (attendanceId > 0) {
            String json = "{\"success\": true, \"message\": \"Attendance record created successfully\", \"id\": " + attendanceId + "}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to create attendance record");
        }
    }

    /**
     * Get all attendance records (admin/staff only) with filtering
     */
    private void handleGetAllAttendance(HttpExchange exchange) throws IOException {
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
        
        // For students/applicants/users, only show their own attendance
        if ("student".equals(role) || "applicant".equals(role) || "user".equals(role)) {
            // Parse query parameters but override student_id to be current user
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());
            String dateFilter = queryParams.get("date");
            String startDate = queryParams.get("start_date");
            String endDate = queryParams.get("end_date");
            
            List<Map<String, Object>> attendanceList = getAttendanceRecords(String.valueOf(userId), null, null, dateFilter, null, startDate, endDate);
            
            int total = attendanceList.size();
            int present = 0;
            int absent = 0;
            int late = 0;
            
            for (Map<String, Object> record : attendanceList) {
                String recStatus = (String) record.get("status");
                if ("present".equals(recStatus)) present++;
                else if ("absent".equals(recStatus)) absent++;
                else if ("late".equals(recStatus)) late++;
            }
            
            String json = "{\"success\": true, \"attendance\": " + listMapToJson(attendanceList) + 
                         ", \"count\": " + total + 
                         ", \"summary\": {\"total\": " + total + ", \"present\": " + present + 
                         ", \"absent\": " + absent + ", \"late\": " + late + "}}";
            sendJsonResponse(exchange, 200, json);
            return;
        }
        
        // Admin/staff can view all attendance
        if (!"admin".equals(role) && !"staff".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin or staff access required");
            return;
        }

        // Parse query parameters for filtering
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());
        String studentIdFilter = queryParams.get("student_id");
        String classIdFilter = queryParams.get("class_id");
        String courseIdFilter = queryParams.get("course_id");
        String dateFilter = queryParams.get("date");
        String statusFilter = queryParams.get("status");
        String startDate = queryParams.get("start_date");
        String endDate = queryParams.get("end_date");

        List<Map<String, Object>> attendanceList = getAttendanceRecords(studentIdFilter, classIdFilter, courseIdFilter, dateFilter, statusFilter, startDate, endDate);
        
        // Calculate summary
        int total = attendanceList.size();
        int present = 0;
        int absent = 0;
        int late = 0;
        
        for (Map<String, Object> record : attendanceList) {
            String recStatus = (String) record.get("status");
            if ("present".equals(recStatus)) present++;
            else if ("absent".equals(recStatus)) absent++;
            else if ("late".equals(recStatus)) late++;
        }
        
        String json = "{\"success\": true, \"attendance\": " + listMapToJson(attendanceList) + 
                     ", \"count\": " + total + 
                     ", \"summary\": {\"total\": " + total + ", \"present\": " + present + 
                     ", \"absent\": " + absent + ", \"late\": " + late + "}}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get attendance record by ID
     */
    private void handleGetAttendanceById(HttpExchange exchange, int attendanceId) throws IOException {
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

        // Get attendance record
        Map<String, Object> attendance = getAttendanceById(attendanceId);
        if (attendance == null) {
            sendErrorResponse(exchange, 404, "Attendance record not found");
            return;
        }

        // Check authorization - admin/staff can view any, students can only view their own
        if ("student".equals(role)) {
            int attendanceStudentId = ((Number) attendance.get("student_id")).intValue();
            if (attendanceStudentId != userId) {
                sendErrorResponse(exchange, 403, "You can only view your own attendance records");
                return;
            }
        }

        String json = "{\"success\": true, \"attendance\": " + mapToJson(attendance) + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Update attendance record (admin/staff only)
     */
    private void handleUpdateAttendance(HttpExchange exchange, int attendanceId) throws IOException {
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

        // Check if attendance exists
        if (!attendanceExistsById(attendanceId)) {
            sendErrorResponse(exchange, 404, "Attendance record not found");
            return;
        }

        // Parse request body
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);

        String status = params.get("status");
        String timeInStr = params.get("time_in");
        String timeOutStr = params.get("time_out");
        String notes = params.get("notes");
        String location = params.get("location");
        String licenseType = params.get("license_type");
        String drivingCourse = params.get("driving_course");
        String computerCourse = params.get("computer_course");
        String transmission = params.get("transmission");
        String preferredSchedule = params.get("preferred_schedule");

        // Validate status if provided
        if (status != null && !status.isEmpty()) {
            if (!isValidStatus(status)) {
                sendErrorResponse(exchange, 400, "Invalid status. Must be: present, absent, late");
                return;
            }
        }

        // Update attendance
        boolean updated = updateAttendance(attendanceId, status, timeInStr, timeOutStr, notes, location, licenseType, drivingCourse, computerCourse, transmission, preferredSchedule);
        if (updated) {
            String json = "{\"success\": true, \"message\": \"Attendance record updated successfully\"}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to update attendance record");
        }
    }

    /**
     * Delete attendance record (admin only)
     */
    private void handleDeleteAttendance(HttpExchange exchange, int attendanceId) throws IOException {
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

        // Check if attendance exists
        if (!attendanceExistsById(attendanceId)) {
            sendErrorResponse(exchange, 404, "Attendance record not found");
            return;
        }

        // Delete attendance
        boolean deleted = deleteAttendance(attendanceId);
        if (deleted) {
            String json = "{\"success\": true, \"message\": \"Attendance record deleted successfully\"}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete attendance record");
        }
    }

    /**
     * Get attendance for a class
     */
    private void handleGetClassAttendance(HttpExchange exchange, int classId) throws IOException {
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

        // Validate class exists
        if (getClassById(classId) == null) {
            sendErrorResponse(exchange, 404, "Class not found");
            return;
        }

        // Parse query parameters for date filtering
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());
        String dateFilter = queryParams.get("date");
        String startDate = queryParams.get("start_date");
        String endDate = queryParams.get("end_date");

        // Get class details
        Map<String, Object> classDetails = getClassById(classId);
        
        // Get all students enrolled in this class
        List<Map<String, Object>> enrolledStudents = getEnrolledStudentsByClassId(classId);
        
        // Get attendance records for the class
        List<Map<String, Object>> attendanceRecords = getAttendanceByClassId(classId, startDate, endDate);
        
        // Build attendance map by student_id
        Map<Integer, Map<String, Object>> attendanceMap = new HashMap<>();
        for (Map<String, Object> record : attendanceRecords) {
            int studentId = ((Number) record.get("student_id")).intValue();
            attendanceMap.put(studentId, record);
        }
        
        // Build student attendance list
        List<Map<String, Object>> studentAttendanceList = new ArrayList<>();
        for (Map<String, Object> student : enrolledStudents) {
            int studentId = ((Number) student.get("id")).intValue();
            Map<String, Object> studentRecord = new HashMap<>();
            studentRecord.put("student_id", studentId);
            studentRecord.put("student_name", student.get("full_name"));
            studentRecord.put("email", student.get("email"));
            
            if (attendanceMap.containsKey(studentId)) {
                studentRecord.put("attendance", attendanceMap.get(studentId));
                studentRecord.put("status", attendanceMap.get(studentId).get("status"));
            } else {
                studentRecord.put("attendance", null);
                studentRecord.put("status", "not_recorded");
            }
            
            studentAttendanceList.add(studentRecord);
        }
        
        // Calculate summary
        int totalStudents = enrolledStudents.size();
        int present = 0;
        int absent = 0;
        int late = 0;
        int notRecorded = 0;
        
        for (Map<String, Object> record : studentAttendanceList) {
            String status = (String) record.get("status");
            if ("present".equals(status)) present++;
            else if ("absent".equals(status)) absent++;
            else if ("late".equals(status)) late++;
            else notRecorded++;
        }
        
        String json = "{\"success\": true, \"class\": " + mapToJson(classDetails) + 
                     ", \"attendance\": " + listMapToJson(studentAttendanceList) + 
                     ", \"summary\": {\"total_students\": " + totalStudents + 
                     ", \"present\": " + present + 
                     ", \"absent\": " + absent + 
                     ", \"late\": " + late + 
                     ", \"not_recorded\": " + notRecorded + "}}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get student's attendance history
     */
    private void handleGetStudentAttendance(HttpExchange exchange, int studentId) throws IOException {
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

        // Students can only view their own attendance
        if ("student".equals(role) && userId != studentId) {
            sendErrorResponse(exchange, 403, "You can only view your own attendance records");
            return;
        }

        // Validate student exists
        if (DBConnection.getUserById(studentId) == null) {
            sendErrorResponse(exchange, 404, "Student not found");
            return;
        }

        // Parse query parameters
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());
        String startDate = queryParams.get("start_date");
        String endDate = queryParams.get("end_date");
        String classIdFilter = queryParams.get("class_id");

        // Get student details
        Map<String, Object> student = DBConnection.getUserById(studentId);
        
        // Get attendance records
        List<Map<String, Object>> attendanceRecords = getAttendanceByStudentId(studentId, classIdFilter, startDate, endDate);
        
        // Calculate attendance percentage
        int total = attendanceRecords.size();
        int present = 0;
        int absent = 0;
        int late = 0;
        
        for (Map<String, Object> record : attendanceRecords) {
            String status = (String) record.get("status");
            if ("present".equals(status)) present++;
            else if ("absent".equals(status)) absent++;
            else if ("late".equals(status)) late++;
        }
        
        double attendancePercentage = total > 0 ? ((double) present / total) * 100 : 0;
        double latePercentage = total > 0 ? ((double) late / total) * 100 : 0;
        
        String json = "{\"success\": true, \"student\": {\"id\": " + studentId + 
                     ", \"name\": \"" + escapeJson((String) student.get("full_name")) + "\" " +
                     ", \"email\": \"" + escapeJson((String) student.get("email")) + "\"}, " +
                     "\"attendance\": " + listMapToJson(attendanceRecords) + 
                     ", \"count\": " + total + 
                     ", \"summary\": {\"total\": " + total + 
                     ", \"present\": " + present + 
                     ", \"absent\": " + absent + 
                     ", \"late\": " + late + 
                     ", \"attendance_percentage\": " + String.format("%.2f", attendancePercentage) + 
                     ", \"late_percentage\": " + String.format("%.2f", latePercentage) + "}}";
        sendJsonResponse(exchange, 200, json);
    }

    // ==================== Database Methods ====================

    /**
     * Check if status is valid
     */
    private boolean isValidStatus(String status) {
        return "present".equals(status) || "absent".equals(status) || "late".equals(status);
    }

    /**
     * Check if attendance exists for student/class/date
     */
    private boolean attendanceExists(int studentId, int classId, String dateStr) {
        String sql = "SELECT id FROM attendance WHERE student_id = ? AND class_id = ? AND date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, classId);
            stmt.setDate(3, java.sql.Date.valueOf(dateStr));
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if attendance exists by ID
     */
    private boolean attendanceExistsById(int attendanceId) {
        String sql = "SELECT id FROM attendance WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attendanceId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create attendance record
     */
    private int createAttendance(int studentId, int classId, String dateStr, String timeInStr, String timeOutStr, 
                                  String status, String notes, int recordedBy, String location,
                                  String licenseType, String drivingCourse, String computerCourse,
                                  String transmission, String preferredSchedule) {
        // Convert time format from HH:mm (HTML input) to HH:mm:ss for SQL Time
        String formattedTimeIn = formatTimeForSQL(timeInStr);
        String formattedTimeOut = formatTimeForSQL(timeOutStr);
        
        // Try with new columns first
        String sql = "INSERT INTO attendance (student_id, class_id, date, time_in, time_out, status, notes, location, " +
                     "license_type, driving_course, computer_course, transmission, preferred_schedule, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) RETURNING id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, classId);
            stmt.setDate(3, java.sql.Date.valueOf(dateStr));
            if (formattedTimeIn != null && !formattedTimeIn.isEmpty()) {
                stmt.setTime(4, java.sql.Time.valueOf(formattedTimeIn));
            } else {
                stmt.setTime(4, null);
            }
            if (formattedTimeOut != null && !formattedTimeOut.isEmpty()) {
                stmt.setTime(5, java.sql.Time.valueOf(formattedTimeOut));
            } else {
                stmt.setTime(5, null);
            }
            stmt.setString(6, status);
            if (notes != null) {
                stmt.setString(7, notes);
            } else {
                stmt.setString(7, null);
            }
            stmt.setString(8, location);
            stmt.setString(9, licenseType);
            stmt.setString(10, drivingCourse);
            stmt.setString(11, computerCourse);
            stmt.setString(12, transmission);
            stmt.setString(13, preferredSchedule);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            // If new columns don't exist, try with old schema
            System.err.println("New schema failed, trying legacy schema: " + e.getMessage());
            try {
                String legacySql = "INSERT INTO attendance (student_id, class_id, date, time_in, time_out, status, notes, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) RETURNING id";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(legacySql)) {
                    stmt.setInt(1, studentId);
                    stmt.setInt(2, classId);
                    stmt.setDate(3, java.sql.Date.valueOf(dateStr));
                    if (formattedTimeIn != null && !formattedTimeIn.isEmpty()) {
                        stmt.setTime(4, java.sql.Time.valueOf(formattedTimeIn));
                    } else {
                        stmt.setTime(4, null);
                    }
                    if (formattedTimeOut != null && !formattedTimeOut.isEmpty()) {
                        stmt.setTime(5, java.sql.Time.valueOf(formattedTimeOut));
                    } else {
                        stmt.setTime(5, null);
                    }
                    stmt.setString(6, status);
                    if (notes != null) {
                        stmt.setString(7, notes);
                    } else {
                        stmt.setString(7, null);
                    }
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            } catch (SQLException e2) {
                e2.printStackTrace();
            }
        }
        return 0;
    }
    
    /**
     * Format time string for SQL Time parsing.
     * Converts HH:mm (HTML input) to HH:mm:ss format.
     */
    private String formatTimeForSQL(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        // If already in HH:mm:ss format, return as-is
        if (timeStr.length() == 8) {
            return timeStr;
        }
        // Convert HH:mm to HH:mm:ss
        if (timeStr.length() == 5) {
            return timeStr + ":00";
        }
        return timeStr;
    }

    /**
     * Get attendance records with filtering
     */
    private List<Map<String, Object>> getAttendanceRecords(String studentIdFilter, String classIdFilter, 
                                                            String courseIdFilter, String dateFilter, String statusFilter,
                                                            String startDate, String endDate) {
        List<Map<String, Object>> attendanceList = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.*, u.first_name as student_first_name, u.last_name as student_last_name, ");
        sql.append("u.full_name as student_name, u.email as student_email, ");
        sql.append("c.name as class_name, c.code as class_code, co.name as course_name ");
        sql.append("FROM attendance a ");
        sql.append("LEFT JOIN users u ON a.student_id = u.id ");
        sql.append("LEFT JOIN classes c ON a.class_id = c.id AND a.class_id > 0 ");
        sql.append("LEFT JOIN courses co ON c.course_id = co.id OR a.class_id < 0 ");
        sql.append("WHERE 1=1 ");
        
        if (studentIdFilter != null && !studentIdFilter.isEmpty()) {
            sql.append("AND a.student_id = ").append(studentIdFilter).append(" ");
        }
        if (classIdFilter != null && !classIdFilter.isEmpty()) {
            sql.append("AND a.class_id = ").append(classIdFilter).append(" ");
        }
        if (courseIdFilter != null && !courseIdFilter.isEmpty()) {
            // For course-based attendance, class_id is stored as negative course_id
            sql.append("AND (c.course_id = ").append(courseIdFilter).append(" OR a.class_id = -").append(courseIdFilter).append(") ");
        }
        if (dateFilter != null && !dateFilter.isEmpty()) {
            sql.append("AND a.date = '").append(dateFilter).append("' ");
        }
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND a.status = '").append(statusFilter).append("' ");
        }
        if (startDate != null && !startDate.isEmpty()) {
            sql.append("AND a.date >= '").append(startDate).append("' ");
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append("AND a.date <= '").append(endDate).append("' ");
        }
        sql.append("ORDER BY a.date DESC");
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                attendanceList.add(mapAttendance(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendanceList;
    }

    /**
     * Get attendance by ID
     */
    private Map<String, Object> getAttendanceById(int attendanceId) {
        String sql = "SELECT a.*, u.first_name as student_first_name, u.last_name as student_last_name, " +
                     "u.full_name as student_name, u.email as student_email, " +
                     "c.name as class_name, c.code as class_code, co.name as course_name " +
                     "FROM attendance a " +
                     "LEFT JOIN users u ON a.student_id = u.id " +
                     "LEFT JOIN classes c ON a.class_id = c.id " +
                     "LEFT JOIN courses co ON c.course_id = co.id " +
                     "WHERE a.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attendanceId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapAttendance(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update attendance record
     */
    private boolean updateAttendance(int attendanceId, String status, String timeInStr, String timeOutStr, String notes, 
                                  String location, String licenseType, String drivingCourse, String computerCourse,
                                  String transmission, String preferredSchedule) {
        StringBuilder sql = new StringBuilder("UPDATE attendance SET ");
        
        // Build comma-separated list of fields to update
        boolean hasUpdates = false;
        
        if (status != null && !status.isEmpty()) {
            sql.append("status = '").append(escapeJson(status)).append("'");
            hasUpdates = true;
        }
        if (timeInStr != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("time_in = '").append(escapeJson(timeInStr)).append("'");
            hasUpdates = true;
        }
        if (timeOutStr != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("time_out = '").append(escapeJson(timeOutStr)).append("'");
            hasUpdates = true;
        }
        if (notes != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("notes = '").append(escapeJson(notes)).append("'");
            hasUpdates = true;
        }
        if (location != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("location = '").append(escapeJson(location)).append("'");
            hasUpdates = true;
        }
        if (licenseType != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("license_type = '").append(escapeJson(licenseType)).append("'");
            hasUpdates = true;
        }
        if (drivingCourse != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("driving_course = '").append(escapeJson(drivingCourse)).append("'");
            hasUpdates = true;
        }
        if (computerCourse != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("computer_course = '").append(escapeJson(computerCourse)).append("'");
            hasUpdates = true;
        }
        if (transmission != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("transmission = '").append(escapeJson(transmission)).append("'");
            hasUpdates = true;
        }
        if (preferredSchedule != null) {
            if (hasUpdates) sql.append(", ");
            sql.append("preferred_schedule = '").append(escapeJson(preferredSchedule)).append("'");
            hasUpdates = true;
        }
        
        if (!hasUpdates) {
            return false; // Nothing to update
        }
        
        sql.append(" WHERE id = ?");
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, attendanceId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // If new columns don't exist, try with legacy schema
            System.err.println("New schema update failed, trying legacy: " + e.getMessage());
            try {
                StringBuilder legacySql = new StringBuilder("UPDATE attendance SET ");
                boolean hasLegacyUpdates = false;
                
                if (status != null && !status.isEmpty()) {
                    legacySql.append("status = '").append(escapeJson(status)).append("'");
                    hasLegacyUpdates = true;
                }
                if (timeInStr != null) {
                    if (hasLegacyUpdates) legacySql.append(", ");
                    legacySql.append("time_in = '").append(escapeJson(timeInStr)).append("'");
                    hasLegacyUpdates = true;
                }
                if (timeOutStr != null) {
                    if (hasLegacyUpdates) legacySql.append(", ");
                    legacySql.append("time_out = '").append(escapeJson(timeOutStr)).append("'");
                    hasLegacyUpdates = true;
                }
                if (notes != null) {
                    if (hasLegacyUpdates) legacySql.append(", ");
                    legacySql.append("notes = '").append(escapeJson(notes)).append("'");
                    hasLegacyUpdates = true;
                }
                
                if (!hasLegacyUpdates) {
                    return false;
                }
                
                legacySql.append(" WHERE id = ?");
                
                try (Connection conn2 = DBConnection.getConnection();
                     PreparedStatement stmt2 = conn2.prepareStatement(legacySql.toString())) {
                    stmt2.setInt(1, attendanceId);
                    return stmt2.executeUpdate() > 0;
                }
            } catch (SQLException e2) {
                e2.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Delete attendance record
     */
    private boolean deleteAttendance(int attendanceId) {
        String sql = "DELETE FROM attendance WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attendanceId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get class by ID
     */
    private Map<String, Object> getClassById(int classId) {
        String sql = "SELECT c.*, co.name as course_name FROM classes c " +
                     "LEFT JOIN courses co ON c.course_id = co.id WHERE c.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, classId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("name", rs.getString("name"));
                map.put("code", rs.getString("code"));
                map.put("course_id", rs.getInt("course_id"));
                map.put("course_name", rs.getString("course_name"));
                map.put("start_date", rs.getDate("start_date"));
                map.put("end_date", rs.getDate("end_date"));
                map.put("start_time", rs.getTime("start_time"));
                map.put("end_time", rs.getTime("end_time"));
                map.put("days_of_week", rs.getString("days_of_week"));
                map.put("location_id", rs.getInt("location_id"));
                map.put("instructor_id", rs.getInt("instructor_id"));
                map.put("max_students", rs.getInt("max_students"));
                map.put("current_students", rs.getInt("current_students"));
                map.put("status", rs.getString("status"));
                return map;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get enrolled students by class ID
     */
    private List<Map<String, Object>> getEnrolledStudentsByClassId(int classId) {
        List<Map<String, Object>> students = new ArrayList<>();
        String sql = "SELECT u.id, u.full_name, u.email FROM users u " +
                     "JOIN enrollments e ON u.id = e.student_id " +
                     "WHERE e.class_id = ? AND e.status IN ('enrolled', 'active') " +
                     "ORDER BY u.full_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, classId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> student = new HashMap<>();
                student.put("id", rs.getInt("id"));
                student.put("full_name", rs.getString("full_name"));
                student.put("email", rs.getString("email"));
                students.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    /**
     * Get attendance by class ID
     */
    private List<Map<String, Object>> getAttendanceByClassId(int classId, String startDate, String endDate) {
        List<Map<String, Object>> attendanceList = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.*, u.full_name as student_name, u.email as student_email ");
        sql.append("FROM attendance a ");
        sql.append("LEFT JOIN users u ON a.student_id = u.id ");
        sql.append("WHERE a.class_id = ? ");
        
        if (startDate != null && !startDate.isEmpty()) {
            sql.append("AND a.date >= '").append(startDate).append("' ");
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append("AND a.date <= '").append(endDate).append("' ");
        }
        sql.append("ORDER BY a.date DESC, u.full_name");
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, classId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                attendanceList.add(mapAttendance(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendanceList;
    }

    /**
     * Get attendance by student ID
     */
    private List<Map<String, Object>> getAttendanceByStudentId(int studentId, String classIdFilter, 
                                                                 String startDate, String endDate) {
        List<Map<String, Object>> attendanceList = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.*, c.name as class_name, c.code as class_code, co.name as course_name ");
        sql.append("FROM attendance a ");
        sql.append("LEFT JOIN classes c ON a.class_id = c.id ");
        sql.append("LEFT JOIN courses co ON c.course_id = co.id ");
        sql.append("WHERE a.student_id = ? ");
        
        if (classIdFilter != null && !classIdFilter.isEmpty()) {
            sql.append("AND a.class_id = ").append(classIdFilter).append(" ");
        }
        if (startDate != null && !startDate.isEmpty()) {
            sql.append("AND a.date >= '").append(startDate).append("' ");
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append("AND a.date <= '").append(endDate).append("' ");
        }
        sql.append("ORDER BY a.date DESC");
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                attendanceList.add(mapAttendance(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendanceList;
    }

    /**
     * Map ResultSet to attendance map
     */
    private Map<String, Object> mapAttendance(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rs.getInt("id"));
        map.put("student_id", rs.getInt("student_id"));
        map.put("class_id", rs.getInt("class_id"));
        map.put("date", rs.getDate("date"));
        map.put("time_in", rs.getTime("time_in"));
        map.put("time_out", rs.getTime("time_out"));
        map.put("status", rs.getString("status"));
        map.put("notes", rs.getString("notes"));
        
        // Handle optional timestamp fields
        try {
            map.put("created_at", rs.getTimestamp("created_at"));
        } catch (SQLException e) {
            map.put("created_at", null);
        }
        try {
            map.put("updated_at", rs.getTimestamp("updated_at"));
        } catch (SQLException e) {
            map.put("updated_at", null);
        }
        
        // Include related info if available
        try {
            map.put("student_name", rs.getString("student_name"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("student_first_name", rs.getString("student_first_name"));
            map.put("student_last_name", rs.getString("student_last_name"));
        } catch (SQLException e) {
            // Fields not in result set
        }
        try {
            map.put("student_email", rs.getString("student_email"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("class_name", rs.getString("class_name"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("class_code", rs.getString("class_code"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("course_name", rs.getString("course_name"));
        } catch (SQLException e) {
            // Field not in result set
        }
        
        // Add new course details fields
        try {
            map.put("location", rs.getString("location"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("license_type", rs.getString("license_type"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("driving_course", rs.getString("driving_course"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("computer_course", rs.getString("computer_course"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("transmission", rs.getString("transmission"));
        } catch (SQLException e) {
            // Field not in result set
        }
        try {
            map.put("preferred_schedule", rs.getString("preferred_schedule"));
        } catch (SQLException e) {
            // Field not in result set
        }
        
        return map;
    }
}
