Add a new frontend page to BOCRM following the established patterns.

Ask me for:
1. Page name (e.g. `Invoices`)
2. Route path (e.g. `/invoices`)
3. Whether it's admin-only or all users
4. Whether it needs a list view, form view, or both

Then implement:

**List page** (`src/pages/FoosPage.tsx`):
- Follow `CustomersPage.tsx` as the canonical template
- Use `DataTable` component with `ColumnDefinition[]`
- State: `listState` from `useListState` hook for pagination/sort/filter
- Fetch via `apiClient.getFoos(params)` with `useEffect` on `listState` + `dataRefreshToken`
- Set header actions via `useUiStore().setHeaderActions` in a `useEffect`

**Form page** (`src/pages/FooFormPage.tsx`):
- Follow `CustomerFormPage.tsx` as the canonical template
- Use `useParams()` for edit mode (`id` param present = edit, absent = create)
- Call `apiClient.createFoo()` or `apiClient.updateFoo(id, data)`

**Wire up:**
1. Add route in `src/App.tsx` inside `<Routes>` wrapped in `<ProtectedRoute>` (admin-only: add role check)
2. Add API methods to `src/api/apiClient.ts`
3. Add nav link in `src/components/Layout.tsx` sidebar if it should appear in nav

**CSS:** No inline styles, no new CSS frameworks — use existing Tailwind classes.
