package com.penglecode.xmodule.fabric.bankmaster.domain;

import java.io.Serializable;

/**
 * 客户账户Model
 * 
 * @author 	pengpeng
 * @date	2018年11月28日 上午10:57:31
 */
public class CustomerAccount implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private String accountNo;
		
		private String realName;
		
		private String idCardNo;
		
		private String mobilePhone;
		
		private String createdTime;
		
		private Double accountBalance;

		public String getAccountNo() {
			return accountNo;
		}

		public void setAccountNo(String accountNo) {
			this.accountNo = accountNo;
		}

		public String getRealName() {
			return realName;
		}

		public void setRealName(String realName) {
			this.realName = realName;
		}

		public String getIdCardNo() {
			return idCardNo;
		}

		public void setIdCardNo(String idCardNo) {
			this.idCardNo = idCardNo;
		}

		public String getMobilePhone() {
			return mobilePhone;
		}

		public void setMobilePhone(String mobilePhone) {
			this.mobilePhone = mobilePhone;
		}

		public String getCreatedTime() {
			return createdTime;
		}

		public void setCreatedTime(String createdTime) {
			this.createdTime = createdTime;
		}

		public Double getAccountBalance() {
			return accountBalance;
		}

		public void setAccountBalance(Double accountBalance) {
			this.accountBalance = accountBalance;
		}
		
	}