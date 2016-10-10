package org.hpccsystems.dsp.webservices;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.WebserviceValidationException;

/**
 * Input bean for the DspWebServiceHandler#selectLayout() method
 * 
 * @author Ashoka_K
 *
 */
public class SelectLayoutInputs {

    private String username;

    private String uuid;

    private String gcidStr;

    private int gcid;

    public int getGcid() {
        return gcid;
    }

    public void setGcid(String gcid) {
        this.gcidStr = gcid;
    }

    /**
     * Gets the user name
     * 
     * @return The user name
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the composition id/name of the composition
     * 
     * @return The composition id/name of the composition
     */
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Validates, cleans and normalizes the attributes in the bean
     * 
     * @throws WebServiceException
     */
    public void postProcess() throws WebServiceException {
        validate();
        normalize();
    }

    private void validate() throws WebServiceException {
        validateIsAvailable(username, "username");
        validateIsAvailable(uuid, "Composition Id (uuid)");
    }

    private void normalize() throws WebServiceException {
        username = username.trim().toLowerCase();
        uuid = uuid.trim();

        if (isBlank(gcidStr)) {
            gcidStr = "-1";
        }

        try {
            this.gcid = Integer.parseInt(gcidStr);
        } catch (NumberFormatException e) {
            throw new WebserviceValidationException(format("Invalid integer \"%s\" supplied for gcid", gcidStr));
        }
    }

    private void validateIsAvailable(String propValue, String propName) throws WebServiceException {
        if (isBlank(propValue)) {
            throw new WebserviceValidationException(propName + " may not be null or blank");
        }
    }

}
