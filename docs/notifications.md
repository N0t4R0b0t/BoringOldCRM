# Email Notifications & Inbox System

BOCRM's notification system sends **ownership-aware** email alerts and persistent in-app messages. It uses the **outbox pattern** for guaranteed delivery via Spring Mail SMTP.

## Overview

### Trigger Events

| Event | Recipients | Example |
|-------|-----------|---------|
| **RECORD_MODIFIED** | Record owner | "Jane Smith updated your Opportunity: Acme Deal Q1" |
| **OWNERSHIP_ASSIGNED** | New owner | "You've been assigned ownership of Customer: Acme Corp" |
| **ACCESS_GRANTED** | Grantee | "You've been granted READ access to CustomRecord: Server Rack A" |
| **ACTIVITY_DUE_SOON** | Activity owner | "Reminder: Activity due tomorrow — Call with Acme" |
| **DAILY_INSIGHT** | (opt-in) User | AI-generated daily CRM summary (requires LLM) |

All events are **ownership-aware**: record owners don't notify themselves on their own edits, and notifications respect the new access control model (`RecordAccessGrant`, `UserGroup`).

## Architecture

### Flow Diagram

```
┌─────────────────────┐
│  CRM Services       │  (CustomerService, OpportunityService, etc.)
│  (on write)         │
└──────────┬──────────┘
           │
           v
┌──────────────────────────────────┐
│ notificationService.notify*()     │  (enqueue OutboxEvent)
│ (same transaction as write)       │
└──────────┬───────────────────────┘
           │
           v
┌──────────────────────────────────┐
│ OutboxEvent table                 │  (tenantId, eventType,
│ (per tenant schema)               │   payloadJsonb, publishedAt)
└──────────┬───────────────────────┘
           │
           v
┌──────────────────────────────────┐
│ NotificationPoller               │  @Scheduled(60s interval)
│ (reads unpublished events)        │  + @Scheduled(8 AM daily)
└──────────┬───────────────────────┘
           │
           v
┌──────────────────────────────────┐
│ NotificationDispatchService      │  • Checks user preferences
│ (per event)                       │  • Checks tenant settings
└──────────┬──────────────────────┬┘
           │                      │
           v                      v
┌─────────────────────┐  ┌──────────────────┐
│ NotificationInbox   │  │ Spring Mail      │
│ (write inbox row)   │  │ (send email)     │
└─────────────────────┘  └──────────────────┘
```

### Components

#### 1. **NotificationService** (`service/NotificationService.java`)
- Called from CRM services and controllers
- Enqueues `OutboxEvent` rows with typed payloads
- Methods:
  - `notifyRecordModified(tenantId, actorId, actorName, ownerId, entityType, entityId, entityName)`
  - `notifyOwnershipAssigned(tenantId, newOwnerId, actorId, actorName, entityType, entityId, entityName)`
  - `notifyAccessGranted(tenantId, recipientUserId, actorId, actorName, entityType, entityId, entityName, permission)`
  - `notifyActivityDueSoon(tenantId, ownerId, activityId, subject, dueAt)`
  - `notifyDailyInsight(tenantId, userId, insightText)`

#### 2. **NotificationPoller** (`service/NotificationPoller.java`)
- Background scheduled tasks:
  - `poll()` — runs every 60 seconds, processes notification events
  - `scanDueActivities()` — runs daily at 8 AM, identifies activities due within 24 hours
- Iterates all tenants, sets `TenantContext` temporarily (only acceptable place to do this outside JWT filter)
- Handles retry logic: increments `retryCount` on transient failures (SMTP timeout), abandons after 3 retries

#### 3. **NotificationDispatchService** (`service/NotificationDispatchService.java`)
- Receives `OutboxEvent` from poller
- Validates:
  1. Tenant email enabled (from `tenant_settings.settings_jsonb.email.enabled`)
  2. User preferences opted-in (from `user.preferences.notifications.<type>`)
  3. User not muted (checks `muted` flag or `mutedUntil` timestamp)
- Actions:
  1. Writes `NotificationInbox` record (always, even if email disabled or opted-out)
  2. Sends email via `JavaMailSender` (gracefully skips if not configured)
- Returns `true` if fully processed (success or intentional skip), `false` for transient failure

#### 4. **EmailTemplateService** (`service/EmailTemplateService.java`)
- Renders HTML email content
- One method per notification type
- Embedded HTML with CSS variables for tenant branding
- Subject: `[BOCRM] <event-specific headline>`
- Body: Table-based layout (email-client safe)

