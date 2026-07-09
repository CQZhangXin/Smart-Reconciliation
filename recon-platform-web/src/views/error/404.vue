<template>
  <div class="error-page">
    <div class="error-content">
      <h1>404</h1>
      <p class="title">页面未找到</p>
      <p class="desc">抱歉，您访问的页面不存在或已被移除</p>
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
  color: #409EFF;
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
