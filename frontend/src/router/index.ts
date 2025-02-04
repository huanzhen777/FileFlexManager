import {createRouter, createWebHistory, RouteRecordRaw} from 'vue-router';

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/files',
        name: 'FileList',
        component: () => import('../views/FileList.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/Register.vue')
    },
    {
        path: '/setting',
        name: 'Setting',
        component: () => import('@/views/setting/SettingView.vue'),
        meta: {
            title: '系统设置'
        }
    },
    {
        path: '/tag-browser',
        name: 'TagBrowser',
        component: () => import('@/views/TagBrowser.vue'),
        meta: {
            title: '标签'
        }
    }
];

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
});

// 路由守卫
/*router.beforeEach(async (to, from, next) => {
/!*    // 检查是否有管理员用户
    try {
        const response = await api.get('/api/auth/check-admin');
        const hasAdmin = response.data;

        if (!hasAdmin && to.path !== '/register') {
            // 如果没有管理员且不是访问注册页面，则跳转到注册页面
            next('/register');
            return;
        }
    } catch (error) {
        console.error('检查管理员失败:', error);
    }

    // 正常的认证检查
    const token = localStorage.getItem('token');

    if (to.matched.some(record => record.meta.requiresAuth)) {
        if (!token) {
            next({
                path: '/login',
                query: { redirect: to.fullPath }
            });
        } else {
            next();
        }
    } else {
        next();
    }*!/
});*/

export default router; 