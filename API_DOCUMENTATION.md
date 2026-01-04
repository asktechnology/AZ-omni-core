# AZ-Omni-Core Payment API Documentation

**Version:** 1.0
**Last Updated:** January 2026
**Base URL:** `http://localhost:8040/api/v1`

---

## Table of Contents

1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Authentication](#authentication)
4. [Core Payment Endpoints](#core-payment-endpoints)
   - [Process Payment](#1-process-payment)
   - [Post-Check Verification](#2-post-check-verification)
   - [Check Transaction Status](#3-check-transaction-status)
5. [Service Discovery Endpoints](#service-discovery-endpoints)
   - [Service Management](#service-management)
   - [Category Management](#category-management)
   - [Biller Management](#biller-management)
   - [Parameter Configuration](#parameter-configuration)
6. [Data Models](#data-models)
7. [Error Handling](#error-handling)
8. [React Native Integration](#react-native-integration)
9. [Parameter System](#parameter-system)
10. [Best Practices](#best-practices)
11. [Security Considerations](#security-considerations)
12. [Appendix](#appendix)

---

## Introduction

The AZ-Omni-Core Payment API is a comprehensive payment processing microservice that enables mobile applications to:

- Process payments through multiple billers
- Perform bill inquiries
- Check transaction status
- Dynamically configure payment forms based on service parameters
- Support multi-language interfaces (English and Arabic)

**Technology Stack:**
- Spring Boot 3.3.0
- Java 17
- Oracle Database
- Redis Caching
- Port: 8040

**Key Features:**
- Dynamic parameter system for flexible payment forms
- Multi-language support (EN/AR)
- Comprehensive transaction logging
- Service chaining for complex workflows
- RESTful API design

---

## Getting Started

### Base URL

```
http://localhost:8040/api/v1
```

> **Note:** Replace `localhost:8040` with your production server URL when deploying.

### Required Headers

All API requests require the following headers:

| Header | Value | Required | Description |
|--------|-------|----------|-------------|
| `Content-Type` | `application/json` | Yes | Request content type |
| `language` | `en` or `ar` | No | Response language (defaults to `en`) |

### Quick Start Example

```javascript
// React Native with Axios
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8040/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'language': 'en'
  }
});

// Fetch available services
const services = await api.get('/service');
console.log(services.data);
```

---

## Authentication

**Current Status:** No authentication is currently implemented.

**For Production:** Implement one of the following:
- API Key authentication
- OAuth 2.0
- JWT tokens
- mTLS (mutual TLS)

See [Security Considerations](#security-considerations) for more details.

---

## Core Payment Endpoints

### 1. Process Payment

Process a payment request through the configured biller.

**Endpoint:** `POST /api/v1/payment/process`

**Headers:**
```
Content-Type: application/json
language: en
```

**Request Body:**

```typescript
interface ProcessRequest {
  id: number;              // Unique request identifier
  serviceId: number;       // Service type ID
  accountFrom: string;     // Source account number
  parameters: RequestParameter[];  // Dynamic parameters
}

interface RequestParameter {
  id: number;      // Parameter definition ID
  key: string;     // Parameter name/key
  value: string;   // Parameter value
}
```

**Example Request:**

```json
{
  "id": 0,
  "serviceId": 1,
  "accountFrom": "1234567890",
  "parameters": [
    {
      "id": 1,
      "key": "phoneNumber",
      "value": "0501234567"
    },
    {
      "id": 2,
      "key": "amount",
      "value": "100"
    }
  ]
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "responseCode": 0,
  "responseMessage": "",
  "data": {
    "response_params": [
      {
        "key": "confirmationCode",
        "value": "ABC123XYZ",
        "displayName": "Confirmation Code",
        "id": 3
      },
      {
        "key": "transactionId",
        "value": "TXN20250104001",
        "displayName": "Transaction ID",
        "id": 4
      }
    ],
    "final_status": 1,
    "isPayment": true,
    "request_params": [...],
    "responseCode": 0,
    "responseMessage": "Payment processed successfully",
    "bankReference": "REF123456"
  }
}
```

**Error Response (400 Bad Request):**

```json
{
  "success": false,
  "responseCode": 400,
  "responseMessage": "Validation failed: request parameter Phone Number with Id 1 not found in request",
  "data": null
}
```

**React Native Example:**

```typescript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8040/api/v1';

interface ProcessPaymentParams {
  serviceId: number;
  accountFrom: string;
  parameters: Array<{
    id: number;
    key: string;
    value: string;
  }>;
}

const processPayment = async (params: ProcessPaymentParams) => {
  try {
    const response = await axios.post(
      `${API_BASE_URL}/payment/process`,
      {
        id: 0,
        ...params
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'language': 'en'
        }
      }
    );

    if (response.data.success) {
      console.log('Payment successful:', response.data.data);
      return response.data.data;
    } else {
      throw new Error(response.data.responseMessage);
    }
  } catch (error) {
    if (axios.isAxiosError(error)) {
      console.error('Payment failed:', error.response?.data?.responseMessage);
      throw error.response?.data;
    }
    throw error;
  }
};

// Usage
const result = await processPayment({
  serviceId: 1,
  accountFrom: '1234567890',
  parameters: [
    { id: 1, key: 'phoneNumber', value: '0501234567' },
    { id: 2, key: 'amount', value: '100' }
  ]
});
```

---

### 2. Post-Check Verification

Verify or complete a transaction after initial processing.

**Endpoint:** `POST /api/v1/payment/postCheck`

**Headers:**
```
Content-Type: application/json
language: en
```

**Request Body:**

```typescript
interface PostCheckRequest {
  id: number;                 // Request identifier
  serviceId: number;          // Service ID
  originOmniRrn: string;      // Original Reference Retrieval Number
  parameters: RequestParameter[];
}
```

**Example Request:**

```json
{
  "id": 0,
  "serviceId": 1,
  "originOmniRrn": "RRN20250104001",
  "parameters": [
    {
      "id": 1,
      "key": "phoneNumber",
      "value": "0501234567"
    }
  ]
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "responseCode": 0,
  "responseMessage": "",
  "data": {
    "response_params": [
      {
        "key": "status",
        "value": "confirmed",
        "displayName": "Status",
        "id": 5
      }
    ],
    "final_status": 1,
    "responseCode": 0,
    "responseMessage": "Post-check completed successfully",
    "bankReference": "REF123456"
  }
}
```

**React Native Example:**

```typescript
const postCheckPayment = async (
  serviceId: number,
  originOmniRrn: string,
  parameters: Array<{ id: number; key: string; value: string }>
) => {
  try {
    const response = await axios.post(
      `${API_BASE_URL}/payment/postCheck`,
      {
        id: 0,
        serviceId,
        originOmniRrn,
        parameters
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'language': 'en'
        }
      }
    );

    return response.data;
  } catch (error) {
    console.error('Post-check failed:', error);
    throw error;
  }
};

// Usage
await postCheckPayment(1, 'RRN20250104001', [
  { id: 1, key: 'phoneNumber', value: '0501234567' }
]);
```

---

### 3. Check Transaction Status

Query the status of a previously initiated transaction.

**Endpoint:** `POST /api/v1/payment/checkStatus`

**Headers:**
```
Content-Type: application/json
language: en
```

**Request Body:**

```typescript
interface CheckStatusRequest {
  transactionId: string;  // Transaction ID to check
}
```

**Example Request:**

```json
{
  "transactionId": "TXN20250104001"
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "responseCode": 0,
  "responseMessage": "",
  "data": {
    "responseCode": 0,
    "responseMessage": "Transaction found",
    "trnStatus": "1",
    "trnStatusDisc": "Success",
    "transactionId": "TXN20250104001",
    "checkReqId": "CHK123",
    "trnDetails": [
      {
        "key": "amount",
        "value": "100",
        "displayName": "Amount",
        "id": 2
      },
      {
        "key": "phoneNumber",
        "value": "0501234567",
        "displayName": "Phone Number",
        "id": 1
      }
    ]
  }
}
```

**React Native Example:**

```typescript
const checkTransactionStatus = async (transactionId: string) => {
  try {
    const response = await axios.post(
      `${API_BASE_URL}/payment/checkStatus`,
      { transactionId },
      {
        headers: {
          'Content-Type': 'application/json',
          'language': 'en'
        }
      }
    );

    const { data } = response.data;
    console.log(`Transaction Status: ${data.trnStatusDisc}`);
    return data;
  } catch (error) {
    console.error('Status check failed:', error);
    throw error;
  }
};

// Usage
const status = await checkTransactionStatus('TXN20250104001');
console.log('Status:', status.trnStatusDisc);
```

---

## Service Discovery Endpoints

### Service Management

#### Get All Services

Retrieve all available payment services.

**Endpoint:** `GET /api/v1/service`

**Response:**

```json
[
  {
    "id": 1,
    "name": "Zain Prepaid Recharge",
    "description": "Mobile prepaid recharge for Zain network",
    "isPayment": true,
    "imageUrl": "https://cdn.example.com/zain.jpg"
  },
  {
    "id": 2,
    "name": "Electricity Bill Payment",
    "description": "Pay electricity bills",
    "isPayment": true,
    "imageUrl": "https://cdn.example.com/electricity.jpg"
  }
]
```

**React Native Example:**

```typescript
const fetchServices = async () => {
  const response = await axios.get(`${API_BASE_URL}/service`, {
    headers: { language: 'en' }
  });
  return response.data;
};

// Usage
const services = await fetchServices();
services.forEach(service => {
  console.log(`${service.name}: ${service.description}`);
});
```

---

#### Get Service by ID

**Endpoint:** `GET /api/v1/service/{service-id}`

**Example:** `GET /api/v1/service/1`

**Response:**

```json
{
  "id": 1,
  "name": "Zain Prepaid Recharge",
  "description": "Mobile prepaid recharge",
  "isPayment": true,
  "imageUrl": "https://cdn.example.com/zain.jpg"
}
```

---

#### Get Service Parameters (Critical for Dynamic Forms)

Get all required parameters for a service. Use this to build dynamic payment forms.

**Endpoint:** `GET /api/v1/service/parameterByServiceId/{service-id}`

**Example:** `GET /api/v1/service/parameterByServiceId/1`

**Response:**

```json
[
  {
    "id": 1,
    "name": "Phone Number",
    "description": "Customer mobile number",
    "internalKey": "phoneNumber",
    "orderNo": 1,
    "regex": "^[0-9]{10}$",
    "inputType": 1,
    "jsonType": 1,
    "paramType": 1,
    "length": 10,
    "minValue": 0,
    "maxValue": 0,
    "option": []
  },
  {
    "id": 2,
    "name": "Amount",
    "description": "Recharge amount",
    "internalKey": "amount",
    "orderNo": 2,
    "regex": "^[0-9]+$",
    "inputType": 2,
    "jsonType": 1,
    "paramType": 1,
    "length": -1,
    "minValue": 10,
    "maxValue": 5000,
    "option": [
      {
        "id": 10,
        "name": "50 SAR",
        "value": "50"
      },
      {
        "id": 11,
        "name": "100 SAR",
        "value": "100"
      }
    ]
  }
]
```

**React Native Example:**

```typescript
interface ParameterConfig {
  id: number;
  name: string;
  description: string;
  internalKey: string;
  orderNo: number;
  regex: string | null;
  inputType: number;
  jsonType: number;
  paramType: number;
  length: number;
  minValue: number;
  maxValue: number;
  option: Array<{
    id: number;
    name: string;
    value: string;
  }>;
}

const getServiceParameters = async (serviceId: number): Promise<ParameterConfig[]> => {
  const response = await axios.get(
    `${API_BASE_URL}/service/parameterByServiceId/${serviceId}`,
    {
      headers: { language: 'en' }
    }
  );
  return response.data;
};

// Usage - Build dynamic form
const params = await getServiceParameters(1);
params.sort((a, b) => a.orderNo - b.orderNo);

params.forEach(param => {
  console.log(`Field: ${param.name}`);
  console.log(`Type: ${param.inputType === 1 ? 'Text' : 'Menu'}`);
  console.log(`Validation: ${param.regex || 'None'}`);
  if (param.option.length > 0) {
    console.log(`Options:`, param.option);
  }
});
```

---

### Category Management

#### Get All Categories

**Endpoint:** `GET /api/v1/category`

**Headers:**
```
language: en
```

**Response:**

```json
[
  {
    "id": 1,
    "name": "Mobile Services",
    "description": "Mobile recharge and bill payment",
    "imageUrl": "https://cdn.example.com/mobile.jpg"
  },
  {
    "id": 2,
    "name": "Utilities",
    "description": "Electricity, water and gas payments",
    "imageUrl": "https://cdn.example.com/utilities.jpg"
  }
]
```

---

#### Get Category by ID

**Endpoint:** `GET /api/v1/category/{category-id}`

**Example:** `GET /api/v1/category/1`

---

#### Get Billers by Category

**Endpoint:** `GET /api/v1/category/billerByCategoryId/{category-id}`

**Example:** `GET /api/v1/category/billerByCategoryId/1`

**Response:**

```json
[
  {
    "id": 1,
    "name": "Zain Telecom",
    "description": "Zain mobile network operator",
    "imageUrl": "https://cdn.example.com/zain-logo.jpg"
  },
  {
    "id": 2,
    "name": "Cashi Digital Wallet",
    "description": "Digital wallet services",
    "imageUrl": "https://cdn.example.com/cashi.jpg"
  }
]
```

---

### Biller Management

#### Get All Billers

**Endpoint:** `GET /api/v1/biller`

**Response:**

```json
{
  "success": true,
  "responseCode": 0,
  "responseMessage": "",
  "data": [
    {
      "id": 1,
      "name": "Zain Telecom",
      "description": "Zain mobile network",
      "imageUrl": "https://cdn.example.com/zain.jpg"
    }
  ]
}
```

---

#### Get Biller by ID

**Endpoint:** `GET /api/v1/biller/{biller-id}`

---

#### Get Services by Biller

**Endpoint:** `GET /api/v1/biller/servicesByBillerId/{biller-id}`

**Example:** `GET /api/v1/biller/servicesByBillerId/1`

**Response:**

```json
[
  {
    "id": 1,
    "name": "Prepaid Recharge",
    "description": "Mobile prepaid recharge",
    "isPayment": true,
    "imageUrl": "https://cdn.example.com/prepaid.jpg"
  }
]
```

---

### Parameter Configuration

#### Get All Parameters

**Endpoint:** `GET /api/v1/parameter`

---

#### Get Parameter by ID

**Endpoint:** `GET /api/v1/parameter/{param-id}`

---

#### Get Parameters by Service

**Endpoint:** `GET /api/v1/parameter/byService/{service-id}`

---

## Data Models

### TypeScript Type Definitions

```typescript
// ============================================
// Request Models
// ============================================

interface ProcessRequest {
  id: number;
  serviceId: number;
  accountFrom: string;
  parameters: RequestParameter[];
}

interface PostCheckRequest {
  id: number;
  serviceId: number;
  originOmniRrn: string;
  parameters: RequestParameter[];
}

interface CheckStatusRequest {
  transactionId: string;
}

interface RequestParameter {
  id: number;
  key: string;
  value: string;
}

// ============================================
// Response Models
// ============================================

interface ApiResponse<T> {
  success: boolean;
  responseCode: number;
  responseMessage: string;
  data: T | null;
}

interface PaymentResponse {
  response_params: PaymentResponseField[];
  final_status: number;
  isPayment: boolean;
  request_params: ParameterResponse[];
  responseCode: number;
  responseMessage: string;
  bankReference?: string;
}

interface PostCheckResponse {
  response_params: PaymentResponseField[];
  final_status: number;
  responseCode: number;
  responseMessage: string;
  bankReference?: string;
}

interface CheckStatusResponse {
  responseCode: number;
  responseMessage: string;
  trnStatus: string;
  trnStatusDisc: string;
  transactionId: string;
  checkReqId: string;
  trnDetails: PaymentResponseField[];
}

interface PaymentResponseField {
  key: string;
  value: any;
  displayName: string;
  id: number;
}

interface ParameterResponse {
  id: number;
  name: string;
  description: string;
  internalKey: string;
  orderNo: number;
  regex: string | null;
  inputType: number;
  jsonType: number;
  paramType: number;
  length: number;
  minValue: number;
  maxValue: number;
  option: OptionResponse[];
}

interface OptionResponse {
  id: number;
  name: string;
  value: string;
}

interface ServiceResponse {
  id: number;
  name: string;
  description: string;
  isPayment: boolean;
  imageUrl: string;
}

interface CategoryResponse {
  id: number;
  name: string;
  description: string;
  imageUrl: string;
}

interface BillerResponse {
  id: number;
  name: string;
  description: string;
  imageUrl: string;
}

// ============================================
// Enums
// ============================================

enum InputType {
  TEXT = 1,
  MENU = 2,
  CHECKBOX = 3,
  TEXTAREA = 4,
  RADIO = 5,
  SHARED = 6
}

enum JsonType {
  PRIMITIVE = 1,
  OBJECT = 2,
  ARRAY = 3,
  ARRAY_OF_OBJECT = 4
}

enum ParamType {
  INPUT = 1,
  OUTPUT = 2,
  BOTH = 3,
  MAPPING = 4
}

enum TransactionStatus {
  FAILED = 0,
  SUCCESS = 1,
  IN_PROGRESS = 2,
  CANCELLED = 3
}
```

---

## Error Handling

### Standard Error Response

All errors follow this structure:

```typescript
interface ErrorResponse {
  success: false;
  responseCode: number;
  responseMessage: string;
  data: null;
}
```

### Response Codes

| Code | Meaning | HTTP Status | Description |
|------|---------|-------------|-------------|
| `0` | SUCCESS | 200 | Request processed successfully |
| `400` | BAD_REQUEST | 400 | Invalid request format or missing fields |
| `400` | INVALID | 400 | Invalid request data (validation failed) |
| `404` | NOTFOUND | 404 | Resource not found |

### Common Error Scenarios

#### Missing Required Parameter

```json
{
  "success": false,
  "responseCode": 400,
  "responseMessage": "Validation failed for request ... request parameter Phone Number with Id 1 not found in request",
  "data": null
}
```

#### Invalid Parameter Format (Regex Mismatch)

```json
{
  "success": false,
  "responseCode": 400,
  "responseMessage": "Invalid value for parameter phoneNumber with value 123 does not matches defined regex [^[0-9]{10}$]",
  "data": null
}
```

#### Value Out of Range

```json
{
  "success": false,
  "responseCode": 400,
  "responseMessage": "Invalid value for parameter amount with value 5 value is Less Than 10.0",
  "data": null
}
```

#### Service Not Found

```json
{
  "success": false,
  "responseCode": 404,
  "responseMessage": "entity not found",
  "data": null
}
```

### React Native Error Handling

```typescript
import axios, { AxiosError } from 'axios';

const handleApiError = (error: unknown) => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ErrorResponse>;

    if (axiosError.response) {
      // Server responded with error
      const { responseCode, responseMessage } = axiosError.response.data;

      switch (responseCode) {
        case 400:
          console.error('Validation Error:', responseMessage);
          // Show user-friendly validation message
          break;
        case 404:
          console.error('Not Found:', responseMessage);
          // Show "Service not available" message
          break;
        default:
          console.error('API Error:', responseMessage);
      }

      return axiosError.response.data;
    } else if (axiosError.request) {
      // Request made but no response
      console.error('Network Error: No response from server');
      return { success: false, responseCode: 0, responseMessage: 'Network error', data: null };
    }
  }

  // Unknown error
  console.error('Unexpected Error:', error);
  return { success: false, responseCode: 0, responseMessage: 'Unexpected error', data: null };
};

// Usage
try {
  const result = await processPayment(params);
} catch (error) {
  const errorResponse = handleApiError(error);
  Alert.alert('Payment Failed', errorResponse.responseMessage);
}
```

---

## React Native Integration

### Complete Implementation Example

```typescript
// api/PaymentAPI.ts
import axios, { AxiosInstance } from 'axios';

const API_BASE_URL = 'http://localhost:8040/api/v1';

class PaymentAPI {
  private client: AxiosInstance;

  constructor(baseURL: string = API_BASE_URL, language: 'en' | 'ar' = 'en') {
    this.client = axios.create({
      baseURL,
      headers: {
        'Content-Type': 'application/json',
        'language': language
      },
      timeout: 30000
    });
  }

  setLanguage(language: 'en' | 'ar') {
    this.client.defaults.headers.language = language;
  }

  // ===================================
  // Service Discovery
  // ===================================

  async getServices(): Promise<ServiceResponse[]> {
    const response = await this.client.get('/service');
    return response.data;
  }

  async getServiceById(serviceId: number): Promise<ServiceResponse> {
    const response = await this.client.get(`/service/${serviceId}`);
    return response.data;
  }

  async getServiceParameters(serviceId: number): Promise<ParameterResponse[]> {
    const response = await this.client.get(`/service/parameterByServiceId/${serviceId}`);
    return response.data;
  }

  async getCategories(): Promise<CategoryResponse[]> {
    const response = await this.client.get('/category');
    return response.data;
  }

  async getBillersByCategory(categoryId: number): Promise<BillerResponse[]> {
    const response = await this.client.get(`/category/billerByCategoryId/${categoryId}`);
    return response.data;
  }

  async getServicesByBiller(billerId: number): Promise<ServiceResponse[]> {
    const response = await this.client.get(`/biller/servicesByBillerId/${billerId}`);
    return response.data;
  }

  // ===================================
  // Payment Operations
  // ===================================

  async processPayment(
    serviceId: number,
    accountFrom: string,
    parameters: RequestParameter[]
  ): Promise<ApiResponse<PaymentResponse>> {
    const response = await this.client.post('/payment/process', {
      id: 0,
      serviceId,
      accountFrom,
      parameters
    });
    return response.data;
  }

  async postCheckPayment(
    serviceId: number,
    originOmniRrn: string,
    parameters: RequestParameter[]
  ): Promise<ApiResponse<PostCheckResponse>> {
    const response = await this.client.post('/payment/postCheck', {
      id: 0,
      serviceId,
      originOmniRrn,
      parameters
    });
    return response.data;
  }

  async checkTransactionStatus(
    transactionId: string
  ): Promise<ApiResponse<CheckStatusResponse>> {
    const response = await this.client.post('/payment/checkStatus', {
      transactionId
    });
    return response.data;
  }
}

export default new PaymentAPI();
```

### Usage in React Native Components

```typescript
// screens/PaymentScreen.tsx
import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, Button, Alert, ActivityIndicator } from 'react-native';
import PaymentAPI from '../api/PaymentAPI';

const PaymentScreen = ({ route }) => {
  const { serviceId } = route.params;

  const [loading, setLoading] = useState(true);
  const [parameters, setParameters] = useState<ParameterResponse[]>([]);
  const [formValues, setFormValues] = useState<Record<string, string>>({});
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    loadServiceParameters();
  }, [serviceId]);

  const loadServiceParameters = async () => {
    try {
      const params = await PaymentAPI.getServiceParameters(serviceId);
      setParameters(params.sort((a, b) => a.orderNo - b.orderNo));

      // Initialize form values
      const initialValues: Record<string, string> = {};
      params.forEach(param => {
        initialValues[param.internalKey] = '';
      });
      setFormValues(initialValues);
    } catch (error) {
      Alert.alert('Error', 'Failed to load service parameters');
    } finally {
      setLoading(false);
    }
  };

  const validateParameter = (param: ParameterResponse, value: string): string | null => {
    // Check length
    if (param.length > 0 && value.length !== param.length) {
      return `${param.name} must be exactly ${param.length} characters`;
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

    // Check regex
    if (param.regex) {
      const regex = new RegExp(param.regex);
      if (!regex.test(value)) {
        return `${param.name} format is invalid`;
      }
    }

    return null;
  };

  const handlePayment = async () => {
    // Validate all parameters
    for (const param of parameters) {
      const value = formValues[param.internalKey];
      const error = validateParameter(param, value);
      if (error) {
        Alert.alert('Validation Error', error);
        return;
      }
    }

    setProcessing(true);

    try {
      // Build request parameters
      const requestParams: RequestParameter[] = parameters.map(param => ({
        id: param.id,
        key: param.internalKey,
        value: formValues[param.internalKey]
      }));

      // Process payment
      const response = await PaymentAPI.processPayment(
        serviceId,
        '1234567890', // Replace with actual account
        requestParams
      );

      if (response.success) {
        // Extract transaction details
        const { data } = response;
        const transactionId = data.response_params.find(p => p.key === 'transactionId')?.value;

        Alert.alert(
          'Payment Successful',
          `Transaction ID: ${transactionId}\n${data.responseMessage}`,
          [
            {
              text: 'OK',
              onPress: () => {
                // Navigate to receipt screen or home
              }
            }
          ]
        );
      } else {
        Alert.alert('Payment Failed', response.responseMessage);
      }
    } catch (error) {
      Alert.alert('Error', 'Payment processing failed. Please try again.');
    } finally {
      setProcessing(false);
    }
  };

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <View style={{ padding: 16 }}>
      <Text style={{ fontSize: 20, fontWeight: 'bold', marginBottom: 16 }}>
        Payment Form
      </Text>

      {parameters.map(param => (
        <View key={param.id} style={{ marginBottom: 16 }}>
          <Text style={{ fontWeight: 'bold', marginBottom: 4 }}>
            {param.name}
          </Text>
          <Text style={{ fontSize: 12, color: '#666', marginBottom: 8 }}>
            {param.description}
          </Text>

          {param.inputType === 1 ? (
            // Text input
            <TextInput
              style={{
                borderWidth: 1,
                borderColor: '#ccc',
                borderRadius: 4,
                padding: 8
              }}
              value={formValues[param.internalKey]}
              onChangeText={(text) =>
                setFormValues({ ...formValues, [param.internalKey]: text })
              }
              placeholder={`Enter ${param.name}`}
              keyboardType={param.regex?.includes('[0-9]') ? 'numeric' : 'default'}
            />
          ) : param.inputType === 2 ? (
            // Dropdown/Picker (simplified)
            <View>
              {param.option.map(option => (
                <Button
                  key={option.id}
                  title={option.name}
                  onPress={() =>
                    setFormValues({ ...formValues, [param.internalKey]: option.value })
                  }
                />
              ))}
            </View>
          ) : null}
        </View>
      ))}

      <Button
        title={processing ? 'Processing...' : 'Submit Payment'}
        onPress={handlePayment}
        disabled={processing}
      />
    </View>
  );
};

export default PaymentScreen;
```

### Complete Payment Flow Example

```typescript
// Example: Complete payment workflow
const completePaymentFlow = async () => {
  try {
    // Step 1: Get available categories
    const categories = await PaymentAPI.getCategories();
    console.log('Available categories:', categories);

    // Step 2: Get billers in a category (e.g., Mobile Services)
    const billers = await PaymentAPI.getBillersByCategory(1);
    console.log('Billers:', billers);

    // Step 3: Get services for a biller
    const services = await PaymentAPI.getServicesByBiller(billers[0].id);
    console.log('Services:', services);

    // Step 4: Get parameters for the selected service
    const parameters = await PaymentAPI.getServiceParameters(services[0].id);
    console.log('Parameters:', parameters);

    // Step 5: Build and submit payment
    const requestParams: RequestParameter[] = [
      { id: 1, key: 'phoneNumber', value: '0501234567' },
      { id: 2, key: 'amount', value: '100' }
    ];

    const paymentResponse = await PaymentAPI.processPayment(
      services[0].id,
      '1234567890',
      requestParams
    );

    if (paymentResponse.success) {
      console.log('Payment successful:', paymentResponse.data);

      // Step 6: Check transaction status
      const transactionId = paymentResponse.data.response_params.find(
        p => p.key === 'transactionId'
      )?.value;

      if (transactionId) {
        const statusResponse = await PaymentAPI.checkTransactionStatus(transactionId);
        console.log('Transaction status:', statusResponse.data.trnStatusDisc);
      }
    }
  } catch (error) {
    console.error('Payment flow failed:', error);
  }
};
```

---

## Parameter System

The API uses a dynamic parameter system that allows services to define their own input/output requirements.

### Parameter Input Types

| Code | Type | Description | React Native Component |
|------|------|-------------|------------------------|
| `1` | Text | Single-line text input | `<TextInput>` |
| `2` | Menu/Dropdown | Selection from options | `<Picker>` or custom dropdown |
| `3` | Checkbox | Boolean selection | `<CheckBox>` |
| `4` | Textarea | Multi-line text | `<TextInput multiline>` |
| `5` | Radio | Single selection from group | Custom radio group |
| `6` | Shared | Reference to another parameter | N/A |

### Parameter JSON Types

| Code | Type | Example | Usage |
|------|------|---------|-------|
| `1` | Primitive | `"value"`, `123`, `true` | Simple values |
| `2` | Object | `{"key": "value"}` | Structured data |
| `3` | Array | `[1, 2, 3]` | Lists |
| `4` | Array of Objects | `[{}, {}]` | Complex lists |

### Parameter Types (Usage)

| Code | Type | Description |
|------|------|-------------|
| `1` | Input Only | User provides this value |
| `2` | Output Only | API returns this value |
| `3` | Both | Used for input and output |
| `4` | Mapping | Internal mapping parameter |

### Validation Rules

The system validates parameters based on:

1. **Required Fields**: All parameters defined for a service must be present
2. **String Length**: Exact length match if `length > 0`
3. **Numeric Range**: Value must be between `minValue` and `maxValue`
4. **Regex Pattern**: Value must match the regex if defined
5. **Options**: Value must be from the defined options list (for menu/dropdown)

### Building Dynamic Forms

```typescript
const buildDynamicForm = (parameters: ParameterResponse[]) => {
  // Sort by display order
  const sortedParams = parameters.sort((a, b) => a.orderNo - b.orderNo);

  return sortedParams.map(param => {
    switch (param.inputType) {
      case 1: // Text
        return {
          type: 'text',
          key: param.internalKey,
          label: param.name,
          placeholder: param.description,
          validation: {
            regex: param.regex,
            length: param.length > 0 ? param.length : null,
            min: param.minValue > 0 ? param.minValue : null,
            max: param.maxValue > 0 ? param.maxValue : null
          }
        };

      case 2: // Menu/Dropdown
        return {
          type: 'select',
          key: param.internalKey,
          label: param.name,
          options: param.option.map(opt => ({
            label: opt.name,
            value: opt.value
          }))
        };

      case 3: // Checkbox
        return {
          type: 'checkbox',
          key: param.internalKey,
          label: param.name
        };

      default:
        return null;
    }
  }).filter(Boolean);
};

// Usage
const parameters = await PaymentAPI.getServiceParameters(serviceId);
const formConfig = buildDynamicForm(parameters);
console.log('Dynamic form configuration:', formConfig);
```

---

## Best Practices

### 1. Always Fetch Service Parameters First

Before building a payment form, always fetch the service parameters:

```typescript
const parameters = await PaymentAPI.getServiceParameters(serviceId);
```

This ensures your form matches the current service configuration.

### 2. Validate User Input

Implement client-side validation based on parameter definitions:

```typescript
const validateInput = (param: ParameterResponse, value: string): boolean => {
  // Check regex
  if (param.regex && !new RegExp(param.regex).test(value)) {
    return false;
  }

  // Check length
  if (param.length > 0 && value.length !== param.length) {
    return false;
  }

  // Check numeric range
  if (param.minValue > 0 || param.maxValue > 0) {
    const num = parseFloat(value);
    if (num < param.minValue || num > param.maxValue) {
      return false;
    }
  }

  return true;
};
```

### 3. Handle Multi-language Responses

Set the appropriate language header based on user preference:

```typescript
PaymentAPI.setLanguage(userPreferredLanguage === 'Arabic' ? 'ar' : 'en');
```

### 4. Store Transaction IDs

Always store transaction IDs for status checking and customer support:

```typescript
const transactionId = paymentResponse.data.response_params.find(
  p => p.key === 'transactionId'
)?.value;

// Store in AsyncStorage or Redux
await AsyncStorage.setItem(`transaction_${Date.now()}`, transactionId);
```

### 5. Implement Proper Error Handling

```typescript
try {
  const result = await PaymentAPI.processPayment(...);
  if (result.success) {
    // Handle success
  } else {
    // Handle business logic error
    Alert.alert('Payment Failed', result.responseMessage);
  }
} catch (error) {
  // Handle network/system error
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      Alert.alert('Network Error', 'Please check your internet connection');
    }
  }
}
```

### 6. Use TypeScript

Define types for better type safety and IDE support:

```typescript
import { PaymentResponse, ServiceResponse, ParameterResponse } from './types';
```

### 7. Cache Service Configurations

Cache service and parameter configurations to reduce API calls:

```typescript
import AsyncStorage from '@react-native-async-storage/async-storage';

const getCachedServices = async (): Promise<ServiceResponse[]> => {
  const cached = await AsyncStorage.getItem('services');
  if (cached) {
    const { data, timestamp } = JSON.parse(cached);
    const isExpired = Date.now() - timestamp > 24 * 60 * 60 * 1000; // 24 hours

    if (!isExpired) {
      return data;
    }
  }

  // Fetch fresh data
  const services = await PaymentAPI.getServices();
  await AsyncStorage.setItem('services', JSON.stringify({
    data: services,
    timestamp: Date.now()
  }));

  return services;
};
```

### 8. Implement Retry Logic

```typescript
const retryRequest = async <T>(
  fn: () => Promise<T>,
  retries: number = 3,
  delay: number = 1000
): Promise<T> => {
  try {
    return await fn();
  } catch (error) {
    if (retries > 0 && axios.isAxiosError(error) && !error.response) {
      await new Promise(resolve => setTimeout(resolve, delay));
      return retryRequest(fn, retries - 1, delay * 2);
    }
    throw error;
  }
};

// Usage
const result = await retryRequest(() => PaymentAPI.processPayment(...));
```

---

## Security Considerations

### Current Status

**WARNING:** The API currently has **NO authentication or authorization** implemented.

- No API keys required
- No OAuth2/JWT validation
- No rate limiting
- CORS configuration is commented out

### Production Recommendations

Before deploying to production, implement:

#### 1. Authentication & Authorization

**Option A: API Key Authentication**
```typescript
// Add API key to all requests
const api = axios.create({
  headers: {
    'X-API-Key': 'your-api-key-here'
  }
});
```

**Option B: OAuth 2.0 / JWT**
```typescript
// Store and use JWT tokens
const api = axios.create({
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

// Refresh token when expired
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const newToken = await refreshAccessToken();
      error.config.headers.Authorization = `Bearer ${newToken}`;
      return axios.request(error.config);
    }
    return Promise.reject(error);
  }
);
```

#### 2. HTTPS/TLS

Always use HTTPS in production:

```typescript
const API_BASE_URL = 'https://api.yourcompany.com/v1'; // Not http://
```

#### 3. Rate Limiting

Implement client-side rate limiting:

```typescript
import PQueue from 'p-queue';

const queue = new PQueue({ concurrency: 1, interval: 1000, intervalCap: 10 });

const rateLimitedRequest = async <T>(fn: () => Promise<T>): Promise<T> => {
  return queue.add(fn);
};
```

#### 4. Input Sanitization

Always sanitize user input before sending to API:

```typescript
const sanitizeInput = (value: string): string => {
  return value.trim().replace(/[<>]/g, '');
};
```

#### 5. Secure Storage

Store sensitive data securely:

```typescript
import * as SecureStore from 'expo-secure-store';

// Store tokens securely
await SecureStore.setItemAsync('accessToken', token);

// Retrieve tokens
const token = await SecureStore.getItemAsync('accessToken');
```

#### 6. Certificate Pinning

For high-security applications:

```typescript
// iOS: Add certificates to Info.plist
// Android: Add certificates to network_security_config.xml
```

#### 7. Request Signing

Sign critical requests:

```typescript
import CryptoJS from 'crypto-js';

const signRequest = (payload: object, secretKey: string): string => {
  const message = JSON.stringify(payload);
  return CryptoJS.HmacSHA256(message, secretKey).toString();
};

// Add signature to request
const signature = signRequest(requestBody, SECRET_KEY);
headers['X-Signature'] = signature;
```

---

## Appendix

### Transaction Status Codes

| Code | Status | Description |
|------|--------|-------------|
| `0` | Failed/Pending | Transaction failed or awaiting processing |
| `1` | Success | Transaction completed successfully |
| `2` | In Progress | Transaction is being processed |
| `3` | Cancelled | Transaction was cancelled |

### Service Types

- **INQUIRY**: Information lookup only (no payment)
- **PAYMENT**: Payment transaction
- **BOTH**: Supports both inquiry and payment
- **CHECKSTATUS**: Status checking service
- **DATASOURCE**: External data source integration

### HTTP Status Codes

| Status | Code | Description |
|--------|------|-------------|
| Success | 200 | Request successful |
| Bad Request | 400 | Invalid request or validation error |
| Not Found | 404 | Resource not found |
| Internal Server Error | 500 | Server error |

### Sample Test Data

For testing purposes, use these sample values:

```typescript
// Test service ID
const TEST_SERVICE_ID = 1;

// Test account
const TEST_ACCOUNT = '1234567890';

// Test parameters (adjust based on actual service)
const TEST_PARAMETERS = [
  { id: 1, key: 'phoneNumber', value: '0501234567' },
  { id: 2, key: 'amount', value: '100' }
];
```

### Backend Source Files Reference

**Controllers:**
- `src/main/java/com/az/payment/controller/PaymentController.java`
- `src/main/java/com/az/payment/controller/ServiceController.java`
- `src/main/java/com/az/payment/controller/CategoryController.java`
- `src/main/java/com/az/payment/controller/BillerController.java`
- `src/main/java/com/az/payment/controller/ParameterController.java`

**Services:**
- `src/main/java/com/az/payment/service/PaymentService.java`
- `src/main/java/com/az/payment/service/ServiceService.java`
- `src/main/java/com/az/payment/service/ClientApi.java`

**Request/Response DTOs:**
- `src/main/java/com/az/payment/request/payment/ProcessRequest.java`
- `src/main/java/com/az/payment/response/PaymentResponse.java`
- `src/main/java/com/az/payment/response/ApiResponse.java`

**Configuration:**
- `src/main/resources/application.yml`
- `src/main/resources/logback-spring.xml`

---

## Support & Contact

For questions, issues, or feature requests, please contact:

- **Email:** [your-email@company.com]
- **Documentation:** [link-to-docs]
- **Issue Tracker:** [link-to-issues]

---

**Document Version:** 1.0
**Last Updated:** January 2026
**API Version:** v1
