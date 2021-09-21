package com.bjmashibing.system.io.testreactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectorThreadGroup {  //天生都是boss

    SelectorThread[] sts;
    ServerSocketChannel server = null;
    AtomicInteger xid = new AtomicInteger(0);

    SelectorThreadGroup stg = this;

    // worker组的stg是自己的引用this
    // boss组的stg是传入的worker组的引用
    public void setWorker(SelectorThreadGroup stg) {
        this.stg = stg;
    }

    // 指定每个group里的线程个数, Group初始化时,默认stg的引用都是this,
    // 根据后面是否设置worker来决定这个组是Boss还是worker
    SelectorThreadGroup(int num) {
        //num  线程数
        sts = new SelectorThread[num];
        for (int i = 0; i < num; i++) {
            sts[i] = new SelectorThread(this);

            new Thread(sts[i]).start();
        }
    }

    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            //注册到哪个selector上呢？
//            nextSelectorV2(server);
            nextSelectorV3(server);  //选择一个selector来注册这个listen socket
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    ServerSocketChannel和SocketChannel都抽象成Channel接口, 方法可以复用

    如果Channel是listen socket, 需要boss组来处理, 此时如果一个组是Boss,那么它的stg一个是worker组的引用,
    直接调用next()方法获取this.sts数组内的一个线程, 并把需要处理的listen socket加入该选中线程的Queue内,
    并设置该Boss线程的worker组, 即把自己获取到的worker组的引用传过去

    所以一旦一个Boss线程被选出来绑定了listen socket, 该线程内的stg引用就都是一个worker组
     */
    public void nextSelectorV3(Channel c) {
        try {
            if (c instanceof ServerSocketChannel) {
                /*
                此时要处理的是listen socket, 也就是准备接收连接的socket, 要注册到boss组
                 */
                SelectorThread st = next();  // listen 选择了 boss组中的一个正在run的线程后，要更新这个线程的worker组
                st.lbq.put(c);
                st.setWorker(stg); // boss组的这个线程设定workerGroup
                st.selector.wakeup();

            } else {
        /*
        如果Channel是连接socket, 需要调用worker组的内线程去处理, 所以需要该类内的stg引用,
        acceptHandler()方法内注册client的操作会调起该方法并传过来一个SocketChannel,
        因为每个boss线程都已经被赋值了worker组的引用, 所以在调用nextV3方法时:
        int index = xid.incrementAndGet() % stg.sts.length;
        return stg.sts[index];
        方法内的stg都是worker组的引用, 所以取到的也都是worker线程
         */
                SelectorThread st = nextV3();  //在 main线程种，取到堆里的selectorThread对象

                //1,通过队列传递数据 消息
                st.lbq.add(c);
                //2,通过打断阻塞，让对应的线程去自己在打断后完成注册selector
                st.selector.wakeup();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


    // Version2, 固定把ServerSocketChannel绑定在0号线程上, 交给0号线程的selector处理所有server socket
    public void nextSelectorV2(Channel c) {

        try {
            if (c instanceof ServerSocketChannel) {
                // 如果是ServerSocket,就固定绑到0号线程上
                sts[0].lbq.put(c);
                sts[0].selector.wakeup();

            } else {
                SelectorThread st = nextV2();  //在main线程种，取到堆里的selectorThread对象
                //1,通过队列传递数据 消息
                st.lbq.add(c);
                //2,通过打断阻塞，让对应的线程去自己在打断后完成注册selector
                st.selector.wakeup();

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void nextSelector(Channel c) {
        SelectorThread st = next();  //在main线程中，取到堆里的selectorThread对象

        //1,通过队列传递数据 消息
        st.lbq.add(c);
        //2,通过打断select()产生的阻塞，让对应的线程去自己在打断后完成注册selector
        st.selector.wakeup();

//    public void nextSelector(Channel c) {
//        SelectorThread st = next();  //在 main线程种，取到堆里的selectorThread对象
//
//        //1,通过队列传递数据 消息
//        st.lbq.add(c);
//        //2,通过打断阻塞，让对应的线程去自己在打断后完成注册selector
//        st.selector.wakeup();

        /*
        重点：  c有可能是 server  有可能是client
        ServerSocketChannel s = (ServerSocketChannel) c;
        呼应上， int nums = selector.select();  //阻塞  wakeup()
        try {

        如果先wakeup, 再register的话, 再线程run方法的执行中, 第一次select被wakeup叫醒, 第二次又马上进入阻塞,
        这时进行register, select的内容不会包含此时register的server, 即没有设置fd到listen状态

            s.register(st.selector, SelectionKey.OP_ACCEPT);  //会被阻塞的!!!!!
            st.selector.wakeup();  //功能是让 selector的select（）方法，立刻返回，不阻塞！

            System.out.println("aaaaa");
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }

         */
    }


    //无论 serversocket  socket  都复用这个方法
    // boss组专用
    private SelectorThread next() {
        int index = xid.incrementAndGet() % sts.length;  //轮询就会很尴尬，倾斜
        return sts[index];
    }

    private SelectorThread nextV2() {
        int index = xid.incrementAndGet() % (sts.length - 1);  //轮询就会很尴尬，倾斜
        return sts[index + 1];
    }

    // worker组专用
    private SelectorThread nextV3() {
        int index = xid.incrementAndGet() % stg.sts.length;  //动用worker的线程分配
        return stg.sts[index];
    }
}
