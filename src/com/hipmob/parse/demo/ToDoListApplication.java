package com.hipmob.parse.demo;

import java.util.UUID;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseInstallation;

import com.parse.ParseUser;

import android.app.Application;

public class ToDoListApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		/*
		 * Include your applicationId and clientKey
		Parse.initialize(this, "<applicationId>", "<clientKey>");
		*/

		ParseUser.enableAutomaticUser();
		ParseACL defaultACL = new ParseACL();
		// Optionally enable public read access.
		// defaultACL.setPublicReadAccess(true);
		ParseACL.setDefaultACL(defaultACL, true);
	}
}
