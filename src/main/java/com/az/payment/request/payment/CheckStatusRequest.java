package com.az.payment.request.payment;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/*
author a.salah

*/
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CheckStatusRequest {

    private String transactionId;
}
