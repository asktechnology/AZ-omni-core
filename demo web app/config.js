// ==================================================
// Configuration
// ==================================================

const CONFIG = {
    // API Configuration
    API_BASE_URL: 'http://10.113.10.86:8040/api/v1',
    DEFAULT_LANGUAGE: 'en',

    // Status Check Configuration
    STATUS_CHECK_INTERVAL: 3000, // 3 seconds
    MAX_STATUS_CHECK_ATTEMPTS: 20, // Max 1 minute (20 * 3 seconds)

    // Image Fallbacks
    IMAGES: {
        FALLBACK_CATEGORY: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"%3E%3Crect width="100" height="100" fill="%23e0e0e0"/%3E%3Ctext x="50" y="50" font-family="Arial" font-size="14" fill="%23666" text-anchor="middle" dominant-baseline="middle"%3ECategory%3C/text%3E%3C/svg%3E',
        FALLBACK_BILLER: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"%3E%3Crect width="100" height="100" fill="%23e8f5e9"/%3E%3Ctext x="50" y="50" font-family="Arial" font-size="14" fill="%234caf50" text-anchor="middle" dominant-baseline="middle"%3EBiller%3C/text%3E%3C/svg%3E',
        FALLBACK_SERVICE: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"%3E%3Crect width="100" height="100" fill="%23e3f2fd"/%3E%3Ctext x="50" y="50" font-family="Arial" font-size="14" fill="%232196f3" text-anchor="middle" dominant-baseline="middle"%3EService%3C/text%3E%3C/svg%3E'
    },

    // Receipt Color Codes
    RECEIPT_COLORS: {
        SUCCESS: 'green',    // responseCode = 0
        PENDING: 'orange',   // responseCode = 20
        FAILED: 'red'        // responseCode not in [0, 20]
    },

    // Response Codes
    RESPONSE_CODES: {
        SUCCESS: 0,
        PENDING: 20
    }
};
