package com.az.payment.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;


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
public class ChainTransactionLog {

    @EmbeddedId
    private ChainTransactionLogId id;

    @Column(length = 1, insertable = false, updatable = false)
    private long reqRes;
    @Column(length = 19, insertable = false, updatable = false)
    private long paramId;

    @Column(length = 255)
    private String key;
    @Column(length = 255)
    private String value;

    @MapsId("id")  // Maps the id part of the composite key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "id")
    @JsonBackReference
    private TransactionLog transactionLog;

}