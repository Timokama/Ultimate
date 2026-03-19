import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base HTTP Handler with common utilities
 */
class ApiHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Default implementation - subclasses handle their own requests
        sendErrorResponse(exchange, 404, "Endpoint not found");
    }
    
    protected void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, json.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(json.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
    
    protected void sendSuccessResponse(HttpExchange exchange, String message) throws IOException {
        String json = "{\"success\": true, \"message\": \"" + message + "\"}";
        sendJsonResponse(exchange, 200, json);
    }
    
    protected void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = "{\"success\": false, \"error\": \"" + message + "\"}";
        sendJsonResponse(exchange, statusCode, json);
    }
    
    protected String escapeJson(Object value) {
        if (value == null) return "";
        String str = value.toString();
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    protected void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }
    
    protected void sendHtmlResponse(HttpExchange exchange, int statusCode, String html) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, html.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(html.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
    
    protected String getTokenFromHeader(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    protected String getRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.joining("\n"));
    }
    
    protected Map<String, String> parseFormData(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return params;
        }
        
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }
    
    // Simple JSON parser for key-value pairs (handles simple JSON objects)
    protected Map<String, String> parseJsonBody(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return params;
        }
        
        // Simple parser - handle "key": "value" patterns
        int len = body.length();
        int i = 0;
        while (i < len) {
            // Skip whitespace
            while (i < len && Character.isWhitespace(body.charAt(i))) i++;
            if (i >= len) break;
            
            if (body.charAt(i) == '"') {
                // Find key
                i++;
                StringBuilder key = new StringBuilder();
                while (i < len) {
                    if (body.charAt(i) == '\\' && i + 1 < len) {
                        // Handle escape sequences
                        i++;
                        char escaped = body.charAt(i);
                        switch (escaped) {
                            case 'n': key.append('\n'); break;
                            case 't': key.append('\t'); break;
                            case 'r': key.append('\r'); break;
                            case '\\': key.append('\\'); break;
                            case '"': key.append('"'); break;
                            default: key.append(escaped); break;
                        }
                        i++;
                    } else if (body.charAt(i) == '"') {
                        break; // End of key
                    } else {
                        key.append(body.charAt(i++));
                    }
                }
                i++; // skip closing quote
                
                // Skip colon
                while (i < len && (body.charAt(i) == ':' || Character.isWhitespace(body.charAt(i)))) i++;
                
                if (i < len) {
                    StringBuilder value = new StringBuilder();
                    if (body.charAt(i) == '"') {
                        // String value
                        i++;
                        while (i < len) {
                            if (body.charAt(i) == '\\' && i + 1 < len) {
                                // Handle escape sequences
                                i++;
                                char escaped = body.charAt(i);
                                switch (escaped) {
                                    case 'n': value.append('\n'); break;
                                    case 't': value.append('\t'); break;
                                    case 'r': value.append('\r'); break;
                                    case '\\': value.append('\\'); break;
                                    case '"': value.append('"'); break;
                                    default: value.append(escaped); break;
                                }
                                i++;
                            } else if (body.charAt(i) == '"') {
                                break; // End of value
                            } else {
                                value.append(body.charAt(i++));
                            }
                        }
                        i++; // skip closing quote
                    } else {
                        // Numeric or other value
                        while (i < len && body.charAt(i) != ',' && body.charAt(i) != '}') {
                            value.append(body.charAt(i++));
                        }
                    }
                    params.put(key.toString(), value.toString().trim());
                }
            } else {
                i++;
            }
            
            // Skip comma
            while (i < len && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
        }
        return params;
    }
    
    protected Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                } else if (keyValue.length == 1) {
                    params.put(keyValue[0], "");
                }
            }
        }
        return params;
    }
    
    protected String readResourceFile(String fileName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    protected String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            // Skip null values
            if (value == null) continue;
            
            if (count > 0) json.append(",");
            json.append("\"").append(escapeJson(entry.getKey())).append("\":");
            
            if (value instanceof String) {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof java.sql.Timestamp) {
                json.append("\"").append(value.toString()).append("\"");
            } else if (value instanceof java.sql.Date) {
                json.append("\"").append(value.toString()).append("\"");
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }
            count++;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Convert list of maps to JSON array
     */
    protected String listMapToJson(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(mapToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}

/**
 * Handler for static files (HTML, CSS, JS)
 */
class StaticHandler extends ApiHandler {
    private final String webRoot;
    
    public StaticHandler(String webRoot) {
        this.webRoot = webRoot;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        // Default to index.html for root
        if (path.equals("/") || path.equals("")) {
            path = "/index.html";
        }
        
        // Remove leading slash
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        String filePath = webRoot + "/" + path;
        java.io.File file = new java.io.File(filePath);
        
        if (file.exists() && file.isFile()) {
            String contentType = getContentType(path);
            byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, content.length);
            OutputStream os = exchange.getResponseBody();
            os.write(content);
            os.close();
        } else {
            String errorHtml = "<html><body><h1>404 Not Found</h1><p>The requested file was not found.</p></body></html>";
            sendHtmlResponse(exchange, 404, errorHtml);
        }
    }
    
    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}

/**
 * Authentication Handler
 */
class AuthHandler extends ApiHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        try {
            if (path.equals("/api/auth/register") && "POST".equals(method)) {
                handleRegister(exchange);
            } else if (path.equals("/api/auth/login") && "POST".equals(method)) {
                handleLogin(exchange);
            } else if (path.equals("/api/auth/create-admin") && "POST".equals(method)) {
                handleCreateAdmin(exchange);
            } else if (path.equals("/api/auth/reset-password") && "POST".equals(method)) {
                handleResetPassword(exchange);
            } else if (path.equals("/api/auth/logout") && "POST".equals(method)) {
                handleLogout(exchange);
            } else if (path.equals("/api/auth/me") && "GET".equals(method)) {
                handleGetCurrentUser(exchange);
            } else if (path.equals("/api/auth/refresh") && "POST".equals(method)) {
                handleRefreshToken(exchange);
            } else {
                sendErrorResponse(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String email = params.get("email");
        String password = params.get("password");
        String fullName = params.get("full_name");
        String phone = params.get("phone");
        
        if (email == null || password == null || fullName == null) {
            sendErrorResponse(exchange, 400, "Missing required fields");
            return;
        }
        
        // Split full name into first and last
        String firstName = fullName;
        String lastName = "";
        if (fullName != null && fullName.contains(" ")) {
            int spaceIndex = fullName.lastIndexOf(' ');
            firstName = fullName.substring(0, spaceIndex);
            lastName = fullName.substring(spaceIndex + 1);
        }
        
        // Simple password hash (in production, use bcrypt)
        String passwordHash = hashPassword(password);
        
        if (DBConnection.emailExists(email)) {
            sendErrorResponse(exchange, 409, "Email already registered");
            return;
        }
        
        int userId = DBConnection.registerUser(email, passwordHash, firstName, lastName, phone, "applicant");
        if (userId > 0) {
            // Generate JWT token
            String token = JWTUtil.generateToken(userId, email, "applicant", phone);
            DBConnection.saveToken(token, userId);
            
            String json = "{\"success\": true, \"token\": \"" + token + "\", \"user\": {\"id\": " + userId + 
                         ", \"email\": \"" + email + "\", \"full_name\": \"" + fullName + "\", \"role\": \"applicant\"}}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to register user");
        }
    }
    
    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String email = params.get("email");
        String password = params.get("password");
        
        if (email == null || password == null) {
            System.err.println("[LOGIN DEBUG] Missing credentials - email: " + email + ", password: " + (password != null ? "***" : "null"));
            sendErrorResponse(exchange, 400, "Missing credentials");
            return;
        }
        
        // Debug: Check if user exists
        boolean userExists = DBConnection.emailExists(email);
        System.err.println("[LOGIN DEBUG] Email: " + email + ", User exists: " + userExists);
        
        // Pass plain password - DBConnection will hash it
        Map<String, Object> user = DBConnection.authenticateUser(email, password);
        
        if (user != null) {
            int userId = (Integer) user.get("id");
            String role = (String) user.get("role");
            String fullName = (String) user.get("full_name");
            String phone = (String) user.get("phone");
            
            Object locationId = user.get("location_id");
            String locationIdStr = locationId != null ? String.valueOf(locationId) : null;
            
            String token = JWTUtil.generateToken(userId, email, role, phone, locationIdStr);
            DBConnection.saveToken(token, userId);
            
            System.err.println("[LOGIN DEBUG] Success for user: " + email + ", role: " + role);
            
            String trainingLocation = "";
            if (locationId != null) {
                try {
                    int locId = Integer.parseInt(String.valueOf(locationId));
                    Map<String, Object> loc = DBConnection.getLocationById(locId);
                    if (loc != null && loc.get("name") != null) {
                        trainingLocation = (String) loc.get("name");
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            String json = "{\"success\": true, \"token\": \"" + token + "\", \"user\": {\"id\": " + userId + 
                         ", \"email\": \"" + escapeJson(email) + "\", \"full_name\": \"" + escapeJson(fullName) + "\", \"phone\": \"" + escapeJson(phone != null ? phone : "") + "\", \"role\": \"" + escapeJson(role) + "\", \"location_id\": " + (locationId != null ? locationId : "null") + ", \"training_location\": \"" + escapeJson(trainingLocation) + "\"}}";
            sendJsonResponse(exchange, 200, json);
        } else {
            System.err.println("[LOGIN DEBUG] Invalid credentials for: " + email);
            // Debug: Get stored hash to compare
            Map<String, Object> storedUser = DBConnection.getUserByEmail(email);
            if (storedUser != null) {
                String storedHash = (String) storedUser.get("password_hash");
                String inputHash = DBConnection.hashPassword(password);
                System.err.println("[LOGIN DEBUG] Stored hash: " + storedHash);
                System.err.println("[LOGIN DEBUG] Input hash: " + inputHash);
                System.err.println("[LOGIN DEBUG] Hash match: " + storedHash.equals(inputHash));
            }
            sendErrorResponse(exchange, 401, "Invalid credentials");
        }
    }
    
    /**
     * Create admin user endpoint
     */
    private void handleCreateAdmin(HttpExchange exchange) throws IOException {
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String email = params.get("email");
        String password = params.get("password");
        String fullName = params.get("full_name");
        String phone = params.get("phone");
        String adminKey = params.get("admin_key");
        
        // Admin key to prevent unauthorized admin creation
        if (adminKey == null || !adminKey.equals("udds_admin_secret_2024")) {
            sendErrorResponse(exchange, 403, "Invalid admin key");
            return;
        }
        
        if (email == null || password == null || fullName == null) {
            sendErrorResponse(exchange, 400, "Missing required fields");
            return;
        }
        
        // Split full name into first and last
        String firstName = fullName;
        String lastName = "";
        if (fullName != null && fullName.contains(" ")) {
            int spaceIndex = fullName.lastIndexOf(' ');
            firstName = fullName.substring(0, spaceIndex);
            lastName = fullName.substring(spaceIndex + 1);
        }
        
        String passwordHash = hashPassword(password);
        
        if (DBConnection.emailExists(email)) {
            sendErrorResponse(exchange, 409, "Email already registered");
            return;
        }
        
        int userId = DBConnection.registerUser(email, passwordHash, firstName, lastName, phone, "admin");
        if (userId > 0) {
            String json = "{\"success\": true, \"message\": \"Admin user created successfully\", \"user_id\": " + userId + "}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to create admin user");
        }
    }
    
    /**
     * Reset password endpoint (for admin recovery)
     */
    private void handleResetPassword(HttpExchange exchange) throws IOException {
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String email = params.get("email");
        String newPassword = params.get("new_password");
        String resetKey = params.get("reset_key");
        
        // Secret key for password reset
        if (resetKey == null || !resetKey.equals("udds_reset_2024")) {
            sendErrorResponse(exchange, 403, "Invalid reset key");
            return;
        }
        
        if (email == null || newPassword == null) {
            sendErrorResponse(exchange, 400, "Missing email or new password");
            return;
        }
        
        String passwordHash = hashPassword(newPassword);
        boolean updated = DBConnection.updateUserProfile(-1, null, null, email, null, passwordHash);
        
        if (updated) {
            sendSuccessResponse(exchange, "Password reset successfully");
        } else {
            sendErrorResponse(exchange, 404, "User not found");
        }
    }
    
    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token != null) {
            DBConnection.revokeToken(token);
        }
        sendSuccessResponse(exchange, "Logged out successfully");
    }
    
    private void handleGetCurrentUser(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "No token provided");
            return;
        }
        
        // Validate JWT signature and expiration
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }
        
        // Token is valid - proceed with authentication
        int userId = JWTUtil.getUserId(token);
        Map<String, Object> user = DBConnection.getUserById(userId);
        
        if (user != null) {
            String json = "{\"success\": true, \"user\": {\"id\": " + user.get("id") + 
                         ", \"email\": \"" + escapeJson(user.get("email")) + "\"," + 
                         ", \"full_name\": \"" + escapeJson(user.get("full_name")) + "\"," + 
                         ", \"phone\": \"" + escapeJson(user.get("phone")) + "\"," + 
                         ", \"role\": \"" + escapeJson(user.get("role")) + "\"," + 
                         ", \"location\": \"" + escapeJson(user.get("location")) + "\"}}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 404, "User not found");
        }
    }
    
    /**
     * Refresh token endpoint
     */
    private void handleRefreshToken(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "No token provided");
            return;
        }
        
        // Validate the current token (allow refresh even if not in database for backward compatibility)
        Map<String, Object> payload = JWTUtil.validateToken(token);
        if (payload == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        // Get user info from token
        int userId = JWTUtil.getUserId(token);
        String email = JWTUtil.getEmail(token);
        String role = JWTUtil.getRole(token);
        
        if (userId < 0 || email == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        // Generate new token
        String newToken = JWTUtil.generateToken(userId, email, role);
        DBConnection.saveToken(newToken, userId);
        
        String json = "{\"success\": true, \"token\": \"" + newToken + "\"}";
        sendJsonResponse(exchange, 200, json);
    }
    
    private String hashPassword(String password) {
        // Use DBConnection's hashPassword for consistency
        return DBConnection.hashPassword(password);
    }
    
    private static java.security.MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return java.security.MessageDigest.getInstance("SHA-256");
    }
}

