# 【998】Netty, Important.MD


## https://netty.io/wiki/user-guide-for-4.x.html

### server, `option()`/`childOption()`
> https://netty.io/wiki/user-guide-for-4.x.html#writing-a-discard-server

```java
// netty-server
public class DiscardServer {
    public static void main(String[] args) throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // (3)
            .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new DiscardServerHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)          // (5)
             .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
    }
}
```

3. Here, we specify to use the NioServerSocketChannel class 
which is used to instantiate a new Channel to accept incoming connections.

4. The handler specified here will always be evaluated by a newly accepted Channel. 
(这里指定的处理程序将始终由新接受的通道计算。 意味着：client发起`channel.connect()`时`initChannel()`将被调用)
The ChannelInitializer is a special handler that is purposed to help a user configure a new Channel. 
It is most likely that you want to configure the ChannelPipeline of the new Channel by adding some handlers 
such as `DiscardServerHandler` to implement your network application. 
As the application gets complicated(复杂), 
it is likely that you will add more handlers to the pipeline 
and extract(提取) this anonymous(匿名) class into a top-level class eventually.

6. Did you notice option() and childOption()? 
`option()` is for the `NioServerSocketChannel` that accepts incoming connections. 
`childOption()` is for the Channels accepted by the parent ServerChannel, 
which is `NioServerSocketChannel` in this case.

> https://netty.io/wiki/user-guide-for-4.x.html#writing-a-time-client
```java
// netty-client
public class TimeClient {
    public static void main(String[] args) throws Exception {
        Bootstrap b = new Bootstrap(); // (1)
        b.group(workerGroup); // (2)
        b.channel(NioSocketChannel.class); // (3)
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new TimeClientHandler());
            }
        });
                    
        // Start the client.
        ChannelFuture f = b.connect(host, port).sync(); // (5)
    }
}
```

1. `Bootstrap` is similar to `ServerBootstrap` except that it's for non-server channels 
such as a client-side or connectionless channel.

2. If you specify only one `EventLoopGroup`, it will be used both as a boss group and as a worker group. 
**The boss worker is not used for the client side though.**

3. Instead of `NioServerSocketChannel`, `NioSocketChannel` is being used to create a client-side Channel.

4. Note that we do not use `childOption()` here unlike we did with ServerBootstrap 
**because the client-side SocketChannel does not have a parent.**

### release message, `ReferenceCountUtil.release(msg)`
> https://netty.io/wiki/user-guide-for-4.x.html#writing-a-discard-server

```
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    // Discard the received data silently.
    ((ByteBuf) msg).release(); // or `ReferenceCountUtil.release(msg)`
}
```
`ByteBuf` is a **reference-counted object(implements ReferenceCounted)** which has to be released explicitly via the `release()` method.   

Please keep in mind that it is the handler's responsibility to release any **reference-counted object** passed to the handler. 
Usually, `channelRead()` handler method is implemented like the following:  
```
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    try {
        // Do something with msg
    } finally {
        ReferenceCountUtil.release(msg);
    }
}
```

### netty 中所有的操作都是 async
> https://netty.io/wiki/user-guide-for-4.x.html#writing-a-time-server

the ChannelHandlerContext.write() (and writeAndFlush()) method returns a ChannelFuture. 
A ChannelFuture represents an I/O operation which has not yet occurred. 
It means, any requested operation might not have been performed yet because all operations are asynchronous in Netty. 
For example, **the following code might close the connection even before a message is sent:**  
```
Channel ch = ...;
ch.writeAndFlush(message);
ch.close();
```

Therefore, you need to call the `close()` method after the `ChannelFuture` is complete, which was returned by the `write()` method, 
and it notifies its listeners when the write operation has been done. 
Please note that, **`close()` also might not close the connection immediately, and it returns a ChannelFuture.**

How do we get notified when a write request is finished then?
```
final ChannelFuture f = ctx.writeAndFlush(msg);

// solution-01, close using future
f.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            assert f == future;
            ctx.close();
        }
    });

// solution-02, close using pre-defined listener:
f.addListener(ChannelFutureListener.CLOSE);
```