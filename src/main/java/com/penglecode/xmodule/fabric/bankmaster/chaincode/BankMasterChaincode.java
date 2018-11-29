package com.penglecode.xmodule.fabric.bankmaster.chaincode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.penglecode.xmodule.fabric.bankmaster.domain.CustomerAccount;
import com.penglecode.xmodule.fabric.common.util.BankUtils;
import com.penglecode.xmodule.fabric.common.util.DateTimeUtils;
import com.penglecode.xmodule.fabric.common.util.JsonUtils;

public class BankMasterChaincode extends ChaincodeBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(BankMasterChaincode.class);
	
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	
	private static final Double DEFAULT_ACCOUNT_BALANCE = 0.0;
	
	private static final String BANK_CARD_PREFIX = "6225";
	
	private static final String KEY_BANK_BALANCE = "BANK_BALANCE";
	
	private static final String KEY_PREFIX_CUSTOMER_ACCOUNT = "CUSTOMER_ACCOUNT_";
	
	/**
	 * 智能合约初始化
	 * 参数列表：args[0] = 100		<银行资产金额>
	 */
	@Override
	public Response init(ChaincodeStub stub) {
		List<String> args = stub.getStringArgs();
		Double bankBalance = DEFAULT_ACCOUNT_BALANCE;
		if(args.size() == 1 && NumberUtils.isCreatable(StringUtils.trimToEmpty(args.get(0))) 
				&& (bankBalance = Double.parseDouble(StringUtils.trimToEmpty(args.get(0)))) > 0) {
			stub.putStringState(KEY_BANK_BALANCE, bankBalance.toString()); //初始化银行资产
			return newSuccessResponse("初始化智能合约成功!");
        } else {
        	return newErrorResponse("初始化智能合约失败：参数只能有一个，并且为非负数值类型数据!");
        }
	}

	/**
	 * 调用智能合约
	 */
	@Override
	public Response invoke(ChaincodeStub stub) {
		String function = stub.getFunction();
        List<String> args = stub.getParameters();
        LOGGER.info(">>> 调用智能合约开始，function = {}, args = {}", function, args);
        Response response = null;
        try {
	        response = doInvoke(stub, function, args);
        } catch (Throwable e) {
        	LOGGER.error(e.getMessage(), e);
        	response = newErrorResponse(String.format("调用智能合约出错：%s", ExceptionUtils.getRootCauseMessage(e)));
        }
        LOGGER.info("<<< 调用智能合约结束，response = [status = {}, message = {}, payload = {}]", response.getStatus().getCode(), response.getMessage(), new String(response.getPayload(), CHARSET));
        return response;
	}
	
	
	protected Response doInvoke(ChaincodeStub stub, String function, List<String> args) throws Exception {
		if("createAccount".equals(function)) {
        	return createAccount(stub, args);
        } else if ("depositMoney".equals(function)) {
        	return depositMoney(stub, args);
        } else if ("drawalMoney".equals(function)) {
        	return drawalMoney(stub, args);
        } else if ("transferAccount".equals(function)) {
        	return transferAccount(stub, args);
        } else if ("getAccountBalance".equals(function)) {
        	return getAccountBalance(stub, args);
        } else if ("getAllAccountList".equals(function)) {
        	return getAllAccountList(stub, args);
        }
		return newErrorResponse(String.format("不存在的智能合约方法名: %s", function));
	}
	
	/**
	 * 客户开户
	 * 参数列表：args[0] = {"accountNo":null,"realName":"彭三","idCardNo":"342425198607284712","mobilePhone":"15151887280"} 		<客户资料json>
	 * @param stub
	 * @param args
	 * @return
	 * @throws Exception
	 */
	protected Response createAccount(ChaincodeStub stub, List<String> args) throws Exception {
		String requestBody = null;
		if(args.size() == 1 && JsonUtils.isJsonObject((requestBody = args.get(0)))) {
			CustomerAccount account = JsonUtils.json2Object(requestBody, CustomerAccount.class);
			if(StringUtils.isBlank(account.getRealName())) {
				return newErrorResponse("请求参数不合法：开户人真实姓名不能为空!");
			}
			if(StringUtils.isBlank(account.getIdCardNo())) {
				return newErrorResponse("请求参数不合法：开户人身份证号码不能为空!");
			}
			if(StringUtils.isBlank(account.getMobilePhone())) {
				return newErrorResponse("请求参数不合法：开户人手机号码不能为空!");
			}
			account.setAccountBalance(ObjectUtils.defaultIfNull(account.getAccountBalance(), DEFAULT_ACCOUNT_BALANCE));
			account.setCreatedTime(DateTimeUtils.formatNow());
			account.setAccountNo(BankUtils.genBankCardNo(BANK_CARD_PREFIX));
			String jsonAccount = JsonUtils.object2Json(account);
			stub.putStringState(customerAccountKey(account.getAccountNo()), jsonAccount);
			return newSuccessResponse("开户成功!", jsonAccount.getBytes(CHARSET));
		} else {
			return newErrorResponse("请求参数不合法：参数只能有一个，并且为json类型数据!");
		}
	}
	
	/**
	 * 客户存款
	 * 参数列表：args[0] = 6225778834761431			<客户账户卡号>
	 * 			 args[1] = 500						<存款金额>
	 * @param stub
	 * @param args
	 * @return
	 * @throws Exception
	 */
	protected synchronized Response depositMoney(ChaincodeStub stub, List<String> args) throws Exception {
		String accountNo = null;
		String amountValue = null;
		if(args.size() == 2) {
			accountNo = StringUtils.trimToEmpty(args.get(0));
			if(!accountNo.matches("\\d{16}")) {
				return newErrorResponse("请求参数不合法：第一个参数为账户卡号，必须是16位银行卡号!");
			}
			amountValue = StringUtils.trimToEmpty(args.get(1));
			if(!NumberUtils.isCreatable(amountValue)) {
				return newErrorResponse("请求参数不合法：第二个参数为存款金额，必须是大于0的数值类型!");
			}
			Double amount = Double.valueOf(amountValue);
			if(amount <= 0) {
				return newErrorResponse("请求参数不合法：第二个参数为存款金额，必须是大于0的数值类型!");
			}
			
			CustomerAccount account = getCustomerAccountByNo(stub, accountNo);
			if(account == null) {
				return newErrorResponse(String.format("对不起，账号(%s)不存在!", accountNo));
			}
			account.setAccountBalance(account.getAccountBalance() + amount); //更新余额
			return newSuccessResponse("存款成功!", account.getAccountBalance().toString().getBytes(CHARSET));
		} else {
			return newErrorResponse("请求参数不合法：参数只能有两个!");
		}
	}
	
	/**
	 * 客户取款
	 * 参数列表：args[0] = 6225778834761431			<客户账户卡号>
	 * 			 args[1] = 500						<取款金额>
	 * @param stub
	 * @param args
	 * @return
	 * @throws Exception
	 */
	protected synchronized Response drawalMoney(ChaincodeStub stub, List<String> args) throws Exception {
		String accountNo = null;
		String amountValue = null;
		if(args.size() == 2) {
			accountNo = StringUtils.trimToEmpty(args.get(0));
			if(!accountNo.matches("\\d{16}")) {
				return newErrorResponse("请求参数不合法：第一个参数为账户卡号，必须是16位银行卡号!");
			}
			amountValue = StringUtils.trimToEmpty(args.get(1));
			if(!NumberUtils.isCreatable(amountValue)) {
				return newErrorResponse("请求参数不合法：第二个参数为取款金额，必须是大于0的数值类型!");
			}
			Double amount = Double.valueOf(amountValue);
			if(amount <= 0) {
				return newErrorResponse("请求参数不合法：第二个参数为取款金额，必须是大于0的数值类型!");
			}
			
			CustomerAccount account = getCustomerAccountByNo(stub, accountNo);
			if(account == null) {
				return newErrorResponse(String.format("对不起，账号(%s)不存在!", accountNo));
			}
			account.setAccountBalance(account.getAccountBalance() - amount); //更新余额
			return newSuccessResponse("取款成功!", account.getAccountBalance().toString().getBytes(CHARSET));
		} else {
			return newErrorResponse("请求参数不合法：参数只能有两个!");
		}
	}
	
	/**
	 * 客户转账
	 * 参数列表：args[0] = 6225778834761431			<转出账户卡号>
	 * 			 args[1] = 6225778834761432			<转入账户卡号>
	 * 			 args[2] = 500						<转账金额>
	 * @param stub
	 * @param args
	 * @return
	 * @throws Exception
	 */
	protected synchronized Response transferAccount(ChaincodeStub stub, List<String> args) throws Exception {
		String accountANo = null, accountBNo = null;
		String amountValue = null;
		if(args.size() == 3) {
			accountANo = StringUtils.trimToEmpty(args.get(0));
			if(!accountANo.matches("\\d{16}")) {
				return newErrorResponse("请求参数不合法：第一个参数为转出账户卡号，必须是16位银行卡号!");
			}
			accountBNo = StringUtils.trimToEmpty(args.get(1));
			if(!accountBNo.matches("\\d{16}")) {
				return newErrorResponse("请求参数不合法：第二个参数为转入账户卡号，必须是16位银行卡号!");
			}
			amountValue = StringUtils.trimToEmpty(args.get(2));
			if(!NumberUtils.isCreatable(amountValue)) {
				return newErrorResponse("请求参数不合法：第三个参数为转账金额，必须是大于0的数值类型!");
			}
			Double amount = Double.valueOf(amountValue);
			if(amount <= 0) {
				return newErrorResponse("请求参数不合法：第三个参数为转账金额，必须是大于0的数值类型!");
			}
			
			CustomerAccount accountA = getCustomerAccountByNo(stub, accountANo);
			if(accountA == null) {
				return newErrorResponse(String.format("对不起，转出账号(%s)不存在!", accountANo));
			}
			CustomerAccount accountB = getCustomerAccountByNo(stub, accountBNo);
			if(accountB == null) {
				return newErrorResponse(String.format("对不起，转入账号(%s)不存在!", accountBNo));
			}
			accountA.setAccountBalance(accountA.getAccountBalance() - amount); //更新余额
			accountB.setAccountBalance(accountB.getAccountBalance() + amount); //更新余额
			return newSuccessResponse("转账成功!", accountA.getAccountBalance().toString().getBytes(CHARSET));
		} else {
			return newErrorResponse("请求参数不合法：参数只能有三个!");
		}
	}
	
	/**
	 * 查询账户余额
	 * 参数列表：args[0] = 6225778834761431			<账户卡号>
	 * @param stub
	 * @param args
	 * @return
	 * @throws Exception
	 */
	protected Response getAccountBalance(ChaincodeStub stub, List<String> args) throws Exception {
		String accountNo = null;
		if(args.size() == 1 && (accountNo = StringUtils.trimToEmpty(args.get(0))).matches("\\d{16}")) {
			CustomerAccount account = getCustomerAccountByNo(stub, accountNo);
			if(account == null) {
				return newErrorResponse(String.format("对不起，账号(%s)不存在!", accountNo));
			}
			return newSuccessResponse("查询余额成功!", account.getAccountBalance().toString().getBytes(CHARSET));
		} else {
			return newErrorResponse("请求参数不合法：参数只能有一个，且必须是16位银行卡号!");
		}
	}
	
	/**
	 * 查询所有账户列表
	 * @param stub
	 * @param args
	 * @return
	 * @throws Exception
	 */
	protected Response getAllAccountList(ChaincodeStub stub, List<String> args) throws Exception {
		List<String> accounts = new ArrayList<String>();
		QueryResultsIterator<KeyValue> results = stub.getStateByPartialCompositeKey(KEY_PREFIX_CUSTOMER_ACCOUNT);
		for(Iterator<KeyValue> it = results.iterator(); it.hasNext();) {
			KeyValue kv = it.next();
			accounts.add(kv.getStringValue());
		}
		results.close();
		String payload = "[" + StringUtils.join(accounts, ",") + "]";
		return newSuccessResponse("查询所有账户列表成功!", payload.getBytes(CHARSET));
	}
	
	protected String customerAccountKey(String accountNo) {
		return KEY_PREFIX_CUSTOMER_ACCOUNT + accountNo;
	}
	
	protected CustomerAccount getCustomerAccountByNo(ChaincodeStub stub, String accountNo) {
		String key = customerAccountKey(accountNo);
		String value = stub.getStringState(key);
		if(!StringUtils.isEmpty(value)) {
			return JsonUtils.json2Object(value, CustomerAccount.class);
		}
		return null;
	}
	
	public static void main(String[] args) {
        new BankMasterChaincode().start(args);
    }

}
