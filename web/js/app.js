/**
 * Ultimate Defensive Driving School - JavaScript Application
 */

// Import API endpoints configuration
const API_BASE = '/api';

// Storage Keys
const TOKEN_KEY = 'ultimate_token';
const USER_KEY = 'ultimate_user';

// Utility Functions
function escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatCurrency(amount) {
    if (amount == null) return 'KSh 0';
    return 'KSh ' + parseFloat(amount).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-KE', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch (e) {
        return dateString;
    }
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
}

const api = {
    async request(endpoint, method = 'GET', body = null) {
        const token = localStorage.getItem(TOKEN_KEY);
        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded'
        };
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        
        const options = {
            method,
            headers
        };
        
        if (body) {
            options.body = new URLSearchParams(body).toString();
        }
        
        try {
            const response = await fetch(`${API_BASE}${endpoint}`, options);
            const data = await response.json();
            
            if (!response.ok) {
                throw new Error(data.error || `HTTP ${response.status}: Request failed`);
            }
            
            return data;
        } catch (error) {
            throw error;
        }
    },
    
    get(endpoint) {
        return this.request(endpoint, 'GET');
    },
    
    post(endpoint, body) {
        return this.request(endpoint, 'POST', body);
    },
    
    put(endpoint, body) {
        return this.request(endpoint, 'PUT', body);
    }
};

const auth = {
    async login(email, password) {
        try {
            const data = await api.post(API_ENDPOINTS.AUTH.LOGIN, { email, password });
            localStorage.setItem(TOKEN_KEY, data.token);
            localStorage.setItem(USER_KEY, JSON.stringify(data.user));
            this.startRefreshTimer();
            return data.user;
        } catch (error) {
            throw error;
        }
    },
    
    async register(formData) {
        const data = await api.post(API_ENDPOINTS.AUTH.REGISTER, formData);
        localStorage.setItem(TOKEN_KEY, data.token);
        localStorage.setItem(USER_KEY, JSON.stringify(data.user));
        this.startRefreshTimer();
        return data.user;
    },
    
    logout() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        this.stopRefreshTimer();
        window.location.href = 'index.html';
    },
    
    getToken() {
        return localStorage.getItem(TOKEN_KEY);
    },
    
    getUser() {
        const user = localStorage.getItem(USER_KEY);
        return user ? JSON.parse(user) : null;
    },
    
    isLoggedIn() {
        return !!this.getToken();
    },
    
    async refreshToken() {
        try {
            const data = await api.post(API_ENDPOINTS.AUTH.REFRESH);
            if (data.success && data.token) {
                localStorage.setItem(TOKEN_KEY, data.token);
                this.startRefreshTimer();
                return true;
            }
        } catch (e) {
        }
        return false;
    },
    
    refreshTimer: null,
    
    startRefreshTimer() {
        this.stopRefreshTimer();
        // Refresh token every 20 minutes (before 24-hour expiry)
        const refreshInterval = 20 * 60 * 1000; // 20 minutes
        this.refreshTimer = setInterval(() => {
            if (this.isLoggedIn()) {
                this.refreshToken();
            }
        }, refreshInterval);
    },
    
    stopRefreshTimer() {
        if (this.refreshTimer) {
            clearInterval(this.refreshTimer);
            this.refreshTimer = null;
        }
    },
    
    async checkAuth() {
        if (!this.getToken()) {
            return false;
        }
        
        try {
            const data = await api.get(API_ENDPOINTS.AUTH.ME);
            if (data.success) {
                this.startRefreshTimer();
            }
            return data.success;
        } catch (e) {
            // Try to refresh token before logging out
            const refreshed = await this.refreshToken();
            if (refreshed) {
                return true;
            }
            this.logout();
            return false;
        }
    }
};

const courses = {
    async getAll() {
        return api.get(API_ENDPOINTS.COURSES.LIST);
    },
    
    async getById(id) {
        return api.get(API_ENDPOINTS.COURSES.GET(id));
    },
    
    async create(courseData) {
        return api.post(API_ENDPOINTS.COURSES.CREATE, courseData);
    },
    
    async update(id, courseData) {
        return api.put(API_ENDPOINTS.COURSES.UPDATE(id), courseData);
    },
    
    async delete(id) {
        const response = await fetch(`${API_BASE}${API_ENDPOINTS.COURSES.DELETE(id)}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem(TOKEN_KEY)}`
            }
        });
        return response.json();
    }
};

const applications = {
    async submit(formData) {
        return api.post(API_ENDPOINTS.APPLICATIONS.CREATE, formData);
    },
    
    async getMy() {
        return api.get(API_ENDPOINTS.APPLICATIONS.MY_APPLICATIONS);
    },
    
    async getAll() {
        return api.get(API_ENDPOINTS.APPLICATIONS.ALL_APPLICATIONS);
    },
    
    async updateStatus(id, status) {
        return api.put(API_ENDPOINTS.APPLICATIONS.UPDATE_STATUS(id), { status });
    }
};

const users = {
    async getAll() {
        return api.get(API_ENDPOINTS.USERS.LIST);
    },
    
    async update(id, userData) {
        return api.put(API_ENDPOINTS.USERS.UPDATE(id), userData);
    },
    
    async delete(id) {
        const response = await fetch(`${API_BASE}${API_ENDPOINTS.USERS.DELETE(id)}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem(TOKEN_KEY)}`
            }
        });
        return response.json();
    }
};

const contacts = {
    async submit(formData) {
        return api.post(API_ENDPOINTS.CONTACT.SUBMIT, formData);
    },
    
    async getAll() {
        return api.get(API_ENDPOINTS.CONTACT.LIST);
    },
    
    async updateStatus(id, status) {
        return api.put(API_ENDPOINTS.CONTACT.UPDATE(id), { status });
    },
    
    async delete(id) {
        const response = await fetch(`${API_BASE}${API_ENDPOINTS.CONTACT.DELETE(id)}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem(TOKEN_KEY)}`
            }
        });
        return response.json();
    }
};

