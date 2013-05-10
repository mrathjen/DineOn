package uw.cse.dineon.user;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import uw.cse.dineon.library.DineOnUser;
import uw.cse.dineon.library.DiningSession;
import uw.cse.dineon.library.RestaurantInfo;
import uw.cse.dineon.library.UserInfo;
import uw.cse.dineon.library.util.DineOnConstants;
import uw.cse.dineon.library.util.ParseUtil;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQuery.CachePolicy;
import com.parse.PushService;

/**
 * This class manages the communication between the Customer. 
 * and a Restaurant associated by dining session
 * 
 * This Receiver manages all the actions that are required by contract
 * 
 * @author mhotan
 */
public class UserSatellite extends BroadcastReceiver {

	/**
	 * Logging tag.
	 */
	private static final String TAG = UserSatellite.class.getSimpleName();

	/**
	 * User this station associates to.
	 */
	private DineOnUser mUser;

	/**
	 * Intent filter to control which actions to listen for.
	 */
	private final IntentFilter mIF;

	/**
	 * The channel associate with THIS Satellite.
	 */
	private String mChannel;

	/**
	 * The activity that the broadcast receiever registers with.
	 * All callback or listener methods are routed through this activity.
	 */
	private DineOnUserActivity mCurrentActivity;

	/**
	 * Creates and prepares this satellite for transmission
	 * and reception. 
	 */
	public UserSatellite() {
		mIF = new IntentFilter();
		mIF.addAction(DineOnConstants.ACTION_CONFIRM_DINING_SESSION);
		mIF.addAction(DineOnConstants.ACTION_CHANGE_RESTAURANT_INFO);
	}

	/**
	 * Registers this activity and registers a channel for the inputed user.
	 * After this notifications (if any) may arise
	 * @param user User to associate this satellite to
	 * @param activity Activity that will be registered
	 */
	public void register(DineOnUser user, DineOnUserActivity activity) {

		// Check for null values
		if (user == null) {
			throw new IllegalArgumentException(
					"Null restaurant when registering broadcast receiver");
		}
		if (activity == null) {
			Log.w(TAG, "RestaurantSatelite attempted to register null activity");
			return;
		}

		// Establish the activity 
		mCurrentActivity = activity;

		// Establish a reference to the restaurant
		mUser = user;

		// Establish the channel to make 
		mChannel = ParseUtil.getChannel(mUser.getUserInfo());

		// Registers this activity to this receiver
		mCurrentActivity.registerReceiver(this, mIF);

		// Subscribe to my channel so I can hear incoming messages
		PushService.subscribe(activity, 
				mChannel, 
				activity.getClass());
	}

	/**
	 * Turns off this receiver.
	 */
	public void unRegister() {
		if (mCurrentActivity == null) {
			return;
		}

		mCurrentActivity.unregisterReceiver(this);
		PushService.unsubscribe(mCurrentActivity, mChannel);
		mCurrentActivity = null;
	}

	/**
	 * User inputed is requesting to check in the current restaurant.
	 * IE. General use case Restaurant customer "user" arrives at a restaurant "rest".
	 * user then attempts to check in to restaurant and table identified at "tableNum".  
	 * The user application will call this method requestCheckIn(user, tableNum, rest).  
	 * Response should then  
	 *  
	 * NOTE: This method does not do any saving. That is if you want to update the
	 * restaurant you must save your argument before you call this method
	 * 
	 * @param user User to associate check in request
	 * @param tableNum Table number to associate check in request to
	 * @param rest Restaurant 
	 */
	public void requestCheckIn(UserInfo user, int tableNum,  RestaurantInfo rest) {
		Map<String, String> attr = new HashMap<String, String>();
		attr.put(DineOnConstants.TABLE_NUM, "" + tableNum);
		attr.put(DineOnConstants.OBJ_ID, user.getObjId());
		notifyByAction(DineOnConstants.ACTION_REQUEST_DINING_SESSION, attr, rest);
	} 

	/**
	 * Notify the restaurant that an order was placed for this dining session.
	 * NOTE: This method does not do any saving. That is if you want to update the
	 * restaurant you must save your argument before you call this method
	 * @param session Dining Session that was updated
	 * @param rest Restaurant to place order at
	 */
	public void notifyOrderPlaced(DiningSession session, RestaurantInfo rest) {
		notifyByAction(DineOnConstants.ACTION_ORDER_PLACED, session.getObjId(), rest);
	}

	/**
	 * Notify the restaurant that a Customer Request was placed for this
	 * dining session.
	 * NOTE: This method does not do any saving. That is if you want to update the
	 * restaurant you must save your argument before you call this method
	 * @param session Saved DiningSession that has a new Customer Request
	 * @param rest Restaurant to send notification to.
	 */
	public void notifyCustomerRequest(DiningSession session,
			RestaurantInfo rest) {
		notifyByAction(DineOnConstants.ACTION_CUSTOMER_REQUEST, session.getObjId(), rest); 
	}

	/**
	 * Notifies the restaurant that the user has successfully checked out.
	 * This method does not do any saving.
	 * NOTE: This method does not do any saving. That is if you want to update the
	 * restaurant you must save your argument before you call this method
	 * @param session Saved DiningSession that has been checked out.
	 * @param rest Restaurant to send notification to.
	 */
	public void notifyCheckOut(DiningSession session, RestaurantInfo rest) {
		notifyByAction(DineOnConstants.ACTION_CHECK_OUT, session.getObjId(), rest); 
	}

