package org.hpccsystems.dsp.eclBuilder.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpcc.HIPIE.utils.HPCCLogicalFile;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.eclBuilder.domain.File;
import org.hpccsystems.dsp.eclBuilder.domain.Folder;
import org.hpccsystems.dsp.eclBuilder.util.TreeCreation;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.FileMeta;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.ws.client.HPCCWsClient;
import org.hpccsystems.ws.client.gen.wsdfu.v1_29.DFUFileDetail;
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
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.Include;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

public class FileBrowserController extends	 SelectorComposer<Component> implements EventListener<Event> {

	@Wire
	Tree treeGrid;
	private String hpccID;

	private String eclBuilderName;

	@Wire
	private Button Add;

	@Wire
	private Button Remove;

	@Wire
	private Listbox selectedFiles;

	@Wire
	private Button buildECL;

	@Wire
	private Button submitECL;

	@Wire
	private Button addfiles;

	@Wire
	private Tabpanel sourceCodeBuilder;

	private Include eclBuilderInclude;
	@Wire
	private Tab sourceCodeTab;

	@Wire
	private Tabbox tabbox;

	private DashboardConfig dashboardConfig;

	private String logicalFilesForBuilder = "";

	private String userAction;

	@Wire
	private Include importInclude;

	@Wire
	private Tab importFileAlternate;

	@Wire
	private Tab filterFiles;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		hpccID = (String) Executions.getCurrent().getArg().get("hpccConnID");

		userAction = (String) Executions.getCurrent().getArg().get("userAction");

		eclBuilderName = (String) Executions.getCurrent().getAttribute("BuilderName");

		Remove.setVisible(false);

        Platform platform = Platform.get("http", "10.173.147.1", 8010, "", "");

        HPCCWsClient connector = platform.getHPCCWSClient();

        
		Builder builder = (Builder) Executions.getCurrent().getArg().get("selectedBuilder");
		/*
		 * if(userAction.equals("clone")){ // Builder builder = (Builder)
		 * Executions.getCurrent().getAttribute("selectedBuilder"); Textbox box
		 * = new Textbox(); box.setValue(builder.getEclbuildercode()); //
		 * sourceCodeBuilder.appendChild(box); logicalFilesForBuilder =
		 * builder.getLogicalFiles(); // submitECL(); return; }else
		 * if(userAction.equals("edit")){ // Builder builder = (Builder)
		 * Executions.getCurrent().getAttribute("selectedBuilder");
		 * 
		 * Textbox box = new Textbox();
		 * box.setValue(builder.getEclbuildercode()); //
		 * sourceCodeBuilder.appendChild(box); logicalFilesForBuilder =
		 * builder.getLogicalFiles();
		 * 
		 * ListModelList<Object> listModel = new ListModelList<Object>();
		 * List<String> selectFiles = new ArrayList<String>(); for(Object obj :
		 * listModel){ selectFiles.add(obj.toString()); } hpccID =
		 * builder.getHpccId(); // commenting out as we do not need to keep the
		 * previously selected Files in the file browse at the next time //
		 * for(String fileName :
		 * Arrays.asList(logicalFilesForBuilder.split(","))){ //
		 * listModel.add(fileName); // } listModel.setMultiple(true);
		 * selectedFiles.setModel(listModel); }
		 */
		/*
		 * DefaultTreeModel<FileMeta> treeMod = getTreeModel();
		 * treeMod.setMultiple(true); treeGrid.setModel(treeMod);
		 * treeGrid.setTreeitemRenderer(new FileInfoRenderer());
		 * treeGrid.setVisible(true); treeGrid.setMultiple(true);
		 */

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
		treeGrid.setStyle("overflow: auto;overflow-y:auto");
		
