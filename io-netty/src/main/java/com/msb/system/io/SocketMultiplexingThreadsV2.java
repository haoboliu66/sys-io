package com.bjmashibing.system.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketMultiplexingThreadsV2 {

  public static void main(String[] args) throws IOException {

    EventLoopGroup boss = new EventLoopGroup(1);
    EventLoopGroup worker = new EventLoopGroup(3);
    ServerBootStrap b = new ServerBootStrap();
    b.group(boss, worker).bind(9090);

    System.in.read();
  }
}

class ServerBootStrap {
  private EventLoopGroup group;
  private EventLoopGroup childGroup;
  ServerAccepter accepter;

  public ServerBootStrap group(EventLoopGroup boss, EventLoopGroup worker) {
    group = boss;
    childGroup = worker;
    return this;
  }

  public void bind(int port) throws IOException {
    //bind 处理的是server的启动过程
    ServerSocketChannel server = ServerSocketChannel.open();
    server.configureBlocking(false);
    server.bind(new InetSocketAddress(port));
    accepter = new ServerAccepter(childGroup, server);
    EventLoop eventloop = group.chooser();
    //把启动server，bind端口的操作变成task，推送到eventloop中执行


    eventloop.execute(new Runnable() {
      @Override
      public void run() {
        eventloop.execute(new Runnable() {
          @Override
          public void run() {
            try {
              eventloop.name = Thread.currentThread() + eventloop.name;
              System.out.println("bind...server...to " + eventloop.name);
              server.register(eventloop.selector, SelectionKey.OP_ACCEPT, accepter);
            } catch (ClosedChannelException e) {
              e.printStackTrace();
            }
          }
        });
      }
    });
  }
}

class EventLoopGroup {
  AtomicInteger cid = new AtomicInteger(0);
  EventLoop[] children;

  EventLoopGroup(int nThreads) {
    children = new EventLoop[nThreads];
    for (int i = 0; i < nThreads; i++) {
      children[i] = new EventLoop("T" + i);
    }
  }

  public EventLoop chooser() {
    return children[cid.getAndIncrement() % children.length];
  }
}

interface Handler {
  void handleRead();
}

class ClientReader implements Handler {

  SocketChannel key;

  ClientReader(SocketChannel server) {
    this.key = server;
  }

  @Override
  public void handleRead() {
    ByteBuffer data = ByteBuffer.allocateDirect(4096);
    try {
      key.read(data);
      data.flip();
      byte[] dd = new byte[data.limit()];
      data.get(dd);
      System.out.println(new String(dd));
      data.clear();
      for (int i = 0; i < 10; i++) {
        data.put("a".getBytes());
        data.flip();
        key.write(data);
        data.clear();

      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
}

class ServerAccepter implements Handler {
  ServerSocketChannel key;
  EventLoopGroup cGroup;

  ServerAccepter(EventLoopGroup cGroup, ServerSocketChannel server) {
    this.key = server;
    this.cGroup = cGroup;
  }

  public void handleRead() {
    try {
      final EventLoop eventLoop = cGroup.chooser();
      final SocketChannel client = key.accept();
      client.configureBlocking(false);
      client.setOption(StandardSocketOptions.TCP_NODELAY, true);
      final ClientReader cHandler = new ClientReader(client);

      final Runnable task = () -> {
        try {
          System.out.println("socket...send...to " + eventLoop.name + " client port : " + client.socket().getPort());

          client.register(eventLoop.selector, SelectionKey.OP_READ, cHandler);
        } catch (final IOException e) {
          e.printStackTrace();
        }
      };

      eventLoop.execute(task);

    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
}

class EventLoop implements Executor {

  Selector selector;
  Thread thread = null;
  BlockingQueue events = new LinkedBlockingQueue();
  int NOT_STARTED = 1;
  int STARTED = 2;
  AtomicInteger STAT = new AtomicInteger(1);
  String name;

  public EventLoop(String name) {
    this.name = name;
    try {
      selector = Selector.open();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //Loop 需要一个死循环  这个死循环在哪里运行呢？当然在一个线程里，那，那个线程怎么出现的呢？？？当然是execute创建出来的。
  public void run() throws InterruptedException, IOException {
    System.out.println("server已经开始：");

    for (; ; ) {
      //select
      int nums = selector.select();
      //selectedkeys to events
      if (nums > 0) {
        Set<SelectionKey> keys = selector.selectedKeys();  //会一直阻塞，不过可以通过外界有task到达来wakeup唤醒
        Iterator<SelectionKey> iter = keys.iterator();
        while (iter.hasNext()) {
          SelectionKey key = iter.next();
          iter.remove();
          Handler handler = (Handler) key.attachment();
          if (handler instanceof ServerAccepter) {
          } else if (handler instanceof ClientReader) {
          }
          handler.handleRead();
        }
      }
      //run events
      runTask();
    }
  }

  //线程池需要持有线程和消息队列
  @Override
  public void execute(Runnable task) {
    try {
      events.put(task);
      this.selector.wakeup();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    if (!inEventLoop() && STAT.incrementAndGet() == STARTED) {

      new Thread(() -> {
        try {
          thread = Thread.currentThread();
          EventLoop.this.run();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();
    }
  }

  public void runTask() throws InterruptedException {
    for (int i = 0; i < 5; i++) {
      Runnable task = (Runnable) events.poll(10, TimeUnit.MILLISECONDS);
      if (task != null) {
        events.remove(task);
        task.run();
      }
    }
  }

  private boolean inEventLoop() {
    return thread == Thread.currentThread();
  }
}