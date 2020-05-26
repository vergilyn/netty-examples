package com.vergilyn.examples.duplex;

import java.time.LocalTime;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import static com.vergilyn.examples.common.NettyConstants.INET_HOST;
import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Duplex;
import static com.vergilyn.examples.common.NettyConstants.newClientBootstrap;

/**
 * @author vergilyn
 * @date 2020-04-03
 */
public class DuplexNettyClient {

    public static void main(String[] args) {
        Bootstrap bootstrap = newClientBootstrap(1, Duplex);

        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                ch.pipeline()
                        .addLast("frame-decoder", new LineBasedFrameDecoder(100))  // inbound
                        .addLast("decoder", new StringDecoder())  // inbound
                        .addLast("encoder", new StringEncoder())  // outbound
                        .addLast(new SimpleChannelInboundHandler<String>() {  // inbound
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                System.out.print(msg);
                                // ctx.close();
                            }
                        });
            }
        });

        Channel channel = bootstrap.connect(INET_HOST, Duplex.inetPort).channel();

        String msg = String.format("client send: %s \r\n", LocalTime.now().toString());
        ChannelFuture future = channel.writeAndFlush(msg);
        System.out.print(msg);

        future.addListener((ChannelFutureListener) f
                -> System.out.printf("client future: %b, cause: %s \r\n", f.isSuccess(), f.cause()));
    }
}
