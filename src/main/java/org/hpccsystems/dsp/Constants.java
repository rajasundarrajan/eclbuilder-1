package org.hpccsystems.dsp;

public class Constants {
    public static final String EXCEPTION = "EXCEPTION - {}";
    public static final String HANDLED_EXCEPTION = "This error is handled \n{}";

    // Spring service names
    public static final String AUTHENTICATION_SERVICE = "authenticationService";
    public static final String USER_SERVICE = "userService";
    public static final String COMPOSITION_SERVICE =  "compositionService";
    public static final String HPCC_SERVICE ="hpccService";
    public static final String DB_LOGGER="dbLogger";
    public static final String DESKTOP ="desktop";
    public static final String PLUGIN_SERVICE ="pluginService";
    public static final String LOGICAL_FILE_SERVICE = "logicalFileService";
    public static final String MIGRATION_SERVICE =  "migrationService";
    public static final String DERMATOLOGY_SERVICE = "mySqlDermatologyService";
    

    // Session Attributes
    public static final String USER = "user";

    public static enum VIEW {
        GRID, LIST
    }

    // Events
    public class EVENTS {
        public static final String ON_FILTER_CHANGE = "onFilterChange";
        public static final String ON_SELECT_PLUGIN = "onSelectpPlugin";
        public static final String ON_DELETE_PLUGIN = "onDeletePlugin";
        public static final String ON_SWAP_PLUGIN = "onSwapPlugin";
        public static final String ON_PAGE_CHANGE = "onPageChange";
        public static final String ON_PROJECT_ADD = "onProjectAdd";
        public static final String ON_DROP_PLUGIN_ON_ARROW = "onDropPluginOnArrow";
        public static final String ON_DELETE_COMPOSITION = "onDeleteComposition";
        public static final String ON_VIEW_PROCESS = "onViewProcess";
        public static final String ON_UPDATE_PROCESSES = "onUpdateProcesses";
        public static final String ON_SELECT_PROCESS_OUTPUT = "onSelectProcessOutput";
        public static final String ON_SAVE = "onSave";
        public static final String ON_SAVE_COMPOSITION = "onSaveComposition";
        public static final String ON_CLOSE_SAVE_AS_WINDOW = "onCloseSaveAsWindow";
        public static final String ON_RUN = "onRun";
        public static final String ON_RUN_COMPLETE = "onRunComplete";
        public static final String ON_VALIDATE = "onValidate";
        public static final String ON_CLICK_CLONE = "onClickClone";
        public static final String ON_CLICK_EDIT = "onClickEdit";
        public static final String ON_OPEN_COMPOSITION = "onOpenComposition";
        public static final String ON_LOAD_CLONED_COMPOSITION = "onLoadClonedComposition";
        public static final String ON_CLICK_VIEW = "onClickView";
        public static final String ON_PLUGIN_ADD = "onPluginAdd";
        public static final String ON_SELECT_ENTITY = "onSelectEntity";
        public static final String ON_REMOVE_ENTITY = "onRemoveEntity";
        public static final String ON_CLICK_VIEW_OR_EDIT = "onClickViewOrEdit";
        public static final String ON_IMPORT_FILE = "onImportFile";
        public static final String ON_SPRAY_PROGRESS = "onSprayProgress";
        public static final String ON_SPRAY_COMPLETE = "onSprayComplete";
        public static final String ON_SPRAY_FAILED = "onSprayFailed";
        public static final String ON_SAVE_CURRENT_PLUGIN = "onSaveCurrentPlugin";
        public static final String ON_OPEN_RAMPS_PERSPECTIVE = "onOpenRampsPerspective";
        public static final String ON_RUN_INITIATED = "onSavePluginBeforeRun";
        public static final String ON_CHANGE_HPCC_CONNECTION = "onChangeHPCCConnection";
        public static final String ON_CLOSE_OLD_PROJECT = "onCloseOldProject";
        public static final String ON_RETURN_TO_EDIT = "onReturnToEdit";
        public static final String ON_CREATE_COMPOSITION = "onCreateComposition";
        public static final String ON_OPEN_WIDGET_CONFIGURATION = "onOpenWidgetConfiguration";
        public static final String ON_DASHBOARD_VIEW = "onDashboardView";
        public static final String ON_DISABLE_SETTINGS = "onDisableSettings";
        public static final String ON_ENABLE_SETTINGS = "onEnableSettings";
        public static final String ON_FAV_COMPOSITION = "onFavoriteComposition";
        /**
         * Event to create and associate a new Datasource to Dashboard. 
         * Event has to be fired to widgetCanvas in WidgetConfig
         * Implementation is at WidgetController 
         */
        public static final String ON_SELECT_DATASOURCE = "onSelectDatasource";
        public static final String ON_OPEN_INTERACTIVITY = "onOpenInteractivity";
        public static final String DELETE_DASHBOARD_DUD_AND_SAVE_COMP = "onDeleteDashboardDudAndSaveComp";

