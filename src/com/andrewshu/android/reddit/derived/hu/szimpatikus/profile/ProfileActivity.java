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

package com.andrewshu.android.reddit.derived.hu.szimpatikus.profile;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.andrewshu.android.reddit.derived.hu.szimpatikus.R;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.captcha.CaptchaCheckRequiredTask;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.captcha.CaptchaDownloadTask;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.comments.CommentsListActivity;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.CacheInfo;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.Common;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.Constants;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.ProgressInputStream;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.tasks.VoteTask;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.util.StringUtils;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.common.util.Util;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.login.LoginDialog;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.login.LoginTask;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.mail.MessageComposeTask;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.settings.RedditSettings;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.things.Listing;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.things.ListingData;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.things.ThingInfo;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.things.ThingListing;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.threads.BitmapManager;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.threads.ThreadsListActivity;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.threads.ThreadsListActivity.ThreadClickDialogOnClickListenerFactory;
import com.andrewshu.android.reddit.derived.hu.szimpatikus.threads.ThreadsListActivity.ThumbnailOnClickListenerFactory;

/**
 * Activity to view user submissions and comments.
 * Also check their link and comment karma.
 * 
 * @author TalkLittle
 *
 */
public final class ProfileActivity extends ListActivity
		implements View.OnCreateContextMenuListener {

	private static final String TAG = "ProfileActivity";
	
	static final Pattern USER_PATH_PATTERN = Pattern.compile(Constants.USER_PATH_PATTERN_STRING);
	// 1: link karma; 2: comment karma
	static final Pattern KARMA_PATTERN = Pattern.compile(">(\\d[^<]*)<.{2,20}link karma.+>(\\d[^<]*)<.{2,20}comment karma");
	
    private final ObjectMapper mObjectMapper = Common.getObjectMapper();
    private BitmapManager mBitmapManager = new BitmapManager();
    
    /** Custom list adapter that fits our threads data into the list. */
    private ThingsListAdapter mThingsAdapter;
    private ArrayList<ThingInfo> mThingsList;
    // Lock used when modifying the mMessagesAdapter
    private static final Object MESSAGE_ADAPTER_LOCK = new Object();
    
    
    private final HttpClient mClient = Common.getGzipHttpClient();
    
    
    // Common settings are stored here
    private final RedditSettings mSettings = new RedditSettings();
    
    // UI State
    private View mVoteTargetView = null;
    private ThingInfo mVoteTargetThingInfo = null;
    private URLSpan[] mVoteTargetSpans = null;
    // TODO: String mVoteTargetId so when you rotate, you can find the TargetThingInfo again
    private DownloadProfileTask mCurrentDownloadThingsTask = null;
    private final Object mCurrentDownloadThingsTaskLock = new Object();
    private View mNextPreviousView = null;
    
    private String mUsername = null;
    
    private String mAfter = null;
    private String mBefore = null;
    private int mCount = 0;
    private String mLastAfter = null;
    private String mLastBefore = null;
    private int mLastCount = 0;
    private String[] mKarma = null;
    private String mSortByUrl = null;
    private String mSortByUrlExtra = null;
    
    private String mJumpToThreadId = null;

    private volatile String mCaptchaIden = null;
	private volatile String mCaptchaUrl = null;
    
    // ProgressDialogs with percentage bars
//    private AutoResetProgressDialog mLoadingCommentsProgress;
//    private int mNumVisibleMessages;
    
    private boolean mCanChord = false;
    
    /**
     * Called when the activity starts up. Do activity initialization
     * here, not in a constructor.
     * 
     * @see Activity#onCreate
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		CookieSyncManager.createInstance(getApplicationContext());
		
        mSettings.loadRedditPreferences(this, mClient);
        setRequestedOrientation(mSettings.getRotation());
        setTheme(mSettings.getTheme());
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.profile_list_content);
        
		if (savedInstanceState != null) {
        	if (Constants.LOGGING) Log.d(TAG, "using savedInstanceState");
        	mUsername = savedInstanceState.getString(Constants.USERNAME_KEY);
        	mAfter = savedInstanceState.getString(Constants.AFTER_KEY);
	        mBefore = savedInstanceState.getString(Constants.BEFORE_KEY);
	        mCount = savedInstanceState.getInt(Constants.THREAD_COUNT_KEY);
	        mLastAfter = savedInstanceState.getString(Constants.LAST_AFTER_KEY);
	        mLastBefore = savedInstanceState.getString(Constants.LAST_BEFORE_KEY);
	        mLastCount = savedInstanceState.getInt(Constants.THREAD_LAST_COUNT_KEY);
	        mKarma = savedInstanceState.getStringArray(Constants.KARMA_KEY);
		    mSortByUrl = savedInstanceState.getString(Constants.CommentsSort.SORT_BY_KEY);
	        mJumpToThreadId = savedInstanceState.getString(Constants.JUMP_TO_THREAD_ID_KEY);
		    mVoteTargetThingInfo = savedInstanceState.getParcelable(Constants.VOTE_TARGET_THING_INFO_KEY);
		    
		    // try to restore mThingsList using getLastNonConfigurationInstance()
		    // (separate function to avoid a compiler warning casting ArrayList<ThingInfo>
		    restoreLastNonConfigurationInstance();
		    if (mThingsList == null) {
	        	// Load previous page of profile items
		        if (mLastAfter != null) {
		        	new DownloadProfileTask(mUsername, mLastAfter, null, mLastCount).execute();
		        } else if (mLastBefore != null) {
		        	new DownloadProfileTask(mUsername, null, mLastBefore, mLastCount).execute();
		        } else {
		        	new DownloadProfileTask(mUsername).execute();
		        }
		    } else {
		    	// Orientation change. Use prior instance.
		    	resetUI(new ThingsListAdapter(this, mThingsList));
	    		setTitle(mUsername + "'s profile");
		    }
		    return;
        }
		// Handle subreddit Uri passed via Intent
        else if (getIntent().getData() != null) {
	    	Matcher userPathMatcher = USER_PATH_PATTERN.matcher(getIntent().getData().getPath());
			if (userPathMatcher.matches()) {
				mUsername = userPathMatcher.group(1);
				new DownloadProfileTask(mUsername).execute();
				return;
			}
		}
		
		// No username specified by Intent, so load the logged in user's profile
		if (mSettings.isLoggedIn()) {
			mUsername = mSettings.getUsername();
        	new DownloadProfileTask(mUsername).execute();
        	return;
        }
		
		// Can't find a username to use. Quit.
    	if (Constants.LOGGING) Log.e(TAG, "Could not find a username to use for ProfileActivity");
    	finish();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
		CookieSyncManager.getInstance().startSync();
    	int previousTheme = mSettings.getTheme();
    	boolean previousLoggedIn = mSettings.isLoggedIn();
    	mSettings.loadRedditPreferences(this, mClient);
    	setRequestedOrientation(mSettings.getRotation());
    	if (mSettings.getTheme() != previousTheme) {
    		resetUI(mThingsAdapter);
    	}
    	updateNextPreviousButtons();
    	updateKarma();
    	if (mSettings.isLoggedIn() != previousLoggedIn) {
    		new DownloadProfileTask(mSettings.getUsername()).execute(Constants.DEFAULT_COMMENT_DOWNLOAD_LIMIT);
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
		CookieSyncManager.getInstance().stopSync();
		mSettings.saveRedditPreferences(this);
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        // Avoid having to re-download and re-parse the messages list
    	// when rotating or opening keyboard.
    	return mThingsList;
    }
    
    @SuppressWarnings("unchecked")
	private void restoreLastNonConfigurationInstance() {
    	mThingsList = (ArrayList<ThingInfo>) getLastNonConfigurationInstance();
    }

    
    
    /**
     * Return the ThingInfo based on linear search over the names
     */
    private ThingInfo findThingInfoByName(String name) {
    	if (name == null)
    		return null;
    	synchronized(MESSAGE_ADAPTER_LOCK) {
    		for (int i = 0; i < mThingsAdapter.getCount(); i++) {
    			if (mThingsAdapter.getItem(i).getName().equals(name))
    				return mThingsAdapter.getItem(i);
    		}
    	}
    	return null;
    }
    
    
    private final class ThingsListAdapter extends ArrayAdapter<ThingInfo> {
    	static final int THREAD_ITEM_VIEW_TYPE = 0;
    	static final int COMMENT_ITEM_VIEW_TYPE = 1;
    	
    	private static final int VIEW_TYPE_COUNT = 2;
    	
    	public boolean mIsLoading = true;
    	
    	private LayoutInflater mInflater;
        
        @Override
        public int getItemViewType(int position) {
        	ThingInfo item = getItem(position);
        	if (item.getName().startsWith(Constants.THREAD_KIND)) {
        		return THREAD_ITEM_VIEW_TYPE;
        	}
        	if (item.getName().startsWith(Constants.COMMENT_KIND)) {
        		return COMMENT_ITEM_VIEW_TYPE;
        	}
            return COMMENT_ITEM_VIEW_TYPE;
        }
        
        @Override
        public int getViewTypeCount() {
        	return VIEW_TYPE_COUNT;
        }
        
    	public boolean isEmpty() {
    		if (mIsLoading)
    			return false;
    		return super.isEmpty();
    	}
    	
        public ThingsListAdapter(Context context, List<ThingInfo> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            
            ThingInfo item = this.getItem(position);
            
            if (getItemViewType(position) == THREAD_ITEM_VIEW_TYPE) {
	            // Here view may be passed in for re-use, or we make a new one.
	            if (convertView == null) {
	                view = mInflater.inflate(R.layout.threads_list_item, null);
	            } else {
	                view = convertView;
	            }
	            
	            ThreadsListActivity.fillThreadsListItemView(view, item, ProfileActivity.this, mSettings,
	            		mBitmapManager, true, thumbnailOnClickListenerFactory);
            }
            
            else if (getItemViewType(position) == COMMENT_ITEM_VIEW_TYPE) {
	            // Here view may be passed in for re-use, or we make a new one.
	            if (convertView == null) {
	                view = mInflater.inflate(R.layout.comments_list_item, null);
	            } else {
	                view = convertView;
	            }
	            
            	CommentsListActivity.fillCommentsListItemView(view, item, mSettings);
            	view.setPadding(15, 5, 0, 5);
            }
            
	        return view;
        }
    } // End of MessagesListAdapter

    
    /**
     * Called when user clicks an item in the list. Mark message read.
     * If item was already focused, open a dialog.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ThingInfo item = mThingsAdapter.getItem(position);
        
        // Mark the message/comment as selected
        mVoteTargetThingInfo = item;
        mVoteTargetView = v;
        
		if (item.getName().startsWith(Constants.THREAD_KIND)) {
			showDialog(Constants.DIALOG_THREAD_CLICK);
		} else {
			openContextMenu(v);
		}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    	int rowId = (int) info.id;
    	ThingInfo item = mThingsAdapter.getItem(rowId);
    	
        // Mark the message/comment as selected
        mVoteTargetThingInfo = item;
        mVoteTargetView = v;
        
        if (item.getName().startsWith(Constants.THREAD_KIND)) {
            menu.add(0, Constants.DIALOG_THREAD_CLICK, Menu.NONE, "Go to thread");
    	} else {
        	// TODO: include the context!
        	menu.add(0, Constants.DIALOG_COMMENT_CLICK, Menu.NONE, "Go to comment");
    	}
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	Intent i;
    	
    	switch (item.getItemId()) {
    	case Constants.DIALOG_COMMENT_CLICK:
			i = new Intent(getApplicationContext(), CommentsListActivity.class);
			i.setData(Util.createCommentUri(mVoteTargetThingInfo, 0));
			i.putExtra(Constants.EXTRA_SUBREDDIT, mVoteTargetThingInfo.getSubreddit());
			i.putExtra(Constants.EXTRA_TITLE, mVoteTargetThingInfo.getTitle());
			startActivity(i);
			return true;
    	case Constants.DIALOG_THREAD_CLICK:
			// Launch an Intent for CommentsListActivity
			CacheInfo.invalidateCachedThread(getApplicationContext());
			i = new Intent(getApplicationContext(), CommentsListActivity.class);
			i.setData(Util.createThreadUri(mVoteTargetThingInfo));
			i.putExtra(Constants.EXTRA_SUBREDDIT, mVoteTargetThingInfo.getSubreddit());
			i.putExtra(Constants.EXTRA_TITLE, mVoteTargetThingInfo.getTitle());
			i.putExtra(Constants.EXTRA_NUM_COMMENTS, Integer.valueOf(mVoteTargetThingInfo.getNum_comments()));
			startActivity(i);
			return true;
		default:
    		return super.onContextItemSelected(item);	
    	}
    }
    	
    

    /**
     * Resets the output UI list contents, retains session state.
     * @param messagesAdapter A MessagesListAdapter to use. Pass in null if you want a new empty one created.
     */
    void resetUI(ThingsListAdapter messagesAdapter) {
    	setTheme(mSettings.getTheme());
    	setContentView(R.layout.profile_list_content);
        registerForContextMenu(getListView());

        if (mSettings.isAlwaysShowNextPrevious()) {
        	// Set mNextPreviousView to null; we can use findViewById(R.id.next_previous_layout).
        	mNextPreviousView = null;
        } else {
            // If we are not using the persistent navbar, then show as ListView footer instead
	        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        mNextPreviousView = inflater.inflate(R.layout.next_previous_list_item, null);
	        getListView().addFooterView(mNextPreviousView);
        }

        synchronized (MESSAGE_ADAPTER_LOCK) {
	    	if (messagesAdapter == null) {
	            // Reset the list to be empty.
	    		mThingsList = new ArrayList<ThingInfo>();
	    		mThingsAdapter = new ThingsListAdapter(this, mThingsList);
	    	} else {
	    		mThingsAdapter = messagesAdapter;
	    	}
	    	
		    setListAdapter(mThingsAdapter);
		    mThingsAdapter.mIsLoading = false;
		    mThingsAdapter.notifyDataSetChanged();  // Just in case
		}
    	getListView().setDivider(null);
        Common.updateListDrawables(this, mSettings.getTheme());
        updateNextPreviousButtons();
    }
    
    private void enableLoadingScreen() {
    	if (Util.isLightTheme(mSettings.getTheme())) {
    		setContentView(R.layout.loading_light);
    	} else {
    		setContentView(R.layout.loading_dark);
    	}
    	synchronized (MESSAGE_ADAPTER_LOCK) {
	    	if (mThingsAdapter != null)
	    		mThingsAdapter.mIsLoading = true;
    	}
    	getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 0);
    }
    
    private void disableLoadingScreen() {
    	resetUI(mThingsAdapter);
    	getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);
    }

    private void updateNextPreviousButtons() {
    	Common.updateNextPreviousButtons(this, mNextPreviousView, mAfter, mBefore, mCount, mSettings,
    			downloadAfterOnClickListener, downloadBeforeOnClickListener);
    }
    
    private void updateKarma() {
    	if (mKarma == null)
    		return;
    	View karmaLayout = findViewById(R.id.karma_layout);
    	View karmaLayoutBorder = findViewById(R.id.karma_border_bottom);
    	if (karmaLayout != null && karmaLayoutBorder != null) {
    		karmaLayout.setVisibility(View.VISIBLE);
	    	if (Util.isLightTheme(mSettings.getTheme())) {
	       		karmaLayout.setBackgroundResource(R.color.white);
	       		karmaLayoutBorder.setBackgroundResource(R.color.black);
	    	} else {
	       		karmaLayoutBorder.setBackgroundResource(R.color.white);
	    	}
	    	TextView linkKarma = (TextView) findViewById(R.id.link_karma);
	    	TextView commentKarma = (TextView) findViewById(R.id.comment_karma);
	    	linkKarma.setText(mKarma[0] + " link karma");
	    	commentKarma.setText(mKarma[1] + " comment karma");
    	}
    }

        
    
    private class DownloadProfileTask extends AsyncTask<Integer, Long, Void>
    		implements PropertyChangeListener {
    	
    	private ArrayList<ThingInfo> _mThingInfos = new ArrayList<ThingInfo>();
    	private long _mContentLength;
    	
    	private String mUsername;
    	private String mAfter;
    	private String mBefore;
    	private int mCount;
    	private String mLastAfter = null;
    	private String mLastBefore = null;
    	private int mLastCount = 0;
    	private String[] mKarma;
    	private String mSortByUrl;
    	private String mSortByUrlExtra;
    	
    	public DownloadProfileTask(String username) {
    		this(username, null, null, Constants.DEFAULT_THREAD_DOWNLOAD_LIMIT,
    				ProfileActivity.this.mSortByUrl, ProfileActivity.this.mSortByUrlExtra);
    	}
    	
    	public DownloadProfileTask(String username, String after, String before, int count) {
    		this(username, after, before, count,
    				ProfileActivity.this.mSortByUrl, ProfileActivity.this.mSortByUrlExtra);
    	}
    	
    	/**
    	 * The real constructor
    	 * @param username
    	 * @param after
    	 * @param before
    	 * @param count
    	 * @param sortByUrl
    	 * @param sortByUrlExtra
    	 */
    	public DownloadProfileTask(String username, String after, String before, int count,
    			String sortByUrl, String sortByUrlExtra) {
    		mUsername = username;
    		mAfter = after;
    		mBefore = before;
    		mCount = count;
    		mSortByUrl = sortByUrl;
    		mSortByUrlExtra = sortByUrlExtra;
    	}
    	
    	protected void saveState() {
			ProfileActivity.this.mLastAfter = mLastAfter;
			ProfileActivity.this.mLastBefore = mLastBefore;
			ProfileActivity.this.mLastCount = mLastCount;
			ProfileActivity.this.mAfter = mAfter;
			ProfileActivity.this.mBefore = mBefore;
			ProfileActivity.this.mCount = mCount;
			ProfileActivity.this.mKarma = mKarma;
			ProfileActivity.this.mSortByUrl = mSortByUrl;
			ProfileActivity.this.mSortByUrlExtra = mSortByUrlExtra;
    	}
    	
    	// XXX: maxComments is unused for now
    	public Void doInBackground(Integer... maxComments) {
    		HttpEntity entity = null;
    		boolean isAfter = false;
    		boolean isBefore = false;
    		InputStream in = null;
    		ProgressInputStream pin = null;
        	
    		try {
    			
    			if (mKarma == null)
    				mKarma = getKarma();
    			
            	String url;
        		StringBuilder sb = new StringBuilder(Constants.REDDIT_BASE_URL + "/user/")
        			.append(mUsername.trim())
        			.append("/.json?").append(mSortByUrl).append("&")
        			.append(mSortByUrlExtra).append("&");
        		
    			// "before" always comes back null unless you provide correct "count"
        		if (mAfter != null) {
        			// count: 25, 50, ...
    				sb = sb.append("count=").append(mCount)
    					.append("&after=").append(mAfter).append("&");
    				isAfter = true;
        		}
        		else if (mBefore != null) {
        			// count: nothing, 26, 51, ...
        			sb = sb.append("count=").append(mCount + 1 - Constants.DEFAULT_THREAD_DOWNLOAD_LIMIT)
        				.append("&before=").append(mBefore).append("&");
        			isBefore = true;
        		}
        		
        		url = sb.toString();
        		if (Constants.LOGGING) Log.d(TAG, "url=" + url);
        		
        		HttpGet request = new HttpGet(url);
            	HttpResponse response = mClient.execute(request);
            	
            	// Read the header to get Content-Length since entity.getContentLength() returns -1
            	Header contentLengthHeader = response.getFirstHeader("Content-Length");
            	if (contentLengthHeader != null) {
	            	_mContentLength = Long.valueOf(contentLengthHeader.getValue());
	            	if (Constants.LOGGING) Log.d(TAG, "Content length: "+_mContentLength);
            	} else {
            		_mContentLength = -1;
            		if (Constants.LOGGING) Log.d(TAG, "Content length: UNAVAILABLE");
            	}

            	entity = response.getEntity();
            	in = entity.getContent();
            	
            	// setup a special InputStream to report progress
            	pin = new ProgressInputStream(in, _mContentLength);
            	pin.addPropertyChangeListener(this);
            	
                parseThingsJSON(pin);
                
            	mLastCount = mCount;
            	if (isAfter)
            		mCount += Constants.DEFAULT_THREAD_DOWNLOAD_LIMIT;
            	else if (isBefore)
            		mCount -= Constants.DEFAULT_THREAD_DOWNLOAD_LIMIT;
            	
                saveState();

            } catch (Exception e) {
            	if (Constants.LOGGING) Log.e(TAG, "failed", e);
        	} finally {
        		if (pin != null) {
        			try {
						pin.close();
					} catch (IOException e2) {
						if (Constants.LOGGING) Log.e(TAG, "pin.close()", e2);
					}
        		}
        		if (in != null) {
        			try {
						in.close();
					} catch (IOException e2) {
						if (Constants.LOGGING) Log.e(TAG, "in.close()", e2);
					}
        		}
        		if (entity != null) {
        			try {
        				entity.consumeContent();
        			} catch (Exception e2) {
        				if (Constants.LOGGING) Log.e(TAG, "entity.consumeContent()", e2);
        			}
        		}
            }
            return null;
	    }
    	
    	private String[] getKarma() throws IOException {
        	String url;
    		StringBuilder sb = new StringBuilder(Constants.REDDIT_BASE_URL + "/user/")
    			.append(mUsername.trim());
    		
    		url = sb.toString();
    		if (Constants.LOGGING) Log.d(TAG, "karma url=" + url);
    		
    		HttpGet request = new HttpGet(url);
        	HttpResponse response = mClient.execute(request);
        	
        	HttpEntity entity = null;
        	BufferedReader in = null;
        	try {
	        	entity = response.getEntity();
	        	in = new BufferedReader(new InputStreamReader(entity.getContent()));
	        	String line;
	        	while ((line = in.readLine()) != null) {
	        		Matcher m = KARMA_PATTERN.matcher(line);
	        		if (m.find()) {
	        			return new String[] { m.group(1), m.group(2) };
	        		}
	        	}
        	} catch (IOException ex) {
        		throw ex;
        	} catch (Exception ex) {
        		if (Constants.LOGGING) Log.e(TAG, "getKarma", ex);
        	} finally {
        		try {
        			in.close();
        		} catch (NullPointerException ex2) {
        		} catch (Exception ex2) {
        			if (Constants.LOGGING) Log.e(TAG, "in.close()", ex2);
        		}
        		try {
        			entity.consumeContent();
        		} catch (Exception ex2) {
        			if (Constants.LOGGING) Log.e(TAG, "entity.consumeContent()", ex2);
        		}
        	}
        	
        	return null;
    	}
    	
    	private void parseThingsJSON(InputStream in) throws IOException,
		    	JsonParseException, IllegalStateException {
		
    		String genericListingError = "Not a user page listing";
    		try {
    			Listing listing = mObjectMapper.readValue(in, Listing.class);
    			
    			if (!Constants.JSON_LISTING.equals(listing.getKind()))
    				throw new IllegalStateException(genericListingError);
    			// Save the modhash, after, and before
    			ListingData data = listing.getData();
    			if (StringUtils.isEmpty(data.getModhash()))
    				mSettings.setModhash(null);
    			else
    				mSettings.setModhash(data.getModhash());
    			
    			mLastAfter = mAfter;
    			mLastBefore = mBefore;
    			mAfter = data.getAfter();
    			mBefore = data.getBefore();
    			
    			// Go through the children and get the ThingInfos
    			for (ThingListing tiContainer : data.getChildren()) {
    				if (Constants.COMMENT_KIND.equals(tiContainer.getKind())) {
	   					ThingInfo ti = tiContainer.getData();
	   					// HTML to Spanned
	   					String unescapedHtmlBody = Html.fromHtml(ti.getBody_html()).toString();
	   					Spanned body = Html.fromHtml(Util.convertHtmlTags(unescapedHtmlBody));
	   					// remove last 2 newline characters
	   					if (body.length() > 2)
	   						ti.setSpannedBody(body.subSequence(0, body.length()-2));
	   					else
	   						ti.setSpannedBody("");
	   					_mThingInfos.add(ti);
    				} else if (Constants.THREAD_KIND.equals(tiContainer.getKind())) {
    					_mThingInfos.add(tiContainer.getData());
    				}
    			}
    		} catch (Exception ex) {
    			if (Constants.LOGGING) Log.e(TAG, "parseThingsJSON", ex);
    		}
    	}

		@Override
    	public void onPreExecute() {
			synchronized (mCurrentDownloadThingsTaskLock) {
				if (mCurrentDownloadThingsTask != null)
					mCurrentDownloadThingsTask.cancel(true);
				mCurrentDownloadThingsTask = this;
			}
    		resetUI(null);
    		enableLoadingScreen();
    		if (_mContentLength == -1)
    			getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_INDETERMINATE_ON);
    	}
    	
		@Override
    	public void onPostExecute(Void v) {
			synchronized (mCurrentDownloadThingsTaskLock) {
				mCurrentDownloadThingsTask = null;
			}
    		synchronized(MESSAGE_ADAPTER_LOCK) {
    			for (ThingInfo mi : _mThingInfos)
    				mThingsAdapter.add(mi);
    		}
    		
    		if (_mContentLength == -1)
    			getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_INDETERMINATE_OFF);
			
    		disableLoadingScreen();
			setTitle(String.format(getResources().getString(R.string.user_profile), mUsername));
			updateNextPreviousButtons();
			updateKarma();
    	}
		
    	@Override
    	public void onProgressUpdate(Long... progress) {
    		// 0-9999 is ok, 10000 means it's finished
    		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress[0].intValue() * 9999 / (int) _mContentLength);
    	}
    	
    	public void propertyChange(PropertyChangeEvent event) {
    		publishProgress((Long) event.getNewValue());
    	}
    }
    
    
    private class MyLoginTask extends LoginTask {
    	public MyLoginTask(String username, String password) {
    		super(username, password, mSettings, mClient, getApplicationContext());
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		showDialog(Constants.DIALOG_LOGGING_IN);
    	}
    	
    	@Override
    	protected void onPostExecute(Boolean success) {
    		dismissDialog(Constants.DIALOG_LOGGING_IN);
    		if (success) {
    			Toast.makeText(ProfileActivity.this, "Logged in as "+mUsername, Toast.LENGTH_SHORT).show();
    			showDialog(Constants.DIALOG_COMPOSE);
    		} else {
            	Common.showErrorToast(mUserError, Toast.LENGTH_LONG, ProfileActivity.this);
    		}
    	}
    }
    
    
    private class MyMessageComposeTask extends MessageComposeTask {
    	MyMessageComposeTask(Dialog dialog, ThingInfo targetThingInfo, String captcha) {
			super(dialog, targetThingInfo, captcha, mCaptchaIden, mSettings, mClient, getApplicationContext());
		}

		@Override
    	public void onPreExecute() {
    		showDialog(Constants.DIALOG_COMPOSING);
    	}
    	
    	@Override
    	public void onPostExecute(Boolean success) {
    		dismissDialog(Constants.DIALOG_COMPOSING);
    		if (success) {
    			Toast.makeText(ProfileActivity.this, "Message sent.", Toast.LENGTH_SHORT).show();
    			// TODO: add the reply beneath the original, OR redirect to sent messages page
    		} else {
    			Common.showErrorToast(_mUserError, Toast.LENGTH_LONG, ProfileActivity.this);
    			new MyCaptchaDownloadTask(_mDialog).execute();
    		}
    	}
    }
    
    private class MyVoteTask extends VoteTask {
    	
    	private int _mPreviousScore;
    	private Boolean _mPreviousLikes;
    	private ThingInfo _mTargetThingInfo;
    	
    	public MyVoteTask(ThingInfo thingInfo, int direction, String subreddit) {
    		super(thingInfo.getName(), direction, subreddit, getApplicationContext(), mSettings, mClient);
    		_mTargetThingInfo = thingInfo;
    		_mPreviousScore = thingInfo.getScore();
    		_mPreviousLikes = thingInfo.getLikes();
    	}
    	
    	@Override
    	public void onPreExecute() {
    		if (!_mSettings.isLoggedIn()) {
        		Common.showErrorToast("You must be logged in to vote.", Toast.LENGTH_LONG, _mContext);
        		cancel(true);
        		return;
        	}
        	if (_mDirection < -1 || _mDirection > 1) {
        		if (Constants.LOGGING) Log.e(TAG, "WTF: _mDirection = " + _mDirection);
        		throw new RuntimeException("How the hell did you vote something besides -1, 0, or 1?");
        	}
        	int newScore;
        	Boolean newLikes;
        	_mPreviousScore = Integer.valueOf(_mTargetThingInfo.getScore());
        	_mPreviousLikes = _mTargetThingInfo.getLikes();
        	if (_mPreviousLikes == null) {
        		if (_mDirection == 1) {
        			newScore = _mPreviousScore + 1;
        			newLikes = true;
        		} else if (_mDirection == -1) {
        			newScore = _mPreviousScore - 1;
        			newLikes = false;
        		} else {
        			cancel(true);
        			return;
        		}
        	} else if (_mPreviousLikes == true) {
        		if (_mDirection == 0) {
        			newScore = _mPreviousScore - 1;
        			newLikes = null;
        		} else if (_mDirection == -1) {
        			newScore = _mPreviousScore - 2;
        			newLikes = false;
        		} else {
        			cancel(true);
        			return;
        		}
        	} else {
        		if (_mDirection == 1) {
        			newScore = _mPreviousScore + 2;
        			newLikes = true;
        		} else if (_mDirection == 0) {
        			newScore = _mPreviousScore + 1;
        			newLikes = null;
        		} else {
        			cancel(true);
        			return;
        		}
        	}
    		_mTargetThingInfo.setLikes(newLikes);
    		_mTargetThingInfo.setScore(newScore);
    		mThingsAdapter.notifyDataSetChanged();
    	}
    	
    	@Override
    	public void onPostExecute(Boolean success) {
    		if (success) {
    			CacheInfo.invalidateCachedSubreddit(_mContext);
    		} else {
    			// Vote failed. Undo the score.
            	_mTargetThingInfo.setLikes(_mPreviousLikes);
        		_mTargetThingInfo.setScore(_mPreviousScore);
        		mThingsAdapter.notifyDataSetChanged();
        		
    			Common.showErrorToast(_mUserError, Toast.LENGTH_LONG, _mContext);
    		}
    	}
    }
    
    private class MyCaptchaCheckRequiredTask extends CaptchaCheckRequiredTask {
    	
    	Dialog _mDialog;
    	
		public MyCaptchaCheckRequiredTask(Dialog dialog) {
			super(Constants.REDDIT_BASE_URL + "/message/compose/", mClient);
			_mDialog = dialog;
		}
		
		@Override
		protected void saveState() {
			ProfileActivity.this.mCaptchaIden = _mCaptchaIden;
			ProfileActivity.this.mCaptchaUrl = _mCaptchaUrl;
		}

		@Override
		public void onPreExecute() {
			// Hide send button so user can't send until we know whether he needs captcha
			final Button sendButton = (Button) _mDialog.findViewById(R.id.compose_send_button);
			sendButton.setVisibility(View.INVISIBLE);
			// Show "loading captcha" label
			final TextView loadingCaptcha = (TextView) _mDialog.findViewById(R.id.compose_captcha_loading);
			loadingCaptcha.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onPostExecute(Boolean required) {
			final TextView captchaLabel = (TextView) _mDialog.findViewById(R.id.compose_captcha_textview);
			final ImageView captchaImage = (ImageView) _mDialog.findViewById(R.id.compose_captcha_image);
			final EditText captchaEdit = (EditText) _mDialog.findViewById(R.id.compose_captcha_input);
			final TextView loadingCaptcha = (TextView) _mDialog.findViewById(R.id.compose_captcha_loading);
			final Button sendButton = (Button) _mDialog.findViewById(R.id.compose_send_button);
			if (required == null) {
				Common.showErrorToast("Error retrieving captcha. Use the menu to try again.", Toast.LENGTH_LONG, ProfileActivity.this);
				return;
			}
			if (required) {
				captchaLabel.setVisibility(View.VISIBLE);
				captchaImage.setVisibility(View.VISIBLE);
				captchaEdit.setVisibility(View.VISIBLE);
				// Launch a task to download captcha and display it
				new MyCaptchaDownloadTask(_mDialog).execute();
			} else {
				captchaLabel.setVisibility(View.GONE);
				captchaImage.setVisibility(View.GONE);
				captchaEdit.setVisibility(View.GONE);
			}
			loadingCaptcha.setVisibility(View.GONE);
			sendButton.setVisibility(View.VISIBLE);
		}
	}
	
    private class MyCaptchaDownloadTask extends CaptchaDownloadTask {
    	
    	Dialog _mDialog;
    	
    	public MyCaptchaDownloadTask(Dialog dialog) {
    		super(mCaptchaUrl, mClient);
    		_mDialog = dialog;
    	}
    	
		@Override
		public void onPostExecute(Drawable captcha) {
			if (captcha == null) {
				Common.showErrorToast("Error retrieving captcha. Use the menu to try again.", Toast.LENGTH_LONG, ProfileActivity.this);
				return;
			}
			final ImageView composeCaptchaView = (ImageView) _mDialog.findViewById(R.id.compose_captcha_image);
			composeCaptchaView.setVisibility(View.VISIBLE);
			composeCaptchaView.setImageDrawable(captcha);
		}
	}
    
    
    
    /**
     * Populates the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile, menu);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId()) {
    	case R.id.compose_message_menu_id:
    		if (mSettings.isLoggedIn()) {
    			showDialog(Constants.DIALOG_COMPOSE);
    		} else {
    			showDialog(Constants.DIALOG_LOGIN);
    		}
    		break;
    	case R.id.refresh_menu_id:
			new DownloadProfileTask(mUsername).execute(Constants.DEFAULT_COMMENT_DOWNLOAD_LIMIT);
			break;
    	}
    	
    	return true;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog;
    	ProgressDialog pdialog;
    	AlertDialog.Builder builder;
    	LayoutInflater inflater;
    	View layout; // used for inflated views for AlertDialog.Builder.setView()
    	
    	switch (id) {
    	case Constants.DIALOG_LOGIN:
    		dialog = new LoginDialog(this, mSettings, false) {
				@Override
				public void onLoginChosen(String user, String password) {
					dismissDialog(Constants.DIALOG_LOGIN);
		        	new MyLoginTask(user, password).execute();
				}
			};
    		break;
    		
    	case Constants.DIALOG_COMPOSE:
    		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		builder = new AlertDialog.Builder(this);
    		layout = inflater.inflate(R.layout.compose_dialog, null);
    		final EditText composeDestination = (EditText) layout.findViewById(R.id.compose_destination_input);
    		final EditText composeSubject = (EditText) layout.findViewById(R.id.compose_subject_input);
    		final EditText composeText = (EditText) layout.findViewById(R.id.compose_text_input);
    		final Button composeSendButton = (Button) layout.findViewById(R.id.compose_send_button);
    		final Button composeCancelButton = (Button) layout.findViewById(R.id.compose_cancel_button);
    		final EditText composeCaptcha = (EditText) layout.findViewById(R.id.compose_captcha_input);
    		composeDestination.setText(mUsername);
    		
    		dialog = builder.setView(layout).create();
    		final Dialog composeDialog = dialog;
    		composeSendButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
		    		ThingInfo hi = new ThingInfo();
		    		// reddit.com performs these sanity checks too.
		    		if ("".equals(composeDestination.getText().toString().trim())) {
		    			Toast.makeText(ProfileActivity.this, "please enter a username", Toast.LENGTH_LONG).show();
		    			return;
		    		}
		    		if ("".equals(composeSubject.getText().toString().trim())) {
		    			Toast.makeText(ProfileActivity.this, "please enter a subject", Toast.LENGTH_LONG).show();
		    			return;
		    		}
		    		if ("".equals(composeText.getText().toString().trim())) {
		    			Toast.makeText(ProfileActivity.this, "you need to enter a message", Toast.LENGTH_LONG).show();
		    			return;
		    		}
		    		if (composeCaptcha.getVisibility() == View.VISIBLE && "".equals(composeCaptcha.getText().toString().trim())) {
		    			Toast.makeText(ProfileActivity.this, "", Toast.LENGTH_LONG).show();
		    			return;
		    		}
		    		hi.setDest(composeDestination.getText().toString().trim());
		    		hi.setSubject(composeSubject.getText().toString().trim());
		    		new MyMessageComposeTask(composeDialog, hi, composeCaptcha.getText().toString().trim())
		    			.execute(composeText.getText().toString().trim());
		    		dismissDialog(Constants.DIALOG_COMPOSE);
				}
    		});
    		composeCancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dismissDialog(Constants.DIALOG_COMPOSE);
				}
    		});
    		break;
    		
    	case Constants.DIALOG_THREAD_CLICK:
    		inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			builder = new AlertDialog.Builder(this);
			dialog = builder.setView(inflater.inflate(R.layout.thread_click_dialog, null)).create();
			break;
    		
   		// "Please wait"
    	case Constants.DIALOG_LOGGING_IN:
    		pdialog = new ProgressDialog(this);
    		pdialog.setMessage("Logging in...");
    		pdialog.setIndeterminate(true);
    		pdialog.setCancelable(false);
    		dialog = pdialog;
    		break;
    	case Constants.DIALOG_REPLYING:
    		pdialog = new ProgressDialog(this);
    		pdialog.setMessage("Sending reply...");
    		pdialog.setIndeterminate(true);
    		pdialog.setCancelable(false);
    		dialog = pdialog;
    		break;   		
    	case Constants.DIALOG_COMPOSING:
    		pdialog = new ProgressDialog(this);
    		pdialog.setMessage("Composing message...");
    		pdialog.setIndeterminate(true);
    		pdialog.setCancelable(false);
    		dialog = pdialog;
    		break;
    		
    	default:
    		throw new IllegalArgumentException("Unexpected dialog id "+id);
    	}
    	return dialog;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	super.onPrepareDialog(id, dialog);
    	    	
    	switch (id) {
//    	case Constants.DIALOG_LOGIN:
//    		if (mSettings.username != null) {
//	    		final TextView loginUsernameInput = (TextView) dialog.findViewById(R.id.login_username_input);
//	    		loginUsernameInput.setText(mSettings.username);
//    		}
//    		final TextView loginPasswordInput = (TextView) dialog.findViewById(R.id.login_password_input);
//    		loginPasswordInput.setText("");
//    		break;
    		
    	case Constants.DIALOG_COMPOSE:
    		final EditText composeDestination = (EditText) dialog.findViewById(R.id.compose_destination_input);
    		composeDestination.setText(mUsername);
    		new MyCaptchaCheckRequiredTask(dialog).execute();
    		break;
    		
    	case Constants.DIALOG_THREAD_CLICK:
    		ThreadsListActivity.fillThreadClickDialog(dialog, mVoteTargetThingInfo, mSettings,
    				threadClickDialogOnClickListenerFactory);
    		break;
    		
		default:
			// No preparation based on app state is required.
			break;
    	}
    }
    
	private final OnClickListener downloadAfterOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			new DownloadProfileTask(mUsername, mAfter, null, mCount).execute();
		}
	};
	private final OnClickListener downloadBeforeOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			new DownloadProfileTask(mUsername, null, mBefore, mCount).execute();
		}
	};
	
	private final ThumbnailOnClickListenerFactory thumbnailOnClickListenerFactory
			= new ThumbnailOnClickListenerFactory() {
		public OnClickListener getThumbnailOnClickListener(String jumpToId, String url, String threadUrl, Context context) {
//			final String fJumpToId = jumpToId;
			final String fUrl = url;
			final String fThreadUrl = threadUrl;
			return new OnClickListener() {
				public void onClick(View v) {
//					ProfileActivity.this.mJumpToThreadId = fJumpToId;
					Common.launchBrowser(ProfileActivity.this, fUrl, fThreadUrl,
							false, false, ProfileActivity.this.mSettings.isUseExternalBrowser());
				}
			};
		}
	};
	
	private final ThreadClickDialogOnClickListenerFactory threadClickDialogOnClickListenerFactory
			= new ThreadClickDialogOnClickListenerFactory() {
		public OnClickListener getLoginOnClickListener() {
			return new OnClickListener() {
				public void onClick(View v) {
					dismissDialog(Constants.DIALOG_THREAD_CLICK);
					showDialog(Constants.DIALOG_LOGIN);
				}
			};
		}
		public OnClickListener getLinkOnClickListener(ThingInfo thingInfo, boolean useExternalBrowser) {
			final ThingInfo info = thingInfo;
			final boolean fUseExternalBrowser = useExternalBrowser;
			return new OnClickListener() {
				public void onClick(View v) {
					dismissDialog(Constants.DIALOG_THREAD_CLICK);
					// Launch Intent to goto the URL
					Common.launchBrowser(ProfileActivity.this, info.getUrl(),
							Util.createThreadUri(info).toString(),
							false, false, fUseExternalBrowser);
				}
			};
		}
		public OnClickListener getCommentsOnClickListener(ThingInfo thingInfo) {
			final ThingInfo info = thingInfo;
			return new OnClickListener() {
				public void onClick(View v) {
					dismissDialog(Constants.DIALOG_THREAD_CLICK);
					// Launch an Intent for CommentsListActivity
					CacheInfo.invalidateCachedThread(ProfileActivity.this);
					Intent i = new Intent(ProfileActivity.this, CommentsListActivity.class);
					i.setData(Util.createThreadUri(info));
					i.putExtra(Constants.EXTRA_SUBREDDIT, info.getSubreddit());
					i.putExtra(Constants.EXTRA_TITLE, info.getTitle());
					i.putExtra(Constants.EXTRA_NUM_COMMENTS, Integer.valueOf(info.getNum_comments()));
					startActivity(i);
				}
			};
		}
		public CompoundButton.OnCheckedChangeListener getVoteUpOnCheckedChangeListener(ThingInfo thingInfo) {
			final ThingInfo info = thingInfo;
			return new CompoundButton.OnCheckedChangeListener() {
		    	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		    		dismissDialog(Constants.DIALOG_THREAD_CLICK);
			    	if (isChecked) {
						new MyVoteTask(info, 1, info.getSubreddit()).execute();
					} else {
						new MyVoteTask(info, 0, info.getSubreddit()).execute();
					}
				}
		    };
		}
		public CompoundButton.OnCheckedChangeListener getVoteDownOnCheckedChangeListener(ThingInfo thingInfo) {
			final ThingInfo info = thingInfo;
			return new CompoundButton.OnCheckedChangeListener() {
		        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			    	dismissDialog(Constants.DIALOG_THREAD_CLICK);
					if (isChecked) {
						new MyVoteTask(info, -1, info.getSubreddit()).execute();
					} else {
						new MyVoteTask(info, 0, info.getSubreddit()).execute();
					}
				}
		    };
		}
	};


	@Override
    protected void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putString(Constants.USERNAME_KEY, mUsername);
    	state.putString(Constants.CommentsSort.SORT_BY_KEY, mSortByUrl);
    	state.putString(Constants.JUMP_TO_THREAD_ID_KEY, mJumpToThreadId);
    	state.putString(Constants.AFTER_KEY, mAfter);
    	state.putString(Constants.BEFORE_KEY, mBefore);
    	state.putInt(Constants.THREAD_COUNT_KEY, mCount);
    	state.putString(Constants.LAST_AFTER_KEY, mLastAfter);
    	state.putString(Constants.LAST_BEFORE_KEY, mLastBefore);
    	state.putInt(Constants.THREAD_LAST_COUNT_KEY, mLastCount);
    	state.putStringArray(Constants.KARMA_KEY, mKarma);
    	state.putParcelable(Constants.VOTE_TARGET_THING_INFO_KEY, mVoteTargetThingInfo);
    }
    
    /**
     * Called to "thaw" re-animate the app from a previous onSaveInstanceState().
     * 
     * @see android.app.Activity#onRestoreInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        final int[] myDialogs = {
        	Constants.DIALOG_COMMENT_CLICK,
        	Constants.DIALOG_LOGGING_IN,
        	Constants.DIALOG_LOGIN,
        	Constants.DIALOG_MESSAGE_CLICK,
        	Constants.DIALOG_REPLY,
        	Constants.DIALOG_REPLYING,
        };
        for (int dialog : myDialogs) {
	        try {
	        	dismissDialog(dialog);
		    } catch (IllegalArgumentException e) {
		    	// Ignore.
		    }
        }
    }
}
