package com.az.payment.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hassan Elmukashfi
 * @date 30/11/2023
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@Entity
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transactionLog_generator")
    @SequenceGenerator(name = "transactionLog_generator", sequenceName = "transactionLog_seq")
    private Long id;
    @Column(length = 19)
    private String transactionId;
    @Column(length = 19)
    private long billerId;
    @Column(length = 19)
    private long serviceId;
    @Column(length = 255)
    private String voucherNo;
    @Column(length = 255)
    private String account;
    @Column(length = 7)
    private long amount;
    @Column(length = 10)
    private String status;
    @Column(length = 255)
    private String responseCode;
    @Column(length = 255)
    private String responseMessage;
    @Column(length = 255)
    private String extRrn;
    @Column(length = 2000)
    private String request;
    @Column(length = 2000)
    private String response;

    @OneToMany(mappedBy = "transactionLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ChainTransactionLog> chainTransactionLogs = new ArrayList<>();

    public void addChainTransactionLog(long paramId, long reqRes, String key, String value) {
        ChainTransactionLogId id = new ChainTransactionLogId(this.id, reqRes, paramId);
        ChainTransactionLog log = ChainTransactionLog.builder()
                .id(id)
                .reqRes(reqRes)
                .paramId(paramId)
                .transactionLog(this)
                .key(key)
                .value(value)
                .build();
        chainTransactionLogs.add(log);
    }

}
