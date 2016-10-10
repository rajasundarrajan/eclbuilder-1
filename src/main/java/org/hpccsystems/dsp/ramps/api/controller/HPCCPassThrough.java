package org.hpccsystems.dsp.ramps.api.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpcc.HIPIE.utils.SSLUtilities;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("*.do")
public class HPCCPassThrough {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(HPCCPassThrough.class);
    String uTF8String = "UTF-8";

    @RequestMapping(value = "/{hpcc}/proxy-WUResult.do")
    public void proxyWUResult(@PathVariable String hpcc,
            HttpServletRequest request, HttpServletResponse response) {
        if(checkAuthentication(request.getSession(), response)) {
            return;
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WUResult Proxy - Params - {}",
                    request.getParameterMap());
        }

        passthrough(hpcc, "/WsWorkunits/WUResult.json", request, response);
    }

    @RequestMapping(value = "/{hpcc}/proxy-WUInfo.do")
    public void proxyWUInfo(@PathVariable String hpcc,
            HttpServletRequest request, HttpServletResponse response) {
        if(checkAuthentication(request.getSession(), response)) {
            return;
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WUInfo Proxy - Params - {}",
                    request.getParameterMap());
        }

        passthrough(hpcc, "/WsWorkunits/WUInfo.json", request, response);
    }

    private void passthrough(String hpccId, String endPoint,
            HttpServletRequest request, HttpServletResponse response) {
        HPCCConnection hpccConnection = HipieSingleton.getHipie()
                .getHpccManager().getConnection(hpccId);

        StringBuilder urlBuilder = new StringBuilder(hpccConnection.getESPUrl())
                .append(endPoint).append("?").append(request.getQueryString());

        writeJSONResponse(response, hpccConnection, urlBuilder.toString());
    }

    @RequestMapping(value = "/{hpcc}/proxy-WsEcl.do")
    public void proxyWsEcl(@PathVariable String hpcc,
            HttpServletRequest request, HttpServletResponse response)
            throws UnsupportedEncodingException {
        if(checkAuthentication(request.getSession(), response)) {
            return;
        }
        
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String[]> copiedParams = new HashMap<String, String[]>(
                params);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WsEcl Proxy - Params - {}", params);
        }

        HPCCConnection hpccConnection = HipieSingleton.getHipie()
                .getHpccManager().getConnection(hpcc);

        StringBuilder urlBuilder = new StringBuilder();

        if (hpccConnection.getIsHttps()) {
            urlBuilder.append("https://");
        } else {
            urlBuilder.append("http://");
        }

        urlBuilder.append(copiedParams.get("IP")[0]).append(":")
                .append(copiedParams.get("PORT")[0])
                .append("/WsEcl/submit/query/")
                .append(copiedParams.get("PATH")[0]);

        copiedParams.remove("IP");
        copiedParams.remove("PORT");
        copiedParams.remove("PATH");

        boolean isFirstArg = true;
        for (Entry<String, String[]> entry : copiedParams.entrySet()) {
            if (isFirstArg) {
                urlBuilder.append("?");
                isFirstArg = false;
            } else {
                urlBuilder.append("&");
            }

            urlBuilder.append(URLEncoder.encode(entry.getKey(), uTF8String))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue()[0], uTF8String));
        }

        writeJSONResponse(response, hpccConnection, urlBuilder.toString());
    }

    private void writeJSONResponse(HttpServletResponse response, HPCCConnection hpccConnection, String urlString) {
        //This is the implementation HIPIE uses.
        // But even removing this block works for me. Have to test 
        if (hpccConnection.getAllowInvalidCerts()) {
            SSLUtilities.trustAllHttpsCertificates();
            SSLUtilities.trustAllHostnames();
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JSON URL " + urlString);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding(uTF8String);
        URL url;
        try {
            url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic "
                    + hpccConnection.getAuthString());

            Scanner scanner = new Scanner(urlConnection.getInputStream());
            scanner.useDelimiter("//A");
            String json = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
            
            response.getWriter().write(json);
        } catch (IOException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    /**
     * Validates authentication and sends error upon failure
     * @param session
     * @param response
     * @return
     *  true when authentication fails
     */
    private boolean checkAuthentication(HttpSession session, HttpServletResponse response) {
        if(session.getAttribute(Constants.USER) == null) {
            LOGGER.error("user object is not present in session");
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } catch (IOException e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
            return true;
        }
        return false;
    }
}
