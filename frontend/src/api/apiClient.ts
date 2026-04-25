/*
 * BoringOldCRM - Open-source multi-tenant CRM
 * Copyright (C) 2026 Ricardo Salvador
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Source: https://github.com/N0t4R0b0t/BoringOldCRM
 */
/**
 * @file Singleton Axios client used for every API call.
 * Auto-injects the JWT access token, handles 401 → token-refresh → retry,
 * and exposes typed methods for every backend endpoint.
 * @author Ricardo Salvador
 * @since 1.0.0
 */
import axios, { type AxiosInstance, type AxiosError } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 600000,  // 10 minutes for long-running requests (bulk operations, complex LLM calls)
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor to include auth token
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    // Add response interceptor to handle token refresh
    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as any;

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;
          const refreshToken = localStorage.getItem('refreshToken');

          if (refreshToken) {
            try {
              const response = await this.client.post('/auth/refresh', {
                refreshToken,
              });
              const { accessToken, refreshToken: newRefreshToken } = response.data;
              localStorage.setItem('accessToken', accessToken);
              // Store the new refresh token as well
              if (newRefreshToken) {
                localStorage.setItem('refreshToken', newRefreshToken);
              }
              this.client.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
              return this.client(originalRequest);
            } catch (refreshError) {
              // Refresh failed, clear tokens and redirect to login
              localStorage.removeItem('accessToken');
              localStorage.removeItem('refreshToken');
              window.location.href = '/login';
            }
          } else {
            // No refresh token available — clear stale access token before redirecting
            // so initializeAuth() doesn't restore isAuthenticated and loop back here
            localStorage.removeItem('accessToken');
            window.location.href = '/login';
          }
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth endpoints
  externalLogin(idToken: string) {
    return this.client.post('/auth/external/login', { idToken }, { timeout: 15000 });
  }

  onboard(tenantName: string) {
    return this.client.post('/auth/onboard', { tenantName });
  }

  switchTenant(tenantId: number) {
    return this.client.post('/auth/switch-tenant', { tenantId });
  }

  logout() {
    return this.client.post('/auth/logout');
  }

  updateUserPreferences(data: { preferences: string }) {
    return this.client.put('/user/preferences', data);
  }

  // Customers endpoints
  getCustomers(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
    search?: string;
    [key: string]: any;
  }) {
    return this.client.get('/customers', { params });
  }

  getCustomer(id: number) {
    return this.client.get(`/customers/${id}`);
  }

  createCustomer(data: any) {
    return this.client.post('/customers', data);
  }

  updateCustomer(id: number, data: any) {
    return this.client.put(`/customers/${id}`, data);
  }

  deleteCustomer(id: number) {
    return this.client.delete(`/customers/${id}`);
  }

  searchCustomers(term?: string, page: number = 0, size: number = 20) {
    return this.client.get('/customers/search', { params: { term, page, size } });
  }

  // Contacts endpoints
  getContacts(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
    search?: string;
    customerId?: number;
    [key: string]: any;
  }) {
    return this.client.get('/contacts', { params });
  }

  getContact(id: number) {
    return this.client.get(`/contacts/${id}`);
  }

  createContact(data: any) {
    return this.client.post('/contacts', data);
  }

  updateContact(id: number, data: any) {
    return this.client.put(`/contacts/${id}`, data);
  }

  deleteContact(id: number) {
    return this.client.delete(`/contacts/${id}`);
  }

  disassociateContact(id: number) {
    return this.client.post(`/contacts/${id}/disassociate`);
  }

  searchContacts(term?: string, page: number = 0, size: number = 20) {
    return this.client.get('/contacts/search', { params: { term, page, size } });
  }

  // Opportunities endpoints
  getOpportunities(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
    search?: string;
    customerId?: number;
    status?: string;
    typeSlug?: string;
    [key: string]: any;
  }) {
    return this.client.get('/opportunities', { params });
  }

  getOpportunity(id: number) {
    return this.client.get(`/opportunities/${id}`);
  }

  createOpportunity(data: any) {
    return this.client.post('/opportunities', data);
  }

  updateOpportunity(id: number, data: any) {
    return this.client.put(`/opportunities/${id}`, data);
  }

  deleteOpportunity(id: number) {
    return this.client.delete(`/opportunities/${id}`);
  }

  searchOpportunities(term?: string, page: number = 0, size: number = 20) {
    return this.client.get('/opportunities/search', { params: { term, page, size } });
  }

  // Activities endpoints
  getActivities(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
    search?: string;
    type?: string;
    [key: string]: any;
  }) {
    return this.client.get('/activities', { params });
  }

  getActivity(id: number) {
    return this.client.get(`/activities/${id}`);
  }

  createActivity(data: any) {
    return this.client.post('/activities', data);
  }

  updateActivity(id: number, data: any) {
    return this.client.put(`/activities/${id}`, data);
  }

  deleteActivity(id: number) {
    return this.client.delete(`/activities/${id}`);
  }

  searchActivities(term?: string, page: number = 0, size: number = 20) {
    return this.client.get('/activities/search', { params: { term, page, size } });
  }

  // Custom fields endpoints
  getCustomFieldDefinitions(entityType: string) {
    return this.client.get('/custom-fields', { params: { entityType } });
  }

  createCustomFieldDefinition(data: any) {
    return this.client.post('/custom-fields', data);
  }

  updateCustomFieldDefinition(id: number, data: any) {
    return this.client.put(`/custom-fields/${id}`, data);
  }

  deleteCustomFieldDefinition(id: number) {
    return this.client.delete(`/custom-fields/${id}`);
  }

  reorderCustomFieldDefinitions(orderedIds: number[]) {
    return this.client.post('/custom-fields/reorder', orderedIds);
  }

  // Calculated fields endpoints
  getCalculatedFieldDefinitions(entityType: string) {
    return this.client.get('/calculated-fields/definitions', { params: { entityType } });
  }

  createCalculatedFieldDefinition(data: any) {
    return this.client.post('/calculated-fields/definitions', data);
  }

  updateCalculatedFieldDefinition(id: number, data: any) {
    return this.client.put(`/calculated-fields/definitions/${id}`, data);
  }

  deleteCalculatedFieldDefinition(id: number) {
    return this.client.delete(`/calculated-fields/definitions/${id}`);
  }

  reorderCalculatedFieldDefinitions(orderedIds: number[]) {
    return this.client.post('/calculated-fields/definitions/reorder', orderedIds);
  }

  evaluateCalculatedFields(entityType: string, entityId: number) {
    return this.client.get('/calculated-fields/evaluate', { params: { entityType, entityId } });
  }

  // Policy Rules endpoints
  getPolicyRuleDefinitions(entityType: string) {
    return this.client.get('/policy-rules/definitions', { params: { entityType } });
  }

  createPolicyRule(data: any) {
    return this.client.post('/policy-rules/definitions', data);
  }

  updatePolicyRule(id: number, data: any) {
    return this.client.put(`/policy-rules/definitions/${id}`, data);
  }

  deletePolicyRule(id: number) {
    return this.client.delete(`/policy-rules/definitions/${id}`);
  }

  reorderPolicyRules(orderedIds: number[]) {
    return this.client.post('/policy-rules/definitions/reorder', orderedIds);
  }

  evaluatePolicyRules(data: { entityType: string; operation: string; entityData: any; previousData?: any }) {
    return this.client.post('/policy-rules/evaluate', data);
  }

  // Entity field options endpoints
  getEntityFieldOptions(entityType: string) {
    return this.client.get(`/entity-field-options/${entityType}`);
  }

  updateEntityFieldOptions(entityType: string, fieldName: string, options: { value: string; label: string }[]) {
    return this.client.put(`/entity-field-options/${entityType}/${fieldName}`, options);
  }

  // Timeline endpoints
  getTimeline(entityType: string, entityId: number) {
    return this.client.get(`/timeline/${entityType}/${entityId}`);
  }

  // Chat endpoints
  sendChatMessage(
    message: string,
    contextEntityType?: string,
    contextEntityId?: number,
    sessionId?: string,
    confirmMode?: 'confirm' | 'auto',
    executeActions?: boolean,
    pageContext?: string,
    attachmentBase64?: string,
    attachmentMimeType?: string,
    attachmentFileName?: string
  ) {
    return this.client.post('/chat/message', {
      message,
      contextEntityType,
      contextEntityId,
      sessionId,
      confirmMode,
      executeActions,
      pageContext,
      attachmentBase64,
      attachmentMimeType,
      attachmentFileName,
    }, { timeout: 600000 });
  }

  executePendingActions(sessionId: string) {
    return this.client.post('/chat/message', {
      sessionId,
      executeActions: true,
    }, { timeout: 600000 });
  }

  getChatHistory(contextEntityType?: string, contextEntityId?: number) {
    return this.client.get('/chat/history', {
      params: {
        contextEntityType,
        contextEntityId,
      },
    });
  }

  clearChatHistory() {
    return this.client.delete('/chat/history');
  }

  // Assistant subscription / quota endpoints
  getAssistantSubscription() {
    return this.client.get('/assistant/subscription');
  }

  getAssistantQuota() {
    return this.client.get('/assistant/quota');
  }

  getAssistantTiers() {
    return this.client.get('/assistant/tiers');
  }

  upgradeAssistantTier(tierName: string) {
    return this.client.post('/assistant/subscription/upgrade', { tierName });
  }

  requestAssistantTierChange(tierName: string) {
    return this.client.post('/assistant/tier-change-request', { tierName });
  }

  getDashboardInsight() {
    return this.client.get<{ text: string; generatedAt: string }>('/assistant/insight');
  }

  refreshDashboardInsight() {
    return this.client.post<{ text: string; generatedAt: string }>('/assistant/insight/refresh', {});
  }

  updateAssistantTier(tierId: number, data: { provider: string; modelId: string }) {
    return this.client.put(`/assistant/tiers/${tierId}`, data);
  }

  getOnboardingSuggestions(data: { orgName: string; orgBio: string }) {
    return this.client.post('/assistant/onboarding-suggestions', data);
  }

  getSystemTenantSubscription(tenantId: number) {
    return this.client.get(`/admin/system/tenants/${tenantId}/subscription`);
  }

  changeSystemTenantTier(tenantId: number, tierName: string) {
    return this.client.put(`/admin/system/tenants/${tenantId}/tier`, { tierName });
  }

  getAiTiers() {
    return this.client.get('/admin/system/ai-tiers');
  }

  updateAiTier(tierId: number, data: any) {
    return this.client.put(`/admin/system/ai-tiers/${tierId}`, data);
  }

  applyAiTiersToAllTenants(tierName: string) {
    return this.client.post('/admin/system/ai-tiers/apply-all-tenants', { tierName });
  }

  getAiModels() {
    return this.client.get('/admin/system/ai-models');
  }

  createAiModel(data: { provider: string; modelId: string; enabled?: boolean }) {
    return this.client.post('/admin/system/ai-models', data);
  }

  updateAiModel(modelId: number, data: { provider?: string; modelId?: string; enabled?: boolean }) {
    return this.client.put(`/admin/system/ai-models/${modelId}`, data);
  }

  deleteAiModel(modelId: number) {
    return this.client.delete(`/admin/system/ai-models/${modelId}`);
  }

  getAvailableModelsFromProvider(provider: string) {
    return this.client.get<{ modelId: string; hasQuota: boolean | null }[]>(`/admin/system/ai-models/providers/${provider}/available`);
  }

  // Bulk operations endpoints
  executeBulkOperation(data: any) {
    return this.client.post('/bulk/execute', data);
  }

  bulkDeleteCustomers(ids: number[]) {
    return this.client.post('/bulk/customers/delete', { ids });
  }

  bulkDeleteContacts(ids: number[]) {
    return this.client.post('/bulk/contacts/delete', { ids });
  }

  bulkDeleteOpportunities(ids: number[]) {
    return this.client.post('/bulk/opportunities/delete', { ids });
  }

  bulkDeleteActivities(ids: number[]) {
    return this.client.post('/bulk/activities/delete', { ids });
  }

  bulkDeleteCustomRecords(ids: number[]) {
    return this.client.post('/bulk/custom-records/delete', { ids });
  }

  bulkDeleteDocuments(ids: number[]) {
    return this.client.post('/bulk/documents/delete', { ids });
  }

  // Reporting endpoints
  getDashboardSummary() {
    return this.client.get('/reports/summary');
  }

  getCustomersReport(filter?: any) {
    return this.client.post('/reports/customers', filter || {});
  }

  getOpportunitiesReport(filter?: any) {
    return this.client.post('/reports/opportunities', filter || {});
  }

  // Opportunity Types endpoints
  getOpportunityTypes() {
    return this.client.get('/opportunity-types');
  }

  createOpportunityType(data: { name: string; description?: string }) {
    return this.client.post('/opportunity-types', data);
  }

  updateOpportunityType(id: number, data: { name: string; description?: string; displayOrder?: number }) {
    return this.client.put(`/opportunity-types/${id}`, data);
  }

  deleteOpportunityType(id: number) {
    return this.client.delete(`/opportunity-types/${id}`);
  }

  // Admin tenants endpoints
  getTenants(params?: { page?: number; size?: number }) {
    return this.client.get('/admin/tenants', { params });
  }

  getTenant(id: number) {
    return this.client.get(`/admin/tenants/${id}`);
  }

  getCurrentTenant() {
    return this.client.get('/admin/tenants/current');
  }

  setLogoFromUrl(imageUrl: string) {
    return this.client.post('/admin/tenants/current/logo', { imageUrl });
  }

  uploadLogo(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.client.post<{ logoUrl: string }>('/admin/tenants/current/logo/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  }

  createTenant(data: { name: string }) {
    return this.client.post('/admin/tenants', data);
  }

  updateTenant(id: number, data: { name?: string; settings?: any }) {
    return this.client.put(`/admin/tenants/${id}`, data);
  }

  deleteTenant(id: number) {
    return this.client.delete(`/admin/tenants/${id}`);
  }

  getTenantSettings(id: number) {
    return this.client.get(`/admin/tenants/${id}/settings`);
  }

  updateTenantSettings(id: number, settings: any) {
    return this.client.put(`/admin/tenants/${id}/settings`, settings);
  }

  // Tenant Membership endpoints
  getTenantUsers(tenantId: number) {
    return this.client.get(`/admin/tenants/${tenantId}/users`);
  }

  addUserToTenant(tenantId: number, userId: number, role: string) {
    return this.client.post(`/admin/tenants/${tenantId}/users`, { userId, role });
  }

  removeUserFromTenant(tenantId: number, userId: number) {
    return this.client.delete(`/admin/tenants/${tenantId}/users/${userId}`);
  }

  // Document endpoints
  listDocuments(params?: { page?: number; size?: number; search?: string; contentType?: string; source?: string; linkedEntityType?: string; linkedEntityId?: number; linkedFieldKey?: string }) {
    return this.client.get('/documents', { params });
  }

  getDocument(id: number) {
    return this.client.get(`/documents/${id}`);
  }

  downloadDocument(id: number) {
    return this.client.get(`/documents/${id}/download`);
  }

  createDocument(data: { name: string; description?: string; mimeType?: string; contentBase64?: string; sizeBytes?: number; contentType?: string; tags?: string[]; linkedEntityType?: string; linkedEntityId?: number }) {
    return this.client.post('/documents', data);
  }

  uploadDocument(file: File, meta: { name?: string; description?: string; linkedEntityType?: string; linkedEntityId?: number; linkedFieldKey?: string; tags?: string[] }) {
    const form = new FormData();
    form.append('file', file);
    if (meta.name) form.append('name', meta.name);
    if (meta.description) form.append('description', meta.description);
    if (meta.linkedEntityType) form.append('linkedEntityType', meta.linkedEntityType);
    if (meta.linkedEntityId != null) form.append('linkedEntityId', String(meta.linkedEntityId));
    if (meta.linkedFieldKey) form.append('linkedFieldKey', meta.linkedFieldKey);
    if (meta.tags) meta.tags.forEach(t => form.append('tags', t));
    return this.client.post('/documents/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } });
  }

  renameDocument(id: number, name: string) {
    return this.client.patch(`/documents/${id}/rename`, { name });
  }

  duplicateDocument(id: number) {
    return this.client.post(`/documents/${id}/duplicate`);
  }

  deleteDocument(id: number) {
    return this.client.delete(`/documents/${id}`);
  }

  // CustomRecord endpoints
  listCustomRecords(params?: { page?: number; size?: number; search?: string; status?: string; customerId?: number; sortBy?: string; sortOrder?: string }) {
    return this.client.get('/custom-records', { params });
  }

  getCustomRecord(id: number) {
    return this.client.get(`/custom-records/${id}`);
  }

  createCustomRecord(data: { name: string; type?: string; serialNumber?: string; status?: string; customerId?: number; notes?: string; customFields?: any; opportunityIds?: number[] }) {
    return this.client.post('/custom-records', data);
  }

  updateCustomRecord(id: number, data: { name?: string; type?: string; serialNumber?: string; status?: string; customerId?: number; notes?: string; customFields?: any }) {
    return this.client.put(`/custom-records/${id}`, data);
  }

  deleteCustomRecord(id: number) {
    return this.client.delete(`/custom-records/${id}`);
  }

  searchCustomRecords(term?: string, page: number = 0, size: number = 20) {
    return this.client.get('/custom-records/search', { params: { term, page, size } });
  }

  linkCustomRecordToOpportunity(customRecordId: number, opportunityId: number) {
    return this.client.post(`/custom-records/${customRecordId}/opportunities/${opportunityId}`);
  }

  unlinkCustomRecordFromOpportunity(customRecordId: number, opportunityId: number) {
    return this.client.delete(`/custom-records/${customRecordId}/opportunities/${opportunityId}`);
  }

  listCustomRecordsByOpportunity(opportunityId: number) {
    return this.client.get(`/opportunities/${opportunityId}/customRecords`);
  }

  listOpportunityContacts(opportunityId: number) {
    return this.client.get(`/opportunities/${opportunityId}/contacts`);
  }

  addContactToOpportunity(opportunityId: number, contactId: number) {
    return this.client.post(`/opportunities/${opportunityId}/contacts/${contactId}`);
  }

  removeContactFromOpportunity(opportunityId: number, contactId: number) {
    return this.client.delete(`/opportunities/${opportunityId}/contacts/${contactId}`);
  }

  getTenantMembers() {
    return this.client.get('/user/team');
  }

  // User Groups
  getGroups() {
    return this.client.get('/groups');
  }

  createGroup(data: { name: string; description?: string }) {
    return this.client.post('/groups', data);
  }

  updateGroup(id: number, data: { name?: string; description?: string }) {
    return this.client.put(`/groups/${id}`, data);
  }

  deleteGroup(id: number) {
    return this.client.delete(`/groups/${id}`);
  }

  getGroupMembers(groupId: number) {
    return this.client.get(`/groups/${groupId}/members`);
  }

  addGroupMember(groupId: number, userId: number) {
    return this.client.post(`/groups/${groupId}/members`, { userId });
  }

  removeGroupMember(groupId: number, userId: number) {
    return this.client.delete(`/groups/${groupId}/members/${userId}`);
  }

  // Access Control
  getAccessSummary(entityType: string, entityId: number, ownerId?: number) {
    return this.client.get(`/access/${entityType}/${entityId}/summary`, {
      params: ownerId != null ? { ownerId } : undefined,
    });
  }

  setAccessPolicy(entityType: string, entityId: number, accessMode: string) {
    return this.client.put(`/access/${entityType}/${entityId}/policy`, { accessMode });
  }

  removeAccessPolicy(entityType: string, entityId: number) {
    return this.client.delete(`/access/${entityType}/${entityId}/policy`);
  }

  addAccessGrant(entityType: string, entityId: number, data: { granteeType: string; granteeId: number; permission: string }) {
    return this.client.post(`/access/${entityType}/${entityId}/grants`, data);
  }

  removeAccessGrant(entityType: string, entityId: number, grantId: number) {
    return this.client.delete(`/access/${entityType}/${entityId}/grants/${grantId}`);
  }

  // Document Templates endpoints
  listDocumentTemplates(params?: { page?: number; size?: number; search?: string; templateType?: string }) {
    return this.client.get('/document-templates', { params });
  }

  getDocumentTemplate(id: number) {
    return this.client.get(`/document-templates/${id}`);
  }

  createDocumentTemplate(data: { name: string; description?: string; templateType: string; styleJson?: string; isDefault?: boolean }) {
    return this.client.post('/document-templates', data);
  }

  updateDocumentTemplate(id: number, data: { name?: string; description?: string; templateType?: string; styleJson?: string; isDefault?: boolean }) {
    return this.client.put(`/document-templates/${id}`, data);
  }

  deleteDocumentTemplate(id: number) {
    return this.client.delete(`/document-templates/${id}`);
  }

  cloneDocumentTemplate(id: number, name?: string) {
    return this.client.post(`/document-templates/${id}/clone`, {}, { params: { ...(name && { name }) } });
  }

  previewReport(data: {
    entityType: string;
    entityId?: number | null;
    reportType: 'slide_deck' | 'one_pager';
    styleJson?: string;
    templateId?: number | null;
    title?: string;
  }) {
    return this.client.post<{ content: string; mimeType: string; reportType: string }>(
      '/report-builder/preview', data, { timeout: 60000 }
    );
  }

  // Notifications endpoints
  getNotificationInbox(params?: { page?: number; size?: number }) {
    return this.client.get('/notifications/inbox', { params });
  }

  getUnreadNotificationCount() {
    return this.client.get('/notifications/inbox/unread-count');
  }

  markNotificationRead(id: number) {
    return this.client.post(`/notifications/inbox/${id}/read`);
  }

  markAllNotificationsRead() {
    return this.client.post('/notifications/inbox/read-all');
  }

  getNotificationPreferences() {
    return this.client.get('/notifications/preferences');
  }

  updateNotificationPreferences(preferences: any) {
    return this.client.put('/notifications/preferences', preferences);
  }

  composeNotification(request: {
    entityType: string;
    entityId: number;
    entityName: string;
    subject: string;
    body: string;
    recipientUserIds: number[];
  }) {
    return this.client.post('/notifications/compose', request);
  }

  // Notification Templates endpoints
  listNotificationTemplates(params?: { page?: number; size?: number; search?: string; notificationType?: string }) {
    return this.client.get('/notification-templates', { params });
  }

  getNotificationTemplate(id: number) {
    return this.client.get(`/notification-templates/${id}`);
  }

  createNotificationTemplate(request: any) {
    return this.client.post('/notification-templates', request);
  }

  updateNotificationTemplate(id: number, request: any) {
    return this.client.put(`/notification-templates/${id}`, request);
  }

  deleteNotificationTemplate(id: number) {
    return this.client.delete(`/notification-templates/${id}`);
  }

  getNotificationTemplateTypes() {
    return this.client.get('/notification-templates/types');
  }

  // Tenant Backup endpoints
  createBackup(includesData: boolean) {
    return this.client.post('/admin/backup/create-backup', { includesData });
  }

  createRestore(payload: string) {
    return this.client.post('/admin/backup/create-restore', { payload });
  }

  listBackupJobs() {
    return this.client.get('/admin/backup/jobs');
  }

  getBackupJob(id: number) {
    return this.client.get(`/admin/backup/jobs/${id}`);
  }

  downloadBackup(id: number) {
    return this.client.get(`/admin/backup/jobs/${id}/download`, { responseType: 'text' as const });
  }

  deleteBackupJob(id: number) {
    return this.client.delete(`/admin/backup/jobs/${id}`);
  }

  // System admin endpoints
  getSystemStats() {
    return this.client.get('/admin/system/stats');
  }

  getSystemUsers(params?: { page?: number; size?: number; search?: string }) {
    return this.client.get('/admin/system/users', { params });
  }

  getUserMemberships(userId: number) {
    return this.client.get(`/admin/system/users/${userId}/memberships`);
  }

  updateUserStatus(userId: number, status: string) {
    return this.client.put(`/admin/system/users/${userId}/status`, { status });
  }

  inviteUser(email: string, displayName?: string) {
    return this.client.post(`/admin/system/users/invite`, { email, displayName });
  }

  // MCP API Key endpoints
  generateMcpApiKey(name: string) {
    return this.client.post('/assistant/mcp-keys/generate', { name });
  }

  listMcpApiKeys() {
    return this.client.get('/assistant/mcp-keys');
  }

  revokeMcpApiKey(keyId: number) {
    return this.client.post(`/assistant/mcp-keys/${keyId}/revoke`);
  }

  // Orders endpoints
  getOrders(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
    [key: string]: any;
  }) {
    return this.client.get('/orders', { params });
  }

  getOrder(id: number) {
    return this.client.get(`/orders/${id}`);
  }

  createOrder(data: any) {
    return this.client.post('/orders', data);
  }

  updateOrder(id: number, data: any) {
    return this.client.put(`/orders/${id}`, data);
  }

  deleteOrder(id: number) {
    return this.client.delete(`/orders/${id}`);
  }

  bulkDeleteOrders(ids: number[]) {
    return this.client.post('/bulk/orders/delete', { ids });
  }

  searchOrders(params?: {
    term?: string;
    page?: number;
    size?: number;
  }) {
    return this.client.get('/orders/search', { params });
  }

  // Invoices endpoints
  getInvoices(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
    [key: string]: any;
  }) {
    return this.client.get('/invoices', { params });
  }

  getInvoice(id: number) {
    return this.client.get(`/invoices/${id}`);
  }

  createInvoice(data: any) {
    return this.client.post('/invoices', data);
  }

  updateInvoice(id: number, data: any) {
    return this.client.put(`/invoices/${id}`, data);
  }

  deleteInvoice(id: number) {
    return this.client.delete(`/invoices/${id}`);
  }

  bulkDeleteInvoices(ids: number[]) {
    return this.client.post('/bulk/invoices/delete', { ids });
  }

  searchInvoices(params?: {
    term?: string;
    page?: number;
    size?: number;
  }) {
    return this.client.get('/invoices/search', { params });
  }

  // Integration endpoints
  getIntegrations() {
    return this.client.get('/integrations');
  }

  getIntegration(id: number) {
    return this.client.get(`/integrations/${id}`);
  }

  createIntegration(data: any) {
    return this.client.post('/integrations', data);
  }

  updateIntegration(id: number, data: any) {
    return this.client.put(`/integrations/${id}`, data);
  }

  deleteIntegration(id: number) {
    return this.client.delete(`/integrations/${id}`);
  }

  testIntegration(id: number) {
    return this.client.post(`/integrations/${id}/test`);
  }

  getFailedIntegrationEvents() {
    return this.client.get('/integrations/failed-events');
  }

  getFinancialReport(params?: { startDate?: string; endDate?: string }) {
    return this.client.get('/reports/financial', { params });
  }
}

export const apiClient = new ApiClient();
