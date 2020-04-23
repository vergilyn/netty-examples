# 【999】Netty, FAQ.MD

## `unsupported message type: String (expected: ByteBuf, FileRegion) `
```java
public class SimplexNettyClient {
    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = newClientBootstrap(1, Simplex);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("frame-decoder", new LineBasedFrameDecoder(100))
                                .addLast("decoder", new StringDecoder())  // inbound
                                .addLast("encoder", new StringEncoder());  // outbound
                    }
                });

        Channel channel = bootstrap.connect(INET_HOST, Simplex.inetPort).channel();

        while (true) {
            String msg = String.format("client send: %s \r\n", LocalTime.now().toString());
            ChannelFuture future = channel.writeAndFlush(msg);
            System.out.println(msg);

            future.addListener((ChannelFutureListener) f
                    -> System.out.printf("client future: %b, cause: %s \r\n", f.isSuccess(), f.cause()));

            TimeUnit.SECONDS.sleep(10);
        }
    }
}
```

```java
public class SimplexNettyServer {
    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = newServerBootstrap(1, 1, Simplex);

        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {  // 3. 连接读写处理逻辑
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("frame-decoder", new LineBasedFrameDecoder(100))
                                .addLast("decoder", new StringDecoder())  // inbound
                                .addLast("encoder", new StringEncoder())  // outbound
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                        System.out.println(msg);

                                        ctx.channel().close();
                                    }
                                });
                    }
                })
                .bind(Simplex.inetPort);// 4. 绑定端口
    }
}
```

```text
client future: false, cause: java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion) 
```

- [Netty ChannelHandlerContext与Channel](https://www.jianshu.com/p/a8a0acfdc96c)：比较类似的问题，但原因不一样。

channel 建立连接后，第1次`channel.writeAndFlush()`会产生以上异常。但第2+次`writeAndFlush`均正常（同一个已建立connect的channel）。  

client 期望的调用栈：  
`tail`(inbound & outbound) -> `encoder`(inbound, ignore) -> `decoder`(outbound) -> `frame-decoer`(outbound) -> `head`(inbound & outbound)

猜测，client 第1次 未经过encoder。通过debug断点，结果又是正确的。 造成的可能原因：
1) handler 顺序错误，outbound是从tail往head查找，未找到encoder
2) handler 不存在于当前的 责任链中

connect建立连接后，适当sleep再发送消息，测试中并未再遇到该异常，所以排除猜测`1)`。
（debug 断点同理，也不会出现异常！）

扩展，针对猜测`1)`尝试过不同的 client发送消息的写法：
- `channel.writeAndFlush(msg)`: 从当前 channelPipeline 开始
- `channel.pipeline().writeAndFlush(msg)`: 确保从`tail.writeAndFlush(msg)`

2020-04-07 >>>>  
根据`StringDecoder`和`StringEncoder`可知（备注，decoder/encoder均会`ReferenceCountUtil.release(msg)`）：  
> `StringDecoder` javadoc  
> Decodes a received ByteBuf into a String. 
> **Please note that this decoder must be used with a proper `ByteToMessageDecoder`**
> **such as `DelimiterBasedFrameDecoder` or `LineBasedFrameDecoder`**
> **if you are using a stream-based transport such as TCP/IP.**
> A typical setup for a text-based line protocol in a TCP/IP socket would be:  
>  
>   ChannelPipeline pipeline = ...;  
>   // Decoders  
>   pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(80));  
>   pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));  
>   // Encoder  
>   pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));  
>  
> and then you can use a String instead of a ByteBuf as a message:  
>   void channelRead(ChannelHandlerContext ctx, String msg) {
>     ch.write("Did you say '" + msg + "'?\n");
>   }

在server&client中均增加`LineBasedFrameDecoder`（之前未增加该decoder），依然会出现该异常。

根据猜想，最大可能是：建立connect并马上write时，责任链中并不存在指定的handler，即只存在 head&tail。  
然而如果通过debug断点，无法验证，所以通过打印信息验证：  
```text
<12:00:51.775> sleep begin 0(SECONDS), bootstrap#connect() 
<12:00:51.776> sleep begin 0(SECONDS), channelFuture#channel() 
<12:00:51.775> client initChannel() before
<12:00:51.777> sleep begin 30(SECONDS), initChannel() before 

<12:00:51.787> writeAndFlush >>>> client send: 12:00:51.777 
<12:01:01.791> writeAndFlush >>>> client send: 12:01:01.791 
<12:01:11.793> writeAndFlush >>>> client send: 12:01:11.793 
<12:01:21.794> writeAndFlush >>>> client send: 12:01:21.794 
<12:01:21.804> client initChannel() after
<12:01:21.811> client future: false, cause: java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion) 
<12:01:21.814> client future: false, cause: java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion) 
<12:01:21.815> client future: false, cause: java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion) 
<12:01:21.815> client future: false, cause: java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion) 
```
由上打印日志可知，client 执行`channel.writeAndFlush(msg)` 是`initChannel()`完成之前。那么，是否是因此导致 第1次的责任链中其实只有 head&tail?
（备注，netty所有的操作都是async...）
但是，channel 是从 ChannelFuture中获取的啊！

