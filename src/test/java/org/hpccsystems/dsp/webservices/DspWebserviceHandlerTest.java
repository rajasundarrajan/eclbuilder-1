package org.hpccsystems.dsp.webservices;

import static org.apache.log4j.LogManager.getLogger;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class DspWebserviceHandlerTest {
	
	private static final Logger LOGGER = getLogger(DspWebServiceHandler.class);	
	
	@Mock private CompositionService cmpService;
	
	private SelectLayoutInputs inputs = new SelectLayoutInputs();
	
	@Mock private HttpServletRequest request;
	
	@Mock private WebServiceHelper helper;
	
	@Mock private DBLogger dbLogger;
	
	private MockHttpServletResponse response = new MockHttpServletResponse();
	
	@InjectMocks
	private DspWebServiceHandler handler = new DspWebServiceHandler();
	
	@Before
	public void setup() {
		LOGGER.setLevel(Level.TRACE);
		initMocks(this);
		
		inputs.setUsername("myUsername");
		inputs.setUuid("myCompositionId");
		inputs.setGcid("1234");
	}
	
	@Ignore
	public void verifySelectLayoutSuccess() throws Exception {
		Composition composition = mock(Composition.class);
		CompositionInstance workunit = mock(CompositionInstance.class);
		HPCCConnection connection = mock(HPCCConnection.class);
		
		when(helper.fetchComposition("myusername", "myCompositionId")).thenReturn(composition);
		when(helper.getCompositionInstance(same(composition))).thenReturn(workunit);
		when(workunit.getHPCCConnection()).thenReturn(connection);
		when(workunit.getCurrentUsername()).thenReturn("myCurrentUser");
		when(connection.getLabel()).thenReturn("myHpccId");
		when(composition.getVersion()).thenReturn("myVersion");
		when(helper.getDdls(workunit)).thenReturn("myDdl");
		when(workunit.getWorkunitId()).thenReturn("myWorkunitId");
		when(helper.fetchLayout(eq("myUsername"),
				eq("myDdl"), eq("myCompositionId"),eq("myVersion"), eq(1234))).thenReturn("myLayout");
		
		handler.selectLayout(request, response, inputs);
		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("myLayout", response.getContentAsString());
	}
	
	@Test
	public void verifyWebserviceExceptionHandling() throws WebServiceException, IOException {
		WebServiceException webserviceException = mock(WebServiceException.class);
		when(webserviceException.getStatusCode()).thenReturn(9999);
		when(webserviceException.getErrorString()).thenReturn("myErrorString");
		inputs.setUsername(null);
		
		handler.selectLayout(request, response, inputs);
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
		
		JsonObject error = getFirstError();
		assertEquals("INVALID_PROPERTY_VALUE", getString(error, "errorCode"));
		assertEquals("VALIDATE", getString(error, "errorType"));
		assertEquals("ERROR", getString(error, "errorLevel"));
		assertEquals("username may not be null or blank", getString(error, "errorString"));

		validateMiscJsonValues(error);
	}

	@Ignore
	public void verifyUnknownExceptionHandling() throws Exception {
		Exception exception = new Exception("myExceptionMessage");
		when(helper.fetchComposition("myusername", "myCompositionId")).thenThrow(exception);
		
		handler.selectLayout(request, response, inputs);
		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		
		JsonObject error = getFirstError();		
		assertEquals("UNCAUGHT_EXCEPTION", getString(error, "errorCode"));
		assertEquals("SYSTEM", getString(error, "errorType"));
		assertEquals("ERROR", getString(error, "errorLevel"));
		assertEquals("Error when fetching layout for user myusername, uuid myCompositionId, wuId myWuId. Exception:" +
				" java.lang.Exception, myExceptionMessage", getString(error, "errorString"));

		validateMiscJsonValues(error);
	}
	
	private void validateMiscJsonValues(JsonObject error) {
		assertEquals(JsonNull.class, error.get("additionalInfo").getClass());
		assertEquals(JsonNull.class, error.get("filename").getClass());
		assertEquals(JsonNull.class, error.get("sourceID").getClass());
		assertEquals(JsonNull.class, error.get("errorSource").getClass());
		
		assertEquals(0, getInt(error, "colNum"));
		assertEquals(0, getInt(error, "lineNum"));
	}
	
	private JsonObject getFirstError() throws UnsupportedEncodingException {
		return new Gson().fromJson(response.getContentAsString(), JsonArray.class).get(0).getAsJsonObject();
	}

	private String getString(JsonObject obj, String attribName) {
		return obj.get(attribName).getAsString();
	}
	
	private int getInt(JsonObject obj, String attribName) {
		return obj.get(attribName).getAsInt();
	}

}
