package com.liwendi.coolweather.util;

public interface HttpCallbackListener {
	void onFinish(String response,String flag);
	void onError(Exception e,String flag);
}