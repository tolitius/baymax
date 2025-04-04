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
