import axios from 'axios';
import {showToast} from 'vant';
import router from '@/router';

// 更新后端响应数据的接口定义
export interface BaseResponse<T = any> {
    data: T;
    code: number;
    message: string;
}

export interface Page<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}

// 根据环境变量设置 baseURL
const baseURL = import.meta.env.VITE_API_URL;

const api = axios.create({
    baseURL,
    timeout: 15000,
    withCredentials: true
});

// 请求拦截器
api.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
api.interceptors.response.use(
    response => {
        // 如果是下载文件请求，直接返回响应
        if (response.config.responseType === 'blob') {
            return response;
        }

        // 处理业务响应
        const res = response.data;
        if (res.code === 200) {
            return res;
        } else {
            const errorMessage = res.message || '操作失败';
            showToast({
                type: 'fail',
                message: errorMessage
            });
            throw new Error(errorMessage);
        }
    },
    error => {
        // 如果是下载文件请求出错
        if (error.config?.responseType === 'blob') {
            return Promise.reject(error);
        }

        // 处理 token 相关错误
        if (error.response?.status === 401) {
            const errorCode = error.response.data?.code;

            // 清除本地存储的 token
            localStorage.removeItem('token');

            let errorMessage = '请重新登录';
            if (errorCode === 401001 || errorCode === 401002) {
                errorMessage = '登录已过期，请重新登录';
                // 直接跳转到登录页，不需要嵌套redirect
                if (router.currentRoute.value.path !== '/login') {
                    router.push('/login');
                }
            } else if (errorCode === 401003) {
                errorMessage = '无任何用户，请注册';
                // 直接跳转到注册页，不需要嵌套redirect
                if (router.currentRoute.value.path !== '/register') {
                    router.push('/register');
                }
            }

            showToast({
                type: 'fail',
                message: errorMessage
            });

            return Promise.reject(new Error(errorMessage));
        }

        // 处理其他错误响应
        let errorMessage;
        if (error.response?.data) {
            const responseData = error.response.data as BaseResponse;
            errorMessage = responseData.message;
        } else if (error.request) {
            errorMessage = '服务器无响应';
        } else {
            errorMessage = error.message || '请求配置错误';
        }

        showToast({
            type: 'fail',
            message: errorMessage
        });
        return Promise.reject(new Error(errorMessage));
    }
);

export default api; 