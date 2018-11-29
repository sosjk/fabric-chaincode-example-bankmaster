package com.penglecode.xmodule.fabric.bankmaster.exception;

public class FabricChaincodeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FabricChaincodeException() {
		super();
	}

	public FabricChaincodeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public FabricChaincodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public FabricChaincodeException(String message) {
		super(message);
	}

	public FabricChaincodeException(Throwable cause) {
		super(cause);
	}

}
