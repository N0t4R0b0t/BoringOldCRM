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
import { useEffect, useState } from 'react';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { useOpportunityTypesStore } from '../store/opportunityTypesStore';
import { usePageTitle } from '../hooks/usePageTitle';

export const ReportingDashboardPage = () => {
  usePageTitle('Reports');
  const { types: opportunityTypes } = useOpportunityTypesStore();
  const [summary, setSummary] = useState<any>(null);
  const [opportunitiesReport, setOpportunitiesReport] = useState<any>(null);
  const [customersReport, setCustomersReport] = useState<any>(null);
  const [financialReport, setFinancialReport] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [dateRange, setDateRange] = useState({
    startDate: '',
    endDate: '',
  });
  const [selectedTypeSlug, setSelectedTypeSlug] = useState('');

  useEffect(() => {
    loadReports();
  }, []);

  const loadReports = async () => {
    try {
      setLoading(true);
      setError('');

      const oppFilter: any = {};
      if (dateRange.startDate || dateRange.endDate) {
        oppFilter.startDate = dateRange.startDate;
        oppFilter.endDate = dateRange.endDate;
      }
      if (selectedTypeSlug) {
        oppFilter.typeSlug = selectedTypeSlug;
      }

      const dateParams = dateRange.startDate || dateRange.endDate ? dateRange : undefined;
      const [summaryRes, oppRes, custRes, finRes] = await Promise.all([
        apiClient.getDashboardSummary(),
        apiClient.getOpportunitiesReport(Object.keys(oppFilter).length > 0 ? oppFilter : undefined),
        apiClient.getCustomersReport(dateParams),
        apiClient.getFinancialReport(dateParams),
      ]);

      setSummary(summaryRes.data);
      setOpportunitiesReport(oppRes.data);
      setCustomersReport(custRes.data);
      setFinancialReport(finRes.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load reports');
    } finally {
      setLoading(false);
    }
  };

  const handleDateRangeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setDateRange((prev) => ({ ...prev, [name]: value }));
  };

  const handleApplyDateRange = () => {
    loadReports();
  };

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-full">
          <div className="text-gray-500 dark:text-gray-400">Loading reports...</div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-8">
        {error && (
          <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        {/* Date Range Filter */}
        <div className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 flex flex-wrap items-end gap-4">
          <div className="space-y-1">
            <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 dark:text-gray-300">Start Date</label>
            <input
              type="date"
              id="startDate"
              name="startDate"
              value={dateRange.startDate}
              onChange={handleDateRangeChange}
              className="block w-full rounded-md border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-3 py-2 border"
            />
          </div>
          <div className="space-y-1">
            <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 dark:text-gray-300">End Date</label>
            <input
              type="date"
              id="endDate"
              name="endDate"
              value={dateRange.endDate}
              onChange={handleDateRangeChange}
              className="block w-full rounded-md border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-3 py-2 border"
            />
          </div>
          {opportunityTypes.length > 0 && (
            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Opportunity Type</label>
              <select
                value={selectedTypeSlug}
                onChange={e => setSelectedTypeSlug(e.target.value)}
                className="block w-full rounded-md border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-3 py-2 border"
              >
                <option value="">All Types</option>
                {opportunityTypes.map(t => (
                  <option key={t.slug} value={t.slug}>{t.name}</option>
                ))}
              </select>
            </div>
          )}
          <button
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium transition-colors shadow-sm"
            onClick={handleApplyDateRange}
          >
            Apply Filters
          </button>
        </div>

        {/* Key Metrics Summary */}
        {summary && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
              <div className="text-sm font-medium text-gray-500 dark:text-gray-400">Total Customers</div>
              <div className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{summary.totalCustomers}</div>
              {summary.keyMetrics && (
                <div className="mt-1 text-sm text-green-600 dark:text-green-400">
                  Active: {summary.keyMetrics.find((m: any) => m.metric === 'Active Customers')?.value.toString()}
                </div>
              )}
            </div>

            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
              <div className="text-sm font-medium text-gray-500 dark:text-gray-400">Open Opportunities</div>
              <div className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{summary.totalOpportunities}</div>
              <div className="mt-1 text-sm text-blue-600 dark:text-blue-400">
                Pipeline: ${summary.totalPipelineValue.toFixed(0)}
              </div>
            </div>

            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
              <div className="text-sm font-medium text-gray-500 dark:text-gray-400">Win Rate</div>
              <div className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{summary.opportunityWinRate.toFixed(1)}%</div>
              <div className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {summary.recentActivities} recent activities
              </div>
            </div>

            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
              <div className="text-sm font-medium text-gray-500 dark:text-gray-400">Total Contacts</div>
              <div className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{summary.totalContacts}</div>
              <div className="mt-1 text-sm text-gray-500 dark:text-gray-400">Across all customers</div>
            </div>

            {summary.totalOrders !== undefined && (
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <div className="text-sm font-medium text-gray-500 dark:text-gray-400">Orders</div>
                <div className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{summary.totalOrders}</div>
                <div className="mt-1 text-sm text-indigo-600 dark:text-indigo-400">
                  Revenue: ${(summary.totalOrderRevenue ?? 0).toFixed(0)}
                </div>
              </div>
            )}

            {summary.totalInvoices !== undefined && (
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <div className="text-sm font-medium text-gray-500 dark:text-gray-400">Invoices</div>
                <div className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{summary.totalInvoices}</div>
                <div className="mt-1 text-sm text-green-600 dark:text-green-400">
                  Billed: ${(summary.totalInvoiceRevenue ?? 0).toFixed(0)}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Opportunities Report */}
        {opportunitiesReport && (
          <div className="space-y-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <span>📈</span> Opportunities Report
            </h2>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-4">Pipeline by Stage</h3>
                <div className="space-y-4">
                  {opportunitiesReport.byStage?.map((stage: any) => (
                    <div key={stage.stage} className="space-y-1">
                      <div className="flex justify-between text-sm">
                        <span className="font-medium text-gray-700 dark:text-gray-300">{stage.stage} ({stage.count})</span>
                        <span className="text-gray-900 dark:text-gray-100">${stage.value.toFixed(0)}</span>
                      </div>
                      <div className="w-full bg-gray-100 dark:bg-gray-700 rounded-full h-2.5 overflow-hidden">
                        <div
                          className="bg-blue-600 h-2.5 rounded-full"
                          style={{
                            width: `${
                              opportunitiesReport.totalPipelineValue > 0
                                ? (stage.value / opportunitiesReport.totalPipelineValue) * 100
                                : 0
                            }%`,
                          }}
                        ></div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-4">Key Metrics</h3>
                <div className="space-y-3">
                  {opportunitiesReport.metrics?.map((metric: any) => (
                    <div key={metric.metric} className="flex justify-between items-center py-2 border-b border-gray-100 dark:border-gray-700 last:border-0">
                      <span className="text-sm text-gray-600 dark:text-gray-400">{metric.metric}</span>
                      <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                        {typeof metric.value === 'number'
                          ? metric.value.toFixed(2)
                          : metric.value}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {opportunitiesReport.monthlyTrend?.length > 0 && (
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-6">Monthly Trend</h3>
                <div className="h-64 flex items-end gap-4">
                  {opportunitiesReport.monthlyTrend.map((month: any) => {
                    const maxCount = Math.max(...opportunitiesReport.monthlyTrend.map((m: any) => m.count));
                    const heightPercentage = maxCount > 0 ? (month.count / maxCount) * 100 : 0;

                    return (
                      <div key={month.month} className="flex-1 flex flex-col items-center gap-2 group">
                        <div className="w-full bg-blue-100 dark:bg-blue-900/30 rounded-t-md relative flex-1 flex items-end group-hover:bg-blue-200 dark:group-hover:bg-blue-900/50 transition-colors">
                          <div
                            className="w-full bg-blue-500 rounded-t-md transition-all duration-500"
                            style={{ height: `${heightPercentage}%` }}
                          ></div>
                          <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-800 dark:bg-gray-600 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                            {month.count} opps
                          </div>
                        </div>
                        <span className="text-xs text-gray-500 dark:text-gray-400 font-medium">{month.month}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {opportunitiesReport.byType?.length > 0 && (
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-4">Pipeline by Type</h3>
                <div className="space-y-4">
                  {opportunitiesReport.byType.map((t: any) => (
                    <div key={t.typeSlug} className="space-y-1">
                      <div className="flex justify-between text-sm">
                        <span className="font-medium text-gray-700 dark:text-gray-300">
                          {t.typeName} ({t.count})
                        </span>
                        <span className="text-gray-500 dark:text-gray-400 text-xs">
                          Win rate: {t.winRate.toFixed(1)}%
                        </span>
                        <span className="text-gray-900 dark:text-gray-100">${t.value.toFixed(0)}</span>
                      </div>
                      <div className="w-full bg-gray-100 dark:bg-gray-700 rounded-full h-2.5 overflow-hidden">
                        <div
                          className="bg-indigo-500 h-2.5 rounded-full"
                          style={{
                            width: `${
                              opportunitiesReport.totalPipelineValue > 0
                                ? (t.value / opportunitiesReport.totalPipelineValue) * 100
                                : 0
                            }%`,
                          }}
                        ></div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Customers Report */}
        {customersReport && (
          <div className="space-y-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <span>👥</span> Customers Report
            </h2>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-4">Status Breakdown</h3>
                <div className="space-y-4">
                  {customersReport.byStatus?.map((status: any) => (
                    <div key={status.status} className="space-y-1">
                      <div className="flex justify-between text-sm">
                        <span className="font-medium text-gray-700 dark:text-gray-300 capitalize">{status.status}</span>
                        <span className="text-gray-900 dark:text-gray-100">{status.count}</span>
                      </div>
                      <div className="w-full bg-gray-100 dark:bg-gray-700 rounded-full h-2.5 overflow-hidden">
                        <div
                          className={`h-2.5 rounded-full ${
                            status.status === 'active' ? 'bg-green-500' :
                            status.status === 'inactive' ? 'bg-gray-400' :
                            'bg-blue-500'
                          }`}
                          style={{
                            width: `${
                              customersReport.totalCustomers > 0
                                ? (status.count / customersReport.totalCustomers) * 100
                                : 0
                            }%`,
                          }}
                        ></div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-4">Statistics</h3>
                <div className="space-y-3">
                  <div className="flex justify-between items-center py-2 border-b border-gray-100 dark:border-gray-700">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Total Customers</span>
                    <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{customersReport.totalCustomers}</span>
                  </div>
                  <div className="flex justify-between items-center py-2 border-b border-gray-100 dark:border-gray-700">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Active</span>
                    <span className="text-sm font-semibold text-green-600 dark:text-green-400">{customersReport.activeCustomers}</span>
                  </div>
                  <div className="flex justify-between items-center py-2 border-b border-gray-100 dark:border-gray-700">
                    <span className="text-sm text-gray-600 dark:text-gray-400">Inactive</span>
                    <span className="text-sm font-semibold text-gray-500 dark:text-gray-400">{customersReport.inactiveCustomers}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
        {/* Financial Report */}
        {financialReport && (
          <div className="space-y-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <span>💰</span> Financial Report
            </h2>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Orders */}
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-4">Orders by Status</h3>
                <div className="space-y-3">
                  {Object.entries(financialReport.ordersByStatus ?? {}).map(([status, count]: [string, any]) => (
                    <div key={status} className="flex justify-between items-center py-1.5 border-b border-gray-100 dark:border-gray-700 last:border-0">
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300 capitalize">{status}</span>
                      <span className="text-sm text-gray-900 dark:text-gray-100">{count}</span>
                    </div>
                  ))}
                  <div className="flex justify-between items-center pt-2 font-semibold">
                    <span className="text-sm text-gray-700 dark:text-gray-300">Total Revenue</span>
                    <span className="text-sm text-indigo-600 dark:text-indigo-400">${(financialReport.totalOrderRevenue ?? 0).toFixed(2)}</span>
                  </div>
                </div>
              </div>

              {/* Invoices */}
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-4">Invoices by Status</h3>
                <div className="space-y-3">
                  {Object.entries(financialReport.invoicesByStatus ?? {}).map(([status, count]: [string, any]) => (
                    <div key={status} className="flex justify-between items-center py-1.5 border-b border-gray-100 dark:border-gray-700 last:border-0">
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300 capitalize">{status}</span>
                      <span className="text-sm text-gray-900 dark:text-gray-100">{count}</span>
                    </div>
                  ))}
                  <div className="flex justify-between items-center pt-2 font-semibold">
                    <span className="text-sm text-gray-700 dark:text-gray-300">Paid Revenue</span>
                    <span className="text-sm text-green-600 dark:text-green-400">${(financialReport.paidInvoiceRevenue ?? 0).toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between items-center font-semibold">
                    <span className="text-sm text-gray-700 dark:text-gray-300">Total Billed</span>
                    <span className="text-sm text-gray-900 dark:text-gray-100">${(financialReport.totalInvoiceRevenue ?? 0).toFixed(2)}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Monthly revenue trends */}
            {Object.keys(financialReport.orderRevenueByMonth ?? {}).length > 0 && (
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                <h3 className="text-base font-medium text-gray-900 dark:text-gray-100 mb-6">Monthly Order Revenue</h3>
                <div className="h-48 flex items-end gap-3">
                  {Object.entries(financialReport.orderRevenueByMonth)
                    .sort(([a], [b]) => a.localeCompare(b))
                    .map(([month, revenue]: [string, any]) => {
                      const max = Math.max(...Object.values(financialReport.orderRevenueByMonth) as number[]);
                      const pct = max > 0 ? (revenue / max) * 100 : 0;
                      return (
                        <div key={month} className="flex-1 flex flex-col items-center gap-2 group">
                          <div className="w-full bg-indigo-100 dark:bg-indigo-900/30 rounded-t-md flex-1 flex items-end">
                            <div className="w-full bg-indigo-500 rounded-t-md transition-all duration-500" style={{ height: `${pct}%` }} />
                          </div>
                          <span className="text-xs text-gray-500 dark:text-gray-400">{month}</span>
                        </div>
                      );
                    })}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Layout>
  );
};
