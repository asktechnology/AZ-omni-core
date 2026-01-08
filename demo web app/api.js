// ==================================================
// API Client Module
// ==================================================

class PaymentAPI {
    constructor(baseURL, language = 'en') {
        this.client = axios.create({
            baseURL: baseURL,
            headers: {
                'Content-Type': 'application/json',
                'language': language
            },
            timeout: 30000
        });

        // Add response interceptor for error handling
        this.client.interceptors.response.use(
            response => response,
            error => this.handleError(error)
        );
    }

    // ==================================================
    // Language Management
    // ==================================================

    setLanguage(lang) {
        this.client.defaults.headers.language = lang;
    }

    // ==================================================
    // Category Endpoints
    // ==================================================

    async getCategories() {
        try {
            const response = await this.client.get('/category');
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    async getCategoryById(categoryId) {
        try {
            const response = await this.client.get(`/category/${categoryId}`);
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    // ==================================================
    // Biller Endpoints
    // ==================================================

    async getBillersByCategory(categoryId) {
        try {
            const response = await this.client.get(`/category/billerByCategoryId/${categoryId}`);
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    async getBillerById(billerId) {
        try {
            const response = await this.client.get(`/biller/${billerId}`);
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    // ==================================================
    // Service Endpoints
    // ==================================================

    async getServicesByBiller(billerId) {
        try {
            const response = await this.client.get(`/biller/servicesByBillerId/${billerId}`);
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    async getServiceById(serviceId) {
        try {
            const response = await this.client.get(`/service/${serviceId}`);
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    // ==================================================
    // Parameter Endpoints
    // ==================================================

    async getServiceParameters(serviceId) {
        try {
            const response = await this.client.get(`/service/parameterByServiceId/${serviceId}`);
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    // ==================================================
    // Payment Processing Endpoints
    // ==================================================

    async processPayment(processRequest) {
        try {
            const response = await this.client.post('/payment/process', processRequest);
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    async postCheckPayment(postCheckRequest) {
        try {
            const response = await this.client.post('/payment/postCheck', postCheckRequest);
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    async checkTransactionStatus(transactionId) {
        try {
            const response = await this.client.post('/payment/checkStatus', {
                transactionId: transactionId
            });
            return response.data;
        } catch (error) {
            throw this.handleError(error);
        }
    }

    // ==================================================
    // Error Handler
    // ==================================================

    handleError(error) {
        if (axios.isAxiosError(error)) {
            if (error.response) {
                // Server responded with error
                const errorData = error.response.data;
                console.error('API Error:', errorData);
                return {
                    success: false,
                    responseCode: errorData.responseCode || error.response.status,
                    responseMessage: errorData.responseMessage || error.message,
                    data: null
                };
            } else if (error.request) {
                // Request made but no response
                console.error('Network Error: No response from server');
                return {
                    success: false,
                    responseCode: 0,
                    responseMessage: 'Network error: Unable to connect to server. Please ensure the backend is running on localhost:8040',
                    data: null
                };
            }
        }

        // Unknown error
        console.error('Unexpected Error:', error);
        return {
            success: false,
            responseCode: 0,
            responseMessage: error.message || 'An unexpected error occurred',
            data: null
        };
    }
}

// ==================================================
// Export Singleton Instance
// ==================================================

const api = new PaymentAPI(CONFIG.API_BASE_URL, CONFIG.DEFAULT_LANGUAGE);
