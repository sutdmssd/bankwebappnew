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

package sg.edu.sutd.bank.webapp.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sg.edu.sutd.bank.webapp.commons.ServiceException;
import sg.edu.sutd.bank.webapp.model.ClientAccount;
import sg.edu.sutd.bank.webapp.model.ClientTransaction;
import sg.edu.sutd.bank.webapp.model.TransactionStatus;
import sg.edu.sutd.bank.webapp.model.User;

public class ClientTransactionDAOImpl extends AbstractDAOImpl implements ClientTransactionDAO {

	@Override
	public void create(ClientTransaction clientTransaction) throws ServiceException {
		Connection conn = connectDB();

		PreparedStatement ps = null;
		PreparedStatement ps1 = null;
		try {
			conn.setAutoCommit(false);
			ps = prepareStmt(conn, "INSERT INTO client_transaction(trans_code, amount, to_account_num, from_account_num, user_id)"
					+ " VALUES(?,?,?,?,?)");
			int idx = 1;
			ps.setString(idx++, clientTransaction.getTransCode());
			ps.setBigDecimal(idx++, clientTransaction.getAmount());
			ps.setString(idx++, clientTransaction.getToAccountNum());
			ps.setString(idx++, clientTransaction.getFromAccountNum());
			ps.setInt(idx++, clientTransaction.getUser().getId());
			executeInsert(clientTransaction, ps);

			ps1 = prepareStmt(conn, "UPDATE transaction_code SET used = ? WHERE code = ? AND user_id = ? AND used = ?");
			ps1.setBoolean(1, true);
			ps1.setString(2, clientTransaction.getTransCode());
			ps1.setInt(3, clientTransaction.getUser().getId());
			ps1.setBoolean(4, false);
			//If transaction code already used, throw the exception and rollback
			if(ps1.executeUpdate() != 1) {
				throw new ServiceException("Invalid transaction code , please use valid code");
			}

			conn.commit();
		} catch (SQLException e) {
			throw ServiceException.wrap(e);
		} finally {
			closeDb(conn,ps,null);
			closeDb(null,ps1,null);
		}
	}

