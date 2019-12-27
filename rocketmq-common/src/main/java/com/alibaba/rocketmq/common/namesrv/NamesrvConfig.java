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

/**
 * $Id: NamesrvConfig.java 1839 2013-05-16 02:12:02Z shijia.wxr $
 */
package com.alibaba.rocketmq.common.namesrv;

import java.io.File;

import com.alibaba.rocketmq.common.MixAll;


/**
 * @author shijia.wxr
 * @author lansheng.zj
 */
public class NamesrvConfig {

	// 示例: rocketmqHome=D:\work\data\RocketMQ
    private String rocketmqHome = System.getProperty(MixAll.ROCKETMQ_HOME_PROPERTY, System.getenv(MixAll.ROCKETMQ_HOME_ENV));
    // 示例: kvConfigPath=C:\Users\Administrator\namesrv\kvConfig.json
    private String kvConfigPath = System.getProperty("user.home") + File.separator + "namesrv" + File.separator + "kvConfig.json";
    // 默认: productEnvName=center
    private String productEnvName = "center";
    // 默认: clusterTest=false
    private boolean clusterTest = false;

	public String getRocketmqHome() {
		return rocketmqHome;
	}

	public void setRocketmqHome(String rocketmqHome) {
		this.rocketmqHome = rocketmqHome;
	}

	public String getKvConfigPath() {
		return kvConfigPath;
	}

	public void setKvConfigPath(String kvConfigPath) {
		this.kvConfigPath = kvConfigPath;
	}

	public String getProductEnvName() {
		return productEnvName;
	}

	public void setProductEnvName(String productEnvName) {
		this.productEnvName = productEnvName;
	}

	public boolean isClusterTest() {
		return clusterTest;
	}

	public void setClusterTest(boolean clusterTest) {
		this.clusterTest = clusterTest;
	}

}