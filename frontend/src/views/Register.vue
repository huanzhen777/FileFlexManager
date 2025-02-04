<template>
  <div class="register-container">
    <van-form @submit="handleRegister">
      <div class="register-header">
        <h2>创建管理员账号</h2>
      </div>

      <van-cell-group inset>
        <van-field
          v-model="username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]"
        >
          <template #left-icon>
            <van-icon name="user-o" />
          </template>
        </van-field>

        <van-field
          v-model="password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <template #left-icon>
            <van-icon name="lock" />
          </template>
        </van-field>

        <van-field
          v-model="confirmPassword"
          type="password"
          name="confirmPassword"
          label="确认密码"
          placeholder="请再次输入密码"
          :rules="[
            { required: true, message: '请确认密码' },
            { validator: validatePassword, message: '两次输入的密码不一致' }
          ]"
        >
          <template #left-icon>
            <van-icon name="passed" />
          </template>
        </van-field>
      </van-cell-group>

      <div style="margin: 16px">
        <van-button 
          round 
          block 
          type="primary" 
          native-type="submit"
          :loading="loading"
          loading-text="注册中..."
        >
          注册
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import api from '@/api/config'

const router = useRouter()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)

// 密码验证
const validatePassword = () => {
  if (!confirmPassword.value) return true
  return password.value === confirmPassword.value
}

// 注册处理
const handleRegister = async () => {
  if (password.value !== confirmPassword.value) {
    showToast('两次输入的密码不一致')
    return
  }

  loading.value = true
  
  try {
    // 注册
    await api.post('/api/auth/register', {
      username: username.value,
      password: password.value
    })
    
    // 注册成功后自动登录
    const loginResponse = await api.post('/api/auth/login', {
      username: username.value,
      password: password.value
    })
    
    localStorage.setItem('token', loginResponse.data.token)
    showToast({
      type: 'success',
      message: '注册成功'
    })
    router.push('/')
  } catch (error: any) {
    showToast({
      type: 'fail',
      message: error.response?.data?.message || '注册失败'
    })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  padding: 16px;
  background-color: var(--van-background);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.register-header h2 {
  margin: 0;
  font-size: 24px;
  color: var(--van-text-color);
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

/* 移动端优化 */
@media (max-width: 767px) {
  .register-header h2 {
    font-size: 20px;
  }

  .van-field {
    padding: 12px 16px;
  }
}
</style> 