	@Override
	public List<ClientTransaction> load(User user) throws ServiceException {
		Connection conn = connectDB();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(
					"SELECT * FROM client_transaction WHERE user_id = ?");
			int idx = 1;
			ps.setInt(idx++, user.getId());
			rs = ps.executeQuery();
			List<ClientTransaction> transactions = new ArrayList<ClientTransaction>();
			while (rs.next()) {
				ClientTransaction trans = new ClientTransaction();
				trans.setId(rs.getInt("id"));
				trans.setUser(user);
				trans.setAmount(rs.getBigDecimal("amount"));
				trans.setDateTime(rs.getDate("datetime"));
				trans.setStatus(TransactionStatus.of(rs.getString("status")));
				trans.setTransCode(rs.getString("trans_code"));
				trans.setToAccountNum(rs.getString("to_account_num"));
				trans.setFromAccountNum(rs.getString("from_account_num"));
				transactions.add(trans);
			}
			return transactions;
		} catch (SQLException e) {
			throw ServiceException.wrap(e);
		} finally {
			closeDb(conn, ps, rs);
		}
	}

	@Override
	public ClientTransaction loadTransactionById(Integer id) throws ServiceException {
		Connection conn = connectDB();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(
					"SELECT * FROM client_transaction WHERE id = ?");
			int idx = 1;
			ps.setInt(idx++, id);
			rs = ps.executeQuery();
			ClientTransaction trans = new ClientTransaction();
			rs = ps.executeQuery();
			if (rs.next()) {
				trans.setId(rs.getInt("id"));
				trans.setAmount(rs.getBigDecimal("amount"));
				trans.setDateTime(rs.getDate("datetime"));
				trans.setStatus(TransactionStatus.of(rs.getString("status")));
				trans.setTransCode(rs.getString("trans_code"));
				trans.setToAccountNum(rs.getString("to_account_num"));
				trans.setFromAccountNum(rs.getString("from_account_num"));
			}

			return trans;
		} catch (SQLException e) {
			throw ServiceException.wrap(e);
		} finally {
			closeDb(conn, ps, rs);
		}
	}

	@Override
	public List<ClientTransaction> loadWaitingList() throws ServiceException {
		Connection conn = connectDB();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(
					"SELECT * FROM client_transaction WHERE status is null");
			rs = ps.executeQuery();
			List<ClientTransaction> transactions = new ArrayList<ClientTransaction>();
			while (rs.next()) {
				ClientTransaction trans = new ClientTransaction();
				trans.setId(rs.getInt("id"));
				User user = new User(rs.getInt("user_id"));
				trans.setUser(user);
				trans.setAmount(rs.getBigDecimal("amount"));
				trans.setDateTime(rs.getDate("datetime"));
				trans.setTransCode(rs.getString("trans_code"));
				trans.setToAccountNum(rs.getString("to_account_num"));
				trans.setFromAccountNum(rs.getString("from_account_num"));
				transactions.add(trans);
			}
			return transactions;
		} catch (SQLException e) {
			throw ServiceException.wrap(e);
		} finally {
			closeDb(conn, ps, rs);
		}
	}

	@Override
	public void updateApprovedTransaction(List<ClientTransaction> transactions) throws ServiceException {
		ClientAccountDAO clientAccountDAO = new ClientAccountDAOImpl();
		Connection conn = connectDB();
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		try {

			conn.setAutoCommit(false);

		for (int i = 0; i < transactions.size(); i++) {
			ClientTransaction clientTransaction = this.loadTransactionById(transactions.get(i).getId());
			ClientAccount fromAccount = clientAccountDAO.loadAccountByAccountNumber(clientTransaction.getFromAccountNum());
			ClientAccount toAccount = clientAccountDAO.loadAccountByAccountNumber(clientTransaction.getToAccountNum());

			if (fromAccount != null && toAccount != null) {
				synchronized (fromAccount) {
					synchronized (toAccount) {
						StringBuilder updateSatusQuery = new StringBuilder("UPDATE client_transaction SET status = ? WHERE id = ? ");
						StringBuilder updateFromAccount = new StringBuilder("UPDATE client_account SET amount = ? WHERE account_number = ? ");
						StringBuilder updateToAccount = new StringBuilder("UPDATE client_account SET amount = ? WHERE account_number = ? ");

						ps1 = prepareStmt(conn, updateSatusQuery.toString());
						ps1.setString(1, TransactionStatus.APPROVED.toString());
						ps1.setInt(2, transactions.get(i).getId());
						executeUpdate(ps1);

						ps2 = prepareStmt(conn, updateFromAccount.toString());
						ps2.setBigDecimal(1, fromAccount.getAmount().subtract(clientTransaction.getAmount()));
						ps2.setString(2, fromAccount.getAccountNumber());
						executeUpdate(ps2);

						ps3 = prepareStmt(conn, updateToAccount.toString());
						ps3.setBigDecimal(1, toAccount.getAmount().add(clientTransaction.getAmount()));
						ps3.setString(2, toAccount.getAccountNumber());
						executeUpdate(ps3);
						conn.commit();
					}
				}
			}
		}
		}  catch (SQLException e) {
			throw ServiceException.wrap(e);
		} finally {
			closeDb(conn, ps1, null);
			closeDb(null, ps2, null);
			closeDb(null,ps3,null);
		}

	}

	@Override
	public void updatedeclinedTransaction(List<ClientTransaction> transactions) throws ServiceException {

		Connection conn = connectDB();
		PreparedStatement ps = null;
		try {

			conn.setAutoCommit(false);
			for (int i = 0; i < transactions.size(); i++) {
				StringBuilder updateSatusQuery = new StringBuilder("UPDATE client_transaction SET status = ? WHERE id = ? ");

				ps = prepareStmt(conn, updateSatusQuery.toString());
				ps.setString(1,TransactionStatus.DECLINED.toString());
				ps.setInt(2, transactions.get(i).getId());
				ps.executeUpdate();
				conn.commit();
			}
		}  catch (SQLException e) {
			throw ServiceException.wrap(e);
		} finally {
			closeDb(conn, ps, null);
		}
	}

}
