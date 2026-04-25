# BOCRM Frontend

React + Vite + TypeScript frontend for the Boring, Scalable CRM system.

## Features (Phase 1)

- ✅ Authentication (Login/Logout with JWT)
- ✅ Protected routes with automatic redirects
- ✅ Multi-page navigation (Dashboard, Customers, Opportunities, Activities)
- ✅ List views with CRUD operations
- ✅ Form pages for creating/editing entities
- ✅ State management with Zustand
- ✅ HTTP client with auth interceptors
- ✅ Responsive UI with CSS

## Project Structure

```
src/
├── api/
│   └── apiClient.ts          # Axios instance with auth interceptors
├── store/
│   ├── authStore.ts          # Zustand auth state (JWT, user info)
│   └── crmStore.ts           # Zustand CRM state (customers, etc)
├── components/
│   ├── Layout.tsx            # Main layout wrapper with header/nav
│   └── ProtectedRoute.tsx    # Route protection HOC
├── pages/
│   ├── LoginPage.tsx         # Login form
│   ├── DashboardPage.tsx     # Dashboard home
│   ├── CustomersPage.tsx     # Customer list view
│   ├── CustomerFormPage.tsx  # Customer create/edit
│   ├── OpportunitiesPage.tsx # Opportunity list view
│   ├── OpportunityFormPage.tsx # Opportunity create/edit
│   ├── ActivitiesPage.tsx    # Activity list view
│   └── ActivityFormPage.tsx  # Activity create/edit
├── styles/
│   ├── global.css            # Global styles and utility classes
│   ├── Login.css             # Login page styles
│   ├── Dashboard.css         # Dashboard styles
│   ├── List.css              # List view styles
│   ├── Form.css              # Form page styles
│   └── Layout.css            # Layout component styles
├── App.tsx                   # Router setup and main app
└── main.tsx                  # Entry point
```

## Setup

### Prerequisites

- Node.js 18+ (check with `node --version`)
- npm or yarn

### Installation

```bash
cd frontend
npm install
```

### Environment Variables

Create `.env` or `.env.local`:

```
VITE_API_URL=http://localhost:8080/api
VITE_OIDC_AUTHORITY=https://your-tenant.auth0.com
VITE_OIDC_CLIENT_ID=your_spa_client_id
VITE_OIDC_REDIRECT_URI=http://localhost:5173/auth/callback
# Optional overrides:
# VITE_OIDC_AUTHORIZATION_ENDPOINT=https://your-tenant.auth0.com/authorize
# VITE_OIDC_TOKEN_ENDPOINT=https://your-tenant.auth0.com/oauth/token
# VITE_OIDC_SCOPE=openid profile email
# VITE_OIDC_ORGANIZATION=org_123456
```

Recommended:
- Commit only `.env.example` with placeholders.
- Put real local values in `.env.local` (ignored by git).
- Never put external service secrets in `VITE_*` variables; these are public in browser bundles.

### Development Server

```bash
npm run dev
```

The app will be available at `http://localhost:5173`

### Build for Production

```bash
npm run build
```

Output will be in `dist/`

## Key Technologies

- **React 19.2.0** - UI library
- **Vite 7.3.1** - Build tool (fast dev server, optimized builds)
- **TypeScript 5.9** - Type safety
- **React Router 6** - Client-side routing
- **Axios** - HTTP client
- **Zustand** - Lightweight state management
- **date-fns** - Date formatting
- **clsx** - Classname utilities

## Authentication Flow

1. User enters email/password on `/login`
2. Frontend calls `POST /api/auth/login`
3. Backend returns `{userId, accessToken, refreshToken}`
4. Tokens stored in localStorage, user state in Zustand store
5. Auth interceptor adds `Authorization: Bearer <token>` to all requests
6. 401 response triggers automatic token refresh via refresh endpoint
7. Failed refresh redirects user back to `/login`

### External SSO (OIDC + PKCE)

1. User clicks `Continue with SSO` on `/login`
2. Frontend redirects to OIDC provider authorize endpoint (PKCE)
3. Provider redirects back to `/auth/callback` with `code`
4. Frontend exchanges `code` for `id_token` at token endpoint
5. Frontend calls `POST /api/auth/external/login` with the `id_token`
6. Backend validates token/JWKS and requires exactly one org in the token (chosen by IdP)
7. Backend auto-provisions missing tenant/membership for that org and returns app JWT tokens scoped to that tenant

## API Client (apiClient.ts)

Pre-configured with:
- Base URL from `VITE_API_URL` env var (defaults to `http://localhost:8080/api`)
- Authorization header injection (reads from localStorage)
- 401 response handling with automatic token refresh
- Methods for all CRUD operations (customers, opportunities, activities)

Usage:
```typescript
import { apiClient } from '../api/apiClient';

const customers = await apiClient.getCustomers();
const customer = await apiClient.createCustomer(data);
```

## State Management

### Auth Store (Zustand)

```typescript
const { user, isAuthenticated, login, logout } = useAuthStore();
```

### CRM Store (Zustand)

```typescript
const { customers, fetchCustomers, addCustomer } = useCrmStore();
```

## Form Pages

All form pages support:
- Creating new records (POST)
- Editing existing records (GET, PUT)
- Form validation
- Error display
- Submit loading state
- Cancel/redirect

## Navigation

- Dashboard (`/dashboard`) - Home page with quick stats
- Customers (`/customers`) - List all customers
- Customers Create/Edit (`/customers/new`, `/customers/:id/edit`)
- Opportunities (`/opportunities`) - List all opportunities
- Opportunities Create/Edit (`/opportunities/new`, `/opportunities/:id/edit`)
- Activities (`/activities`) - List all activities
- Activities Create/Edit (`/activities/new`, `/activities/:id/edit`)

## Demo Credentials

When running locally with backend:
- Email: `demo@bocrm.com`
- Password: `demo123`

## Next Steps (Phase 2+)

- [ ] Dynamic custom field form component
- [ ] Calculated field display
- [ ] Advanced filtering and search
- [ ] Bulk operations
- [ ] Reporting views
- [ ] AI chat integration (Phase 3)
- [ ] Multi-tenant tenant selector UI
- [ ] User/role management
- [ ] Activity timeline views
- [ ] Contact/Opportunity relationships UI

## Running Backend + Frontend Together

1. Start PostgreSQL (localhost:5432)
2. Start RabbitMQ (localhost:5672) - optional for Phase 1
3. Start backend: `cd backend && ./gradlew bootRun`
4. Start frontend: `cd frontend && npm run dev`
5. Backend Swagger docs: http://localhost:8080/swagger-ui.html
6. Frontend: http://localhost:5173/login

## Troubleshooting

### CORS Issues

If you see CORS errors:
- Ensure backend is running and listening on `http://localhost:8080`
- Check `VITE_API_URL` matches backend URL
- Backend should have CORS configured for `http://localhost:5173`

### 401 Unauthorized

- Check token in localStorage (browser DevTools > Application > Local Storage)
- Verify backend is returning valid JWT from login endpoint
- Check JWT expiration isn't too short

### Build Errors

- Clear node_modules: `rm -rf node_modules && npm install`
- Clear Vite cache: `rm -rf .vite`
- Check Node version: `node --version` (should be 18+)

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```
