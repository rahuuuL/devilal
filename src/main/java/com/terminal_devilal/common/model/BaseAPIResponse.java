package com.terminal_devilal.common.model;

public class BaseAPIResponse<T> {
	public String message;
	public boolean result;
	public T data;

	public BaseAPIResponse(String message, boolean result, T data) {
		super();
		this.message = message;
		this.result = result;
		this.data = data;
	}

	public BaseAPIResponse(String message, boolean result) {
		super();
		this.message = message;
		this.result = result;
		this.data = null;
	}

	public BaseAPIResponse(String message) {
		super();
		this.message = message;
		this.result = true;
		this.data = null;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

}
