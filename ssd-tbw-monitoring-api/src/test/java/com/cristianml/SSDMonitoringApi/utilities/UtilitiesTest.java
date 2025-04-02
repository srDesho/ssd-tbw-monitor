package com.cristianml.SSDMonitoringApi.utilities;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.cristianml.SSDMonitoringApi.utilities.Utilities.generateResponse;
import static org.junit.jupiter.api.Assertions.*;

public class UtilitiesTest {

    @Test
    public void testUtilities() {
        Utilities utilities = new Utilities();
    }

    @Test
    public void testUtilities_success() {
        HttpStatus httpStatus = HttpStatus.OK;
        String message = "success";

        ResponseEntity<Object> result = generateResponse(httpStatus, message);
        Map<String, Object> resultBody = (Map<String, Object>) result.getBody();

        assertEquals(httpStatus, result.getStatusCode());
    }

    @Test
    public void testUtilities_ErrorCase() {
        HttpStatus httpStatus = null;
        String message = "test";

        ResponseEntity<Object> result = generateResponse(httpStatus, message);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertTrue(result.getBody() instanceof  Map);

        Map<String, Object> resultBody = (Map<String, Object>) result.getBody();
        assertNotNull(resultBody.get("fecha"));
        assertNotNull(resultBody.get("mensaje"));
        assertEquals(httpStatus.INTERNAL_SERVER_ERROR.value(), resultBody.get("status"));

    }

}
