package com.Unkn0wn0ne.unknownnet.server.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class UnknownLogger {
 
	private static Logger logger = Logger.getLogger("UnknownNet");
	
	private static boolean isFilteringNetworking = false;
	private static boolean isFilteringDebugInformation = false;
	private static boolean isFilteringImplementation = false;
	
	static {
		logger.setUseParentHandlers(false);
		ConsoleHandler cHandler = new ConsoleHandler();
		Formatter formatter = new Formatter() {		
			@Override
			public String format(LogRecord record) {
				StringBuilder strBuilder = new StringBuilder();
				strBuilder.append("[");
				strBuilder.append(record.getLevel().getName());
				strBuilder.append("] ");
				strBuilder.append(record.getMessage());
				
				if (record.getThrown() != null) {
					strBuilder.append(" ");
					strBuilder.append(record.getThrown().getMessage());
					
					StackTraceElement[] sTES = record.getThrown().getStackTrace();
					for (int i = 0; i < sTES.length; i++) {
						strBuilder.append(sTES[i].toString());
						strBuilder.append("\n");
					}
				}
				strBuilder.append("\n");
				return strBuilder.toString();
			}
		};
		cHandler.setFormatter(formatter);
		logger.addHandler(cHandler);
		logger.addHandler(new FileLogHandler());
	}
	
	public static void log(Level level, LogType msgType, String content) {
		log(level, msgType, content, null);
	}
	
	public static void log(Level level, LogType msgType, String content, Throwable throwable) {
		if (msgType == LogType.NETWORKING && isFilteringNetworking) {
			return;
		}
		
		if (msgType == LogType.DEBUG_INFORMATION && isFilteringDebugInformation) {
			return;
		}
		
		if (msgType == LogType.IMPLEMENTATION && isFilteringImplementation) {
			return;
		}
		
		
	    if (throwable == null)  {
	    	logger.log(level, msgType.getPrefix() + content);
	    } else {
	    	logger.log(level, msgType.getPrefix() + content, throwable);
	    }
	}
}
