# BOCRM API Tests

Comprehensive HTTP test suite for the BOCRM (Business Operational CRM) API.

## Prerequisites

1. **IntelliJ IDEA** or **VS Code** with REST Client extension
2. Running BOCRM backend server (default: `http://localhost:8080`)
3. Demo user credentials (configured in `http-client.env.json`)

## Test Files Overview

| File | Description | Test Count |
|------|-------------|------------|
| `auth.http` | Authentication & token management | 2 tests |
| `01-customers-comprehensive.http` | Customer CRUD, search, filters, validation | 25 tests |
| `02-contacts-comprehensive.http` | Contact CRUD, search, filters, customer relations | 30 tests |
| `03-opportunities-comprehensive.http` | Opportunity management, stages, pipeline | 37 tests |
| `04-activities-comprehensive.http` | Activity management, types, status tracking | 42 tests |
| `05-custom-fields.http` | Custom field definitions, configurations | 15 tests |
| `06-timeline-chat-bulk-reporting.http` | Advanced features: timeline, chat, bulk ops, reports | 21 tests |

**Total: 172 comprehensive API tests**

## Quick Start

### 1. Configure Environment

Edit `http-client.env.json`:

```json
{
  "dev": {
    "baseUrl": "http://localhost:8080/api",
    "demoEmail": "demo@bocrm.com",
    "demoPassword": "demo123"
  }
}
```

### 2. Run Tests in Order

**Recommended execution order:**

1. **Authentication** - `auth.http`
   - Login and capture access token

2. **Core Entities** (in order)
   - `01-customers-comprehensive.http`
   - `02-contacts-comprehensive.http`
   - `03-opportunities-comprehensive.http`
   - `04-activities-comprehensive.http`

3. **Advanced Features**
   - `05-custom-fields.http`
   - `06-timeline-chat-bulk-reporting.http`

### 3. Using IntelliJ IDEA

1. Open any `.http` file
2. Click the green arrow (▶) next to any request
3. View results in the "Run" panel at the bottom

### 4. Using VS Code

1. Install "REST Client" extension by Huachao Mao
2. Open any `.http` file
3. Click "Send Request" above each request
4. View results in a new tab

## Test Features

### Automated Test Assertions

Tests include automated assertions that verify:
- HTTP status codes
- Response structure
- Data integrity
- Business logic rules

Example:
```javascript
> {%
    client.test("Status is 201", function() {
        client.assert(response.status === 201, "Expected 201");
    });
    client.test("Response has id", function() {
        client.assert(response.body.id !== undefined, "Response should have id");
    });
%}
```

### Variable Capture

Tests automatically capture and reuse IDs:
```javascript
> {%
    client.global.set("customerId", response.body.id);
    client.log("✓ Created customer with ID: " + response.body.id);
%}
```

### Test Categories

Each test file includes:

1. **CRUD Operations**
   - Create (POST)
   - Read/Get (GET)
   - Update (PUT)
   - Delete (DELETE)

2. **Search & Filter**
   - Keyword search
   - Field-specific filters
   - Combined filters
   - Sorting

3. **Pagination**
   - Page size control
   - Page navigation
   - Total counts

4. **Validation Tests**
   - Required fields
   - Invalid data
   - Edge cases

5. **Error Handling**
   - 404 Not Found
   - 400 Bad Request
   - 401 Unauthorized
   - 422 Validation Errors

## Test Examples

### Customer CRUD
```http
### Create Customer
POST {{baseUrl}}/customers
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "name": "Acme Corporation",
  "status": "active"
}
```

### Search with Filters
```http
### Search Active Customers
GET {{baseUrl}}/customers?search=acme&status=active&sortBy=name&sortOrder=asc
Authorization: Bearer {{accessToken}}
```

### Update with Partial Data
```http
### Update Customer Status
PUT {{baseUrl}}/customers/{{customerId}}
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "status": "inactive"
}
```

## Custom Field Testing

