import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Utility class for token generation and validation
 * Implements a simplified JWT-like token system
 */
public class JWTUtil {
    
    private static final String SECRET = "UltimateDefensiveDrivingSchoolSecretKey2024!";
    private static final long EXPIRATION_TIME = 86400000L; // 24 hours in milliseconds
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * Generate a JWT token for a user
     */
    public static String generateToken(int userId, String email, String role) {
        return generateToken(userId, email, role, null, null);
    }
    
    /**
     * Generate a JWT token for a user with phone
     */
    public static String generateToken(int userId, String email, String role, String phone) {
        return generateToken(userId, email, role, phone, null);
    }
    
    /**
     * Generate a JWT token for a user with phone and location_id
     */
    public static String generateToken(int userId, String email, String role, String phone, String locationId) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        // Header
        Map<String, Object> header = new HashMap<>();
        header.put("typ", "JWT");
        header.put("alg", "HS256");
        
        // Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", userId);
        payload.put("email", email);
        payload.put("role", role);
        if (phone != null) {
            payload.put("phone", phone);
        }
        if (locationId != null) {
            payload.put("location_id", locationId);
        }
        payload.put("iat", now.getTime());
        payload.put("exp", expirationDate.getTime());
        
        // Generate signature
        String headerJson = objectToJson(header);
        String payloadJson = objectToJson(payload);
        String headerBase64 = base64Encode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadBase64 = base64Encode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = generateSignature(headerBase64, payloadBase64);
        
        return headerBase64 + "." + payloadBase64 + "." + signature;
    }
    
    /**
     * Validate a JWT token
     */
    public static Map<String, Object> validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            String headerBase64 = parts[0];
            String payloadBase64 = parts[1];
            String signature = parts[2];
            
            // Verify signature
            String expectedSignature = generateSignature(headerBase64, payloadBase64);
            if (!signature.equals(expectedSignature)) {
                return null;
            }
            
            // Decode payload
            String payloadJson = new String(base64Decode(payloadBase64), StandardCharsets.UTF_8);
            Map<String, Object> payload = jsonToMap(payloadJson);
            
            // Check expiration
            if (payload.containsKey("exp")) {
                long exp = ((Number) payload.get("exp")).longValue();
                if (System.currentTimeMillis() > exp) {
                    return null;
                }
            }
            
            return payload;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extract user ID from token
     */
    public static int getUserId(String token) {
        Map<String, Object> payload = validateToken(token);
        if (payload != null && payload.containsKey("sub")) {
            return ((Number) payload.get("sub")).intValue();
        }
        return -1;
    }
    
    /**
     * Extract email from token
     */
    public static String getEmail(String token) {
        Map<String, Object> payload = validateToken(token);
        if (payload != null && payload.containsKey("email")) {
            return (String) payload.get("email");
        }
        return null;
    }
    
    /**
     * Extract role from token
     */
    public static String getRole(String token) {
        Map<String, Object> payload = validateToken(token);
        if (payload != null && payload.containsKey("role")) {
            return (String) payload.get("role");
        }
        return null;
    }
    
    /**
     * Check if token is expired
     */
    public static boolean isTokenExpired(String token) {
        Map<String, Object> payload = validateToken(token);
        if (payload == null) {
            return true;
        }
        if (payload.containsKey("exp")) {
            long exp = ((Number) payload.get("exp")).longValue();
            return System.currentTimeMillis() > exp;
        }
        return false;
    }
    
    /**
     * Generate HMAC signature
     */
    private static String generateSignature(String headerBase64, String payloadBase64) {
        try {
            String data = headerBase64 + "." + payloadBase64;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String secretWithSalt = SECRET + "ultimate";
            byte[] hash = digest.digest(secretWithSalt.getBytes(StandardCharsets.UTF_8));
            byte[] dataHash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            
            // Combine and hash
            byte[] combined = new byte[hash.length + dataHash.length];
            System.arraycopy(hash, 0, combined, 0, hash.length);
            System.arraycopy(dataHash, 0, combined, hash.length, dataHash.length);
            byte[] finalHash = digest.digest(combined);
            
            return base64Encode(finalHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Base64 encode
     */
    private static String base64Encode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
    
    /**
     * Base64 decode
     */
    private static byte[] base64Decode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }
    
    /**
     * Convert object to JSON string (simplified)
     */
    private static String objectToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (count > 0) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(value).append("\"");
            }
            count++;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Convert JSON string to map (simplified)
     */
    private static Map<String, Object> jsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.substring(1, json.length() - 1); // Remove { and }
        if (json.isEmpty()) {
            return map;
        }
        
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim();
                
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                    map.put(key, value);
                } else if (value.equals("true") || value.equals("false")) {
                    map.put(key, Boolean.parseBoolean(value));
                } else if (value.contains(".")) {
                    try {
                        map.put(key, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        map.put(key, value);
                    }
                } else {
                    try {
                        map.put(key, Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }
    
    /**
     * Generate a random token (for refresh tokens)
     */
    public static String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Check if token needs refresh (expiring within 5 minutes)
     */
    public static boolean needsRefresh(String token) {
        Map<String, Object> payload = validateToken(token);
        if (payload == null || !payload.containsKey("exp")) {
            return true;
        }
        long exp = ((Number) payload.get("exp")).longValue();
        long now = System.currentTimeMillis();
        long fiveMinutes = 5 * 60 * 1000; // 5 minutes in milliseconds
        return (exp - now) < fiveMinutes;
    }
    
    /**
     * Get token expiration time in milliseconds
     */
    public static long getExpirationTime(String token) {
        Map<String, Object> payload = validateToken(token);
        if (payload != null && payload.containsKey("exp")) {
            return ((Number) payload.get("exp")).longValue();
        }
        return 0;
    }
}
