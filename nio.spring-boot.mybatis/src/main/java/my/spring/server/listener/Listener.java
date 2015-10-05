package my.spring.server.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import my.spring.server.configuration.Config;
import my.spring.server.threads.Job;
import my.spring.server.threads.progress.IProgressMonitor;
import my.spring.server.threads.status.IStatus;
import my.spring.server.threads.status.Status;

public class Listener extends Job {
    private static Logger log = LoggerFactory.getLogger(Listener.class);
    private static IStatus OK = new Status(Status.OK, "empty", "Ok");
    private static IStatus CANCEL = new Status(Status.CANCEL, "empty", "Cancel");

    public Listener(String name) {
	super(name);
	OK.setProcessName(name);
	CANCEL.setProcessName(name);
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
	return implementation() ? OK : CANCEL;
    }

    private boolean implementation() {
	boolean result = false;

	final SslContext sslCtx = null;

	EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	try {
	    ServerBootstrap b = new ServerBootstrap();
	    b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
		    .handler(new LoggingHandler(Config.getLogLevel()))
		    .childHandler(new FactorialServerInitializer(sslCtx));

	    b.bind(Config.getPort()).sync().channel().closeFuture().sync();
	} catch (InterruptedException e) {
	    log.error(e.getMessage(), e);
	} finally {
	    bossGroup.shutdownGracefully();
	    workerGroup.shutdownGracefully();
	    result = true;
	}

	return result;
    }

}
