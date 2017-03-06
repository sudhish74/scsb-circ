package org.recap.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.recap.ReCAPConstants;
import org.recap.gfa.model.*;
import org.recap.ils.model.response.ItemInformationResponse;
import org.recap.model.ItemRequestInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sudhishk on 27/1/17.
 */
@Service
public class GFAService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${gfa.item.status}")
    private String gfaItemStatus;

    @Value("${gfa.item.retrieval.order}")
    private String gfaItemRetrival;

    @Value("${gfa.item.edd.retrieval.order}")
    private String gfaItemEDDRetrival;

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public String getGfaItemStatus() {
        return gfaItemStatus;
    }

    public String getGfaItemRetrival() {
        return gfaItemRetrival;
    }

    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }

    public GFAItemStatusCheckResponse itemStatusCheck(GFAItemStatusCheckRequest gfaItemStatusCheckRequest) {

        ObjectMapper objectMapper = new ObjectMapper();
        String filterParamValue = "";
        GFAItemStatusCheckResponse gfaItemStatusCheckResponse = null;
        try {
            filterParamValue = objectMapper.writeValueAsString(gfaItemStatusCheckRequest);
            logger.info(filterParamValue);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity requestEntity = new HttpEntity<>(new HttpHeaders());
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(gfaItemStatus).queryParam(ReCAPConstants.GFA_SERVICE_PARAM, filterParamValue);
            ResponseEntity<GFAItemStatusCheckResponse> responseEntity = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, requestEntity, GFAItemStatusCheckResponse.class);
            gfaItemStatusCheckResponse = responseEntity.getBody();

            logger.info("", responseEntity.getStatusCode());
        } catch (JsonProcessingException e) {
            logger.info(ReCAPConstants.REQUEST_PARSE_EXCEPTION,e);
        } catch (Exception e) {
            logger.error(ReCAPConstants.REQUEST_EXCEPTION,e);
        }
        return gfaItemStatusCheckResponse;
    }

    public GFARetrieveItemResponse itemRetrival(GFARetrieveItemRequest gfaRetrieveItemRequest) {
        GFARetrieveItemResponse gfaRetrieveItemResponse = null;
        try {
            HttpEntity requestEntity = new HttpEntity(gfaRetrieveItemRequest, getHttpHeaders());
            ResponseEntity<GFARetrieveItemResponse> responseEntity = getRestTemplate().exchange(getGfaItemRetrival(), HttpMethod.POST, requestEntity, GFARetrieveItemResponse.class);
            getLogger().info(responseEntity.getStatusCode().toString());
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                gfaRetrieveItemResponse = responseEntity.getBody();
                if (gfaRetrieveItemResponse != null && gfaRetrieveItemResponse.getRetrieveItem() != null && gfaRetrieveItemResponse.getRetrieveItem().getTtitem() != null && !gfaRetrieveItemResponse.getRetrieveItem().getTtitem().isEmpty()) {
                    List<Ttitem> titemList = gfaRetrieveItemResponse.getRetrieveItem().getTtitem();
                    for (Ttitem ttitem : titemList) {
                        getLogger().info(ttitem.getErrorCode());
                        getLogger().info(ttitem.getErrorNote());
                        gfaRetrieveItemResponse.setSuccess(false);
                        gfaRetrieveItemResponse.setScrenMessage(ttitem.getErrorNote());
                    }
                } else {
                    gfaRetrieveItemResponse.setSuccess(true);
                }
            } else {
                gfaRetrieveItemResponse.setSuccess(false);
            }
        } catch (Exception e) {
            getLogger().error(ReCAPConstants.REQUEST_EXCEPTION, e);
        }
        return gfaRetrieveItemResponse;
    }

    public GFARetrieveItemResponse itemEDDRetrival(GFARetrieveEDDItemRequest gfaRetrieveEDDItemRequest) {
        GFARetrieveItemResponse gfaRetrieveItemResponse = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity requestEntity = new HttpEntity(gfaRetrieveEDDItemRequest, getHttpHeaders());
            ResponseEntity<GFARetrieveItemResponse> responseEntity = restTemplate.exchange(gfaItemEDDRetrival, HttpMethod.POST, requestEntity, GFARetrieveItemResponse.class);
            logger.info(responseEntity.getStatusCode().toString());
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                gfaRetrieveItemResponse = responseEntity.getBody();
                if (gfaRetrieveItemResponse != null && gfaRetrieveItemResponse.getRetrieveItem() != null && gfaRetrieveItemResponse.getRetrieveItem().getTtitem() != null && !gfaRetrieveItemResponse.getRetrieveItem().getTtitem().isEmpty()) {
                    List<Ttitem> titemList = gfaRetrieveItemResponse.getRetrieveItem().getTtitem();
                    for (Ttitem ttitem : titemList) {
                        logger.info(ttitem.getErrorCode());
                        logger.info(ttitem.getErrorNote());
                        gfaRetrieveItemResponse.setSuccess(false);
                        gfaRetrieveItemResponse.setScrenMessage(ttitem.getErrorNote());
                    }
                } else {
                    gfaRetrieveItemResponse.setSuccess(true);
                }
            } else {
                gfaRetrieveItemResponse.setSuccess(false);
                gfaRetrieveItemResponse.setScrenMessage("GFA HTTP request error");
            }
        } catch (Exception e) {
            gfaRetrieveItemResponse = new GFARetrieveItemResponse();
            gfaRetrieveItemResponse.setSuccess(false);
            gfaRetrieveItemResponse.setScrenMessage(e.getMessage());
            logger.error(ReCAPConstants.REQUEST_EXCEPTION, e);
        }
        return gfaRetrieveItemResponse;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public ItemInformationResponse executeRetriveOrder(ItemRequestInformation itemRequestInfo, ItemInformationResponse itemResponseInformation) {
        GFAItemStatusCheckRequest gfaItemStatusCheckRequest = new GFAItemStatusCheckRequest();

        GFAItemStatusCheckResponse gfaItemStatusCheckResponse = null;
        String itemStatus = "";
        String gfaOnlyStaus = "";

        try {
            GFAItemStatus gfaItemStatus001 = new GFAItemStatus();
            gfaItemStatus001.setItemBarCode(itemRequestInfo.getItemBarcodes().get(0));
            List<GFAItemStatus> gfaItemStatuses = new ArrayList<>();
            gfaItemStatuses.add(gfaItemStatus001);
            gfaItemStatusCheckRequest.setItemStatus(gfaItemStatuses);
            gfaItemStatusCheckResponse = itemStatusCheck(gfaItemStatusCheckRequest);
            if (gfaItemStatusCheckResponse != null
                    && gfaItemStatusCheckResponse.getDsitem() != null
                    && gfaItemStatusCheckResponse.getDsitem().getTtitem() != null && !gfaItemStatusCheckResponse.getDsitem().getTtitem().isEmpty()) {

                itemStatus = gfaItemStatusCheckResponse.getDsitem().getTtitem().get(0).getItemStatus();
                if (itemStatus.contains(":")) {
                    gfaOnlyStaus = itemStatus.substring(0, itemStatus.indexOf(':') + 1).toUpperCase();
                } else {
                    gfaOnlyStaus = itemStatus.toUpperCase();
                }
                logger.info(gfaOnlyStaus);

                if (ReCAPConstants.getGFAStatusAvailableList().contains(gfaOnlyStaus)) {
                    if (itemRequestInfo.getRequestType().equalsIgnoreCase(ReCAPConstants.REQUEST_TYPE_EDD)) {
                        itemResponseInformation = callItemEDDRetrivate(itemRequestInfo, itemResponseInformation);
                    } else if (itemRequestInfo.getRequestType().equalsIgnoreCase(ReCAPConstants.REQUEST_TYPE_RETRIEVAL)) {
                        itemResponseInformation = callItemRetrivate(itemRequestInfo, itemResponseInformation);
                    }
                } else {
                    itemResponseInformation.setSuccess(false);
                    itemResponseInformation.setScreenMessage(ReCAPConstants.GFA_RETRIVAL_ITEM_NOT_AVAILABLE);
                }
            } else {
                itemResponseInformation.setSuccess(false);
                itemResponseInformation.setScreenMessage(ReCAPConstants.GFA_ITEM_STATUS_CHECK_FAILED);
            }
        } catch (Exception e) {
            logger.error(ReCAPConstants.REQUEST_EXCEPTION, e);
        }
        return itemResponseInformation;
    }

    private ItemInformationResponse callItemRetrivate(ItemRequestInformation itemRequestInfo, ItemInformationResponse itemResponseInformation) {
        GFARetrieveItemRequest gfaRetrieveItemRequest = new GFARetrieveItemRequest();
        TtitemRequest ttitem001 = new TtitemRequest();
        ttitem001.setCustomerCode(itemRequestInfo.getCustomerCode());
        ttitem001.setItemBarcode(itemRequestInfo.getItemBarcodes().get(0));
        ttitem001.setDestination(itemRequestInfo.getDeliveryLocation());
        ttitem001.setRequestId(itemResponseInformation.getRequestId().toString());
        ttitem001.setRequestor(itemRequestInfo.getPatronBarcode());

        List<TtitemRequest> ttitems = new ArrayList<>();
        ttitems.add(ttitem001);
        RetrieveItemRequest retrieveItem = new RetrieveItemRequest();
        retrieveItem.setTtitem(ttitems);
        gfaRetrieveItemRequest.setRetrieveItem(retrieveItem);
        GFARetrieveItemResponse gfaRetrieveItemResponse = itemRetrival(gfaRetrieveItemRequest);
        if (gfaRetrieveItemResponse.isSuccess()) {
            itemResponseInformation.setSuccess(true);
            itemResponseInformation.setScreenMessage(ReCAPConstants.GFA_RETRIVAL_ORDER_SUCCESSFUL);
        } else {
            itemResponseInformation.setSuccess(false);
            itemResponseInformation.setScreenMessage(gfaRetrieveItemResponse.getScrenMessage());
        }
        return itemResponseInformation;
    }

    private ItemInformationResponse callItemEDDRetrivate(ItemRequestInformation itemRequestInfo, ItemInformationResponse itemResponseInformation) {
        GFARetrieveEDDItemRequest gfaRetrieveEDDItemRequest = new GFARetrieveEDDItemRequest();
        TtitemEDDRequest ttitem001 = new TtitemEDDRequest();
        ttitem001.setCustomerCode(itemRequestInfo.getCustomerCode());
        ttitem001.setItemBarcode(itemRequestInfo.getItemBarcodes().get(0));
        ttitem001.setRequestId(itemResponseInformation.getRequestId().toString());
        ttitem001.setRequestor(itemResponseInformation.getPatronBarcode());
        ttitem001.setStartPage(itemRequestInfo.getStartPage().toString());
        ttitem001.setEndPage(itemRequestInfo.getEndPage().toString());
        ttitem001.setArticleTitle(itemRequestInfo.getChapterTitle());
        ttitem001.setArticleAuthor(itemRequestInfo.getAuthor());

        List<TtitemEDDRequest> ttitems = new ArrayList<>();
        ttitems.add(ttitem001);
        RetrieveItemEDDRequest retrieveItemEDDRequest = new RetrieveItemEDDRequest();
        retrieveItemEDDRequest.setTtitem(ttitems);
        gfaRetrieveEDDItemRequest.setRetrieveEDD(retrieveItemEDDRequest);
        GFARetrieveItemResponse gfaRetrieveItemResponse = itemEDDRetrival(gfaRetrieveEDDItemRequest);
        if (gfaRetrieveItemResponse.isSuccess()) {
            itemResponseInformation.setSuccess(true);
            itemResponseInformation.setScreenMessage(ReCAPConstants.GFA_RETRIVAL_ORDER_SUCCESSFUL);
        } else {
            itemResponseInformation.setSuccess(false);
            itemResponseInformation.setScreenMessage(gfaRetrieveItemResponse.getScrenMessage());
        }
        return itemResponseInformation;
    }

}
