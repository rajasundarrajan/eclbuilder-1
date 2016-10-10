package org.hpccsystems.dsp.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.ramps.entity.Company;

/**
 * Dao class to interact with MBS database to get company IDs/Reference IDS
 *
 */
public interface CompanyDao {

    /**
     * This returns list of Companies which matches the user entered Company
     * Name or GCID
     * 
     * @param company
     * @return List<Company>
     * @throws DatabaseException
     */
     List<Company> getMatchingCompany(Company company) throws DatabaseException;
     /**
      * Gcid for which the compliance values to be fetched from MBS DB
      * complianceTags List of compliance tags fetched from MBS DB
      * Throws database exception if connection failure or and for any runtime exception
      */
     Map<String, String> getGCIDComplianceValues(String gcid,Collection<String> complianceTags) throws DatabaseException;
}
