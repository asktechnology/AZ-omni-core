package com.az.payment.mapper;

import com.az.payment.domain.Option;
import com.az.payment.response.OptionResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OptionMapper {

    public List<OptionResponse> toOptionsResponse(List<Option> options) {
        options.sort(Comparator.comparingLong(Option::getId).reversed());
        options.forEach(System.out::println);

        return options.stream().
                map(this::toOptionResponse)
                .collect(Collectors.toList());
    }

    public OptionResponse toOptionResponse(Option option) {
        return new OptionResponse(option.getId(), option.getName(), option.getValue());
    }
}
