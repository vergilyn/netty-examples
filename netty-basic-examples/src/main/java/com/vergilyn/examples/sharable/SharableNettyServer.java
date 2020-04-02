package com.vergilyn.examples.sharable;

import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Sharable;
import static com.vergilyn.examples.common.NettyConstants.newServerBootstrap;

/**
 * @author vergilyn
 * @date 2020-04-01
 */
public class SharableNettyServer {
    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = newServerBootstrap(1, 8, Sharable);

        final ShareableHandler shareableHandler = new ShareableHandler();
        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline()
                                .addLast("decoder", new StringDecoder())
                                .addLast("sharable-handler", shareableHandler)
                                .addLast("no-sharable-handler", new NoShareableHandler());
                    }
                })
                .bind(Sharable.inetPort);// 4. 绑定端口
    }

    //@ChannelHandler.Sharable
    static class ShareableHandler extends ChannelInboundHandlerAdapter {
        private AtomicInteger index = new AtomicInteger(0);
        private String name;

        public ShareableHandler() {
            this.name = this.getClass().getSimpleName();
            System.out.println(name + " init...");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.printf("%s.channelRead() >>>> [%d]. %s \r\n", name, index.incrementAndGet(), msg);

            ctx.fireChannelRead(msg);
        }
    }

    static class NoShareableHandler extends ChannelInboundHandlerAdapter {
        private AtomicInteger index = new AtomicInteger(0);
        private String name;

        public NoShareableHandler() {
            this.name = this.getClass().getSimpleName();
            System.out.println(name + " init...");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.printf("%s.channelRead() >>>> [%d]. %s \r\n", name, index.incrementAndGet(), msg);

            ctx.fireChannelRead(msg);
        }
    }
}
