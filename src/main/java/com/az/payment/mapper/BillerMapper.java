package com.az.payment.mapper;

import com.az.payment.domain.Biller;
import com.az.payment.request.BillerRequest;
import com.az.payment.response.BillerResponse;
import com.az.payment.response.ServiceResponse;
import org.springframework.stereotype.Service;

@Service
public class BillerMapper {


    public BillerResponse toBillerResponse(Biller biller) {
        if (biller == null)
            throw new NullPointerException("biller is null");
        return new BillerResponse(
                biller.getId(),
                biller.getName(),
                biller.getDescription(),
                biller.getImageUrl()
        );
    }

    public Biller toBiller(BillerRequest request) {
        if (request == null)
            throw new NullPointerException("Request is null");
        return Biller.builder()
                .id(request.id())
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .baseUrl(request.baseUrl())
                .build();
    }

    public ServiceResponse toServiceResponse(com.az.payment.domain.Service service) {
        if (service == null)
            throw new NullPointerException("Service is null");
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.isPayment(),
                service.getImageUrl()
        );
    }
    public ServiceResponse toServiceResponse(com.az.payment.domain.Service service,String lang) {
        if (service == null)
            throw new NullPointerException("Service is null");
        return new ServiceResponse(
                service.getId(),
               lang.equals("en") ?  service.getName() : service.getNameAr(),
                service.getDescription(),
                service.isPayment(),
                service.getImageUrl()
        );
    }
}
