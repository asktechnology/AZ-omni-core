//package com.az.payment.mapper;
//
//
//import com.az.payment.domain.Biller;
//import com.az.payment.request.BillerRequest;
//import com.az.payment.response.BillerResponse;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//class BillerMapperTest {
//
//    private BillerMapper mapper;
//
//    @BeforeEach
//    void setUp() {
//        mapper = new BillerMapper();
//    }
//
//    @Test
//    public void shouldMapBillerRequestToBiller() {
//        BillerRequest request = new BillerRequest
//                (1, "Zain", "Zain Payment", "http://127.0.0.1/zain", "image");
//        Biller biller = mapper.toBiller(request);
//        assertEquals(request.id(), biller.getId());
//        assertEquals(request.name(), biller.getName());
//        assertEquals(request.baseUrl(), biller.getBaseUrl());
//        assertEquals(request.description(), biller.getDescription());
//        assertEquals(request.imageUrl(), biller.getImageUrl());
//    }
//
//    @Test
//    public void shouldThrowNullPointerExceptionWhenBillerRequestIsNull() {
//        var exp = assertThrows(NullPointerException.class, () -> mapper.toBiller(null));
//        assertEquals("Request is null", exp.getMessage());
//
//    }
//
//    @Test
//    public void shouldThrowIllegalArgumentExceptionWhenBillerIsEmpty() {
//        var exp = assertThrows(NullPointerException.class, () -> mapper.toBillerResponse(null));
//        assertEquals("biller is null", exp.getMessage());
//    }
//
//    @Test
//    public void shouldMapBillerToBillerResponse() {
//        // Given
//        Biller biller = new Biller(1, "zain","", "zain biller def", "http://127.0.0.1/zain", "image", true, null, null);
//        // When
//        BillerResponse billerResponse = mapper.toBillerResponse(biller);
//        // Then
//        assertEquals(billerResponse.id(), biller.getId());
//        assertEquals(billerResponse.name(), biller.getName());
//        assertEquals(billerResponse.description(), biller.getDescription());
//        assertEquals(billerResponse.imageUrl(), biller.getImageUrl());
//    }
//
//}