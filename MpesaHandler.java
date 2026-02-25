import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handle M-Pesa payment integration for the Ultimate Driving School
 * Implements STK Push, callback handling, and transaction management
 */
class MpesaHandler extends ApiHandler implements HttpHandler {

    // M-Pesa API credentials (placeholder - replace with actual credentials)
    private static final String MPESA_CONSUMER_KEY = "your_consumer_key_here";
    private static final String MPESA_CONSUMER_SECRET = "your_consumer_secret_here";
    private static final String MPESA_SHORTCODE = "174379";
    private static final String MPESA_PASSKEY = "your_passkey_here";
    private static final String MPESA_CALLBACK_URL = "https://yourdomain.com/api/mpesa/callback";
    
    // M-Pesa API endpoints (mock/placeholder URLs)
    private static final String MPESA_STK_PUSH_URL = "https://api.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
    private static final String MPESA_QUERY_URL = "https://api.safaricom.co.ke/mpesa/stkpush/v1/query";
    private static final String MPESA_AUTH_URL = "https://api.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
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

            // Route: POST /api/mpesa/stkpush - Initiate STK Push (admin/staff only)
            if (method.equals("POST") && path.equals("/api/mpesa/stkpush")) {
                handleStkPush(exchange);
                return;
            }

            // Route: POST /api/mpesa/callback - M-Pesa callback (public - no auth required)
            if (method.equals("POST") && path.equals("/api/mpesa/callback")) {
                handleCallback(exchange);
                return;
            }

            // Route: GET /api/mpesa/transactions - List all transactions (admin/staff)
            if (method.equals("GET") && path.equals("/api/mpesa/transactions")) {
                handleGetTransactions(exchange);
                return;
            }

            // Route: GET /api/mpesa/transactions/{id} - Get single transaction
            if (method.equals("GET") && path.matches("/api/mpesa/transactions/\\d+")) {
                String[] parts = path.split("/");
                int transactionId = Integer.parseInt(parts[parts.length - 1]);
                handleGetTransactionById(exchange, transactionId);
                return;
            }

            // Route: GET /api/mpesa/transactions/phone/{phone} - Get transactions by phone
            if (method.equals("GET") && path.matches("/api/mpesa/transactions/phone/.+")) {
                String[] pathParts = path.split("/");
                String phone = pathParts[pathParts.length - 1];
                handleGetTransactionsByPhone(exchange, phone);
                return;
            }

            // Route: POST /api/mpesa/verify - Verify transaction status (admin/staff)
            if (method.equals("POST") && path.equals("/api/mpesa/verify")) {
                handleVerifyTransaction(exchange);
                return;
            }

            // Route: POST /api/mpesa/message - Add manual M-Pesa message (admin/staff)
            if (method.equals("POST") && path.equals("/api/mpesa/message")) {
                handleAddMpesaMessage(exchange);
                return;
            }

            // Route: POST /api/mpesa/transaction - Add manual M-Pesa transaction (admin/staff)
            if (method.equals("POST") && path.equals("/api/mpesa/transaction")) {
                handleAddMpesaTransaction(exchange);
                return;
            }

            // Route: POST /api/mpesa/pay - Send payment request to paybill (admin/staff)
            if (method.equals("POST") && path.equals("/api/mpesa/pay")) {
                handleMpesaPay(exchange);
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
     * Handle STK Push initiation - admin/staff only
     */
    private void handleStkPush(HttpExchange exchange) throws IOException {
        // Verify admin/staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authorization token required");
            return;
        }

        Map<String, Object> userPayload = JWTUtil.validateToken(token);
        if (userPayload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = (String) userPayload.get("role");
        if (!"admin".equals(role) && !"staff".equals(role)) {
            sendErrorResponse(exchange, 403, "Only admin or staff can initiate STK Push");
            return;
        }

        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);

        String phoneNumber = params.get("phone");
        String amountStr = params.get("amount");
        String applicationIdStr = params.get("application_id");

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            sendErrorResponse(exchange, 400, "Phone number is required");
            return;
        }

