<template>
  <div class="error-page">
    <div class="error-content">
      <h1>403</h1>
      <p class="title">访问被拒绝</p>
      <p class="desc">抱歉，您没有权限访问此页面</p>
      <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 400px;
}
.error-content {
  text-align: center;
}
.error-content h1 {
  font-size: 72px;
  color: #E6A23C;
  margin: 0;
}
.error-content .title {
  font-size: 20px;
  color: #303133;
  margin: 16px 0 8px;
}
.error-content .desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}
</style>