        public static final String ON_PROCESS_LOADED = "onProcessLoaded";
        public static final String ON_PROCESS_LOAD_FAIL = "onProcessLoadFail";
        /**
         * Event to open more-info popup as a prespective. 'Process' object needs to be passed in as Data
         */
        public static final String ON_OPEN_PROCESS_INFO = "onOpenProcessInfo";
        public static final String ON_CLOSE_PROCESS_INFO = "onCloseProcessInfo";
        public static final String ON_CLOSE_PLUGIN_BROWSER = "onClosePluginBrowser";
        public static final String ON_POPULATE_FILE_PROPS = "onPopulateProps";
        public static final String ON_UPDATE_GRID_PLUGINS = "onUpdateGridPlugins";
        public static final String ON_SAVE_HPCC_CONNECTION = "onSaveHPCCConnection";
        public static final String ON_SELECT_COMPANY_ID = "onSelectCompanyId";
        public static final String ON_FILE_LOADED = "onFileLoaded";
        public static final String ON_FILE_LOAD_FAILED = "onFileLoadFailed";
        public static final String ON_CHANGE_LABEL = "onChangeLabel";
        public static final String ON_SAVE_DASHBOARD_CONFIG = "onSaveDashboardConfig";
        
        //Events to select/change GCIDs
        public static final String ON_CONFIRM_GCID = "onConfirmGCID";
        public static final String ON_REFRESH_PLUGIN_BROWSER = "onRefreshPluginBrowser";
        public static final String ON_SELECT_LATEST_LIST = "onSelectLatestList";
        public static final String ON_COMPLETE_BROWSER_LOADING = "onCompleteFileBrowserLoading";
        
        //Events to select/use Global variable
        public static final String ON_ADD_GLOBAL_VARIABLE = "onAddGlobalVariable";
        public static final String SHOW_HIDE_GLOBAL_VAR_OPTION = "onChangeGlobalVar";
        public static final String ON_POPULATE_GLOBAL_VAR_FILE_PROPS = "onPopulateGlobalVarFileProps";
        public static final String ON_STARTOF_BROWSER_LOADING = "onStratofBrowserLoading";
        public static final String ON_CLOSE_PROCESS_WINDOW = "onCloseProcessWindow";
        
        public static final String ON_PROMOTION_COMPLETE = "onPromotionComplete";
        public static final String ON_PROMOTION_FAIL = "onPromotionFail";
        
        // Events to open/interact with dashboard advanced mode
        public static final String ON_OPEN_ADVANCED_MODE = "onOpenAdvancedMode";
        
        private EVENTS() {
        }
    }

    //GCID constants
    public static final String GCIDS = "gcids";
    
    public static final String HOME_COMPONENT = "homeComponent";
    
    public static final String PROJECT = "project";
    public static final String PROCESS = "process";
    public static final String OUTPUT = "output";
    public static final String HPCC_DATA = "hpccData";
    public static final String HPCC_CONNNECTION = "hpccConnection";
    //This constant is also used as URL parameter. Don't change the value
    public static final String COMPOSITION = "composition";
    public static final String COLON = ":";
    public static final String PLUGIN = "plugin";
    public static final String HIPIE_ERROR = "hipieError";
    public static final String VISUALIZATION_DDL = "visualizationDDL";

    // Edit project screen flow type
    public static final String FLOW_TYPE = "flowType";
    public static final String TAB_DATA = "tabData";

    public static final String ACTION_SAVE = "saveAction";
    public static final String SAVE_AS_TEMPLATE = "saveAsTemplate";

    public static final String OPEN_PROJECT_LABELS = "fileNameSet";

    // Restful Service request actions
    public static final String OPEN_PROJECTS = "openProjects";

    // Notification position
    public static final String POSITION_BEFORE_CENTER = "before_center";
    public static final String POSITION_BEFORE_START = "before_start";
    public static final String POSITION_BEFORE_END = "before_end";
    public static final String POSITION_END_CENTER = "end_center";
    public static final String POSITION_END_AFTER = "end_after";
    public static final String POSITION_MIDDLE_CENTER = "middle_center";
    public static final String POSITION_MIDDLE_AFTER = "middle_after";
    public static final String POSITION_TOP_CENTER = "top_center";
    public static final String POSITION_BOTTOM_RIGHT = "bottom_right";
    public static final String POSITION_AFTER_CENTER = "after_center";
    public static final String POSITION_START_CENTER = "start_center";

