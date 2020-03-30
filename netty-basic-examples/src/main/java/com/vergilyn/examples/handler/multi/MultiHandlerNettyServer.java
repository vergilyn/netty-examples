package com.vergilyn.examples.handler.multi;

import com.vergilyn.examples.common.NettyEventLoopFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author vergilyn
 * @date 2020-03-23
 */
@Slf4j
public class MultiHandlerNettyServer {
    public static final int INET_PORT = 8081;
    public static final String INET_HOST = "127.0.0.1";

    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup boss = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss"));
        NioEventLoopGroup worker = new NioEventLoopGroup(NettyEventLoopFactory.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyServerWork"));
        ChannelFuture channelFuture = serverBootstrap.group(boss, worker)    // 1. 线程模型
                .channel(NioServerSocketChannel.class)  // 2. IO模型
                .childHandler(new ChannelInitializer<NioSocketChannel>() {  // 3. 连接读写处理逻辑
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline()
                                /* 调用顺序，特别 index:7 只打印了1次！
                                    inbound >>>> index: 1, invoke-method: channelRead
                                    inbound >>>> index: 2, invoke-method: channelRead
                                    inbound >>>> index: 3, invoke-method: channelRead
                                    inbound & outbound >>>> index: 7, invoke-method: channelRead
                                    outbound >>>> index: 6, invoke-method: write
                                    outbound >>>> index: 5, invoke-method: write
                                    outbound >>>> index: 4, invoke-method: write
                                 */

                                // inbound 正序调用
                                .addLast(new StringDecoder())
                                .addLast(new CustomInboundHandler1())
                                .addLast(new CustomInboundHandler2())
                                .addLast(new CustomInboundHandler3())

                                // outbound 倒序调用，且必须定义在最后一个Inbound之前
                                .addLast(new CustomOutboundHandler4())
                                .addLast(new CustomOutboundHandler5())
                                .addLast(new CustomOutboundHandler6())

                                /* inbound & outbound
                                 * outbound 必须定义在最后一个 inbound前，所以此处只会用到 inbound。（待求证！！！）
                                 */
                                .addLast(new CustomInAndOutboundHandler7());
                    }
                })
                .bind(INET_PORT);// 4. 绑定端口
    }

    static interface IndexPrint {
        String TYPE_INBOUND = "inbound";
        String TYPE_OUTBOUND = "outbound";
        String TYPE_IN_OUT_BOUND = "inbound & outbound";

        String[] getIndexAndType();

        default void print(Object msg){
            String[] indexAndType = getIndexAndType();
            log.info("{} >>>> index: {}, invoke-method: {}", indexAndType[1], indexAndType[0]
                    , Thread.currentThread().getStackTrace()[2].getMethodName());
        }
    }

    @Slf4j
    static class CustomInboundHandler1 extends ChannelInboundHandlerAdapter implements IndexPrint  {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            print(msg);
            ctx.fireChannelRead(msg);  // 传递给下一个 InboundHandler
        }

        @Override
        public String[] getIndexAndType() {
            return new String[]{"1", TYPE_INBOUND};
        }
    }

    @Slf4j
    static class CustomInboundHandler2 extends ChannelInboundHandlerAdapter implements IndexPrint {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            print(msg);

            ctx.fireChannelRead(msg);  // 传递给下一个 InboundHandler
        }

        @Override
        public String[] getIndexAndType() {
            return new String[]{"2", TYPE_INBOUND};
        }
    }

    @Slf4j
    static class CustomInboundHandler3 extends ChannelInboundHandlerAdapter implements IndexPrint {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            print(msg);
            ctx.fireChannelRead(msg);
            // ctx.writeAndFlush(msg);
        }

        @Override
        public String[] getIndexAndType() {
            return new String[]{"3", TYPE_INBOUND};
        }
    }

    @Slf4j
    static class CustomOutboundHandler4 extends ChannelOutboundHandlerAdapter implements IndexPrint {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            print(msg);

            ctx.write(msg, promise);
            ctx.flush();
        }

        @Override
        public String[] getIndexAndType() {
            return new String[]{"4", TYPE_OUTBOUND};
        }
    }

    @Slf4j
    static class CustomOutboundHandler5 extends ChannelOutboundHandlerAdapter implements IndexPrint {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            print(msg);
            ctx.writeAndFlush(msg, promise);
        }

        @Override
        public String[] getIndexAndType() {
            return new String[]{"5", TYPE_OUTBOUND};
        }
    }

    @Slf4j
    static class CustomOutboundHandler6 extends ChannelOutboundHandlerAdapter implements IndexPrint {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            print(msg);
            ctx.writeAndFlush(msg, promise);
        }

        @Override
        public String[] getIndexAndType() {
            return new String[]{"6", TYPE_OUTBOUND};
        }
    }

    @Slf4j
    static class CustomInAndOutboundHandler7 extends ChannelOutboundHandlerAdapter implements ChannelInboundHandler, IndexPrint {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            print(msg);

            ctx.writeAndFlush(msg, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            print(msg);

            /**
             * Request to write a message via this {@link ChannelHandlerContext} through the {@link ChannelPipeline}.
             * This method will not request to actual flush, so be sure to call {@link #flush()}
             * once you want to request to flush all pending data to the actual transport.
             */
            ctx.write(msg);  // 传递给下一个 OutboundHandler。当前class属于 In&Out，并不会传递到当前class的Outbound
            ctx.flush();
        }

        @Override
        public String[] getIndexAndType() {
            return new String[]{"7", TYPE_IN_OUT_BOUND};
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelRegistered();
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelUnregistered();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelInactive();
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelReadComplete();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelWritabilityChanged();
        }
    }
}
