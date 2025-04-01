import { fetchDashboardIntel } from './lib/api';
import { dashboardStore } from './stores/dashboard';

// update the uptime display
function updateUptimeDisplay(uptime: string) {
  const uptimeElement = document.getElementById('uptime-value');
  if (uptimeElement) {
    uptimeElement.textContent = uptime;
  }
}

// subscribe to store changes
dashboardStore.subscribe(data => {
  if (data.uptime) {
    updateUptimeDisplay(data.uptime);
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
