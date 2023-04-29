package com.jlogbeat.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApiUtils {
	private static final String CLAB_SESSION_ID_HEADER = "clab-session-id";
	public static <T> ResponseEntity<T> buildSuccess(T body){
		return new ResponseEntity<T>(body,HttpStatus.OK);
	}
	
	public static ResponseEntity<String> buildError(String msg){
		return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
	}
	
	public static ResponseEntity<String> buildError(String msg,HttpStatus status){
		return new ResponseEntity<String>(msg,status );
	}
	
	public static Integer extractClabSessionId(HttpServletRequest request) throws Exception{
		return Integer.parseInt(request.getHeader(CLAB_SESSION_ID_HEADER));
	}
}
