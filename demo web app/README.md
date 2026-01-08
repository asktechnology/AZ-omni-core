# AZ Payment Gateway - Demo Web Application

A demo web application that simulates mobile payment flow for the AZ-omni-core Payment Gateway API.

## Features

- **Dynamic Service Discovery**: Browse categories, billers, and services
- **Dynamic Form Generation**: Forms automatically adapt to service parameters
- **Multi-Step Services**: Support for inquiry + payment workflows
- **Color-Coded Receipts**:
  - Green: Successful transactions
  - Orange: Pending transactions (with auto-polling)
  - Red: Failed transactions
- **Real-time Status Checking**: Auto-polls every 3 seconds for pending transactions
- **Multi-language Support**: English and Arabic (with RTL support)
- **Responsive Design**: Works on desktop, tablet, and mobile
- **No Build Process**: Just open index.html and go!

## Prerequisites

- **Modern Web Browser** (Chrome, Firefox, Edge, Safari)
- **Backend Server** running on `http://localhost:8040`
- **CORS enabled** on the backend (for local development)

## Setup Instructions

### 1. Start the Backend Server

Make sure the AZ-omni-core backend is running on port 8040:

```bash
cd AZ-omni-core
mvn spring-boot:run
```

Verify the server is running by visiting: `http://localhost:8040/api/v1/category`

### 2. Open the Demo App

Simply open `index.html` in your web browser:

```bash
# Windows
start index.html

# Mac
open index.html

# Linux
xdg-open index.html
```

Or right-click `index.html` → Open with → Your browser

## Usage Guide

### Basic Payment Flow

1. **Select Category**
   - Choose a payment category (e.g., Mobile Services, Utilities)

2. **Select Biller**
   - Choose a service provider (e.g., Zain, MTN)

3. **Select Service**
   - Choose a specific service (e.g., Prepaid Top-Up)

4. **Fill Payment Form**
   - Form fields are dynamically generated based on service requirements
   - All validations are applied (regex, length, min/max values)

5. **Process Payment**
   - Click "Process Payment" to submit
   - Watch for the color-coded receipt

### Multi-Step Services

Some services require two steps (inquiry, then payment):

1. Complete the first form (inquiry)
2. A modal will appear with inquiry results
3. Fill the second form (payment details)
4. Click "Complete Payment"
5. View the final receipt

### Pending Transactions (Orange Receipt)

If a transaction is pending (responseCode = 20):

1. Orange receipt appears with status "Processing..."
2. App automatically checks status every 3 seconds
3. Click "Check Now" for manual status update
4. Receipt updates to green (success) or red (failed) when final status is received

### Language Support

Click the language buttons in the header to switch between English and Arabic. The entire interface updates, including:
- UI text
- Form labels
- Error messages
- Receipt details

## Configuration

Edit `config.js` to customize:

```javascript
const CONFIG = {
    API_BASE_URL: 'http://localhost:8040/api/v1', // Change for production
    STATUS_CHECK_INTERVAL: 3000, // Polling interval (ms)
    MAX_STATUS_CHECK_ATTEMPTS: 20, // Max polling attempts
    // ... other settings
};
```

## File Structure

```
demo web app/
├── index.html     # Main HTML structure
├── style.css      # Complete styling (responsive)
├── config.js      # Configuration
├── api.js         # API client (Axios wrapper)
├── app.js         # Main application logic
└── README.md      # This file
```

## API Endpoints Used

The demo app uses the following endpoints:

### Discovery Endpoints
- `GET /category` - Get all categories
- `GET /category/billerByCategoryId/{id}` - Get billers by category
- `GET /biller/servicesByBillerId/{id}` - Get services by biller
- `GET /service/parameterByServiceId/{id}` - Get service parameters

### Payment Endpoints
- `POST /payment/process` - Process payment
- `POST /payment/checkStatus` - Check transaction status

## Troubleshooting

### Problem: Categories Not Loading

**Symptoms**: "Network error: Unable to connect to server"

**Solutions**:
1. Ensure backend is running: `http://localhost:8040`
2. Check if CORS is enabled on backend
3. Open browser console (F12) to see detailed error

### Problem: CORS Error

**Symptoms**: Console shows "blocked by CORS policy"

**Solutions**:
1. Add CORS configuration to backend:
   ```java
   @Configuration
   public class WebConfiguration {
       @Bean
       public WebMvcConfigurer corsConfigurer() {
           return new WebMvcConfigurer() {
               @Override
               public void addCorsMappings(CorsRegistry registry) {
                   registry.addMapping("/**")
                          .allowedOrigins("*")
                          .allowedMethods("GET", "POST", "PUT", "DELETE");
               }
           };
       }
   }
   ```
2. Restart backend server

### Problem: Images Not Loading

**Symptoms**: Placeholder images shown instead of actual images

**Solutions**:
- Images are served from the backend `/images/` directory
- If images don't load, the app falls back to SVG placeholders
- Check backend image configuration in `application.yml`

### Problem: Form Validation Errors

**Symptoms**: Cannot submit form, validation errors appear

**Solutions**:
- Check parameter regex patterns
- Verify input length matches requirements
- Ensure numeric values are within min/max range
- All fields are required by default

### Problem: Status Polling Not Working

**Symptoms**: Orange receipt doesn't update automatically

**Solutions**:
1. Check browser console for errors
2. Verify `/payment/checkStatus` endpoint is accessible
3. Ensure transaction ID is included in response
4. Try manual "Check Now" button

## Browser Compatibility

Tested and working on:
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## Development

### Making Changes

1. **Styling**: Edit `style.css`
2. **API Configuration**: Edit `config.js`
3. **Business Logic**: Edit `app.js`
4. **Layout**: Edit `index.html`

### Debugging

Open browser DevTools (F12):
- **Console**: View logs and errors
- **Network**: Monitor API requests/responses
- **Elements**: Inspect DOM and styling

### Testing Multi-Step Services

To test multi-step services, configure a service in the backend with:
- `serviceType = INQUIRY` (first step)
- `serviceType = PAYMENT` (second step)
- Link them using `beforeService` and `afterService`

## Production Deployment

For production deployment:

1. **Update API URL** in `config.js`:
   ```javascript
   API_BASE_URL: 'https://your-domain.com/api/v1'
   ```

2. **Enable HTTPS**: Use secure connection for production

3. **Host Files**: Upload all files to web server (Apache, Nginx, etc.)

4. **Configure Backend**: Update CORS to allow production domain

## Known Limitations

- No authentication/authorization (demo purposes)
- Account number is hardcoded ('1234567890')
- No transaction history/storage
- No print receipt functionality
- Basic error handling

## Support

For issues or questions:
- Check backend logs: `logs/Payment_YY_MM_DD.log`
- Review browser console errors
- Verify API endpoint responses
- Refer to API documentation: `../API_DOCUMENTATION.md`

## License

Internal use only - AZ Technology Co. Ltd

## Version History

- **v1.0** (2026-01-04): Initial release
  - Complete payment flow
  - Multi-step services
  - Status polling
  - Multi-language support
  - Responsive design
