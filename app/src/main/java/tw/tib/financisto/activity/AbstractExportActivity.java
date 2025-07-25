/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import tw.tib.financisto.R;
import tw.tib.financisto.datetime.DateUtils;
import tw.tib.financisto.datetime.Period;
import tw.tib.financisto.datetime.PeriodType;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.filter.DateTimeCriteria;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;

import java.text.DateFormat;
import java.util.Date;

public abstract class AbstractExportActivity extends AppCompatActivity {

    private final int layoutId;
	private final WhereFilter filter = WhereFilter.empty();

	private Button bPeriod;
	private DateFormat df;

    public AbstractExportActivity(int layoutId) {
        this.layoutId = layoutId;
    }

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(layoutId);

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
					| WindowInsetsCompat.Type.ime());
			var lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			lp.topMargin = insets.top;
			lp.bottomMargin = insets.bottom;
			v.setLayoutParams(lp);
			return WindowInsetsCompat.CONSUMED;
		});

		df = DateUtils.getShortDateFormat(this);
		
		filter.put(new DateTimeCriteria(this, PeriodType.THIS_MONTH));
		
		bPeriod = (Button)findViewById(R.id.bPeriod);
		bPeriod.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(AbstractExportActivity.this, DateFilterActivity.class);
				filter.toIntent(intent);
				startActivityForResult(intent, 1);
			}
		});

		Button bOk = (Button)findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                savePreferences();
                Intent data = new Intent();
                filter.toIntent(data);
                updateResultIntentFromUi(data);
                setResult(RESULT_OK, data);
                finish();
            }
        });

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        internalOnCreate();
        restorePreferences();
		updatePeriod();
	}

    protected abstract void savePreferences();

    protected abstract void restorePreferences();

    protected abstract void internalOnCreate();

    protected abstract void updateResultIntentFromUi(Intent data);

    public void clearFilter() {
        filter.clear();
    }

	private void updatePeriod() {
		DateTimeCriteria c = filter.getDateTime();
		if (c == null) {
			bPeriod.setText(R.string.no_filter);
		} else {
			Period p = c.getPeriod();
			if (p.isCustom()) {
				long periodFrom = c.getLongValue1();
				long periodTo = c.getLongValue2();
				bPeriod.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
			} else {
				bPeriod.setText(p.type.titleId);
			}		
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_FIRST_USER) {
				filter.clearDateTime();
			} else if (resultCode == RESULT_OK) {
				String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
				PeriodType p = PeriodType.valueOf(periodType);
				if (PeriodType.CUSTOM == p) {
					long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
					long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
					filter.put(new DateTimeCriteria(periodFrom, periodTo));
				} else {
					filter.put(new DateTimeCriteria(this, p));
				}			
			}
			updatePeriod();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
	}	
}
