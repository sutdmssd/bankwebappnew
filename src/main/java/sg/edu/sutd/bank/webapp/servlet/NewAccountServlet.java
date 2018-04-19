package sg.edu.sutd.bank.webapp.servlet;

import sg.edu.sutd.bank.webapp.commons.ServiceException;
import sg.edu.sutd.bank.webapp.model.ClientAccount;
import sg.edu.sutd.bank.webapp.model.ClientInfo;
import sg.edu.sutd.bank.webapp.model.ClientTransaction;
import sg.edu.sutd.bank.webapp.model.User;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAO;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAOImpl;
import sg.edu.sutd.bank.webapp.service.ClientInfoDAO;
import sg.edu.sutd.bank.webapp.service.ClientInfoDAOImpl;
import sg.edu.sutd.bank.webapp.util.AccountNumberGenerator;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static sg.edu.sutd.bank.webapp.servlet.ServletPaths.CLIENT_DASHBOARD_PAGE;
import static sg.edu.sutd.bank.webapp.servlet.ServletPaths.NEW_CLIENT_ACCOUNT;

@WebServlet(NEW_CLIENT_ACCOUNT)
public class NewAccountServlet extends DefaultServlet {
	private static final long serialVersionUID = 1L;
	private ClientInfoDAO clientInforDao = new ClientInfoDAOImpl();
	private ClientAccountDAO clientAccountDAO = new ClientAccountDAOImpl();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			List<ClientInfo> clientInfoList = clientInforDao.loadApprovedList();
			req.getSession().setAttribute("clientInfoList", clientInfoList);
			req.getSession().setAttribute("accountNumber" , AccountNumberGenerator.generate());
		} catch (ServiceException e) {
			sendError(req, e.getMessage());
		}
		forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			User user = new User(Integer.valueOf(req.getParameter("user")));
			ClientAccount clientAccount = new ClientAccount();
			clientAccount.setAmount(new BigDecimal(0));
			clientAccount.setUser(user);
			clientAccount.setAccountNumber(req.getParameter("accountNum"));
			clientAccountDAO.create(clientAccount);
			redirect(resp, ServletPaths.CLIENT_DASHBOARD_PAGE);
		} catch (ServiceException e) {
			sendError(req, e.getMessage());
			forward(req, resp);
		}
	}
}
