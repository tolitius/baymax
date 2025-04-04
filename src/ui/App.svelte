<script>
  import { onMount } from 'svelte';
  import { fetchDashboardIntel } from './lib/api';
  import { dashboardStore } from './stores/dashboard';
  import Dashboard from './components/Dashboard.svelte';

  let loading = true;
  let error = null;

  async function loadDashboardData() {
    try {
      const data = await fetchDashboardIntel();
      dashboardStore.set(data);
      loading = false;
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
      error = 'Failed to load dashboard data. Please try again later.';
      loading = false;
    }
  }

  function setupRefresh() {
    const refreshInterval = setInterval(async () => {
      try {
        const freshData = await fetchDashboardIntel();
        dashboardStore.set(freshData);
      } catch (err) {
        console.error('Failed to refresh dashboard data:', err);
      }
    }, 10000);

    return () => clearInterval(refreshInterval);
  }

  onMount(() => {
    loadDashboardData();
    return setupRefresh();
  });
</script>

{#if loading}
  <!-- loading -->
  <div class="flex items-center justify-center h-screen bg-baymax">
    <div class="text-center">
      <i class="fas fa-spinner fa-spin text-4xl text-baymax-primary mb-4"></i>
      <p class="text-xl text-gray-700">Loading baymax dashboard...</p>
    </div>
  </div>
{:else if error}
  <!-- error -->
  <div class="flex items-center justify-center h-screen bg-baymax">
    <div class="bg-white p-8 rounded-lg shadow-md max-w-md text-center">
      <i class="fas fa-exclamation-circle text-4xl text-red-500 mb-4"></i>
      <h2 class="text-2xl font-bold text-gray-800 mb-4">Oops!</h2>
      <p class="text-gray-700 mb-6">{error}</p>
      <button
        class="bg-baymax-primary hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
        on:click={loadDashboardData}
      >
        Try Again
      </button>
    </div>
  </div>
{:else}
  <!-- da dashboard -->
  <Dashboard />
{/if}
