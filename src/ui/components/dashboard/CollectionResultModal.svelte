<script>
  import Modal from '../shared/Modal.svelte';

  export let show = false;
  export let result = null;

  // format timestamp
  $: formattedTimestamp = result?.timestamp || '';

  // use the status to determine display style
  $: statusColor = result?.status === 'completed' ? 'text-green-600' : 'text-red-600';
  $: statusIcon = result?.status === 'completed' ? 'fa-check-circle' : 'fa-exclamation-circle';
</script>

<Modal bind:show title="collection status" on:close>
  {#if result}
    <div class="space-y-4">
      <!-- status header with icon -->
      <div class="flex items-center">
        <i class="fas {statusIcon} {statusColor} text-2xl mr-2"></i>
        <span class="text-lg font-medium {statusColor} capitalize">{result.status}</span>
      </div>

      <!-- collection details -->
      <div class="bg-gray-50 p-4 rounded-lg">
        <dl class="grid grid-cols-1 gap-x-4 gap-y-3 sm:grid-cols-2">
          <div class="sm:col-span-1">
            <dt class="text-sm font-medium text-gray-500">collector</dt>
            <dd class="mt-1 text-sm text-gray-900">{result['collector-id']}</dd>
          </div>
          <div class="sm:col-span-1">
            <dt class="text-sm font-medium text-gray-500">took</dt>
            <dd class="mt-1 text-sm text-gray-900">{result.duration}</dd>
          </div>
          <div class="sm:col-span-2">
            <dt class="text-sm font-medium text-gray-500">published to</dt>
            <dd class="mt-1 text-sm text-gray-900">
              <div class="flex flex-wrap gap-2">
                {#each result.publishers as publisher}
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                    {publisher}
                  </span>
                {/each}
              </div>
            </dd>
          </div>
          <div class="sm:col-span-2">
            <dt class="text-sm font-medium text-gray-500">was done at</dt>
            <dd class="mt-1 text-sm text-gray-900">{formattedTimestamp}</dd>
          </div>
        </dl>
      </div>
    </div>
  {:else}
    <div class="py-4 text-center text-gray-500">
      No result information available
    </div>
  {/if}
</Modal>
