package uw.cse.dineon.user.bill;

import uw.cse.dineon.user.DineOnUserActivity;
import uw.cse.dineon.user.R;
import uw.cse.dineon.user.bill.CurrentBillFragment.PayBillListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * 
 * @author mhotan
 */
public class CurrentOrderActivity extends DineOnUserActivity implements PayBillListener { 
	
	
	private final String TAG = "CurrentOrderActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_current_order);
		
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {		
		// TODO If in landscape mode then user already sees the bill
		// So hide the fragments
		MenuItem paybillItem = menu.findItem(R.id.option_bill);
		if (paybillItem != null) {
			paybillItem.setEnabled(false);
			paybillItem.setVisible(false);
		}
		MenuItem checkInItem = menu.findItem(R.id.option_check_in);
		if (checkInItem != null) {
			checkInItem.setEnabled(false);
			checkInItem.setVisible(false);
		}
		MenuItem viewOrderItem = menu.findItem(R.id.option_view_order);
		if (viewOrderItem != null) {
			viewOrderItem.setEnabled(false);
			viewOrderItem.setVisible(false);
		}
		
		return true;
	}

	@Override
	public void doneWithOrder() {
		finish();
	}

	@Override
	public void payCurrentBill() {
		super.payBill();
		finish();
	}
			
}
