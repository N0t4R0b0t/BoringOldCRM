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
import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/apiClient';

export interface TimelineEvent {
  id: number;
  type: 'ACTIVITY' | 'AUDIT' | 'NOTE';
  action: string;
  title: string;
  description?: string;
  timestamp: string;
  actorId?: number;
  actorName?: string;
  details?: Record<string, any>;
}

interface TimelineProps {
  entityType: string;
  entityId: number;
}

export const Timeline: React.FC<TimelineProps> = ({ entityType, entityId }) => {
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchTimeline = async () => {
      try {
        setLoading(true);
        const response = await apiClient.getTimeline(entityType, entityId);
        setEvents(response.data);
        setError(null);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to load timeline');
        setEvents([]);
      } finally {
        setLoading(false);
      }
    };

    fetchTimeline();
  }, [entityType, entityId]);

  if (loading) {
    return <div className="timeline-loading">Loading timeline...</div>;
  }

  if (error) {
    return <div className="timeline-error">Error: {error}</div>;
  }

  if (events.length === 0) {
    return <div className="timeline-empty">No events yet</div>;
  }

  const getEventIcon = (type: string, action: string) => {
    if (type === 'AUDIT') {
      if (action.includes('CREATE')) return '✚';
      if (action.includes('UPDATE')) return '✎';
      if (action.includes('DELETE')) return '✕';
      return '◉';
    }
    return '○';
  };

  const getEventColor = (type: string, action: string) => {
    if (type === 'AUDIT') {
      if (action.includes('CREATE')) return 'event-create';
      if (action.includes('UPDATE')) return 'event-update';
      if (action.includes('DELETE')) return 'event-delete';
      return 'event-audit';
    }
    return 'event-activity';
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } else if (date.toDateString() === yesterday.toDateString()) {
      return 'Yesterday ' + date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } else {
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: date.getFullYear() !== today.getFullYear() ? 'numeric' : undefined });
    }
  };

  return (
    <div className="timeline-container">
      <h3>Activity Timeline</h3>
      <div className="timeline">
        {events.map((event, index) => (
          <div key={event.id} className={`timeline-item ${getEventColor(event.type, event.action)}`}>
            <div className="timeline-marker">
              <div className="timeline-icon">{getEventIcon(event.type, event.action)}</div>
              {index < events.length - 1 && <div className="timeline-line"></div>}
            </div>
            <div className="timeline-content">
              <div className="timeline-header">
                <h4 className="timeline-title">{event.title}</h4>
                <span className="timeline-badge">{event.type}</span>
              </div>
              {event.description && (
                <p className="timeline-description">{event.description}</p>
              )}
              <div className="timeline-meta">
                <span className="timeline-actor">{event.actorName || 'System'}</span>
                <span className="timeline-timestamp">{formatDate(event.timestamp)}</span>
              </div>
              {event.details && Object.keys(event.details).length > 0 && (
                <details className="timeline-details">
                  <summary>View details</summary>
                  <pre className="timeline-payload">{JSON.stringify(event.details, null, 2)}</pre>
                </details>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
