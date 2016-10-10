package org.hpccsystems.dsp.ramps.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.hpcc.HIPIE.HIPIEService;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class AboutController extends SelectorComposer<Window> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AboutController.class);

    @Wire
    private Label dspVersion;

    @Wire
    private Label hipieVersion;
    
    @Wire
    private Label vizVersion;

    @Override
    public void doAfterCompose(Window comp) throws Exception {
        super.doAfterCompose(comp);
        dspVersion.setValue(getDSPVersion());
        hipieVersion.setValue(HIPIEService.getVersion());
        vizVersion.setValue(getVIZVersion());
    }

    /**
     * This method will load the maven properties file from class path and
     * extract the version property for DSP (On jetty - dev environment, the property file
     * resides in the target folder, and in the prod/tomcat environment it
     * exists in the META-INF directory of the app context)
     * 
     * @return the version string
     */
    public static String getDSPVersion() {
        InputStream is = null;
        String version = null;
        String devFilepath = "./target/m2e-wtp/web-resources/META-INF/maven/org.hpccsystems/DSP/pom.properties";
        String prodFilepath = HipieSingleton.getDSPWebAppPath() + "/META-INF/maven/org.hpccsystems/DSP/pom.properties";
        
        try {
            File devFile = new File(devFilepath);
            if (devFile.exists() && devFile.isFile()) {
                is = new FileInputStream(devFile);
            } else {
                LOGGER.info("prodFilepath-->{}", prodFilepath);
                is = new FileInputStream(prodFilepath);
            }
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                version = props.getProperty("version", "");
                LOGGER.debug("DSP version-->{}", version);
            }
        } catch (Exception e) {
            LOGGER.error("Error opening POM properties file: {}", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                LOGGER.error("Error closing input stream: {}", e);
            }
        }
        return version;
    }
    
    /**
     * This method will load the package.json properties file from class path and
     * extract the version property for Visualization lib 
     * 
     * @return the version string
     */
    public static String getVIZVersion() {
        String version = null;
        BufferedReader br =null;
        String vizVersionFilepath = HipieSingleton.getDSPWebAppPath() + "/js/Visualization/package.json";
        try {
            File vizVersionFile = new File(vizVersionFilepath);
            if (vizVersionFile.exists() && vizVersionFile.isFile()) {
                LOGGER.info("Viz package.json file exists");
                br = new BufferedReader(new FileReader(vizVersionFile)); 
                JsonParser parser = new JsonParser();
                JsonObject obj=parser.parse(br).getAsJsonObject();
                LOGGER.info("Viz version {}",obj.get("version").getAsString());
                version = obj.get("version").getAsString();
            }else{
                LOGGER.info("Viz package.json file not exists");
            }
        } catch (Exception e) {
            LOGGER.error("Error opening package.json file: {}", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                LOGGER.error("Error closing Json Reader: {}", e);
            }
        }
        return version;
    }

}