猜测相关的源码：
```java
abstract class AbstractChannelHandlerContext implements ChannelHandlerContext, ResourceLeakHint {

   private void write(Object msg, boolean flush, ChannelPromise promise) {
        // 主要是 next 的获取，很可能是 next获取到的是 head，而不是 encoder
        final AbstractChannelHandlerContext next = findContextOutbound(flush ?
                    (MASK_WRITE | MASK_FLUSH) : MASK_WRITE);

            final Object m = pipeline.touch(msg, next);
            EventExecutor executor = next.executor();
            if (executor.inEventLoop()) {
                if (flush) {
                    next.invokeWriteAndFlush(m, promise);
                } else {
                    next.invokeWrite(m, promise);
                }
            }
    }
}
```
通过debug可知，异常的时候 `next = HeadContext`，正常的时候`next = StringEncoder`。

1. `initChannel()` 在什么时候调用？
2. `addLast()` 与 `write()` 的执行关系 并发or串行？

- [《Netty系列四》- Server端childHandler如何绑定到SocketChannel？](https://www.jianshu.com/p/b9162352073b)
- [《Netty系列六》- ChannelInitializer实现SocketChannel绑定ChannelHandler](https://www.jianshu.com/p/7ae37f4c1f14)

## channel 第2+次 速度远快于 第1次
use-case: 
dubbo provider 业务方法限制 sleep(1s)，consumer 调用 timeout(1s)。
最终其实可能调用成功，原因：1) dubbo 的timeout-check机制 2) 重试时节约了一部分client-elapsed用于server-elapsed。

```text
>>>> first request
FailoverClusterInvoker#doInvoke() before `invoker.invoke()` >>>> 15:19:11.382
HeaderExchangeChannel#request() before `newFuture` >>>> 15:19:11.385
HeaderExchangeChannel#request() after `newFuture` >>>> 15:19:11.389             4 ms, <=> start-time
NettyClientHandler#Override() before `super.write()` >>>> 15:19:11.395
NettyClientHandler#Override() after `super.write()` >>>> 15:19:11.468           73 ms, 主要时间差
NettyClientHandler#Override() before `promise.addListener()` >>>> 15:19:11.469
NettyClientHandler#sent() sent before >>>> 15:19:11.473
NettyClientHandler#sent() sent after >>>> 15:19:11.473

>>>> first retry
FailoverClusterInvoker#doInvoke() before `invoker.invoke()` >>>> 15:19:12.412
HeaderExchangeChannel#request() before `newFuture` >>>> 15:19:12.412
HeaderExchangeChannel#request() after `newFuture` >>>> 15:19:12.412             0 ms, <=> start-time
NettyClientHandler#Override() before `super.write()` >>>> 15:19:12.412
NettyClientHandler#Override() after `super.write()` >>>> 15:19:12.413           1 ms, 主要时间差
NettyClientHandler#Override() before `promise.addListener()` >>>> 15:19:12.413
NettyClientHandler#sent() sent before >>>> 15:19:12.413
NettyClientHandler#sent() sent after >>>> 15:19:12.413

>>>> first request
Caused by: org.apache.dubbo.remoting.TimeoutException: 
  Waiting server-side response timeout by scan timer. 
  start time: 2020-04-23 15:19:11.388, end time: 2020-04-23 15:19:12.410, sent time: 2020-04-23 15:19:11.473, 
  client elapsed: 85 ms, server elapsed: 937 ms, timeout: 1000 ms, 
  request: ...

>>>> first retry
Caused by: org.apache.dubbo.remoting.TimeoutException: 
  Waiting server-side response timeout by scan timer. 
  start time: 2020-04-23 15:19:12.412, end time: 2020-04-23 15:19:13.429, sent time: 2020-04-23 15:19:12.413, 
  client elapsed: 1 ms, server elapsed: 1016 ms, timeout: 1000 ms, 
  request: ...

```

分析可知，dubbo中重试机制的主要时间差是由 `channel.write()` 造成（72ms）。
为什么第2次这么快？

1. netty-connect 在第1次 write前就完成（3次握手完成）
2. 测试代码 `WriteNettyServer`、`WriteNettyClient`
```text
[01] write elapsed >>>> 20,205,200 ns 
16:17:10.772 [WriteNettyClient-1-1] DEBUG io.netty.buffer.AbstractByteBuf - -Dio.netty.buffer.checkAccessible: true
16:17:10.772 [WriteNettyClient-1-1] DEBUG io.netty.buffer.AbstractByteBuf - -Dio.netty.buffer.checkBounds: true
16:17:10.773 [WriteNettyClient-1-1] DEBUG io.netty.util.ResourceLeakDetectorFactory - Loaded default ResourceLeakDetector: io.netty.util.ResourceLeakDetector@2dc9113c
[02] write elapsed >>>> 340,700 ns 
[03] write elapsed >>>> 348,400 ns 
[04] write elapsed >>>> 337,400 ns 
[05] write elapsed >>>> 288,200 ns 
[06] write elapsed >>>> 298,900 ns 
[07] write elapsed >>>> 1,658,000 ns 
[08] write elapsed >>>> 418,500 ns 
[09] write elapsed >>>> 531,100 ns 
[10] write elapsed >>>> 460,100 ns 
[11] write elapsed >>>> 295,000 ns 
[12] write elapsed >>>> 689,200 ns 
[13] write elapsed >>>> 319,300 ns 
[14] write elapsed >>>> 302,000 ns 
[15] write elapsed >>>> 1,686,400 ns 
[16] write elapsed >>>> 270,700 ns 
[17] write elapsed >>>> 360,600 ns 
[18] write elapsed >>>> 292,400 ns 
[19] write elapsed >>>> 406,600 ns 
[20] write elapsed >>>> 253,400 ns 
```

第1次 write 的执行速度明显慢于之后的。