Tests include comprehensive custom field scenarios:
- Text fields with validation
- Number fields with min/max
- Select fields with options
- Date fields
- Boolean flags

## Advanced Testing

### Pipeline Analysis
```http
### Calculate Pipeline Value
GET {{baseUrl}}/opportunities?stage=prospecting,discovery,proposal,negotiation
Authorization: Bearer {{accessToken}}

> {%
    let totalValue = 0;
    let weightedValue = 0;
    response.body.content.forEach(opp => {
        totalValue += opp.value || 0;
        weightedValue += (opp.value || 0) * ((opp.probability || 0) / 100);
    });
    client.log("Total Pipeline: $" + totalValue.toLocaleString());
    client.log("Weighted Pipeline: $" + weightedValue.toLocaleString());
%}
```

### Activity Metrics
```http
### Get Activity Summary
GET {{baseUrl}}/activities
Authorization: Bearer {{accessToken}}

> {%
    let byType = {};
    let byStatus = {};
    response.body.content.forEach(activity => {
        byType[activity.type] = (byType[activity.type] || 0) + 1;
        byStatus[activity.status] = (byStatus[activity.status] || 0) + 1;
    });
    client.log("Activity Metrics:");
    client.log("By Type: " + JSON.stringify(byType));
    client.log("By Status: " + JSON.stringify(byStatus));
%}
```

## Integration Testing

File `06-timeline-chat-bulk-reporting.http` includes a full integration workflow:

1. Create Customer
2. Create Contact for Customer
3. Create Opportunity for Customer
4. Create Activity related to Customer
5. Verify Timeline shows all events

## Error Testing

Each test file includes error scenarios:

- Missing required fields
- Invalid data types
- Non-existent resource IDs
- Unauthorized access
- Duplicate keys
- Invalid enum values
- Out-of-range values

## Troubleshooting

### Tests Failing with 401 Unauthorized

1. Run `auth.http` first to get a fresh token
2. Ensure `accessToken` variable is set
3. Check token expiration (refresh if needed)

### Tests Failing with 404 Not Found

1. Ensure prerequisites are run first
2. Check that referenced IDs exist
3. Verify variables are captured correctly

### Server Not Responding

1. Confirm backend is running: `http://localhost:8080/actuator/health`
2. Check `baseUrl` in `http-client.env.json`
3. Verify port number matches your configuration

## Best Practices

1. **Run Auth First**: Always authenticate before running other tests
2. **Run in Order**: Execute test files in numerical order
3. **Check Logs**: Review console output for captured IDs
4. **Clean Data**: Consider cleaning up test data periodically
5. **Use Assertions**: Review test assertions to ensure quality

## Coverage Matrix

| Feature | Create | Read | Update | Delete | Search | Filter | Sort | Validate |
|---------|--------|------|--------|--------|--------|--------|------|----------|
| Customers | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Contacts | ✅ | ✅ | ✅ | - | ✅ | ✅ | ✅ | ✅ |
| Opportunities | ✅ | ✅ | ✅ | - | ✅ | ✅ | ✅ | ✅ |
| Activities | ✅ | ✅ | ✅ | - | ✅ | ✅ | ✅ | ✅ |
| Custom Fields | ✅ | ✅ | ✅ | ✅ | - | ✅ | - | ✅ |
| Timeline | - | ✅ | - | - | - | - | - | - |
| Chat | ✅ | ✅ | - | - | - | - | - | - |
| Bulk Operations | - | - | ✅ | ✅ | - | - | - | - |
| Reporting | - | ✅ | - | - | - | ✅ | - | - |

## Contributing

When adding new tests:

1. Follow the existing naming convention
2. Include test assertions
3. Capture and reuse IDs
4. Add logging for clarity
5. Test both success and error cases
6. Update this README

## Support

For issues or questions:
- Check the [main project README](../../README.md)
- Review API documentation at `http://localhost:8080/swagger-ui.html`
- Check backend logs for detailed error messages

---

**Last Updated**: 2026-02-18
**Test Suite Version**: 1.0
**Total Tests**: 172
