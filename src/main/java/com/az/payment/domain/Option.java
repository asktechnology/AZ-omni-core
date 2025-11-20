package com.az.payment.domain;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "parameter_option")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@Entity
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paramoption_generator")
    @SequenceGenerator(name = "paramoption_generator", sequenceName = "paramoption_seq")
    private long id;

    private String nameAr;

    private String name;

    private String value;
    @ManyToOne
    @JoinColumn(name = "parameterId")
    @JsonBackReference(value = "ref_options")
    Parameter parameter;
}
