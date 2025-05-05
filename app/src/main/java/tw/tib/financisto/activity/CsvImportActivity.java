/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package tw.tib.financisto.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.MyEntityAdapter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.export.csv.CsvImportOptions;
import tw.tib.financisto.export.csv.CsvImportTask;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.utils.CurrencyExportPreferences;

public class CsvImportActivity extends AbstractImportActivity {

    private static final String TAG = "CSVImportActivity";

    public static final String CSV_IMPORT_SELECTED_ACCOUNT_2 = "CSV_IMPORT_SELECTED_ACCOUNT_2";
    public static final String CSV_IMPORT_DATE_FORMAT = "CSV_IMPORT_DATE_FORMAT";
    public static final String CSV_IMPORT_URI = "CSV_IMPORT_URI";
    public static final String CSV_IMPORT_FIELD_SEPARATOR = "CSV_IMPORT_FIELD_SEPARATOR";
    public static final String CSV_IMPORT_USE_HEADER_FROM_FILE = "CSV_IMPORT_USE_HEADER_FROM_FILE";

    private final CurrencyExportPreferences currencyPreferences = new CurrencyExportPreferences("csv");

    private DatabaseAdapter db;
    private List<Account> accounts;
    private Spinner accountSpinner;
    private CheckBox useHeaderFromFile;

    private Uri openedUri;

    public CsvImportActivity() {
        super(R.layout.csv_import);
    }

    @Override
    protected void internalOnCreate() {
        db = new DatabaseAdapter(this);
        db.open();

        accounts = db.getAllAccountsList();
        ArrayAdapter<Account> accountsAdapter = new MyEntityAdapter<>(this, android.R.layout.simple_spinner_item, android.R.id.text1, accounts);
        accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner = findViewById(R.id.spinnerAccount);
        accountSpinner.setAdapter(accountsAdapter);

        useHeaderFromFile = findViewById(R.id.cbUseHeaderFromFile);

        Button bOk = findViewById(R.id.bOK);
        bOk.setOnClickListener(view -> {
            bOk.setEnabled(false);

            if (edFilename.getText().toString().equals("")) {
                Toast.makeText(CsvImportActivity.this, R.string.select_filename, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent data = new Intent();
            updateResultIntentFromUi(data);

            // started from main activity menu, continue import work there
            if (openedUri == null) {
                setResult(RESULT_OK, data);
                finish();
            }
            else {
                // opened file from other app, do import here, then redirect to main activity
                CsvImportOptions options = CsvImportOptions.fromIntent(data);
                ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_import_inprogress), true);
                CsvImportTask task = new CsvImportTask(this, progressDialog, options);
                task.setListener(result -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                });
                task.execute();
            }
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            openedUri = intent.getData();
            Log.i(TAG, "uri: " + openedUri);

            edFilename.setText(getText(R.string.provided_by_other_app));
            edFilename.setEnabled(false);
            bBrowse.setEnabled(false);
        }
    }


    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        currencyPreferences.updateIntentFromUI(this, data);
        data.putExtra(CSV_IMPORT_SELECTED_ACCOUNT_2, getSelectedAccountId());
        Spinner dateFormats = findViewById(R.id.spinnerDateFormats);
        data.putExtra(CSV_IMPORT_DATE_FORMAT, dateFormats.getSelectedItem().toString());
        if (openedUri != null) {
            data.putExtra(CSV_IMPORT_URI, openedUri.toString());
        }
        else {
            data.putExtra(CSV_IMPORT_URI, importFileUri.toString());
        }
        Spinner fieldSeparator = findViewById(R.id.spinnerFieldSeparator);
        data.putExtra(CSV_IMPORT_FIELD_SEPARATOR, fieldSeparator.getSelectedItem().toString().charAt(1));
        data.putExtra(CSV_IMPORT_USE_HEADER_FROM_FILE, useHeaderFromFile.isChecked());
    }

    @Override
    protected void savePreferences() {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        currencyPreferences.savePreferences(this, editor);
        editor.putLong(CSV_IMPORT_SELECTED_ACCOUNT_2, getSelectedAccountId());
        Spinner dateFormats = findViewById(R.id.spinnerDateFormats);
        editor.putInt(CSV_IMPORT_DATE_FORMAT, dateFormats.getSelectedItemPosition());
        if (importFileUri != null) {
            editor.putString(CSV_IMPORT_URI, importFileUri.toString());
        }
        Spinner fieldSeparator = findViewById(R.id.spinnerFieldSeparator);
        editor.putInt(CSV_IMPORT_FIELD_SEPARATOR, fieldSeparator.getSelectedItemPosition());
        editor.putBoolean(CSV_IMPORT_USE_HEADER_FROM_FILE, useHeaderFromFile.isChecked());
        editor.apply();
    }

    @Override
    protected void restorePreferences() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        currencyPreferences.restorePreferences(this, preferences);

        long selectedAccountId = preferences.getLong(CSV_IMPORT_SELECTED_ACCOUNT_2, 0);
        selectedAccount(selectedAccountId);

        Spinner dateFormats = findViewById(R.id.spinnerDateFormats);
        dateFormats.setSelection(preferences.getInt(CSV_IMPORT_DATE_FORMAT, 0));

        if (openedUri == null) {
            edFilename = findViewById(R.id.edFilename);
            importFileUri = Uri.parse(preferences.getString(CSV_IMPORT_URI, ""));
            String filePath = importFileUri.getPath();
            if (filePath != null) {
                edFilename.setText(filePath.substring(filePath.lastIndexOf("/") + 1));
            }
        }

        Spinner fieldSeparator = findViewById(R.id.spinnerFieldSeparator);
        fieldSeparator.setSelection(preferences.getInt(CSV_IMPORT_FIELD_SEPARATOR, 0));
        useHeaderFromFile.setChecked(preferences.getBoolean(CSV_IMPORT_USE_HEADER_FROM_FILE, true));
    }

    private long getSelectedAccountId() {
        return accountSpinner.getSelectedItemId();
    }

    private void selectedAccount(long selectedAccountId) {
        for (int i=0; i<accounts.size(); i++) {
            Account a = accounts.get(i);
            if (a.id == selectedAccountId) {
                accountSpinner.setSelection(i);
                break;
            }
        }
    }

}
