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
package com.alibaba.rocketmq.namesrv.kvconfig;

import com.alibaba.rocketmq.common.MixAll;
import com.alibaba.rocketmq.common.constant.LoggerName;
import com.alibaba.rocketmq.common.protocol.body.KVTable;
import com.alibaba.rocketmq.namesrv.NamesrvController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author shijia.wxr
 */
public class KVConfigManager {
	private static final Logger log = LoggerFactory.getLogger(LoggerName.NamesrvLoggerName);

	private final NamesrvController namesrvController;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final HashMap<String/* Namespace */, HashMap<String/* Key */, String/* Value */>> configTable = new HashMap<String, HashMap<String, String>>();

	public KVConfigManager(NamesrvController namesrvController) {
		this.namesrvController = namesrvController;
	}

	public void load() {
		// 解析kv信息
		String content = MixAll.file2String(namesrvController.getNamesrvConfig().getKvConfigPath());
		if (content != null) {
			KVConfigSerializeWrapper kvConfigSerializeWrapper = 
				KVConfigSerializeWrapper.fromJson(content, KVConfigSerializeWrapper.class);
			if (null != kvConfigSerializeWrapper) {
				configTable.putAll(kvConfigSerializeWrapper.getConfigTable());
				log.info("load KV config table OK");
			}
		}
	}

	public void putKVConfig(final String namespace, final String key, final String value) {
		try {
			this.lock.writeLock().lockInterruptibly();
			try {
				HashMap<String, String> kvTable = this.configTable.get(namespace);
				if (null == kvTable) {
					kvTable = new HashMap<String, String>();
					this.configTable.put(namespace, kvTable);
					log.info("putKVConfig create new Namespace {}", namespace);
				}

				final String prev = kvTable.put(key, value);
				if (null != prev) {
					log.info("putKVConfig update config item, Namespace: {} Key: {} Value: {}", namespace, key, value);
				} else {
					log.info("putKVConfig create new config item, Namespace: {} Key: {} Value: {}", namespace, key, value);
				}
			} finally {
				this.lock.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("putKVConfig InterruptedException", e);
		}

		this.persist();
	}

	public void deleteKVConfig(final String namespace, final String key) {
		try {
			this.lock.writeLock().lockInterruptibly();
			try {
				HashMap<String, String> kvTable = this.configTable.get(namespace);
				if (null != kvTable) {
					String value = kvTable.remove(key);
					log.info("deleteKVConfig delete a config item, Namespace: {} Key: {} Value: {}", namespace, key, value);
				}
			} finally {
				this.lock.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("deleteKVConfig InterruptedException", e);
		}

		this.persist();
	}

	public byte[] getKVListByNamespace(final String namespace) {
		try {
			this.lock.readLock().lockInterruptibly();
			try {
				HashMap<String, String> kvTable = this.configTable.get(namespace);
				if (null != kvTable) {
					KVTable table = new KVTable();
					table.setTable(kvTable);
					return table.encode();
				}
			} finally {
				this.lock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("getKVListByNamespace InterruptedException", e);
		}

		return null;
	}

	public String getKVConfig(final String namespace, final String key) {
		try {
			this.lock.readLock().lockInterruptibly();
			try {
				HashMap<String, String> kvTable = this.configTable.get(namespace);
				if (null != kvTable) {
					return kvTable.get(key);
				}
			} finally {
				this.lock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("getKVConfig InterruptedException", e);
		}

		return null;
	}

	public void persist() {
		try {
			this.lock.readLock().lockInterruptibly();
			try {
				KVConfigSerializeWrapper kvConfigSerializeWrapper = new KVConfigSerializeWrapper();
				kvConfigSerializeWrapper.setConfigTable(this.configTable);

				String content = kvConfigSerializeWrapper.toJson();

				if (null != content) {
					MixAll.string2File(content, this.namesrvController.getNamesrvConfig().getKvConfigPath());
				}
			} catch (IOException e) {
				log.error("persist kvconfig Exception, " + this.namesrvController.getNamesrvConfig().getKvConfigPath(), e);
			} finally {
				this.lock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("persist InterruptedException", e);
		}

	}

	public void printAllPeriodically() {
		try {
			this.lock.readLock().lockInterruptibly();
			try {
				log.info("--------------------------------------------------------");
				{
					log.info("configTable SIZE: {}", this.configTable.size());
					Iterator<Entry<String, HashMap<String, String>>> it = configTable.entrySet().iterator();
					while (it.hasNext()) {
						Entry<String, HashMap<String, String>> kvConfigEntry = it.next();
						Iterator<Entry<String, String>> itSub = kvConfigEntry.getValue().entrySet().iterator();
						while (itSub.hasNext()) {
							Entry<String, String> nextSub = itSub.next();
							log.info("configTable Namespace: {} Key: {} Value: {}", kvConfigEntry.getKey(), nextSub.getKey(), nextSub.getValue());
						}
					}
				}
			} finally {
				this.lock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			log.error("printAllPeriodically InterruptedException", e);
		}
	}

}