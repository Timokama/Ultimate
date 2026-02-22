/**
 * Ultimate Defensive Driving School - API Endpoints Configuration
 * Centralized endpoint definitions for backend and frontend
 */

const API_ENDPOINTS = {
    // Base path
    BASE: '/api',

    // Auth endpoints
    AUTH: {
        REGISTER: '/auth/register',
        LOGIN: '/auth/login',
        LOGOUT: '/auth/logout',
        ME: '/auth/me',
        REFRESH: '/auth/refresh',
        CREATE_ADMIN: '/auth/create-admin',
        RESET_PASSWORD: '/auth/reset-password'
    },

    // Users endpoints
    USERS: {
        LIST: '/users',
        GET: (id) => `/users/${id}`,
        UPDATE: (id) => `/users/${id}`,
        DELETE: (id) => `/users/${id}`,
        PROFILE: '/users/profile'
    },

    // Courses endpoints
    COURSES: {
        LIST: '/courses',
        GET: (id) => `/courses/${id}`,
        CREATE: '/courses',
        UPDATE: (id) => `/courses/${id}`,
        DELETE: (id) => `/courses/${id}`
    },

    // Applications endpoints
    APPLICATIONS: {
        LIST: '/applications',
        GET: (id) => `/applications/${id}`,
        CREATE: '/applications',
        UPDATE: (id) => `/applications/${id}`,
        UPDATE_STATUS: (id) => `/applications/${id}/status`,
        MY_APPLICATIONS: '/applications',
        ALL_APPLICATIONS: '/applications/all'
    },

    // Staff endpoints
    STAFF: {
        LOCATION_STUDENTS: '/staff/location-students',
        FEES_SUMMARY: '/staff/fees-summary',
        UPDATE_APPLICATION_FEES: '/staff/application/fees',
        UPDATE_PROFILE: '/staff/profile',
        STUDENTS: '/staff/students',
        APPLICATIONS: '/staff/applications',
        ALL_STAFF: '/staff/all',
        CREATE: '/staff/create',
        UPDATE: '/staff/update',
        DELETE: (id) => `/staff/delete/${id}`,
        ALL_APPLICATIONS: '/staff/all-applications',
        APPLICANTS: '/staff/applicants',
        USERS: '/staff/users'
    },

    // Contact endpoints
    CONTACT: {
        SUBMIT: '/contact',
        LIST: '/contacts',
        GET: (id) => `/contacts/${id}`,
        UPDATE: (id) => `/contacts/${id}`,
        DELETE: (id) => `/contacts/${id}`
    },

    // Location endpoints
    LOCATIONS: {
        LIST: '/locations',
        GET: (id) => `/locations/${id}`,
        CREATE: '/locations',
        UPDATE: (id) => `/locations/${id}`,
        DELETE: (id) => `/locations/${id}`
    },

    // Enrollment endpoints
    ENROLLMENTS: {
        LIST: '/enrollments',
        GET: (id) => `/enrollments/${id}`,
        CREATE: '/enrollments',
        UPDATE: (id) => `/enrollments/${id}`,
        DELETE: (id) => `/enrollments/${id}`,
        MY_ENROLLMENTS: '/enrollments/my'
    },

    // Attendance endpoints
    ATTENDANCE: {
        LIST: '/attendance',
        GET: (id) => `/attendance/${id}`,
        CREATE: '/attendance',
        UPDATE: (id) => `/attendance/${id}`,
        DELETE: (id) => `/attendance/${id}`,
        BY_CLASS: (classId) => `/attendance/class/${classId}`,
        BY_STUDENT: (studentId) => `/attendance/student/${studentId}`
    },

    // M-Pesa endpoints
    MPESA: {
        STK_PUSH: '/mpesa/stkpush',
        CALLBACK: '/mpesa/callback',
        TRANSACTIONS: '/mpesa/transactions',
        GET_TRANSACTION: (id) => `/mpesa/transactions/${id}`,
        GET_TRANSACTIONS_BY_PHONE: (phone) => `/mpesa/transactions/phone/${phone}`,
        VERIFY: '/mpesa/verify'
    }
};

// Helper function to build full URL
function getApiUrl(endpoint) {
    return API_ENDPOINTS.BASE + endpoint;
}

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { API_ENDPOINTS, getApiUrl };
}
