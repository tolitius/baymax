<script>
  import { onMount } from 'svelte';
  import { dashboardStore } from '../../stores/dashboard';
  import StatusBadge from '../shared/StatusBadge.svelte';
  import MetricCard from '../shared/MetricCard.svelte';
  import { formatMetricValue } from '../../lib/utils';

  let uptime = 'loading...';
  let collectorsValue = 'loading...';
  let sourcesValue = 'loading...';
  let publishersValue = 'loading...';
  let systemHealthy = true;

  const unsubscribe = dashboardStore.subscribe(data => {
    if (data.uptime) {
      uptime = data.uptime;
    }

    if (data.collectors) {
      const activeCount = data.collectors.items.filter(c => c["running?"]).length;
      const totalCount = data.collectors.count;
      collectorsValue = formatMetricValue(activeCount, totalCount);
    }

    if (data.sources) {
      const healthyCount = data.sources.items.filter(s => s.health.healthy).length;
      const totalCount = data.sources.count;
      sourcesValue = formatMetricValue(healthyCount, totalCount);
    }

    if (data.publishers) {
      const healthyCount = data.publishers.items.filter(p => p.health.healthy).length;
      const totalCount = data.publishers.count;
      publishersValue = formatMetricValue(healthyCount, totalCount);
    }

    // update system health status based on both sources and publishers
    systemHealthy = data.sources?.items.every(s => s.health.healthy) &&
                    data.publishers?.items.every(p => p.health.healthy);
  });

  onMount(() => {
    return () => {
      unsubscribe();
    };
  });
</script>

<div class="bg-white rounded-lg shadow-md p-6 mb-8">
  <div class="flex justify-between items-center mb-4">
    <h2 class="text-xl font-semibold text-gray-800">System Status</h2>
    <StatusBadge status={systemHealthy ? 'healthy' : 'unhealthy'} />
  </div>

  <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
    <MetricCard
      title="Uptime"
      value={uptime}
      icon="fas fa-clock"
      color="blue"
    />

    <MetricCard
      title="Active Collectors"
      value={collectorsValue}
      icon="fas fa-chart-line"
      color="green"
    />

    <MetricCard
      title="Healthy Sources"
      value={sourcesValue}
      icon="fas fa-database"
      color="purple"
    />

    <MetricCard
      title="Healthy Publishers"
      value={publishersValue}
      icon="fas fa-paper-plane"
      color="amber"
    />
  </div>
</div>
