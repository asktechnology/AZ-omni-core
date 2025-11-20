package com.az.payment.domain;


import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@EqualsAndHashCode
public class ChainTransactionLogId implements Serializable {
    private long id;
    private long reqRes;
    private long paramId;
}
