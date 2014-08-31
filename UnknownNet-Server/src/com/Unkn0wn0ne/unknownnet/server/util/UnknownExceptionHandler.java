package com.Unkn0wn0ne.unknownnet.server.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Logger;

import com.Unkn0wn0ne.unknownnet.server.UnknownServer;

public class UnknownExceptionHandler implements UncaughtExceptionHandler {

	private UnknownServer server;
	
	public UnknownExceptionHandler(UnknownServer us) {
		this.server = us;
	}
	
	@Override
	public void uncaughtException(Thread arg0, Throwable arg1) {
		Logger logger = Logger.getLogger("UnknownNet");
		logger.severe("Internal/UnknownExceptionHandler: Uncaught exception in thread, " + arg0.getName() + ", occurred. Terminating thread.");
		logger.severe("Internal/UnknownExceptionHandler: If you believe this error is a result of an issue with UnknownNet, please report it to the projects github page. https://github.com/Unkn0wn-0nes/UnknownNet ");
		arg1.printStackTrace();
		logger.severe("Internal/UnknownExceptionHandler: Attempting to recover system...");
		
		if (arg0.getName().startsWith("Client")) {
		   int id = Integer.parseInt(arg0.getName().split("-")[1]);
		   logger.severe("Internal/UnknownExceptionHandler: System is recoverable, ejecting client " + id + " for triggering this.");
		   this.server.removeClientOnError(id);
		   return;
		} else if (arg0.getName().equalsIgnoreCase("Server-Connection-Thread")) {
			logger.severe("Internal/UnknownExceptionHandler: System is not recoverable, shutting down server...");
			this.server.shutdown(true);
			return;
		} else if (arg0.getName().equalsIgnoreCase("Server-UDP-Receive-Thread")) {
			logger.severe("Internal/UnknownExceptionHandler: System is not recoverable, shutting down server...");
			this.server.shutdown(true);
			return;
		} else if (arg0.getName().equalsIgnoreCase("Server-UDP-Director-Thread")) {
			logger.severe("Internal/UnknownExceptionHandler: System is not recoverable, shutting down server...");
			this.server.shutdown(true);
			return;
		} else {
			logger.severe("Internal/UnknownExceptionHandler: Thread does not belong to UnknownNet. Keeping rest of the server alive.");
		}
	}

}
