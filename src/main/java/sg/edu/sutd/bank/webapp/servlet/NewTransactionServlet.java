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

package sg.edu.sutd.bank.webapp.servlet;

import static sg.edu.sutd.bank.webapp.servlet.ServletPaths.NEW_TRANSACTION;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sg.edu.sutd.bank.webapp.commons.ServiceException;
import sg.edu.sutd.bank.webapp.model.ClientAccount;
import sg.edu.sutd.bank.webapp.model.ClientTransaction;
import sg.edu.sutd.bank.webapp.model.User;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAO;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAOImpl;
import sg.edu.sutd.bank.webapp.service.ClientTransactionDAO;
import sg.edu.sutd.bank.webapp.service.ClientTransactionDAOImpl;
import sg.edu.sutd.bank.webapp.util.AccountNumberGenerator;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

@WebServlet(NEW_TRANSACTION)
public class NewTransactionServlet extends DefaultServlet {
	private static final long serialVersionUID = 1L;
	public static final String NEW_FORM_TRANSACTION = "formTransaction";
	public static final String NEW_FILE_TRANSACTION = "fileTransaction";

	private ClientTransactionDAO clientTransactionDAO = new ClientTransactionDAOImpl();
	private ClientAccountDAO clientAccountDAO = new ClientAccountDAOImpl();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			List<ClientAccount> myAccountList = clientAccountDAO.loadMyAccounts(getUserId(req));
			req.getSession().setAttribute("myAccountList" , myAccountList);
		} catch (ServiceException e) {
			sendError(req, e.getMessage());
		}
		forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String actionType = req.getParameter("actionType");
		if (actionType == null || NEW_FILE_TRANSACTION.endsWith(actionType)) {
			newFileTransaction(req,resp);
		} else if (NEW_FORM_TRANSACTION.endsWith(actionType)) {
			newFormTransaction(req, resp);
		}
	}

	private void newFormTransaction(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			if(!AccountNumberGenerator.validateAccountNumber(req.getParameter("toAccountNum"))) {
				throw new ServiceException("Invalid account number , please verify To Account Number again");
			}
			ClientTransaction clientTransaction = new ClientTransaction();
			User user = new User(getUserId(req));
			clientTransaction.setUser(user);
			clientTransaction.setAmount(new BigDecimal(req.getParameter("amount")));
			clientTransaction.setTransCode(req.getParameter("transcode"));
			clientTransaction.setToAccountNum(req.getParameter("toAccountNum"));
			clientTransaction.setFromAccountNum(req.getParameter("fromAccountNum"));
			clientTransactionDAO.create(clientTransaction);
			redirect(resp, ServletPaths.CLIENT_DASHBOARD_PAGE);
		} catch (ServiceException e) {
			sendError(req, e.getMessage());
			forward(req, resp);
		}
	}

	private void newFileTransaction(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		if(ServletFileUpload.isMultipartContent(req)){
			try {
				List<FileItem> multiparts = new ServletFileUpload(
						new DiskFileItemFactory()).parseRequest(req);
				List<String> error = new ArrayList<>();
				for(FileItem item : multiparts){
					if(item.getContentType().contains("csv") && !item.isFormField()){
						List<ClientAccount> myAccountList = clientAccountDAO.loadMyAccounts(getUserId(req));
						List<String> myAccountNumbers = new ArrayList<>();
						myAccountList.forEach(a -> myAccountNumbers.add(a.getAccountNumber()));
						BufferedReader br = new BufferedReader(new InputStreamReader(item.getInputStream()));

						String strLine;
						int lineNum = 1;
						while ((strLine = br.readLine()) != null)   {
							if(strLine.trim() != "") {
								strLine = strLine.trim();
								try {
									String[] transaction = strLine.split(",");
									String validationError = validateCSVTransaction(transaction, myAccountNumbers);
									if (validationError != "") {
										throw new ServiceException(validationError);
									}
									ClientTransaction clientTransaction = new ClientTransaction();
									User user = new User(getUserId(req));
									clientTransaction.setUser(user);
									clientTransaction.setAmount(new BigDecimal(transaction[0].trim()));
									clientTransaction.setTransCode(transaction[1].trim());
									clientTransaction.setToAccountNum(transaction[2].trim());
									clientTransaction.setFromAccountNum(transaction[3].trim());
									clientTransactionDAO.create(clientTransaction);
								} catch (Exception e) {
									error.add(" Error on line : " + lineNum + ", " + e.getMessage() + "\n");
								}
								lineNum++;
							}
						}
						br.close();
					} else {
						throw new ServiceException("Invalid file, file should be csv and less than 2Mb");
					}
				}
				if(error.size() > 0) {
					throw new ServiceException("Please find invalid transactions and related error messages. Errors : " + error.toString());
				}
			}  catch(Exception ex) {
				ex.printStackTrace();
				sendError(req, ex.getMessage());
				forward(req, resp);
			}

		}
		redirect(resp, ServletPaths.CLIENT_DASHBOARD_PAGE);
	}

	private String validateCSVTransaction(String[] transaction, final List<String> accountNumbers) {
		String errorMessage = "";
		try {
			BigDecimal ammount = new BigDecimal(transaction[0].trim());
			String transactionCode = transaction[1].trim();
			String toAccount = transaction[2].trim();
			String fromAccount = transaction[3].trim();

			if(!AccountNumberGenerator.validateAccountNumber(toAccount)) {
				errorMessage = "Invalid account number , please verify To Account Number again.";
			}

			if(!accountNumbers.contains(fromAccount)) {
				errorMessage = errorMessage + "Invalid from account number.";
			}
		} catch (Exception ex) {
			errorMessage = errorMessage + ex.getMessage();
		}
		return errorMessage;
	}
}
