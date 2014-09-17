package com.gockelhut.jsvcgen;

public class JsonRpcException extends RuntimeException {
	private static final long serialVersionUID = 1249369866608452195L;

	public JsonRpcException() {
		super("JSON-RPC exception");
	}

	public JsonRpcException(String message) {
		super(message);
	}

	public JsonRpcException(Throwable cause) {
		super(cause);
	}

	public JsonRpcException(String message, Throwable cause) {
		super(message, cause);
	}
}
