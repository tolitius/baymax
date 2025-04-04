<script lang="ts">
  export let title = '';
  export let value = '';
  export let icon = '';
  export let color = 'blue';

  const colorMap = {
    blue: {
      bg: 'bg-blue-50',
      border: 'border-blue-200',
      text: 'text-blue-500'
    },
    green: {
      bg: 'bg-green-50',
      border: 'border-green-200',
      text: 'text-green-500'
    },
    purple: {
      bg: 'bg-purple-50',
      border: 'border-purple-200',
      text: 'text-purple-500'
    },
    orange: {
      bg: 'bg-orange-50',
      border: 'border-orange-200',
      text: 'text-orange-500'
    },
    red: {
      bg: 'bg-red-50',
      border: 'border-red-200',
      text: 'text-red-500'
    }
  };
</script>

<div class="border rounded-lg p-4 {colorMap[color].bg} {colorMap[color].border}">
  <div class="flex justify-between items-center">
    <div>
      <p class="text-sm text-gray-600">{title}</p>
      <p class="text-lg font-semibold">{value}</p>
    </div>
    <i class="{icon} text-2xl {colorMap[color].text}"></i>
  </div>
</div>