/**
 * Application Handler
 */
class ApplicationHandler extends ApiHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        try {
            if (path.equals("/api/applications") && "POST".equals(method)) {
                handleSubmitApplication(exchange);
            } else if (path.equals("/api/applications") && "GET".equals(method)) {
                handleGetMyApplications(exchange);
            } else if (path.equals("/api/applications/mpesa-message") && "POST".equals(method)) {
                handleSubmitMpesaMessage(exchange);
            } else if (path.equals("/api/applications/mpesa-messages") && "GET".equals(method)) {
                handleGetMpesaMessages(exchange);
            } else if (path.startsWith("/api/applications/") && path.contains("/mpesa-messages") && "GET".equals(method)) {
                // GET /api/applications/{id}/mpesa-messages - Get mpesa messages for specific application
                String idStr = path.replace("/api/applications/", "").replace("/mpesa-messages", "");
                try {
                    int appId = Integer.parseInt(idStr);
                    handleGetMpesaMessagesByAppId(exchange, appId);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid application ID");
                }
            } else if (path.startsWith("/api/applications/") && "GET".equals(method)) {
                String idStr = path.substring("/api/applications/".length());
                if (idStr.equals("all")) {
                    handleGetAllApplications(exchange);
                } else {
                    handleGetApplication(exchange, Integer.parseInt(idStr));
                }
            } else if (path.startsWith("/api/applications/") && path.endsWith("/status") && "PUT".equals(method)) {
                String idStr = path.substring("/api/applications/".length(), path.length() - "/status".length());
                handleUpdateStatus(exchange, Integer.parseInt(idStr));
            } else if (path.startsWith("/api/applications/") && "PUT".equals(method)) {
                String idStr = path.substring("/api/applications/".length());
                // Check for query parameters - extract ID first
                if (idStr.contains("?")) {
                    idStr = idStr.substring(0, idStr.indexOf("?"));
                }
                String query = exchange.getRequestURI().getQuery() != null ? exchange.getRequestURI().getQuery() : "";
                if (query.contains("action=fees")) {
                    handleUpdateApplicationFees(exchange, Integer.parseInt(idStr));
                } else {
                    handleUpdateApplication(exchange, Integer.parseInt(idStr));
                }
            } else if (path.startsWith("/api/applications/") && "DELETE".equals(method)) {
                String idStr = path.substring("/api/applications/".length());
                // Check for query parameters
                if (idStr.contains("?")) {
                    idStr = idStr.substring(0, idStr.indexOf("?"));
                }
                handleDeleteApplication(exchange, Integer.parseInt(idStr));
            } else {
                sendErrorResponse(exchange, 404, "Endpoint not found");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleSubmitApplication(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        Map<String, Object> tokenValidation = JWTUtil.validateToken(token);
        
        if (tokenValidation == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }
        
        int userId = JWTUtil.getUserId(token);
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        Map<String, Object> applicationData = new HashMap<>();
        applicationData.put("user_id", userId);
        applicationData.put("first_name", params.get("first_name"));
        applicationData.put("last_name", params.get("last_name"));
        applicationData.put("email", params.get("email"));
        applicationData.put("phone", params.get("phone"));
        applicationData.put("date_of_birth", params.get("date_of_birth"));
        applicationData.put("address", params.get("address"));
        applicationData.put("city", params.get("city"));
        applicationData.put("postal_code", params.get("postal_code"));
        applicationData.put("id_number", params.get("id_number"));
        applicationData.put("license_type", params.get("license_type"));
        applicationData.put("driving_course", params.get("driving_course"));
        applicationData.put("computer_course", params.get("computer_course"));
        
        // Handle location_id - use directly if provided, otherwise look up
        if (params.get("location_id") != null && !params.get("location_id").isEmpty()) {
            try {
                applicationData.put("location_id", Integer.parseInt(params.get("location_id")));
            } catch (NumberFormatException e) {
                // Try to look up by name
                Integer locationId = DBConnection.getLocationIdByName(params.get("location_id"));
                applicationData.put("location_id", locationId);
            }
        }
        
        // Handle training_location - use directly if provided
        if (params.get("training_location") != null && !params.get("training_location").isEmpty()) {
            applicationData.put("training_location", params.get("training_location"));
        }
        
        // Look up course_id based on driving_course name (if not already set)
        String drivingCourse = params.get("driving_course");
        if (drivingCourse != null && !drivingCourse.isEmpty() && applicationData.get("course_id") == null) {
            Integer courseId = DBConnection.getCourseIdByName(drivingCourse);
            applicationData.put("course_id", courseId);
        }
        
        applicationData.put("preferred_schedule", params.get("preferred_schedule"));
        applicationData.put("preferred_start", params.get("preferred_start"));
        applicationData.put("emergency_contact_name", params.get("emergency_contact_name"));
        applicationData.put("emergency_contact_phone", params.get("emergency_contact_phone"));
        applicationData.put("emergency_contact_relation", params.get("emergency_contact_relation"));
        applicationData.put("comments", params.get("comments"));
        applicationData.put("transmission", params.get("transmission"));
        applicationData.put("medical_conditions", params.get("medical_conditions"));
        applicationData.put("previous_driving_experience", params.containsKey("previous_driving_experience"));
        
        // Calculate fees manually based on selected courses
        String computerCourse = params.get("computer_course");
        double drivingFees = drivingCourse != null && !drivingCourse.isEmpty() ? 15000.0 : 0.0;
        double computerFees = computerCourse != null && !computerCourse.isEmpty() ? 5000.0 : 0.0;
        double totalFees = drivingFees + computerFees;
        applicationData.put("school_fees", totalFees);
        applicationData.put("fees_paid", 0);
        applicationData.put("fees_balance", totalFees);
        applicationData.put("payment_status", "unpaid");
        applicationData.put("payment_method", "");
        applicationData.put("status", "pending");
        
        int applicationId = DBConnection.submitApplication(applicationData);
        
        if (applicationId > 0) {
            // Also update user's training_location
            String userTrainingLocation = params.get("training_location");
            if (userTrainingLocation != null && !userTrainingLocation.isEmpty()) {
                DBConnection.updateStaffLocation(userId, userTrainingLocation);
            }
            String json = "{\"success\": true, \"message\": \"Application submitted successfully\", \"application_id\": " + applicationId + "}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to submit application");
        }
    }
    
