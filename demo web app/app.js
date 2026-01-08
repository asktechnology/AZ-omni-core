// ==================================================
// Application State
// ==================================================

const AppState = {
    currentView: 'categories',
    language: 'en',
    breadcrumb: ['Categories'],
    selectedCategory: null,
    selectedBiller: null,
    selectedService: null,
    serviceParameters: [],
    formData: {},
    paymentResponse: null,
    firstStepResponse: null, // For multi-step services
    transactionId: null,
    statusCheckInterval: null,
    pollCount: 0
};

// ==================================================
// UI Helper Functions
// ==================================================

const UI = {
    showLoading() {
        document.getElementById('loading').classList.add('active');
        document.getElementById('content').style.display = 'none';
    },

    hideLoading() {
        document.getElementById('loading').classList.remove('active');
        document.getElementById('content').style.display = 'block';
    },

    showModal(modalId) {
        document.getElementById(modalId).classList.add('active');
    },

    hideModal(modalId) {
        document.getElementById(modalId).classList.remove('active');
    },

    updateBreadcrumb(items) {
        const breadcrumb = document.getElementById('breadcrumb');
        breadcrumb.innerHTML = items.map((item, index) => `
            <span class="breadcrumb-item ${index === items.length - 1 ? 'active' : ''}"
                  data-index="${index}">
                ${item}
            </span>
        `).join('');

        // Add click handlers to breadcrumb items
        breadcrumb.querySelectorAll('.breadcrumb-item:not(.active)').forEach(item => {
            item.addEventListener('click', () => {
                const index = parseInt(item.dataset.index);
                BreadcrumbHandler.navigateToLevel(index);
            });
        });
    },

    showError(message) {
        const content = document.getElementById('content');
        content.innerHTML = `
            <div class="alert alert-error">
                <strong>Error:</strong> ${message}
            </div>
            <button class="btn btn-primary" onclick="location.reload()">Refresh</button>
        `;
    },

    showSuccess(message) {
        const content = document.getElementById('content');
        content.innerHTML = `
            <div class="alert alert-success">
                ${message}
            </div>
        `;
    },

    renderButtonGrid(items, onClickCallback, type = 'category') {
        const fallbackImage = CONFIG.IMAGES[`FALLBACK_${type.toUpperCase()}`] || CONFIG.IMAGES.FALLBACK_CATEGORY;

        return `
            <div class="button-grid">
                ${items.map(item => `
                    <button class="item-button" data-id="${item.id}">
                        <img src="${item.imageUrl || fallbackImage}"
                             alt="${item.name}"
                             onerror="this.src='${fallbackImage}'">
                        <span class="item-name">${item.name}</span>
                    </button>
                `).join('')}
            </div>
        `;
    }
};

// ==================================================
// Breadcrumb Handler
// ==================================================

const BreadcrumbHandler = {
    navigateToLevel(level) {
        switch (level) {
            case 0:
                // Back to categories
                AppState.breadcrumb = ['Categories'];
                AppState.selectedCategory = null;
                AppState.selectedBiller = null;
                AppState.selectedService = null;
                ViewController.showCategories();
                break;
            case 1:
                // Back to billers
                if (AppState.selectedCategory) {
                    AppState.breadcrumb = ['Categories', AppState.selectedCategory.name];
                    AppState.selectedBiller = null;
                    AppState.selectedService = null;
                    ViewController.showBillers(AppState.selectedCategory.id);
                }
                break;
            case 2:
                // Back to services
                if (AppState.selectedBiller) {
                    AppState.breadcrumb = [
                        'Categories',
                        AppState.selectedCategory.name,
                        AppState.selectedBiller.name
                    ];
                    AppState.selectedService = null;
                    ViewController.showServices(AppState.selectedBiller.id);
                }
                break;
        }
    }
};

// ==================================================
// View Controller
// ==================================================

