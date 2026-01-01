package com.terminal_devilal.core_processes.sync_data.model;

import com.terminal_devilal.base_models.BaseAPIResponse;

public class DataSyncProcessResponse extends BaseAPIResponse<String> {

	public DataSyncProcessResponse(String message, boolean result, String data) {
		super(message, result, data);
	}

	public DataSyncProcessResponse(String message, boolean result) {
		super(message, result);
	}

	public DataSyncProcessResponse(String message) {
		super(message);
	}
}