    private void handleGetMyApplications(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.isTokenExpired(token)) {
            sendErrorResponse(exchange, 401, "Token expired");
            return;
        }
        
        int userId = JWTUtil.getUserId(token);
        List<Map<String, Object>> applications = DBConnection.getApplicationsDataByUserId(userId);
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"applications\": [");
        for (int i = 0; i < applications.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(applications.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
    
    private void handleGetApplication(HttpExchange exchange, int id) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        int userId = JWTUtil.getUserId(token);
        
        // Staff | instuctor can view applications in their assigned location
        if ("staff".equals(role)) {
            Map<String, Object> app = DBConnection.getApplicationById(id);
            if (app != null) {
                String staffLocation = DBConnection.getUserLocation(userId);
                String appLocation = (String) app.get("training_location");
                // Allow if staff has no location assigned OR locations match OR app has no location
                if (staffLocation == null || staffLocation.isEmpty() || 
                    staffLocation.equals(appLocation) || appLocation == null || appLocation.isEmpty()) {
                    String json = "{\"success\": true, \"application\": " + mapToJson(app) + "}";
                    sendJsonResponse(exchange, 200, json);
                } else {
                    sendErrorResponse(exchange, 403, "Access denied. You can only view applications in your assigned location.");
                }
            } else {
                sendErrorResponse(exchange, 404, "Application not found");
            }
            return;
        }
        
        // Only admin or owner can view application
        if (!"admin".equals(role)) {
            List<Map<String, Object>> myApps = DBConnection.getApplicationsDataByUserId(userId);
            boolean found = myApps.stream().anyMatch(app -> app.get("id").equals(id));
            if (!found) {
                sendErrorResponse(exchange, 403, "Access denied");
                return;
            }
        }
        
        // Use the new method that returns all fields
        Map<String, Object> app = DBConnection.getApplicationById(id);
        
        if (app != null) {
            String json = "{\"success\": true, \"application\": " + mapToJson(app) + "}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 404, "Application not found");
        }
    }
    
    private void handleGetAllApplications(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        List<Map<String, Object>> applications = DBConnection.getAllApplications();
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"applications\": [");
        for (int i = 0; i < applications.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(applications.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
    
    private void handleUpdateStatus(HttpExchange exchange, int id) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        String status = params.get("status");
        
        if (status == null) {
            sendErrorResponse(exchange, 400, "Status required");
            return;
        }
        
        if (DBConnection.updateApplicationStatus(id, status)) {
            sendSuccessResponse(exchange, "Status updated");
        } else {
            sendErrorResponse(exchange, 500, "Failed to update status");
        }
    }
    
    private void handleUpdateApplication(HttpExchange exchange, int id) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        int userId = JWTUtil.getUserId(token);
        
        // Check if user owns this application or is admin - use correct method
        List<Map<String, Object>> userApps = DBConnection.getApplicationsDataByUserId(userId);
        boolean ownsApp = userApps.stream().anyMatch(app -> app.get("id").equals(id));
        
        if (!ownsApp && !"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Access denied");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        // Admins can update status and all fields, users can only update non-status fields
        String status = params.get("status");
        if (status != null && !"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Only admins can update application status");
            return;
        }
        
        Map<String, Object> applicationData = new HashMap<>();
        applicationData.put("id", id);
        applicationData.put("first_name", params.get("first_name"));
        applicationData.put("last_name", params.get("last_name"));
        applicationData.put("email", params.get("email"));
        applicationData.put("phone", params.get("phone"));
        // Only add date_of_birth if it has a value (to avoid CAST empty string error)
        String dateOfBirth = params.get("date_of_birth");
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
            applicationData.put("date_of_birth", dateOfBirth);
        }
        applicationData.put("address", params.get("address"));
        applicationData.put("city", params.get("city"));
        applicationData.put("postal_code", params.get("postal_code"));
        applicationData.put("id_number", params.get("id_number"));
        applicationData.put("license_type", params.get("license_type"));
        applicationData.put("driving_course", params.get("driving_course"));
        applicationData.put("computer_course", params.get("computer_course"));
        // Handle location_id (numeric) and training_location (text)
        if (params.get("location_id") != null && !params.get("location_id").isEmpty()) {
            applicationData.put("location_id", Integer.parseInt(params.get("location_id")));
        }
        if (params.get("training_location") != null && !params.get("training_location").isEmpty()) {
            applicationData.put("training_location", params.get("training_location"));
        }
        applicationData.put("transmission", params.get("transmission"));
        // Add preferred_start for date field
        String preferredStart = params.get("preferred_start");
        if (preferredStart != null && !preferredStart.isEmpty()) {
            applicationData.put("preferred_start", preferredStart);
        }
        applicationData.put("preferred_schedule", params.get("preferred_schedule"));
        applicationData.put("emergency_contact_name", params.get("emergency_contact_name"));
        applicationData.put("emergency_contact_phone", params.get("emergency_contact_phone"));
        applicationData.put("emergency_contact_relation", params.get("emergency_contact_relation"));
        applicationData.put("comments", params.get("comments"));
        applicationData.put("medical_conditions", params.get("medical_conditions"));
        // Only include fees fields if they are provided (not null/empty)
        if (params.get("school_fees") != null && !params.get("school_fees").isEmpty()) {
            applicationData.put("school_fees", Double.parseDouble(params.get("school_fees")));
        }
        if (params.get("fees_paid") != null && !params.get("fees_paid").isEmpty()) {
            applicationData.put("fees_paid", Double.parseDouble(params.get("fees_paid")));
        }
        if (params.get("fees_balance") != null && !params.get("fees_balance").isEmpty()) {
            applicationData.put("fees_balance", Double.parseDouble(params.get("fees_balance")));
        }
        if (params.get("payment_status") != null && !params.get("payment_status").isEmpty()) {
            applicationData.put("payment_status", params.get("payment_status"));
        }
        if (params.get("payment_method") != null && !params.get("payment_method").isEmpty()) {
            applicationData.put("payment_method", params.get("payment_method"));
        }
        if (params.containsKey("previous_driving_experience")) {
            applicationData.put("previous_driving_experience", params.get("previous_driving_experience").equals("true") || params.get("previous_driving_experience").equals("on"));
        }
        if (status != null) {
            applicationData.put("status", status);
        }
        
        if (DBConnection.updateApplication(applicationData)) {
            sendSuccessResponse(exchange, "Application updated successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to update application");
        }
    }
    
    // Handle fees-only update for applications (admin only)
    private void handleUpdateApplicationFees(HttpExchange exchange, int id) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Only admins can update application fees");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String schoolFees = params.get("school_fees");
        String feesPaid = params.get("fees_paid");
        String feesBalance = params.get("fees_balance");
        String paymentStatus = params.get("payment_status");
        String paymentMethod = params.get("payment_method");
        
        boolean success = DBConnection.updateApplicationFees(
            id, 
            schoolFees, 
            feesPaid, 
            feesBalance, 
            paymentStatus, 
            paymentMethod
        );
        
        if (success) {
            sendSuccessResponse(exchange, "Application fees updated successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to update application fees");
        }
    }
    
    // Handle delete application (admin only)
    private void handleDeleteApplication(HttpExchange exchange, int applicationId) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        if (DBConnection.deleteApplication(applicationId)) {
            sendSuccessResponse(exchange, "Application deleted successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete application");
        }
    }
    
    // ============ MPESA MESSAGE HANDLERS ============
    
    private void handleSubmitMpesaMessage(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.isTokenExpired(token)) {
            sendErrorResponse(exchange, 401, "Token expired");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String applicationId = params.get("application_id");
        String message = params.get("message");
        String phone = params.get("phone");
        String amount = params.get("amount");
        String mpesaCode = params.get("mpesa_code");
        
        if (applicationId == null || message == null || message.trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Application ID and message are required");
            return;
        }
        
        try {
            int appId = Integer.parseInt(applicationId);
            double amt = amount != null ? Double.parseDouble(amount) : 0;
            int msgId = DBConnection.addMpesaMessage(appId, message, phone, amt, mpesaCode);
            
            if (msgId > 0) {
                String json = "{\"success\": true, \"message\": \"Mpesa message submitted successfully\", \"id\": " + msgId + "}";
                sendJsonResponse(exchange, 201, json);
            } else {
                sendErrorResponse(exchange, 500, "Failed to submit Mpesa message");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid application ID");
        }
    }
    
    private void handleGetMpesaMessages(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.isTokenExpired(token)) {
            sendErrorResponse(exchange, 401, "Token expired");
            return;
        }
        
        int userId = JWTUtil.getUserId(token);
        java.util.List<Map<String, Object>> messages = DBConnection.getMpesaMessagesByUserId(userId);
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"messages\": [");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(messages.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
    
    private void handleGetMpesaMessagesByAppId(HttpExchange exchange, int appId) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.isTokenExpired(token)) {
            sendErrorResponse(exchange, 401, "Token expired");
            return;
        }
        
        // Check if admin or staff for access
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role) && !"staff".equals(role) && !"instructor".equals(role)) {
            // Users can only access their own messages
            int userId = JWTUtil.getUserId(token);
            java.util.List<Map<String, Object>> userApps = DBConnection.getApplicationsByUserId(userId);
            boolean ownsApp = userApps.stream().anyMatch(app -> app.get("id").equals(appId));
            if (!ownsApp) {
                sendErrorResponse(exchange, 403, "Access denied");
                return;
            }
        }
        
        java.util.List<Map<String, Object>> messages = DBConnection.getMpesaMessagesByAppId(appId);
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"messages\": [");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(messages.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
}

