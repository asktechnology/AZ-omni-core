package com.az.payment.domain;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@Entity
public class Parameter {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parameter_generator")
    @SequenceGenerator(name = "parameter_generator", sequenceName = "parameter_seq")
    private long id;
    private String name;
    private String nameAr;
    private String description;


    private String internalKey;
    private String externalKey;

    private boolean isLogged;

    private boolean isGeneratedTransactionId;

    private int orderNo;

    private int isFixed;

    private String fixedValue;

    private boolean isViewable;

    private boolean isReceipt;

    private String regex;

    private int inputType; // [1=inputText, 2=menu, 3=checkBox, 4=textArea, 5=radio]

    private int jsonType; // [1= primitive, 2= object {}, 3= array [0,1,2], 4= arrayOfObject [{},{}] ]

    private int paramType; // [1=in, 2=out, 3=both, 4=map]
    @Enumerated(EnumType.ORDINAL)
    private ParameterDataType parameterType;
    private Boolean isResponseParam = false; // Added By Me to identify which response Code to be Mapped
    private int length;
    private double minValue;
    private double maxValue;

    private int isAmount;
    private int isBillid;
    private int isBillRescode;
    private int isBillResmsg;
    private int isExtrrn;
    private int isReqrrn;
    private String ReqrrnRegex;
    private long dataSourceId;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST, mappedBy = "parameter")
    List<Option> options = new ArrayList<>();


    @JsonBackReference
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "parameters")
    List<Service> services = new ArrayList<>();

}
