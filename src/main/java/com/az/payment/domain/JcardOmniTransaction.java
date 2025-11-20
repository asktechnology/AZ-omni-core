package com.az.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "JCARD_OMNI_TRANSACTION")
//@NoArgsConstructor
@AllArgsConstructor
@Data
public class JcardOmniTransaction {
    public JcardOmniTransaction() {
    }

    @Id
    @Column(name = "JCA_OMN_TRN_CODE", precision = 10)
    private Long code;

    @Column(name = "JCA_OMN_TRN_REQ_DATE", length = 50)
    private String requestDate;

    @Column(name = "JCA_OMN_TRN_RES_DATE", length = 50)
    private String responseDate;

    @Column(name = "JCA_OMN_TRN_MTI", length = 4)
    private String mti;

    @Column(name = "JCA_OMN_TRN_CHNL", length = 30)
    private String channel;

    @Column(name = "JCA_OMN_TRN_RRN", length = 30)
    private String rrn;

    @Column(name = "JCA_OMN_TRN_BILLER_ID", length = 10)
    private String billerId;

    @Column(name = "JCA_OMN_TRN_BILLER_SUB_ID", length = 10)
    private String billerSubId;

    @Column(name = "JCA_OMN_TRN_VOUCHER_ID", length = 30)
    private String voucherId;

    @Column(name = "JCA_OMN_TRN_EX_RRN", length = 30)
    private String externalRrn;

    @Column(name = "JCA_OMN_TRN_FRMACT", length = 30)
    private String fromAccount;

    @Column(name = "JCA_OMN_TRN_TOACT", length = 30)
    private String toAccount;

    @Column(name = "JCA_OMN_TRN_FEES_ACT", length = 30)
    private String feesAccount;

    @Column(name = "JCA_OMN_TRN_AMT", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "JCA_OMN_TRN_FEES", precision = 10, scale = 2)
    private BigDecimal fees;

    @Column(name = "JCA_OMN_TRN_CUST_BAL", length = 20)
    private String customerBalance;

    @Column(name = "JCA_OMN_TRN_CB_RES", length = 30)
    private String cbResponse;

    @Column(name = "JCA_OMN_TRN_CB_DATE", length = 50)
    private String cbDate;

    @Column(name = "JCA_OMN_TRN_CB_RRN", length = 30)
    private String cbRrn;

    @Column(name = "JCA_OMN_TRN_CB_ERR_CODE", length = 100)
    private String cbErrorCode;

    @Column(name = "JCA_OMN_TRN_CB_ERR_MSG", length = 1000)
    private String cbErrorMessage;

    @Column(name = "JCA_OMN_TRN_RVR", length = 10)
    private String reverse;

    @Column(name = "JCA_OMN_TRN_TYPE", length = 20)
    private String type;

    @Column(name = "JCA_OMN_TRN_COMM_ACT", length = 30)
    private String commissionAccount;

    @Column(name = "JCA_OMN_TRN_COMM", precision = 10, scale = 2)
    private BigDecimal commission;

    @Column(name = "JCA_OMN_TRN_RSP_MSG", length = 100)
    private String responseMessage;

    @Column(name = "JCA_OMN_TRN_TAX_ACT", length = 30)
    private String taxAccount;

    @Column(name = "JCA_OMN_TRN_TAX", precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "JCA_OMN_TRN_SETT_STS", length = 30)
    private String settlementStatus;

    @Column(name = "JCA_OMN_TRN_FRM_BRANCH", length = 10)
    private String fromBranch;

    @Column(name = "JCA_OMN_TRN_TO_BRANCH", length = 10)
    private String toBranch;

    @Column(name = "JCA_OMN_TRN_SETT_FILE", length = 100)
    private String settlementFile;

    @Column(name = "JCA_OMN_SETT_TRN_ADVICE_CODE", length = 30)
    private String settlementTransactionAdviceCode;

    @Column(name = "JCA_OMN_SETT_FEES_ADVICE_CODE", length = 30)
    private String settlementFeesAdviceCode;

    @Column(name = "JCA_OMN_SETT_COMM_ADVICE_CODE", length = 30)
    private String settlementCommissionAdviceCode;

    @Column(name = "JCA_OMN_SETT_TAX_ADVICE_CODE", length = 30)
    private String settlementTaxAdviceCode;

    @Column(name = "JCA_OMN_SETT_TRN_SUMM_CODE", length = 30)
    private String settlementTransactionSummaryCode;

    @Column(name = "JCA_OMN_SETT_FEES_SUMM_CODE", length = 30)
    private String settlementFeesSummaryCode;

    @Column(name = "JCA_OMN_SETT_COMM_SUMM_CODE", length = 30)
    private String settlementCommissionSummaryCode;

    @Column(name = "JCA_OMN_SETT_TAX_SUMM_CODE", length = 30)
    private String settlementTaxSummaryCode;

    @Column(name = "JCA_OMN_TRN_SETT_RVR", length = 10)
    @Builder.Default
    private String settlementReverse = "N";

    @Column(name = "JCA_OMN_TRN_BILLER_RES", length = 30)
    private String billerResponse;

    @Column(name = "JCA_OMN_TRN_BILLER_MSG", length = 100)
    private String billerMessage;
}