/**
 * Course Handler
 */
class CourseHandler extends ApiHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        try {
            if (path.equals("/api/courses") && "GET".equals(method)) {
                handleGetCourses(exchange);
            } else if (path.equals("/api/courses") && "POST".equals(method)) {
                handleCreateCourse(exchange);
            } else if (path.startsWith("/api/courses/") && "GET".equals(method)) {
                String idStr = path.substring("/api/courses/".length());
                handleGetCourse(exchange, Integer.parseInt(idStr));
            } else if (path.startsWith("/api/courses/") && "PUT".equals(method)) {
                String idStr = path.substring("/api/courses/".length());
                handleUpdateCourse(exchange, Integer.parseInt(idStr));
            } else if (path.startsWith("/api/courses/") && "DELETE".equals(method)) {
                String idStr = path.substring("/api/courses/".length());
                handleDeleteCourse(exchange, Integer.parseInt(idStr));
            } else {
                sendErrorResponse(exchange, 404, "Endpoint not found");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleGetCourses(HttpExchange exchange) throws IOException {
        // Check for category parameter
        String query = exchange.getRequestURI().getQuery();
        String category = null;
        String categories = null; // For multiple categories (comma-separated)
        
        if (query != null && query.contains("category=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("category=")) {
                    category = param.substring("category=".length());
                    category = java.net.URLDecoder.decode(category, "UTF-8");
                    break;
                }
            }
        }
        
        // Check for categories parameter (comma-separated)
        if (query != null && query.contains("categories=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("categories=")) {
                    categories = param.substring("categories=".length());
                    categories = java.net.URLDecoder.decode(categories, "UTF-8");
                    break;
                }
            }
        }
        
