import { ref } from 'vue'
import type { FormInstance } from 'element-plus'

export function useFormDialog<T extends Record<string, any>>(initData: () => T) {
  const dialogVisible = ref(false)
  const formData = ref<T>(initData())
  const formRef = ref<FormInstance>()
  const isEdit = ref(false)

  function openDialog(data?: T) {
    formData.value = data ? { ...data } : initData()
    isEdit.value = !!data
    dialogVisible.value = true
  }

  function closeDialog() {
    dialogVisible.value = false
    formRef.value?.resetFields()
  }

  return {
    dialogVisible,
    formData,
    formRef,
    isEdit,
    openDialog,
    closeDialog
  }
}
