package org.hpccsystems.dsp.webservices;

import static org.hpccsystems.util.ExceptionValidator.validateException;
import static org.junit.Assert.assertEquals;

import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.WebserviceValidationException;
import org.junit.Before;
import org.junit.Test;

public class SelectLayoutsInputsTest {
	
	private SelectLayoutInputs inputs = new SelectLayoutInputs();
	
	@Before
	public void setup() throws WebServiceException {
		inputs.setUsername(" myUserName ");
		inputs.setUuid(" myCompositionId ");
		inputs.setGcid("1234");
	}
	
	@Test
	public void verifyPostProcessing() throws WebServiceException {
		inputs.postProcess();
		
		assertEquals("myusername", inputs.getUsername());
		assertEquals("myCompositionId", inputs.getUuid());
		
		assertEquals(1234, inputs.getGcid());
		
		inputs.setGcid("");
        inputs.postProcess();
        assertEquals(-1, inputs.getGcid());
        
        inputs.setGcid(null);
        inputs.postProcess();
        assertEquals(-1, inputs.getGcid());
	}
	
	@Test
	public void verifyValidation() throws Exception {
		inputs.setUsername(null);
		validateValidationException("username may not be null or blank");		
		inputs.setUsername(" ");
		validateValidationException("username may not be null or blank");		
		inputs.setUsername("myUserName");
		
		inputs.setUuid(null);
		validateValidationException("Composition Id (uuid) may not be null or blank");
		inputs.setUuid(" ");
		validateValidationException("Composition Id (uuid) may not be null or blank");		
	}

	private void validateValidationException(String message) throws Exception {
		validateException(inputs, "postProcess", WebserviceValidationException.class, 
				message, (Throwable) null);
	}

}