        List<Map<String, Object>> courses;
        if (categories != null && !categories.isEmpty()) {
            // Split comma-separated categories
            String[] categoryList = categories.split(",");
            courses = DBConnection.getCoursesByCategories(categoryList);
        } else if (category != null && !category.isEmpty()) {
            courses = DBConnection.getCoursesByCategory(category);
        } else {
            courses = DBConnection.getAllCourses();
        }
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"courses\": [");
        for (int i = 0; i < courses.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(courses.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
    
    private void handleGetCourse(HttpExchange exchange, int id) throws IOException {
        Map<String, Object> course = DBConnection.getCourseById(id);
        if (course != null) {
            String json = "{\"success\": true, \"course\": " + mapToJson(course) + "}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 404, "Course not found");
        }
    }
    
    private void handleCreateCourse(HttpExchange exchange) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String name = params.get("name");
        String description = params.get("description");
        String duration = params.get("duration");
        double price = Double.parseDouble(params.get("price"));
        String requirements = params.get("requirements");
        
        int courseId = DBConnection.createCourse(name, description, duration, price, requirements);
        if (courseId > 0) {
            String json = "{\"success\": true, \"message\": \"Course created successfully\", \"course_id\": " + courseId + "}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to create course");
        }
    }
    
    private void handleUpdateCourse(HttpExchange exchange, int courseId) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String name = params.get("name");
        String description = params.get("description");
        String duration = params.get("duration");
        double price = Double.parseDouble(params.get("price"));
        String requirements = params.get("requirements");
        
        if (DBConnection.updateCourse(courseId, name, description, price)) {
            sendSuccessResponse(exchange, "Course updated successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to update course");
        }
    }
    
    private void handleDeleteCourse(HttpExchange exchange, int courseId) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        if (DBConnection.deleteCourse(courseId)) {
            sendSuccessResponse(exchange, "Course deleted successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete course");
        }
    }
}

/**
 * Class Handler for Class Management
 */
