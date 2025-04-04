<!-- NavigationCards component -->
<script>
  // Get the root URI from the global config
  const rootUri = window.BAYMAX_CONFIG?.rootUri || '/';

  // Define the navigation cards
  const navCards = [
    {
      title: 'All Metrics',
      description: 'View all collected intel',
      icon: 'fas fa-chart-bar',
      iconColor: 'blue-500',
      borderColor: 'baymax-primary',
      href: `${rootUri}intel-all`
    },
    {
      title: 'Schedules',
      description: 'Monitor collection schedules',
      icon: 'fas fa-calendar-alt',
      iconColor: 'green-500',
      borderColor: 'green-500',
      href: `${rootUri}schedule/status`
    },
    {
      title: 'Health Check',
      description: 'View system health status',
      icon: 'fas fa-heartbeat',
      iconColor: 'purple-500',
      borderColor: 'purple-500',
      href: `${rootUri}schedule/health`
    }
  ];
</script>

<div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
  {#each navCards as card}
    <a href={card.href} class="block bg-white rounded-lg shadow-md p-6 transition duration-300 ease-in-out hover-card border-l-4 border-{card.borderColor}">
      <div class="flex items-center">
        <div class="bg-{card.iconColor.replace('-500', '-100')} p-3 rounded-lg mr-4">
          <i class="{card.icon} text-{card.iconColor} text-xl"></i>
        </div>
        <div>
          <h3 class="text-lg font-semibold text-gray-800">{card.title}</h3>
          <p class="text-gray-600 text-sm">{card.description}</p>
        </div>
      </div>
    </a>
  {/each}
</div>
