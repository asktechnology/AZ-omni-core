package com.az.payment.service;

import com.az.payment.domain.*;
import com.az.payment.exception.BusinessException;
import com.az.payment.exception.ValidationException;
import com.az.payment.mapper.RequestMapper;
import com.az.payment.mapper.ResponseMapper;
import com.az.payment.repository.*;
import com.az.payment.request.payment.CheckStatusRequest;
import com.az.payment.request.payment.PostCheckRequest;
import com.az.payment.request.payment.ProcessRequest;
import com.az.payment.request.payment.RequestParameter;
import com.az.payment.response.CheckStatusResponse;
import com.az.payment.response.PaymentResponse;
import com.az.payment.response.PaymentResponseField;
import com.az.payment.response.PostCheckResponse;
import com.az.payment.utils.*;
//import com.az.payment.domain.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    @Autowired
    EntityManager entityManager;

    private final ServiceRepository serviceRepository;
    private final TransactionLogRepository transactionLogRepository;

    private final ServiceService service;
    private final RequestMapper requestMapper;
    private final ResponseMapper responseMapper;

    private final TransactionLogUtils transactionLogUtils;
    private final ServiceUtils serviceUtils;
    private final CommonUtils commonUtils;
    private final BillerUtils billerUtils;

    private final ClientApi clientApi;

    private final String billerFailCode="-1";
    private final String timeoutOrUnknown="-2";
    private final String billerAccessFail="-3";
    private final String paymentFail="-4";

    public PaymentResponse processRequest(ProcessRequest request, String locale) {
        log.info("inside PaymentService.processRequest");
        log.info("mobile request : "+request);
        TransactionLog transactionLog = new TransactionLog();
        String omniRRN = "";
        try{
            transactionLog.setResponseMessage("(put main details): \n"+request);//to keep tracking the process
            transactionLog.setBillerId(request.getId());
            transactionLog.setServiceId(request.getServiceId());
            transactionLog.setAccount(request.getAccountFrom());
            transactionLog = transactionLogRepository.saveAndFlush(transactionLog); //Generates ID

            //omniRRN
            transactionLog.setResponseMessage("put main details + (put rrn): \n"+request);//to keep tracking the process
            omniRRN = commonUtils.generateRRN(request.getId(),request.getServiceId());
            transactionLog.setTransactionId(omniRRN);
            log.info("transactionLog Id : "+transactionLog.getId().toString()+" : rrn("+omniRRN+")");

            // Get Service By Service id
            log.info("PaymentService::processRequest Getting service By ID {} ,,,, and locale {}", request.getServiceId(), locale);
            var processService = service.findById(request.getServiceId());

            //check the biller code
            if (request.getId()!=processService.id()){
                log.info("Biller({}) doesn't have service({})",request.getId(),processService.id());
            }
            // should get All configuration Parameters
            // only if process Service got config data
            // call request mapper to handle mapping requests
            log.info("PaymentService::processRequest found Service Named {}: By Id {}", processService.name(), request.getServiceId());

            //do any necessary step that may cause exception before processing payment

            //check and map the request
            transactionLog.setResponseMessage("payment not initiated and Biller not called yet + (mapRequest): \n"+request);//to keep tracking the process
            JSONObject mappedRequest = requestMapper.toServiceRequest(request, processService, omniRRN, locale);
            log.info("PaymentService::processRequest mapped  RequestDTO To JSON Object {}", mappedRequest);

            //add datasource parameters
            transactionLog.setResponseMessage("payment not initiated and Biller not called yet + mapRequest+(DataSource): \n"+request);//to keep tracking the process
            commonUtils.mapDataSourceParameters(request,mappedRequest,omniRRN);

            try{
                transactionLog.setResponseMessage("payment not initiated and Biller not called yet + (parsing mappedRequest): \n"+request);//to keep tracking the process
                transactionLog = transactionLogUtils.parseRequest(transactionLog, mappedRequest.toString());
                transactionLog = transactionLogRepository.saveAndFlush(transactionLog);//needed to generate childes while setting REQ/RES
            } catch (Exception e){
                log.info("error while saving init transactionLog details or parsing req body : "+e.getMessage());
            }

            transactionLog.setResponseMessage("(process payment) but Biller not called yet: \n"+request);//to keep tracking the process
            PaymentResponse bankMapedResponse = new PaymentResponse();
            // call restApi Client with configuration attained from processService Object
            if ((processService.isPayment() && ObjectUtils.isEmpty(request.getAccountFrom()) &&   request.getAccountFrom().length() < 16) ) {

                PaymentResponse mapResponse = new PaymentResponse();

                mapResponse.setFinalStatus(-1);

                //system messages are at billerID = -1
                mapResponse.setResponseCode(-1);
                mapResponse.setResponseMessage("Request invalid");
                log.info("mapResponse : {}",mapResponse);
                return mapResponse;
            }
            if ((processService.isPayment() && !ObjectUtils.isEmpty(request.getAccountFrom()) &&   !(request.getAccountFrom().length() < 10)) ) {
                log.info("processing payment from account : "+request.getAccountFrom());

                bankMapedResponse = payCoreBank(request, omniRRN, locale);
                log.info("bankMapedResponse : {}",bankMapedResponse);

                //if fail return failure
                if (bankMapedResponse.getFinalStatus()==-1){
                    transactionLog.setStatus(paymentFail);
                    transactionLog.setResponseCode(String.valueOf(bankMapedResponse.getResponseCode()));
                    transactionLog.setResponseMessage("Payment Failed with code("+bankMapedResponse.getResponseCode()+"):\n"+bankMapedResponse.getResponseMessage());
                    transactionLogRepository.saveAndFlush(transactionLog);

                    //map the client err msg
                    try{
                        int resCode = bankMapedResponse.getResponseCode();
                        bankMapedResponse.setResponseCode(commonUtils.mapResponseCode(-1, resCode));//-1=pyment system code
                        bankMapedResponse.setResponseMessage(commonUtils.mapResponseMessage(-1, resCode, locale));
                    }
                    catch (ValidationException ve){
                        if(ve.getMessage().contains("does not have equivalent")){
                            log.error("exception while mapping res code/msg:"+ve.getMessage());
                            bankMapedResponse.setResponseCode(Integer.parseInt(paymentFail));
                            bankMapedResponse.setResponseMessage("Payment Failed");
                        }
                        else
                            throw ve;
                    }catch (Exception e){
                        System.out.println("e.getClass():"+e.getClass());
                    }
                    //TODO: make sure the other exceptions caught by the bigger catch

                    bankMapedResponse.setBankReference(omniRRN);
                    return bankMapedResponse;
                }
            }

            //update the log
            transactionLog.setResponseMessage("payment done but (Biller not called yet)");//to keep tracking the process

            //Call the Biller
            JSONObject response = clientApi.callService(mappedRequest, processService, request.getId());

            //update the log
            transactionLog.setResponseMessage("payment done and Biller called successfully but (response not saved yet)");//to keep tracking the process

            try{
                log.info(response.toString());
                transactionLog = transactionLogUtils.parseResponse(transactionLog, response.toString());
                transactionLogRepository.saveAndFlush(transactionLog);
            } catch (Exception e){
                log.info("error while parsing res body or saving final transactionLog details : "+e.getMessage());
            }

            //check biller response status:
            //get the stored biller success/timeout code from DB
            String billerSuccessCode = billerUtils.getBillerSuccessCode(request.getId());
            String billerTimeoutCode = billerUtils.getBillerTimeoutCode(request.getId());

            //get biller response code
            String finalStatus = "00001";//safe case: set success
            String billerResCode = billerSuccessCode;//safe case
            String billerResCodeKey = serviceUtils.getSpecialParameter(request.getServiceId(), "resCode").getExternalKey();

            //check if biller dosn't return the expected response body
            boolean htmlerr=false;
            if(response.has(billerResCodeKey)){//normal reponse body
                billerResCode = String.valueOf(response.getInt(billerResCodeKey));
            }
            else if(response.has("code")) {//invalid reponse body
                log.info("================================="+response.getInt("code"));
                htmlerr = response.getInt("code") != 0;

                //if no htmlerr and no response code then we should put err code and getStatus job will decide the action
                if(!htmlerr){
                    billerResCode = "-1015";
                }
                //else set the returned error code and getStatus job will decide the action
                else{
                    billerResCode = String.valueOf(response.getInt("code"));
                }

                //if timeout set the code and getStatus job will decide the action
                if(response.getInt("code") == -10){
                    finalStatus = "-2";//timeout
                    billerResCode = String.valueOf(response.getInt("code"));
                }
            }
            //if every other failure put err code and getStatus job will decide the action
            else
                billerResCode = "-1016";

            //if there is no stored success code we can't check if the response is success or not
            //safe case: we will not reverse the amount and getStatus job will decide the action when success code have configured
            if(billerSuccessCode =="-1101"){
                billerSuccessCode = "-1017";
                log.info("No success code found in DB for Biller("+request.getId()+")");
            }

            //if timeout(biller didn't response)
            if(finalStatus.equals("-2")){
                //revers will be done by checkstatus job
                log.info("#################### TIME OUT");
                transactionLog.setStatus(processService.isPayment()?finalStatus:"-1");
                transactionLog.setResponseCode(billerResCode);
                transactionLogRepository.saveAndFlush(transactionLog);

                //update jcard
                if(processService.isPayment()){
                    billerUtils.updateBillerTrnSts(omniRRN, transactionLog.getExtRrn(), transactionLog.getResponseCode(), transactionLog.getResponseMessage(), billerSuccessCode);
                }

                //add response code/msg for timeout
                String billerResMsgKey = serviceUtils.getSpecialParameter(request.getServiceId(), "resMsg").getExternalKey();

                response.put(billerResCodeKey,billerTimeoutCode);
                response.put(billerResMsgKey,"Transacion bending");//it will be changed by stored msg at responseMapper.toServiceResponse() line
            }
            //if biller return timeout code
            else if(billerResCode.equals(billerTimeoutCode)){
                log.info("#################### BILLER TIME OUT CODE");
                finalStatus = "-2";//timeout//revers will be done by checkstatus job
                transactionLog.setStatus(processService.isPayment()?finalStatus:"-1");
                transactionLog.setResponseCode(billerResCode);
                transactionLogRepository.saveAndFlush(transactionLog);

                //update jcard
                if(processService.isPayment()){
                    billerUtils.updateBillerTrnSts(omniRRN, transactionLog.getExtRrn(), transactionLog.getResponseCode(), transactionLog.getResponseMessage(), billerSuccessCode);
                }
            }
            //if access to biller fail we should reverse the payment(after checking the status if it's possible)
            else if(htmlerr){
                log.info("#################### HTML ERR");

                long nextService = serviceRepository.findById(request.getServiceId())
                        .orElseThrow(() -> new EntityNotFoundException(String.format("Service with id %s not found", request.getServiceId())))
                        .getAfterServiceId();

                finalStatus=nextService==0?"-3":"-2";//if there is checkstatus service configured revers will be done by checkstatus job
                finalStatus=processService.isPayment()?finalStatus:"-3";//if it is not payment don't reverse
                transactionLog.setStatus(finalStatus);
                transactionLog.setResponseCode(billerResCode);
                transactionLogRepository.saveAndFlush(transactionLog);

                //update jcard
                PaymentResponse paymentResponse = new PaymentResponse();
                if(processService.isPayment()){
                    billerUtils.updateBillerTrnSts(omniRRN, transactionLog.getExtRrn(), transactionLog.getResponseCode(), transactionLog.getResponseMessage(), billerSuccessCode);

                    if(nextService==0){
                        paymentResponse = payCoreBankReverse(request, omniRRN);
                    }
                }

                paymentResponse.setBankReference(omniRRN);
                paymentResponse.setFinalStatus(-3);//system error
//                mapResponseRev.setResponseCode(commonUtils.mapResponseCode(request.getId(),Integer.parseInt(billerResCode)));  commented till putting sys code in DB
//                mapResponseRev.setResponseMessage(commonUtils.mapResponseMessage(request.getId(),Integer.parseInt(billerResCode), locale)); commented till putting sys code in DB
                paymentResponse.setResponseCode("-2".equals(finalStatus)?Integer.parseInt(billerTimeoutCode):-2);//if it needs check status then should return timeoutcode to mobile
                paymentResponse.setResponseMessage("Unknown from Biller, please check transaction history");
                log.info(paymentResponse.toString());
                return paymentResponse;
            }
            //if biller returns fail we should reverse the payment(after checking the status if it's possible)
            else if(!billerResCode.equals(billerSuccessCode)){
                log.info("#################### Biller Failed:Starting reverse process if the service isPayment");

                long nextService = serviceRepository.findById(request.getServiceId())
                        .orElseThrow(() -> new EntityNotFoundException(String.format("Service with id %s not found", request.getServiceId())))
                        .getAfterServiceId();

                finalStatus=nextService==0?"-1":"-2";//if there is checkstatus service configured revers will be done by checkstatus job
                finalStatus=processService.isPayment()?finalStatus:"-1";//if it is not payment don't reverse
                transactionLog.setStatus(finalStatus);
                transactionLogRepository.saveAndFlush(transactionLog);

                PaymentResponse mapResponseRev = new PaymentResponse();
                if(processService.isPayment()) {
                    billerUtils.updateBillerTrnSts(omniRRN, transactionLog.getExtRrn(), transactionLog.getResponseCode(), transactionLog.getResponseMessage(), billerSuccessCode);

                    if(nextService==0){
                        mapResponseRev = payCoreBankReverse(request, omniRRN);
                    }
                }

                mapResponseRev.setResponseParams(responseMapper
                                                    .toServiceResponse(response, processService, request.getId(),locale)
                                                        .getResponseParams());
                mapResponseRev.setBankReference(omniRRN);
                mapResponseRev.setFinalStatus(-1);
                mapResponseRev.setResponseCode(commonUtils.mapResponseCode(request.getId(),Integer.parseInt(billerResCode)));
                mapResponseRev.setResponseMessage(commonUtils.mapResponseMessage(request.getId(),Integer.parseInt(billerResCode), locale));
                log.info(mapResponseRev.toString());
                return mapResponseRev;
            }
            //if success
            else{
                log.info("#################### SUCCESS");
                transactionLog.setStatus(finalStatus="00001");
                transactionLogRepository.saveAndFlush(transactionLog);

                //update jcard
                if(processService.isPayment()){
                    billerUtils.updateBillerTrnSts(omniRRN, transactionLog.getExtRrn(), transactionLog.getResponseCode(), transactionLog.getResponseMessage(), billerSuccessCode);
                }
            }

            //return response to mobile:
            PaymentResponse mappedResponse = new PaymentResponse();
            log.info("PaymentService::processResponse  Response returned From restApi {}", response);
            try{
                mappedResponse = responseMapper.toServiceResponse(response, processService, request.getId(),locale);
            }catch (Exception e){
                //the transaction is completed and the error related to the server
                log.error("Error while validating response details: "+e.getMessage());
                mappedResponse.setResponseCode(commonUtils.mapResponseCode(request.getId(),Integer.parseInt(billerResCode)));
                mappedResponse.setResponseMessage(commonUtils.mapResponseMessage(request.getId(),Integer.parseInt(billerResCode), locale));
            }
            log.info("PaymentService::processResponse mapped from toServiceResponse {}", mappedResponse);

            //add next service if the status is success
            if("00001".equals(finalStatus))
                mappedResponse = requestMapper.toClientResponse(mappedResponse, request,locale);
            else
                mappedResponse.setFinalStatus(Long.parseLong(finalStatus));

            if (processService.isPayment())
                System.out.println("bankMapedResponse.getBankReference()"+bankMapedResponse.getBankReference());

            mappedResponse.setBankReference(omniRRN);

            log.info("mapResponse --->: {}",mappedResponse);
            return mappedResponse;
        }
        catch(Exception e){
            transactionLog = transactionLogRepository.saveAndFlush(transactionLog);
            log.error("SYSTEM ERR: "+e.getMessage());
//            throw  new BusinessException("Error Occurred, please check transaction history ");

            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setBankReference(omniRRN);
            paymentResponse.setFinalStatus(-1);
            paymentResponse.setResponseCode(10);//error
            paymentResponse.setResponseMessage("Error Occurred, please check transaction history");

            log.info(paymentResponse.toString());
            return paymentResponse;
        }
    }

    private PaymentResponse payCoreBank(ProcessRequest request, String omniRRN,String locale){
        PaymentResponse mapResponse = new PaymentResponse();

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("processhbimdwbillpay_Fees_2025");
        Map<Long, String> paramMap = request.getParameters().stream()
                .collect(Collectors.toMap(RequestParameter::id, RequestParameter::value));

        List<Parameter> parameters = serviceRepository.findById(request.getServiceId()).get().getParameters();
        //get the id of the parameter where parameter.getIsAmount()==1
        Optional<Parameter> amountParameter = parameters.stream()
                .filter(parameter -> parameter.getIsAmount() == 1)
                .findFirst();
        if (!amountParameter.isPresent()) {
            //Amount parameter not found
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage("Cannot Handle Payment, Amount not found");
            return mapResponse;
        }
        //get the id of the parameter where parameter.getIsBillid()==1
        Optional<Parameter> billIdParameter = parameters.stream()
                .filter(parameter -> parameter.getIsBillid() == 1)
                .findFirst();
        if (!billIdParameter.isPresent()) {
            //Bill ID parameter not found
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage("Cannot Handle Payment, Voucher ID not found");
            return mapResponse;
        }
        String actfrm = request.getAccountFrom();
        String amount = paramMap.get(amountParameter.get().getId());
        String exRRN = omniRRN;
        String vNum = paramMap.get(billIdParameter.get().getId());
        String billerId = ""+request.getId();
        String serviceId = ""+request.getServiceId();

        // Register IN parameters
        query.registerStoredProcedureParameter("IP_actfrm", String.class, ParameterMode.IN).setParameter("IP_actfrm",actfrm);
        query.registerStoredProcedureParameter("IP_AMT", String.class, ParameterMode.IN).setParameter("IP_AMT",amount);
        query.registerStoredProcedureParameter("IP_OMNI_RRN", String.class, ParameterMode.IN).setParameter("IP_OMNI_RRN",omniRRN);
        query.registerStoredProcedureParameter("IP_Ex_RRN", String.class, ParameterMode.IN).setParameter("IP_Ex_RRN",exRRN);
        query.registerStoredProcedureParameter("IP_Channel_ID", String.class, ParameterMode.IN).setParameter("IP_Channel_ID","Sahil");
        query.registerStoredProcedureParameter("IP_Voucher_Number", String.class, ParameterMode.IN).setParameter("IP_Voucher_Number",vNum);
        query.registerStoredProcedureParameter("IP_Biller_ID", String.class, ParameterMode.IN).setParameter("IP_Biller_ID",billerId);
        query.registerStoredProcedureParameter("IP_Biller_SRV_ID", String.class, ParameterMode.IN).setParameter("IP_Biller_SRV_ID",serviceId);
        query.registerStoredProcedureParameter("IP_LANG", String.class, ParameterMode.IN).setParameter("IP_LANG",locale);

        // Register OUT parameters
        query.registerStoredProcedureParameter("OP_RSP_Code", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_RSP_Msg", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_CB_Date", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_CB_RRN", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_Balance", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_ErrCode", Integer.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_ErrMsg", String.class, ParameterMode.OUT);

        // Execute the stored procedure
        query.execute();

        // Retrieve OUT parameters
        String rspCode = (String) query.getOutputParameterValue("OP_RSP_Code");
        String rspMsg = (String) query.getOutputParameterValue("OP_RSP_Msg");
//        String cbDate = (String) query.getOutputParameterValue("OP_CB_Date");
        String cbRrn = (String) query.getOutputParameterValue("OP_CB_RRN");
//        String balance = (String) query.getOutputParameterValue("OP_Balance");
        Integer errCode = (Integer) query.getOutputParameterValue("OP_ErrCode");
        String errMsg = (String) query.getOutputParameterValue("OP_ErrMsg");


        //if fail
        if(!rspCode.equals("00001")){
            log.info("ERROR[payCoreBank]:"+rspCode+":"+rspMsg+":"+errCode+":"+errMsg);
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(Integer.parseInt(rspCode));
            mapResponse.setResponseMessage("rspMsg:"+rspMsg+"\n"+"sqlCode("+errCode+"):"+errMsg);
            return mapResponse;
        }

        mapResponse.setBankReference(cbRrn);
        mapResponse.setFinalStatus(1);
        return mapResponse;
    }

    private PaymentResponse payCoreBank_Old(ProcessRequest request, String omniRRN,String locale){
        PaymentResponse mapResponse = new PaymentResponse();

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("processhbimdwbillpayment_2025");
        Map<Long, String> paramMap = request.getParameters().stream()
                .collect(Collectors.toMap(RequestParameter::id, RequestParameter::value));

        List<Parameter> parameters = serviceRepository.findById(request.getServiceId()).get().getParameters();
        //get the id of the parameter where parameter.getIsAmount()==1
        Optional<Parameter> amountParameter = parameters.stream()
                .filter(parameter -> parameter.getIsAmount() == 1)
                .findFirst();
        if (!amountParameter.isPresent()) {
            //Amount parameter not found
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage("Cannot Handle Payment, Amount not found");
            return mapResponse;
        }
        //get the id of the parameter where parameter.getIsBillid()==1
        Optional<Parameter> billIdParameter = parameters.stream()
                .filter(parameter -> parameter.getIsBillid() == 1)
                .findFirst();
        if (!billIdParameter.isPresent()) {
            //Bill ID parameter not found
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage("Cannot Handle Payment, Voucher ID not found");
            return mapResponse;
        }
        String actfrm = request.getAccountFrom();
        String amount = paramMap.get(amountParameter.get().getId());
        String exRRN = omniRRN;
        String vNum = paramMap.get(billIdParameter.get().getId());
        String billerId = ""+request.getId();
        String serviceId = ""+request.getServiceId();

        // Register IN parameters
        query.registerStoredProcedureParameter("IP_actfrm", String.class, ParameterMode.IN).setParameter("IP_actfrm",actfrm);
        query.registerStoredProcedureParameter("IP_AMT", String.class, ParameterMode.IN).setParameter("IP_AMT",amount);
        query.registerStoredProcedureParameter("IP_OMNI_RRN", String.class, ParameterMode.IN).setParameter("IP_OMNI_RRN",omniRRN);
        query.registerStoredProcedureParameter("IP_Ex_RRN", String.class, ParameterMode.IN).setParameter("IP_Ex_RRN",exRRN);
        query.registerStoredProcedureParameter("IP_Channel_ID", String.class, ParameterMode.IN).setParameter("IP_Channel_ID","Sahil");
        query.registerStoredProcedureParameter("IP_Voucher_Number", String.class, ParameterMode.IN).setParameter("IP_Voucher_Number",vNum);
        query.registerStoredProcedureParameter("IP_Biller_ID", String.class, ParameterMode.IN).setParameter("IP_Biller_ID",billerId);
        query.registerStoredProcedureParameter("IP_Biller_SRV_ID", String.class, ParameterMode.IN).setParameter("IP_Biller_SRV_ID",serviceId);
        query.registerStoredProcedureParameter("IP_LANG", String.class, ParameterMode.IN).setParameter("IP_LANG",locale);

        // Register OUT parameters
        query.registerStoredProcedureParameter("OP_RSP_Code", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_RSP_Msg", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_CB_Date", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_CB_RRN", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_Balance", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_ErrCode", Integer.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_ErrMsg", String.class, ParameterMode.OUT);

        // Execute the stored procedure
        query.execute();

        // Retrieve OUT parameters
        String rspCode = (String) query.getOutputParameterValue("OP_RSP_Code");
        String rspMsg = (String) query.getOutputParameterValue("OP_RSP_Msg");
//        String cbDate = (String) query.getOutputParameterValue("OP_CB_Date");
        String cbRrn = (String) query.getOutputParameterValue("OP_CB_RRN");
//        String balance = (String) query.getOutputParameterValue("OP_Balance");
        Integer errCode = (Integer) query.getOutputParameterValue("OP_ErrCode");
        String errMsg = (String) query.getOutputParameterValue("OP_ErrMsg");


        //if fail
        if(!rspCode.equals("00001")){
            log.info("ERROR[payCoreBank]:"+rspCode+":"+rspMsg+":"+errCode+":"+errMsg);
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage(rspMsg);
            return mapResponse;
        }

        mapResponse.setBankReference(cbRrn);
        mapResponse.setFinalStatus(1);
        return mapResponse;
    }

    private PaymentResponse payCoreBankReverse(ProcessRequest request, String omniRRN/*,String locale*/){
        PaymentResponse mapResponse = new PaymentResponse();

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("processhbimdwbillreversal_fees");
        Map<Long, String> paramMap = request.getParameters().stream()
                .collect(Collectors.toMap(RequestParameter::id, RequestParameter::value));

        List<Parameter> parameters = serviceRepository.findById(request.getServiceId()).get().getParameters();
        //get the id of the parameter where parameter.getIsAmount()==1
        Optional<Parameter> amountParameter = parameters.stream()
                .filter(parameter -> parameter.getIsAmount() == 1)
                .findFirst();
        if (!amountParameter.isPresent()) {
            //Amount parameter not found
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage("Cannot Handle Payment, Amount not found");
            return mapResponse;
        }
        //get the id of the parameter where parameter.getIsBillid()==1
        Optional<Parameter> billIdParameter = parameters.stream()
                .filter(parameter -> parameter.getIsBillid() == 1)
                .findFirst();
        if (!billIdParameter.isPresent()) {
            //Bill ID parameter not found
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage("Cannot Handle Payment, Voucher ID not found");
            return mapResponse;
        }
        String actfrm = request.getAccountFrom();
        String amount = paramMap.get(amountParameter.get().getId());
        String exRRN = omniRRN;
        String vNum = paramMap.get(billIdParameter.get().getId());
        String billerId = ""+request.getId();
        String serviceId = ""+request.getServiceId();

        // Register IN parameters
        query.registerStoredProcedureParameter("IP_actfrm", String.class, ParameterMode.IN).setParameter("IP_actfrm",actfrm);
        query.registerStoredProcedureParameter("IP_AMT", String.class, ParameterMode.IN).setParameter("IP_AMT",amount);
        query.registerStoredProcedureParameter("IP_OMNI_RRN", String.class, ParameterMode.IN).setParameter("IP_OMNI_RRN",omniRRN);
        query.registerStoredProcedureParameter("IP_Ex_RRN", String.class, ParameterMode.IN).setParameter("IP_Ex_RRN",exRRN);
        query.registerStoredProcedureParameter("IP_Channel_ID", String.class, ParameterMode.IN).setParameter("IP_Channel_ID","Sahil");
        query.registerStoredProcedureParameter("IP_Voucher_Number", String.class, ParameterMode.IN).setParameter("IP_Voucher_Number",vNum);
        query.registerStoredProcedureParameter("IP_Biller_ID", String.class, ParameterMode.IN).setParameter("IP_Biller_ID",billerId);
        query.registerStoredProcedureParameter("IP_Biller_SRV_ID", String.class, ParameterMode.IN).setParameter("IP_Biller_SRV_ID",serviceId);
//        query.registerStoredProcedureParameter("IP_LANG", String.class, ParameterMode.IN).setParameter("IP_LANG",locale);

        // Register OUT parameters
        query.registerStoredProcedureParameter("OP_RSP_Code", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_RSP_Msg", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_CB_Date", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_CB_RRN", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_Balance", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_ErrCode", Integer.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_ErrMsg", String.class, ParameterMode.OUT);

        // Execute the stored procedure
        query.execute();

        // Retrieve OUT parameters
        String rspCode = (String) query.getOutputParameterValue("OP_RSP_Code");
        String rspMsg = (String) query.getOutputParameterValue("OP_RSP_Msg");
        String cbDate = (String) query.getOutputParameterValue("OP_CB_Date");
        String cbRrn = (String) query.getOutputParameterValue("OP_CB_RRN");
        String balance = (String) query.getOutputParameterValue("OP_Balance");
        Integer errCode = (Integer) query.getOutputParameterValue("OP_ErrCode");
        String errMsg = (String) query.getOutputParameterValue("OP_ErrMsg");


        //if fail
        if(!rspCode.equals("00001")){
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage(rspMsg);
            return mapResponse;
        }

        mapResponse.setFinalStatus(1);
        return mapResponse;
    }

    private PaymentResponse payCoreBankReverse_old(ProcessRequest request, String omniRRN/*,String locale*/){
        PaymentResponse mapResponse = new PaymentResponse();

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("processhbimdwbillreversal_2025");
        Map<Long, String> paramMap = request.getParameters().stream()
                .collect(Collectors.toMap(RequestParameter::id, RequestParameter::value));

        List<Parameter> parameters = serviceRepository.findById(request.getServiceId()).get().getParameters();
        //get the id of the parameter where parameter.getIsAmount()==1
        Optional<Parameter> amountParameter = parameters.stream()
                .filter(parameter -> parameter.getIsAmount() == 1)
                .findFirst();
        if (!amountParameter.isPresent()) {
            //Amount parameter not found
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage("Cannot Handle Payment, Amount not found");
            return mapResponse;
        }
        //get the id of the parameter where parameter.getIsBillid()==1
        Optional<Parameter> billIdParameter = parameters.stream()
                .filter(parameter -> parameter.getIsBillid() == 1)
                .findFirst();
        if (!billIdParameter.isPresent()) {
            //Bill ID parameter not found
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage("Cannot Handle Payment, Voucher ID not found");
            return mapResponse;
        }
        String actfrm = request.getAccountFrom();
        String amount = paramMap.get(amountParameter.get().getId());
        String exRRN = omniRRN;
        String vNum = paramMap.get(billIdParameter.get().getId());
        String billerId = ""+request.getId();
        String serviceId = ""+request.getServiceId();

        // Register IN parameters
        query.registerStoredProcedureParameter("IP_actfrm", String.class, ParameterMode.IN).setParameter("IP_actfrm",actfrm);
        query.registerStoredProcedureParameter("IP_AMT", String.class, ParameterMode.IN).setParameter("IP_AMT",amount);
        query.registerStoredProcedureParameter("IP_OMNI_RRN", String.class, ParameterMode.IN).setParameter("IP_OMNI_RRN",omniRRN);
        query.registerStoredProcedureParameter("IP_Ex_RRN", String.class, ParameterMode.IN).setParameter("IP_Ex_RRN",exRRN);
        query.registerStoredProcedureParameter("IP_Channel_ID", String.class, ParameterMode.IN).setParameter("IP_Channel_ID","Sahil");
        query.registerStoredProcedureParameter("IP_Voucher_Number", String.class, ParameterMode.IN).setParameter("IP_Voucher_Number",vNum);
        query.registerStoredProcedureParameter("IP_Biller_ID", String.class, ParameterMode.IN).setParameter("IP_Biller_ID",billerId);
        query.registerStoredProcedureParameter("IP_Biller_SRV_ID", String.class, ParameterMode.IN).setParameter("IP_Biller_SRV_ID",serviceId);
//        query.registerStoredProcedureParameter("IP_LANG", String.class, ParameterMode.IN).setParameter("IP_LANG",locale);

        // Register OUT parameters
        query.registerStoredProcedureParameter("OP_RSP_Code", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_RSP_Msg", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_CB_Date", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_CB_RRN", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_Balance", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_ErrCode", Integer.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("OP_ErrMsg", String.class, ParameterMode.OUT);

        // Execute the stored procedure
        query.execute();

        // Retrieve OUT parameters
        String rspCode = (String) query.getOutputParameterValue("OP_RSP_Code");
        String rspMsg = (String) query.getOutputParameterValue("OP_RSP_Msg");
        String cbDate = (String) query.getOutputParameterValue("OP_CB_Date");
        String cbRrn = (String) query.getOutputParameterValue("OP_CB_RRN");
        String balance = (String) query.getOutputParameterValue("OP_Balance");
        Integer errCode = (Integer) query.getOutputParameterValue("OP_ErrCode");
        String errMsg = (String) query.getOutputParameterValue("OP_ErrMsg");


        //if fail
        if(!rspCode.equals("00001")){
            mapResponse.setFinalStatus(-1);
            mapResponse.setResponseCode(-1);
            mapResponse.setResponseMessage(rspMsg);
            return mapResponse;
        }

        mapResponse.setFinalStatus(1);
        return mapResponse;
    }

    public PostCheckResponse postCheck(PostCheckRequest request, String locale){
        PostCheckResponse postCheckResponse = new PostCheckResponse();

        //TODO : processRequest used or passing data to the methods. methods should be changed to accept data in general way not as processRequest
        ProcessRequest processRequest = new ProcessRequest();
        processRequest.setId(request.getId());
        processRequest.setServiceId(request.getServiceId());
        processRequest.setParameters(request.getParameters());

        PaymentResponse paymentResponse = processRequest(processRequest, locale);

        postCheckResponse.setResponseParams(paymentResponse.getResponseParams());
        postCheckResponse.setResponseMessage(paymentResponse.getResponseMessage());
        postCheckResponse.setResponseCode(paymentResponse.getResponseCode());
        postCheckResponse.setFinalStatus(paymentResponse.getFinalStatus());
        postCheckResponse.setBankReference(paymentResponse.getBankReference());

        return postCheckResponse;

    }

    public CheckStatusResponse checkStatus(CheckStatusRequest request, String locale){
        log.info("inside PaymentService.checkStatus: "+request);
        CheckStatusResponse checkStatusResponse = new CheckStatusResponse();
        TransactionLog checkTransactionLog = new TransactionLog();
        TransactionLog transactionLog = new TransactionLog();
        String omniRRN = "";
        try{
            try{
                transactionLog = transactionLogRepository.findByTransactionId(request.getTransactionId()).get();
            }catch (NoSuchElementException e){
                log.info("Original transaction not found : "+request.getTransactionId());

                //while we didn't know the check service the service id has been set to 0
                omniRRN = "".equals(omniRRN)?commonUtils.generateRRN(transactionLog.getBillerId(),0):omniRRN;

                checkTransactionLog.setTransactionId(omniRRN);
                checkTransactionLog.setStatus("-1");
                checkTransactionLog.setResponseCode("10");
                checkTransactionLog.setResponseMessage("Original transaction not found : "+request);
                checkTransactionLog.setBillerId(transactionLog.getBillerId());
                checkTransactionLog.setAccount(transactionLog.getAccount());
                checkTransactionLog = transactionLogRepository.saveAndFlush(checkTransactionLog);

                checkStatusResponse.setCheckReqId(omniRRN);
                checkStatusResponse.setTransactionId(transactionLog.getTransactionId());
                checkStatusResponse.setResponseCode(10);
                checkStatusResponse.setResponseMessage("Original transaction not found");

                log.info(checkStatusResponse.toString());
                checkStatusResponse.setTrnStatusDisc("");
                checkStatusResponse.setTrnDetails(null);
                return checkStatusResponse;
            }

            System.out.println("getCheckStatusService");
            com.az.payment.domain.Service checkService = serviceUtils.getCheckStatusService(transactionLog.getServiceId());
            System.out.println("after service : "+checkService);

            long billerId = transactionLog.getBillerId();
            long serviceId = checkService.getId();
            String accountFrom = transactionLog.getAccount();

            //TODO: complete the scenario of if the status already fetched by the job
//            //trransaction is success after the job gets the status
//            if("00001".equals(transactionLog.getStatus())){
//                log.info("the check already done by the job and the staus is success");
//                checkTransactionLog.setResponseMessage("the check already done by the job and the status is success");//to keep tracking the process
//                checkTransactionLog.setBillerId(billerId);
//                checkTransactionLog.setServiceId(serviceId);
//                checkTransactionLog.setAccount(accountFrom);
//                checkTransactionLog = transactionLogRepository.saveAndFlush(checkTransactionLog); //Generates ID
//
//                List<RequestParameter> parameters = new ArrayList<>();
//                //fill request parameters
//                for (Parameter parameter: checkService.getParameters()) {
//                    String key = parameter.getInternalKey();
//
//                    Object objValue = commonUtils.findValueByKey(new JSONObject(transactionLog.getRequest()), key);
//                    String value = (objValue == null) ? "" : objValue.toString();
//
//                    RequestParameter param = new RequestParameter(parameter.getId(), parameter.getExternalKey(), value);
//                    parameters.add(param);
//                }
//
//                checkTransactionLog.setResponseMessage("(put main details): \n"+parameters);//to keep tracking the process
//                checkTransactionLog.setBillerId(billerId);
//                checkTransactionLog.setServiceId(serviceId);
//                checkTransactionLog.setAccount(accountFrom);
//                checkTransactionLog = transactionLogRepository.saveAndFlush(checkTransactionLog); //Generates ID
//
//                //omniRRN
//                checkTransactionLog.setResponseMessage("put main details + (put rrn): \n"+parameters.toString());//to keep tracking the process
//                omniRRN = commonUtils.generateRRN(billerId,serviceId);
//                checkTransactionLog.setTransactionId(omniRRN);
//                log.info("transactionLog Id : "+checkTransactionLog.getId().toString()+" : rrn("+omniRRN+")");
//
//                //return response to mobile:
//                checkStatusResponse.setCheckReqId(omniRRN);
//                checkStatusResponse.setTransactionId(transactionLog.getTransactionId());
//
//                //TODO: the "trnStatus" is hardcoded but should be configured in DB or config file
//                String trnStatusExKey = checkService.getParameters().stream().filter(parameter -> "trnStatus".equals(parameter.getInternalKey())).findFirst().get().getExternalKey();
//                Long trnStatus = response.has(trnStatusExKey)? Long.parseLong((String) response.get(trnStatusExKey)):null;
//                trnStatus = trnStatus == null?null:(trnStatus.longValue()==Long.parseLong(billerSuccessCode)?0L:(trnStatus.longValue()==Long.parseLong(billerTimeoutCode)?20L:10L));
//                String trnStatusDiscExKey = checkService.getParameters().stream().filter(parameter -> "trnStatusDisc".equals(parameter.getInternalKey())).findFirst().get().getExternalKey();
//                String trnStatusDisc = response.has(trnStatusExKey)? (String) response.get(trnStatusDiscExKey):null;
//
//                checkStatusResponse.setResponseCode(0);
//                checkStatusResponse.setResponseMessage("Success");
//                checkStatusResponse.setTrnStatus(trnStatus);
//                checkStatusResponse.setTrnStatusDisc(trnStatusDisc);
//                List<PaymentResponseField> trnDetails = responseMapper.toServiceResponse(response, processService, billerId,locale).getResponseParams();
//                log.info("PaymentService::checkStatus  Response returned From restApi {}", response);
//                log.info("PaymentService::checkStatus mapped from toServiceResponse {}", trnDetails);
//                checkStatusResponse.setTrnDetails(trnDetails);
//
//            } else if(!"-2".equals(transactionLog.getStatus())){//if it's not timeout and not success then the transaction is failed after the job gets the status
//
//            }

            if(checkService.getId()!=0){

                List<RequestParameter> parameters = new ArrayList<>();
                //fill request parameters
                for (Parameter parameter: checkService.getParameters()) {
                    if(parameter.getParamType()==1 || parameter.getParamType()==3) {

                        String key = parameter.getInternalKey();//internal[checkSer]=ext[orgnSer]
                        String value = "";
                        JSONObject OrgRequest = new JSONObject(transactionLog.getRequest());
                        if(OrgRequest.has(key)){
                            Object objValue = OrgRequest.get(key);
                            value = (objValue == null) ? "" : objValue.toString();
                        } else{
                            continue;
                        }

                        RequestParameter param = new RequestParameter(parameter.getId(), key, value);
                        parameters.add(param);
                    }
                }


                log.info("call biller for service("+serviceId+") : "+parameters.toString());
                checkTransactionLog.setResponseMessage("(put main details): \n"+parameters);//to keep tracking the process
                checkTransactionLog.setBillerId(billerId);
                checkTransactionLog.setServiceId(serviceId);
                checkTransactionLog.setAccount(accountFrom);
                checkTransactionLog = transactionLogRepository.saveAndFlush(checkTransactionLog); //Generates ID

                //omniRRN
                checkTransactionLog.setResponseMessage("put main details + (put rrn): \n"+parameters.toString());//to keep tracking the process
                omniRRN = commonUtils.generateRRN(billerId,serviceId);
                checkTransactionLog.setTransactionId(omniRRN);
                log.info("transactionLog Id : "+checkTransactionLog.getId().toString()+" : rrn("+omniRRN+")");

                // Get Service By Service id
                log.info("PaymentService::callService Getting service By ID {} ,,,, and locale {}", serviceId, locale);
                var processService = service.findById(serviceId);

                //check the biller code
                if (billerId!=processService.id()){
                    log.info("Biller({}) doesn't have service({})",billerId,processService.id());
                }
                // should get All configuration Parameters
                // only if process Service got config data
                // call request mapper to handle mapping requests
                log.info("PaymentService::checkStatus found Service Named {}: By Id {}", processService.name(), serviceId);

                //do any necessary step that may cause exception before processing payment

                //check and map the request
                checkTransactionLog.setResponseMessage("Biller not called yet + (mapRequest): \n"+parameters.toString());//to keep tracking the process
                ProcessRequest processRequest = new ProcessRequest(billerId, serviceId, parameters, accountFrom);
                JSONObject mappedRequest = requestMapper.toServiceRequest(processRequest, processService, omniRRN, locale);
                log.info("PaymentService::checkStatus mapped  RequestDTO To JSON Object {}", mappedRequest);

                //add datasource parameters
                checkTransactionLog.setResponseMessage("Biller not called yet + mapRequest+(DataSource): \n"+processRequest);//to keep tracking the process
                commonUtils.mapDataSourceParameters(processRequest,mappedRequest,omniRRN);

                try{
                    checkTransactionLog.setResponseMessage("Biller not called yet + (parsing mappedRequest): \n"+processRequest);//to keep tracking the process
                    checkTransactionLog = transactionLogUtils.parseRequest(checkTransactionLog, mappedRequest.toString());
                    checkTransactionLog = transactionLogRepository.saveAndFlush(checkTransactionLog);//needed to generate childes while setting REQ/RES
                } catch (Exception e){
                    log.info("error while saving init transactionLog details or parsing req body : "+e.getMessage());
                }

                checkTransactionLog.setResponseMessage("Biller not called yet: \n"+processRequest);//to keep tracking the process
                PaymentResponse bankMapedResponse = new PaymentResponse();
                // Call restApi Client with configuration attained from processService Object
                JSONObject response = clientApi.callService(mappedRequest, processService, processRequest.getId());

                //update the log
                checkTransactionLog.setResponseMessage("Biller called successfully but (response not saved yet)");//to keep tracking the process

                //parse the response
                try{
                    log.info(response.toString());
                    checkTransactionLog = transactionLogUtils.parseResponse(checkTransactionLog, response.toString());
                    transactionLogRepository.saveAndFlush(checkTransactionLog);
                } catch (Exception e){
                    log.info("error while parsing res body or saving final transactionLog details : "+e.getMessage());
                }

                //check biller response status:
                //get the stored biller success/timeout code from DB
                String billerSuccessCode = billerUtils.getBillerSuccessCode(processRequest.getId());
                String billerTimeoutCode = billerUtils.getBillerTimeoutCode(processRequest.getId());

                //get biller response code
                String finalStatus = "00001";//safe case: set success
                String billerResCode = billerSuccessCode;//safe case
                String billerResCodeKey = serviceUtils.getSpecialParameter(processRequest.getServiceId(), "resCode").getExternalKey();

                //check if biller dosn't return the expected response body
                boolean htmlerr=false;
                if(response.has(billerResCodeKey)){//normal reponse body
                    billerResCode = String.valueOf(response.getInt(billerResCodeKey));
                }
                else if(response.has("code")) {//invalid reponse body
                    log.info("================================="+response.getInt("code"));
                    htmlerr = response.getInt("code") != 0;

                    //if no htmlerr and no response code then we should put err code and getStatus job will decide the action
                    if(!htmlerr){
                        billerResCode = "-1015";
                    }
                    //else set the returned error code and getStatus job will decide the action
                    else{
                        billerResCode = String.valueOf(response.getInt("code"));
                    }

                    //if timeout set the code and getStatus job will decide the action
                    if(response.getInt("code") == -10){
                        finalStatus = "-2";//timeout
                        billerResCode = String.valueOf(response.getInt("code"));
                    }
                }
                //if every other failure put err code and getStatus job will decide the action
                else
                    billerResCode = "-1016";

                //if there is no stored success code we can't check if the response is success or not
                //safe case: we will not reverse the amount and getStatus job will decide the action when success code have configured
                if(billerSuccessCode =="-1101"){
                    billerSuccessCode = "-1017";
                    log.info("No success code found in DB for Biller("+processRequest.getId()+")");
                }

                //return response to mobile:
                checkStatusResponse.setCheckReqId(omniRRN);
                checkStatusResponse.setTransactionId(transactionLog.getTransactionId());

                //TODO: the "trnStatus" is hardcoded but should be configured in DB or config file
                String trnStatusExKey = checkService.getParameters().stream().filter(parameter -> "trnStatus".equals(parameter.getInternalKey())).findFirst().get().getExternalKey();
                Long trnStatus = response.has(trnStatusExKey)? Long.parseLong((String) response.get(trnStatusExKey)):null;
                trnStatus = trnStatus == null?null:(trnStatus.longValue()==Long.parseLong(billerSuccessCode)?0L:(trnStatus.longValue()==Long.parseLong(billerTimeoutCode)?20L:10L));
                String trnStatusDiscExKey = checkService.getParameters().stream().filter(parameter -> "trnStatusDisc".equals(parameter.getInternalKey())).findFirst().get().getExternalKey();
                String trnStatusDisc = response.has(trnStatusExKey)? (String) response.get(trnStatusDiscExKey):null;

                //TODO: the returning codes 0,10,20 are hardcoded but should be configured in DB or config file
                //if timeout(biller didn't response)
                if(finalStatus.equals("-2")){
                    checkStatusResponse.setResponseCode(20);
                    checkStatusResponse.setResponseMessage("Biller not responding");
                }
                //if biller return timeout code
                else if(billerResCode.equals(billerTimeoutCode)){
                    checkStatusResponse.setResponseMessage(billerResCodeKey.equals(trnStatusExKey)?"Success":"Biller returned with timeout");
                    checkStatusResponse.setResponseCode(0);
                    checkStatusResponse.setTrnStatus(trnStatus);
                    checkStatusResponse.setTrnStatusDisc(trnStatusDisc);
                    List<PaymentResponseField> trnDetails = responseMapper.toServiceResponse(response, processService, billerId,locale).getResponseParams();
                    log.info("PaymentService::checkStatus  Response returned From restApi {}", response);
                    log.info("PaymentService::checkStatus mapped from toServiceResponse {}", trnDetails);
                    checkStatusResponse.setTrnDetails(trnDetails);
                }
                //if access to biller fail
                else if(htmlerr){
                    checkStatusResponse.setResponseCode(10);
                    checkStatusResponse.setResponseMessage("Process Failed");
                }
                //if biller returns fail
                else if(!billerResCode.equals(billerSuccessCode)){
                    if(billerResCodeKey.equals(trnStatusExKey)){
                        checkStatusResponse.setResponseCode(0);
                        checkStatusResponse.setResponseMessage("Success");
                        checkStatusResponse.setTrnStatus(trnStatus);
                        checkStatusResponse.setTrnStatusDisc(trnStatusDisc);
                        List<PaymentResponseField> trnDetails = responseMapper.toServiceResponse(response, processService, billerId,locale).getResponseParams();
                        log.info("PaymentService::checkStatus  Response returned From restApi {}", response);
                        log.info("PaymentService::checkStatus mapped from toServiceResponse {}", trnDetails);
                        checkStatusResponse.setTrnDetails(trnDetails);
                    } else {
                        checkStatusResponse.setResponseCode(10);
                        checkStatusResponse.setResponseMessage("Biller returned with fail");
                    }
                }
                //if success
                else{
                    checkStatusResponse.setResponseCode(0);
                    checkStatusResponse.setResponseMessage("Success");
                    checkStatusResponse.setTrnStatus(trnStatus);
                    checkStatusResponse.setTrnStatusDisc(trnStatusDisc);
                    List<PaymentResponseField> trnDetails = responseMapper.toServiceResponse(response, processService, billerId,locale).getResponseParams();
                    log.info("PaymentService::checkStatus  Response returned From restApi {}", response);
                    log.info("PaymentService::checkStatus mapped from toServiceResponse {}", trnDetails);
                    checkStatusResponse.setTrnDetails(trnDetails);
                }

                return checkStatusResponse;
            }else{
                log.info("check Status service not configured "+request);

                //while we didn't know the check service the service id has been set to 0
                omniRRN = "".equals(omniRRN)?commonUtils.generateRRN(transactionLog.getBillerId(),0):omniRRN;

                checkTransactionLog.setTransactionId(omniRRN);
                checkTransactionLog.setStatus("-1");
                checkTransactionLog.setResponseCode("10");
                checkTransactionLog.setResponseMessage("check Status service not configured"+request);
                checkTransactionLog.setBillerId(transactionLog.getBillerId());
                checkTransactionLog.setAccount(transactionLog.getAccount());
                checkTransactionLog = transactionLogRepository.saveAndFlush(checkTransactionLog);

                checkStatusResponse.setCheckReqId(omniRRN);
                checkStatusResponse.setTransactionId(transactionLog.getTransactionId());
                checkStatusResponse.setResponseCode(10);
                checkStatusResponse.setResponseMessage("Check status not allowed for this service");

                log.info(checkStatusResponse.toString());
                checkStatusResponse.setTrnStatusDisc("");
                checkStatusResponse.setTrnDetails(null);
                return checkStatusResponse;
            }
        }
        catch(Exception e){
            log.info("An Error occured while processing the request "+request);
            log.info("Exception: "+e.getMessage());
            log.error(omniRRN+": "+e.getClass());

            //check if omniRRN not generated
            //while we didn't know the check service the service id has been set to 0
            omniRRN = "".equals(omniRRN)?commonUtils.generateRRN(transactionLog.getBillerId(),0):omniRRN;

            checkTransactionLog.setTransactionId(omniRRN);
            checkTransactionLog.setStatus("-1");
            checkTransactionLog.setResponseCode("10");
            checkTransactionLog.setResponseMessage("An Error occured while processing the request "+request);
            checkTransactionLog.setBillerId(transactionLog.getBillerId());
            checkTransactionLog.setAccount(transactionLog.getAccount());
            checkTransactionLog = transactionLogRepository.saveAndFlush(checkTransactionLog);

            checkStatusResponse.setCheckReqId(omniRRN);
            checkStatusResponse.setTransactionId(transactionLog.getTransactionId());
            checkStatusResponse.setResponseCode(10);
            checkStatusResponse.setResponseMessage("An Error occured while processing the request");

            log.info(checkStatusResponse.toString());
            checkStatusResponse.setTrnStatusDisc("");
            checkStatusResponse.setTrnDetails(null);
            return checkStatusResponse;
        }
    }
}