// Staff Module for Staff Dashboard
const staff = {
    async getLocationStudents(location) {
        return api.get(API_ENDPOINTS.STAFF.LOCATION_STUDENTS + `?location=${encodeURIComponent(location)}`);
    },
    
    async getFeesSummary(location) {
        return api.get(API_ENDPOINTS.STAFF.FEES_SUMMARY + `?location=${encodeURIComponent(location)}`);
    },
    
    async updateApplicationFees(formData) {
        return api.put(API_ENDPOINTS.STAFF.UPDATE_APPLICATION_FEES, formData);
    },
    
    async updateProfile(formData) {
        return api.put(API_ENDPOINTS.STAFF.UPDATE_PROFILE, formData);
    },
    
    async getAllStaff() {
        return api.get(API_ENDPOINTS.STAFF.ALL_STAFF);
    },
    
    async createStaff(formData) {
        return api.post(API_ENDPOINTS.STAFF.CREATE, formData);
    },
    
    async updateStaff(formData) {
        return api.put(API_ENDPOINTS.STAFF.UPDATE, formData);
    },
    
    async deleteStaff(id) {
        const response = await fetch(`${API_BASE}${API_ENDPOINTS.STAFF.DELETE(id)}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem(TOKEN_KEY)}`
            }
        });
        return response.json();
    },
    
    async getAllApplications() {
        return api.get(API_ENDPOINTS.STAFF.ALL_APPLICATIONS);
    },
    
    async getApplicants() {
        return api.get(API_ENDPOINTS.STAFF.APPLICANTS);
    },
    
    async getAllUsers() {
        return api.get(API_ENDPOINTS.STAFF.USERS);
    }
};

// UI Functions
function showAlert(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    
    // Find a suitable container
    const container = document.querySelector('.form-container, .dashboard-content, .auth-container');
    if (container) {
        container.insertBefore(alertDiv, container.firstChild);
    } else {
        document.body.insertBefore(alertDiv, document.body.firstChild);
    }
    
    // Auto-remove after 5 seconds
    setTimeout(() => alertDiv.remove(), 5000);
}

