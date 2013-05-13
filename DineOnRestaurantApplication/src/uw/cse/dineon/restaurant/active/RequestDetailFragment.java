package uw.cse.dineon.restaurant.active;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import uw.cse.dineon.library.CustomerRequest;
import uw.cse.dineon.library.UserInfo;
import uw.cse.dineon.restaurant.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Shows the details of the request.
 * @author mhotan
 */
public class RequestDetailFragment extends Fragment 
implements OnCheckedChangeListener, OnClickListener {

	private TextView mTitle, mDetails, mTableNumber, mTimeTaken;
	private ArrayAdapter<String> mStaffAdapter;
	private Map<RadioButton, String> mUrgencyMap;
	private ImageButton mSendMessage, mSendTask;
	private EditText mMessageBlock;
	private Spinner mStaffList;

	private RequestDetailListener mListener;
	private String mUrgency;

	private CustomerRequest mRequest;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_request_detail,
				container, false);
		mRequest = null;

		mTitle = (TextView) view.findViewById(R.id.label_request_title_detail);
		mDetails = (TextView) view.findViewById(R.id.text_details_request);
		mTableNumber = (TextView) view.findViewById(R.id.input_table_number);
		mTimeTaken = (TextView) view.findViewById(R.id.input_time_taken);

		// TODO Add staff members implementation
		ArrayList<String> staff = new ArrayList<String>();
		staff.add("Bert");
		staff.add("Ernie");
		staff.add("Big Bird");
		staff.add("Elmo");
		mStaffAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, staff);
		mStaffList = (Spinner) view.findViewById(R.id.spinner_staff);
		mStaffList.setAdapter(mStaffAdapter);

		// For all the radio buttons
		// Assign a string value to send back to user as a urgency level
		// Then add a listener for changes in state
		RadioButton normal = (RadioButton) view.findViewById(R.id.radio_urgency_normal);
		RadioButton important = (RadioButton) view.findViewById(R.id.radio_urgency_important);
		RadioButton priority = (RadioButton) view.findViewById(R.id.radio_urgency_priority);
		mUrgencyMap = new HashMap<RadioButton, String>();
		mUrgencyMap.put(normal, "Normal");
		mUrgencyMap.put(important, "Important");
		mUrgencyMap.put(priority, "Priority");
		mUrgency = mUrgencyMap.get(normal);	
		normal.setOnCheckedChangeListener(this);
		important.setOnCheckedChangeListener(this);
		priority.setOnCheckedChangeListener(this);

		mMessageBlock = (EditText) view.findViewById(R.id.edittext_send_message_request);

		// For every button
		// Set listener to do fragment-> activity call backs
		mSendMessage = (ImageButton) view.findViewById(R.id.button_send_message);
		mSendTask = (ImageButton) view.findViewById(R.id.button_send_to_staff);

		mSendMessage.setOnClickListener(this);
		mSendTask.setOnClickListener(this);

		updateState();

		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof RequestDetailListener) {
			mListener = (RequestDetailListener) activity;
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implemenet RequestDetailFragment.RequestDetailListener");
		}
	}

	//////////////////////////////////////////////////////
	//// Following are public setters.  That Activities can use
	//// to set the values of what is showed to the user for this 
	//// fragment
	//////////////////////////////////////////////////////

	/**
	 * Sets the state of this fragment to this request.
	 * @param request request to update the fragment to
	 */
	public void setRequest(String request, UserInfo user) {
		mRequest = new CustomerRequest(request, user);
		if (mRequest != null) {
			mMessageBlock.setText("");
			// Set text TODO fix
			mTitle.setText(mRequest.getDescription());
			mDetails.setText("I'm Hungry!");
			mTableNumber.setText("5");
			mTimeTaken.setText("7:45pm");

			//TODO Udpate the adapter
		}

		updateState();
	}

	/**
	 * Updates the state of the view pending the whether there is a request.
	 */
	private void updateState() {
		if (mRequest == null) {
			mSendMessage.setEnabled(false);
			mSendTask.setEnabled(false);
		} else {
			mSendMessage.setEnabled(true);
			mSendTask.setEnabled(true);
		}
	}

	//////////////////////////////////////////////////////
	//// Following is the interface in which activities
	//// that wish to attach this Fragment must implement
	//// Intended to use for user input
	//////////////////////////////////////////////////////

	/**
	 * Listener for this fragment.
	 * @author mhotan
	 */
	public interface RequestDetailListener {

		/**
		 * TODO Replace all of the argument data types appropiately.
		 * @param request Request to reference
		 * @param staff staff to assign to
		 * @param urgency Urgency to accomplish task
		 */
		public void onSendTaskToStaff(String request, String staff, String urgency);

		/**
		 * Send a message to the customer about their request.
		 * @param request request to send
		 * @param message message
		 */
		public void onSendMessage(String request, String message);

	}

	@Override
	public void onCheckedChanged(CompoundButton button, boolean isChecked) {
		if (isChecked) {
			mUrgency = mUrgencyMap.get(button);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_send_message:
			mListener.onSendMessage(mRequest.getDescription(), mMessageBlock.getText().toString());
			break;
		case R.id.button_send_to_staff:
			String staffMember = mStaffList.getSelectedItem().toString();
			mListener.onSendTaskToStaff(mRequest.getDescription(), staffMember, mUrgency);
		default:

		}
	}
}