class ClassHandler extends ApiHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        try {
            if (path.equals("/api/classes") && "GET".equals(method)) {
                handleGetClasses(exchange);
            } else if (path.equals("/api/classes") && "POST".equals(method)) {
                handleCreateClass(exchange);
            } else if (path.startsWith("/api/classes/") && "GET".equals(method)) {
                String idStr = path.substring("/api/classes/".length());
                handleGetClass(exchange, Integer.parseInt(idStr));
            } else if (path.startsWith("/api/classes/") && "PUT".equals(method)) {
                String idStr = path.substring("/api/classes/".length());
                handleUpdateClass(exchange, Integer.parseInt(idStr));
            } else if (path.startsWith("/api/classes/") && "DELETE".equals(method)) {
                String idStr = path.substring("/api/classes/".length());
                handleDeleteClass(exchange, Integer.parseInt(idStr));
            } else {
                sendErrorResponse(exchange, 404, "Endpoint not found");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleGetClasses(HttpExchange exchange) throws IOException {
        // Check for location parameter
        String query = exchange.getRequestURI().getQuery();
        String location = null;
        if (query != null && query.contains("location=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("location=")) {
                    location = param.substring("location=".length());
                    break;
                }
            }
        }
        
        List<Map<String, Object>> classes;
        if (location != null && !location.isEmpty()) {
            classes = DBConnection.getClassesByLocation(location);
        } else {
            classes = DBConnection.getAllClasses();
        }
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"classes\": [");
        for (int i = 0; i < classes.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(classes.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
    
    private void handleGetClass(HttpExchange exchange, int id) throws IOException {
        Map<String, Object> classObj = DBConnection.getClassById(id);
        if (classObj != null) {
            String json = "{\"success\": true, \"class\": " + mapToJson(classObj) + "}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 404, "Class not found");
        }
    }
    
    private void handleCreateClass(HttpExchange exchange) throws IOException {
        // Check admin/staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role) && !"staff".equals(role) && !"instructor".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin or Staff access required");
            return;
        }
        
        try {
            String body = getRequestBody(exchange);
            Map<String, String> params = parseFormData(body);
            
            // Parse required fields
            int courseId = 0;
            int locationId = 0;
            try {
                courseId = params.get("course_id") != null && !params.get("course_id").isEmpty() ? 
                    Integer.parseInt(params.get("course_id")) : 0;
            } catch (NumberFormatException e) {
                courseId = 0;
            }
            try {
                locationId = params.get("location_id") != null && !params.get("location_id").isEmpty() ? 
                    Integer.parseInt(params.get("location_id")) : 0;
            } catch (NumberFormatException e) {
                locationId = 0;
            }
            
            String name = params.get("name");
            String code = params.get("code");
            String startDate = params.get("start_date");
            String endDate = params.get("end_date");
            String startTime = params.get("start_time");
            String endTime = params.get("end_time");
            String daysOfWeek = params.get("days_of_week");
            
            Integer instructorId = 0;
            try {
                instructorId = params.get("instructor_id") != null && !params.get("instructor_id").isEmpty() ? 
                    Integer.parseInt(params.get("instructor_id")) : 0;
            } catch (NumberFormatException e) {
                instructorId = 0;
            }
            
            int maxStudents = 30;
            try {
                maxStudents = params.get("max_students") != null && !params.get("max_students").isEmpty() ? 
                    Integer.parseInt(params.get("max_students")) : 30;
            } catch (NumberFormatException e) {
                maxStudents = 30;
            }
            
            int currentStudents = 0;
            try {
                currentStudents = params.get("current_students") != null && !params.get("current_students").isEmpty() ? 
                    Integer.parseInt(params.get("current_students")) : 0;
            } catch (NumberFormatException e) {
                currentStudents = 0;
            }
            
            double classFee = 0;
            try {
                classFee = params.get("class_fee") != null && !params.get("class_fee").isEmpty() ? 
                    Double.parseDouble(params.get("class_fee")) : 0;
            } catch (NumberFormatException e) {
                classFee = 0;
            }
            
            int classId = DBConnection.createClass(courseId, name, code, locationId, startDate, endDate, 
                startTime, endTime, daysOfWeek, instructorId, maxStudents, currentStudents, classFee, 
                params.get("description"), params.get("status"));
            
            if (classId > 0) {
                String json = "{\"success\": true, \"message\": \"Class created successfully\", \"class_id\": " + classId + "}";
                sendJsonResponse(exchange, 201, json);
            } else {
                sendErrorResponse(exchange, 500, "Failed to create class");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Error: " + e.getMessage());
        }
    }
    
    private void handleUpdateClass(HttpExchange exchange, int classId) throws IOException {
        // Check admin/staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role) && !"staff".equals(role) && !"instructor".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin or Staff access required");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String name = params.get("name");
        String code = params.get("code");
        String status = params.get("status");
        
        // Parse all fields for full update
        int courseId = 0;
        int locationId = 0;
        int instructorId = 0;
        int maxStudents = 30;
        double classFee = 0;
        
        try {
            courseId = params.get("course_id") != null && !params.get("course_id").isEmpty() ? 
                Integer.parseInt(params.get("course_id")) : 0;
        } catch (NumberFormatException e) {
            courseId = 0;
        }
        try {
            locationId = params.get("location_id") != null && !params.get("location_id").isEmpty() ? 
                Integer.parseInt(params.get("location_id")) : 0;
        } catch (NumberFormatException e) {
            locationId = 0;
        }
        try {
            instructorId = params.get("instructor_id") != null && !params.get("instructor_id").isEmpty() ? 
                Integer.parseInt(params.get("instructor_id")) : 0;
        } catch (NumberFormatException e) {
            instructorId = 0;
        }
        try {
            maxStudents = params.get("max_students") != null && !params.get("max_students").isEmpty() ? 
                Integer.parseInt(params.get("max_students")) : 30;
        } catch (NumberFormatException e) {
            maxStudents = 30;
        }
        try {
            classFee = params.get("class_fee") != null && !params.get("class_fee").isEmpty() ? 
                Double.parseDouble(params.get("class_fee")) : 0;
        } catch (NumberFormatException e) {
            classFee = 0;
        }
        
        String startDate = params.get("start_date");
        String endDate = params.get("end_date");
        String startTime = params.get("start_time");
        String endTime = params.get("end_time");
        String daysOfWeek = params.get("days_of_week");
        String description = params.get("description");
        
        // Use DBConnection.updateClass to update all fields
        boolean success = DBConnection.updateClass(classId, courseId, name, code, locationId, 
            startDate, endDate, startTime, endTime, daysOfWeek, instructorId, maxStudents, 
            classFee, description, status);
        
        if (success) {
            sendSuccessResponse(exchange, "Class updated successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to update class");
        }
    }
    
    private void handleDeleteClass(HttpExchange exchange, int classId) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        // Soft delete by setting status to cancelled
        if (DBConnection.updateClassStatus(classId, "cancelled")) {
            sendSuccessResponse(exchange, "Class cancelled successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete class");
        }
    }
}

/**
 * Department Handler for Department Management
 */
class DepartmentHandler extends ApiHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        try {
            // Check authentication for departments
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
            if (!"admin".equals(role) && !"staff".equals(role) && !"instructor".equals(role)) {
                sendErrorResponse(exchange, 403, "Admin or staff access required");
                return;
            }
            
            if (path.equals("/api/departments") && "GET".equals(method)) {
                handleGetDepartments(exchange);
            } else if (path.equals("/api/departments") && "POST".equals(method)) {
                handleCreateDepartment(exchange);
            } else if (path.startsWith("/api/departments/") && "GET".equals(method)) {
                String idStr = path.substring("/api/departments/".length());
                handleGetDepartment(exchange, Integer.parseInt(idStr));
            } else if (path.startsWith("/api/departments/") && "PUT".equals(method)) {
                String idStr = path.substring("/api/departments/".length());
                handleUpdateDepartment(exchange, Integer.parseInt(idStr));
            } else if (path.startsWith("/api/departments/") && "DELETE".equals(method)) {
                String idStr = path.substring("/api/departments/".length());
                handleDeleteDepartment(exchange, Integer.parseInt(idStr));
            } else {
                sendErrorResponse(exchange, 404, "Endpoint not found");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleGetDepartments(HttpExchange exchange) throws IOException {
        // Check for location parameter
        String query = exchange.getRequestURI().getQuery();
        String location = null;
        if (query != null && query.contains("location=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("location=")) {
                    location = param.substring("location=".length());
                    break;
                }
            }
        }
        
        List<Map<String, Object>> departments;
        if (location != null && !location.isEmpty()) {
            departments = DBConnection.getDepartmentsByLocation(location);
        } else {
            departments = DBConnection.getAllDepartments();
        }
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"departments\": [");
        for (int i = 0; i < departments.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(departments.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
    
    private void handleGetDepartment(HttpExchange exchange, int id) throws IOException {
        Map<String, Object> dept = DBConnection.getDepartmentById(id);
        if (dept != null) {
            String json = "{\"success\": true, \"department\": " + mapToJson(dept) + "}";
            sendJsonResponse(exchange, 200, json);
        } else {
            sendErrorResponse(exchange, 404, "Department not found");
        }
    }
    
    private void handleCreateDepartment(HttpExchange exchange) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String name = params.get("name");
        String description = params.get("description");
        Integer headId = params.get("head_id") != null && !params.get("head_id").isEmpty() ? 
            Integer.parseInt(params.get("head_id")) : null;
        Integer locationId = params.get("location_id") != null && !params.get("location_id").isEmpty() ? 
            Integer.parseInt(params.get("location_id")) : null;
        
        int deptId = DBConnection.createDepartment(name, description, headId, locationId);
        
        if (deptId > 0) {
            String json = "{\"success\": true, \"message\": \"Department created successfully\", \"department_id\": " + deptId + "}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to create department");
        }
    }
    
