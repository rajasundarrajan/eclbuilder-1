package org.hpccsystems.dsp.log;


public class HipieQuery extends DBLog {
    
    private static final long serialVersionUID = 1L;
    public static final String GET_COMPOSITIONS ="Fetch Compositions";
    public static final String GET_HPCC_CONNECTION = "Get hpcc connection";
    public static final String FILE_FETCH = "Retrive datasource";
    public static final String ACCESSING_COMP_INSTANCE = "Accessing composition instance";
    public static final String PROCESS_RETRIVE = "Retrive process";
    public static final String SPRAY_FILE = "File spray";
    public static final String FILE_BROWSER_RETRIVE = "Retrive File Browser";
    
    private String detail;
    
    public HipieQuery(String action, long startTime, String detail) {
        super(action,startTime);
        this.detail = detail;
    }

    @Override
    public String getDetail() {
        StringBuilder build = new StringBuilder();
        build.append("Work done:")
            .append(this.detail);
            return build.toString();
    }

}