const ViewController = {
    async showCategories() {
        UI.showLoading();
        AppState.currentView = 'categories';
        AppState.breadcrumb = ['Categories'];
        UI.updateBreadcrumb(AppState.breadcrumb);

        try {
            const categories = await api.getCategories();

            if (!categories || categories.length === 0) {
                UI.hideLoading();
                UI.showError('No categories available');
                return;
            }

            const content = document.getElementById('content');
            content.innerHTML = `
                <h2 class="content-title">Select a Category</h2>
                ${UI.renderButtonGrid(categories, null, 'category')}
            `;

            // Add click handlers
            content.querySelectorAll('.item-button').forEach(button => {
                button.addEventListener('click', () => {
                    const categoryId = parseInt(button.dataset.id);
                    const category = categories.find(c => c.id === categoryId);
                    AppState.selectedCategory = category;
                    this.showBillers(categoryId);
                });
            });

            UI.hideLoading();
        } catch (error) {
            UI.hideLoading();
            UI.showError(error.responseMessage || 'Failed to load categories');
        }
    },

    async showBillers(categoryId) {
        UI.showLoading();
        AppState.currentView = 'billers';
        AppState.breadcrumb = ['Categories', AppState.selectedCategory.name];
        UI.updateBreadcrumb(AppState.breadcrumb);

        try {
            const billers = await api.getBillersByCategory(categoryId);

            if (!billers || billers.length === 0) {
                UI.hideLoading();
                UI.showError('No billers available for this category');
                return;
            }

            const content = document.getElementById('content');
            content.innerHTML = `
                <h2 class="content-title">Select a Provider</h2>
                ${UI.renderButtonGrid(billers, null, 'biller')}
            `;

            // Add click handlers
            content.querySelectorAll('.item-button').forEach(button => {
                button.addEventListener('click', () => {
                    const billerId = parseInt(button.dataset.id);
                    const biller = billers.find(b => b.id === billerId);
                    AppState.selectedBiller = biller;
                    this.showServices(billerId);
                });
            });

            UI.hideLoading();
        } catch (error) {
            UI.hideLoading();
            UI.showError(error.responseMessage || 'Failed to load billers');
        }
    },

    async showServices(billerId) {
        UI.showLoading();
        AppState.currentView = 'services';
        AppState.breadcrumb = [
            'Categories',
            AppState.selectedCategory.name,
            AppState.selectedBiller.name
        ];
        UI.updateBreadcrumb(AppState.breadcrumb);

        try {
            const services = await api.getServicesByBiller(billerId);

            if (!services || services.length === 0) {
                UI.hideLoading();
                UI.showError('No services available for this biller');
                return;
            }

            const content = document.getElementById('content');
            content.innerHTML = `
                <h2 class="content-title">Select a Service</h2>
                ${UI.renderButtonGrid(services, null, 'service')}
            `;

            // Add click handlers
            content.querySelectorAll('.item-button').forEach(button => {
                button.addEventListener('click', () => {
                    const serviceId = parseInt(button.dataset.id);
                    const service = services.find(s => s.id === serviceId);
                    AppState.selectedService = service;
                    this.showPaymentForm(serviceId);
                });
            });

            UI.hideLoading();
        } catch (error) {
            UI.hideLoading();
            UI.showError(error.responseMessage || 'Failed to load services');
        }
    },

    async showPaymentForm(serviceId) {
        UI.showLoading();
        AppState.currentView = 'form';
        AppState.breadcrumb = [
            'Categories',
            AppState.selectedCategory.name,
            AppState.selectedBiller.name,
            AppState.selectedService.name
        ];
        UI.updateBreadcrumb(AppState.breadcrumb);

        try {
            const parameters = await api.getServiceParameters(serviceId);
            AppState.serviceParameters = parameters;

            if (!parameters || parameters.length === 0) {
                UI.hideLoading();
                UI.showError('No parameters defined for this service');
                return;
            }

            const content = document.getElementById('content');
            content.innerHTML = `
                <h2 class="content-title">${AppState.selectedService.name}</h2>
                <form id="paymentForm" class="payment-form">
                    ${FormBuilder.buildDynamicForm(parameters)}
                    <div class="btn-group">
                        <button type="submit" class="btn btn-primary btn-block">Process Payment</button>
                    </div>
                </form>
            `;

            // Add form submit handler
            document.getElementById('paymentForm').addEventListener('submit', (e) => {
                e.preventDefault();
                this.handlePaymentSubmit();
            });

            UI.hideLoading();
        } catch (error) {
            UI.hideLoading();
            UI.showError(error.responseMessage || 'Failed to load service parameters');
        }
    },

    async handlePaymentSubmit() {
        // Collect form data
        const formData = FormBuilder.collectFormData(AppState.serviceParameters);

        // Validate
        const validation = FormBuilder.validateForm(AppState.serviceParameters, formData);
        if (!validation.valid) {
            alert('Validation Error: ' + validation.errors.join('\n'));
            return;
        }

        // Build request
        const processRequest = {
            id: 0,
            serviceId: AppState.selectedService.id,
            accountFrom: '1234567890', // TODO: Get from user
            parameters: Object.keys(formData).map(key => {
                const param = AppState.serviceParameters.find(p => p.internalKey === key);
                return {
                    id: param.id,
                    key: key,
                    value: formData[key]
                };
            })
        };

        UI.showLoading();

        try {
            const response = await api.processPayment(processRequest);
            UI.hideLoading();

            if (!response.success) {
                UI.showError(response.responseMessage);
                return;
            }

            // Check if multi-step service
            if (response.data.final_status > 0) {
                // Multi-step service - show modal for next step
                AppState.firstStepResponse = response.data;
                MultiStepHandler.showNextStepModal(response.data);
            } else {
                // Single-step service - show receipt
                ReceiptHandler.showReceipt(response);
            }
        } catch (error) {
            UI.hideLoading();
            UI.showError(error.responseMessage || 'Payment processing failed');
        }
    }
};

