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
import { usePolicyRulesStore } from '../store/policyRulesStore';
import type { PolicyRule } from '../store/policyRulesStore';

interface PolicyInfoButtonProps {
  entityType: string;
}

export const PolicyInfoButton = ({ entityType }: PolicyInfoButtonProps) => {
  const { fetchPolicyRules, getRulesByEntityType } = usePolicyRulesStore();
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    fetchPolicyRules(entityType).then(() => setLoaded(true));
  }, [entityType]);

  if (!loaded) return null;

  const rules = getRulesByEntityType(entityType).filter((r: PolicyRule) => r.enabled);

  if (rules.length === 0) return null;

  const denyRules = rules.filter((r) => r.severity === 'DENY');
  const warnRules = rules.filter((r) => r.severity === 'WARN');

  return (
    <div className="policy-info-wrapper">
      <button type="button" className="policy-info-btn" aria-label="View active policies">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M8 1L2 4v4c0 3.31 2.56 6.41 6 7 3.44-.59 6-3.69 6-7V4L8 1z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" fill="none"/>
          <path d="M8 5v3M8 10h.01" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
        </svg>
        <span>{rules.length}</span>
      </button>
      <div className="policy-info-balloon">
        <div className="policy-info-balloon-title">Active Policies</div>
        {denyRules.length > 0 && (
          <div className="policy-info-section">
            <div className="policy-info-section-label policy-info-deny">Blocking</div>
            {denyRules.map((r) => (
              <div key={r.id} className="policy-info-rule">
                <span className="policy-info-rule-name">{r.name}</span>
                {r.description && <span className="policy-info-rule-desc">{r.description}</span>}
              </div>
            ))}
          </div>
        )}
        {warnRules.length > 0 && (
          <div className="policy-info-section">
            <div className="policy-info-section-label policy-info-warn">Warnings</div>
            {warnRules.map((r) => (
              <div key={r.id} className="policy-info-rule">
                <span className="policy-info-rule-name">{r.name}</span>
                {r.description && <span className="policy-info-rule-desc">{r.description}</span>}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
