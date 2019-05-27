package com.mango.mbus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: Mangoer
 * Time: 2019/5/15 20:20
 * Version:
 * Desc: TODO(代理类)
 */
public class MangoBus {

    private String TAG = "MangoBus";

    /**
     * 订阅方法
     * key 订阅类
     * value 类中的订阅方法集合
     */
    private Map<Object,List<SubscriptionMethod>>  mSubcription ;
    private Handler mHandler;
    private ExecutorService THREAD_POOL_EXECUTOR;

    private final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT-1,4));
    private final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2;
    private final int KEEP_ALIVE_SECONDS = 60;
    private final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<>(8);

    private final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            return new Thread(r, "MangoBus #" + mCount.getAndIncrement());
        }
    };

    private class RejectedHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            //可在这里做一些提示用户的操作
        }
    }

    {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory,new RejectedHandler()){
            @Override
            public void execute(Runnable command) {
                super.execute(command);
            }
        };
        //允许核心线程空闲超时时被回收
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    private MangoBus() {
        mSubcription = new HashMap<>();
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 将这个类进行单例处理，保证mSubcription唯一性
     */
    private static class SingleTonInsance{
        private static final MangoBus MANGO_BUS = new MangoBus();
    }

    public static MangoBus getInstance(){
        return SingleTonInsance.MANGO_BUS;
    }

    /**
     * 供订阅者调用 进行注册
     * 取出订阅者的所有订阅方法，将其保存到mSubcription
     * @param subscriber
     */
    public void register(Object subscriber){

        List<SubscriptionMethod> methodList = mSubcription.get(subscriber);
        if (methodList == null) {
            Class<?> clazz = subscriber.getClass();
            methodList = new ArrayList<>();
            /**
             * 某些情况下我们继承了父类，无需自己在写订阅方法，那就重写父类的订阅方法
             * 想要获取到事件进行处理，那么就需要将父类的订阅方法也保存起来
             */
            while (clazz != null) {
                String className = clazz.getName();
                if (className.startsWith("java.") || className.startsWith("javax.")
                        || className.startsWith("android.")) {
                    break;
                }
                findAnnotationMethod(methodList,clazz);
                clazz = clazz.getSuperclass();
            }
            mSubcription.put(subscriber,methodList);
        }
    }

    private void findAnnotationMethod(List<SubscriptionMethod> methodList, Class<?> clazz){
        //获取订阅者自身的所有方法，而getMethod会将父类的方法也拿到
        Method[] m = clazz.getDeclaredMethods();
        int size = m.length;
        for (int i=0; i<size; i++) {
            Method method = m[i];
            Log.e("MangoBus","findAnnotationMethod "+method.getName());

            //拿到该方法的注解，找到使用Subscribe注解的方法
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;
            /**
             * 到这里说明该方法使用了我们定义的Subscribe注解
             * 接下来需要判断该注解方法是否符合规范
             * 1. 返回值必须是void
             * 2. 方法修饰符必须是public，且是非静态抽象的
             * 3. 方法参数必须只有一个
             */
            //如果方法返回类型不是void 抛出异常
            Type genericReturnType = method.getGenericReturnType();
            if (!"void".equals(genericReturnType.toString())) {
                throw new MangoBusException("方法返回值必须是void");
            }
            //如果方法修饰符不是public 抛出异常
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 1) {
                throw new MangoBusException("方法修饰符必须是public，且是非静态，非抽象");
            }
            //获取方法的参数，如果参数不是一个 抛出异常
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new MangoBusException("方法参数个数必须是一个");
            }

            //这里就需要实例化订阅方法对象了
            SubscriptionMethod subscriptionMethod = new SubscriptionMethod();
            subscriptionMethod.setMethod(method);
            subscriptionMethod.setType(parameterTypes[0]);
            subscriptionMethod.setThreadMode(annotation.threadMode());
            methodList.add(subscriptionMethod);
        }
    }

    /**
     * 发布粘性事件
     * @param event
     */
    public void postSticky(Object event){


    }

    /**
     * 发布事件
     * 根据参数类型找出对应的方法并调用
     * @param event
     */
    public void post(final Object event){

        Set<Object> set = mSubcription.keySet();
        Iterator<Object> iterator = set.iterator();
        while (iterator.hasNext()) {
            final Object target = iterator.next();
            List<SubscriptionMethod> methodList = mSubcription.get(target);
            if (methodList == null || mSubcription.size() == 0) {
                continue;
            }

            int size = methodList.size();
            for (int i = 0; i < size; i++) {
                final SubscriptionMethod method = methodList.get(i);
                //method.getType()是获取方法参数类型，这里是判断发布的对象类型是否与订阅方法的参数类型一致
                if (method.getType().isAssignableFrom(event.getClass())) {
                    //进行线程切换
                    switch (method.getThreadMode()) {
                        case POSTING:
                            invoke(target,method,event);
                            break;
                        case MAIN:
                            //通过Looper判断当前线程是否是主线程
                            //也可以通过线程名判断 "main".equals(Thread.currentThread().getName())
                            if (Looper.getMainLooper() == Looper.myLooper()) {
                                invoke(target,method,event);
                            } else {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(target,method,event);
                                    }
                                });
                            }
                            break;
                        case BACKGROUND:
                            if (Looper.getMainLooper() == Looper.myLooper()) {
                                THREAD_POOL_EXECUTOR.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(target,method,event);
                                    }
                                });
                            } else {
                                invoke(target,method,event);
                            }
                            break;
                        case ASYNC:
                            THREAD_POOL_EXECUTOR.execute(new Runnable() {
                                @Override
                                public void run() {
                                    invoke(target,method,event);
                                }
                            });
                            break;
                    }

                }
            }
        }

    }


    /**
     * 通过反射执行订阅方法
     * @param next
     * @param method
     * @param event
     */
    private void invoke(Object next, SubscriptionMethod method, Object event) {

        Method m = method.getMethod();
        try {
            m.invoke(next,event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消订阅
     * @param target
     */
    public void unRegister(Object target){

        List<SubscriptionMethod> methodList = mSubcription.get(target);
        if (methodList == null) return;
        methodList.clear();
        mSubcription.remove(target);
    }

}
