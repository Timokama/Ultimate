import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Staff Dashboard Handler
 * Handles staff-specific operations like managing students, fees, and locations
 */
class StaffHandler extends ApiHandler implements HttpHandler {

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
            if (!"staff".equals(role) && !"admin".equals(role)) {
                sendErrorResponse(exchange, 403, "Staff access required");
                return;
            }

            // Route: GET /api/staff/locations - Get all training locations
            if (path.equals("/api/staff/locations") && "GET".equals(method)) {
                handleGetLocations(exchange);
                return;
            }
            
            // Route: POST /api/staff/locations - Create a new location
            if (path.equals("/api/staff/locations") && "POST".equals(method)) {
                handleCreateLocation(exchange);
                return;
            }
            
            // Route: PUT /api/staff/locations/{id} - Update a location
            if (path.startsWith("/api/staff/locations/") && path.split("/").length == 5 && "PUT".equals(method)) {
                String idStr = path.substring(path.lastIndexOf("/") + 1);
                try {
                    int locationId = Integer.parseInt(idStr);
                    handleUpdateLocationById(exchange, locationId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid location ID");
                }
                return;
            }

            // Route: GET /api/staff/location-students - Get students by location
            if (path.equals("/api/staff/location-students") && "GET".equals(method)) {
                handleGetLocationStudents(exchange);
                return;
            }

            // Route: PUT /api/staff/application/fees - Update application fees
            if (path.equals("/api/staff/application/fees") && "PUT".equals(method)) {
                handleUpdateFees(exchange);
                return;
            }

            // Route: PUT /api/staff/application/location - Update application location
            if (path.equals("/api/staff/application/location") && "PUT".equals(method)) {
                handleUpdateLocation(exchange);
                return;
            }

            // Route: PUT /api/staff/profile - Update staff profile
            if (path.equals("/api/staff/profile") && "PUT".equals(method)) {
                handleUpdateProfile(exchange);
                return;
            }

            // Route: GET /api/staff/fees-summary - Get fees summary by location
            if (path.equals("/api/staff/fees-summary") && "GET".equals(method)) {
                handleGetFeesSummary(exchange);
                return;
            }

            // Route: GET /api/staff/my-students - Get assigned students
            if (path.equals("/api/staff/my-students") && "GET".equals(method)) {
                handleGetMyStudents(exchange, payload);
                return;
            }

            // Route: GET /api/staff/all - Get all staff members (admin only)
            if (path.equals("/api/staff/all") && "GET".equals(method)) {
                handleGetAllStaff(exchange);
                return;
            }

            // Route: PUT /api/staff/update - Update staff member
            if (path.equals("/api/staff/update") && "PUT".equals(method)) {
                handleUpdateStaff(exchange);
                return;
            }
            
            // Route: POST /api/staff/create - Create new staff member (admin only)
            if (path.equals("/api/staff/create") && "POST".equals(method)) {
                handleCreateStaff(exchange);
                return;
            }

            // Route: DELETE /api/staff/delete/{id} - Delete staff member
            if (path.startsWith("/api/staff/delete/") && "DELETE".equals(method)) {
                String staffIdStr = path.substring("/api/staff/delete/".length());
                handleDeleteStaff(exchange, staffIdStr);
                return;
            }

            // Route: PUT /api/staff/change-password - Change staff password (admin only)
            if (path.equals("/api/staff/change-password") && "PUT".equals(method)) {
                handleChangeStaffPassword(exchange);
                return;
            }

            // Route: GET /api/staff/application/{id} - Get application by ID (for staff to edit students)
            if (path.startsWith("/api/staff/application/") && "GET".equals(method)) {
                String appIdStr = path.substring("/api/staff/application/".length());
                handleGetApplication(exchange, appIdStr);
                return;
            }
            
            // Route: PUT /api/staff/application/{id} - Update application (for staff to edit students)
            if (path.startsWith("/api/staff/application/") && "PUT".equals(method)) {
                String appIdStr = path.substring("/api/staff/application/".length());
                handleUpdateApplication(exchange, appIdStr);
                return;
            }
            
            // Route: DELETE /api/staff/application/{id} - Delete application (for staff)
            if (path.startsWith("/api/staff/application/") && "DELETE".equals(method)) {
                String appIdStr = path.substring("/api/staff/application/".length());
                handleDeleteApplication(exchange, appIdStr);
                return;
            }
            
            // Route: GET /api/staff/applicants - Get all applicants from staff's location
            if (path.equals("/api/staff/applicants") && "GET".equals(method)) {
                handleGetStaffApplicants(exchange);
                return;
            }
            
            // Route: GET /api/staff/users - Get all users with their applications
            if (path.equals("/api/staff/users") && "GET".equals(method)) {
                handleGetAllUsersWithApps(exchange);
                return;
            }
            
            // Route: GET /api/staff/user/{userId} - Get user details for editing
            if (path.startsWith("/api/staff/user/") && "GET".equals(method)) {
                String userIdStr = path.substring("/api/staff/user/".length());
                handleGetStaffUser(exchange, userIdStr);
                return;
            }
            
            // Route: PUT /api/staff/user/{userId} - Update user fees/details
            if (path.startsWith("/api/staff/user/") && "PUT".equals(method)) {
                String userIdStr = path.substring("/api/staff/user/".length());
                handleUpdateStaffUser(exchange, userIdStr);
                return;
            }
            
            // Route: GET /api/staff/mpesa-messages - Get Mpesa messages by location
            if (path.equals("/api/staff/mpesa-messages") && "GET".equals(method)) {
                handleGetMpesaMessages(exchange);
                return;
            }

            // Route: POST /api/staff/mpesa-message/verify - Verify or reject Mpesa message
            if (path.equals("/api/staff/mpesa-message/verify") && "POST".equals(method)) {
                handleVerifyMpesaMessage(exchange);
                return;
            }
            
            // Route: GET /api/staff/all-applications - Get all applications (for staff dashboard)
            if (path.equals("/api/staff/all-applications") && "GET".equals(method)) {
                handleGetAllApplications(exchange);
                return;
            }
            
            // Route: GET /api/staff/students - Get all students
            if (path.equals("/api/staff/students") && "GET".equals(method)) {
                handleGetAllStudents(exchange);
                return;
            }
            
            // Route: GET /api/staff/applications - Get applications (alias for /all-applications)
            if (path.equals("/api/staff/applications") && "GET".equals(method)) {
                handleGetAllApplications(exchange);
                return;
            }

            sendErrorResponse(exchange, 404, "Endpoint not found");

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    /**
     * Get all training locations
     */
    private void handleGetLocations(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> locations = DBConnection.getAllLocations();
        String json = "{\"success\": true, \"locations\": " + listToJsonMaps(locations) + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get application by ID for staff (allows all staff to access any application)
     */
    private void handleGetApplication(HttpExchange exchange, String appIdStr) throws IOException {
        try {
            int appId = Integer.parseInt(appIdStr);
            
            // Get the current user's info
            String token = getTokenFromHeader(exchange);
            String role = JWTUtil.getRole(token);
            
            // Get the application
            Map<String, Object> app = DBConnection.getApplicationById(appId);
            
            if (app == null) {
                sendErrorResponse(exchange, 404, "Application not found");
                return;
            }
            
            // Fetch M-Pesa messages for this application
            List<Map<String, Object>> mpesaMessages = DBConnection.getMpesaMessagesByAppId(appId);
            app.put("mpesa_messages", mpesaMessages);
            
            // Allow admin and staff to view any application
            if ("admin".equals(role) || "staff".equals(role)) {
                String json = "{\"success\": true, \"application\": " + mapToJson(app) + "}";
                sendJsonResponse(exchange, 200, json);
                return;
            }
            
            // For regular users, check if they own the application
            int userId = JWTUtil.getUserId(token);
            List<Map<String, Object>> userApps = DBConnection.getApplicationsByUserId(userId);
            boolean ownsApp = userApps.stream().anyMatch(a -> a.get("id").equals(appId));
            
            if (ownsApp) {
                String json = "{\"success\": true, \"application\": " + mapToJson(app) + "}";
                sendJsonResponse(exchange, 200, json);
            } else {
                sendErrorResponse(exchange, 403, "Access denied");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid application ID");
        }
    }

    /**
     * Get students by training location
     */
    private void handleGetLocationStudents(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI());
        String location = params.get("location");
        String role = params.get("role");
        
        // If no location is provided, return all users with role 'user' (students)
        if ((location == null || location.isEmpty()) && (role == null || role.isEmpty())) {
            // Return empty by default unless role filter is provided
            sendJsonResponse(exchange, 200, "{\"success\": true, \"students\": [], \"count\": 0}");
            return;
        }

        List<Map<String, Object>> students;
        
        if (location != null && !location.isEmpty()) {
            students = DBConnection.getApplicationsByLocation(location);
        } else if (role != null && !role.isEmpty()) {
            // Get all users by role (e.g., role=user)
            students = DBConnection.getUsersByRole(role);
        } else {
            students = new ArrayList<>();
        }
        
        String json = "{\"success\": true, \"students\": " + listMapToJson(students) + ", \"count\": " + students.size() + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Update application (full update for staff)
     */
    private void handleUpdateApplication(HttpExchange exchange, String appIdStr) throws IOException {
        try {
            int appId = Integer.parseInt(appIdStr);
            
            String body = getRequestBody(exchange);
            Map<String, String> params = parseFormData(body);
            
            // First get the application to find the user_id
            Map<String, Object> existingApp = DBConnection.getApplicationById(appId);
            if (existingApp == null) {
                sendErrorResponse(exchange, 404, "Application not found");
                return;
            }
            int userId = 0;
            Object userIdObj = existingApp.get("user_id");
            if (userIdObj != null) {
                userId = ((Number) userIdObj).intValue();
            }
            
            // Filter out empty strings - only include fields with actual values
            Map<String, String> filteredParams = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String value = entry.getValue();
                if (value != null && !value.trim().isEmpty()) {
                    filteredParams.put(entry.getKey(), value);
                }
            }
            
            // Update user details if user_id exists and there are user fields to update
            if (userId > 0 && (filteredParams.containsKey("first_name") || filteredParams.containsKey("last_name") || 
                filteredParams.containsKey("phone") || filteredParams.containsKey("date_of_birth") || filteredParams.containsKey("id_number")
                || filteredParams.containsKey("emergency_contact_name") || filteredParams.containsKey("emergency_contact_phone"))) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("user_id", userId);
                if (filteredParams.containsKey("first_name")) userData.put("first_name", filteredParams.get("first_name"));
                if (filteredParams.containsKey("last_name")) userData.put("last_name", filteredParams.get("last_name"));
                if (filteredParams.containsKey("phone")) userData.put("phone", filteredParams.get("phone"));
                if (filteredParams.containsKey("date_of_birth")) userData.put("date_of_birth", filteredParams.get("date_of_birth"));
                if (filteredParams.containsKey("id_number")) userData.put("id_number", filteredParams.get("id_number"));
                if (filteredParams.containsKey("emergency_contact_name")) userData.put("emergency_contact_name", filteredParams.get("emergency_contact_name"));
                if (filteredParams.containsKey("emergency_contact_phone")) userData.put("emergency_contact_phone", filteredParams.get("emergency_contact_phone"));
                DBConnection.updateStudentDetails(userData);
            }
            
            // Update application details only if there are fields to update
            if (!filteredParams.isEmpty()) {
                Map<String, Object> applicationData = new HashMap<>();
                applicationData.put("id", appId);
                
                for (Map.Entry<String, String> entry : filteredParams.entrySet()) {
                    applicationData.put(entry.getKey(), entry.getValue());
                }
                
                // Handle location
                if (filteredParams.containsKey("training_location") || filteredParams.containsKey("location")) {
                    String loc = filteredParams.get("training_location") != null ? filteredParams.get("training_location") : filteredParams.get("location");
                    applicationData.put("location", loc);
                    applicationData.put("training_location", loc);
                }
                
                // Handle fees
                if (filteredParams.containsKey("school_fees")) {
                    applicationData.put("school_fees", Double.parseDouble(filteredParams.get("school_fees")));
                }
                if (filteredParams.containsKey("fees_paid")) {
                    applicationData.put("fees_paid", Double.parseDouble(filteredParams.get("fees_paid")));
                }
                if (filteredParams.containsKey("fees_balance")) {
                    applicationData.put("fees_balance", Double.parseDouble(filteredParams.get("fees_balance")));
                }
                
                DBConnection.updateApplication(applicationData);
            }
            
            sendSuccessResponse(exchange, "Application updated successfully");
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid application ID or number format");
        }
    }
    
    /**
     * Delete application (for staff)
     */
    private void handleDeleteApplication(HttpExchange exchange, String appIdStr) throws IOException {
        try {
            int appId = Integer.parseInt(appIdStr);
            
            // Verify the user is a staff member
            String token = getTokenFromHeader(exchange);
            String role = JWTUtil.getRole(token);
            
            // Only admin and staff can delete applications
            if (!"admin".equals(role) && !"staff".equals(role)) {
                sendErrorResponse(exchange, 403, "Access denied");
                return;
            }
            
            // Check if application exists
            Map<String, Object> app = DBConnection.getApplicationById(appId);
            if (app == null) {
                sendErrorResponse(exchange, 404, "Application not found");
                return;
            }
            
            // Delete the application
            if (DBConnection.deleteApplication(appId)) {
                sendSuccessResponse(exchange, "Application deleted successfully");
            } else {
                sendErrorResponse(exchange, 500, "Failed to delete application");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid application ID");
        }
    }

    /**
     * Update application fees
     */
    private void handleUpdateFees(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseFormData(getRequestBody(exchange));

        String appId = params.get("application_id");
        String schoolFees = params.get("school_fees");
        String feesPaid = params.get("fees_paid");
        String paymentStatus = params.get("payment_status");
        String paymentMethod = params.get("payment_method");

        if (appId == null) {
            sendErrorResponse(exchange, 400, "Application ID required");
            return;
        }

        try {
            int applicationId = Integer.parseInt(appId);
            
            // Calculate fees balance
            double fees = schoolFees != null ? Double.parseDouble(schoolFees) : 0;
            double paid = feesPaid != null ? Double.parseDouble(feesPaid) : 0;
            double balance = fees - paid;
            String feesBalance = String.valueOf(balance);
            
            boolean success = DBConnection.updateApplicationFees(applicationId, schoolFees, feesPaid, feesBalance, paymentStatus, paymentMethod);

            if (success) {
                sendSuccessResponse(exchange, "Fees updated successfully");
            } else {
                sendErrorResponse(exchange, 500, "Failed to update fees");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid application ID");
        }
    }

    /**
     * Update application location
     */
    private void handleUpdateLocation(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseFormData(getRequestBody(exchange));

        String appId = params.get("application_id");
        String location = params.get("training_location");

        if (appId == null || location == null) {
            sendErrorResponse(exchange, 400, "Application ID and location required");
            return;
        }

        try {
            int applicationId = Integer.parseInt(appId);
            boolean success = DBConnection.updateApplicationLocation(applicationId, location);

            if (success) {
                sendSuccessResponse(exchange, "Location updated successfully");
            } else {
                sendErrorResponse(exchange, 500, "Failed to update location");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid application ID");
        }
    }
    
    /**
     * Update a location by ID
     */
    private void handleUpdateLocationById(HttpExchange exchange, int locationId) throws IOException {
        Map<String, String> params = parseFormData(getRequestBody(exchange));

        String name = params.get("name");
        String address = params.get("address");
        String phone = params.get("phone");
        String email = params.get("email");
        String isActiveStr = params.get("is_active");
        String deleteApplications = params.get("delete_applications");

        if (name == null) {
            sendErrorResponse(exchange, 400, "Location name is required");
            return;
        }

        boolean isActive = isActiveStr == null || Boolean.parseBoolean(isActiveStr);
        boolean shouldDeleteApps = "true".equalsIgnoreCase(deleteApplications);

        try {
            // If deactivating and delete_applications is true, delete applications with this location first
            if (!isActive && shouldDeleteApps) {
                DBConnection.deleteApplicationsByLocationId(locationId);
            }
            
            boolean success = DBConnection.updateLocationById(locationId, name, address, phone, email, isActive);

            if (success) {
                sendSuccessResponse(exchange, "Location updated successfully");
            } else {
                sendErrorResponse(exchange, 500, "Failed to update location");
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Error updating location: " + e.getMessage());
        }
    }
    
    /**
     * Create a new location
     */
    private void handleCreateLocation(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseFormData(getRequestBody(exchange));

        String name = params.get("name");
        String address = params.get("address");
        String phone = params.get("phone");
        String email = params.get("email");

        if (name == null || name.trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Location name is required");
            return;
        }

        try {
            int newId = DBConnection.createLocation(name.trim(), address, phone, email);

            if (newId > 0) {
                String json = "{\"success\": true, \"message\": \"Location created successfully\", \"id\": " + newId + "}";
                sendJsonResponse(exchange, 201, json);
            } else {
                sendErrorResponse(exchange, 500, "Failed to create location");
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Error creating location: " + e.getMessage());
        }
    }

    /**
     * Update staff profile
     */
    private void handleUpdateProfile(HttpExchange exchange) throws IOException {
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);

        String userId = params.get("user_id");
        String trainingLocation = params.get("training_location");
        String fullName = params.get("full_name");
        String phone = params.get("phone");

        if (userId == null) {
            sendErrorResponse(exchange, 400, "User ID required");
            return;
        }

        try {
            int staffId = Integer.parseInt(userId);
            
            // Update staff location if provided
            if (trainingLocation != null) {
                DBConnection.updateStaffLocation(staffId, trainingLocation);
            }

            // Update profile if provided
            if (fullName != null || phone != null) {
                String currentFullName = fullName != null ? fullName : "";
                String currentPhone = phone != null ? phone : "";
                String currentEmail = ""; // Email cannot be changed
                String currentPassword = null; // Password not changed via this endpoint
                DBConnection.updateUserProfile(staffId, currentFullName, currentEmail, currentPhone, currentPassword, trainingLocation);
            }

            sendSuccessResponse(exchange, "Profile updated successfully");
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid user ID");
        }
    }
    
    /**
     * Create new staff member (admin only)
     */
    private void handleCreateStaff(HttpExchange exchange) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        String role = JWTUtil.getRole(token);
        
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String fullName = params.get("full_name");
        String email = params.get("email");
        String phone = params.get("phone");
        String trainingLocation = params.get("training_location");
        String userRole = params.get("role");
        String password = params.get("password");
        
        if (fullName == null || email == null || phone == null) {
            sendErrorResponse(exchange, 400, "Full name, email, and phone are required");
            return;
        }
        
        // Default role to staff if not provided
        if (userRole == null || userRole.isEmpty()) {
            userRole = "staff";
        }
        
        // Check if email already exists - if so, promote existing user to staff
        if (DBConnection.emailExists(email)) {
            // Get existing user by email
            List<Map<String, Object>> users = DBConnection.getAllUsers();
            int existingUserId = -1;
            String currentRole = "applicant";
            
            for (Map<String, Object> user : users) {
                if (email.equals(user.get("email"))) {
                    existingUserId = (Integer) user.get("id");
                    currentRole = (String) user.get("role");
                    break;
                }
            }
            
            if (existingUserId > 0) {
                // Update role to staff/admin
                if (!currentRole.equals(userRole)) {
                    DBConnection.updateUserRole(existingUserId, userRole);
                }
                
                // Update profile
                DBConnection.updateUserProfile(existingUserId, fullName, email, phone, null, trainingLocation);
                
                String successMsg = "User promoted to " + userRole;
                String json = "{\"success\": true, \"message\": \"" + successMsg + "\", \"user_id\": " + existingUserId + "}";
                sendJsonResponse(exchange, 200, json);
            } else {
                sendErrorResponse(exchange, 500, "Failed to update existing user");
            }
            return;
        }
        
        // Use default password if not provided
        if (password == null || password.isEmpty()) {
            password = "staff123";
        }
        
        String passwordHash = DBConnection.hashPassword(password);
        
        // Split full name into first and last
        String firstName = fullName;
        String lastName = "";
        if (fullName != null && fullName.contains(" ")) {
            int spaceIndex = fullName.lastIndexOf(' ');
            firstName = fullName.substring(0, spaceIndex);
            lastName = fullName.substring(spaceIndex + 1);
        }
        
        int userId = DBConnection.registerUser(email, passwordHash, firstName, lastName, phone, userRole);
        
        if (userId > 0) {
            // Update training location if provided
            if (trainingLocation != null && !trainingLocation.isEmpty()) {
                DBConnection.updateStaffLocation(userId, trainingLocation);
            }
            
            String json = "{\"success\": true, \"message\": \"Staff member created successfully\", \"user_id\": " + userId + ", \"password\": \"" + password + "\"}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to create staff member");
        }
    }

    /**
     * Get fees summary by location
     */
    private void handleGetFeesSummary(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI());
        String location = params.get("location");
        String role = params.get("role");

        // If no location is provided, try to get all users summary
        if ((location == null || location.isEmpty()) && (role == null || role.isEmpty())) {
            // Return zero summary by default
            Map<String, Object> emptySummary = new HashMap<>();
            emptySummary.put("total_students", 0);
            emptySummary.put("total_fees", 0);
            emptySummary.put("total_paid", 0);
            emptySummary.put("total_balance", 0);
            String json = "{\"success\": true, \"summary\": " + mapToJson(emptySummary) + "}";
            sendJsonResponse(exchange, 200, json);
            return;
        }

        Map<String, Object> summary;
        if (location != null && !location.isEmpty()) {
            summary = DBConnection.getFeesSummaryByLocation(location);
        } else {
            // Get fees summary for all users with specified role
            summary = new HashMap<>();
            summary.put("total_students", 0);
            summary.put("total_fees", 0);
            summary.put("total_paid", 0);
            summary.put("total_balance", 0);
        }
        String json = "{\"success\": true, \"summary\": " + mapToJson(summary) + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get staff's assigned students
     */
    private void handleGetMyStudents(HttpExchange exchange, Map<String, Object> payload) throws IOException {
        int staffId = ((Number) payload.get("sub")).intValue();
        List<Map<String, Object>> students = DBConnection.getStaffAssignedApplications(staffId);
        String json = "{\"success\": true, \"students\": " + listMapToJson(students) + ", \"count\": " + students.size() + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get all staff members (admin only)
     */
    private void handleGetAllStaff(HttpExchange exchange) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        String role = JWTUtil.getRole(token);
        
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        List<Map<String, Object>> staff = DBConnection.getAllStaff();
        String json = "{\"success\": true, \"staff\": " + listMapToJson(staff) + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Update staff member
     */
    private void handleUpdateStaff(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseFormData(getRequestBody(exchange));
        
        String userId = params.get("user_id");
        String fullName = params.get("full_name");
        String email = params.get("email");
        String phone = params.get("phone");
        String trainingLocation = params.get("training_location");
        
        if (userId == null || fullName == null || email == null) {
            sendErrorResponse(exchange, 400, "User ID, full name, and email are required");
            return;
        }
        
        try {
            int id = Integer.parseInt(userId);
            boolean success = DBConnection.updateUserProfile(id, fullName, email, phone, null, trainingLocation);
            
            if (success) {
                String json = "{\"success\": true, \"message\": \"Staff updated successfully\"}";
                sendJsonResponse(exchange, 200, json);
            } else {
                sendErrorResponse(exchange, 500, "Failed to update staff");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid user ID");
        }
    }

    /**
     * Delete staff member
     */
    private void handleDeleteStaff(HttpExchange exchange, String staffIdStr) throws IOException {
        try {
            int staffId = Integer.parseInt(staffIdStr);
            boolean success = DBConnection.deleteUser(staffId);
            
            if (success) {
                String json = "{\"success\": true, \"message\": \"Staff deleted successfully\"}";
                sendJsonResponse(exchange, 200, json);
            } else {
                sendErrorResponse(exchange, 500, "Failed to delete staff");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid staff ID");
        }
    }

    /**
     * Change staff password (admin only)
     */
    private void handleChangeStaffPassword(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseFormData(getRequestBody(exchange));
        
        String userId = params.get("user_id");
        String newPassword = params.get("password");
        
        if (userId == null || newPassword == null || newPassword.isEmpty()) {
            sendErrorResponse(exchange, 400, "User ID and new password are required");
            return;
        }
        
        try {
            int staffId = Integer.parseInt(userId);
            String passwordHash = DBConnection.hashPassword(newPassword);
            
            // Update user's password_hash directly
            String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, passwordHash);
                stmt.setInt(2, staffId);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Revoke all existing tokens
                    DBConnection.revokeAllUserTokens(staffId);
                    
                    String json = "{\"success\": true, \"message\": \"Password changed successfully\"}";
                    sendJsonResponse(exchange, 200, json);
                } else {
                    sendErrorResponse(exchange, 500, "Failed to change password");
                }
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid user ID");
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Error changing password: " + e.getMessage());
        }
    }

    /**
     * Get Mpesa messages by location or phone
     */
    private void handleGetMpesaMessages(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI());
        String location = params.get("location");
        String phone = params.get("phone");
        String status = params.get("status");

        // If phone is provided, filter by phone (for fees modal)
        if (phone != null && !phone.isEmpty()) {
            List<Map<String, Object>> messages = DBConnection.getMpesaMessagesByPhone(phone);
            String json = "{\"success\": true, \"messages\": " + listMapToJson(messages) + ", \"count\": " + messages.size() + "}";
            sendJsonResponse(exchange, 200, json);
            return;
        }

        // Otherwise, filter by location
        if (location == null || location.isEmpty()) {
            sendErrorResponse(exchange, 400, "Location or phone parameter required");
            return;
        }

        List<Map<String, Object>> messages = DBConnection.getMpesaMessagesByLocation(location, status);
        String json = "{\"success\": true, \"messages\": " + listMapToJson(messages) + ", \"count\": " + messages.size() + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Verify or reject Mpesa message
     */
    private void handleVerifyMpesaMessage(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseFormData(getRequestBody(exchange));

        String messageId = params.get("message_id");
        String verifiedStr = params.get("verified");

        if (messageId == null || verifiedStr == null) {
            sendErrorResponse(exchange, 400, "Message ID and verification status required");
            return;
        }

        try {
            int id = Integer.parseInt(messageId);
            boolean verified = "true".equalsIgnoreCase(verifiedStr);
            boolean success = DBConnection.verifyMpesaMessage(id, verified);

            if (success) {
                String msg = verified ? "Message verified successfully" : "Message rejected";
                String json = "{\"success\": true, \"message\": \"" + msg + "\"}";
                sendJsonResponse(exchange, 200, json);
            } else {
                sendErrorResponse(exchange, 500, "Failed to update message");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid message ID");
        }
    }
    
    /**
     * Get all applications (for staff dashboard)
     */
    private void handleGetAllApplications(HttpExchange exchange) throws IOException {
        try {
            List<Map<String, Object>> applications = DBConnection.getAllApplications();
            String json = "{\"success\": true, \"applications\": " + listToJsonMaps(applications) + ", " +
                         "\"count\": " + applications.size() + "}";
            sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Failed to get applications: " + e.getMessage());
        }
    }
    
    /**
     * Get all students
     */
    private void handleGetAllStudents(HttpExchange exchange) throws IOException {
        try {
            List<Map<String, Object>> students = DBConnection.getStudents();
            String json = "{\"success\": true, \"students\": " + listToJsonMaps(students) + ", " +
                         "\"count\": " + students.size() + "}";
            sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Failed to get students: " + e.getMessage());
        }
    }
    
    /**
     * Get all applicants from staff's training location
     */
    private void handleGetStaffApplicants(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        String role = JWTUtil.getRole(token);
        int userId = JWTUtil.getUserId(token);
        
        // Parse query parameters for location filter
        String query = exchange.getRequestURI().getQuery();
        String requestedLocation = null;
        if (query != null && query.contains("location=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("location=")) {
                    requestedLocation = java.net.URLDecoder.decode(param.split("=")[1], "UTF-8");
                    break;
                }
            }
        }
        
        // Admin can see all applicants or filter by location
        if ("admin".equals(role)) {
            if (requestedLocation != null && !requestedLocation.isEmpty()) {
                // Admin requesting specific location
                List<Map<String, Object>> apps = DBConnection.getApplicationsByLocation(requestedLocation);
                // Add mpesa_messages to each application
                for (Map<String, Object> app : apps) {
                    int appId = (Integer) app.get("id");
                    List<Map<String, Object>> mpesaMsgs = DBConnection.getMpesaMessagesByAppId(appId);
                    app.put("mpesa_messages", mpesaMsgs);
                }
                String json = "{\"success\": true, \"applicants\": " + listMapToJson(apps) + ", \"count\": " + apps.size() + ", \"location\": \"" + requestedLocation + "\"}";
                sendJsonResponse(exchange, 200, json);
            } else {
                // Admin requesting all applicants
                List<Map<String, Object>> users = DBConnection.getAllUsers();
                // Filter to only applicants
                List<Map<String, Object>> applicants = users.stream()
                    .filter(u -> "applicant".equals(u.get("role")) || "user".equals(u.get("role")))
                    .collect(Collectors.toList());
                String json = "{\"success\": true, \"applicants\": " + listMapToJson(applicants) + ", \"count\": " + applicants.size() + "}";
                sendJsonResponse(exchange, 200, json);
            }
            return;
        }
        
        // Staff can only see applicants from their assigned location (from database)
        String staffLocation = DBConnection.getUserLocation(userId);
        if (staffLocation == null || staffLocation.isEmpty()) {
            // No location assigned - return all applicants with their application data
            List<Map<String, Object>> users = DBConnection.getAllUsers();
            // Filter to only applicants
            List<Map<String, Object>> applicants = users.stream()
                .filter(u -> "applicant".equals(u.get("role")) || "user".equals(u.get("role")))
                .collect(Collectors.toList());
            
            // For each user, try to get their application data
            for (Map<String, Object> applicant : applicants) {
                int userIdVal = (Integer) applicant.get("id");
                List<Map<String, Object>> userApps = DBConnection.getApplicationsByUserId(userIdVal);
                if (!userApps.isEmpty()) {
                    // Merge application data into user object
                    Map<String, Object> app = userApps.get(0);
                    applicant.put("application_id", app.get("id"));
                    applicant.put("license_type", app.get("license_type"));
                    applicant.put("driving_course", app.get("driving_course"));
                    applicant.put("computer_course", app.get("computer_course"));
                    applicant.put("transmission", app.get("transmission"));
                    applicant.put("preferred_schedule", app.get("preferred_schedule"));
                    applicant.put("school_fees", app.get("school_fees"));
                    applicant.put("fees_paid", app.get("fees_paid"));
                    applicant.put("payment_status", app.get("payment_status"));
                    applicant.put("status", app.get("status"));
                }
            }
            
            String json = "{\"success\": true, \"applicants\": " + listMapToJson(applicants) + ", \"count\": " + applicants.size() + "}";
            sendJsonResponse(exchange, 200, json);
            return;
        }
        
        // Use staff's assigned location (ignore query param for security)
        List<Map<String, Object>> apps = DBConnection.getApplicationsByLocation(staffLocation);
        // Add mpesa_messages to each application
        for (Map<String, Object> app : apps) {
            int appId = (Integer) app.get("id");
            List<Map<String, Object>> mpesaMsgs = DBConnection.getMpesaMessagesByAppId(appId);
            app.put("mpesa_messages", mpesaMsgs);
        }
        String json = "{\"success\": true, \"applicants\": " + listMapToJson(apps) + ", \"count\": " + apps.size() + ", \"location\": \"" + staffLocation + "\"}";
        sendJsonResponse(exchange, 200, json);
    }
    
    /**
     * Get all users with their applications for staff/admin
     */
    private void handleGetAllUsersWithApps(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        String role = JWTUtil.getRole(token);
        int userId = JWTUtil.getUserId(token);
        
        // Get all users
        List<Map<String, Object>> users = DBConnection.getAllUsers();
        
        // Build response with applications for each user
        List<Map<String, Object>> usersWithApps = users.stream().map(user -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.get("id"));
            userData.put("email", user.get("email"));
            userData.put("full_name", user.get("full_name"));
            userData.put("phone", user.get("phone"));
            userData.put("role", user.get("role"));
            userData.put("location", user.get("location"));
            userData.put("created_at", user.get("created_at"));
            
            // Get applications for this user
            int uid = (Integer) user.get("id");
            List<Map<String, Object>> apps = DBConnection.getApplicationsByUserId(uid);
            
            // Filter apps by staff location if not admin
            if (!"admin".equals(role)) {
                String staffLocation = DBConnection.getUserLocation(userId);
                apps = apps.stream().filter(app -> {
                    String appLocation = (String) app.get("training_location");
                    return staffLocation.equals(appLocation);
                }).collect(Collectors.toList());
            }
            
            userData.put("applications", apps);
            userData.put("application_count", apps.size());
            
            // Calculate total fees and paid
            double totalFees = apps.stream().mapToDouble(a -> (Double) a.getOrDefault("school_fees", 0.0)).sum();
            double totalPaid = apps.stream().mapToDouble(a -> (Double) a.getOrDefault("fees_paid", 0.0)).sum();
            userData.put("total_fees", totalFees);
            userData.put("total_paid", totalPaid);
            userData.put("total_balance", totalFees - totalPaid);
            
            return userData;
        }).collect(Collectors.toList());
        
        String json = "{\"success\": true, \"users\": " + listMapToJson(usersWithApps) + ", \"count\": " + usersWithApps.size() + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get user details for staff to edit
     */
    private void handleGetStaffUser(HttpExchange exchange, String userIdStr) throws IOException {
        try {
            int userId = Integer.parseInt(userIdStr);
            
            String token = getTokenFromHeader(exchange);
            String role = JWTUtil.getRole(token);
            int staffUserId = JWTUtil.getUserId(token);
            
            // Get the user
            Map<String, Object> user = DBConnection.getUserById(userId);
            if (user == null) {
                sendErrorResponse(exchange, 404, "User not found");
                return;
            }
            
            // Admin can access any user
            if ("admin".equals(role)) {
                user.remove("password_hash");
                String json = "{\"success\": true, \"user\": " + mapToJson(user) + "}";
                sendJsonResponse(exchange, 200, json);
                return;
            }
            
            // Staff can only access users from their location
            String staffLocation = DBConnection.getUserLocation(staffUserId);
            String userLocation = DBConnection.getUserLocation(userId);
            
            // Check if user's application is at staff's location
            List<Map<String, Object>> userApps = DBConnection.getApplicationsByUserId(userId);
            boolean hasAccess = userApps.stream().anyMatch(app -> {
                String appLocation = (String) app.get("training_location");
                return staffLocation.equals(appLocation);
            });
            
            if (hasAccess) {
                user.remove("password_hash");
                String json = "{\"success\": true, \"user\": " + mapToJson(user) + "}";
                sendJsonResponse(exchange, 200, json);
            } else {
                sendErrorResponse(exchange, 403, "Access denied: User not at your location");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid user ID");
        }
    }

    /**
     * Update user fees/details by staff
     */
    private void handleUpdateStaffUser(HttpExchange exchange, String userIdStr) throws IOException {
        try {
            int userId = Integer.parseInt(userIdStr);
            
            String token = getTokenFromHeader(exchange);
            String role = JWTUtil.getRole(token);
            int staffUserId = JWTUtil.getUserId(token);
            
            // Check access for staff
            if (!"admin".equals(role)) {
                String staffLocation = DBConnection.getUserLocation(staffUserId);
                List<Map<String, Object>> userApps = DBConnection.getApplicationsByUserId(userId);
                boolean hasAccess = userApps.stream().anyMatch(app -> {
                    String appLocation = (String) app.get("training_location");
                    return staffLocation.equals(appLocation);
                });
                
                if (!hasAccess) {
                    sendErrorResponse(exchange, 403, "Access denied: User not at your location");
                    return;
                }
            }
            
            String body = getRequestBody(exchange);
            Map<String, String> params = parseFormData(body);
            
            // Handle fees update
            String schoolFeesStr = params.get("school_fees");
            String feesPaidStr = params.get("fees_paid");
            String feesBalanceStr = params.get("fees_balance");
            String paymentStatus = params.get("payment_status");
            
            if (schoolFeesStr != null && feesPaidStr != null) {
                double schoolFees = Double.parseDouble(schoolFeesStr);
                double feesPaid = Double.parseDouble(feesPaidStr);
                double feesBalance = feesBalanceStr != null ? Double.parseDouble(feesBalanceStr) : (schoolFees - feesPaid);
                
                if (DBConnection.updateUserFees(userId, schoolFees, feesPaid, feesBalance, paymentStatus != null ? paymentStatus : "unpaid")) {
                    sendSuccessResponse(exchange, "User fees updated successfully");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to update user fees");
                }
                return;
            }
            
            // Handle personal details update
            String fullName = params.get("full_name");
            String email = params.get("email");
            String phone = params.get("phone");
            String trainingLocation = params.get("training_location");
            
            if (DBConnection.updateUserProfile(userId, fullName, email, phone, null, trainingLocation)) {
                sendSuccessResponse(exchange, "User details updated successfully");
            } else {
                sendErrorResponse(exchange, 500, "Failed to update user details");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid user ID or number format");
        }
    }

    /**
     * Helper to convert List to JSON array string (for String lists)
     */
    private String listToJsonStrings(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Helper to convert List of Maps to JSON array string
     */
    private String listToJsonMaps(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(mapToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}
