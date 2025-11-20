//package com.az.payment.service;
//
//import com.az.payment.domain.Biller;
//import com.az.payment.mapper.BillerMapper;
//import com.az.payment.repository.BillerRepository;
//import com.az.payment.request.BillerRequest;
//import com.az.payment.response.ApiResponse;
//import com.az.payment.response.BillerResponse;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.MockitoAnnotations.openMocks;
//
//class BillerServiceTest {
//
//    @InjectMocks
//    // which service we want to test
//    private BillerService service;
//
//    // declare the dependencies
//    @Mock
//    private BillerRepository repository;
//    @Mock
//    private BillerMapper mapper;
//
//
//    @BeforeEach
//    void setUp() {
//        openMocks(this);
//    }
//
//    @Test
//    public void should_successfully_create_biller() {
//        //Given
//        BillerRequest request = new BillerRequest(
//                1,
//                "zain",
//                "zain biller",
//                "http://127.0.0.1/zain",
//                "http://127.0.0.1/images/zain.png"
//        );
//        Biller biller = new Biller(
//                1,
//                "zain",
//                "",
//                "zain biller",
//                "http://127.0.0.1/zain",
//                "http://127.0.0.1/images/zain.png",
//                false,
//                null,
//                null
//        );
//        // mock the call
////        Mockito.when(mapper.to)
//        //when
//        ApiResponse apiResponse = service.createBiller(request);
//        //then
//        assertNotNull(apiResponse);
//        Assertions.assertEquals(apiResponse.getCode(), 0);
//        assertData((BillerResponse) apiResponse.getData(), request);
//    }
//
//    private void assertData(BillerResponse data, BillerRequest request) {
//        assertNotNull(data);
////        assertEquals(data.id(),request.id());
//        assertEquals(data.imageUrl(), request.imageUrl());
//        assertEquals(data.name(), request.name());
//        assertEquals(data.description(), request.description());
//        assertEquals(data.description(), request.description());
//    }
//}