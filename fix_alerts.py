# Script to replace alert() with showToast() in admin.html

with open('web/admin.html', 'r') as f:
    content = f.read()

# Replace all alert() with showToast()
replacements = [
    ("alert('Error: ' + error.message)", "showToast('Error: '+ error.message, 'error')"),
    ("alert('User deleted successfully!')", "showToast('User deleted successfully!', 'success')"),
    ("alert('Error: ' + data.error)", "showToast('Error: '+ data.error, 'error')"),
    ("alert('Please enter phone number and amount')", "showToast('Please enter phone number and amount', 'warning')"),
    ("alert('No application found for this user')", "showToast('No application found for this user', 'error')"),
    ("alert('Course status updated successfully!')", "showToast('Course status updated successfully!', 'success')"),
    ("alert('Error: ' + (data.error || 'Failed to update status'))", "showToast('Error: '+ (data.error || 'Failed to update status'), 'error')"),
    ("alert('Fees details saved successfully!')", "showToast('Fees details saved successfully!', 'success')"),
    ("alert('Error: ' + (data.error || 'Failed to save fees details'))", "showToast('Error: '+ (data.error || 'Failed to save fees details'), 'error')"),
    ("alert('Failed to load application: ' + (data.error || 'Unknown error'))", "showToast('Failed to load application: '+ (data.error || 'Unknown error'), 'error')"),
    ("alert('Error loading application: ' + error.message)", "showToast('Error loading application: '+ error.message, 'error')"),
    ("alert('Application updated successfully!')", "showToast('Application updated successfully!', 'success')"),
    ("alert('Error: ' + (data.error || 'Failed to update application'))", "showToast('Error: '+ (data.error || 'Failed to update application'), 'error')"),
    ("alert(message)", "showToast(message, 'info')"),
    ("alert('Error: ' + (data.error || 'Failed to save staff'))", "showToast('Error: '+ (data.error || 'Failed to save staff'), 'error')"),
    ("alert('Application not found')", "showToast('Application not found', 'error')"),
]

for old, new in replacements:
    content = content.replace(old, new)

with open('web/admin.html', 'w') as f:
    f.write(content)

print('Done replacing alerts in admin.html')