    private void handleUpdateDepartment(HttpExchange exchange, int deptId) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String name = params.get("name");
        String description = params.get("description");
        
        if (DBConnection.updateDepartment(deptId, name, description)) {
            sendSuccessResponse(exchange, "Department updated successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to update department");
        }
    }
    
    private void handleDeleteDepartment(HttpExchange exchange, int deptId) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        if (DBConnection.deleteDepartment(deptId)) {
            sendSuccessResponse(exchange, "Department deleted successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete department");
        }
    }
}

/**
 * User Handler for Admin User Management
 */
class UserHandler extends ApiHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        try {
            // User profile endpoints (for students to view/update their own profile)
            if (path.equals("/api/user/profile") && "GET".equals(method)) {
                handleGetCurrentUserProfile(exchange);
            } else if (path.equals("/api/user/update") && "POST".equals(method)) {
                handleUpdateCurrentUserProfile(exchange);
            } else if (path.equals("/api/user/update-login") && "POST".equals(method)) {
                handleUpdateLoginDetails(exchange);
            } else if (path.equals("/api/users") && "GET".equals(method)) {
                handleGetUsers(exchange);
            } else if (path.matches("/api/users/\\d+") && "GET".equals(method)) {
                String idStr = path.substring("/api/users/".length());
                handleGetUser(exchange, Integer.parseInt(idStr));
            } else if (path.startsWith("/api/users/") && "PUT".equals(method)) {
                String idStr = path.substring("/api/users/".length());
                // Handle query parameters
                if (idStr.contains("?")) {
                    idStr = idStr.substring(0, idStr.indexOf("?"));
                }
                // Check if this is a fees update - use getQuery() to get the query string
                String query = exchange.getRequestURI().getQuery() != null ? exchange.getRequestURI().getQuery() : "";
                if (query.contains("action=fees")) {
                    handleUpdateUserFees(exchange, Integer.parseInt(idStr));
                } else {
                    handleUpdateUserRole(exchange, Integer.parseInt(idStr));
                }
            } else if (path.startsWith("/api/users/") && "DELETE".equals(method)) {
                String idStr = path.substring("/api/users/".length());
                // Handle query parameters
                if (idStr.contains("?")) {
                    idStr = idStr.substring(0, idStr.indexOf("?"));
                }
                System.err.println("[ERROR] DELETE /api/users/" + idStr);
                handleDeleteUser(exchange, Integer.parseInt(idStr));
            } else {
                sendErrorResponse(exchange, 404, "Endpoint not found");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleGetUsers(HttpExchange exchange) throws IOException {
        // Check admin or staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        // Allow admin, staff, and instructor roles to access user list
        if (!"admin".equals(role) && !"staff".equals(role) && !"instructor".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin, staff, or instructor access required");
            return;
        }
        
        // Check for role filter query parameter
        String query = exchange.getRequestURI().getQuery();
        String roleFilter = null;
        if (query != null && query.contains("role=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("role=")) {
                    roleFilter = param.substring(5);
                    break;
                }
            }
        }

        List<Map<String, Object>> users = DBConnection.getAllUsers();

        // Populate training_location from location_id if not set
        for (Map<String, Object> user : users) {
            if (user.get("training_location") == null) {
                Object locIdObj = user.get("location_id");
                if (locIdObj != null) {
                    try {
                        int locationId;
                        if (locIdObj instanceof Number) {
                            locationId = ((Number) locIdObj).intValue();
                        } else {
                            locationId = Integer.parseInt(locIdObj.toString());
                        }
                        Map<String, Object> location = DBConnection.getLocationById(locationId);
                        if (location != null && location.get("name") != null) {
                            user.put("training_location", location.get("name"));
                        }
                    } catch (Exception e) {
                        // Ignore location lookup errors
                    }
                }
            }
        }

        // Filter by role if specified
        if (roleFilter != null && !roleFilter.isEmpty()) {
            final String filter = roleFilter;
            users = users.stream()
                .filter(u -> {
                    Object roleObj = u.get("role");
                    if (roleObj == null) return false;
                    return filter.equalsIgnoreCase(roleObj.toString());
                })
                .collect(java.util.stream.Collectors.toList());
        }
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"users\": [");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(users.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
    
    // Handle get current user profile
    protected void handleGetCurrentUserProfile(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        int userId = JWTUtil.getUserId(token);
        Map<String, Object> user = DBConnection.getUserById(userId);
        
        if (user == null) {
            sendErrorResponse(exchange, 404, "User not found");
            return;
        }
        
        // Remove sensitive data
        user.remove("password");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("user", user);
        
        sendJsonResponse(exchange, 200, mapToJson(response));
    }
    
    // Handle update current user profile
    protected void handleUpdateCurrentUserProfile(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        int userId = JWTUtil.getUserId(token);
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String name = params.get("name");
        String email = params.get("email");
        String phone = params.get("phone");
        String idno = params.get("idno");
        String gender = params.get("gender");
        String dob = params.get("dob");
        String address = params.get("address");
        String emergencyContactName = params.get("emergency_contact_name");
        String emergencyContactPhone = params.get("emergency_contact_phone");
        
        // Update user profile using DBConnection
        boolean success = DBConnection.updateUserExtendedProfile(userId, name, email, phone, idno, gender, dob, address, emergencyContactName, emergencyContactPhone);
        
        if (success) {
            Map<String, Object> user = DBConnection.getUserById(userId);
            user.remove("password");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("user", user);
            
            sendJsonResponse(exchange, 200, mapToJson(response));
        } else {
            sendErrorResponse(exchange, 500, "Failed to update profile");
        }
    }
    
    // Handle update login details (email/password)
    protected void handleUpdateLoginDetails(HttpExchange exchange) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        int userId = JWTUtil.getUserId(token);
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String email = params.get("email");
        String currentPassword = params.get("current_password");
        String newPassword = params.get("new_password");
        
        // Get current user
        Map<String, Object> currentUser = DBConnection.getUserById(userId);
        if (currentUser == null) {
            sendErrorResponse(exchange, 404, "User not found");
            return;
        }
        
        // If changing password, verify current password
        if (newPassword != null && !newPassword.isEmpty()) {
            if (currentPassword == null || currentPassword.isEmpty()) {
                sendErrorResponse(exchange, 400, "Current password is required to change password");
                return;
            }
            
            // Verify current password against hash
            String storedPasswordHash = (String) currentUser.get("password");
            String inputPasswordHash = DBConnection.hashPassword(currentPassword);
            
            if (storedPasswordHash == null || !storedPasswordHash.equals(inputPasswordHash)) {
                sendErrorResponse(exchange, 400, "Current password is incorrect");
                return;
            }
        }
        
        // Update email and/or password
        boolean success = DBConnection.updateUserLogin(userId, email, newPassword);
        
        if (success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login details updated successfully");
            sendJsonResponse(exchange, 200, mapToJson(response));
        } else {
            sendErrorResponse(exchange, 500, "Failed to update login details");
        }
    }
    
