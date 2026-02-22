import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Handle contact form submissions and admin message management
 */
class ContactHandler extends ApiHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers for contact form
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Route: POST /api/contact - Submit contact form (public)
            if (method.equals("POST") && path.equals("/api/contact")) {
                handleSubmitContact(exchange);
                return;
            }

            // Route: GET /api/contacts - Get all messages (admin only)
            if (method.equals("GET") && path.equals("/api/contacts")) {
                handleGetContacts(exchange);
                return;
            }

            // Route: PUT /api/contacts/{id} - Update message status (admin only)
            if (method.equals("PUT") && path.startsWith("/api/contacts/")) {
                String[] parts = path.split("/");
                int messageId = Integer.parseInt(parts[parts.length - 1]);
                handleUpdateContact(exchange, messageId);
                return;
            }

            // Route: DELETE /api/contacts/{id} - Delete message (admin only)
            if (method.equals("DELETE") && path.startsWith("/api/contacts/")) {
                String[] parts = path.split("/");
                int messageId = Integer.parseInt(parts[parts.length - 1]);
                handleDeleteContact(exchange, messageId);
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
     * Submit a contact form message (public endpoint)
     */
    private void handleSubmitContact(HttpExchange exchange) throws IOException {
        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);

        String name = params.get("name");
        String email = params.get("email");
        String phone = params.get("phone");
        String subject = params.get("subject");
        String message = params.get("message");

        if (name == null || name.isEmpty() || email == null || email.isEmpty() || message == null || message.isEmpty()) {
            sendErrorResponse(exchange, 400, "Name, email, and message are required");
            return;
        }
        
        // Check for duplicate message (same email + message content within last 5 minutes)
        if (DBConnection.isDuplicateContactMessage(email, message)) {
            String json = "{\"success\": false, \"duplicate\": true, \"error\": \"This message has already been submitted recently\"}";
            sendJsonResponse(exchange, 409, json);
            return;
        }

        int messageId = DBConnection.saveContactMessage(name, email, phone, subject, message);

        if (messageId > 0) {
            String json = "{\"success\": true, \"message\": \"Message sent successfully! We'll get back to you soon.\", \"id\": " + messageId + "}";
            sendJsonResponse(exchange, 201, json);
        } else {
            sendErrorResponse(exchange, 500, "Failed to send message");
        }
    }

    /**
     * Get all contact messages (admin only)
     */
    private void handleGetContacts(HttpExchange exchange) throws IOException {
        // Check authentication
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

        List<Map<String, Object>> messages = DBConnection.getAllContactMessages();
        String json = "{\"success\": true, \"messages\": " + listToJson(messages) + ", \"count\": " + messages.size() + "}";
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Update contact message status (admin only)
     */
    private void handleUpdateContact(HttpExchange exchange, int messageId) throws IOException {
        // Check authentication
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
        String status = params.get("status");

        if (status == null || status.isEmpty()) {
            sendErrorResponse(exchange, 400, "Status is required");
            return;
        }

        if (!status.equals("new") && !status.equals("read") && !status.equals("replied") && !status.equals("closed")) {
            sendErrorResponse(exchange, 400, "Invalid status. Must be: new, read, replied, or closed");
            return;
        }

        if (DBConnection.updateContactMessageStatus(messageId, status)) {
            sendSuccessResponse(exchange, "Message status updated");
        } else {
            sendErrorResponse(exchange, 500, "Failed to update message");
        }
    }

    /**
     * Delete contact message (admin only)
     */
    private void handleDeleteContact(HttpExchange exchange, int messageId) throws IOException {
        // Check authentication
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

        if (DBConnection.deleteContactMessage(messageId)) {
            sendSuccessResponse(exchange, "Message deleted");
        } else {
            sendErrorResponse(exchange, 500, "Failed to delete message");
        }
    }

    /**
     * Convert list of maps to JSON array string
     */
    private String listToJson(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(mapToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}
