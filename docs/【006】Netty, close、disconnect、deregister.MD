# 【006】Netty, close、disconnect、deregister.MD

- [netty源码解析(4.0)-13 Channel NIO实现: 关闭和清理](https://www.cnblogs.com/brandonli/p/10240998.html)

netty-client:  
- `channel.close()`
- `channel.disconnect()`
- `channel.deregister()`

netty-server:
- `ctx.close()`
- `ctx.disconnect()`
- `ctx.deregister()`
- `ctx.pipeline().close()`
- `ctx.pipeline().disconnect()`
- `ctx.pipeline().deregister()`

## disconnect
> 如果channel的状态从active变成inactive，就调用pipeline的fireChannelInactive方法触发channelInactive事件。

TCP和UDP的disconnect含义是不一样的。  

对TCP来说disconnect就是关闭socket；
UDP的disconnect是调用socket的disconnect方法（socket 的 disconnect又是什么） ？？？

对UDP来说，它没有连接的概念，默认情况下通过udp socket发送数据需要指定远程地址，
但如果调用connect之后，就不需指定这个地址，数据报会被发送到connect指定的地址上，
disconnect含义是删除connect指定的地址，发送数据时必须指定地址。

## close
> 1. 确保在多线程环境下，多次调用close和一次调用的影响一致，并且可以通过promis得到同样的结果。
> 2. 保证在执行close的过程中，不能向channel写数据。
> 3. 调用doClose0执行执真正的close操作。
> 4. 调用deregister对channel做最后的清理工作，并触发channelInactive, channelUnregistered事件。

## deregister 
> 从eventLoop中注销channel
> 主要是执行deregister操作，然后触发channelInactive事件(如果fireChannelInactive参数是true)和channelUnregistered事件。
> 
> 保证把当前正在eventLoop队列中所有任何都执行完之后再执行真正的deregister操作。