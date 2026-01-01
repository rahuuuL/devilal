package com.terminal_devilal.core_processes.nse_cookie_mng.entity;

import com.terminal_devilal.base_models.BaseAPIResponse;

public class SaveCookieResponse extends BaseAPIResponse<String> {

	public SaveCookieResponse(String message, boolean result, String data) {
		super(message, result, data);
	}

	public SaveCookieResponse(String message, boolean result) {
		super(message, result);
	}

	public SaveCookieResponse(String message) {
		super(message);
	}

}