	/**
	 * Notifies the Restaurant that user has changed some aspects about
	 * itself.  
	 * NOTE: This method does not do any saving. That is if you want to update the
	 * restaurant you must save your argument before you call this method
	 * @param user User that has already been saved.
	 * @param rest Restaurant to send notification to.
	 */
	public void notifyChangeUserInfo(UserInfo user, RestaurantInfo rest) {
		notifyByAction(DineOnConstants.ACTION_CHANGE_USER_INFO, user.getObjId(), rest);
	}

	/**
	 * General notifier that tells the Restaurant associated with 
	 * info that a new object is ready for them to download.  The type
	 * of object to download is dictated by the action argument.  This
	 * "action" is a specification that is predetermined by Restaurant and
	 * Customer
	 * @param action Action to send to Restaurant
	 * @param id Object to ID to notify restaurant for
	 * @param info Restaurant to associate to
	 */
	private void notifyByAction(String action, 
			String id, RestaurantInfo info) {
		// Have to check the pointers before sending a request 
		if (id == null) {
			throw new NullPointerException("[notifiyAction] id is null");
		}
		Map<String, String> attr = new HashMap<String, String>();
		attr.put(DineOnConstants.OBJ_ID, id);
		notifyByAction(action, attr, info);
	}

	/**
	 * General notifier that sends a mapping of attributes to the restaurant.
	 * The type of reaction by the restaurant is dictated by the action argument.
	 * This "action" is a specification that is predetermined by Restaurant and
	 * Customer IE DineOnConstant.ACTION_...
	 * @param action Action to send to Restaurant
	 * @param attr Attributes to sent to the Restaurant.
	 * @param info Restaurant to associate to
	 */
	private void notifyByAction(String action, 
			Map<String, String> attr, RestaurantInfo info) {
		if (action == null) {
			throw new NullPointerException("[notifiyAction] action is null");
		}
		if (info == null) {
			throw new NullPointerException("[notifiyAction] info is null");
		}

		// Send IT!
		ParseUtil.notifyApplication(
				action,
				attr,
				ParseUtil.getChannel(info));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// Extract the channel they were sending to
		String theirChannel = intent.getExtras() == null ? null 
				: intent.getExtras().getString(DineOnConstants.PARSE_CHANNEL);

		// IF they don't have a channel are our activity died
		// Then exit this method
		if (theirChannel == null || mCurrentActivity == null) {
			Log.w(TAG, "[onReceive] Their channel: " + theirChannel 
					+ " Our activity: " + mCurrentActivity);
			return;
		}

		// They are sending to the wrong channel
		if (!theirChannel.equals(mChannel)) {
			return;
		}

		String id = null;
		JSONObject jo;
		try {
			jo = new JSONObject(
					intent.getExtras().getString(DineOnConstants.PARSE_DATA));
			id = jo.getString(DineOnConstants.OBJ_ID);
		} catch (JSONException e) {
			Log.d(TAG, "Customer sent fail case: " + e.getMessage());
			mCurrentActivity.onFail(e.getMessage());
			// Restaurant sent malformed data...
			// NOTE (MH) : What does it mean when we fail like this?
			return;
		}

		// Retrieve the action that the other satellite is requesting
		String action = intent.getAction();

		// Prepare the queries that we might need 
		ParseQuery restInfo = new ParseQuery(RestaurantInfo.class.getSimpleName());
		ParseQuery dsQuery = new ParseQuery(DiningSession.class.getSimpleName());
		restInfo.setCachePolicy(CachePolicy.NETWORK_ONLY);
		dsQuery.setCachePolicy(CachePolicy.NETWORK_ONLY);

		// Restaurant is confirming the dining session by returning a dining session.
		if (DineOnConstants.ACTION_CONFIRM_DINING_SESSION.equals(action)) {
			
			// Actually do the query knowing it is a Dining Session
			ParseQuery query = new ParseQuery(DiningSession.class.getSimpleName());
			query.getInBackground(id, new GetCallback() {
				@Override
				public void done(ParseObject object, ParseException e) {
					if (e == null) {
						mCurrentActivity.onInitialDiningSessionReceived(
								new DiningSession(object));
					} else {
						// Some error possibly internet
						mCurrentActivity.onFail(e.getMessage());
					}
				}
			});
		} 
		// Restaurant that we are currently associated to has changed some state
		else if (DineOnConstants.ACTION_CHANGE_RESTAURANT_INFO.equals(action)) {
			// WE received a dining session
			ParseQuery query = new ParseQuery(DiningSession.class.getSimpleName());
			query.getInBackground(id, new GetCallback() {
				@Override
				public void done(ParseObject object, ParseException e) {
					if (e == null) {
						mCurrentActivity.onRestaurantInfoChanged(
								new RestaurantInfo(object));
					} else {
						mCurrentActivity.onFail(e.getMessage());
					}
				}
			});
		}
	}

	/**
	 * Listener for network callback from the Satellite.
	 * @author mhotan
	 */
	public interface SatelliteListener {

		/**
		 * Notifies that a error occured.
		 * Most likely it was a network error
		 * @param message Failure message that generally describes problem.
		 */
		void onFail(String message);

		/**
		 * Notifies Customer user that a Dining session has been established
		 * and returns it via this callback.
		 * 
		 * @param session DiningSession instance on success, null on failure 
		 * (null => Restaurant not accepting dining features)
		 */
		void onInitialDiningSessionReceived(DiningSession session);

		/**
		 * Notifies the user that the restaurant has changed its state.
		 * @param restaurant Restaurant that has recently changed
		 */
		void onRestaurantInfoChanged(RestaurantInfo restaurant);
	}

}