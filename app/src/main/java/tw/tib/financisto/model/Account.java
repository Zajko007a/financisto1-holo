/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - parameters for bill filtering
 ******************************************************************************/
package tw.tib.financisto.model;

import static tw.tib.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static tw.tib.orb.EntityManager.DEF_SORT_COL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = ACCOUNT_TABLE)
public class Account extends MyEntity {
	
	@Column(name = "creation_date")
	public long creationDate = System.currentTimeMillis();

    @Column(name = "last_transaction_date")
    public long lastTransactionDate = System.currentTimeMillis();

	@Column(name = "updated_on")
	public long updatedOn = System.currentTimeMillis();

    @JoinColumn(name = "currency_id")
	public Currency currency;

	@Column(name = "type")
	public String type = AccountType.CASH.name();
	
	@Column(name = "card_issuer")
	public String cardIssuer;

	@Column(name = "issuer")
	public String issuer;
	
	@Column(name = "number")
	public String number;
	
	@Column(name = "total_amount")
	public long totalAmount;
	
	@Column(name = "total_limit")
	public long limitAmount;

	@Column(name = DEF_SORT_COL)
	public int sortOrder;

	@Column(name = "is_include_into_totals")
	public boolean isIncludeIntoTotals = true; 
	
	@Column(name = "last_account_id")
	public long lastAccountId;

	@Column(name = "last_category_id")
	public long lastCategoryId;
	
	@Column(name = "closing_day")
	public int closingDay;
	
	@Column(name = "payment_day")
	public int paymentDay;

    @Column(name = "note")
    public String note;

	@Column(name = "icon")
	public String icon;

	@Column(name = "accent_color")
	public String accentColor;

    public boolean shouldIncludeIntoTotals() {
        return isActive && isIncludeIntoTotals;
    }
}
