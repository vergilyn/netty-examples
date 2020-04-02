# 【004】Netty, @Sharable.MD

```java
/**
 * Indicates that the same instance of the annotated {@link ChannelHandler}
 * can be added to one or more {@link ChannelPipeline}s multiple times
 * without a race condition.
 * （标识同一个ChannelHandler的实例可以被多次添加到多个ChannelPipelines中，而且不会出现竞争条件。）
 * <p>
 * If this annotation is not specified, you have to create a new handler
 * instance every time you add it to a pipeline because it has unshared
 * state such as member variables.
 * （如果一个ChannelHandler没有标志@Shareable，在添加到到一个pipeline中时，
 *  你需要每次都创建一个新的handler实例，因为它的成员变量是不可分享的。）
 * <p>
 * This annotation is provided for documentation purpose, just like
 * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
 * （这个注解仅作为文档参考使用，比如说JCIP注解。）
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface Sharable {
    // no value
}
```

```text
15:08:40.390 [ShareableNettyServerWork-3-3] WARN io.netty.channel.ChannelInitializer - 
    Failed to initialize a channel. Closing: [id: 0x3b9c03c9, L:/127.0.0.1:18082 - R:/127.0.0.1:50603]
io.netty.channel.ChannelPipelineException: com.vergilyn.examples.shareable.ShareableNettyServer$ShareableHandler 
    is not a @Sharable handler, so can't be added or removed multiple times.
```

异常原因，`ShareableHandler` 未标记为 `@Sharable`:  
```
PacketChannelHandler packetChannelHandler = new PacketChannelHandler();

serverBootstrap.group(boss, worker)
   .childHandler(new ChannelInitializer<NioSocketChannel>() {
       @Override
       protected void initChannel(NioSocketChannel ch) {
           ch.pipeline().addLast(shareableHandler);
       }
   });

//@ChannelHandler.Sharable
class ShareableHandler extends ChannelInboundHandlerAdapter {
    // 省略...
}
```

以上写法才能 share ChannelHandler，即`ShareableHandler`只会实例化1次。

```
// 即使`NoShareableHandler`标记为`@Shareable`，也会是 每个ChannelPipeline 多次实例化。
serverBootstrap.group(boss, worker)
   .childHandler(new ChannelInitializer<NioSocketChannel>() {
       @Override
       protected void initChannel(NioSocketChannel ch) {
           ch.pipeline().addLast(new NoShareableHandler());
       }
   });
``` 