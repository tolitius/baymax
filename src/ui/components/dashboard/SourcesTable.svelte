<script>
  import { onMount } from 'svelte';
  import { dashboardStore } from '../../stores/dashboard';
  import StatusBadge from '../shared/StatusBadge.svelte';
  import { getSourceTypeIcon } from '../../lib/utils';

  let sources = [];

  const unsubscribe = dashboardStore.subscribe(data => {
    if (data.sources?.items) {
      sources = data.sources.items;
    }
  });

  onMount(() => {
    return () => {
      unsubscribe();
    };
  });
</script>

<div class="bg-white rounded-lg shadow-md p-6">
  <div class="flex justify-between items-center mb-4">
    <h2 class="text-xl font-semibold text-gray-800">Data Sources</h2>
  </div>

  <div class="overflow-x-auto">
    <table class="w-full">
      <thead>
        <tr class="bg-gray-50">
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Source ID</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Type</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Connection</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Status</th>
        </tr>
      </thead>
      <tbody>
        {#if sources.length > 0}
          {#each sources as source}
            {@const typeInfo = getSourceTypeIcon(source.type)}
            <tr>
              <td class="px-4 py-3 text-sm text-gray-900">{source.id}</td>
              <td class="px-4 py-3 text-sm text-gray-500">
                <span class="flex items-center">
                  <i class="{typeInfo.icon} mr-2 {typeInfo.color}"></i>
                  {typeInfo.label}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-500">
                <span class="font-mono">[redacted]:[redacted]/[redacted]</span>
              </td>
              <td class="px-4 py-3 text-sm text-left">
                <div class="inline-flex">
                  <StatusBadge status={source.health.healthy ? 'healthy' : 'unhealthy'} size="sm" />
                </div>
              </td>
            </tr>
          {/each}
        {:else}
          <tr>
            <td colspan="4" class="px-4 py-3 text-sm text-gray-500 text-center">
              No sources available or data is loading...
            </td>
          </tr>
        {/if}
      </tbody>
    </table>
  </div>
</div>
