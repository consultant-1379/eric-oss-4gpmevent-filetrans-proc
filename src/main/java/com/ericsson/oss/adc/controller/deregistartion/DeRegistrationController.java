/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.adc.controller.deregistartion;

import com.ericsson.oss.adc.enums.DeregisterStatus;
import com.ericsson.oss.adc.responses.deregister.DeregisterResponse;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogService;
import com.ericsson.oss.adc.util.StartupUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal REST Controller handling DeRegistration tasks. Not to be exposed to other services.
 * Currently, the only clients of these endpoints are the relevant Helm Hook jobs.
 */
@RestController
@Slf4j
public class DeRegistrationController {

    public static final String DEREGISTER_DATA_SERVICE_INSTANCE_URI = "/data-service-instance";
    @Value("${dmm.data-catalog.dataServiceName}")
    private String dataServiceName;

    @Value("${dmm.data-catalog.dataCollectorName}")
    private String dataCollectorName;

    @Autowired
    private DataCatalogService dataCatalogService;

    @Autowired
    private StartupUtil startupUtil;

    /**
     * Delete this {@link com.ericsson.oss.adc.models.data.catalog.v2.DataServiceInstance} from {@link DataCatalogService}
     * @return Response indicating if success or failure in deleting the {@link com.ericsson.oss.adc.models.data.catalog.v2.DataServiceInstance}
     */
    @DeleteMapping(value = DEREGISTER_DATA_SERVICE_INSTANCE_URI, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeregisterResponse> deleteDataServiceInstance() {

        List<String> failInstanceList = new ArrayList<>();
        List<String> passInstanceList = new ArrayList<>();
        List<String> enmList = startupUtil.getAllEnmsFromDataCatalog();

        String dataServiceInstanceName = null;
        ResponseEntity<Void> dataCatalogResponse = null;
        int failCount = 0;
        if (!enmList.isEmpty()) {
            for (String enm : enmList) {
                dataServiceInstanceName = dataServiceName + "--" + enm;
                log.info("Received a request to DELETE (deregister) the Data Service Instance '{}'.", dataServiceInstanceName);
                dataCatalogResponse =
                        dataCatalogService.deleteDataServiceInstance(dataServiceName, dataServiceInstanceName);

                if (HttpStatus.NO_CONTENT.equals(dataCatalogResponse.getStatusCode()) || HttpStatus.NOT_FOUND.equals(dataCatalogResponse.getStatusCode())) {
                    passInstanceList.add(dataServiceInstanceName);
                } else {
                    failInstanceList.add(dataServiceInstanceName);
                    failCount++;
                }
            }
        } else {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(DeregisterResponse.build(passInstanceList, failInstanceList));
        }
        HttpStatusCode dataCatalogStatus;
        if (failCount == enmList.size() && dataCatalogResponse != null) {
            dataCatalogStatus = dataCatalogResponse.getStatusCode();
        } else if (failCount > 0) {
            dataCatalogStatus = HttpStatus.OK;
        } else {
            dataCatalogStatus = HttpStatus.OK;
        }

        return ResponseEntity.status(dataCatalogStatus)
                .body(DeregisterResponse.build(passInstanceList, failInstanceList));
    }

}