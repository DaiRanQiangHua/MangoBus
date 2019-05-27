package com.mango.mbus;

import java.lang.reflect.Method;

/**
 * Author: Mangoer
 * Time: 2019/5/15 20:23
 * Version:
 * Desc: TODO(订阅者对象)
 */
public class SubscriptionMethod {

    //订阅方法
    private Method method;

    //订阅方法的参数类型
    private Class<?> type;

    //注解里的参数
    private ThreadMode threadMode;

    private boolean sticky;

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public ThreadMode getThreadMode() {
        return threadMode;
    }

    public void setThreadMode(ThreadMode threadMode) {
        this.threadMode = threadMode;
    }
}
