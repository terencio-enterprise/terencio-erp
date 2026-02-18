# Authentication System

## 1. Overview
The **Terencio ERP** uses a **JWT-based authentication** system secured with **HttpOnly Cookies**. This approach mitigates XSS attacks by preventing JavaScript access to tokens, while enhancing security and user experience.

The system entities traditionally referred to as "Users" are now **Employees**.

## 2. Authentication Flow

### 2.1 Login
- **Endpoint:** `POST /api/v1/auth/login`
- **Request Body:** `LoginRequest`
  ```json
  {
    "username": "admin",
    "password": "123"
  }
  ```
- **Response:** 200 OK with `Set-Cookie` headers.
  - **Body:** `ApiResponse<LoginResponse>`
  ```json
  {
    "success": true,
    "message": "Login successful",
    "data": {
      "token": "ey... (Access Token)",
      "type": "Bearer",
      "username": "admin",
      "role": "ROLE_ADMIN"
    },
    "meta": {
      "timestamp": "2023-10-27T10:00:00Z"
    }
  }
  ```
- **Cookies Set:**
  - `ACCESS_TOKEN`: The JWT access token (HttpOnly, Secure).
  - `REFRESH_TOKEN`: The JWT refresh token (HttpOnly, Secure, Path=/api/v1/auth/refresh).

### 2.2 Accessing Protected Resources
- The client (browser) automatically sends the HttpOnly cookies with every request.
- **Header (Optional):** You can also send the token via Authorization header if not using cookies.
  `Authorization: Bearer <token>`

### 2.3 Token Refresh
- **Endpoint:** `POST /api/v1/auth/refresh`
- **Request:** Empty body (Uses `REFRESH_TOKEN` cookie).
- **Response:** 200 OK with new Access Token cookie.
  - **Body:** `ApiResponse<LoginResponse>`
  ```json
  {
    "success": true,
    "message": "Token refreshed successfully",
    "data": {
      "token": "ey... (New Access Token)",
      "type": "Bearer",
      "username": "admin",
      "role": "ROLE_ADMIN"
    },
    "meta": {
      "timestamp": "2023-10-27T10:15:00Z"
    }
  }
  ```

### 2.4 Logout
- **Endpoint:** `POST /api/v1/auth/logout`
- **Request:** Empty body.
- **Response:** 200 OK
  - **Body:** `ApiResponse<Void>`
  ```json
  {
    "success": true,
    "message": "Logout successful",
    "data": null,
    "meta": {
      "timestamp": "2023-10-27T10:30:00Z"
    }
  }
  ```

### 2.5 Get Current Employee
- **Endpoint:** `GET /api/v1/auth/me`
- **Response:** 200 OK
  - **Body:** `ApiResponse<EmployeeInfoDto>`
  ```json
  {
    "success": true,
    "message": "User info fetched successfully",
    "data": {
      "id": 1,
      "username": "admin",
      "fullName": "Administrator",
      "role": "ADMIN",
      "organizationId": null,
      "companyId": null,
      "storeId": null,
      "grants": [],
      "active": true
    },
    "meta": {
      "timestamp": "2023-10-27T10:05:00Z"
    }
  }
  ```

## 3. Data Transfer Objects (DTOs)

### LoginRequest
| Field | Type | Description |
| :--- | :--- | :--- |
| `username` | String | The employee's unique username. |
| `password` | String | The employee's password or PIN. |

### LoginResponse
| Field | Type | Description |
| :--- | :--- | :--- |
| `token` | String | The JWT Access Token. |
| `type` | String | Token type (default "Bearer"). |
| `username` | String | The authenticated username. |
| `role` | String | The assigned role (e.g., ROLE_ADMIN). |

### EmployeeInfoDto
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | Long | Internal Database ID. |
| `username` | String | Unique username. |
| `fullName` | String | Full name of the employee. |
| `role` | String | Assigned Role. |
| `organizationId` | UUID | Associated Organization ID (if any). |
| `companyId` | UUID | Associated Company ID (if any). |
| `storeId` | UUID | Associated Store ID (if any). |
| `grants` | Array | List of specific access grants. |
| `active` | Boolean | Account status. |

## 4. Error Response

All API errors follow a standard structure.

### TypeScript Interface

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  error?: ApiError;
  meta: {
    timestamp: string; // ISO 8601
  };
}

interface ApiError {
  code: string;       // Machine-readable code (e.g., "VALIDATION_ERROR")
  message: string;    // Human-readable error message
  details?: ErrorDetail[];
}

interface ErrorDetail {
  field: string;      // The field that caused the error
  message: string;    // Description of the specific error
}
```

### Example Error Response

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "One or more fields are invalid",
    "details": [
      {
        "field": "username",
        "message": "must not be blank"
      }
    ]
  },
  "meta": {
    "timestamp": "2023-10-27T10:00:00Z"
  }
}
```

## 5. Default Admin Employee
On application startup, the system checks for the existence of a default admin employee. If not found, it is created automatically:

- **Username:** `admin` (configurable via `app.admin.username`)
- **Password:** `123` (configurable via `app.admin.password`)
- **Role:** `ADMIN`
- **Permissions:** Global access (no specific Store/Company restrictions).

> **Note:** For production, it is highly recommended to change the admin password immediately after the first login.
