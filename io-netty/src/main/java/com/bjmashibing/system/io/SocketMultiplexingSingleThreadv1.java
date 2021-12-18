package com.bjmashibing.system.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SocketMultiplexingSingleThreadv1 {

  private ServerSocketChannel server = null;
  private Selector selector = null;   //linux 多路复用器（select poll    epoll kqueue） nginx  event{}
  private final int port = 9090;

  public void initServer() {
    try {
      server = ServerSocketChannel.open();   // open serverSocket
      server.configureBlocking(false);
      server.bind(new InetSocketAddress(port)); // bind & listen

      //如果在epoll模型下，open-->  epoll_create -> fd3 (epoll_instance)
      selector = Selector.open();  //  select  poll  *epoll  优先选择：epoll  但是可以 -D修正
      // 在poll的模型下, 与内核没有交互

      //server 约等于 listen状态的 fd4
            /*
            register
            如果：
            在select，poll模型下：jvm里开辟一个数组 fd4 放进去, 没有系统调用
            epoll：  epoll_ctl(fd3,ADD,fd4,EPOLLIN)

            懒加载:
            其实在触碰到selector.select()调用的时候才触发了epoll_ctl的调用
             */
      server.register(selector, SelectionKey.OP_ACCEPT);

    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  public void start() {
    initServer();
    System.out.println("服务器启动了。。。。。");
    try {
      while (true) {

        Set<SelectionKey> keys = selector.keys();
        System.out.println(keys.size() + "   size");

        //1,调用多路复用器(select, poll  or  epoll  (epoll_wait))
                /*
                select()方法的意思:
                1，select, poll:  通过syscall调用的是 内核的select（fd4）  poll(fd4)
                2，epoll： syscall调用的是 内核的 epoll_wait(), 直接取ready的fd结果集
                *, 参数可以带时间：没有时间, 默认是0：  阻塞; 有时间, 设置一个超时
                selector.wakeup()  结果返回0

                懒加载:
                其实在selector.select()调用的时候, 触发了epoll_ctl + epoll_wait的调用
                 */
        while (selector.select() > 0) {  // epoll_wait
          Set<SelectionKey> selectionKeys = selector.selectedKeys();  //返回的有状态的fd集合
          Iterator<SelectionKey> iter = selectionKeys.iterator();  // 迭代处理有状态的结果集
          //so，管你啥多路复用器，你呀只能提供fd状态，我还得一个一个的去处理他们的R/W。同步好辛苦！！！！
          //  NIO  自己对着每一个fd调用系统调用，浪费资源，那么你看，这里是不是调用了一次select方法，知道具体的那些可以R/W了？

          //我前边可以强调过，socket：  listen   通信 R/W
          while (iter.hasNext()) {
            SelectionKey key = iter.next();
            iter.remove(); //set  不移除会重复循环处理
            if (key.isAcceptable()) {
              //看代码的时候，这里是重点，如果要去接受一个新的连接
              //语义上，accept接受连接且返回新连接的fd对吧？
              //那新的fd怎么办？
              /* select，poll，因为他们内核没有开辟额外空间，那么在jvm中保存和前边的fd4那个listen的一起
                 epoll： 我们希望通过epoll_ctl把新的客户端fd注册到内核空间 */
              acceptHandler(key);
            } else if (key.isReadable()) {
              readHandler(key);  // 连read 还有 write都处理了
              //在当前线程，处理读的这个方法可能会阻塞, 如果读的程序很复杂, 阻塞了十年，其他的IO早就没电了。。。
              //所以，为什么提出了 IO THREADS
              //redis  是不是用了epoll，redis是不是有个io threads的概念 ，redis是不是单线程的
              //tomcat 8,9  异步的处理方式  <IO>  和   <读写的处理上>  解耦
            }
          }
        }
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  /*
   register the new socket/key onto epoll instance(i.e selector) in the kernel
   */
  public void acceptHandler(final SelectionKey key) {
    try {
      ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
      SocketChannel client = ssc.accept(); //来啦，目的是调用accept接受客户端  fd7
      client.configureBlocking(false);

      ByteBuffer buffer = ByteBuffer.allocate(8192);  //前边讲过了
      //重点:
      //你看，调用了register
            /*
            select/poll: JVM里开辟一个数组 fd7 放进去
            epoll:  epoll_ctl(fd3,ADD,fd7,EPOLLIN, 关注的是read事件

            accept()接收后返回一个fd是表示一个客户端的socket连接,
            调用该client的register(), 在epoll模型下是把这个socket对应的fd通过epoll_ctl放(register)到了红黑树中,
            并关注了该fd的READ事件
             */
      client.register(selector, SelectionKey.OP_READ, buffer);
      System.out.println("-------------------------------------------");
      System.out.println("新客户端：" + client.getRemoteAddress());
      System.out.println("-------------------------------------------");
            /*
            此处系统调用中可以发现, 先发生了write调用, 再发生了epoll_ctl
            ==> 也是类似懒加载, 再外层重新执行selector.select()的时候,
                才真正调用 epoll_ctl 把这个client的socket对应的fd放到了红黑树中

            fcntl(10, F_SETFL, O_RDWR|O_NONBLOCK)   = 0
            write(1, "--------------------------------"..., 43) = 43
            write(1, "\n", 1)                       = 1
            write(1, "\346\226\260\345\256\242\346\210\267\347\253\257\357\274\232/0:0:0:0:0:0:0:1:"..., 37) = 37
            write(1, "\n", 1)                       = 1
            write(1, "--------------------------------"..., 43) = 43
            write(1, "\n", 1)                       = 1
            epoll_ctl(9, EPOLL_CTL_ADD, 10, {EPOLLIN, {u32=10, u64=140685948747786}}) = 0
            epoll_wait(9, [], 4096, 50)             = 0
            epoll_wait(9, [], 4096, 50)             = 0
            epoll_wait(9, [], 4096, 50)             = 0
             */

    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  public void readHandler(final SelectionKey key) {
    SocketChannel client = (SocketChannel) key.channel();
    ByteBuffer buffer = (ByteBuffer) key.attachment();
    buffer.clear();
    int read;
    try {
      while (true) {
        read = client.read(buffer);
        if (read > 0) {
          buffer.flip();
          while (buffer.hasRemaining()) {
            client.write(buffer);
          }
          buffer.clear();
        } else if (read == 0) {
          break;
        } else {
          client.close();
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    final SocketMultiplexingSingleThreadv1 service = new SocketMultiplexingSingleThreadv1();
    service.start();
  }
}
