# 【003】Netty, 粘包 & 拆包.MD

- [Netty中粘包/拆包处理](https://mp.weixin.qq.com/s/oeSWUfSgcHGv3xpegCFOSw)
- [Netty通信中的粘包、拆包？](https://mp.weixin.qq.com/s/A7CkBAN3fX6z2x60ZATgHA)

粘包：sticky-packet, merged-packet    
拆包：unpack, split-packet  

## 粘包/拆包描述
假设当前有`123`和`abc`两个数据包，那么他们传输情况示意图如下：  
![netty-packet.png](./images/netty-packet.png)

- I 为正常情况，两次传输两个独立完整的包。
- II 为粘包情况，`123` 和 `abc` 封装成了一个包。
- III 为拆包情况，图中的描述是将`123`拆分成了`1`和`23`，并且`1`和`abc`一起传输。
`123`和`abc`也可能是`abc`进行拆包。甚至`123`和`abc`进行多次拆分也有可能。

## 解决方案
**明确消息结束的标志（消息定长，设置消息边界）。**  
不管是，根据 分隔符 还是 长度，其目的都是 明确一条完整消息 的边界。

- io.netty.handler.codec.LineBasedFrameDecoder: "\n" or "\r\n"
- io.netty.handler.codec.DelimiterBasedFrameDecoder: 自定义分隔符
- io.netty.handler.codec.FixedLengthFrameDecoder: 固定长度
- io.netty.handler.codec.LengthFieldBasedFrameDecoder: 动态长度，每条消息的长度都是随着消息头进行指定

均继承`ByteToMessageDecoder extends ChannelInboundHandlerAdapter`。


## 那么UDP是否会发生粘包或拆包的现象呢？
**答案是不会。**  

UDP是基于报文发送的，从UDP的帧结构可以看出，**在UDP首部采用了16bit来指示UDP数据报文的长度**，
因此在应用层能很好的将不同的数据报文区分开，从而避免粘包和拆包的问题。

而TCP是基于字节流的，虽然应用层和TCP传输层之间的数据交互是大小不等的数据块，
但是TCP把这些数据块仅仅看成一连串无结构的字节流，没有边界；  
另外从TCP的帧结构也可以看出，**在TCP的首部没有表示数据长度的字段**，
基于上面两点，在使用TCP传输数据时，才有粘包或者拆包现象发生的可能。