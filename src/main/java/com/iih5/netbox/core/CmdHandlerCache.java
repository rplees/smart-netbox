package com.iih5.netbox.core;

import java.util.HashMap;
import java.util.Map;

public class CmdHandlerCache {
	// 单例操作
	private static CmdHandlerCache cmdStore;
	private static Object lockObj = new Object();

	public static CmdHandlerCache getInstance() {
		if (cmdStore == null) {
			synchronized (lockObj) {
				if (cmdStore == null) {
					cmdStore = new CmdHandlerCache();
				}
			}
		}
		return cmdStore;
	}

	private Map<Short, AnnObject> msgHandlers = new HashMap<Short, AnnObject>();

	public void putCmdHandler(short cmdId, AnnObject obj) {
		if (msgHandlers.containsKey(cmdId)) {
			throw new RuntimeException("重复加载cmd:" + cmdId + " method=" + obj.getMethod().getName());
		} else {
			msgHandlers.put(cmdId, obj);
		}
	}

	public Map<Short, AnnObject> getCmdHandlers() {
		return msgHandlers;
	}

	public AnnObject getAnnObject(short cmdId) {
		return getCmdHandlers().get(cmdId);
	}
}