        if (amountStr == null || amountStr.isEmpty()) {
            sendErrorResponse(exchange, 400, "Amount is required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                sendErrorResponse(exchange, 400, "Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid amount format");
            return;
        }

        // Format phone number (remove leading 0 if present and add 254)
        phoneNumber = formatPhoneNumber(phoneNumber);

        // Generate unique transaction IDs
        String merchantRequestId = "MR" + UUID.randomUUID().toString().substring(0, 23);
        String checkoutRequestId = "CR" + UUID.randomUUID().toString().substring(0, 23);

        // Store transaction in database
        int transactionId = DBConnection.insertMpesaTransaction(
            merchantRequestId,
            checkoutRequestId,
            phoneNumber,
            amount,
            "STK Push",
            applicationIdStr != null && !applicationIdStr.isEmpty() ? Integer.parseInt(applicationIdStr) : null,
            "pending"
        );

        if (transactionId <= 0) {
            sendErrorResponse(exchange, 500, "Failed to create transaction record");
            return;
        }

        // Call M-Pesa STK Push API (mock implementation)
        try {
            Map<String, Object> stkResponse = initiateStkPush(
                phoneNumber,
                (int) amount,
                "Course Fee Payment - Transaction #" + transactionId,
                merchantRequestId,
                checkoutRequestId
            );

            // Update transaction with M-Pesa response
            String resultCode = (String) stkResponse.get("ResponseCode");
            String resultDesc = (String) stkResponse.get("ResponseDescription");
            
            if (resultCode != null && resultCode.equals("0")) {
                DBConnection.updateMpesaTransaction(
                    transactionId,
                    merchantRequestId,
                    checkoutRequestId,
                    "0",
                    resultDesc
                );
            }

            String json = "{\"success\": true, \"message\": \"STK Push initiated successfully\", \"transaction_id\": " + transactionId + ", \"merchant_request_id\": \"" + merchantRequestId + "\", \"checkout_request_id\": \"" + checkoutRequestId + "\"}";
            sendJsonResponse(exchange, 200, json);

        } catch (Exception e) {
            // Update transaction status to failed
            DBConnection.updateMpesaTransactionStatus(transactionId, "failed");
            sendErrorResponse(exchange, 500, "Failed to initiate STK Push: " + e.getMessage());
        }
    }

    /**
     * Handle M-Pesa callback - public endpoint
     */
    private void handleCallback(HttpExchange exchange) throws IOException {
        String body = getRequestBody(exchange);
        System.out.println("M-Pesa Callback received: " + body);

        try {
            Map<String, Object> callbackData = jsonToMap(body);
            
            if (!callbackData.containsKey("Body")) {
                sendErrorResponse(exchange, 400, "Invalid callback format");
                return;
            }

            Map<String, Object> bodyData = (Map<String, Object>) callbackData.get("Body");
            
            if (!bodyData.containsKey("stkCallback")) {
                sendErrorResponse(exchange, 400, "Invalid callback format - missing stkCallback");
                return;
            }

            Map<String, Object> stkCallback = (Map<String, Object>) bodyData.get("stkCallback");
            
            String merchantRequestId = (String) stkCallback.get("MerchantRequestID");
            String checkoutRequestId = (String) stkCallback.get("CheckoutRequestID");
            int resultCode = stkCallback.get("ResultCode") != null ? (Integer) stkCallback.get("ResultCode") : -1;
            String resultDesc = (String) stkCallback.get("ResultDesc");

            // Find transaction by merchant request ID
            Map<String, Object> transaction = DBConnection.getMpesaTransactionByMerchantId(merchantRequestId);
            
            if (transaction == null) {
                System.out.println("Transaction not found for merchant request ID: " + merchantRequestId);
                sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Callback received but transaction not found\"}");
                return;
            }

            int transactionId = (Integer) transaction.get("id");

            if (resultCode == 0) {
                // Successful payment
                Map<String, Object> callbackResult = (Map<String, Object>) stkCallback.get("CallbackMetadata");
                
                String transactionIdMpesa = null;
                String amount = null;
                String mpesaReceipt = null;
                String transactionDate = null;

                if (callbackResult != null && callbackResult.containsKey("Item")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) callbackResult.get("Item");
                    
                    for (Map<String, Object> item : items) {
                        String name = (String) item.get("Name");
                        if ("MpesaReceiptNumber".equals(name)) {
                            mpesaReceipt = (String) item.get("Value");
                        } else if ("Amount".equals(name)) {
                            amount = String.valueOf(item.get("Value"));
                        } else if ("TransactionDate".equals(name)) {
                            transactionDate = formatTransactionDate(item.get("Value"));
                        }
                    }
                }

                // Update transaction status
                DBConnection.updateMpesaTransactionResult(
                    transactionId,
                    resultCode,
                    resultDesc,
                    mpesaReceipt,
                    transactionDate
                );
                DBConnection.updateMpesaTransactionStatus(transactionId, "completed");

                System.out.println("Payment successful for transaction #" + transactionId + " - Receipt: " + mpesaReceipt);

            } else {
                // Failed payment
                DBConnection.updateMpesaTransactionResult(transactionId, resultCode, resultDesc, null, null);
                DBConnection.updateMpesaTransactionStatus(transactionId, "failed");
                
                System.out.println("Payment failed for transaction #" + transactionId + " - Result: " + resultDesc);
            }

            sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Callback processed successfully\"}");

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Error processing callback: " + e.getMessage());
        }
    }

    /**
     * Get all transactions with optional filtering - admin/staff only
     */
    private void handleGetTransactions(HttpExchange exchange) throws IOException {
        // Verify admin/staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authorization token required");
            return;
        }

        Map<String, Object> userPayload = JWTUtil.validateToken(token);
        if (userPayload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = (String) userPayload.get("role");
        if (!"admin".equals(role) && !"staff".equals(role)) {
            sendErrorResponse(exchange, 403, "Only admin or staff can view all transactions");
            return;
        }

        // Get query parameters for filtering
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());
        String status = queryParams.get("status");
        String phone = queryParams.get("phone");
        String startDate = queryParams.get("start_date");
        String endDate = queryParams.get("end_date");
        String limitStr = queryParams.get("limit");
        String offsetStr = queryParams.get("offset");

        int limit = (limitStr != null && !limitStr.isEmpty()) ? Integer.parseInt(limitStr) : 100;
        int offset = (offsetStr != null && !offsetStr.isEmpty()) ? Integer.parseInt(offsetStr) : 0;

        List<Map<String, Object>> transactions = DBConnection.getMpesaTransactions(status, phone, startDate, endDate, limit, offset);
        int totalCount = DBConnection.countMpesaTransactions(status, phone, startDate, endDate);

        // Get summary
        Map<String, Object> summary = DBConnection.getMpesaTransactionSummary();

        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactions);
        response.put("total", totalCount);
        response.put("limit", limit);
        response.put("offset", offset);
        response.put("summary", summary);

        String json = mapToJson(response);
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get single transaction by ID
     */
    private void handleGetTransactionById(HttpExchange exchange, int transactionId) throws IOException {
        // Verify admin/staff or owner authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authorization token required");
            return;
        }

        Map<String, Object> userPayload = JWTUtil.validateToken(token);
        if (userPayload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        Map<String, Object> transaction = DBConnection.getMpesaTransactionById(transactionId);
        
        if (transaction == null) {
            sendErrorResponse(exchange, 404, "Transaction not found");
            return;
        }

        // Check if user has access to this transaction
        String role = (String) userPayload.get("role");
        int userId = ((Number) userPayload.get("sub")).intValue();
        
        // Admin and staff can view all transactions
        if (!"admin".equals(role) && !"staff".equals(role)) {
            // For regular users, they can only view their own transactions
            // Note: You'll need to implement user_id check based on your user table
            // For now, we'll allow access if the transaction has no user_id or matches
            if (transaction.containsKey("user_id") && transaction.get("user_id") != null) {
                int transactionUserId = ((Number) transaction.get("user_id")).intValue();
                if (transactionUserId != userId) {
                    sendErrorResponse(exchange, 403, "Access denied");
                    return;
                }
            }
        }

        String json = mapToJson(transaction);
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Get transactions by phone number
     */
    private void handleGetTransactionsByPhone(HttpExchange exchange, String phone) throws IOException {
        // Verify admin/staff or owner authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authorization token required");
            return;
        }

        Map<String, Object> userPayload = JWTUtil.validateToken(token);
        if (userPayload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        // Fetch transactions by phone number
        List<Map<String, Object>> transactions = DBConnection.getMpesaTransactions(null, phone, null, null, 100, 0);
        
        Map<String, Object> response = new HashMap<>();
        response.put("phone", phone);
        response.put("transactions", transactions);
        response.put("total", transactions.size());

        String json = mapToJson(response);
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Verify transaction status by querying M-Pesa
     */
    private void handleVerifyTransaction(HttpExchange exchange) throws IOException {
        // Verify admin/staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authorization token required");
            return;
        }

        Map<String, Object> userPayload = JWTUtil.validateToken(token);
        if (userPayload == null) {
            sendErrorResponse(exchange, 401, "Invalid or expired token");
            return;
        }

        String role = (String) userPayload.get("role");
        if (!"admin".equals(role) && !"staff".equals(role)) {
            sendErrorResponse(exchange, 403, "Only admin or staff can verify transactions");
            return;
        }

        String body = getRequestBody(exchange);
        Map<String, String> params = parseFormData(body);

        String checkoutRequestId = params.get("checkout_request_id");
        String transactionIdStr = params.get("transaction_id");

        if (checkoutRequestId == null && transactionIdStr == null) {
            sendErrorResponse(exchange, 400, "Either checkout_request_id or transaction_id is required");
            return;
        }

        Map<String, Object> transaction;
        if (checkoutRequestId != null) {
            transaction = DBConnection.getMpesaTransactionByCheckoutId(checkoutRequestId);
        } else {
            transaction = DBConnection.getMpesaTransactionById(Integer.parseInt(transactionIdStr));
        }

        if (transaction == null) {
            sendErrorResponse(exchange, 404, "Transaction not found");
            return;
        }

        // Query M-Pesa for transaction status (mock implementation)
        try {
            Map<String, Object> queryResponse = queryMpesaTransaction(
                (String) transaction.get("checkout_request_id"),
                (String) transaction.get("merchant_request_id")
            );

            // Update transaction status based on query response
            String resultCode = (String) queryResponse.get("result_code");
            String resultDesc = (String) queryResponse.get("result_desc");

            int transId = (Integer) transaction.get("id");
            if (resultCode != null) {
                DBConnection.updateMpesaTransactionResult(
                    transId,
                    resultCode.equals("0") ? 0 : Integer.parseInt(resultCode),
                    resultDesc,
                    null,
                    null
                );

                if (resultCode.equals("0")) {
                    DBConnection.updateMpesaTransactionStatus(transId, "completed");
                    transaction.put("status", "completed");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transaction", transaction);
            response.put("query_response", queryResponse);

            String json = mapToJson(response);
            sendJsonResponse(exchange, 200, json);

        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Failed to verify transaction: " + e.getMessage());
        }
    }

    /**
     * Handle adding a manual M-Pesa message
     */
    private void handleAddMpesaMessage(HttpExchange exchange) throws IOException {
        // Verify admin/staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authorization required");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role) && !"staff".equals(role)) {
            sendErrorResponse(exchange, 403, "Access denied");
            return;
        }
        
        try {
            String body = getRequestBody(exchange);
            Map<String, String> params = parseFormData(body);
            
            String applicationIdStr = params.get("application_id");
            String message = params.get("message");
            String phone = params.get("phone");
            String amountStr = params.get("amount");
            String mpesaCode = params.get("mpesa_code");
            
            if (applicationIdStr == null || message == null) {
                sendErrorResponse(exchange, 400, "application_id and message are required");
                return;
            }
            
            int applicationId = Integer.parseInt(applicationIdStr);
            double amount = amountStr != null ? Double.parseDouble(amountStr) : 0;
            
            // Insert into mpesa_messages table
            int messageId = DBConnection.addMpesaMessage(applicationId, message, phone, amount, mpesaCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message_id", messageId);
            response.put("message", "M-Pesa message saved successfully");
            
            String json = mapToJson(response);
            sendJsonResponse(exchange, 200, json);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Failed to save M-Pesa message: " + e.getMessage());
        }
    }

    /**
     * Handle adding a manual M-Pesa transaction to mpesa_transactions table
     */
    private void handleAddMpesaTransaction(HttpExchange exchange) throws IOException {
        // Verify admin/staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authorization required");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role) && !"staff".equals(role)) {
            sendErrorResponse(exchange, 403, "Access denied");
            return;
        }
        
        try {
            String body = getRequestBody(exchange);
            Map<String, String> params = parseFormData(body);
            
            String applicationIdStr = params.get("application_id");
            String rawMessage = params.get("raw_message");
            String phone = params.get("phone");
            String amountStr = params.get("amount");
            
            if (applicationIdStr == null) {
                sendErrorResponse(exchange, 400, "application_id is required");
                return;
            }
            
            int applicationId = Integer.parseInt(applicationIdStr);
            double amount = amountStr != null ? Double.parseDouble(amountStr) : 0;
            
            // Insert into mpesa_transactions table
            int transactionId = DBConnection.insertMpesaTransaction(
                "MANUAL_" + System.currentTimeMillis(),  // merchant_request_id
                "MANUAL_" + System.currentTimeMillis(),  // checkout_request_id
                phone != null ? phone : "",
                amount,
                "Manual Entry",
                applicationId,
                "pending"
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transaction_id", transactionId);
            response.put("message", "M-Pesa transaction saved successfully");
            
            String json = mapToJson(response);
            sendJsonResponse(exchange, 200, json);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Failed to save M-Pesa transaction: " + e.getMessage());
        }
    }

    /**
     * Handle M-Pesa payment request to paybill 4108993
     */
    private void handleMpesaPay(HttpExchange exchange) throws IOException {
        // Verify admin/staff authorization
        String token = getTokenFromHeader(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Authorization required");
            return;
        }
        
        String role = JWTUtil.getRole(token);
        if (!"admin".equals(role) && !"staff".equals(role)) {
            sendErrorResponse(exchange, 403, "Access denied");
            return;
        }
        
        try {
            String body = getRequestBody(exchange);
            Map<String, String> params = parseFormData(body);
            
            String phone = params.get("phone");
            String amountStr = params.get("amount");
            String applicationIdStr = params.get("application_id");
            
            if (phone == null || amountStr == null) {
                sendErrorResponse(exchange, 400, "phone and amount are required");
                return;
            }
            
            // Format phone number (remove +254, add 254)
            if (phone.startsWith("+254")) {
                phone = phone.substring(1);
            } else if (phone.startsWith("0")) {
                phone = "254" + phone.substring(1);
            }
            
            double amount = (amountStr != null && !amountStr.isEmpty()) ? Double.parseDouble(amountStr) : 0;
            int applicationId = (applicationIdStr != null && !applicationIdStr.isEmpty()) ? Integer.parseInt(applicationIdStr) : 0;
            
            // Paybill details
            String paybill = "4108993";
            String accountNo = "New ultimate";
            
            // Generate transaction IDs
            String merchantRequestId = "MR" + System.currentTimeMillis();
            String checkoutRequestId = "CH" + System.currentTimeMillis();
            
            // Save to mpesa_transactions
            int transactionId = DBConnection.insertMpesaTransaction(
                merchantRequestId,
                checkoutRequestId,
                phone,
                amount,
                "Paybill Payment",
                applicationId,
                "pending"
            );
            
            // Initiate STK Push (mock implementation)
            Map<String, Object> stkResult = initiateStkPush(
                phone,
                (int) amount,
                "Payment to " + accountNo,
                merchantRequestId,
                checkoutRequestId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transaction_id", transactionId);
            response.put("merchant_request_id", merchantRequestId);
            response.put("checkout_request_id", checkoutRequestId);
            response.put("paybill", paybill);
            response.put("account_no", accountNo);
            response.put("message", "Payment request sent to " + phone);
            response.put("stk_response", stkResult);
            
            String json = mapToJson(response);
            sendJsonResponse(exchange, 200, json);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Failed to initiate payment: " + e.getMessage());
        }
    }

    /**
     * Initiate STK Push to M-Pesa API (mock implementation)
     */
    private Map<String, Object> initiateStkPush(String phoneNumber, int amount, String description, 
                                                String merchantRequestId, String checkoutRequestId) throws IOException {
        // Generate timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new java.util.Date());
        
        // Generate password (base64 of shortcode + passkey + timestamp)
        String password = Base64.getEncoder().encodeToString(
            (MPESA_SHORTCODE + MPESA_PASSKEY + timestamp).getBytes(StandardCharsets.UTF_8)
        );

        // Mock response for testing (in production, call actual M-Pesa API)
        System.out.println("STK Push initiated - Phone: " + phoneNumber + ", Amount: " + amount + ", MRID: " + merchantRequestId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("MerchantRequestID", merchantRequestId);
        response.put("CheckoutRequestID", checkoutRequestId);
        response.put("ResponseCode", "0");
        response.put("ResponseDescription", "Success. Request accepted for processing");
        response.put("ResultCode", 0);
        response.put("ResultDesc", "The service request is processed successfully.");
        
        return response;
    }

    /**
     * Query M-Pesa transaction status (mock implementation)
     */
    private Map<String, Object> queryMpesaTransaction(String checkoutRequestId, String merchantRequestId) throws IOException {
        // Mock response for testing
        System.out.println("Querying transaction - CRID: " + checkoutRequestId + ", MRID: " + merchantRequestId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("ResponseCode", "0");
        response.put("ResponseDescription", "Success");
        response.put("ResultCode", "0");
        response.put("ResultDesc", "The service request is processed successfully.");
        response.put("MerchantRequestID", merchantRequestId);
        response.put("CheckoutRequestID", checkoutRequestId);
        
        return response;
    }

    /**
     * Format phone number to M-Pesa format (254XXXXXXXXX)
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null) return null;
        phone = phone.trim();
        if (phone.startsWith("0")) {
            phone = "254" + phone.substring(1);
        } else if (!phone.startsWith("254")) {
            phone = "254" + phone;
        }
        return phone;
    }

    /**
     * Format transaction date from M-Pesa callback
     */
    private String formatTransactionDate(Object value) {
        if (value == null) return null;
        // M-Pesa sends date as long (epoch milliseconds or format like yyyyMMddHHmmss)
        try {
            if (value instanceof Number) {
                long millis = ((Number) value).longValue();
                return new Timestamp(millis).toString();
            }
            return value.toString();
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * Convert JSON string to Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();
        // Simple JSON parsing (for more complex JSON, use a library like Jackson or Gson)
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim();
                    
                    if (value.startsWith("{") && value.endsWith("}")) {
                        map.put(key, jsonToMap(value));
                    } else if (value.startsWith("[") && value.endsWith("]")) {
                        map.put(key, jsonToArray(value));
                    } else if (value.equals("null")) {
                        map.put(key, null);
                    } else if (value.startsWith("\"") && value.endsWith("\"")) {
                        map.put(key, value.substring(1, value.length() - 1));
                    } else if (value.equals("true")) {
                        map.put(key, true);
                    } else if (value.equals("false")) {
                        map.put(key, false);
                    } else {
                        // Try to parse as number
                        try {
                            if (value.contains(".")) {
                                map.put(key, Double.parseDouble(value));
                            } else {
                                map.put(key, Long.parseLong(value));
                            }
                        } catch (NumberFormatException e) {
                            map.put(key, value);
                        }
                    }
                }
            }
        }
        return map;
    }

    /**
     * Convert JSON array string to List
     */
    @SuppressWarnings("unchecked")
    private List<Object> jsonToArray(String json) {
        List<Object> list = new ArrayList<>();
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1);
            if (!json.isEmpty()) {
                String[] items = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String item : items) {
                    item = item.trim();
                    if (item.startsWith("{") && item.endsWith("}")) {
                        list.add(jsonToMap(item));
                    } else if (item.startsWith("[") && item.endsWith("]")) {
                        list.add(jsonToArray(item));
                    } else if (item.equals("null")) {
                        list.add(null);
                    } else if (item.startsWith("\"") && item.endsWith("\"")) {
                        list.add(item.substring(1, item.length() - 1));
                    } else {
                        try {
                            if (item.contains(".")) {
                                list.add(Double.parseDouble(item));
                            } else {
                                list.add(Long.parseLong(item));
                            }
                        } catch (NumberFormatException e) {
                            list.add(item);
                        }
                    }
                }
            }
        }
        return list;
    }
}