// Toast notification with improved styling and animations
function showToast(message, type = 'info') {
    // Create toast container if not exists
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        container.innerHTML = `
            <style>
                @keyframes slideIn {
                    from { transform: translateX(100%); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
                @keyframes slideOut {
                    from { transform: translateX(0); opacity: 1; }
                    to { transform: translateX(100%); opacity: 0; }
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(-10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                @keyframes progressShrink {
                    from { width: 100%; }
                    to { width: 0%; }
                }
                .toast-container {
                    position: fixed;
                    top: 20px;
                    right: 20px;
                    z-index: 99999;
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                    max-width: 400px;
                    width: 100%;
                }
                .toast {
                    background: white;
                    border-radius: 12px;
                    padding: 16px 20px;
                    display: flex;
                    align-items: flex-start;
                    gap: 12px;
                    box-shadow: 0 10px 40px rgba(0,0,0,0.12), 0 2px 10px rgba(0,0,0,0.08);
                    animation: slideIn 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
                    border-left: 4px solid;
                    position: relative;
                    overflow: hidden;
                }
                .toast.removing {
                    animation: slideOut 0.3s ease forwards;
                }
                .toast-icon {
                    width: 24px;
                    height: 24px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    flex-shrink: 0;
                    font-size: 14px;
                }
                .toast-content {
                    flex: 1;
                    min-width: 0;
                }
                .toast-title {
                    font-weight: 600;
                    font-size: 14px;
                    margin-bottom: 4px;
                    text-transform: capitalize;
                }
                .toast-message {
                    font-size: 13px;
                    color: #64748b;
                    line-height: 1.5;
                    word-wrap: break-word;
                }
                .toast-close {
                    background: none;
                    border: none;
                    color: #94a3b8;
                    font-size: 18px;
                    cursor: pointer;
                    padding: 4px;
                    line-height: 1;
                    border-radius: 6px;
                    transition: all 0.2s;
                    flex-shrink: 0;
                }
                .toast-close:hover {
                    background: #f1f5f9;
                    color: #475569;
                }
                .toast-progress {
                    position: absolute;
                    bottom: 0;
                    left: 0;
                    height: 3px;
                    background: currentColor;
                    opacity: 0.3;
                    animation: progressShrink 5s linear forwards;
                }
                .toast-success { border-left-color: #10b981; }
                .toast-success .toast-icon { background: #d1fae5; color: #059669; }
                .toast-success .toast-title { color: #059669; }
                .toast-error { border-left-color: #ef4444; }
                .toast-error .toast-icon { background: #fee2e2; color: #dc2626; }
                .toast-error .toast-title { color: #dc2626; }
                .toast-warning { border-left-color: #f59e0b; }
                .toast-warning .toast-icon { background: #fef3c7; color: #d97706; }
                .toast-warning .toast-title { color: #d97706; }
                .toast-info { border-left-color: #3b82f6; }
                .toast-info .toast-icon { background: #dbeafe; color: #2563eb; }
                .toast-info .toast-title { color: #2563eb; }
            </style>
        `;
        document.body.appendChild(container);
    }
    
    // Toast types configuration
    const toastConfig = {
        success: { icon: 'fa-check', title: 'Success' },
        error: { icon: 'fa-times', title: 'Error' },
        warning: { icon: 'fa-exclamation', title: 'Warning' },
        info: { icon: 'fa-info-circle', title: 'Info' }
    };
    
    const config = toastConfig[type] || toastConfig.info;
    
    // Create toast element
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <div class="toast-icon">
            <i class="fas ${config.icon}"></i>
        </div>
        <div class="toast-content">
            <div class="toast-title">${config.title}</div>
            <div class="toast-message">${escapeHtml(message)}</div>
        </div>
        <button class="toast-close" onclick="removeToast(this)">&times;</button>
        <div class="toast-progress"></div>
    `;
    
    container.appendChild(toast);
    
    // Remove toast function
    window.removeToast = function(btn) {
        const toast = btn.closest('.toast');
        toast.classList.add('removing');
        setTimeout(() => toast.remove(), 300);
    };
    
    // Auto remove after 5 seconds
    const timeoutId = setTimeout(() => {
        if (toast.parentElement) {
            toast.classList.add('removing');
            setTimeout(() => toast.remove(), 300);
        }
    }, 5000);
    
    // Pause progress bar on hover
    toast.addEventListener('mouseenter', () => {
        clearTimeout(timeoutId);
        toast.querySelector('.toast-progress').style.animationPlayState = 'paused';
    });
    
    toast.addEventListener('mouseleave', () => {
        toast.querySelector('.toast-progress').style.animationPlayState = 'running';
        setTimeout(() => {
            if (toast.parentElement) {
                toast.classList.add('removing');
                setTimeout(() => toast.remove(), 300);
            }
        }, 2000);
    });
}

function showLoading(element) {
    element.innerHTML = '<div class="course-loader"></div><p>Loading...</p>';
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    } catch (e) {
        return 'N/A';
    }
}

function getStatusBadge(status) {
    if (!status) status = 'unknown';
    const statusClass = `status-${status.toLowerCase()}`;
    return `<span class="status-badge ${statusClass}">${status}</span>`;
}

// Course icons mapping
const courseIcons = {
    // Driving courses
    'Defensive Driving Basics': '🚗',
    'Advanced Defensive Driving': '🏎️',
    'Commercial Defensive Driving': '🚌',
    'Refresher Course': '🔄',
    'Teen Defensive Driving': '🎓',
    // Computer courses
    'Computer Basics': '🖥️',
    'Microsoft Office Specialist': '📊',
    'Web Development': '🌐',
    'Python Programming': '🐍',
    'Database Management': '🗄️',
    'Graphic Design': '🎨'
};

function getCourseIcon(courseName) {
    return courseIcons[courseName] || '📚';
}

function getCoursesDisplay(app) {
    if (!app) return 'No course selected';
    let courses = [];
    if (app.driving_course) {
        courses.push(getCourseIcon(app.driving_course) + ' ' + app.driving_course);
    }
    if (app.computer_course) {
        courses.push(getCourseIcon(app.computer_course) + ' ' + app.computer_course);
    }
    return courses.length > 0 ? courses.join('<br>') : 'No course selected';
}

// Load courses preview on home page
async function loadCoursesPreview() {
    const container = document.getElementById('courses-preview');
    if (!container) return;
    
    try {
        const data = await courses.getAll();
        
        if (data.success && data.courses.length > 0) {
            container.innerHTML = '';
            
            // Sort courses by created_at (newest first)
            const sortedCourses = [...data.courses].sort((a, b) => {
                const dateA = new Date(a.created_at || 0);
                const dateB = new Date(b.created_at || 0);
                return dateB - dateA;
            });
            
            // Show first 3 courses with latest tag on the newest one
            sortedCourses.slice(0, 3).forEach((course, index) => {
                const isLatest = index === 0; // First course (newest) gets the "Latest" tag
                const courseCard = createCourseCard(course, isLatest);
                container.appendChild(courseCard);
            });
        } else {
            container.innerHTML = '<p class="text-center">No courses available at the moment.</p>';
        }
    } catch (error) {
        container.innerHTML = '<p class="text-center">Unable to load courses. Please try again later.</p>';
    }
}

// Create course card HTML
function createCourseCard(course, isLatest = false) {
    const card = document.createElement('div');
    card.className = 'course-card';
    const icon = getCourseIcon(course.name);
    
    const latestBadge = isLatest ? '<span class="course-badge"><i class="fas fa-book"></i> Latest</span>' : '';
    
    card.innerHTML = `
        ${latestBadge}
        <div class="course-image">${icon}</div>
        <div class="course-content">
            <h3>${course.name}</h3>
            <p>${course.description}</p>
            <div class="course-meta">
                <span class="course-price">${formatCurrency(course.price)}</span>
                <span class="course-duration">${course.duration_hours} hours</span>
            </div>
            <a href="course-detail.html?id=${course.id}" class="btn btn-outline btn-small" style="width: 100%; margin-top: 15px; text-align: center;">View Details</a>
        </div>
    `;
    return card;
}

// Load all courses on courses page
async function loadAllCourses() {
    const container = document.getElementById('all-courses');
    if (!container) return;
    
    try {
        showLoading(container);
        const data = await courses.getAll();
        
        container.innerHTML = '';
        
        if (data.success && data.courses.length > 0) {
            data.courses.forEach(course => {
                const courseCard = createCourseCard(course);
                container.appendChild(courseCard);
            });
        } else {
            container.innerHTML = '<p class="text-center">No courses available at the moment.</p>';
        }
    } catch (error) {
        container.innerHTML = '<p class="text-center">Unable to load courses. Please try again later.</p>';
    }
}

// Load course detail
async function loadCourseDetail() {
    const urlParams = new URLSearchParams(window.location.search);
    const courseId = urlParams.get('id');
    
    if (!courseId) {
        window.location.href = 'courses.html';
        return;
    }
    
    const container = document.getElementById('course-detail');
    if (!container) return;
    
    try {
        const data = await courses.getById(courseId);
        
        if (data.success && data.course) {
            const course = data.course;
            const requirements = course.requirements.split(',');
            
            // Determine course type and set appropriate features
            const isDrivingCourse = course.name.toLowerCase().includes('driving') || 
                                    course.name.toLowerCase().includes('license') ||
                                    course.category === 'driving';
            
            const features = isDrivingCourse ? [
                'Certified instructors',
                'Modern training vehicles',
                'Hands-on driving practice',
                'Flexible scheduling',
                'Certificate upon completion'
            ] : [
                'Hands-on programming projects',
                'Industry-standard software tools',
                'Expert-led training sessions',
                'Flexible learning schedule',
                'Certificate upon completion'
            ];
            
            container.innerHTML = `
                <div class="course-detail-grid">
                    <div class="course-info">
                        <h1>${course.name}</h1>
                        <div class="course-meta-info">
                            <div class="meta-item">
                                <span>⏱️</span>
                                <span>${course.duration_hours} Hours</span>
                            </div>
                            <div class="meta-item">
                                <span>💰</span>
                                <span>${formatCurrency(course.price)}</span>
                            </div>
                            <div class="meta-item">
                                <span>📚</span>
                                <span>${isDrivingCourse ? 'Driving' : 'Computer'} Certified</span>
                            </div>
                        </div>
                        <div class="course-description">
                            <p>${course.description}</p>
                        </div>
                        <div class="course-requirements">
                            <h3>Requirements</h3>
                            <ul>
                                ${requirements.map(req => `<li>${req.trim()}</li>`).join('')}
                            </ul>
                        </div>
                    </div>
                    <div class="enroll-card">
                        <div class="enroll-price">${formatCurrency(course.price)}</div>
                        <div class="enroll-duration">${course.duration_hours} hours of ${isDrivingCourse ? 'driving' : 'computer'} training</div>
                        <a href="apply.html?course=${course.id}" class="btn btn-primary" style="width: 100%; text-align: center;">Apply Now</a>
                        <ul class="enroll-features">
                            ${features.map(feature => `<li>${feature}</li>`).join('')}
                        </ul>
                    </div>
                </div>
            `;
        } else {
            container.innerHTML = '<p class="text-center">Course not found.</p>';
        }
    } catch (error) {
        container.innerHTML = '<p class="text-center">Unable to load course details.</p>';
    }
}

// Load user applications on dashboard
async function loadUserApplications() {
    const container = document.getElementById('my-applications');
    if (!container) return;
    
    try {
        showLoading(container);
        const data = await applications.getMy();
        
        container.innerHTML = '';
        
        // Check if data is valid
        if (!data || !data.success || !data.applications) {
            container.innerHTML = '<p class="text-center">Unable to load applications. Please refresh the page.</p>';
            return;
        }
        
        if (data.applications.length > 0) {
            let tableHtml = `
                <table class="applications-table">
                    <thead>
                        <tr>
                            <th>Course</th>
                            <th>Applied Date</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            
            data.applications.forEach(app => {
                tableHtml += `
                    <tr>
                        <td>${getCoursesDisplay(app)}</td>
                        <td>${formatDate(app.created_at)}</td>
                        <td>${getStatusBadge(app.status)}</td>
                    </tr>
                `;
            });
            
            tableHtml += '</tbody></table>';
            container.innerHTML = tableHtml;
        } else {
            container.innerHTML = '<p class="text-center">No applications yet. <a href="courses.html">Browse courses</a> and apply!</p>';
        }
    } catch (error) {
        console.error('Failed to load applications:', error);
        // Check if token expired or auth error
        if (error.message && (error.message.includes('Token expired') || error.message.includes('401') || error.message.includes('Invalid token'))) {
            container.innerHTML = '<p class="text-center">Your session has expired. <a href="login.html">Please login again</a>.</p>';
        } else if (error.message && error.message.includes('403')) {
            container.innerHTML = '<p class="text-center">Access denied. Please contact an administrator.</p>';
        } else {
            container.innerHTML = '<p class="text-center">Unable to load your applications. Please <a href="javascript:location.reload()">refresh</a> the page or <a href="login.html">login again</a>.</p>';
        }
    }
}

