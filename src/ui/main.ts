import { fetchDashboardIntel } from './lib/api';
import { dashboardStore } from './stores/dashboard';

// update the uptime display
function updateUptimeDisplay(uptime: string) {
  const uptimeElement = document.getElementById('uptime-value');
  if (uptimeElement) {
    uptimeElement.textContent = uptime;
  }
}

// update the collectors count display
function updateCollectorsDisplay(collectors) {
  const collectorsElement = document.getElementById('collectors-count');
  if (collectorsElement) {
    // running has a question mark in the key
    const activeCount = collectors.items.filter(c => c["running?"]).length;
    const totalCount = collectors.count;
    collectorsElement.textContent = `${activeCount} / ${totalCount}`;
  }
}

// update the sources count display
function updateSourcesDisplay(sources) {
  const sourcesElement = document.getElementById('sources-count');
  if (sourcesElement) {
    const healthyCount = sources.items.filter(s => s.health.healthy).length;
    const totalCount = sources.count;
    sourcesElement.textContent = `${healthyCount} / ${totalCount}`;
  }
}

// update the sources table display
function updateSourcesTable(sources) {
  const sourcesTableBody = document.getElementById('sources-table-body');
  if (sourcesTableBody) {

    sourcesTableBody.innerHTML = '';

    sources.items.forEach(source => {
      const row = document.createElement('tr');

      // source name
      const idCell = document.createElement('td');
      idCell.className = 'px-4 py-3 text-sm text-gray-900';
      idCell.textContent = source.id;
      row.appendChild(idCell);

      // source section icon
      const typeCell = document.createElement('td');
      typeCell.className = 'px-4 py-3 text-sm text-gray-500';

      const typeSpan = document.createElement('span');
      typeSpan.className = 'flex items-center';

      const icon = document.createElement('i');

      // icon per source type
      if (source.type === 'postgres') {
        icon.className = 'fas fa-database mr-2 text-indigo-500';
        typeSpan.appendChild(icon);
        typeSpan.appendChild(document.createTextNode('PostgreSQL'));
      } else if (source.type === 'http') {
        icon.className = 'fas fa-globe mr-2 text-blue-500';
        typeSpan.appendChild(icon);
        typeSpan.appendChild(document.createTextNode('HTTP'));
      } else if (source.type === 'jmx') {
        icon.className = 'fas fa-cogs mr-2 text-orange-500';
        typeSpan.appendChild(icon);
        typeSpan.appendChild(document.createTextNode('JMX'));
      } else {
        icon.className = 'fas fa-question-circle mr-2 text-gray-500';
        typeSpan.appendChild(icon);
        typeSpan.appendChild(document.createTextNode(source.type));
      }

      typeCell.appendChild(typeSpan);
      row.appendChild(typeCell);

      // connection
      const connCell = document.createElement('td');
      connCell.className = 'px-4 py-3 text-sm text-gray-500';

      const connSpan = document.createElement('span');
      connSpan.className = 'font-mono';
      // todo: replace with actual connection string based on source type
      connSpan.textContent = '[redacted]:[redacted]/[redacted]';

      connCell.appendChild(connSpan);
      row.appendChild(connCell);

      // status
      const statusCell = document.createElement('td');
      statusCell.className = 'px-4 py-3 text-sm';

      const statusSpan = document.createElement('span');
      if (source.health.healthy) {
        statusSpan.className = 'px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs flex items-center w-20 justify-center';

        const statusIcon = document.createElement('i');
        statusIcon.className = 'fas fa-check-circle mr-1';
        statusSpan.appendChild(statusIcon);
        statusSpan.appendChild(document.createTextNode('Healthy'));
      } else {
        statusSpan.className = 'px-2 py-1 bg-red-100 text-red-800 rounded-full text-xs flex items-center w-20 justify-center';

        const statusIcon = document.createElement('i');
        statusIcon.className = 'fas fa-exclamation-circle mr-1';
        statusSpan.appendChild(statusIcon);
        statusSpan.appendChild(document.createTextNode('Unhealthy'));
      }

      statusCell.appendChild(statusSpan);
      row.appendChild(statusCell);

      // latch it onto the table
      sourcesTableBody.appendChild(row);
    });
  }
}


// subscribe to store changes
dashboardStore.subscribe(data => {
  if (data.uptime) {
    updateUptimeDisplay(data.uptime);
  }
  if (data.collectors) {
    updateCollectorsDisplay(data.collectors);
  }
  if (data.sources) {
    updateSourcesDisplay(data.sources);
    updateSourcesTable(data.sources);
  }
});

// initial data fetch
async function initDashboard() {
  try {
    const data = await fetchDashboardIntel();
    dashboardStore.set(data);

    // start periodic refresh
    setInterval(async () => {
      try {
        const freshData = await fetchDashboardIntel();
        dashboardStore.set(freshData);
      } catch (error) {
        console.error('could not refresh:', error);
      }
    }, 10000); // every 10 seconds
  } catch (error) {
    console.error('initial data fetch failed:', error);
    updateUptimeDisplay('could not load dashboard intel');
  }
}

// boot it up
initDashboard();
