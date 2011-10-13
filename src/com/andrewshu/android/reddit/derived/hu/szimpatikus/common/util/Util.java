/*
 * Copyright 2009 Andrew Shu
 *
 * This file is part of "reddit is fun".
 *
 * "reddit is fun" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * "reddit is fun" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "reddit is fun".  If not, see <http://www.gnu.org/licenses/>.
 */

package com.andrewshu.android.reddit.derived.hu.szimpatikus.common.util;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.net.Uri;
import android.text.style.URLSpan;
import android.util.Log;

import com.andrewshu.android.reddit.derived.hu.szimpatikus.R;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.Common;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.Constants;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.things.ThingInfo;

public class Util {
	
	private static final String TAG = "Util"; //$NON-NLS-1$
	
	public static ArrayList<String> extractUris(URLSpan[] spans) {
        int size = spans.length;
        ArrayList<String> accumulator = new ArrayList<String>();

        for (int i = 0; i < size; i++) {
            accumulator.add(spans[i].getURL());
        }
        return accumulator;
    }
	
	/**
	 * Convert HTML tags so they will be properly recognized by
	 * android.text.Html.fromHtml()
	 * @param html unescaped HTML
	 * @return converted HTML
	 */
	public static String convertHtmlTags(String html) {
		// Handle <code>
		html = html.replaceAll("<code>", "<tt>").replaceAll("</code>", "</tt>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		// Handle <pre>
		int preIndex = html.indexOf("<pre>"); //$NON-NLS-1$
		int preEndIndex = -6;  // -"</pre>".length()
		StringBuilder bodyConverted = new StringBuilder();
		while (preIndex != -1) {
			// get the text between previous </pre> and next <pre>.
			bodyConverted = bodyConverted.append(html.substring(preEndIndex + 6, preIndex));
			preEndIndex = html.indexOf("</pre>", preIndex); //$NON-NLS-1$
			// Replace newlines with <br> inside the <pre></pre>
			// Retain <pre> tags since android.text.Html.fromHtml() will ignore them anyway.
			bodyConverted = bodyConverted.append(html.substring(preIndex, preEndIndex).replaceAll("\n", "<br>")) //$NON-NLS-1$ //$NON-NLS-2$
				.append("</pre>"); //$NON-NLS-1$
			preIndex = html.indexOf("<pre>", preEndIndex); //$NON-NLS-1$
		}
		html = bodyConverted.append(html.substring(preEndIndex + 6)).toString();
		
		// Handle <li>
		html = html.replaceAll("<li>", "* ").replaceAll("</li>", "<br>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		// Handle <strong> and <em>, which are normally <b> and <i> respectively, but reversed in Android.
		// ANDROID BUG: http://code.google.com/p/android/issues/detail?id=3473
		html = html.replaceAll("<strong>", "<b>").replaceAll("</strong>", "</b>") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		           .replaceAll("<em>", "<i>").replaceAll("</em>", "</i>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		return html;
	}
	
	/**
	 * To the second, not millisecond like reddit
	 * @param timeSeconds
	 * @return
	 */
	public static String getTimeAgo(long utcTimeSeconds) {
		long systime = System.currentTimeMillis() / 1000;
		long diff = systime - utcTimeSeconds;
		if (diff <= 0)
			return Messages.getString("Util.justNow"); //$NON-NLS-1$
		else if (diff < 60) {
			if (diff == 1)
				return Messages.getString("Util.aSecondAgo"); //$NON-NLS-1$
			else
				return diff + Messages.getString("Util.secondsAgo"); //$NON-NLS-1$
		}
		else if (diff < 3600) {
			if ((diff / 60) == 1)
				return Messages.getString("Util.aMinuteAgo"); //$NON-NLS-1$
			else
				return (diff / 60) + Messages.getString("Util.minutesAgo"); //$NON-NLS-1$
		}
		else if (diff < 86400) { // 86400 seconds in a day
			if ((diff / 3600) == 1)
				return Messages.getString("Util.anHourAgo"); //$NON-NLS-1$
			else
				return (diff / 3600) + Messages.getString("Util.hoursAgo"); //$NON-NLS-1$
		}
		else if (diff < 604800) { // 86400 * 7
			if ((diff / 86400) == 1)
				return Messages.getString("Util.aDayAgo"); //$NON-NLS-1$
			else
				return (diff / 86400) + Messages.getString("Util.daysAgo"); //$NON-NLS-1$
		}
		else if (diff < 2592000) { // 86400 * 30
			if ((diff / 604800) == 1)
				return Messages.getString("Util.aWeekAgo"); //$NON-NLS-1$
			else
				return (diff / 604800) + Messages.getString("Util.weeksAgo"); //$NON-NLS-1$
		}
		else if (diff < 31536000) { // 86400 * 365
			if ((diff / 2592000) == 1)
				return Messages.getString("Util.aMonthAgo"); //$NON-NLS-1$
			else
				return (diff / 2592000) + Messages.getString("Util.monthsAgo"); //$NON-NLS-1$
		}
		else {
			if ((diff / 31536000) == 1)
				return Messages.getString("Util.aYearAgo"); //$NON-NLS-1$
			else
				return (diff / 31536000) + Messages.getString("Util.yearsAgo"); //$NON-NLS-1$
		}
	}
	
	public static String getTimeAgo(double utcTimeSeconds) {
		return getTimeAgo((long)utcTimeSeconds);
	}
	
	public static String showNumComments(int comments) {
		if (comments == 1) {
			return Messages.getString("Util.oneComment"); //$NON-NLS-1$
		} else {
			return comments + Messages.getString("Util.comments"); //$NON-NLS-1$
		}
	}
	
	public static String showNumPoints(int score) {
		if (score == 1) {
			return Messages.getString("Util.onePoint"); //$NON-NLS-1$
		} else {
			return score + Messages.getString("Util.points"); //$NON-NLS-1$
		}
	}
	
	public static String absolutePathToURL(String path) {
		if (path.startsWith("/")) //$NON-NLS-1$
			return Constants.REDDIT_BASE_URL + path;
		return path;
	}
	
	public static String nameToId(String name) {
		// indexOf('_') == -1 if not found; -1 + 1 == 0
		return name.substring(name.indexOf('_') + 1);
	}
	
	public static boolean isHttpStatusOK(HttpResponse response) {
		if (response == null || response.getStatusLine() == null) {
			return false;
		}
		return response.getStatusLine().getStatusCode() == 200;
	}
	
	public static String getResponseErrorMessage(String line) throws Exception{
    	String error = null;
		
		if (StringUtils.isEmpty(line)) {
			error = Messages.getString("Util.connectError"); //$NON-NLS-1$
    		throw new HttpException("No content returned from subscribe POST"); //$NON-NLS-1$
    	}
    	if (line.contains("WRONG_PASSWORD")) { //$NON-NLS-1$
    		error = Messages.getString("Util.wrongPassword"); //$NON-NLS-1$
    		throw new Exception("Wrong password."); //$NON-NLS-1$
    	}
    	if (line.contains("USER_REQUIRED")) { //$NON-NLS-1$
    		throw new Exception("User required. Huh? The modhash probably expired."); //$NON-NLS-1$
    	}
    	
    	Common.logDLong(TAG, line);
		return error;
	}

	// ===============
	//      Theme
	// ===============

	public static boolean isLightTheme(int theme) {
		return theme == R.style.Reddit_Light_Medium || theme == R.style.Reddit_Light_Large || theme == R.style.Reddit_Light_Larger || theme == R.style.Reddit_Light_Huge;
	}
	
	public static boolean isDarkTheme(int theme) {
		return theme == R.style.Reddit_Dark_Medium || theme == R.style.Reddit_Dark_Large || theme == R.style.Reddit_Dark_Larger || theme == R.style.Reddit_Dark_Huge;
	}
	
	public static int getInvertedTheme(int theme) {
		switch (theme) {
		case R.style.Reddit_Light_Medium:
			return R.style.Reddit_Dark_Medium;
		case R.style.Reddit_Light_Large:
			return R.style.Reddit_Dark_Large;
		case R.style.Reddit_Light_Larger:
			return R.style.Reddit_Dark_Larger;
		case R.style.Reddit_Light_Huge:
			return R.style.Reddit_Dark_Huge;
		case R.style.Reddit_Dark_Medium:
			return R.style.Reddit_Light_Medium;
		case R.style.Reddit_Dark_Large:
			return R.style.Reddit_Light_Large;
		case R.style.Reddit_Dark_Larger:
			return R.style.Reddit_Light_Larger;
		case R.style.Reddit_Dark_Huge:
			return R.style.Reddit_Light_Huge;
		default:
			return R.style.Reddit_Light_Medium;	
		}
	}
	
	public static int getThemeResourceFromPrefs(String themePref, String textSizePref) {
		if (Constants.PREF_THEME_LIGHT.equals(themePref)) {
			if (Constants.PREF_TEXT_SIZE_MEDIUM.equals(textSizePref))
				return R.style.Reddit_Light_Medium;
			else if (Constants.PREF_TEXT_SIZE_LARGE.equals(textSizePref))
				return R.style.Reddit_Light_Large;
			else if (Constants.PREF_TEXT_SIZE_LARGER.equals(textSizePref))
				return R.style.Reddit_Light_Larger;
			else if (Constants.PREF_TEXT_SIZE_HUGE.equals(textSizePref))
				return R.style.Reddit_Light_Huge;
		} else /* if (Constants.PREF_THEME_DARK.equals(themePref)) */ {
			if (Constants.PREF_TEXT_SIZE_MEDIUM.equals(textSizePref))
				return R.style.Reddit_Dark_Medium;
			else if (Constants.PREF_TEXT_SIZE_LARGE.equals(textSizePref))
				return R.style.Reddit_Dark_Large;
			else if (Constants.PREF_TEXT_SIZE_LARGER.equals(textSizePref))
				return R.style.Reddit_Dark_Larger;
			else if (Constants.PREF_TEXT_SIZE_HUGE.equals(textSizePref))
				return R.style.Reddit_Dark_Huge;
		}
		return R.style.Reddit_Light_Medium;
	}
	
	/**
	 * Return the theme and textSize String prefs
	 */
	public static String[] getPrefsFromThemeResource(int theme) {
		switch (theme) {
		case R.style.Reddit_Light_Medium:
			return new String[] { Constants.PREF_THEME_LIGHT, Constants.PREF_TEXT_SIZE_MEDIUM };
		case R.style.Reddit_Light_Large:
			return new String[] { Constants.PREF_THEME_LIGHT, Constants.PREF_TEXT_SIZE_LARGE };
		case R.style.Reddit_Light_Larger:
			return new String[] { Constants.PREF_THEME_LIGHT, Constants.PREF_TEXT_SIZE_LARGER };
		case R.style.Reddit_Light_Huge:
			return new String[] { Constants.PREF_THEME_LIGHT, Constants.PREF_TEXT_SIZE_HUGE };
		case R.style.Reddit_Dark_Medium:
			return new String[] { Constants.PREF_THEME_DARK, Constants.PREF_TEXT_SIZE_MEDIUM };
		case R.style.Reddit_Dark_Large:
			return new String[] { Constants.PREF_THEME_DARK, Constants.PREF_TEXT_SIZE_LARGE };
		case R.style.Reddit_Dark_Larger:
			return new String[] { Constants.PREF_THEME_DARK, Constants.PREF_TEXT_SIZE_LARGER };
		case R.style.Reddit_Dark_Huge:
			return new String[] { Constants.PREF_THEME_DARK, Constants.PREF_TEXT_SIZE_HUGE };
		default:
			return new String[] { Constants.PREF_THEME_LIGHT, Constants.PREF_TEXT_SIZE_MEDIUM };
		}
	}
	
	public static int getTextAppearanceResource(int themeResource, int androidTextAppearanceStyle) {
		switch (themeResource) {
		case R.style.Reddit_Light_Medium:
		case R.style.Reddit_Dark_Medium:
			switch (androidTextAppearanceStyle) {
			case android.R.style.TextAppearance_Small:
				return R.style.TextAppearance_Medium_Small;
			case android.R.style.TextAppearance_Medium:
				return R.style.TextAppearance_Medium_Medium;
			case android.R.style.TextAppearance_Large:
				return R.style.TextAppearance_Medium_Large;
			default:
				return R.style.TextAppearance_Medium_Medium;
			}
		case R.style.Reddit_Light_Large:
		case R.style.Reddit_Dark_Large:
			switch (androidTextAppearanceStyle) {
			case android.R.style.TextAppearance_Small:
				return R.style.TextAppearance_Large_Small;
			case android.R.style.TextAppearance_Medium:
				return R.style.TextAppearance_Large_Medium;
			case android.R.style.TextAppearance_Large:
				return R.style.TextAppearance_Large_Large;
			default:
				return R.style.TextAppearance_Large_Medium;
			}
		case R.style.Reddit_Light_Larger:
		case R.style.Reddit_Dark_Larger:
			switch (androidTextAppearanceStyle) {
			case android.R.style.TextAppearance_Small:
				return R.style.TextAppearance_Larger_Small;
			case android.R.style.TextAppearance_Medium:
				return R.style.TextAppearance_Larger_Medium;
			case android.R.style.TextAppearance_Large:
				return R.style.TextAppearance_Larger_Large;
			default:
				return R.style.TextAppearance_Larger_Medium;
			}
		case R.style.Reddit_Light_Huge: 
		case R.style.Reddit_Dark_Huge:
			switch (androidTextAppearanceStyle) {
			case android.R.style.TextAppearance_Small:
				return R.style.TextAppearance_Huge_Small;
			case android.R.style.TextAppearance_Medium:
				return R.style.TextAppearance_Huge_Medium;
			case android.R.style.TextAppearance_Large:
				return R.style.TextAppearance_Huge_Large;
			default:
				return R.style.TextAppearance_Huge_Medium;
			}
		default:
			return R.style.TextAppearance_Medium_Medium;	
		}
	}
	
	
	// =======================
	//    Mail Notification
	// =======================
	
	public static long getMillisFromMailNotificationPref(String pref) {
		if (Constants.PREF_MAIL_NOTIFICATION_SERVICE_OFF.equals(pref)) {
			return 0;
		} else if (Constants.PREF_MAIL_NOTIFICATION_SERVICE_5MIN.equals(pref)) {
			return 5 * 60 * 1000;
		} else if (Constants.PREF_MAIL_NOTIFICATION_SERVICE_30MIN.equals(pref)) {
			return 30 * 60 * 1000;
		} else if (Constants.PREF_MAIL_NOTIFICATION_SERVICE_1HOUR.equals(pref)) {
			return 1 * 3600 * 1000;
		} else if (Constants.PREF_MAIL_NOTIFICATION_SERVICE_6HOURS.equals(pref)) {
			return 6 * 3600 * 1000;
		} else /* if (Constants.PREF_MAIL_NOTIFICATION_SERVICE_1DAY.equals(pref)) */ {
			return 24 * 3600 * 1000;
		}
	}
	
	
	// ===============
	//   Transitions
	// ===============
	
	public static void overridePendingTransition(Method activity_overridePendingTransition, Activity act, int enterAnim, int exitAnim) {
		// only available in Android 2.0 (SDK Level 5) and later
		if (activity_overridePendingTransition != null) {
			try {
				activity_overridePendingTransition.invoke(act, enterAnim, exitAnim);
			} catch (Exception ex) {
				if (Constants.LOGGING) Log.e(TAG, "overridePendingTransition", ex); //$NON-NLS-1$
			}
		}
	}
	
	// ===============
	//       Uri
	// ===============
	
	static Uri createCommentUri(String linkId, String commentId, int commentContext) {
		return Uri.parse(new StringBuilder(Constants.REDDIT_BASE_URL + "/comments/") //$NON-NLS-1$
			.append(linkId)
			.append("/z/") //$NON-NLS-1$
			.append(commentId)
			.append("?context=") //$NON-NLS-1$
			.append(commentContext)
			.toString());
	}
	
	public static Uri createCommentUri(ThingInfo commentThingInfo, int commentContext) {
		if (commentThingInfo.getContext() != null)
			return Uri.parse(absolutePathToURL(commentThingInfo.getContext()));
		if (commentThingInfo.getLink_id() != null)
			return createCommentUri(nameToId(commentThingInfo.getLink_id()), commentThingInfo.getId(), commentContext);
		return null;
	}
	
	public static Uri createProfileUri(String username) {
		return Uri.parse(new StringBuilder(Constants.REDDIT_BASE_URL + "/user/") //$NON-NLS-1$
			.append(username)
			.toString());
	}
	
	public static Uri createSubmitUri(String subreddit) {
		if (Constants.FRONTPAGE_STRING.equals(subreddit))
			return Uri.parse(Constants.REDDIT_BASE_URL + "/submit"); //$NON-NLS-1$
		
		return Uri.parse(new StringBuilder(Constants.REDDIT_BASE_URL + "/r/") //$NON-NLS-1$
			.append(subreddit)
			.append("/submit") //$NON-NLS-1$
			.toString());
	}
	
	static Uri createSubmitUri(ThingInfo thingInfo) {
		return createSubmitUri(thingInfo.getSubreddit());
	}
	
	public static Uri createSubredditUri(String subreddit) {
		if (Constants.FRONTPAGE_STRING.equals(subreddit))
			return Uri.parse(Constants.REDDIT_BASE_URL + "/"); //$NON-NLS-1$
		
		return Uri.parse(new StringBuilder(Constants.REDDIT_BASE_URL + "/r/") //$NON-NLS-1$
			.append(subreddit)
			.toString());
	}
	
	static Uri createSubredditUri(ThingInfo thingInfo) {
		return createSubredditUri(thingInfo.getSubreddit());
	}
	
	static Uri createThreadUri(String subreddit, String threadId) {
		return Uri.parse(new StringBuilder(Constants.REDDIT_BASE_URL + "/r/") //$NON-NLS-1$
			.append(subreddit)
			.append("/comments/") //$NON-NLS-1$
			.append(threadId)
			.toString());
	}
	
	public static Uri createThreadUri(ThingInfo threadThingInfo) {
		return createThreadUri(threadThingInfo.getSubreddit(), threadThingInfo.getId());
	}
	
	public static boolean isRedditUri(Uri uri) {
		if (uri == null) return false;
		String host = uri.getHost();
		return host != null && (host.equals("reddit.com") || host.endsWith(".reddit.com")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static boolean isRedditShortenedUri(Uri uri) {
		if (uri == null) return false;
		String host = uri.getHost();
		return host != null && host.equals("redd.it"); //$NON-NLS-1$
	}
	
    /**
     * Creates mobile version of <code>uri</code> if applicable.
     * 
     * @return original uri if no mobile version of uri is known
     */
    public static Uri optimizeMobileUri(Uri uri) {
    	if (isWikipediaUri(uri)) {
    		uri = createMobileWikpediaUri(uri);
    	}
    	return uri;
    }
    
    /**
     * @return if uri points to a non-mobile wikpedia uri.
     */
    static boolean isWikipediaUri(Uri uri) {
    	if (uri == null) return false;
    	String host = uri.getHost();
    	return host != null && host.endsWith(".wikipedia.org") && !host.contains(".m.wikipedia.org"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * @return mobile version of a wikipedia uri
     */
    static Uri createMobileWikpediaUri(Uri uri) {
    	String uriString = uri.toString();
    	return Uri.parse(uriString.replace(".wikipedia.org/", ".m.wikipedia.org/")); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    public static boolean isYoutubeUri(Uri uri) {
    	if (uri == null) return false;
    	String host = uri.getHost();
    	return host != null && host.endsWith(".youtube.com"); //$NON-NLS-1$
    }
    
}
