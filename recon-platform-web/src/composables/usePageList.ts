import { ref, reactive } from 'vue'
import type { PageResult } from '@/types'

interface UsePageListOptions<T> {
  fetchApi: (params: any) => Promise<PageResult<T>>
  deleteApi?: (id: number) => Promise<void>
  defaultPageSize?: number
}

export function usePageList<T extends { id?: number }>(options: UsePageListOptions<T>) {
  const loading = ref(false)
  const dataList = ref<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(options.defaultPageSize || 10)
  const searchForm = reactive<Record<string, any>>({})
  // NOTE: Search currently triggers immediately on change. Consider adding a debounce wrapper
  // (e.g., lodash debounce or setTimeout-based) for search fields that fire on keystroke input
  // to avoid excessive API calls. The type signature is intentionally kept as Record<string, any>
  // because changing it to generics would break all existing consumers.

  async function fetchData() {
    loading.value = true
    try {
      const res = await options.fetchApi({
        page: page.value,
        size: pageSize.value,
        ...searchForm
      })
      dataList.value = res.records || []
      total.value = res.total || 0
    } finally {
      loading.value = false
    }
  }

  async function handleDelete(id: number) {
    if (options.deleteApi) {
      await options.deleteApi(id)
    }
    await fetchData()
  }

  function handleSearch() {
    page.value = 1
    fetchData()
  }

  function handleReset() {
    Object.keys(searchForm).forEach((k) => delete searchForm[k])
    handleSearch()
  }

  function handlePageChange(p: number) {
    page.value = p
    fetchData()
  }

  function handleSizeChange(s: number) {
    pageSize.value = s
    page.value = 1
    fetchData()
  }

  return {
    loading,
    dataList,
    total,
    page,
    pageSize,
    searchForm,
    fetchData,
    handleDelete,
    handleSearch,
    handleReset,
    handlePageChange,
    handleSizeChange
  }
}
