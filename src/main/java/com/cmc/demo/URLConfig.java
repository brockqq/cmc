package com.cmc.demo;



public class URLConfig {
	public static final String  FUNCTION_BOOK_URI="/reader/book";
	public static final String  FUNCTION_PROMOTE_SEARCH_URI="/reader/search/";
	public static final String  FUNCTION_PROMOTE_ACTIVE_BOOKS_URI="/reader/action";
	public static final String  FUNCTION_BOOKS_URI="/reader/books";
	public static final String  FUNCTION_CATEGORY_URI="/reader/category";
	public static final String  FUNCTION_SEARCH_URI="/reader/serrch";
	public static final String  FUNCTION_MONTHLY_URI="/reader/monthly";
	
	
	public static final String  BASE_URI="/reader";
	public static final String  LOGIN="login";
	public static final String  USER_VERIFY="userverify";
	
	public static final String  GET_CHECKCODE="getCheckCode";
	public static final String  CHECK_CODESTATUS="checkCodeStatus";
	public static final String  BY_CODE="byCode";
	
	public static final  String VERSION = "/version";
	public static final  String AUTH_LOGIN_GETCODE = "/"+LOGIN+"/"+GET_CHECKCODE;
	public static final  String AUTH_USERVERIFY_GETCODE = "/"+USER_VERIFY+"/"+GET_CHECKCODE;
	
	public static final  String AUTH_LOGIN_CHECK_CODESTATUS = "/"+LOGIN+"/"+CHECK_CODESTATUS;
	public static final  String AUTH_USERVERIFY_CHECK_CODESTATUS = "/"+USER_VERIFY+"/"+CHECK_CODESTATUS;
	
	public static final  String AUTH_LOGIN_PROCESS_BYCODE = "/"+LOGIN+"/"+BY_CODE;
	public static final  String AUTH_USERVERIFY_PROCESS_BYCODE = "/"+USER_VERIFY+"/"+BY_CODE;
}