// Load all applications (for admin)
async function loadAllApplications() {
    const container = document.getElementById('all-applications');
    if (!container) return;
    
    try {
        showLoading(container);
        const data = await applications.getAll();
        
        container.innerHTML = '';
        
        // Check if data is valid
        if (!data || !data.success || !data.applications) {
            container.innerHTML = '<p class="text-center">Unable to load applications. Please refresh the page.</p>';
            return;
        }
        
        if (data.applications.length > 0) {
            let tableHtml = `
                <table class="applications-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Course</th>
                            <th>Applied Date</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            
            data.applications.forEach(app => {
                tableHtml += `
                    <tr>
                        <td>${app.id}</td>
                        <td>${app.first_name} ${app.last_name}</td>
                        <td>${app.email}</td>
                        <td>${getCoursesDisplay(app)}</td>
                        <td>${formatDate(app.created_at)}</td>
                        <td>${getStatusBadge(app.status)}</td>
                        <td>
                            <select onchange="updateApplicationStatus(${app.id}, this.value)" style="padding: 5px; border-radius: 4px;">
                                <option value="pending" ${app.status === 'pending' ? 'selected' : ''}>Pending</option>
                                <option value="approved" ${app.status === 'approved' ? 'selected' : ''}>Approved</option>
                                <option value="rejected" ${app.status === 'rejected' ? 'selected' : ''}>Rejected</option>
                            </select>
                        </td>
                    </tr>
                `;
            });
            
            tableHtml += '</tbody></table>';
            container.innerHTML = tableHtml;
        } else {
            container.innerHTML = '<p class="text-center">No applications yet.</p>';
        }
    } catch (error) {
        container.innerHTML = '<p class="text-center">Unable to load applications.</p>';
    }
}

// Update application status (admin)
async function updateApplicationStatus(id, status) {
    try {
        await applications.updateStatus(id, status);
        showAlert('Application status updated successfully', 'success');
    } catch (error) {
        showAlert('Failed to update status: ' + error.message, 'error');
    }
}

// Handle login form submission
async function handleLoginSubmit(event) {
    event.preventDefault();
    
    const form = event.target;
    const email = form.querySelector('[name="email"]').value;
    const password = form.querySelector('[name="password"]').value;
    
    try {
        await auth.login(email, password);
        showAlert('Login successful!', 'success');
        
        // Redirect based on user role
        const user = auth.getUser();
        const redirectUrl = getDashboardUrlByRole(user);
        
        setTimeout(() => {
            window.location.href = redirectUrl;
        }, 1000);
    } catch (error) {
        showAlert(error.message, 'error');
    }
}

// Handle register form submission
async function handleRegisterSubmit(event) {
    event.preventDefault();
    
    const form = event.target;
    const formData = {
        full_name: form.querySelector('[name="full_name"]').value,
        email: form.querySelector('[name="email"]').value,
        phone: form.querySelector('[name="phone"]').value,
        password: form.querySelector('[name="password"]').value
    };
    
    if (formData.password !== form.querySelector('[name="confirm_password"]').value) {
        showAlert('Passwords do not match', 'error');
        return;
    }
    
    try {
        await auth.register(formData);
        showAlert('Registration successful!', 'success');
        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 1000);
    } catch (error) {
        showAlert(error.message, 'error');
    }
}

// Handle application form submission
async function handleApplicationSubmit(event) {
    event.preventDefault();
    
    if (!auth.isLoggedIn()) {
        showAlert('Please login to submit an application', 'error');
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 1500);
        return;
    }
    
    const form = event.target;
    const formData = {
        first_name: form.querySelector('[name="first_name"]').value,
        last_name: form.querySelector('[name="last_name"]').value,
        email: form.querySelector('[name="email"]').value,
        phone: form.querySelector('[name="phone"]').value,
        date_of_birth: form.querySelector('[name="date_of_birth"]').value,
        address: form.querySelector('[name="address"]').value,
        city: form.querySelector('[name="city"]').value,
        postal_code: form.querySelector('[name="postal_code"]').value,
        id_number: form.querySelector('[name="id_number"]').value,
        license_type: form.querySelector('[name="license_type"]').value,
        driving_course: form.querySelector('[name="driving_course"]').value,
        computer_course: form.querySelector('[name="computer_course"]').value,
        preferred_start: form.querySelector('[name="preferred_start"]').value,
        training_location: form.querySelector('[name="location"]').value,
        transmission: form.querySelector('[name="transmission"]').value,
        preferred_schedule: form.querySelector('[name="preferred_schedule"]').value,
        emergency_contact_name: form.querySelector('[name="emergency_contact_name"]').value,
        emergency_contact_phone: form.querySelector('[name="emergency_contact_phone"]').value,
        medical_conditions: form.querySelector('[name="medical_conditions"]').value,
        comments: form.querySelector('[name="comments"]').value,
        previous_driving_experience: form.querySelector('[name="previous_driving_experience"]')?.checked ? 'on' : ''
    };
    
    try {
        const data = await applications.submit(formData);
        showAlert('Application submitted successfully! Application ID: ' + data.application_id, 'success');
        form.reset();
        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 2000);
    } catch (error) {
        showAlert(error.message, 'error');
    }
}

// Pre-fill form with user data
function prefillUserData() {
    const user = auth.getUser();
    if (!user) return;
    
    const emailInputs = document.querySelectorAll('[name="email"]');
    emailInputs.forEach(input => input.value = user.email || '');
    
    const fullNameInputs = document.querySelectorAll('[name="full_name"]');
    fullNameInputs.forEach(input => input.value = user.full_name || '');
    
    const phoneInputs = document.querySelectorAll('[name="phone"]');
    phoneInputs.forEach(input => input.value = user.phone || '');
}

// Toggle user dropdown menu
function toggleUserDropdown() {
    const dropdown = document.getElementById('user-dropdown');
    if (dropdown) {
        dropdown.classList.toggle('active');
    }
}

// Close dropdown when clicking outside
document.addEventListener('click', function(event) {
    const dropdown = document.getElementById('user-dropdown');
    const avatar = document.querySelector('.user-avatar');
    
    if (dropdown && avatar && !avatar.contains(event.target) && !dropdown.contains(event.target)) {
        dropdown.classList.remove('active');
    }
});

// Mobile menu toggle
function toggleMobileMenu() {
    const navLinks = document.querySelector('.nav-links');
    const menuBtn = document.querySelector('.mobile-menu-btn');
    if (navLinks) {
        navLinks.classList.toggle('mobile-open');
    }
    if (menuBtn) {
        menuBtn.classList.toggle('active');
    }
}

// Switch auth tabs
function switchAuthTab(tab) {
    document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.auth-form').forEach(f => f.classList.remove('active'));
    
    tab.classList.add('active');
    const formId = tab.dataset.form;
    document.getElementById(formId).classList.add('active');
}

// Toggle user dropdown menu
function toggleUserDropdown() {
    const dropdown = document.getElementById('user-dropdown');
    if (dropdown) {
        dropdown.classList.toggle('active');
    }
}

// Close dropdown when clicking outside
document.addEventListener('click', function(event) {
    const dropdown = document.getElementById('user-dropdown');
    const avatar = document.querySelector('.user-avatar');
    
    if (dropdown && avatar && !avatar.contains(event.target) && !dropdown.contains(event.target)) {
        dropdown.classList.remove('active');
    }
});

// Get dashboard URL based on user role
function getDashboardUrlByRole(user) {
    if (!user) return 'login.html';
    
    const role = (user.role || '').toLowerCase();
    
    switch (role) {
        case 'admin':
            return 'admin.html';
        case 'staff':
            return 'staff.html';
        case 'student':
        case 'applicant':
        case 'user':
        default:
            return 'dashboard.html';
    }
}

// Protect routes
function requireAuth(redirectTo = 'login.html') {
    if (!auth.isLoggedIn()) {
        window.location.href = redirectTo;
        return false;
    }
    return true;
}

function requireAdmin() {
    const user = auth.getUser();
    if (!user || user.role !== 'admin') {
        window.location.href = 'dashboard.html';
        return false;
    }
    return true;
}

// Load all users (admin)
async function loadAllUsers() {
    const container = document.getElementById('all-users');
    if (!container) return;
    
    try {
        showLoading(container);
        const data = await users.getAll();
        
        container.innerHTML = '';
        
        if (data.success && data.users.length > 0) {
            let tableHtml = `
                <table class="applications-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Phone</th>
                            <th>Role</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            
            data.users.forEach(user => {
                tableHtml += `
                    <tr>
                        <td>${user.id}</td>
                        <td>${user.full_name || 'N/A'}</td>
                        <td>${user.email}</td>
                        <td>${user.phone || 'N/A'}</td>
                        <td><span class="status-badge status-${user.role === 'admin' ? 'approved' : 'pending'}">${user.role}</span></td>
                        <td>
                            <div class="action-buttons">
                                <button class="btn btn-small btn-outline" onclick="openUserModal(${user.id}, '${user.full_name || ''}', '${user.email}', '${user.phone || ''}', '${user.role}')">
                                    <i class="fas fa-edit"></i> Edit
                                </button>
                                ${user.role !== 'admin' ? `
                                <button class="btn btn-small btn-danger" onclick="deleteUser(${user.id})">
                                    <i class="fas fa-trash"></i>
                                </button>
                                ` : ''}
                            </div>
                        </td>
                    </tr>
                `;
            });
            
            tableHtml += '</tbody></table>';
            container.innerHTML = tableHtml;
        } else {
            container.innerHTML = '<p class="text-center">No users found.</p>';
        }
    } catch (error) {
        console.error('Failed to load users:', error);
        container.innerHTML = '<p class="text-center">Unable to load users.</p>';
    }
}

