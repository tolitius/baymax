<script>
  import { onMount } from 'svelte';
  import { dashboardStore } from '../../stores/dashboard';
  import StatusBadge from '../shared/StatusBadge.svelte';

  let publishers = [];

  function findPublisherIcon(type) {
    switch (type) {
      case 'prometheus':
        return {
          icon: 'fas fa-chart-line',
          color: 'text-orange-500',
          label: 'Prometheus'
        };
      case 'elasticsearch':
      case 'elastic':
        return {
          icon: 'fas fa-search',
          color: 'text-yellow-500',
          label: 'Elasticsearch'
        };
      case 'stdout':
        return {
          icon: 'fas fa-terminal',
          color: 'text-gray-500',
          label: 'Stdout'
        };
      default:
        return {
          icon: 'fas fa-question-circle',
          color: 'text-gray-500',
          label: type
        };
    }
  }

  const unsubscribe = dashboardStore.subscribe(data => {
    if (data.publishers?.items) {
      publishers = data.publishers.items;
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
    <h2 class="text-xl font-semibold text-gray-800">Publishers</h2>
  </div>

  <div class="overflow-x-auto">
    <table class="w-full">
      <thead>
        <tr class="bg-gray-50">
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Publisher ID</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Type</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Collectors</th>
          <th class="px-4 py-3 text-left text-sm font-medium text-gray-500 uppercase tracking-wider">Status</th>
        </tr>
      </thead>
      <tbody>
        {#if publishers.length > 0}
          {#each publishers as publisher}
            {@const typeInfo = findPublisherIcon(publisher.type)}
            <tr>
              <td class="px-4 py-3 text-sm text-gray-900">{publisher.id}</td>
              <td class="px-4 py-3 text-sm text-gray-500">
                <span class="flex items-center">
                  <i class="{typeInfo.icon} mr-2 {typeInfo.color}"></i>
                  {typeInfo.label}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-500">
                <span>{publisher.collectors?.length || 0} collectors</span>
              </td>
              <td class="px-4 py-3 text-sm text-left">
                <div class="inline-flex">
                  <StatusBadge status={publisher.health?.healthy ? 'healthy' : 'unhealthy'} size="sm" />
                </div>
              </td>
            </tr>
          {/each}
        {:else}
          <tr>
            <td colspan="4" class="px-4 py-3 text-sm text-gray-500 text-center">
              No publishers available or data is loading...
            </td>
          </tr>
        {/if}
      </tbody>
    </table>
  </div>
</div>