		HPCCConnection connection =  HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);

		if (null != rootFolder.getListOfFolders()) {
			TreeCreation.buildTree(rootFolder.getListOfFolders(), children, Add, connector);
			for (Component titem : children.getChildren()) {
				if (titem instanceof Treeitem && titem.getChildren() != null) {
					titem.addForward("onOpen", treeGrid, "onClickAction");
				}
			}
		}

		selectedFiles.setStyle("text-align: left");
		/*
		selectedFiles.setItemRenderer(new ListitemRenderer<String>() {

			@Override
			public void render(Listitem listItem, String value, int arg2) throws Exception {
				// TODO Auto-generated method stub
				listItem.setValue(value);
				listItem.setLabel(value);
				listItem.addForward("onDoubleClick", Remove, "onClick");
				listItem.setStyle("align:center");
			}
		});*/
	}

	public void addListeners(List<Component> children) {
		for (Component titem : children) {
			// titem.addEventListener("onClick", e ->
			// onClickAddChildren(e));
			if (titem instanceof Treeitem && titem.getChildren() != null) {
				titem.addForward("onOpen", treeGrid, "onClickAction");
				// titem.addForward("onClick", treeGrid, "onClickAction");
				// titem.addForward("onSelect", treeGrid, "onClickAction");
			}
		}
	}

	@Listen("onClick = #newDataSource")
	public void showModal(Event e) {
		// dashboardConfig.setFileBrowser(getSelf());
		// importInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG,
		// dashboardConfig);
		// importInclude.setDynamicProperty(Dashboard.WIDGET_CONFIG,
		// widgetConfig);
		// importInclude.setDynamicProperty(Constants.FILE, logicalDirName);
		importInclude.setDynamicProperty("hpccConnID", hpccID);
		importInclude.setSrc("eclBuilder/import_file.zul");
		importFileAlternate.setSelected(true);
		filterFiles.setDisabled(true);
		sourceCodeTab.setDisabled(true);
	}

	@Listen("onSprayCancel = #importInclude")
	public void onSprayCancel(Event e) {
		filterFiles.setDisabled(false);
		filterFiles.setSelected(true);
	}

	@Listen("onClick = #addfiles")
	public void onAddFiles(Event evnt) {
		// System.out.println(evnt.getTarget().getId() +
		// evnt.getTarget().getParent().getId());
		Events.postEvent("onAddFiles", evnt.getTarget().getParent().getParent().getParent(),
				((List<String>) selectedFiles.getModel()));
		evnt.getTarget().getParent().getParent().detach();
	}

	@Listen("onSprayDone = #importInclude")
	public void onSprayDone(Event e) {
		ListModelList<Object> selFiles = (ListModelList<Object>) selectedFiles.getModel();
		if (null == selFiles) {
			selFiles = new ListModelList<Object>();
		}
		String uploadedFile = (String) e.getData();
		uploadedFile = uploadedFile.contains("::") ? uploadedFile : ".::" + uploadedFile;
		selFiles.add(uploadedFile);
		
		selectedFiles.setModel(selFiles);
		filterFiles.setSelected(true);
		importFileAlternate.setDisabled(true);
		filterFiles.setDisabled(false);
	}

	@Listen("onClick=#Add")
	public void addSelectedFiles(Event e) {
		ListModelList<Object> listModel = selectedFiles.getModel() != null
				? (ListModelList<Object>) selectedFiles.getModel() : new ListModelList<Object>();
		List<String> selectFiles = new ArrayList<String>();
		for (Object obj : listModel) {
			selectFiles.add(obj.toString());
		}
		if (e instanceof ForwardEvent) {
			String selectedVal = ((Treeitem) ((ForwardEvent) e).getOrigin().getTarget()).getValue();
			if (!listModel.contains(selectedVal)) {
				listModel.add(selectedVal);
			}
		} else {
			for (Component comp : treeGrid.getSelectedItems()) {
				if (!selectFiles.contains(((Treeitem) comp).getValue())) {
					listModel.add(((Treeitem) comp).getValue());
				}
			}
		}
		listModel.setMultiple(true);
		selectedFiles.setModel(listModel);
		// listModel.addEventListener("onDoubleClick", event ->
		// {removeSelectedFiles(event);});
		if (selectedFiles.getModel().getSize() > 0) {
			addfiles.setDisabled(false);
		}
	}

	@Listen("onClick=#Remove")
	public void removeSelectedFiles(Event e) {
		ListModelList<Object> listModel = (ListModelList<Object>) selectedFiles.getModel();
		Set<Listitem> selItems = new HashSet<Listitem>();
		if (e instanceof ForwardEvent) {
			selItems.add(((Listitem) ((ForwardEvent) e).getOrigin().getTarget()));
		} else {
			selItems.addAll(selectedFiles.getSelectedItems());
		}
		for (Object obj : selItems) {
			listModel.remove(((Listitem) obj).getValue());
		}
		selectedFiles.setModel(listModel);
		if (selectedFiles.getModel().getSize() == 0) {
			addfiles.setDisabled(true);
		}

	}

	private String formBasicECLForFile(String logicalFile, boolean addOutput) {

		logicalFile = logicalFile.startsWith("~") ? logicalFile : "~" + logicalFile;
		HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);
		String tempStrArr[] = logicalFile.split("::");
		String datasetName = tempStrArr[tempStrArr.length - 1];
		DFUFileDetailInfo dfuFileDetail;
		try {
			dfuFileDetail = ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getFileDetail(logicalFile,
					connection, connection.getThorCluster());

			String eclCode = datasetName + "recName :=" + dfuFileDetail.getEcl();
			eclCode += "\n\n " + datasetName + " := DATASET(\'" + logicalFile + "\', " + datasetName
					+ "recName, THOR);";

			if (addOutput) {
				eclCode += "\n\n OUTPUT(" + datasetName + ", NAMED(\'" + datasetName + "_Data\'));";
			}
			return eclCode + "\n";

		} catch (HPCCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} finally {
		}
	}

	@Listen("onClick=#submitECL")
	public void submitECL() {

		sourceCodeBuilder.getChildren().stream().filter(comp -> comp.getId().equals("builderCode")).findFirst();
		String eclBuilderCode = ((Textbox) sourceCodeBuilder.getChildren().get(0)).getValue();
		System.out.println(eclBuilderCode);
		try {
			DFUFileDetail dfuFileDetail;

			Platform platform;
			HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);
			platform = Platform.get((connection.getIsHttps() ? "https" : "http"), connection.getServerHost(),
					connection.getServerPort(), connection.getUserName(), connection.getPwd());

			Version v = platform.getVersion();
			System.out.println(v.toString());

			HPCCWsClient connector = platform.getHPCCWSClient();

			String[] clusterGroups = connector.getAvailableClusterGroups();

			/*
			 * dfuFileDetail = ((HPCCService)
			 * SpringUtil.getBean(Constants.HPCC_SERVICE)).getFileDetail(
			 * logicalFilename,
			 * dashboardConfig.getDashboard().getHpccConnection(),
			 * dashboardConfig.getDashboard().getClusterConfig().getThorCluster(
			 * ));
			 * 
			 * String eclCode = "recName :=" + dfuFileDetail.getEcl(); eclCode
			 * += "\n\n datasetValues := DATASET(\'" + logicalFilename +
			 * "\', recName, THOR);"; eclCode +=
			 * "\n\n OUTPUT(datasetValues, NAMED(\'FileData\'));";
			 */

			WorkunitInfo wu = new WorkunitInfo();
			wu.setECL(eclBuilderCode);
			wu.getExceptions();
			wu.setJobname("myflatoutput");
			wu.setCluster(Arrays.asList(clusterGroups).contains("thor") ? "thor" : clusterGroups[1]);
			wu.setResultLimit(100);
			wu.setMaxMonitorMillis(50000);
			wu.setJobname("MyJob");
			// this is just one way to submitECL, you can also submit via
			// ecldirect and request the resulting WUID
			// you can also, submit via WSWorkunits and have more control over
			// the result window you get back.

			String results = connector.submitECLandGetResults(wu);

			JSONObject jsonObj = XML.toJSONObject(results);

			ListModelList<ECLBuilderReportData> reportDetailsList = new ListModelList<ECLBuilderReportData>();

			JSONArray dummyJSONArray = new JSONArray();
			Object datasetValue = ((JSONObject) jsonObj.get("Result")).get("Dataset");

			JSONArray rptJsonArr = (datasetValue instanceof JSONArray ? (JSONArray) datasetValue
					: dummyJSONArray.put(0, (JSONObject) datasetValue));

			JSONObject tempRptJSON;
			List<ECLBuilderReportData> reportDetails = new ArrayList<ECLBuilderReportData>();

			ECLBuilderReportData tempReportDetails = new ECLBuilderReportData();

			for (int i = 0; i < rptJsonArr.length(); i++) {

				tempRptJSON = (JSONObject) rptJsonArr.get(i);
				tempReportDetails = new ECLBuilderReportData();
				tempReportDetails.setBuilderReportName(tempRptJSON.getString("name"));
				tempReportDetails.setRptColumns(getRptColumns(tempRptJSON));
				tempReportDetails.setListData(getRptData(tempRptJSON, tempReportDetails.getRptColumns()));
				reportDetails.add(tempReportDetails);
				reportDetailsList.add(tempReportDetails);
			}
			eclBuilderInclude = (Include) this.getSelf().getParent();

			Events.postEvent("onCompleteUserAction", eclBuilderInclude.getParent().getParent().getParent(), null);
			eclBuilderInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, new DashboardConfig());
			eclBuilderInclude.setDynamicProperty("reportDetails", reportDetailsList);
			eclBuilderInclude.setSrc("eclBuilder/ECLBuilderReport.zul");

			ECLBuilder eclBuilderDtls = new ECLBuilder(
					((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()
							.getId(),
					eclBuilderName, logicalFilesForBuilder, new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()),
					eclBuilderCode, hpccID, wu.getWuid());

			((DSPDao) SpringUtil.getBean("dspDao")).addOrUpdateECLBuilders(eclBuilderDtls, !userAction.equals("edit"));

		} catch (HPCCException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	}

	private List<String> getRptColumns(JSONObject rptJSONObject) {
		try {
			List<String> rptColumns = new ArrayList<String>();
			JSONArray dummyJSONArray = new JSONArray();

			Object obj;

			obj = rptJSONObject.get("Row");

			Iterator<String> iter = obj instanceof JSONArray ? ((JSONObject) ((JSONArray) obj).get(0)).keys()
					: ((JSONObject) obj).keys();

			ArrayList<String[]> listData = new ArrayList<String[]>();

			while (iter.hasNext()) {
				rptColumns.add(iter.next());
			}
			return rptColumns;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private List<String[]> getRptData(JSONObject rptJSONObject, List<String> rptColumns) {
		try {
			String[] tempColArr = new String[0];
			List<String[]> listData = new ArrayList<String[]>();
			// listData.add(rptColumns.toArray(tempColArr));
			JSONObject tempJson;

			JSONArray dummyJSONArray = new JSONArray();

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

	@Listen("onClick=#buildECL")
	public void buildECL(Event e) {

		String builderCode = "";

		if (selectedFiles.getChildren().size() == 1) {
			logicalFilesForBuilder = ((Listitem) selectedFiles.getChildren().get(0)).getValue();
			builderCode = formBasicECLForFile(((Listitem) selectedFiles.getChildren().get(0)).getValue(), true);
		} else {
			for (Component item : selectedFiles.getChildren()) {
				logicalFilesForBuilder += ((Listitem) item).getValue() + ",";
				builderCode += formBasicECLForFile(((Listitem) item).getValue(), false);
			}
			if (logicalFilesForBuilder.endsWith(",")) {
				logicalFilesForBuilder = logicalFilesForBuilder.substring(0, logicalFilesForBuilder.length() - 2);
			}
		}
		List<Component> children = new ArrayList<>();
		children.addAll(sourceCodeBuilder.getChildren());
		for (Component comp : children) {
			sourceCodeBuilder.removeChild(comp);
		}

		Textbox tBox = new Textbox();
		tBox.setId("builderCode");
		tBox.setCols(220);
		tBox.setRows(23);
		tBox.setMultiline(true);
		tBox.setValue(builderCode);
		sourceCodeBuilder.appendChild(tBox);
		sourceCodeTab.setDisabled(false);
		submitECL.setVisible(true);
		tabbox.setOrient("bottom");
		sourceCodeTab.setSelected(true);
	}

	@Listen("onClick=#selectedFiles")
	public void enableDisableRemoveButton(Event e) {
		Remove.setVisible(selectedFiles.getSelectedCount() > 0 ? true : false);
	}

	@Listen("onClickAction=#treeGrid")
	public void onClickAddChildren(ForwardEvent event) {
		Treeitem selectedItem = (Treeitem) event.getOrigin().getTarget();
		if (((Event) event.getOrigin()).getName().equals("onClick") && !selectedItem.isSelected()) {
			// selectedItem.setOpen(false);
			return;
		}
		if (((Event) event.getOrigin()).getName().equals("onOpen") && selectedItem.isSelected()) {
			selectedItem.setSelected(false);
			selectedItem.setOpen(false);
			return;
		}
		
        Platform platform = Platform.get("http", "10.173.147.1", 8010, "", "");

        HPCCWsClient connector = platform.getHPCCWSClient();

		Folder newFolder = TreeCreation.populateTree((selectedItem).getValue(), connector, hpccID);
		if (((Treechildren) selectedItem.getChildren().get(1)).getChildren().size() > 0) {
			/*
			 * if(selectedItem.isOpen()){ selectedItem.setOpen(false); }else{
			 * selectedItem.setOpen(true); }
			 */
			return;
		}
		newFolder.setBaseFolderName("Child");

		if (null != newFolder.getListOfFolders()) {
			for (Component children : treeGrid.getTreechildren().getChildren().get(0).getChildren()) {
				if (children instanceof Treechildren) {
					Treechildren tc = new Treechildren();
					Treeitem newItem = ((Treeitem) event.getOrigin().getTarget());

					// newItem.addEventListener("onClick", e ->
					// onClickAddChildren(e));
					newItem.addForward("onOpen", treeGrid, "onClickAction");
					// newItem.addForward("onClick", treeGrid, "onClickAction");
					// newItem.addForward("onSelect", treeGrid,
					// "onClickAction");
					if (newItem.getChildren().size() > 0) {
						Treeitem newItem1 = new Treeitem();
						if (newItem.getChildren().size() > 1) {
							tc = ((Treechildren) newItem.getChildren().get(1));
						} else {
							tc = new Treechildren();
							newItem.appendChild(tc);
						}
						tc.appendChild(newItem1);
						// newItem.appendChild(newItem1);
						newItem = newItem1;
					}
					Treerow newRow = new Treerow(newFolder.getBaseFolderName());
					Treechildren newChild = new Treechildren();
					newItem.appendChild(newRow);
					newItem.appendChild(newChild);
					children.appendChild(newItem);
					if (null != newFolder.getListOfFiles()) {
						for (File file : newFolder.getListOfFiles()) {
							Treeitem childItem = new Treeitem(file.getFileName());
							childItem.setValue(file.getFileName());
							childItem.addForward("onDoubleClick", Add, "onClick");
							tc.appendChild(childItem);
						}
					}
					
					HPCCConnection connection =  HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);
					
					TreeCreation.buildTree(newFolder.getListOfFolders(), tc, Add, connector);

					for (Component titem : tc.getChildren()) {
						// titem.addEventListener("onClick", e ->
						// onClickAddChildren(e));
						titem.addForward("onOpen", treeGrid, "onClickAction");
						// titem.addForward("onClick", treeGrid,
						// "onClickAction");
						// titem.addForward("onSelect", treeGrid,
						// "onClickAction");
					}
					break;
				}
			}
		} else if (null != newFolder.getListOfFiles()) {
			Treechildren tc = (Treechildren) selectedItem.getChildren().get(1);
			for (File file : newFolder.getListOfFiles()) {
				Treeitem childItem = new Treeitem(file.getFileName());
				childItem.setValue(file.getFileName());
				childItem.addForward("onDoubleClick", Add, "onClick");
				tc.appendChild(childItem);
			}
		}
	}

	public DefaultTreeModel<FileMeta> getTreeModel() {
		return (new DefaultTreeModel<FileMeta>(getFileInfoTreeData(null, "")));
	}

	/*
	 * public FileInfoRenderer getTreeRenderer() { return new
	 * FileInfoRenderer(); }
	 */

	private void loadChildrenNodes(Event e) {
		FileMetaTreeNode seltreeNode = ((Treeitem) e.getTarget().getParent()).getValue();

		seltreeNode.setLeaf(false);

		getFileInfoTreeData(seltreeNode.getData(), seltreeNode.getData().getFileName());

		// treeGrid.getModel().getRoot()

		// new FileMetaTreeNode(((FileMeta)seltreeNode.getData()),
		// getFileInfoTreeData(seltreeNode.getData().getFileName()));

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

	@Override
	public void onEvent(Event event) throws Exception {
		// TODO Auto-generated method stub

	}
}
