package com.Unkn0wn0ne.unknownnet.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class FileLogHandler extends Handler implements Runnable{

	public File log;
	
	private FileOutputStream outputStream = null;
	
	private Queue<String> logs = new LinkedList<String>();

	private boolean signalClose = false;
	
	public FileLogHandler() {
		File folder = new File("logs");
		if (!folder.exists()) {
			folder.mkdirs();
		}
		
		Calendar calendar = Calendar.getInstance();
		String timeSuffix = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DAY_OF_MONTH) + "-" + calendar.getTimeInMillis();
		this.log = new File(folder, "log-" + timeSuffix + ".txt");
		
		if (this.log.exists()) {
			this.log = new File(folder, "log-" + timeSuffix + "(1).txt");
		}
		
		try {
			this.log.createNewFile();
			this.outputStream = new FileOutputStream(this.log);
		} catch (IOException e) {
			
		}
		
		// We thread this so it doesn't lag down the client or server threads. Especially on those servers with slow hard-drives
		new Thread(this).start();
	}
	@Override
	public void close() throws SecurityException {
		this.signalClose = true;
	}

	@Override
	public void flush() {
		try {
			this.outputStream.flush();
		} catch (IOException e) {
			
		}
	}

	@Override
	public void publish(LogRecord arg0) {
		this.logs.add("[" + arg0.getLevel().getName() + "] " + arg0.getMessage());
	}
	
	@Override
	public void run() {
		while (!this.signalClose) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				
			}
			
			while (!this.logs.isEmpty()) {
				try {
					this.outputStream.write((this.logs.poll() + "\n").getBytes());
				} catch (IOException e) {
					
				}
			}
		}
		
		while (!this.logs.isEmpty()) {
			try {
				this.outputStream.write((this.logs.poll() + "\n").getBytes());
			} catch (IOException e) {
				
			}
		}
			
		try {
			this.outputStream.close();
		} catch (IOException e) {
			
		}
	}
}