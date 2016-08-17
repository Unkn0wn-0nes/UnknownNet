/** Copyright 2014 Unkn0wn0ne

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. **/
package com.Unkn0wn0ne.unknownnet.server.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Level;

import com.Unkn0wn0ne.unknownnet.server.UnknownServer;
import com.Unkn0wn0ne.unknownnet.server.logging.LogType;
import com.Unkn0wn0ne.unknownnet.server.logging.UnknownLogger;

public class UnknownExceptionHandler implements UncaughtExceptionHandler {

	private UnknownServer server;
	
	public UnknownExceptionHandler(UnknownServer us) {
		this.server = us;
	}
	
	@Override
	public void uncaughtException(Thread arg0, Throwable arg1) {
		UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: Uncaught exception in thread, " + arg0.getName() + ", occurred. Terminating thread.");
		UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR,"Internal/UnknownExceptionHandler: If you believe this error is a result of an issue with UnknownNet, please report it to the projects github page. https://github.com/Unkn0wn-0nes/UnknownNet ");
	    UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: Stack Trace: ", arg1);
	    UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR,"Internal/UnknownExceptionHandler: Attempting to recover system...");
		
		if (arg0.getName().startsWith("Client")) {
		   int id = Integer.parseInt(arg0.getName().split("-")[1]);
		   UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: System is recoverable, ejecting client " + id + " for triggering this.");
		   this.server.removeClientOnError(id);
		   return;
		} else if (arg0.getName().equalsIgnoreCase("Server-Connection-Thread")) {
			UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: System is not recoverable, shutting down server...");
			this.server.shutdown(true);
			return;
		} else if (arg0.getName().equalsIgnoreCase("Server-UDP-Receive-Thread")) {
			UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: System is not recoverable, shutting down server...");
			this.server.shutdown(true);
			return;
		} else if (arg0.getName().equalsIgnoreCase("Server-UDP-Director-Thread")) {
			UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: System is not recoverable, shutting down server...");
			this.server.shutdown(true);
			return;
		} else if (arg0.getName().equalsIgnoreCase("Shutdown-Hook-Thread")) {
			UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: System is recoverable. Exception on shutdown thread. Ignoring.");
			return;
		} else if (arg0.getName().equalsIgnoreCase("Server-KeepAlive-Thread")) {
			UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: System is not recoverable, shutting down server.");
			this.server.shutdown(true);
			return;
		} else {
			UnknownLogger.log(Level.SEVERE, LogType.CRITICAL_ERROR, "Internal/UnknownExceptionHandler: Thread does not belong to UnknownNet. Keeping rest of the server alive.");
		}
	}

}