// ==================================================
// Form Builder
// ==================================================

const FormBuilder = {
    buildDynamicForm(parameters) {
        // Sort by orderNo
        const sortedParams = parameters.sort((a, b) => a.orderNo - b.orderNo);

        return sortedParams.map(param => this.renderField(param)).join('');
    },

    renderField(param) {
        const required = 'required';

        switch (param.inputType) {
            case 1: // Text input
                return `
                    <div class="form-group">
                        <label class="form-label">${param.name}</label>
                        <span class="form-description">${param.description || ''}</span>
                        <input type="text"
                               class="form-input"
                               name="${param.internalKey}"
                               id="${param.internalKey}"
                               ${param.length > 0 ? `maxlength="${param.length}"` : ''}
                               ${required}>
                        <span class="form-error" id="error-${param.internalKey}"></span>
                    </div>
                `;

            case 2: // Dropdown/Select
                return `
                    <div class="form-group">
                        <label class="form-label">${param.name}</label>
                        <span class="form-description">${param.description || ''}</span>
                        <select class="form-select"
                                name="${param.internalKey}"
                                id="${param.internalKey}"
                                ${required}>
                            <option value="">-- Select --</option>
                            ${param.option.map(opt => `
                                <option value="${opt.value}">${opt.name}</option>
                            `).join('')}
                        </select>
                        <span class="form-error" id="error-${param.internalKey}"></span>
                    </div>
                `;

            case 3: // Checkbox
                return `
                    <div class="form-group">
                        <label class="form-label">${param.name}</label>
                        <span class="form-description">${param.description || ''}</span>
                        <div class="form-checkbox">
                            <input type="checkbox"
                                   name="${param.internalKey}"
                                   id="${param.internalKey}"
                                   value="true">
                            <label for="${param.internalKey}">${param.name}</label>
                        </div>
                        <span class="form-error" id="error-${param.internalKey}"></span>
                    </div>
                `;

            case 4: // Textarea
                return `
                    <div class="form-group">
                        <label class="form-label">${param.name}</label>
                        <span class="form-description">${param.description || ''}</span>
                        <textarea class="form-textarea"
                                  name="${param.internalKey}"
                                  id="${param.internalKey}"
                                  ${required}></textarea>
                        <span class="form-error" id="error-${param.internalKey}"></span>
                    </div>
                `;

            case 5: // Radio buttons
                return `
                    <div class="form-group">
                        <label class="form-label">${param.name}</label>
                        <span class="form-description">${param.description || ''}</span>
                        ${param.option.map((opt, index) => `
                            <div class="form-radio">
                                <input type="radio"
                                       name="${param.internalKey}"
                                       id="${param.internalKey}-${index}"
                                       value="${opt.value}"
                                       ${required}>
                                <label for="${param.internalKey}-${index}">${opt.name}</label>
                            </div>
                        `).join('')}
                        <span class="form-error" id="error-${param.internalKey}"></span>
                    </div>
                `;

            default:
                return '';
        }
    },

    collectFormData(parameters) {
        const formData = {};
        parameters.forEach(param => {
            const element = document.getElementById(param.internalKey);
            if (element) {
                if (param.inputType === 3) {
                    // Checkbox
                    formData[param.internalKey] = element.checked ? 'true' : 'false';
                } else if (param.inputType === 5) {
                    // Radio
                    const selected = document.querySelector(`input[name="${param.internalKey}"]:checked`);
                    formData[param.internalKey] = selected ? selected.value : '';
                } else {
                    formData[param.internalKey] = element.value;
                }
            }
        });
        return formData;
    },

    validateForm(parameters, formData) {
        const errors = [];

        parameters.forEach(param => {
            const value = formData[param.internalKey];
            const error = this.validateField(param, value);
            if (error) {
                errors.push(error);
            }
        });

        return {
            valid: errors.length === 0,
            errors: errors
        };
    },

    validateField(param, value) {
        // Check if empty
        if (!value || value.trim() === '') {
            return `${param.name} is required`;
        }

        // Check length
        if (param.length > 0 && value.length !== param.length) {
            return `${param.name} must be exactly ${param.length} characters`;
        }

        // Check regex
        if (param.regex) {
            const regex = new RegExp(param.regex);
            if (!regex.test(value)) {
                return `${param.name} format is invalid`;
            }
        }

        // Check numeric range
        if (param.minValue > 0 || param.maxValue > 0) {
            const numValue = parseFloat(value);
            if (isNaN(numValue)) {
                return `${param.name} must be a number`;
            }
            if (param.minValue > 0 && numValue < param.minValue) {
                return `${param.name} must be at least ${param.minValue}`;
            }
            if (param.maxValue > 0 && numValue > param.maxValue) {
                return `${param.name} must not exceed ${param.maxValue}`;
            }
        }

        return null;
    }
};

