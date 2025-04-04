<script>
  import { onMount } from 'svelte';
  import { dashboardStore } from '../../stores/dashboard';
  import StatusBadge from '../shared/StatusBadge.svelte';

  // root uri from the global config
  const rootUri = window.BAYMAX_CONFIG?.rootUri || '/';

  let collectors = [];
  let scheduleInfo = {};
  let isLoading = true;

  const unsubscribe = dashboardStore.subscribe(data => {
    if (data.collectors?.items) {

      // "running?" has a question mark in the key, needs care
      collectors = data.collectors.items.map(collector => ({
        ...collector,
        running: collector.running || collector['running?'] || false
      }));
    }
  });

  // fetch schedule intel separately to get the next run times
  async function fetchScheduleStatus() {
    try {
      const response = await fetch(`${rootUri}schedule/status`);
      if (!response.ok) {
        throw new Error(`Failed to fetch schedule status: ${response.status}`);
      }

      const data = await response.json();

      // collector id to next run time
      const scheduleMap = {};

      if (data && data.schedules && Array.isArray(data.schedules)) {
        data.schedules.forEach(schedule => {
          scheduleMap[schedule.id] = {
            nextRun: schedule['next-run'],
            running: schedule['running?'] || false
          };
        });
      }

      scheduleInfo = scheduleMap;
      isLoading = false; // after data is fetched
    } catch (error) {
      console.error('Error fetching schedule status:', error);
      isLoading = false;
    }
  }

  onMount(() => {
    fetchScheduleStatus();

    const intervalId = setInterval(fetchScheduleStatus, 30000);

    return () => {
      unsubscribe();
      clearInterval(intervalId);
    };
  });

  function findNextRunTime(collectorId) {
    return scheduleInfo[collectorId]?.nextRun || 'Unknown';
  }
</script>

<div class="bg-white rounded-lg shadow-md p-6 mb-8">
  <div class="flex justify-between items-center mb-4">
    <h2 class="text-xl font-semibold text-gray-800">Collectors</h2>
    <a href="{rootUri}intel-all" class="text-baymax-primary hover:underline text-sm">
      <span>View all intel</span>
      <i class="fas fa-arrow-right ml-1"></i>
    </a>
  </div>

  <div class="overflow-x-auto">
    <table class="w-full">
      <thead>
        <tr class="bg-gray-50">
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Collector ID</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Source</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Schedule</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Next Run</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Status</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Actions</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-200">
        {#if collectors.length > 0}
          {#each collectors as collector}
            <tr>
              <td class="px-4 py-3 text-sm text-gray-900">{collector.id}</td>
              <td class="px-4 py-3 text-sm text-gray-500">{collector.source}</td>
              <td class="px-4 py-3 text-sm text-gray-500">{collector.schedule}</td>
              <td class="px-4 py-3 text-sm text-gray-500">
                {#if isLoading}
                  <span class="text-gray-400 italic">Loading...</span>
                {:else}
                  {findNextRunTime(collector.id)}
                {/if}
              </td>
              <td class="px-4 py-3 text-sm">
                <StatusBadge status={collector.running ? 'active' : 'inactive'} size="sm" />
              </td>
              <td class="px-4 py-3 text-sm">
                <a href="{rootUri}intel/{collector.id}" class="text-baymax-primary hover:underline mr-2">View Intel</a>
                <a href="{rootUri}schedule/status/{collector.id}" class="text-gray-500 hover:underline">Schedule</a>
              </td>
            </tr>
          {/each}
        {:else}
          <tr>
            <td colspan="6" class="px-4 py-3 text-sm text-gray-500 text-center">
              No collectors available or data is loading...
            </td>
          </tr>
        {/if}
      </tbody>
    </table>
  </div>
</div>