#### 5. **NotificationInboxService** (`service/NotificationInboxService.java`)
- CRUD for user's notification inbox
- Methods:
  - `getInbox(page, size)` — returns `PagedResponse<NotificationInboxDTO>` ordered by `createdAt DESC`
  - `getUnreadCount()` — counts rows where `readAt IS NULL`
  - `markRead(notificationId)` — sets `readAt = now`
  - `markAllRead()` — updates all unread to read for current user

#### 6. **NotificationController** (`controller/NotificationController.java`)
- REST endpoints:
  - `GET /notifications/inbox` — list user notifications (paginated)
  - `GET /notifications/inbox/unread-count` — get unread count
  - `POST /notifications/inbox/{id}/read` — mark one as read
  - `POST /notifications/inbox/read-all` — mark all as read
  - `GET /notifications/preferences` — get user's preferences (from `user.preferences` JSONB)
  - `PUT /notifications/preferences` — update preferences (merge into `user.preferences`)

## Database Schema

### notification_inbox (per-tenant)
```sql
CREATE TABLE notification_inbox (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  entity_type VARCHAR(50),
  entity_id BIGINT,
  actor_user_id BIGINT,
  actor_display_name VARCHAR(255),
  message TEXT NOT NULL,
  read_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_notification_inbox_user ON notification_inbox(tenant_id, user_id, created_at DESC);
CREATE INDEX idx_notification_inbox_unread ON notification_inbox(tenant_id, user_id, read_at) WHERE read_at IS NULL;
```

### OutboxEvent enhancements
```sql
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS retry_count INT DEFAULT 0;

-- Index for faster poller queries
CREATE INDEX idx_outbox_events_notification ON outbox_events(published_at, event_type) WHERE published_at IS NULL;
```

### OutboxEvent payload schema (for `eventType="NOTIFICATION_EMAIL"`)
```json
{
  "notificationType": "RECORD_MODIFIED",
  "recipientUserId": 42,
  "actorUserId": 17,
  "actorDisplayName": "Jane Smith",
  "tenantId": 3,
  "entityType": "Opportunity",
  "entityId": 99,
  "entityName": "Acme Deal Q1",
  "permission": null,
  "dueAt": null,
  "retryCount": 0
}
```

## Configuration

### Email Setup (application.yml or env vars)

**SMTP via Spring Mail:**
```yaml
spring:
  mail:
    host: ${MAIL_HOST}                    # Required to enable (e.g., smtp.gmail.com)
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

app:
  mail:
    default-from: ${MAIL_FROM:noreply@bocrm.com}
    default-from-name: ${MAIL_FROM_NAME:BOCRM}
    activity-due-reminder-hours: ${MAIL_ACTIVITY_REMINDER_HOURS:24}
```

**Common SMTP providers:**
- **Gmail**: `smtp.gmail.com:587` (app password required)
- **AWS SES**: `email-smtp.<region>.amazonaws.com:587`
- **Mailgun**: `smtp.mailgun.org:587`
- **Postfix**: `localhost:25` (local server)

**Disable email (inbox-only mode):**
```yaml
spring:
  mail:
    host:  # Empty → disables email, inbox still works
```

### Tenant-level email settings (in `tenant_settings.settings_jsonb`)
```json
{
  "email": {
    "enabled": true,
    "senderName": "Acme CRM Team",
    "senderEmail": "crm@acme.com"
  }
}
```

If not set, defaults to `app.mail.default-from-name` and `app.mail.default-from`.

## User Preferences

Stored in `user.preferences` JSONB (in `users.preferences` column):
```json
{
  "notifications": {
    "recordModified": true,
    "ownershipAssigned": true,
    "accessGranted": true,
    "activityDueSoon": true,
    "dailyInsight": false,
    "muted": false,
    "mutedUntil": null
  }
}
```

- Default behavior (if key absent): all notifications enabled
- `muted: true` → all notifications paused until `mutedUntil` timestamp
- Individual flags: `false` → opt out of that notification type
- Updated via `PUT /notifications/preferences` endpoint

## Frontend

### Components

**NotificationsPage** (`frontend/src/pages/NotificationsPage.tsx`)
- Inbox with pagination, list of user's notifications
- Color-coded by notification type
- Mark-as-read button (individual or bulk)
- Unread badge styling

**Bell Icon** (`frontend/src/components/Layout.tsx`)
- In header, next to dark mode toggle
- Shows unread count badge (red, "9+" if over 9)
- Clickable link to `/notifications`
- Loads unread count on mount

**API Client** (`frontend/src/api/apiClient.ts`)
- `getNotificationInbox(params)` → `PagedResponse<NotificationInboxItem>`
- `getUnreadNotificationCount()` → `{ unreadCount: number }`
- `markNotificationRead(id)` → `{ id, read: true }`
- `markAllNotificationsRead()` → success
- `getNotificationPreferences()` → `NotificationPreferences`
- `updateNotificationPreferences(prefs)` → updated preferences

