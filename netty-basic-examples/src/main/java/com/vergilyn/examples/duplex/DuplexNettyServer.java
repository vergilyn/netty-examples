package com.vergilyn.examples.duplex;

import java.time.LocalTime;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Duplex;
import static com.vergilyn.examples.common.NettyConstants.newServerBootstrap;

/**
 * @author vergilyn
 * @date 2020-04-03
 */
public class DuplexNettyServer {

    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = newServerBootstrap(1, 1, Duplex);

        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline()
                        .addLast("decoder", new StringDecoder())  // inbound
                        .addLast("encoder", new StringEncoder())  // outbound
                        .addLast("duplex", new SimpleChannelInboundHandler<String>() {

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                System.out.println("> active");
                            }

                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                System.out.println(msg);

                                String resp = "server response: " + LocalTime.now().toString();
                                ByteBuf byteBuf = ctx.alloc().buffer();
                                byteBuf.writeBytes(resp.getBytes());
                                ctx.channel().writeAndFlush(byteBuf);
                                System.out.println(resp);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                System.out.println("exceptionCaught >>>> ");
                                cause.printStackTrace();
                                // ctx.close();
                            }
                        });
            }
        }).bind(Duplex.inetPort);

    }
}
