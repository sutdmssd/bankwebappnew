/*
 * Copyright 2017 SUTD Licensed under the
	Educational Community License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may
	obtain a copy of the License at

https://opensource.org/licenses/ECL-2.0

	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an "AS IS"
	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
	or implied. See the License for the specific language governing
	permissions and limitations under the License.
 */

package sg.edu.sutd.bank.webapp.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class ClientAccount extends AbstractIdEntity {
	private User user;
	private BigDecimal amount;
	private String accountNumber;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public void widrawal(BigDecimal value) {
		this.amount.subtract(value);
	}

	public void send(BigDecimal value) {
		this.amount.add(value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClientAccount that = (ClientAccount) o;
		return Objects.equals(accountNumber, that.accountNumber);
	}

	@Override
	public int hashCode() {

		return Objects.hash(accountNumber);
	}
}