**Types** (`frontend/src/types/notifications.ts`)
```typescript
interface NotificationInboxItem {
  id: number;
  notificationType: string;
  entityType?: string;
  entityId?: number;
  actorUserId?: number;
  actorDisplayName?: string;
  message: string;
  read: boolean;
  createdAt: string;
}

interface NotificationPreferences {
  recordModified?: boolean;
  ownershipAssigned?: boolean;
  accessGranted?: boolean;
  activityDueSoon?: boolean;
  dailyInsight?: boolean;
  muted?: boolean;
  mutedUntil?: string;
}
```

## Integration with Existing Services

### CustomerService, OpportunityService, ActivityService, CustomRecordService, TenantDocumentService
```java
// In createXxx():
if (saved.getOwnerId() != null && !saved.getOwnerId().equals(currentUserId)) {
    notificationService.notifyOwnershipAssigned(tenantId, saved.getOwnerId(),
        currentUserId, "System", entityType, saved.getId(), saved.getName());
}

// In updateXxx():
Long previousOwnerId = entity.getOwnerId();
// ... make changes ...
if (updated.getOwnerId() != null) {
    if (!updated.getOwnerId().equals(previousOwnerId) && !updated.getOwnerId().equals(currentUserId)) {
        notificationService.notifyOwnershipAssigned(...);
    } else if (!currentUserId.equals(updated.getOwnerId())) {
        notificationService.notifyRecordModified(...);
    }
}
```

### AccessControlService
```java
// In addGrant():
if ("USER".equals(granteeType)) {
    notificationService.notifyAccessGranted(tenantId, granteeId, userId, "System",
        entityType, entityId, entityName, permission);
}
```

## Testing

### In Integration Tests
```java
@Transactional
public class NotificationTest extends BaseIntegrationTest {
    @Autowired
    private NotificationInboxRepository inboxRepository;

    @Test
    public void testRecordModifiedNotification() {
        // Create a customer as user A
        testTenantId = testTenant.getId();
        Long ownerId = testUser.getId(); // User A
        CustomerDTO customer = customerService.createCustomer(
            CreateCustomerRequest.builder().name("Test").ownerId(ownerId).build()
        );

        // Modify as user B
        TenantContext.setUserId(otherUserId);
        customerService.updateCustomer(customer.getId(),
            UpdateCustomerRequest.builder().name("Updated").build()
        );

        // Verify inbox entry was written
        long unread = inboxRepository.countByTenantIdAndUserIdAndReadAtIsNull(testTenantId, ownerId);
        assertEquals(1, unread);

        NotificationInbox notif = inboxRepository.findByTenantIdAndUserId(testTenantId, ownerId, PageRequest.of(0, 1)).getContent().get(0);
        assertEquals("RECORD_MODIFIED", notif.getNotificationType());
    }
}
```

### Disable Email in Test Profile
```yaml
# application-test.yml
spring:
  mail:
    host:  # Empty — no email in tests, only inbox
```

## Troubleshooting

### Email not sending
1. Check `MAIL_HOST` is configured in `application.yml` or env vars
2. Check SMTP credentials (`MAIL_USERNAME`, `MAIL_PASSWORD`)
3. Check firewall/network access to SMTP server (port 587 or 25)
4. Check `tenant_settings.settings_jsonb.email.enabled = true`
5. Check user preferences: `user.preferences.notifications.<type> != false`
6. Check poller logs: `NotificationPoller` should log dispatch attempts every 60 seconds
7. Use `MailHog` or similar local SMTP for local testing

### Notifications not appearing in inbox
1. Verify `NotificationDispatchService.dispatch()` was called
2. Check `NotificationInboxService.getUnreadCount()` endpoint
3. Check database: `SELECT * FROM tenant_<id>.notification_inbox WHERE user_id = ?`
4. Check poller is running: `@Scheduled` task requires `@EnableScheduling` in config

### High OutboxEvent retry count
- Indicates SMTP failures; check mail server logs and network
- Events with `retryCount >= 3` are abandoned (marked published) to prevent infinite loops
- Consider alerting on high retry counts in production

## Future Enhancements

- **Digest mode**: batch notifications into daily/weekly summaries
- **Webhook delivery**: POST notification payload to configurable URL (Slack integration)
- **Mention system**: @username in notes triggers notification
- **Notification audit**: track delivery status per event
- **SMS notifications**: add Twilio or similar for urgent alerts
- **Watchlist**: users can watch any record for all change notifications
- **Escalation alerts**: manager notified if opportunity stalls at a stage