// ==================================================
// Receipt Handler
// ==================================================

const ReceiptHandler = {
    showReceipt(response) {
        const data = response.data;
        const responseCode = data.responseCode;

        // Determine receipt color
        let receiptType = 'failed';
        if (responseCode === CONFIG.RESPONSE_CODES.SUCCESS) {
            receiptType = 'success';
        } else if (responseCode === CONFIG.RESPONSE_CODES.PENDING) {
            receiptType = 'pending';
        }

        // Store for status checking
        AppState.paymentResponse = response;

        // Build receipt HTML
        let receiptHTML = '';

        if (receiptType === 'success') {
            receiptHTML = this.buildGreenReceipt(data);
        } else if (receiptType === 'pending') {
            receiptHTML = this.buildOrangeReceipt(data);
        } else {
            receiptHTML = this.buildRedReceipt(data);
        }

        // Show in modal
        document.getElementById('receipt').innerHTML = receiptHTML;
        UI.showModal('receiptModal');

        // Start status polling if pending
        if (receiptType === 'pending') {
            // Extract transaction ID
            const txnIdParam = data.response_params.find(p =>
                p.key.toLowerCase().includes('transaction') ||
                p.key.toLowerCase().includes('txn') ||
                p.key === 'transactionId'
            );
            if (txnIdParam) {
                AppState.transactionId = txnIdParam.value;
                this.startStatusPolling(txnIdParam.value);
            }
        }
    },

    buildGreenReceipt(data) {
        return `
            <div class="receipt-header success">
                <div class="receipt-icon">✓</div>
                <div class="receipt-title">Transaction Successful</div>
                <div class="receipt-subtitle">${data.responseMessage || 'Payment completed successfully'}</div>
            </div>
            <div class="receipt-body">
                <div class="receipt-section">
                    <h3 class="receipt-section-title">Transaction Details</h3>
                    ${data.response_params.map(param => `
                        <div class="receipt-item">
                            <div class="receipt-item-label">${param.displayName || param.key}</div>
                            <div class="receipt-item-value">${param.value}</div>
                        </div>
                    `).join('')}
                </div>
                ${data.bankReference ? `
                    <div class="receipt-section">
                        <h3 class="receipt-section-title">Reference</h3>
                        <div class="receipt-item">
                            <div class="receipt-item-label">Bank Reference</div>
                            <div class="receipt-item-value">${data.bankReference}</div>
                        </div>
                    </div>
                ` : ''}
            </div>
        `;
    },

    buildOrangeReceipt(data) {
        return `
            <div class="receipt-header pending">
                <div class="receipt-icon">⏳</div>
                <div class="receipt-title">Transaction Pending</div>
                <div class="receipt-subtitle">${data.responseMessage || 'Processing your transaction...'}</div>
            </div>
            <div class="receipt-body">
                <div class="status-check-section">
                    <div class="status-spinner"></div>
                    <div class="status-message">Checking transaction status...</div>
                    <div class="status-countdown" id="statusCountdown">Next check in 3 seconds</div>
                    <button class="btn btn-primary" id="checkNowBtn">Check Now</button>
                </div>
                <div class="receipt-section">
                    <h3 class="receipt-section-title">Transaction Details</h3>
                    ${data.response_params.map(param => `
                        <div class="receipt-item">
                            <div class="receipt-item-label">${param.displayName || param.key}</div>
                            <div class="receipt-item-value">${param.value}</div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    },

    buildRedReceipt(data) {
        return `
            <div class="receipt-header failed">
                <div class="receipt-icon">✕</div>
                <div class="receipt-title">Transaction Failed</div>
                <div class="receipt-subtitle">${data.responseMessage || 'Transaction could not be completed'}</div>
            </div>
            <div class="receipt-body">
                <div class="alert alert-error">
                    <strong>Error Code:</strong> ${data.responseCode}<br>
                    <strong>Message:</strong> ${data.responseMessage}
                </div>
                ${data.response_params && data.response_params.length > 0 ? `
                    <div class="receipt-section">
                        <h3 class="receipt-section-title">Transaction Details</h3>
                        ${data.response_params.map(param => `
                            <div class="receipt-item">
                                <div class="receipt-item-label">${param.displayName || param.key}</div>
                                <div class="receipt-item-value">${param.value}</div>
                            </div>
                        `).join('')}
                    </div>
                ` : ''}
            </div>
        `;
    },

    startStatusPolling(transactionId) {
        AppState.pollCount = 0;
        let secondsRemaining = 3;

        // Clear any existing interval
        this.stopStatusPolling();

        // Start polling
        AppState.statusCheckInterval = setInterval(async () => {
            AppState.pollCount++;
            secondsRemaining = 3;

            if (AppState.pollCount > CONFIG.MAX_STATUS_CHECK_ATTEMPTS) {
                this.stopStatusPolling();
                alert('Status check timeout. Please try again later.');
                return;
            }

            try {
                await this.checkStatusManually(transactionId);
            } catch (error) {
                console.error('Status check failed:', error);
            }
        }, CONFIG.STATUS_CHECK_INTERVAL);

        // Countdown timer
        const countdownInterval = setInterval(() => {
            secondsRemaining--;
            const countdownEl = document.getElementById('statusCountdown');
            if (countdownEl) {
                countdownEl.textContent = `Next check in ${secondsRemaining} seconds`;
            }
            if (secondsRemaining <= 0) {
                secondsRemaining = 3;
            }
            if (!AppState.statusCheckInterval) {
                clearInterval(countdownInterval);
            }
        }, 1000);

        // Add manual check button handler
        const checkNowBtn = document.getElementById('checkNowBtn');
        if (checkNowBtn) {
            checkNowBtn.addEventListener('click', () => {
                this.checkStatusManually(transactionId);
            });
        }
    },

    stopStatusPolling() {
        if (AppState.statusCheckInterval) {
            clearInterval(AppState.statusCheckInterval);
            AppState.statusCheckInterval = null;
        }
    },

    async checkStatusManually(transactionId) {
        try {
            const statusResponse = await api.checkTransactionStatus(transactionId);

            if (statusResponse.success) {
                const status = statusResponse.data;

                // Check if status has changed
                if (status.responseCode === CONFIG.RESPONSE_CODES.SUCCESS) {
                    this.stopStatusPolling();
                    // Update to green receipt
                    const greenReceipt = this.buildGreenReceipt({
                        responseCode: status.responseCode,
                        responseMessage: status.responseMessage,
                        response_params: status.trnDetails || [],
                        bankReference: status.transactionId
                    });
                    document.getElementById('receipt').innerHTML = greenReceipt;
                } else if (status.responseCode !== CONFIG.RESPONSE_CODES.PENDING) {
                    this.stopStatusPolling();
                    // Update to red receipt
                    const redReceipt = this.buildRedReceipt({
                        responseCode: status.responseCode,
                        responseMessage: status.responseMessage,
                        response_params: status.trnDetails || []
                    });
                    document.getElementById('receipt').innerHTML = redReceipt;
                }
            }
        } catch (error) {
            console.error('Status check error:', error);
        }
    }
};

