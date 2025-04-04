import StatusBadge from '../components/shared/StatusBadge.svelte';
import { getSourceTypeIcon } from './utils';

/**
 * data => sources table
 */
export function updateSourcesTable(sources) {
  const sourcesTableBody = document.getElementById('sources-table-body');
  if (!sourcesTableBody) return;

  sourcesTableBody.innerHTML = '';

  sources.items.forEach(source => {
    const row = document.createElement('tr');

    const idCell = document.createElement('td');
    idCell.className = 'px-4 py-3 text-sm text-gray-900';
    idCell.textContent = source.id;
    row.appendChild(idCell);

    const typeCell = document.createElement('td');
    typeCell.className = 'px-4 py-3 text-sm text-gray-500';

    const typeSpan = document.createElement('span');
    typeSpan.className = 'flex items-center';

    const typeInfo = getSourceTypeIcon(source.type);

    const icon = document.createElement('i');
    icon.className = `${typeInfo.icon} mr-2 ${typeInfo.color}`;

    typeSpan.appendChild(icon);
    typeSpan.appendChild(document.createTextNode(typeInfo.label));

    typeCell.appendChild(typeSpan);
    row.appendChild(typeCell);

    // connection
    const connCell = document.createElement('td');
    connCell.className = 'px-4 py-3 text-sm text-gray-500';

    const connSpan = document.createElement('span');
    connSpan.className = 'font-mono';
    connSpan.textContent = '[redacted]:[redacted]/[redacted]';

    connCell.appendChild(connSpan);
    row.appendChild(connCell);

    // status
    const statusCell = document.createElement('td');
    statusCell.className = 'px-4 py-3 text-sm';

    const statusBadgeContainer = document.createElement('div');
    statusCell.appendChild(statusBadgeContainer);

    new StatusBadge({
      target: statusBadgeContainer,
      props: {
        status: source.health.healthy ? "healthy" : "unhealthy",
        size: "sm"
      }
    });

    row.appendChild(statusCell);

    sourcesTableBody.appendChild(row);
  });
}

/**
 * updates component with new value if it exists
 */
export function updateSvelteComponentValue(elementId: string, value: any): void {
  const element = document.getElementById(elementId);
  if (!element) return;

  const component = element.__svelte?.instance;
  if (component) {
    component.$$set({ value });
  }
}

/**
 * Updates component status if it exists
 */
export function updateSvelteComponentStatus(elementId: string, status: string): void {
  const element = document.getElementById(elementId);
  if (!element) return;

  const component = element.__svelte?.instance;
  if (component) {
    component.$$set({ status });
  }
}
