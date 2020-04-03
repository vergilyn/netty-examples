# 【999】Netty, FAQ.MD

## `unsupported message type: String (expected: ByteBuf, FileRegion) `
client/server 均添加了: `StringDecoder`, `StringEncoder` (MessageToMessageCodec)

- MessageToMessageDecoder, MessageToMessageEncoder
- ByteToMessageDecoder, MessageToByteEncoder

```text
// client
String msg = String.format("client send: %s", LocalTime.now().toString());
ChannelFuture future = channel.writeAndFlush(msg);

future.addListener((ChannelFutureListener) f
            -> System.out.printf("client future: %b, cause: %s \r\n", f.isSuccess(), f.cause()));

// output
client future: false, cause: java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion) 

```

2020-04-03 >>>>  
channel 建立连接后，首次 channel.writeAndFlush() 会产生以上异常。但再次 writeAndFlush 确正常，经过了`StringDecoder`。

感觉是 第1次 client 并未经过 encoder 导致，但为什么第2+又会通过？