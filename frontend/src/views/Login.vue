<template>
  <div class="login-container">
    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field
          v-model="username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>

      <div style="margin: 16px">
        <van-button 
          round 
          block 
          type="primary" 
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
        >
          登录
        </van-button>
      </div>
    </van-form>

    <!-- 错误提示 -->
    <van-toast v-if="error" type="fail">
      {{ error }}
    </van-toast>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import api from '@/api/config'
import type { AxiosResponse } from 'axios'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

interface LoginResponse {
  token: string;
  // 其他可能的响应字段
}

const handleLogin = async () => {
  loading.value = true
  error.value = ''
  
  try {
    const response = await api.post<LoginResponse>('/api/auth/login', {
      username: username.value,
      password: password.value
    }) as AxiosResponse<LoginResponse>
    
    // 保存 token
    localStorage.setItem('token', response.data.token)
    
    // 获取重定向地址
    const redirect = route.query.redirect?.toString() || '/'
    router.push(redirect)
  } catch (err: any) {
    error.value = err.response?.data?.message || '登录失败，请检查用户名和密码'
    showToast({
      type: 'fail',
      message: error.value
    })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  padding: 16px;
  background-color: var(--van-background);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* 桌面端优化 */
@media (min-width: 768px) {
  .van-cell-group {
    max-width: 400px;
    margin: 0 auto;
  }

  .van-button {
    max-width: 400px;
    margin: 16px auto;
  }
}
</style> 