    private void handleGetUser(HttpExchange exchange, int userId) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        // Get user from database
        Map<String, Object> user = DBConnection.getUserById(userId);
        if (user == null) {
            sendErrorResponse(exchange, 404, "User not found");
            return;
        }
        
        // Convert to JSON
        String json = mapToJson(user);
        sendJsonResponse(exchange, 200, json);
    }
    
    private void handleUpdateUserRole(HttpExchange exchange, int userId) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String tokenUserId = String.valueOf(JWTUtil.getUserId(token));
        String tokenRole = JWTUtil.getRole(token);
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        // Check if this is updating own profile or admin updating role
        String newRole = params.get("role");
        String fullName = params.get("full_name");
        String email = params.get("email");
        String phone = params.get("phone");
        String password = params.get("password");
        String trainingLocation = params.get("training_location");
        String locationId = params.get("location_id");
        
        // Check if user is updating their own profile (not admin)
        int requestUserId = JWTUtil.getUserId(token);
        boolean isUpdatingOwnProfile = (requestUserId == userId) && !"admin".equals(tokenRole);
        
        // Allow users to update their own email, phone, and password
        if (isUpdatingOwnProfile) {
            // Users can update email, phone, and password for themselves
            boolean updated = DBConnection.updateUserProfile(userId, null, email, phone, password, trainingLocation);
            if (updated) {
                sendSuccessResponse(exchange, "Profile updated successfully");
            } else {
                sendErrorResponse(exchange, 500, "Failed to update profile");
            }
            return;
        }
        
        // Admin can update role and profile together
        if ("admin".equals(tokenRole)) {
            // Validate role if provided
            if (newRole != null && !newRole.equals("admin") && !newRole.equals("applicant") && !newRole.equals("instructor") && !newRole.equals("staff") && !newRole.equals("user")) {
                sendErrorResponse(exchange, 400, "Invalid role. Must be 'admin', 'staff', 'instructor', 'applicant', or 'user'");
                return;
            }
            
            // Update role if provided
            if (newRole != null) {
                DBConnection.updateUserRole(userId, newRole);
            }
            
            // Also update profile fields if provided
            boolean profileUpdated = false;
            if (trainingLocation != null || (password != null && !password.isEmpty())) {
                DBConnection.updateUserProfile(userId, fullName, email, phone, password, trainingLocation);
                profileUpdated = true;
            }
            
            // Update location_id if provided
            if (locationId != null && !locationId.isEmpty()) {
                try {
                    DBConnection.updateUserLocationById(userId, Integer.parseInt(locationId));
                } catch (NumberFormatException e) {
                    // Invalid location_id format
                }
            }
            
            String successMsg = "User role updated successfully";
            if (profileUpdated) {
                successMsg = "User updated successfully";
            }
            sendSuccessResponse(exchange, successMsg);
            return;
        }
        
        // Non-admin users can only update their own profile
        if (!tokenUserId.equals(String.valueOf(userId)) && !"admin".equals(tokenRole)) {
            sendErrorResponse(exchange, 403, "You can only update your own profile");
            return;
        }
        
        // Update profile (requires fullName, email, phone)
        if (fullName == null || fullName.isEmpty()) {
            sendErrorResponse(exchange, 400, "Full name is required");
            return;
        }
        if (email == null || email.isEmpty()) {
            sendErrorResponse(exchange, 400, "Email is required");
            return;
        }
        if (phone == null || phone.isEmpty()) {
            sendErrorResponse(exchange, 400, "Phone is required");
            return;
        }
        
        // Password will be hashed in DBConnection.updateUserProfile
        
        if (DBConnection.updateUserProfile(userId, fullName, email, phone, password, trainingLocation)) {
            // Return updated user data
            Map<String, Object> user = DBConnection.getUserById(userId);
            if (user != null) {
                user.remove("password_hash");
                String json = "{\"success\": true, \"message\": \"Profile updated successfully\", \"user\": " + mapToJson(user) + "}";
                sendJsonResponse(exchange, 200, json);
            } else {
                sendSuccessResponse(exchange, "Profile updated successfully");
            }
        } else {
            sendErrorResponse(exchange, 500, "Failed to update profile");
        }
    }
    
    // Handle fees-only update
    private void handleUpdateUserFees(HttpExchange exchange, int userId) throws IOException {
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);
        
        String schoolFeesStr = params.get("school_fees");
        String feesPaidStr = params.get("fees_paid");
        String feesBalanceStr = params.get("fees_balance");
        String paymentStatus = params.get("payment_status");
        
        if (schoolFeesStr == null || feesPaidStr == null) {
            sendErrorResponse(exchange, 400, "School fees and fees paid are required");
            return;
        }
        
        double schoolFees = Double.parseDouble(schoolFeesStr);
        double feesPaid = Double.parseDouble(feesPaidStr);
        double feesBalance = feesBalanceStr != null ? Double.parseDouble(feesBalanceStr) : (schoolFees - feesPaid);
        
        if (DBConnection.updateUserFees(userId, schoolFees, feesPaid, feesBalance, paymentStatus != null ? paymentStatus : "unpaid")) {
            sendSuccessResponse(exchange, "Fees updated successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to update fees");
        }
    }
    
    private void handleDeleteUser(HttpExchange exchange, int userId) throws IOException {
        // Check admin authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authentication required");
            return;
        }
        
        if (JWTUtil.validateToken(token) == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role)) {
            sendErrorResponse(exchange, 403, "Admin access required");
            return;
        }
        
        if (DBConnection.deleteUser(userId)) {
            sendSuccessResponse(exchange, "User deleted successfully");
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete user");
        }
    }
}

/**
 * Public Location Handler for getting training locations (no auth required)
 */
class LocationHandler extends ApiHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        try {
            if (path.equals("/api/locations") && "GET".equals(method)) {
                handleGetLocations(exchange);
            } else {
                sendErrorResponse(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleGetLocations(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> locations = DBConnection.getAllLocations();
        
        StringBuilder json = new StringBuilder("{\"success\": true, \"locations\": [");
        for (int i = 0; i < locations.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJson(locations.get(i)));
        }
        json.append("]}");
        sendJsonResponse(exchange, 200, json.toString());
    }
}