// Open user edit modal
function openUserModal(id, fullName, email, phone, role) {
    document.getElementById('user-id').value = id;
    document.getElementById('user-fullname').value = fullName;
    document.getElementById('user-email').value = email;
    document.getElementById('user-phone').value = phone;
    document.getElementById('user-password').value = '';
    document.getElementById('user-role').value = role;
    document.getElementById('user-modal').classList.add('show');
}

// Close user modal
function closeUserModal() {
    document.getElementById('user-modal').classList.remove('show');
    document.getElementById('user-form').reset();
}

// Handle user form submission (admin)
async function handleUserSubmit(event) {
    event.preventDefault();
    
    const form = event.target;
    const userId = form.querySelector('[name="user_id"]').value;
    const formData = {
        full_name: form.querySelector('[name="full_name"]').value,
        email: form.querySelector('[name="email"]').value,
        phone: form.querySelector('[name="phone"]').value,
        password: form.querySelector('[name="password"]').value,
        role: form.querySelector('[name="role"]').value
    };
    
    // Remove empty password to keep current one
    if (!formData.password) {
        delete formData.password;
    }
    
    try {
        await users.update(userId, formData);
        showAlert('User updated successfully', 'success');
        closeUserModal();
        loadAllUsers();
    } catch (error) {
        showAlert('Failed to update user: ' + error.message, 'error');
    }
}

