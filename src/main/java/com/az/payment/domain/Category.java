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
public class Category {


    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_generator")
//    @SequenceGenerator(name = "category_generator", sequenceName = "category_seq",allocationSize = 10)
     private long id;
    private String name;
    private String description;
    private String nameAr;
    private String imageUrl;
//    @JsonBackReference(value = "ref_services")
//    @ManyToMany(fetch = FetchType.EAGER)
//    @JoinTable(name = "Category_Service", joinColumns = {@JoinColumn(name = "serviceId")}, inverseJoinColumns = {
//            @JoinColumn(name = "categoryId")})
//    List<Service> serviceCategories = new ArrayList<>();

    @JsonBackReference(value = "ref_billers")
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "Category_biller", joinColumns = {@JoinColumn(name = "category_id")}, inverseJoinColumns = {
            @JoinColumn(name = "biller_id")})
    List<Biller> billers = new ArrayList<>();
}
