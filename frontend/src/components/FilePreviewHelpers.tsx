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
export function CsvPreview({ content }: { content: string }) {
  const lines = content.trim().split('\n').filter(Boolean);
  if (lines.length === 0) return <p className="text-sm text-gray-500 dark:text-gray-400 py-4 text-center">Empty file</p>;

  const parseRow = (line: string): string[] => {
    const cols: string[] = [];
    let cur = '';
    let inQuote = false;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      if (ch === '"' && !inQuote) { inQuote = true; continue; }
      if (ch === '"' && inQuote && line[i + 1] === '"') { cur += '"'; i++; continue; }
      if (ch === '"' && inQuote) { inQuote = false; continue; }
      if (ch === ',' && !inQuote) { cols.push(cur); cur = ''; continue; }
      cur += ch;
    }
    cols.push(cur);
    return cols;
  };

  const headers = parseRow(lines[0]);
  const rows = lines.slice(1).map(parseRow);

  return (
    <div className="overflow-auto max-h-[65vh]">
      <table className="w-full text-sm border-collapse">
        <thead className="bg-gray-100 dark:bg-gray-700 sticky top-0">
          <tr>
            {headers.map((h, i) => (
              <th key={i} className="text-left px-3 py-2 font-medium text-gray-700 dark:text-gray-200 border-b border-gray-200 dark:border-gray-600 whitespace-nowrap">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, ri) => (
            <tr key={ri} className="even:bg-gray-50 dark:even:bg-gray-800/50 hover:bg-blue-50 dark:hover:bg-blue-900/20">
              {row.map((cell, ci) => (
                <td key={ci} className="px-3 py-1.5 text-gray-700 dark:text-gray-300 border-b border-gray-100 dark:border-gray-700/50">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <p className="text-xs text-gray-400 dark:text-gray-500 px-3 py-2">{rows.length} row{rows.length !== 1 ? 's' : ''}</p>
    </div>
  );
}

export function MarkdownPreview({ content }: { content: string }) {
  const toHtml = (md: string): string => {
    let html = md
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/^######\s+(.+)$/gm, '<h6>$1</h6>')
      .replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>')
      .replace(/^####\s+(.+)$/gm, '<h4>$1</h4>')
      .replace(/^###\s+(.+)$/gm, '<h3>$1</h3>')
      .replace(/^##\s+(.+)$/gm, '<h2>$1</h2>')
      .replace(/^#\s+(.+)$/gm, '<h1>$1</h1>')
      .replace(/^---+$/gm, '<hr/>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/__(.+?)__/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/_(.+?)_/g, '<em>$1</em>')
      .replace(/`(.+?)`/g, '<code>$1</code>')
      .replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>')
      .replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')
      .replace(/(<li>[\s\S]+?<\/li>)/g, '<ul>$1</ul>')
      .replace(/\n\n+/g, '</p><p>')
      .replace(/\n/g, '<br/>');
    return `<p>${html}</p>`;
  };

  return (
    <div
      className="prose prose-sm dark:prose-invert max-w-none p-4 overflow-auto max-h-[65vh] [&_h1]:text-xl [&_h1]:font-bold [&_h2]:text-lg [&_h2]:font-semibold [&_h3]:font-semibold [&_code]:bg-gray-100 [&_code]:dark:bg-gray-700 [&_code]:px-1 [&_code]:rounded [&_a]:text-blue-600 [&_a]:underline [&_ul]:list-disc [&_ul]:pl-5"
      dangerouslySetInnerHTML={{ __html: toHtml(content) }}
    />
  );
}