    // Notification types
    public static final String ERROR = "error";

    public static final String DRAGGED = "dragged";
    public static final String DROPPED = "dropped";
    public static final String BASIC_TEMPLATE = "BasicTemplate";

    public static final String DATE_FORMAT = "MM/dd/yyyy hh:mm:ss a";

    public static final String VISUALIZATION_DDLS = "ddls";

    public static final String SUCCESS = "success";

    // Create project flows
    public static final String CREATE_PROJECT = "createProject";
    public static final String CREATE_PROJECT_FROM_TEMPLATE = "createProjectFromTemplate";

    public static final String OVERFLOW_AUTO = "overflow: auto";

    public static final String EMPTY_MESSAGE = "sideeffects-empty-message";

    public static final String POPULATE_HPCC_VALUE = "populate hpcc values";
    public static final String PARENT = "parent";
    public static final String FILE = "file";
    public static final String DEFAULT_STRUCTURE = "STRING|Field1";
    public static final String SCOPE_RESOLUTION_OPR = "::";
    public static final String REFERENCE_ID = "ReferenceId";
    public static final String LOGICAL_FILENAME = "LogicalFilename";
    public static final String TILDE = "~";

    public static final String PREVIEW_TAB = "preview_tab";
    public static final String DASHBOARD_CONFIG = "dashboardConfig";
    public static final String ON_CLICK_NEXT_CHOOSE_WIDGET = "onClickNextToChooseWidget";
    public static final String WIDGET = "widget";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String ON_CLICK_CLOSE_WIDGET_CONFIGURATION = "onClickCloseWidgetConfiguration";
    public static final String OUTPUTS = "outputs";
    public static final String INPUTS = "inputs";
    public static final String PLUGIN_RELATION = "pluginRelation";
    public static final String ON_CHOOSING_PLUGIN_RELATION = "onChoosingPluginRelation";
    public static final String CONTRACT_INSTANCE = "contractInstance";
    public static final String DASHBOARD = "dashboard";
    public static final String REPO = "repo";
    public static final String OPEN_DASHBOARD_LABELS = "dashboardlabels";
    public static final String PROCESS_PAGE_TYPE = "processPageType";
    public static final String NAME = "name";
    public static final String MEASURE_SORTED = "measureSorted";
    public static final String ATTRIBUTE_SORTED = "attributeSorted";
    public static final String CLUSTER = "Cluster";
    public static final String PROJECT_TAB = "projectTab";
    
    //Stub/mbs switch constants
    public static final String STUB = "stub";
    public static final String USER_GROUP_SVS_TYPE = "UserGroupSvcType";
    public static final String ROW_COUNT = "rowcount";
    public static final String UNSIGNED = "unsigned";
    
    //Large data error message
    public static final String ERROR_MSG_LARGE_DATA ="Dataset too large to output to workunit";
    public static final String DATA_TOO_LARGE = "dataTooLarge";
    public static final String LARGE_DATA_TOO_LARGE = "largeDataErrorMsg";
    
    //process not complete error message
    public static final String PROCESS_NOT_COMPLETE="processNotComplete";
    
    //process not complete error message
    public static final String CONTRACT_NOT_AVAILABE="contractNotAvailable";
    
    public class ACTION {
        public static final String RUN = "run";
        public static final String NAVIGATION = "nav";
        public static final String VALIDATE = "validate";
        public static final String SAVE = "save";
        public static final String SAVE_AS = "saveAs";

        private ACTION() {
        }
    }

    public static enum Flow {
        NEW, EDIT, CLONE, VIEW
    }
    
    public static enum DatasourceStatus {
        LOADING, VALID, INVALID
    }
    
    public static final String COUNT="COUNT";
    //Time to display the status message in milliseconds
    public static final int MESSAGE_VIEW_TIME = 2000;
    public static final String COMPOSITION_PERMISSION = "compositionPermission";
    public static final String FORWARD_EVENT = "forwardEvent";
    public static final String REFRESH_MODEL = "onRefreshModel";
    public static final String ADD_TO_MODEL = "onAddToModel";
    public static final String STRUCTURE = "Structure";
    
    // Global variable constants
    public static final String GLOBAL_VAR_PREFIX = "^";
    public static final String GLOBAL = "GLOBAL";

    
    public static final String SERVICE = "_service";
    public static final String DASHBOARD_HOLDER_UUID = "ChartHolderUuid";
    
    public static final String GENERIC_USER = "genericUser";
    
    public static final String PIPE = "|";
    public static final String BATCH_COMPOSITION = "BatchComposition";
    public static final String SPACE = " ";
    
