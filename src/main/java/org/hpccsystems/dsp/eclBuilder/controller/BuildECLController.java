package org.hpccsystems.dsp.eclBuilder.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.utils.ECLPackage;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpcc.HIPIE.utils.HPCCLogicalFile;
import org.hpcc.HIPIE.utils.Utility;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.eclBuilder.domain.File;
import org.hpccsystems.dsp.eclBuilder.domain.Folder;
import org.hpccsystems.dsp.eclBuilder.util.TreeCreation;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.FileMeta;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.ws.client.HPCCWsClient;
import org.hpccsystems.ws.client.gen.wsdfu.v1_29.DFUFileDetail;
import org.hpccsystems.ws.client.gen.wsfileio.v0_0.ReadFileDataRequest;
import org.hpccsystems.ws.client.gen.wsworkunits.v1_58.ECLResult;
import org.hpccsystems.ws.client.platform.DFUFileDetailInfo;
import org.hpccsystems.ws.client.platform.Platform;
import org.hpccsystems.ws.client.platform.Version;
import org.hpccsystems.ws.client.platform.WorkunitInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Script;
import org.zkoss.zul.Span;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Window;

public class BuildECLController extends SelectorComposer<Component> implements EventListener<Event> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Wire
	private Button ImportFiles;

	@Wire
	private Button selectFiles;

	@Wire
	private Window DivId;

	@Wire
	Tree treeGrid;

	@Wire
	org.zkoss.zul.Center centerBorder;

	@Wire
	private Script codemirrorJS;

	@Wire
	private Button runECL;

	@Wire
	private Button joinECL;

	@Wire
	private Button saveECL;

	@Wire
	private Button ChooseFiles;

	@Wire
	private Textbox builderCode;

	@Wire
	private Textbox builderOldCode;

	Platform platform ;
	
	HPCCWsClient connector;
	
	@Wire
	private Include AddFileInclude;

	private String hpccID;

	private String userAction;

	@Wire
	private Include eclBuilderInclude;

	@Wire
	private Tabpanel eclWatchPanel;

	@Wire
	private Tabpanel eclBuilderPanel;

	@Wire
	private Tabpanel builderHistoryPanel;
	@Wire
	private Include builderHistoryInclude;

	@Wire
	private Tab eclWatch;

	@Wire
	private Tab builderHistory;

	@Wire
	private Tab eclBuilder;

	private String logicalFilesForBuilder = "";

	private String eclBuilderName;

	private Builder cloningBuilder;

	private HPCCConnection connection;

	private String wuID = "";

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		userAction = (String) Executions.getCurrent().getAttribute("userAction");

		hpccID = (String) Executions.getCurrent().getAttribute("hpccConnId");

		eclBuilderName = (String) Executions.getCurrent().getAttribute("BuilderName");

		DivId.addEventListener("onAddFiles", event -> {
			loadFiles(event);
		});

		DivId.addEventListener("onAddJoins", event -> {
			addJoinQueries(event);
		});
		
		platform = Platform.get("http", "10.173.147.1", 8010, "", "");

        connector = platform.getHPCCWSClient();


		Folder rootFolder = TreeCreation.populateTree("", connector, hpccID);
		Treechildren rootChildren = new Treechildren();

		Treeitem treeItem = new Treeitem();
		Treerow treeRow = new Treerow(rootFolder.getBaseFolderName());
		Treechildren children = new Treechildren();
		treeRow.setVisible(false);
		treeItem.appendChild(treeRow);
		treeItem.appendChild(children);
		rootChildren.appendChild(treeItem);
		treeGrid.appendChild(rootChildren);
		centerBorder.setDroppable("true");
		centerBorder.addEventListener(Events.ON_DROP, event -> {
			onDropEventbuilderCode(event);
		});

		if (null != rootFolder.getListOfFolders()) {
			TreeCreation.buildTree(rootFolder.getListOfFolders(), children, null, connector);
			for (Component titem : children.getChildren()) {
				if (titem instanceof Treeitem && titem.getChildren() != null) {
					titem.addForward("onClick", treeGrid, "onOpen");
				} else {
					titem.addForward("onClick", treeGrid, "onOpen");
				}
			}
		}

		codemirrorJS.setSrc("ramps/js/codemirror.js");

		Clients.evalJavaScript("CodeMirror.fromTextArea(\"" + builderCode.getUuid() + "\",\"" + builderOldCode.getUuid()
				+ "\", {lineNumbers: true, height: \"650px\", stylesheet: \"css\\codemirror.css\", textWrapping: true})");

		if (userAction.equals("clone") || userAction.equals("edit")) {

			cloningBuilder = (Builder) Executions.getCurrent().getAttribute("selectedBuilder");
			
			builderHistory.setDisabled(false);

			updateECLbuilderforCloneCopy(userAction.equals("clone") ? cloningBuilder.getName() : eclBuilderName,
					userAction);
		}

	}

	@Listen("onClick=#builderHistory")
	public void openBuilderHistoryPanel(Event e) {
		try {
			if (org.apache.commons.lang3.StringUtils.isNotEmpty(eclBuilderName)) {

				List<Builder> builders = ((DSPDao) SpringUtil.getBean("dspDao"))
						.getECLBuilder(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE))
								.getCurrentUser().getId(), eclBuilderName, hpccID);

				builderHistoryInclude.setDynamicProperty("buildersList", builders);

				builderHistoryInclude.setDynamicProperty("userAction", userAction);

				builderHistoryInclude.setDynamicProperty("hpccConnId", hpccID);

				builderHistoryInclude.setDynamicProperty("BuilderName", eclBuilderName);

				builderHistoryInclude.setSrc("eclBuilder/BuilderHistory.zul");

				builderHistoryInclude.invalidate();

			}
		} catch (DatabaseException e1) {
			System.out.println(e1.getLocalizedMessage());
		}
	}

	public void updateECLbuilderforCloneCopy(String builderName, String userAction) {
		try {
			String userID = ((User) ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE))
					.getCurrentUser()).getId();

			List<Builder> eclBuilders = ((DSPDao) SpringUtil.getBean("dspDao")).getECLBuilder(userID, builderName,
					hpccID);
			if (eclBuilders.size() > 0) {
				Builder builder = eclBuilders.get(0);
				builderCode.setValue(builder.getEclbuildercode());
				logicalFilesForBuilder = builder.getLogicalFiles();
				joinECL.setVisible(Arrays.asList(logicalFilesForBuilder.split(",")).size() > 0 ? true : false);
				Executions.getCurrent().setAttribute("selectedBuilder", builder);
				if ("clone".equals(userAction)) {
					runECLBuilder();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void loadFiles(Event evnt) {
		loadFiles((List<String>) evnt.getData());
	}

	@Listen("onDrop=#builderCode")
	public void onDropEventbuilderCode(Event e) {
		List<String> files = new ArrayList<String>();
		Treeitem fileToAdd = ((Treeitem) ((DropEvent) e).getDragged());
		files.add(fileToAdd.getValue());
		if (Arrays.asList(logicalFilesForBuilder.split(",")).contains(fileToAdd.getValue())) {
			Clients.showNotification("This file is Already added!!!");

			return;
		}

		fileToAdd.getChildren().stream().filter(comp -> comp.isVisible());

		List<Component> comps = ((Treechildren) fileToAdd.getChildren().get(1)).getChildren().stream()
				.filter(comp -> ((Treeitem) comp).isSelected()).collect(Collectors.toList());

		boolean tableActionRequested = comps.size() != 0
				|| ((Treechildren) fileToAdd.getChildren().get(1)).getChildren().size() < comps.size() ? true : false;

		String tempStrArr[] = fileToAdd.getValue().toString().split("::");

		String datasetName = tempStrArr[tempStrArr.length - 1].replaceAll("[^A-Za-z_]+", "");

		logicalFilesForBuilder += (org.apache.commons.lang.StringUtils.isNotEmpty(logicalFilesForBuilder) ? "," : "")
				+ datasetName;

		if (Arrays.asList(logicalFilesForBuilder.split(",")).size() > 1) {
			joinECL.setVisible(true);
		}

		String TableStr = "table" + datasetName + " := " + "TABLE(" + datasetName + ",{";

		for (Component x : comps) {
			TableStr += datasetName + "." + ((Treeitem) x).getValue().toString().trim() + ",";
		}

		if (TableStr.endsWith(",")) {
			TableStr = TableStr.substring(0, TableStr.length() - 1);
		}

		TableStr += "});";

		TableStr += "\n\nOUTPUT(" + (tableActionRequested ? "table" : "") + datasetName + ", NAMED(\'" + "table"
				+ datasetName + "_Data\'));";

		loadEclCodeToCodeMirror(
				formBasicECLForFile(fileToAdd.getValue(), !tableActionRequested).replaceAll("[\n]+", "\n") + "\n"
						+ (tableActionRequested ? TableStr : ""));

	}

	public void addJoinQueries(Event eve) {

		org.json.simple.JSONObject joinData = (org.json.simple.JSONObject) eve.getData();

		String eclBuilderCode = (String) joinData.get("joinString");

		logicalFilesForBuilder += "," + (String) joinData.get("joinNames");

		List<Component> objs = builderCode.getParent().getChildren().stream()
				.filter(e -> e.getClass().equals("codemirror")).collect(Collectors.toList());
		for (Component comp : objs) {
			builderCode.getParent().removeChild(comp);
		}
		builderCode.setText(eclBuilderCode);
		Clients.evalJavaScript("CodeMirror.fromTextArea(\"" + builderCode.getUuid() + "\",\"" + builderOldCode.getUuid()
				+ "\", {lineNumbers: true, height: \"650px\", stylesheet: \"css\\codemirror.css\", textWrapping: true})");
		runECL.setDisabled(false);

	}

	public void loadFiles(List<String> selectedFiles) {

		String eclBuilderCode = "";
		List<String> selectedUniqueFiles = new ArrayList<String>(selectedFiles);
		for (String str : selectedFiles) {
			if (Arrays.asList(logicalFilesForBuilder.split(",")).contains(str)) {
				selectedUniqueFiles.remove(str);
			}
		}
		if (selectedUniqueFiles.size() == 0) {
			return;
		} else {
			for (String str : selectedUniqueFiles) {

				eclBuilderCode += formBasicECLForFile(str, true);

				logicalFilesForBuilder += (org.apache.commons.lang.StringUtils.isNotEmpty(logicalFilesForBuilder) ? ","
						: "") + str;
			}
		}

		if (Arrays.asList(logicalFilesForBuilder.split(",")).size() > 1) {
			joinECL.setVisible(true);
		}
		loadEclCodeToCodeMirror(eclBuilderCode);
		runECL.setDisabled(false);

	}

	private void loadEclCodeToCodeMirror(String eclCode) {
		List<Component> objs = builderCode.getParent().getChildren().stream()
				.filter(e -> e.getClass().equals("codemirror")).collect(Collectors.toList());
		for (Component comp : objs) {
			builderCode.getParent().removeChild(comp);
		}
		builderCode.setText(eclCode);
		Clients.evalJavaScript("CodeMirror.fromTextArea(\"" + builderCode.getUuid() + "\",\"" + builderOldCode.getUuid()
				+ "\", {lineNumbers: true, height: \"650px\", stylesheet: \"css\\codemirror.css\", textWrapping: true})");

	}

	@Listen("onClick=#joinECL")
	public void joinECL() {

		HashMap<String, Object> args = new HashMap<String, Object>();
		args.put("selectedFiles", logicalFilesForBuilder);
		args.put("hpccConnID", hpccID);
		args.put("selectedBuilder", cloningBuilder);
		args.put("BuilderName", eclBuilderName);
		Window win = (Window) Executions.createComponents("/eclBuilder/draganddrop.zul", null, args);
		Executions.getCurrent().getAttribute("userAction");
		win.setParent(this.getSelf());
		win.doModal();

	}

	@Listen("onClick=#runECL")
	public void shadowRunECLCall() {
		Clients.evalJavaScript("CodeMirror.runEclBuilder(\"" + builderCode.getUuid() + "\")");
	}

	@Listen("onLoadBox=#builderCode")
	public void loadTextBoxComp(Event e) {
		builderCode.setText(e.getData().toString());

		runECLBuilder();
	}
	
	@Listen("onLoadBox=#builderOldCode")
	public void loadTextBoxCompOld(Event e) {
		builderCode.setText(e.getData().toString());

		runECLBuilder();
	}

	public ECLPackage validateECLCode() {
		try {
			DFUFileDetail dfuFileDetail;

			Platform platform;
			connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);
			platform = Platform.get((connection.getIsHttps() ? "https" : "http"), connection.getServerHost(),
					connection.getServerPort(), connection.getUserName(), connection.getPwd());

			Version v = platform.getVersion();
			System.out.println(v.toString());

			connector = platform.getHPCCWSClient();

			ReadFileDataRequest readFileReq = new ReadFileDataRequest();

			String[] clusterGroups = connector.getAvailableClusterGroups();

			WorkunitInfo wu = new WorkunitInfo();
			wu.setECL(builderCode.getText());
			wu.getExceptions();
			wu.setJobname("myflatoutput");
			wu.setCluster(Arrays.asList(clusterGroups).contains("thor") ? "thor" : clusterGroups[1]);
			wu.setResultLimit(100);
			wu.setMaxMonitorMillis(50000);
			wu.setJobname("MyJob");

			boolean deleteTempFiles = true;
			String tempCompileDir = HipieSingleton.getHipie().getHpccManager().getTempDir();
			String tempFileBasename = "tempfile";
			String eclccdir = HipieSingleton.getHipie().getHpccManager().getEclCCDir();
			int compiletimeout = 60000; // one minute
			String eclToCompile = builderCode.getText();

			ECLPackage actionPackage = Utility.compileECL(eclToCompile, eclccdir, tempCompileDir, tempFileBasename,
					null, null, deleteTempFiles, compiletimeout);

			return actionPackage;

		} catch (Exception e) {
			return new ECLPackage();
		}
	}

	@Listen("onClick=#saveECL")
	public void saveECLBuilder() {

		if (StringUtils.isEmpty(builderCode.getText())) {
			Clients.showNotification("Empty Builder Code cannot be Saved! Please build the code before Save!");
			return;
		}

		try {
			ECLPackage actionPackage = validateECLCode();

			if (actionPackage.getCompileErrors().size() == 0) {

				ECLBuilder eclBuilderDtls = new ECLBuilder(
						((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()
								.getId(),
						eclBuilderName, logicalFilesForBuilder,
						new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()), builderCode.getText(), hpccID,
						wuID);

				((DSPDao) SpringUtil.getBean("dspDao")).addOrUpdateECLBuilders(eclBuilderDtls, true);
			}
		} catch (Exception e) {

		}

	}

	public void runECLBuilder() {
		try {

			if (StringUtils.isEmpty(builderCode.getText())) {
				Clients.showNotification("Empty Builder Code cannot be Run! Please build the code before Run!");
				return;
			}
			ECLPackage actionPackage = validateECLCode();

			if (actionPackage.getCompileErrors().size() == 0) {
				actionPackage = connection.runECLPackage(actionPackage, "thor", "jobname");
				if (actionPackage.getHpccErrors() != null) {
					wuID = actionPackage.getWorkunit().getWuid();
					actionPackage.getWorkunit().getResults();
				} else {
					throw new Exception(actionPackage.getHpccErrors().toECLErrorString());
				}
			} else {
				throw new Exception(actionPackage.getCompileErrors().toECLErrorString());
			}

			String wuresults = "";
			JSONObject resJson;
			List<ECLBuilderReportData> reportDetails = new ArrayList<ECLBuilderReportData>();

			ECLBuilderReportData tempReportDetails = new ECLBuilderReportData();
			ListModelList<ECLBuilderReportData> reportDetailsList = new ListModelList<ECLBuilderReportData>();
			for (ECLResult result : actionPackage.getWorkunit().getResults()) {
				wuresults = connection.getWorkunitResult(wuID, result.getName());
				resJson = XML.toJSONObject(wuresults);
				resJson = (JSONObject) resJson.get("Dataset");
				tempReportDetails = new ECLBuilderReportData();
				tempReportDetails.setBuilderReportName(resJson.getString("name"));
				tempReportDetails.setRptColumns(getRptColumns(resJson));
				tempReportDetails.setWuId(wuID);
				tempReportDetails.setListData(getRptData(resJson, tempReportDetails.getRptColumns()));
				reportDetails.add(tempReportDetails);
				reportDetailsList.add(tempReportDetails);
			}

			Events.postEvent("onCompleteUserAction", eclBuilderInclude.getParent().getParent().getParent(), null);
			eclBuilderInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, new DashboardConfig());
			eclBuilderInclude.setDynamicProperty("reportDetails", reportDetailsList);
			eclBuilderInclude.setDynamicProperty(Constants.HPCC_CONNNECTION, hpccID);
			eclBuilderInclude.setSrc("eclBuilder/ECLBuilderReport.zul");
			eclBuilderInclude.invalidate();

			eclWatch.setDisabled(false);

			builderHistory.setDisabled(false);

			eclWatch.setSelected(true);

			eclBuilder.setDisabled(false);

			ECLBuilder eclBuilderDtls = new ECLBuilder(
					((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()
							.getId(),
					eclBuilderName, logicalFilesForBuilder, new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()),
					builderCode.getText(), hpccID, wuID);

			((DSPDao) SpringUtil.getBean("dspDao")).addOrUpdateECLBuilders(eclBuilderDtls, true);
		} catch (Exception e) {
			Clients.showNotification(e.getLocalizedMessage(), builderCode, true);
			System.out.println(e.getStackTrace());
		}
	}

	public void refreshLeftFileTreeOnSprayComplete(Event e) {
		treeGrid.invalidate();
	}

	@Listen("onClick=#ImportFiles")
	public void openImportFiles() {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("userAction", userAction);
		args.put("hpccConnID", hpccID);
		Window win = (Window) Executions.createComponents("/eclBuilder/import_file.zul", null, args);
		win.setParent(this.getSelf().getParent());
		win.doModal();
	}

	@Listen("onClick=#builderHistory")
	public void loadBuilderHistory() throws DatabaseException {

		Grid historyGrid = new Grid();
		historyGrid.setId("historyList");
		historyGrid.setAutopaging(true);
		historyGrid.setMold("paging");
		historyGrid.setEmptyMessage(Labels.getLabel("nobuilderAvailable"));
		Columns cols = new Columns();
		Column col = new Column(Labels.getLabel("workunit ID"));
		cols.appendChild(col);
		col = new Column(Labels.getLabel("Last Modified Date"));
		cols.appendChild(col);

		List<Builder> historyList = ((DSPDao) SpringUtil.getBean("dspDao")).getECLBuildersByName(
				((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(),
				true, eclBuilderName);
		Rows rows = new Rows();
		Row row;
		Label lab1;
		Label lab2;
		Button button1;

		for (Builder build : historyList) {
			row = new Row();
			lab1 = new Label();
			lab1.setValue(build.getWuID());
			lab2 = new Label();
			lab2.setValue(build.getLastmodifieddate().toString());
			button1 = new Button();
			button1.setLabel("view");
			button1.setAttribute("wuID", build.getWuID());
			row.appendChild(lab1);
			row.appendChild(lab2);
			row.appendChild(button1);
			row.addEventListener("onClick", event -> openHistoryTab(event));
			rows.appendChild(row);
		}
		historyGrid.appendChild(cols);
		historyGrid.appendChild(rows);
	}

	public void openHistoryTab(Event e) {
		System.out.println(e.getTarget().getAttribute("wwID"));
	}

	@Listen("onClick=#ChooseFiles")
	public void openChooseFiles() {
		HashMap<String, Object> args = new HashMap<String, Object>();
		args.put("userAction", userAction);
		args.put("hpccConnID", hpccID);
		args.put("selectedBuilder", cloningBuilder);
		Window win = (Window) Executions.createComponents("/eclBuilder/fileBrowser.zul", null, args);

		win.doModal();
		AddFileInclude.getParent().detach();
	}

	@Override
	public void onEvent(Event event) throws Exception {
		// TODO Auto-generated method stub

	}

	private List<String> getRptColumns(JSONObject rptJSONObject) {
		try {
			List<String> rptColumns = new ArrayList<String>();
			JSONArray dummyJSONArray = new JSONArray();

			Object obj;
			if (!rptJSONObject.has("Row")) {
				return rptColumns;
			}
			obj = rptJSONObject.get("Row");

			Iterator<String> iter = obj instanceof JSONArray ? ((JSONObject) ((JSONArray) obj).get(0)).keys()
					: ((JSONObject) obj).keys();

			ArrayList<String[]> listData = new ArrayList<String[]>();

			while (iter.hasNext()) {
				rptColumns.add(iter.next());
			}
			return rptColumns;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Listen("onFileClickAction=#treeGrid")
	public void onClickFileName(ForwardEvent event) {
		String str = formBasicECLForFile(((Treeitem) event.getOrigin().getTarget()).getValue(), true);

		System.out.println(str);
	}

	private String formBasicECLForFile(String logicalFile, boolean addOutput) {

		logicalFile = logicalFile.startsWith("~") ? logicalFile : "~" + logicalFile;
		HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);
		String tempStrArr[] = logicalFile.split("::");
		String datasetName = tempStrArr[tempStrArr.length - 1].replaceAll("[^A-Za-z_]+", "");
		DFUFileDetailInfo dfuFileDetail;
		try {
			dfuFileDetail = ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getFileDetail(logicalFile,
					connection, connection.getThorCluster());

			String eclCode = "\n" +  datasetName + "recName :=" + dfuFileDetail.getEcl().replaceAll("[\n]+", "\n");
			eclCode += "\n\n" + datasetName + " := DATASET(\'" + logicalFile + "\', " + datasetName + "recName, THOR);";

			if (addOutput) {
				eclCode += "\n\nOUTPUT(" + datasetName + ", NAMED(\'" + datasetName + "_Data\'));";
			}
			return eclCode + "\n";

		} catch (HPCCException e) {
			e.printStackTrace();
			return "";
		}
	}

	@Listen("onOpen=#treeGrid")
	public void onClickAddChildren(ForwardEvent event) {

		Button Add = null;
		
		connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);

		Treeitem selectedItem = (Treeitem) event.getOrigin().getTarget();
		
			selectedItem.setImage("/ramps/icons/" + ("folder".equals(selectedItem.getAttribute("type")) ? "FolderOpen.png" : "FileOpen.png"));
		selectedItem.setOpen(!selectedItem.isOpen());
		
		if(!selectedItem.isOpen()){
			return;
		}
		Folder newFolder = TreeCreation.populateTree((selectedItem).getValue(), connector, hpccID);
		newFolder.setBaseFolderName("Child");
		Treechildren tc = new Treechildren();

		if (null != newFolder.getListOfFolders()) {
			for (Component children : treeGrid.getTreechildren().getChildren().get(0).getChildren()) {
				if (children instanceof Treechildren) {
					Treeitem newItem = ((Treeitem) event.getOrigin().getTarget());
					newItem.addForward("onClick", treeGrid, "onOpen");
					if (newItem.getChildren().size() > 0) {
						Treeitem newItem1 = new Treeitem();
						if (newItem.getChildren().size() > 1) {
							tc = ((Treechildren) newItem.getChildren().get(1));
						} else {
							tc = new Treechildren();
							newItem.appendChild(tc);
						}
						tc.appendChild(newItem1);
						newItem = newItem1;
					}
					Treerow newRow = new Treerow(newFolder.getBaseFolderName());
					Treechildren newChild = new Treechildren();
					newItem.setOpen(newFolder.getIsFile() ? false : true);
					newItem.appendChild(newRow);
					newItem.appendChild(newChild);
					children.appendChild(newItem);
					if (null != newFolder.getListOfFiles()) {
						for (File file : newFolder.getListOfFiles()) {
							Treeitem childItem = new Treeitem(file.getFileName());
							childItem.setValue(file.getFileName());
							childItem.addForward("onClick", treeGrid, "onOpen");
							tc.appendChild(childItem);
						}
					}
					TreeCreation.buildTree(newFolder.getListOfFolders(), tc, Add, connector);

					for (Component titem : tc.getChildren()) {
						titem.addForward("onClick", treeGrid, "onOpen");
					}
					break;
				}
			}
		} else if (null != newFolder.getListOfFiles()) {

			selectedItem.setDraggable("true");
			tc = (Treechildren) selectedItem.getChildren().get(1);
			for (Component c : selectedItem.getChildren().get(1).getChildren()) {
				tc.appendChild(c);
			}
			for (File file : newFolder.getListOfFiles()) {
				Treeitem childItem = new Treeitem(file.getFileName());
				Span spanTag = new Span();
				spanTag.setStyle(returnStrBtwParenthesis(file.getActualFileName()));
				spanTag.setSclass(returnStrBtwParenthesis(file.getActualFileName()));
				spanTag.setClass(returnStrBtwParenthesis(file.getActualFileName()));
				Treecell tCell = new Treecell();
				tCell.appendChild(spanTag);
				childItem.setValue(file.getFileName());
				childItem.setImage("/ramps/icons/" + findIconPath(returnStrBtwParenthesis(file.getActualFileName())));
				childItem.addEventListener(Events.ON_CLICK, evnt -> {System.out.println("File Variable Clicked ");});
				childItem.setZclass(returnStrBtwParenthesis(file.getActualFileName()));
				childItem.setSclass(returnStrBtwParenthesis(file.getActualFileName()));
				childItem.setLabel(file.getActualFileName());
				tc.appendChild(childItem);
			}
		}
	}

	private String findIconPath(String typename) {
		if (typename.startsWith("string")) {
			return "String.png";
		} else if (typename.startsWith("integer")){
			return "Number.png";
		} else if (typename.startsWith("boolean")) {
			return "Boolean.png";
		} else if(typename.startsWith("decimal")) {
			return "Decimal.png";
		}else{
			return "String.png";
		}
	}

	public DefaultTreeModel<FileMeta> getTreeModel() {
		return (new DefaultTreeModel<FileMeta>(getFileInfoTreeData(null, "")));
	}

	private FileMetaTreeNode getFileInfoTreeData(FileMeta obj, String currentDir) {

		HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);
		FileMeta newFile = new FileMeta();
		newFile.setFileName("");
		List<FileMetaTreeNode> rootFile = new ArrayList<FileMetaTreeNode>();
		rootFile.add(new FileMetaTreeNode(newFile));
		try {
			List<FileMeta> files = getFileList(currentDir, connection);

			FileMetaTreeNode innerFile;

			for (FileMeta file : files) {
				innerFile = new FileMetaTreeNode(file);
				rootFile.add(innerFile);
			}
		} catch (HPCCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FileMetaTreeNode[] t = new FileMetaTreeNode[1];
		return new FileMetaTreeNode(obj, rootFile.toArray(t));
	}

	public static List<FileMeta> getFileList(String scope, HPCCConnection hpccConnection) throws HPCCException {

		List<FileMeta> results = new ArrayList<FileMeta>();

		List<HPCCLogicalFile> resultsArray;
		try {
			resultsArray = hpccConnection.getFilenames(scope, hpccConnection.getThorCluster());
			FileMeta fileMeta;

			for (HPCCLogicalFile hpccLogicalFile : resultsArray) {
				fileMeta = new FileMeta();
				if (hpccLogicalFile.isDirectory()) {
					fileMeta.setIsDirectory(true);
					fileMeta.setFileName(hpccLogicalFile.getFileName());
					fileMeta = settingScope(scope, fileMeta, hpccLogicalFile);
				} else {
					fileMeta.setIsDirectory(false);
					fileMeta.setFileName(hpccLogicalFile.getFileName());
					fileMeta.setScope(hpccLogicalFile.getScope());
				}
				results.add(fileMeta);
			}
		} catch (Exception e) {
			throw new HPCCException(Labels.getLabel("unableToFetchFileList"), e);
		}

		return results;
	}

	private static FileMeta settingScope(String scope, FileMeta fileMeta, HPCCLogicalFile hpccLogicalFile) {
		if (scope.length() > 0) {
			fileMeta.setScope(scope + "::" + hpccLogicalFile.getFileName());
		} else {
			fileMeta.setScope("~" + hpccLogicalFile.getFileName());
		}
		return fileMeta;
	}

	private List<String[]> getRptData(JSONObject rptJSONObject, List<String> rptColumns) {
		try {
			String[] tempColArr = new String[0];
			List<String[]> listData = new ArrayList<String[]>();
			// listData.add(rptColumns.toArray(tempColArr));
			JSONObject tempJson;

			JSONArray dummyJSONArray = new JSONArray();
			if (!rptJSONObject.has("Row")) {
				return listData;
			}
			Object rowObj = rptJSONObject.get("Row");

			JSONArray rptJSONArr = rowObj instanceof JSONArray ? ((JSONArray) rowObj)
					: dummyJSONArray.put(0, (JSONObject) rowObj);

			String[] tempStrArr;
			for (int i = 0; i < rptJSONArr.length(); i++) {

				tempJson = (JSONObject) rptJSONArr.get(i);
				tempStrArr = new String[tempJson.length()];
				for (int cnt = 0; cnt < rptColumns.size(); cnt++) {
					tempStrArr[cnt] = tempJson.get(rptColumns.get(cnt)).toString();
				}
				listData.add(tempStrArr);
			}
			return listData;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String returnStrBtwParenthesis(String inputStr) {
		return inputStr.substring(inputStr.indexOf("(") + 1, inputStr.length() - 1).toLowerCase();
	}

}