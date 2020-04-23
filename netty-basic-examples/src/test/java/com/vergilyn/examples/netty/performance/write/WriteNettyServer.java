package com.vergilyn.examples.netty.performance.write;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Write;
import static com.vergilyn.examples.common.NettyConstants.newServerBootstrap;
import static com.vergilyn.examples.common.NettyConstants.println;

/**
 * @author vergilyn
 * @date 2020-04-23
 */
public class WriteNettyServer {

    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = newServerBootstrap(1, 1, Write);

        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {  // 3. 连接读写处理逻辑
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                        .addLast("frame-decoder", new LineBasedFrameDecoder(100))
                        .addLast("decoder", new StringDecoder())  // inbound
                        .addLast("encoder", new StringEncoder())  // outbound
                        .addLast(new SimpleChannelInboundHandler<String>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, String msg) {  // inbound
                                println(msg);

                                ctx.fireChannelRead(msg);
                            }
                        });
            }
        })
                .bind(Write.inetPort);// 4. 绑定端口
    }
}