// ==================================================
// Multi-Step Handler
// ==================================================

const MultiStepHandler = {
    showNextStepModal(firstStepResponse) {
        const nextServiceId = firstStepResponse.final_status;
        const nextParams = firstStepResponse.request_params;

        const modalBody = document.getElementById('modalBody');
        modalBody.innerHTML = `
            <div class="receipt-section">
                <h3 class="receipt-section-title">Step 1 - Inquiry Results</h3>
                ${firstStepResponse.response_params.map(param => `
                    <div class="receipt-item">
                        <div class="receipt-item-label">${param.displayName || param.key}</div>
                        <div class="receipt-item-value">${param.value}</div>
                    </div>
                `).join('')}
            </div>
            <div class="receipt-section">
                <h3 class="receipt-section-title">Step 2 - Complete Payment</h3>
                <form id="nextStepForm" class="payment-form">
                    ${FormBuilder.buildDynamicForm(nextParams)}
                    <div class="btn-group">
                        <button type="submit" class="btn btn-primary btn-block">Complete Payment</button>
                    </div>
                </form>
            </div>
        `;

        // Add form submit handler
        document.getElementById('nextStepForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.processNextStep(nextServiceId, nextParams);
        });

        UI.showModal('modal');
    },

    async processNextStep(serviceId, nextParams) {
        // Collect form data
        const formData = FormBuilder.collectFormData(nextParams);

        // Validate
        const validation = FormBuilder.validateForm(nextParams, formData);
        if (!validation.valid) {
            alert('Validation Error: ' + validation.errors.join('\n'));
            return;
        }

        // Merge parameters from first step with new input
        const mergedParameters = this.mergeParameters(AppState.firstStepResponse, formData, nextParams);

        // Build request
        const processRequest = {
            id: 0,
            serviceId: serviceId,
            accountFrom: '1234567890',
            parameters: mergedParameters
        };

        UI.showLoading();
        UI.hideModal('modal');

        try {
            const response = await api.processPayment(processRequest);
            UI.hideLoading();

            if (!response.success) {
                UI.showError(response.responseMessage);
                return;
            }

            // Show final receipt
            ReceiptHandler.showReceipt(response);
        } catch (error) {
            UI.hideLoading();
            UI.showError(error.responseMessage || 'Payment processing failed');
        }
    },

    mergeParameters(firstStepResponse, formData, nextParams) {
        const merged = [];

        // Add all response parameters from first step
        firstStepResponse.response_params.forEach(param => {
            merged.push({
                id: param.id,
                key: param.key,
                value: param.value
            });
        });

        // Add new input parameters
        Object.keys(formData).forEach(key => {
            const param = nextParams.find(p => p.internalKey === key);
            if (param) {
                merged.push({
                    id: param.id,
                    key: key,
                    value: formData[key]
                });
            }
        });

        return merged;
    }
};