// Delete user (admin)
async function deleteUser(id) {
    if (!confirm('Are you sure you want to delete this user?')) {
        return;
    }
    
    try {
        await users.delete(id);
        showAlert('User deleted successfully', 'success');
        loadAllUsers();
    } catch (error) {
        showAlert('Failed to delete user: ' + error.message, 'error');
    }
}

// Handle profile update (user dashboard)
async function handleProfileUpdate(event) {
    event.preventDefault();
    
    const form = event.target;
    const user = auth.getUser();
    
    if (!user) {
        showAlert('Please login to update your profile', 'error');
        return;
    }
    
    const formData = {
        full_name: form.querySelector('[name="full_name"]').value,
        email: form.querySelector('[name="email"]').value,
        phone: form.querySelector('[name="phone"]').value,
        password: form.querySelector('[name="password"]').value
    };
    
    // Remove empty password to keep current one
    if (!formData.password) {
        delete formData.password;
    }
    
    try {
        await users.update(user.id, formData);
        
        // Update local storage with new user data
        const updatedUser = { ...user, ...formData };
        localStorage.setItem(USER_KEY, JSON.stringify(updatedUser));
        
        showAlert('Profile updated successfully', 'success');
        closeProfileModal();
    } catch (error) {
        showAlert('Failed to update profile: ' + error.message, 'error');
    }
}

