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
import { useEffect, useState, useRef } from 'react';
import { Layout } from '../components/Layout';
import { apiClient } from '../api/apiClient';
import { usePageTitle } from '../hooks/usePageTitle';

export const TenantBackupPage = () => {
  usePageTitle('Backup');
  const [jobs, setJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const pollingRef = useRef<number | null>(null);

  const loadJobs = async () => {
    setLoading(true);
    try {
      const res = await apiClient.listBackupJobs();
      setJobs(res.data || []);
      setError(null);
    } catch (err: any) {
      console.error('Failed to load jobs', err);
      setError('Failed to load backup jobs');
    } finally {
      setLoading(false);
    }
  };

  // Poll a single job until it completes or fails, then refresh
  const pollJobUntilDone = async (jobId: number) => {
    if (!jobId) return;
    if (pollingRef.current) window.clearInterval(pollingRef.current);
    pollingRef.current = window.setInterval(async () => {
      try {
        const res = await apiClient.getBackupJob(jobId);
        const job = res.data;
        if (!job) return;
        if (job.status === 'COMPLETED') {
          window.clearInterval(pollingRef.current as number);
          pollingRef.current = null;
          setSuccess('Backup ready');
          setTimeout(() => setSuccess(null), 3000);
          await loadJobs();
        } else if (job.status === 'FAILED') {
          window.clearInterval(pollingRef.current as number);
          pollingRef.current = null;
          setError('Backup failed');
          await loadJobs();
        }
      } catch (e) {
        // ignore transient errors
      }
    }, 2000);
  };

  useEffect(() => {
    loadJobs();
    return () => {
      if (pollingRef.current) window.clearInterval(pollingRef.current);
    };
  }, []);

  const handleCreateBackup = async (withData: boolean) => {
    setCreating(true);
    setError(null);
    try {
      const res = await apiClient.createBackup(withData);
      const job = res.data;
      if (job?.id) await pollJobUntilDone(job.id);
      await loadJobs();
    } catch (err) {
      console.error(err);
      setError('Failed to create backup');
    } finally {
      setCreating(false);
    }
  };

  const [restoreFile, setRestoreFile] = useState<File | null>(null);

  const handleUploadRestore = async () => {
    if (!restoreFile) return setError('No file selected');
    try {
      const text = await restoreFile.text();
      const res = await apiClient.createRestore(text);
      const job = res.data;
      if (job?.id) await pollJobUntilDone(job.id);
      await loadJobs();
      setRestoreFile(null);
    } catch (err) {
      console.error('Restore failed', err);
      setError('Restore failed');
    }
  };

  const handleDownload = async (id: number) => {
    try {
      const res = await apiClient.downloadBackup(id);
      const disposition = (res.headers as Record<string, string>)['content-disposition'] ?? '';
      const fnMatch = disposition.match(/filename="?([^"]+)"?/);
      const filename = fnMatch ? fnMatch[1] : `backup_${id}.json`;
      const blob = new Blob([res.data], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Download failed', err);
      setError('Download failed');
    }
  };

  return (
    <Layout>
      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">Tenant Backup & Restore</h2>

        <div className="max-w-5xl mx-auto px-4">
          <div className="flex gap-3 items-center">
            <button
              onClick={() => handleCreateBackup(false)}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium text-sm"
              disabled={creating}
            >
              Create Backup (settings only)
            </button>
            <button
              onClick={() => handleCreateBackup(true)}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 font-medium text-sm"
              disabled={creating}
            >
              Create Backup (include data)
            </button>
            <div className="ml-4 flex items-center gap-2">
              <input type="file" accept="application/json" onChange={e => setRestoreFile(e.target.files ? e.target.files[0] : null)} />
              <button onClick={handleUploadRestore} className="px-3 py-1 bg-yellow-600 text-white rounded-lg font-medium text-sm">Upload & Restore</button>
            </div>
          </div>
        </div>

        {error && (
          <div className="max-w-5xl mx-auto px-4">
            <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg mb-4">
              {error}
            </div>
          </div>
        )}
        {success && (
          <div className="max-w-5xl mx-auto px-4">
            <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 text-green-700 dark:text-green-300 px-4 py-3 rounded-lg mb-4">
              {success}
            </div>
          </div>
        )}

        <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm overflow-hidden max-w-5xl mx-auto">
          {loading ? (
            <div className="p-8 text-center text-gray-500 dark:text-gray-400">Loading...</div>
          ) : jobs.length === 0 ? (
            <div className="p-8 text-center text-gray-500 dark:text-gray-400">No backup jobs yet.</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 dark:bg-gray-700 text-gray-500 dark:text-gray-400 uppercase text-xs tracking-wider">
                <tr>
                  <th className="px-4 py-3 text-left">ID</th>
                  <th className="px-4 py-3 text-left">Type</th>
                  <th className="px-4 py-3 text-left">Label</th>
                  <th className="px-4 py-3 text-left">Created</th>
                  <th className="px-4 py-3 text-left">Status</th>
                  <th className="px-4 py-3 text-left">Progress</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {jobs.map((j) => (
                  <tr key={j.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                    <td className="px-4 py-3 font-medium text-gray-900 dark:text-gray-100">{j.id}</td>
                    <td className="px-4 py-3">{j.type}</td>
                    <td className="px-4 py-3 text-gray-500 dark:text-gray-400 max-w-xs truncate">{j.label}</td>
                    <td className="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">{j.createdAt ? new Date(j.createdAt).toLocaleString() : ''}</td>
                    <td className="px-4 py-3">{j.status}</td>
                    <td className="px-4 py-3">{j.progress}%</td>
                    <td className="px-4 py-3 text-right space-x-3">
                      <button
                        onClick={() => handleDownload(j.id)}
                        className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
                        disabled={j.status !== 'COMPLETED'}
                      >
                        Download
                      </button>
                      <button
                        onClick={async () => {
                          if (!confirm('Delete this job?')) return;
                          try {
                            await apiClient.deleteBackupJob(j.id);
                            await loadJobs();
                          } catch (err) {
                            console.error('Delete failed', err);
                            setError('Failed to delete job');
                          }
                        }}
                        className="text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 font-medium"
                        disabled={j.status === 'RUNNING'}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default TenantBackupPage;
