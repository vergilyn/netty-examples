# 【005】Netty, 事件传播.MD
事件传播（event propagation）

- [netty源码分析之pipeline(二)](https://www.jianshu.com/p/087b7e9a27a2)
- [netty学习系列三：pipeline原理与事件处理](https://www.jianshu.com/p/607501fdeb92)

```
#### client
client connected: 10:44:26.194
client send: [0][10:44:28.201]
client disconnect: 10:44:38.217

#### server init / ChannelPipeline create (client connect)
> inbound-01, handlerAdded(), 10:44:26.302 
> outbound-01, handlerAdded(), 10:44:26.306 
> outbound-02, handlerAdded(), 10:44:26.306 
> inbound-02, handlerAdded(), 10:44:26.307 

#### client channel.connect()
> inbound-01, channelRegistered(), 10:44:26.307 
> inbound-01, channelActive(), 10:44:26.308 

> outbound-02, read(), 10:44:26.308 
> outbound-01, read(), 10:44:26.308 

#### client send msg
> inbound-01, channelRead(), 10:44:28.268 
receive message: [0][10:44:28.201]
> inbound-01, channelReadComplete(), 10:44:28.270 
> outbound-02, read(), 10:44:28.271 
> outbound-01, read(), 10:44:28.271 

```

client 调用`channel.connect(host, port)` 会依次触发 server 的`channelRegistered() -> channelActive()`。

client 调用`channel.writeAndFlush(msg)`时，会触发 server-head进而调用`pipeline.fireChannelRead(byteBuf);`， 
从而引发 从head节点开始传播至整个pipeline的读事件传播。

server-head 在依次数据读完，channelReadComplete方法首先被调用，TA要做的事情除了将事件继续传播下去之外，
还得继续向reactor线程注册读事件，即调用readIfIsAutoRead()。

Channel都是`默认开启自动读取模式`，即只要Channel是active的，读完一波数据之后就继续向selector注册读事件，
这样就可以连续不断得读取数据，最终，通过pipeline，还是传递到head节点

三次握手成功之后，pipeline.fireChannelActive();被调用，然后以head节点为参数，直接一个静态调用


## outbound 中的 read

+ [In Netty4,why read and write both in OutboundHandler](https://stackoverflow.com/questions/22354135/in-netty4-why-read-and-write-both-in-outboundhandler)
- [为什么ChannelOutboundHandler会声明一个read方法？](https://my.oschina.net/joymufeng/blog/782113)

> Inbound handlers are supposed to handle inbound events. Events are triggered by external stimuli such as data received from a socket. 
> Outbound handlers are supposed to intercept the operations issued by your application.
> 
> 所以ChannelOutboundHandler上的read方法，如其注释所述，是为了拦截ChannelHandlerContext.read()操作。
> 也就是说，ChannelOutboundHandler可以通过read()方法 **在必要的时候阻止向inbound读取更多数据的操作。**这个设计在处理协议的握手时非常有用。



## 事件传播
**默认handler-name生成规则: 简单类名#0~n**

`ctx.pipeline().addLast(handler)`

1. 当 add 成功后， call ChannelHandler#handlerAdded()
   若 add exception/error，call ChannelHandler#handlerRemoved()
   
---

client, `channel.connect(host, port)`

