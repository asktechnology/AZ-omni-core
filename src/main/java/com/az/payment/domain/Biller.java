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
public class Biller {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "biller_generator")
    @SequenceGenerator(name = "biller_generator",allocationSize = 1,sequenceName = "biller_seq")
    private long id;
    private String name;
    private String nameAr;
    private String description;
    private String baseUrl;
    private String imageUrl;
    private boolean active = false;

    @ManyToMany(mappedBy = "billers")
    List<Category> categories;

    // one to many services
    @JsonBackReference(value = "ref_billers")
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "service_biller", joinColumns = {@JoinColumn(name = "biller_id")}, inverseJoinColumns = {
            @JoinColumn(name = "service_id")})
    List<Service> services = new ArrayList<>();


}
