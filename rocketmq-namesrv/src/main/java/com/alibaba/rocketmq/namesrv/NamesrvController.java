/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.alibaba.rocketmq.namesrv;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.alibaba.rocketmq.common.ThreadFactoryImpl;
import com.alibaba.rocketmq.common.namesrv.NamesrvConfig;
import com.alibaba.rocketmq.namesrv.kvconfig.KVConfigManager;
import com.alibaba.rocketmq.namesrv.processor.ClusterTestRequestProcessor;
import com.alibaba.rocketmq.namesrv.processor.DefaultRequestProcessor;
import com.alibaba.rocketmq.namesrv.routeinfo.BrokerHousekeepingService;
import com.alibaba.rocketmq.namesrv.routeinfo.RouteInfoManager;
import com.alibaba.rocketmq.remoting.RemotingServer;
import com.alibaba.rocketmq.remoting.netty.NettyRemotingServer;
import com.alibaba.rocketmq.remoting.netty.NettyRequestProcessor;
import com.alibaba.rocketmq.remoting.netty.NettyServerConfig;

/**
 * @author shijia.wxr
 */
public class NamesrvController {

	private final NamesrvConfig namesrvConfig;
	private final NettyServerConfig nettyServerConfig;
	/** 初始化为NettyRemotingServer实例，详见{@link #initialize()} */
	private RemotingServer remotingServer;
	private BrokerHousekeepingService brokerHousekeepingService;
	private ExecutorService remotingExecutor;

	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("NSScheduledThread"));
	private final KVConfigManager kvConfigManager;
	private final RouteInfoManager routeInfoManager;

	public NamesrvController(NamesrvConfig namesrvConfig, NettyServerConfig nettyServerConfig) {
		// nameserv参数配置
		this.namesrvConfig 			   = namesrvConfig;
		// netty的参数配置
		this.nettyServerConfig 		   = nettyServerConfig;
		this.kvConfigManager 		   = new KVConfigManager(this);
		// 初始化RouteInfoManager
		this.routeInfoManager 		   = new RouteInfoManager();
		// 监听客户端连接(Channel)的变化，通知RouteInfoManager检查broker是否有变化
		this.brokerHousekeepingService = new BrokerHousekeepingService(this);
	}

	public boolean initialize() {
		/** 1、初始化KVConfigManager */
		this.kvConfigManager.load();

	    /** 2、初始化netty server */
		this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.brokerHousekeepingService);

		/** 3、客户端请求处理的线程池 */
		int serverWorkerThreadCount = nettyServerConfig.getServerWorkerThreads();
		this.remotingExecutor = Executors.newFixedThreadPool(serverWorkerThreadCount, new ThreadFactoryImpl("RemotingExecutorThread_"));

		/** 4、注册DefaultRequestProcessor，所有的客户端请求都会转给这个Processor来处理 */
		this.registerProcessor();

		/** 5、启动定时调度，每10秒钟扫描所有Broker，检查存活状态 */
		this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				NamesrvController.this.routeInfoManager.scanNotActiveBroker();
			}
		}, 5, 10, TimeUnit.SECONDS);

	    /** 6、日志打印的调度器，定时打印kvConfigManager的内容 */
		this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				NamesrvController.this.kvConfigManager.printAllPeriodically();
			}
		}, 1, 10, TimeUnit.MINUTES);

		return true;
	}

	private void registerProcessor() {
		if (namesrvConfig.isClusterTest()) {// 默认: clusterTest=false
			NettyRequestProcessor processor = new ClusterTestRequestProcessor(this, namesrvConfig.getProductEnvName());
			this.remotingServer.registerDefaultProcessor(processor, this.remotingExecutor);
		} else {
			this.remotingServer.registerDefaultProcessor(new DefaultRequestProcessor(this), this.remotingExecutor);
		}
	}

	public void start() throws Exception {
		this.remotingServer.start();
	}

	public void shutdown() {
		this.remotingServer.shutdown();
		this.remotingExecutor.shutdown();
		this.scheduledExecutorService.shutdown();
	}

	public NamesrvConfig getNamesrvConfig() {
		return namesrvConfig;
	}

	public NettyServerConfig getNettyServerConfig() {
		return nettyServerConfig;
	}

	public KVConfigManager getKvConfigManager() {
		return kvConfigManager;
	}

	public RouteInfoManager getRouteInfoManager() {
		return routeInfoManager;
	}

	public RemotingServer getRemotingServer() {
		return remotingServer;
	}

	public void setRemotingServer(RemotingServer remotingServer) {
		this.remotingServer = remotingServer;
	}

}