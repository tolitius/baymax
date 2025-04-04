<script lang="ts">
  export let status = 'healthy'; // 'healthy', 'unhealthy', 'warning', 'active', 'inactive'
  export let size = 'md';

  const statusConfig = {
    healthy: {
      bg: 'bg-green-100',
      text: 'text-green-800',
      icon: 'fa-check-circle'
    },
    unhealthy: {
      bg: 'bg-red-100',
      text: 'text-red-800',
      icon: 'fa-exclamation-circle'
    },
    warning: {
      bg: 'bg-yellow-100',
      text: 'text-yellow-800',
      icon: 'fa-exclamation-triangle'
    },
    active: {
      bg: 'bg-green-100',
      text: 'text-green-800',
      icon: 'fa-check-circle'
    },
    inactive: {
      bg: 'bg-gray-100',
      text: 'text-gray-800',
      icon: 'fa-times-circle'
    }
  };

  // consistent padding and width
  const sizeClass = size === 'sm'
    ? 'px-2 py-1 text-xs min-w-[80px] inline-flex justify-center'
    : 'px-3 py-1 text-sm min-w-[100px] inline-flex justify-center';

  // display text based on status
  function findStatusText(status) {
    const displayMap = {
      'healthy': 'Healthy',
      'unhealthy': 'Unhealthy',
      'warning': 'Warning',
      'active': 'Active',
      'inactive': 'Inactive'
    };

    return displayMap[status] || status;
  }
</script>

<span class="{sizeClass} {statusConfig[status].bg} {statusConfig[status].text} rounded-full flex items-center justify-center">
  <i class="fas {statusConfig[status].icon} mr-1"></i>
  {findStatusText(status)}
</span>
