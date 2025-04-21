package com.devStudy.chat.service.utils.Exceptions;

public class WebSocketException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4874950248325178533L;

	public WebSocketException(String message) {
		super(message);
	}
	
	public WebSocketException(String message, Throwable cause) {
		super(message, cause);
	}

}
