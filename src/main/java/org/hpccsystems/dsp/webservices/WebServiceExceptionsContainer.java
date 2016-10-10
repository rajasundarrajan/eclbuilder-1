package org.hpccsystems.dsp.webservices;

import static java.lang.String.format;

import org.apache.http.HttpStatus;
import org.hpcc.HIPIE.error.HipieErrorCode;
import org.hpcc.HIPIE.error.HipieErrorType;

/**
 * A container for exceptions that can occur when processing DSP webservices
 * 
 * @author Ashoka_K
 *
 */
public class WebServiceExceptionsContainer {

	
    private WebServiceExceptionsContainer(){
        
    }
    
    /**
	 * Thrown case the user does not have rights to view compositions
	 * 
	 * @author Ashoka_K 
	 *
	 */
    public static class CannotViewPermissionsException extends WebServiceException {

        private static final long serialVersionUID = 1L;

        /**
         * Arged constructor
         * 
         * @param userId The id of the user who does not have the rights to view compositions
         * @param compositionId The composition which the user was trying to view
         * @param cause The Throwable which caused this exception
         */
        public CannotViewPermissionsException(String userId, String compositionId, Throwable cause) {
            super(format("User %s does not have permissions to view composition %s", userId, compositionId),
                    HttpStatus.SC_FORBIDDEN, cause);
            
            this.errorType = HipieErrorType.SECURITY;
            this.errorCode = HipieErrorCode.PERMISSION_DENIED;
        }

    }
    
    /**
     * Thrown in case the specified composition
     * 
     * @author Ashoka_K
     *
     */
    public static class CompositionNotFoundException extends WebServiceException {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * Arged constructor
         * 
         * @param compositionId The id/name of the composition which the user was trying to fetch
         */
        public CompositionNotFoundException(String compositionId) {
            super("No composition found for composition " + compositionId, HttpStatus.SC_BAD_REQUEST);
            
            this.errorCode = HipieErrorCode.COMPOSITION_NOT_FOUND;
        }
        
    }
    
    /**
     * Throwin in case the specified workunit was not found
     * 
     * @author Ashoka_K
     *
     */
    public static class WorkunitNotFoundException extends WebServiceException {

        private static final long serialVersionUID = 1L;

        /**
         * Arged constructor
         * 
         * @param workunitId The id of the workunit which the user is being retrieved
         * @param compositionId The id of the composition corresponding to the workunit id
         */
        public WorkunitNotFoundException(String workunitId, String compositionId) {
            super(format("No workunit %s found for composition %s", workunitId, compositionId),
                    HttpStatus.SC_BAD_REQUEST);
            
            this.errorCode = HipieErrorCode.INVALID_RESOURCE;
        }

    }
    
    /**
     * Thrown in case there are no workunits available for the composition
     * 
     * @author Ashoka_K
     *
     */
    public static class NoWorkunitsForCompositionException extends WebServiceException {

        private static final long serialVersionUID = 1L;

        /**
         * Arged constructor
         * 
         * @param compositionId The id of the composition for which the workunit was being fetched
         */
        public NoWorkunitsForCompositionException(String compositionId) {
            super(format("No workunits found for composition %s", compositionId),
                    HttpStatus.SC_BAD_REQUEST);
            
            this.errorCode = HipieErrorCode.INVALID_RESOURCE;
        }

    }
    
    /**
     * Thrown in case the validation of an input parameter (to the webservice) fails
     * 
     * @author Ashoka_K
     *
     */
    public static class WebserviceValidationException extends WebServiceException {

        private static final long serialVersionUID = 1L;

        /**
         * Arged constructor
         * 
         * @param message The error message of the constructor
         */
        public WebserviceValidationException(String message) {
            super(message, HttpStatus.SC_BAD_REQUEST);
            
            this.errorCode = HipieErrorCode.INVALID_PROPERTY_VALUE;
        }

    }
    
    /**
     * Thrown in case no layout could be found for the specified composition/workunit
     * combination 
     * 
     * @author Ashoka_K
     *
     */
    public static class NoLayoutFoundException extends WebServiceException {

        private static final long serialVersionUID = 1L;
        
        /**
         * Arged constructor
         * 
         * @param username The username used when the user attempted to retrieve the layout
         * @param compositionId The id of the composition for which the user attempted to retrieve the layout
         * @param workunitId The id of the workunit for which the user attempted to retrieve the layout
         */
        public NoLayoutFoundException(String username, String compositionId) {
            super(format("System Error. No layout could be found for the given inputs, i.e. username: %s, "
                    + "cmpId: %s", username, compositionId),
                    HttpStatus.SC_BAD_REQUEST);
            
            this.errorCode = HipieErrorCode.INVALID_RESOURCE;
        }

    }

    /**
     * Thrown in case the DLD for the given composition-workunit combination could not be found
     * 
     * @author Ashoka_K
     *
     */
    public static class DdlNotFoundException extends WebServiceException {

        private static final long serialVersionUID = 1L;
        
        /**
         * Arged Constructor
         * 
         * @param compositionId The id of the composition for which the DDL was being retrieved
         * @param workunitOwner the owner of the workunit
         */
        public DdlNotFoundException(String compositionId, String workunitOwner) {
            super(format("No DDL found corresponding to composition %s, work unit %s",
                    compositionId, workunitOwner),
                    HttpStatus.SC_BAD_REQUEST);
            
            this.errorCode = HipieErrorCode.INVALID_RESOURCE;
        }

    }
    
    /**
     * Thrown in case an unhandled exception is thrown when processing the webservice
     * 
     * @author Ashoka_K
     *
     */
    public static class UnknownWebserviceException extends WebServiceException {

        private static final long serialVersionUID = 1L;
        
        /**
         * Arged constructor
         * 
         * @param message The error message of the exception
         */
        public UnknownWebserviceException(String message) {
            super(message, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            
            this.errorType = HipieErrorType.SYSTEM;
            this.errorCode = HipieErrorCode.UNCAUGHT_EXCEPTION;
        }

    }
    
}
