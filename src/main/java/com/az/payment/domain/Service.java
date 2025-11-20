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
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_generator")
    @SequenceGenerator(name = "service_generator", sequenceName = "service_seq")
    private long id;
    private String name;
    private String nameAr;
    private String description;
    private boolean isActive;
    private String servicePath;
    private String imageUrl;
    @Enumerated(EnumType.ORDINAL)
    private ServiceType serviceType; // [1=custom, 2=inq-pay, 3=pay, 4=data-source]
    // above key represents the service
//    @org.hibernate.annotations.ColumnDefault("0l")
    private long beforeServiceId = 0;// if application should perform any request before this
    //    @org.hibernate.annotations.ColumnDefault("0l")
    private long afterServiceId = 0; // for payment only service to return its value to mobile inorder to shows the payment serviceId

    @ManyToMany(mappedBy = "services", fetch = FetchType.EAGER)
    List<Biller> billers = new ArrayList<>();
    // add service parameters
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "Service_Parameter", joinColumns = {@JoinColumn(name = "parameterId")}, inverseJoinColumns = {
            @JoinColumn(name = "serviceId")})
    List<Parameter> parameters = new ArrayList<>();

    private boolean isPayment = false;


}
