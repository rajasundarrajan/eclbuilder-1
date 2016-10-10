package org.hpccsystems.dsp.webservices;

/**
 * A container to hold the user id and the password of a user
 * 
 * @author Ashoka_K
 *
 */
public class BasicAuthCredentials {

    private String userName;

    private String password;

    /**
     * Argumented constructor
     * 
     * @param userName
     *            The id of the user
     * @param password
     *            The password of the user
     */
    public BasicAuthCredentials(String userName, String password) {
        super();
        this.userName = userName;
        this.password = password;
    }

    /**
     * Gets the id of the user
     * 
     * @return The id of the user
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gets the password of the user
     * 
     * @return The password of the user
     */
    public String getPassword() {
        return password;
    }

}
