package com.vergilyn.examples.handler.idle;

import java.time.LocalTime;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import static com.vergilyn.examples.common.NettyConstants.NettyThreadPrefix.Handler_IdleState;
import static com.vergilyn.examples.common.NettyConstants.newServerBootstrap;

/**
 *
 * @author vergilyn
 * @date 2020-05-28
 */
public class IdleNettyServer {

    public static void main(String[] args) throws InterruptedException {
        ServerBootstrap serverBootstrap = newServerBootstrap(1, 1, Handler_IdleState);

        int readerIdleTimeSeconds = 10;
        int writerIdleTimeSeconds = 0;
        int allIdleTimeSeconds = 0;
        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline()
                        .addLast("decoder", new StringDecoder())  // inbound
                        .addLast("encoder", new StringEncoder())  // outbound
                        .addLast("server-idle-handler", new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds))
                        .addLast("vergilyn-server-handler", new NettyServerHandler());
            }
        }).bind(Handler_IdleState.inetPort);
    }

    /**
     * see: dubbo
     *   - `org.apache.dubbo.remoting.transport.netty4.NettyServer#doOpen`
     *   - `org.apache.dubbo.remoting.transport.netty4.NettyServerHandler#userEventTriggered`
     */
    @ChannelHandler.Sharable
    private static class NettyServerHandler extends ChannelDuplexHandler {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.printf("server[%s] channelRead() >>>> %s \r\n", LocalTime.now().toString(), msg);
            // super.channelRead(ctx, msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            LocalTime now = LocalTime.now();
            if (evt instanceof IdleStateEvent) {
                String msg = String.format("[%s]server IdleStateEvent triggered, closed channel", now.toString());
                ctx.writeAndFlush(msg);

                ctx.close();
                System.out.println(msg);

            }else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }
}
