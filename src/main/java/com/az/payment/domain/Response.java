package com.az.payment.domain;


import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

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
@SequenceGenerator(initialValue = 1, name = "AZ_SEQ", sequenceName = "RESPCODE_SEQ", allocationSize = 1)
public class Response {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transactionLog_generator")
    @SequenceGenerator(name = "transactionLog_generator", sequenceName = "transactionLog_seq")
    private long id;
    @ElementCollection
    Map<String, String> translator;
    long billerId;
    String externalResponseCode;
    String internalResponseCode;
    String description;
    int isSuccess;
    int isTimeout;
}
