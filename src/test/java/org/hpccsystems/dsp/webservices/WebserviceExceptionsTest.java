package org.hpccsystems.dsp.webservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.powermock.reflect.Whitebox.getInternalState;

import org.apache.http.HttpStatus;
import org.hpcc.HIPIE.error.HipieErrorCode;
import org.hpcc.HIPIE.error.HipieErrorType;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.CannotViewPermissionsException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.CompositionNotFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.DdlNotFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.NoLayoutFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.NoWorkunitsForCompositionException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.UnknownWebserviceException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.WebserviceValidationException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.WorkunitNotFoundException;
import org.hpccsystems.error.HError.ErrorLevel;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

public class WebserviceExceptionsTest {
    
    @Mock private Throwable cause;
    
    @Test
    public void verifyCannotViewPermissionsException() {
        CannotViewPermissionsException e = 
                new CannotViewPermissionsException("myUserId", "myCompositionId", cause);
        
        assertSame(cause, e.getCause());
        assertEquals("User myUserId does not have permissions to view composition myCompositionId", e.getMessage());
        assertEquals(HttpStatus.SC_FORBIDDEN, e.getStatusCode());
        validateHError(e, HipieErrorType.SECURITY, HipieErrorCode.PERMISSION_DENIED, ErrorLevel.ERROR);
    }

    @Test
    public void verifyCompositionNotFoundException() {
        CompositionNotFoundException e = 
                new CompositionNotFoundException("myCompositionId");
        assertEquals("No composition found for composition myCompositionId", e.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
        validateHError(e, HipieErrorType.VALIDATE, HipieErrorCode.COMPOSITION_NOT_FOUND, ErrorLevel.ERROR);
    }
    
    @Test
    public void verifyNoWorkunitsForCompositionException() {
        NoWorkunitsForCompositionException e = 
                new NoWorkunitsForCompositionException("myCompositionId");
        assertEquals("No workunits found for composition myCompositionId", e.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
        validateHError(e, HipieErrorType.VALIDATE, HipieErrorCode.INVALID_RESOURCE, ErrorLevel.ERROR);
    }
    
    @Test
    public void verifyWorkunitNotFoundException() {
        WorkunitNotFoundException e = 
                new WorkunitNotFoundException("myWorkunitId", "myCompositionId");
        assertEquals("No workunit myWorkunitId found for composition myCompositionId", e.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
        validateHError(e, HipieErrorType.VALIDATE, HipieErrorCode.INVALID_RESOURCE, ErrorLevel.ERROR);
    }
    
    @Test
    public void verifyWebserviceValidationException() {
        WebserviceValidationException e = 
                new WebserviceValidationException("myErrorMessage");
        assertEquals("myErrorMessage", e.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
        validateHError(e, HipieErrorType.VALIDATE, HipieErrorCode.INVALID_PROPERTY_VALUE, ErrorLevel.ERROR);
    }
    
    @Ignore
    public void verifyNoLayoutFoundException() {
        NoLayoutFoundException e = 
                new NoLayoutFoundException("myUserName", "myCompositionId");
        assertEquals("System Error. No layout could be found for the given inputs,"
                + " i.e. username: myUserName, cmpId: myWorkunitId, wuId: myCompositionId", e.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
        validateHError(e, HipieErrorType.VALIDATE, HipieErrorCode.INVALID_RESOURCE, ErrorLevel.ERROR);
    }
    
    @Test
    public void verifyDdlNotFoundException() {
        DdlNotFoundException e = 
                new DdlNotFoundException("myCompositionId", "myWorkunitOwner");
        assertEquals("No DDL found corresponding to composition myCompositionId, work unit myWorkunitOwner",
                e.getMessage());
        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
        validateHError(e, HipieErrorType.VALIDATE, HipieErrorCode.INVALID_RESOURCE, ErrorLevel.ERROR);
    }
    
    @Test
    public void verifyUnknownWebserviceException() {
        UnknownWebserviceException e = 
                new UnknownWebserviceException("myErrorMessage");
        assertEquals("myErrorMessage", e.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getStatusCode());
        validateHError(e, HipieErrorType.SYSTEM, HipieErrorCode.UNCAUGHT_EXCEPTION, ErrorLevel.ERROR);
    }
    
    private void validateHError(WebServiceException e, HipieErrorType errorType,
            HipieErrorCode errorCode, ErrorLevel errorLevel) {
        assertEquals(errorType, getInternalState(e, "errorType"));
        assertEquals(errorCode, getInternalState(e, "errorCode"));
        assertEquals(errorLevel, getInternalState(e, "errorLevel"));
    }

}