// ==================================================
// Language Handler
// ==================================================

const LanguageHandler = {
    switchLanguage(lang) {
        AppState.language = lang;
        api.setLanguage(lang);

        // Update UI direction
        if (lang === 'ar') {
            document.body.setAttribute('dir', 'rtl');
        } else {
            document.body.removeAttribute('dir');
        }

        // Update button states
        document.querySelectorAll('.lang-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.getElementById(`lang${lang === 'en' ? 'En' : 'Ar'}`).classList.add('active');

        // Reload current view
        this.reloadCurrentView();
    },

    reloadCurrentView() {
        switch (AppState.currentView) {
            case 'categories':
                ViewController.showCategories();
                break;
            case 'billers':
                if (AppState.selectedCategory) {
                    ViewController.showBillers(AppState.selectedCategory.id);
                }
                break;
            case 'services':
                if (AppState.selectedBiller) {
                    ViewController.showServices(AppState.selectedBiller.id);
                }
                break;
            case 'form':
                if (AppState.selectedService) {
                    ViewController.showPaymentForm(AppState.selectedService.id);
                }
                break;
        }
    }
};

// ==================================================
// Initialize Application
// ==================================================

function initializeApp() {
    // Language switchers
    document.getElementById('langEn').addEventListener('click', () => {
        LanguageHandler.switchLanguage('en');
    });

    document.getElementById('langAr').addEventListener('click', () => {
        LanguageHandler.switchLanguage('ar');
    });

    // Modal close buttons
    document.getElementById('closeModal').addEventListener('click', () => {
        UI.hideModal('modal');
    });

    document.getElementById('closeReceipt').addEventListener('click', () => {
        UI.hideModal('receiptModal');
        ReceiptHandler.stopStatusPolling();
    });

    // New transaction button
    document.getElementById('newTransaction').addEventListener('click', () => {
        UI.hideModal('receiptModal');
        ReceiptHandler.stopStatusPolling();
        ViewController.showCategories();
    });

    // Load categories on startup
    ViewController.showCategories();
}

// Start the application when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeApp);
} else {
    initializeApp();
}
