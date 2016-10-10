package org.hpccsystems.dsp.requestwrapper;

import java.text.Normalizer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter the HTTP Request from HIPIE form validation, save Also filtering the
 * visualization web service request
 *
 */
public class MyHttpRequestWrapper extends HttpServletRequestWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyHttpRequestWrapper.class);

    private Map<String, String[]> sanitizedQueryString;

    public MyHttpRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    // QueryString overrides

    @Override
    public String getParameter(String name) {
        String parameter = null;
        String[] vals = getParameterMap().get(name);

        if (vals != null && vals.length > 0) {
            parameter = vals[0];
        }

        return parameter;
    }

    @Override
    public String[] getParameterValues(String name) {
        return getParameterMap().get(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        if (sanitizedQueryString == null) {
            Map<String, String[]> res = new HashMap<String, String[]>();
            Map<String, String[]> originalQueryString = super.getParameterMap();
            String[] rawVals, snzVals = null;
            if (originalQueryString != null) {
                for (String key : (Set<String>) originalQueryString.keySet()) {
                    rawVals = originalQueryString.get(key);
                    snzVals = new String[rawVals.length];
                    for (int i = 0; i < rawVals.length; i++) {
                        snzVals[i] = stripXSS(rawVals[i]);
                        LOGGER.debug("Sanitized: {} to {}", rawVals[i], snzVals[i]);
                    }
                    res.put(stripXSS(key), snzVals);
                }
            }
            sanitizedQueryString = res;
        }
        return sanitizedQueryString;
    }

    // TODO: Implement support for headers and cookies (override getHeaders and
    // getCookies)

    /**
     * Removes all the potentially malicious characters from a string
     * 
     * @param value
     *            the raw string
     * @return the sanitized string
     */
    private String stripXSS(String value) {
        String cleanValue = null;
        if (value != null) {
            cleanValue = Normalizer.normalize(value, Normalizer.Form.NFD);

            // Avoid null characters
            cleanValue = cleanValue.replaceAll("\0", "");

            // Avoid anything between script tags
            Pattern scriptPattern = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid anything in a src='...' type of expression
            scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Remove any lonesome </script> tag
            scriptPattern = Pattern.compile("</script>", Pattern.CASE_INSENSITIVE);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Remove any lonesome <script ...> tag
            scriptPattern = Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid eval(...) expressions
            scriptPattern = Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid expression(...) expressions
            scriptPattern = Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid javascript:... expressions
            scriptPattern = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid vbscript:... expressions
            scriptPattern = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid onload= expressions
            scriptPattern = Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");
            
            LOGGER.debug("cleanValue after xss cleanup :: {}",cleanValue);
        }

        return cleanValue;
    }

}