    public static final String KEEP_ECL = "KeepECL";
    public static final String VIZ_SERVICE_VERSION = "VIZSERVICEVERSION";
    public static final String INDUSTRY_CLASS = "INDUSTRYCLASS";
    public static final String FCRA = "FCRA";
    public static final String GCID = "GCID";
    
    public static final String STATUS_COMPLETED = "completed";
    // To to hide the implicit public Constructor
    private Constants() {
    }

    public static enum ValueCategory {
        BLACK_LIST_PLUGIN, BLACK_LIST_THOR_FILE;
    } 
    
    public static final String INPUT = "INPUT";
    public static final String HOME_TABBOX = "homeTabbox";
	public static final String ECL_BUILDER = "eclBuilder";

public final static String EXCEL_FORMAT = "EXCEL";
	
	public final static String JSON_FORMAT = "JSON";
	
	public final static String XML_FORMAT = "XML";
	
	public final static String EXCEL_FORMATEXT = ".XLSX";
	
	public final static String JSON_FORMATEXT = ".TXT";
	
	public final static String XML_FORMATEXT = ".XML";
	
	public final static String JSON_PATH = "Response.Results.Result 1.Row";
	
	public final static String METHOD_NAME_CONSTANT = "methodname(";
	
	public final static String FRONT_SLASH = "/";
	
	public final static String CLOSE_PARENTHESIS = ");";
	
	public final static String RESULTS_STR = "Results";
	
	public final static String ROW_STR = "Row";
	
	public final static String EMPTY_STR = "";
	
	public final static String COMMA = ",";
	
	public final static String AUTH_STR = "Authorization";
	
	public final static String BASIC_AUTH_STR = "Basic ";
	
	public final static String METHOD_STR =	"Method";
	
	public final static String GET_METHOD =	"GET";
	
	public final static String SSL_STR = "SSL";
	
	public final static String MOVE_SEL_RIGHT = ">";
	
	public final static String MOVE_SEL_LEFT = "<";
	
	public final static String MOVE_ALL_RIGHT = ">>";
	
	public final static String MOVE_ALL_LEFT = "<<";
	
	public final static String MOVE_UP = "UP";
	
	public final static String MOVE_DOWN = "DN";
	
	public final static String LABEL_DOWNLOAD_FRMT = "Download Format";
	
	public final static String LABEL_SUBMIT_INPUTS = "Submit Inputs";
	
	public final static String ACT_CALL_SERVICE = "CallService";
	
	public final static String ACT_CALL_WEB_SERVICE = "CallWebService";
	
	public final static String ACT_CALL = "Call";
	
	public final static String 	QUES_MARK = "?";
	
	public final static String  AMPRASENT = "&";

	public static final String EQUALSTO = "=";
	
	public static final String LABEL_USERNAME = "UserName";
	
	public static final String LABEL_PASSWORD = "UserName";
	
	public static final String LABEL_FILTERFILEDS = "Filter Fields";
	
	public static final String LABEL_AVAIL_FIELDS = "Available Fields";
	
	public static final String LABEL_SELECT_FIELDS = "Selected Fields";

	public static final String ACT_LABEL_SECURED = "secured";
	
	public static final String ACT_SSLENABLED = "sslEnabled";
	
	public static final String LABEL_SSLENABLED = "SSL Enabled";
	
	public static final String LABEL_INVALIDURL = "Invalid Cluster Details";
	
	public static final String LABEL_ROXIENAME = "Roxie Name";
	
	public static final String LABEL_IPHOST ="IP Address";
	
	public static final String LABEL_PORT ="PORT";
	
	public static final String LABEL_ROXIEQUERY = "ROXIE QUERY";
	
	public static final String HTTPS_SCHEME = "https://";
	
	public static final String HTTP_SCHEME = "http://";
	
	public static final String URL_QUERY_PART = "/WsEcl/submit/query/";
	
	public static final String URL_EXAMPLE_REQ = "/example/request/";
	
	public static final String URL_EXAMPLE_RESPONSE = "/example/response/";
	
	public static final String URL_JSON_STR = "/json";
	
	public static final String URL_SUBMIT_STR = "/submit/";
	
	public static final String APP_JFRAME_TITLE = "RoxieWebServiceCall";
	
	public static final String METHODNAME_URLPART = "/json?jsonp=methodname";
	
	public static final String TRUE_STR = "true";
	
	public static final String FALSE_STR = "false";
	
	public static final String ERROR_STR = "Error";
	
	public static final String SUBMIT_CLUSTER_DTLS = "Submit Cluster Details";
	
	public static final String CALL_WEB_SERVICE = "Call Web Service";
	
	public static final String NEXT = "Next";

	public static final String DOT = ".";
}
