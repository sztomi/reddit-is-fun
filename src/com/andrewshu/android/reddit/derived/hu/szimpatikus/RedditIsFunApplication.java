package com.andrewshu.android.reddit.derived.hu.szimpatikus;

import android.app.Application;

public class RedditIsFunApplication extends Application {
	private static RedditIsFunApplication application;
	
	public RedditIsFunApplication(){
		application = this;
	}
	
	public static RedditIsFunApplication getApplication(){
		return application;
	}
}
