package nio.springserver.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import nio.springserver.configuration.Config;
import nio.springserver.mthreads.Job;
import nio.springserver.mthreads.progress.IProgressMonitor;
import nio.springserver.mthreads.status.IStatus;
import nio.springserver.mthreads.status.Status;

public class Server extends Job {
	private static Logger log = LoggerFactory.getLogger(Server.class);
	private static IStatus OK = new Status(Status.OK, null, "Ok");
	private static IStatus CANCEL = new Status(Status.CANCEL, null, "Cancel");
	private Config configuration = null;
	public Server(String name, Config config) {
		super(name);
		OK.setProcessName(name);
		CANCEL.setProcessName(name);
		setConfiguration(config);
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
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(
							getConfiguration().getLogLevel()))
					.childHandler(new FactorialServerInitializer(sslCtx));

			b.bind(getConfiguration().getPort()).sync().channel().closeFuture()
					.sync();
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			result = true;
		}

		return result;
	}

	public Config getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Config configuration) {
		this.configuration = configuration;
	}
}
