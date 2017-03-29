
package com.iih5.netbox;

import com.iih5.actor.ActorManager;
import com.iih5.actor.util.ThreadFactoryUtil;
import com.iih5.netbox.annotation.InOut;
import com.iih5.netbox.annotation.Protocol;
import com.iih5.netbox.annotation.Request;
import com.iih5.netbox.core.*;
import com.iih5.netbox.session.SessionManager;
import com.iih5.netbox.tcp.TcpServerInitializer;
import com.iih5.netbox.util.ClassUtil;
import com.iih5.netbox.websocket.WebSocketServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public class NetBoxEngine {
	private static Logger logger = LoggerFactory.getLogger(NetBoxEngine.class);
	public static ConnectExtension extension=null;
	private NetBoxEngineSetting settings=null;
	public NetBoxEngine() {
		this.settings =new NetBoxEngineSetting();
	}
	public NetBoxEngineSetting getSettings() {
		return settings;
	}
	public void setSettings(NetBoxEngineSetting settings) {
		this.settings = settings;
	}
	public NetBoxEngine(NetBoxEngineSetting settings) {
		this.settings=settings;
	}
	/**启动网络服务*/
	public void start() {
		if (settings.getPlayerThreadSize()>0){
			SessionManager.getInstance().setActorManager(new ActorManager(settings.getPlayerThreadSize(), ThreadFactoryUtil.createThreadFactory("User-Pool-")));
		}else {
			logger.error("用户管理线程数量不能小于1个!");
			throw new UnsupportedOperationException("用户管理线程数量不能小于1个!");
		}
		EventLoopGroup bossGroup   = new NioEventLoopGroup(settings.getBossThreadSize());
		EventLoopGroup workerGroup = new NioEventLoopGroup(settings.getWorkerThreadSize());
		try {
			ServerBootstrap b = new ServerBootstrap();
			if (settings.getBasePackage()==null || settings.getBasePackage().equals("")) {
				logger.error("请设置协议映射扫描目录，比如 com.ab!");
				throw new UnsupportedOperationException("请设置协议映射扫描目录，比如 com.ab");
			}else {
				protocolMapping();
					if (ProtocolConstant.transformType==TransformType.WS_BINARY
						||ProtocolConstant.transformType==TransformType.WS_TEXT){
					b.group(bossGroup, workerGroup)
							.channel(NioServerSocketChannel.class)
							.childHandler(new WebSocketServerInitializer());
					b.bind(settings.getPort()).sync();
					logger.info("WebSocket port="+settings.getPort()+" Start Success !");
					logger.info("Open your web browser and navigate to http://127.0.0.1:" + settings.getPort() + '/');
				}else {
						b.group(bossGroup, workerGroup)
							.option(ChannelOption.TCP_NODELAY, true)
							.option(ChannelOption.SO_KEEPALIVE, true)
							.channel(NioServerSocketChannel.class)
							.childHandler(new TcpServerInitializer());
					ChannelFuture f = b.bind(settings.getPort()).sync();
					logger.info("TCP port="+settings.getPort()+" Start Success !");
				}
			}
		}catch (InterruptedException e) {
			logger.error("服务器关闭 shutdown！");
			workerGroup.shutdownGracefully();

			bossGroup.shutdownGracefully();
		}
	}
	//映射扫描
	private void protocolMapping(){
		List<Class<?>> allClass = ClassUtil.getClasses(settings.getBasePackage());
		for (Class<?> class1 : allClass) {
			if (class1.isAnnotationPresent(Request.class)) {
				Method[] mds= class1.getDeclaredMethods();
				for (Method method : mds) {
					if (method.isAnnotationPresent(Protocol.class)) {
						Annotation[] ans= method.getAnnotations();
						for (Annotation annotation : ans) {
							Protocol methn = (Protocol) annotation;
							try {
								CmdHandlerCache.getInstance().putCmdHandler(methn.value(), new AnnObject(class1, method));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}else if (class1.isAnnotationPresent(InOut.class)) {
				if(extension!=null){
					throw new IllegalArgumentException("重复加载Extension( extension = "+class1.getName()+")");
				}else {
					try {
						Object obj = class1.newInstance();
						extension=(ConnectExtension)obj;
					} catch (InstantiationException e) {
						logger.error("加载Extension 出错：",e);
					} catch (IllegalAccessException e) {
						logger.error("加载Extension 出错：",e);
					}
				}
			}
		}
	}
	
}