// Handle contact form submission
async function handleContactSubmit(event) {
    event.preventDefault();
    
    const form = event.target;
    const submitBtn = form.querySelector('button[type="submit"]');
    const originalBtnText = submitBtn.innerHTML;
    
    // Show loading state
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';
    submitBtn.disabled = true;
    
    const formData = {
        name: form.querySelector('[name="name"]').value,
        email: form.querySelector('[name="email"]').value,
        phone: form.querySelector('[name="phone"]').value,
        subject: form.querySelector('[name="subject"]').value,
        message: form.querySelector('[name="message"]').value
    };
    
    try {
        const data = await contacts.submit(formData);
        
        if (data.success) {
            showAlert('Message sent successfully! We\'ll get back to you soon.', 'success');
            form.reset();
        } else {
            showAlert(data.error || 'Failed to send message', 'error');
        }
    } catch (error) {
        showAlert('Failed to send message: ' + error.message, 'error');
    } finally {
        submitBtn.innerHTML = originalBtnText;
        submitBtn.disabled = false;
    }
}

// Staff Applicants Functions for Staff Dashboard
let staffApplicantsData = [];

let staffAllUsersData = [];

async function loadStaffApplicants() {
    const container = document.getElementById('staff-applicants-list');
    if (!container) return;
    
    container.innerHTML = '<div class="course-card" style="text-align: center; padding: 60px 20px;"><div class="course-loader"></div><p style="margin-top: 16px; color: var(--text-light);">Loading applicants...</p></div>';
    
    try {
        // Get auth token
        const token = localStorage.getItem(TOKEN_KEY);
        if (!token) {
            throw new Error('Not logged in');
        }
        
        // Fetch applicants filtered by staff location from database
        const response = await fetch('/api/staff/applicants', {
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Error response:', errorText);
            throw new Error('HTTP ' + response.status + ': ' + errorText);
        }
        
        const data = await response.json();
        
        if (data.success) {
            staffApplicantsData = data.applicants || [];
            renderStaffApplicants(staffApplicantsData);
            
            // Update stats
            const pendingCount = staffApplicantsData.filter(a => a.status === 'pending').length;
            const approvedCount = staffApplicantsData.filter(a => a.status === 'approved').length;
            
            const totalApplicantsEl = document.getElementById('staff-total-applicants');
            const pendingApplicantsEl = document.getElementById('staff-pending-applicants');
            const approvedApplicantsEl = document.getElementById('staff-approved-applicants');
            
            if (totalApplicantsEl) totalApplicantsEl.textContent = staffApplicantsData.length;
            if (pendingApplicantsEl) pendingApplicantsEl.textContent = pendingCount;
            if (approvedApplicantsEl) approvedApplicantsEl.textContent = approvedCount;
            
            // Calculate totals from applicants
            const totalFees = staffApplicantsData.reduce((sum, a) => sum + (a.school_fees || 0), 0);
            const totalPaid = staffApplicantsData.reduce((sum, a) => sum + (a.fees_paid || 0), 0);
            
            const totalFeesEl = document.getElementById('staff-total-fees');
            const totalPaidEl = document.getElementById('staff-total-paid');
            
            if (totalFeesEl) totalFeesEl.textContent = formatNumber(totalFees);
            if (totalPaidEl) totalPaidEl.textContent = formatNumber(totalPaid);
            
            // Update total students count
            const totalStudentsEl = document.getElementById('staff-total-students');
            if (totalStudentsEl) totalStudentsEl.textContent = staffApplicantsData.length;
        } else {
            container.innerHTML = '<div class="course-card" style="text-align: center; padding: 60px 20px;"><p style="color: var(--text-light);">No applicants found at your location.</p></div>';
        }
    } catch (error) {
        console.error('Error loading staff applicants:', error);
        container.innerHTML = '<div class="course-card" style="text-align: center; padding: 60px 20px;"><p style="color: var(--text-light);">Error: ' + error.message + '</p><p style="color: var(--text-light); margin-top: 10px;">Please make sure you are logged in and have staff access.</p></div>';
    }
}

function renderStaffApplicants(applicants) {
    const container = document.getElementById('staff-applicants-list');
    if (!container) return;
    
    if (!applicants || applicants.length === 0) {
        container.innerHTML = '<div class="course-card" style="text-align: center; padding: 60px 20px;"><p style="color: var(--text-light);">No applicants found.</p></div>';
        return;
    }
    
    container.innerHTML = '';
    
    applicants.forEach(applicant => {
        const card = document.createElement('div');
        card.className = 'course-card';
        
        const fullName = applicant.first_name && applicant.last_name 
            ? `${applicant.first_name} ${applicant.last_name}` 
            : applicant.full_name || applicant.user_full_name || 'Unknown';
        
        const drivingCourse = applicant.driving_course || applicant.computer_course || 'N/A';
        const status = applicant.status || 'pending';
        const paymentStatus = applicant.payment_status || 'unpaid';
        const location = applicant.training_location || 'N/A';
        const phone = applicant.phone || 'N/A';
        const email = applicant.email || applicant.user_email || 'N/A';
        
        card.innerHTML = `
            <div class="course-header">
                <h3 style="margin: 0; font-size: 18px;">${fullName}</h3>
                ${getStatusBadge(status)}
            </div>
            <div class="course-content">
                <p><strong>📍 Location:</strong> ${location}</p>
                <p><strong>🚗 Course:</strong> ${drivingCourse}</p>
                <p><strong>📞 Phone:</strong> ${phone}</p>
                <p><strong>📧 Email:</strong> ${email}</p>
                <p><strong>💰 Fees:</strong> KSh ${formatNumber(applicant.school_fees || 0)}</p>
                <p><strong>📊 Status:</strong> <span class="status-badge status-${paymentStatus}">${paymentStatus}</span></p>
                <p style="font-size: 12px; color: var(--text-light); margin-top: 10px;">
                    <strong>📅 Applied:</strong> ${formatDate(applicant.created_at)}
                </p>
                <div style="margin-top: 15px; display: flex; gap: 10px;">
                    <button onclick="openApplicantModal(${applicant.user_id || applicant.id})" class="btn btn-small"><i class="fas fa-edit"></i> Edit</button>
                </div>
            </div>
        `;
        
        container.appendChild(card);
    });
}

function searchApplicants() {
    const searchTerm = document.getElementById('applicant-search')?.value.toLowerCase().trim() || '';
    
    if (!searchTerm) {
        renderStaffApplicants(staffApplicantsData);
        return;
    }
    
    const filtered = staffApplicantsData.filter(applicant => {
        const fullName = applicant.first_name && applicant.last_name 
            ? `${applicant.first_name} ${applicant.last_name}`.toLowerCase()
            : (applicant.full_name || applicant.user_full_name || '').toLowerCase();
        const email = (applicant.email || applicant.user_email || '').toLowerCase();
        const phone = applicant.phone || '';
        const course = (applicant.driving_course || applicant.computer_course || '').toLowerCase();
        
        return fullName.includes(searchTerm) || 
               email.includes(searchTerm) || 
               phone.includes(searchTerm) || 
               course.includes(searchTerm);
    });
    
    renderStaffApplicants(filtered);
}

function viewApplicantDetails(applicantId) {
    // Find the applicant data
    const applicant = staffApplicantsData.find(a => a.id === applicantId);
    if (!applicant) {
        showAlert('Applicant not found', 'error');
        return;
    }
    
    // Show the applicant details modal
    showApplicantModal(applicant);
}

function showApplicantModal(applicant) {
    // Create modal if it doesn't exist
    let modal = document.getElementById('applicant-details-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'applicant-details-modal';
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 600px;">
                <div class="modal-header">
                    <h2>Applicant Details</h2>
                    <span class="close" onclick="closeApplicantModal()">&times;</span>
                </div>
                <div class="modal-body" id="applicant-modal-body"></div>
                <div class="modal-footer">
                    <button onclick="closeApplicantModal()" class="btn">Close</button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
    }
    
    const fullName = applicant.first_name && applicant.last_name 
        ? `${applicant.first_name} ${applicant.last_name}` 
        : applicant.full_name || 'Unknown';
    
    const modalBody = document.getElementById('applicant-modal-body');
    modalBody.innerHTML = `
        <div style="margin-bottom: 20px;">
            <h3 style="margin-bottom: 15px; border-bottom: 1px solid #eee; padding-bottom: 10px;">${fullName}</h3>
            ${getStatusBadge(applicant.status || 'pending')}
            <span class="status-badge status-${applicant.payment_status || 'unpaid'}" style="margin-left: 10px;">${applicant.payment_status || 'unpaid'}</span>
        </div>
        
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
            <div>
                <p><strong>📍 Training Location:</strong></p>
                <p>${applicant.location || 'N/A'}</p>
            </div>
            <div>
                <p><strong>📅 Applied Date:</strong></p>
                <p>${formatDate(applicant.created_at)}</p>
            </div>
        </div>
        
        <div style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
            <h4 style="margin-top: 0; margin-bottom: 10px;">🚗 Driving Course</h4>
            <p><strong>Course:</strong> ${applicant.driving_course || 'N/A'}</p>
            <p><strong>License Type:</strong> ${applicant.license_type || 'N/A'}</p>
            <p><strong>Transmission:</strong> ${applicant.transmission || 'N/A'}</p>
            <p><strong>Schedule:</strong> ${applicant.preferred_schedule || 'N/A'}</p>
            ${applicant.previous_driving_experience ? '<p><span class="status-badge status-approved">Has Experience</span></p>' : ''}
        </div>
        
        ${applicant.computer_course ? `
        <div style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
            <h4 style="margin-top: 0; margin-bottom: 10px;">💻 Computer Course</h4>
            <p><strong>Course:</strong> ${applicant.computer_course}</p>
        </div>
        ` : ''}
        
        <div style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
            <h4 style="margin-top: 0; margin-bottom: 10px;">📞 Contact Information</h4>
            <p><strong>Phone:</strong> ${applicant.phone || 'N/A'}</p>
            <p><strong>Email:</strong> ${applicant.email || 'N/A'}</p>
            ${applicant.emergency_contact_name ? `
            <p><strong>Emergency Contact:</strong> ${applicant.emergency_contact_name} (${applicant.emergency_contact_phone || 'N/A'})</p>
            ` : ''}
        </div>
        
        <div style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
            <h4 style="margin-top: 0; margin-bottom: 10px;">💰 Fees</h4>
            <p><strong>School Fees:</strong> ${applicant.school_fees ? formatCurrency(applicant.school_fees) : 'N/A'}</p>
            <p><strong>Fees Paid:</strong> ${applicant.fees_paid ? formatCurrency(applicant.fees_paid) : 'N/A'}</p>
            <p><strong>Balance:</strong> ${applicant.fees_balance ? formatCurrency(applicant.fees_balance) : 'N/A'}</p>
        </div>
        
        ${applicant.address ? `
        <div style="margin-bottom: 20px;">
            <p><strong>📍 Address:</strong></p>
            <p>${applicant.address}</p>
        </div>
        ` : ''}
        
        ${applicant.comments ? `
        <div style="margin-bottom: 20px;">
            <p><strong>📝 Comments:</strong></p>
            <p>${applicant.comments}</p>
        </div>
        ` : ''}
    `;
    
    modal.style.display = 'block';
}

function closeApplicantModal() {
    const modal = document.getElementById('applicant-details-modal');
    if (modal) {
        modal.style.display = 'none';
    }
}

// Initialize contact form if present
document.addEventListener('DOMContentLoaded', function() {
    const contactForm = document.getElementById('contactForm');
    if (contactForm) {
        contactForm.addEventListener('submit', handleContactSubmit);
    }
    
    // Load courses preview on home page
    loadCoursesPreview();
});
