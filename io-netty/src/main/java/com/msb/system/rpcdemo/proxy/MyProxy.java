package com.bjmashibing.system.rpcdemo.proxy;


import com.bjmashibing.system.rpcdemo.rpc.Dispatcher;
import com.bjmashibing.system.rpcdemo.rpc.protocol.MyContent;
import com.bjmashibing.system.rpcdemo.rpc.transport.ClientFactory;
import lombok.extern.java.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

@Log
public class MyProxy {

    public static <T> T proxyGet(Class<T> interfaceInfo) {
        //实现各个版本的动态代理。。。。

        ClassLoader loader = interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};

        //TODO  LOCAL REMOTE  实现：  用到dispatcher  直接给你返回，还是本地调用的时候也代理一下

        Dispatcher dis = Dispatcher.getDis();
        return (T) Proxy.newProxyInstance(loader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                Object result = null;
                /* 获取调用的interface name, 看是否local     */
                Object o = dis.get(interfaceInfo.getName());
                if (o == null) {
                    //就要走rpc
                    log.info("MyProxy#proxyGet-InvocationHandler: RPC call...");

                    String name = interfaceInfo.getName();
                    String methodName = method.getName();
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    //TODO  RPC  就像小火车拉货  content是service的具体数据，但是还需要header层完成IO传输的控制
                    MyContent content = new MyContent();
                    content.setArgs(args);
                    content.setName(name);
                    content.setMethodName(methodName);
                    content.setParameterTypes(parameterTypes);
                    //TODO 未来的小火车可能会变

                    /**
                     * 1,缺失了注册发现，zk
                     * 2,第一层负载面向的provider
                     * 3，consumer  线程池  面向 service；并发就有木桶，倾斜
                     * serviceA
                     *      ipA:port
                     *          socket1
                     *          socket2
                     *      ipB:port
                     */
                    CompletableFuture resultFuture = ClientFactory.transport(content);

                    result = resultFuture.get();//阻塞的

                } else {
                    //就是local
                    //插入一些插件的机会，做一些扩展
                    log.info("MyProxy#proxyGet-InvocationHandler: local Function call...");
                    Class<?> clazz = o.getClass();
                    try {
                        Method m = clazz.getMethod(method.getName(), method.getParameterTypes());
                        result = m.invoke(o, args);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                }
                return result;

                //TODO 应该在service的方法执行的时候确定是本地的还是远程的，用到dispatcher来区分下

            }
        });


    }


}
