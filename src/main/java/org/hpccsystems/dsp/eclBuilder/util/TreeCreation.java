/**
 * 
 */
package org.hpccsystems.dsp.eclBuilder.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis.utils.StringUtils;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.eclBuilder.controller.FileMetaTreeNode;
import org.hpccsystems.dsp.eclBuilder.domain.File;
import org.hpccsystems.dsp.eclBuilder.domain.Folder;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.FileMeta;
import org.hpccsystems.ws.client.HPCCWsClient;
import org.hpccsystems.ws.client.gen.wsdfu.v1_34.DFUInfoResponse;
import org.hpccsystems.ws.client.gen.wsdfu.v1_34.DFULogicalFile;
import org.zkoss.util.resource.Labels;
import org.zkoss.zul.Button;
import org.zkoss.zul.Image;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

/**
 * @author Bhuvaneswari_L02
 *
 */
public class TreeCreation {

	public static Folder populateTree(String currentFolder, HPCCWsClient connection, String hpccID) {

		
//	    HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);
	    FileMeta newFile = new FileMeta();
	    
		newFile.setFileName("");
		
		List<FileMetaTreeNode> rootFile = new ArrayList<FileMetaTreeNode>();
		rootFile.add(new FileMetaTreeNode(newFile));

		List<FileMeta> files = new ArrayList<FileMeta>();
		
		try {
			files = getFileList(currentFolder, connection, hpccID);
		} catch (HPCCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Folder rootFolder = new Folder();
		rootFolder.setBaseFolderName("C");
		Folder f1;
		File file1;
		List<File> newFileList = new ArrayList<File>();
		List<Folder> newFolderList = new ArrayList<Folder>();

		for(FileMeta fm : files){
			
			if(fm instanceof org.hpccsystems.dsp.eclBuilder.domain.FileMeta){
				file1 = new File();
				file1.setFileName(fm.getFileName());
				file1.setActualFileName(fm.getDisplayFileName());
				
					newFileList.add(file1);
					rootFolder.setListOfFiles(newFileList);
			
			}else if(fm instanceof FileMeta){
				f1 = new Folder();
				if(!fm.isDirectory()){
					f1.setIsFile(true);
				}
				String[] nameParts = fm.getFileName().split("::");
				String name = nameParts[nameParts.length - 1];
				f1.setBaseFolderName(name);
				f1.setActualFolderName(((!f1.getIsFile() && !StringUtils.isEmpty(currentFolder)) ? currentFolder + "::" : "") + fm.getFileName());
				if(null != rootFolder.getListOfFolders()){
					rootFolder.getListOfFolders().add(f1);
				}else{
					newFolderList.add(f1);
					rootFolder.setListOfFolders(newFolderList);
				}
			}
			
		}
		return rootFolder;

/*
		Folder amm = new Folder();
		amm.setBaseFolderName("AMM");

		Folder amm1 = new Folder();
		amm1.setBaseFolderName("AMM1");

		Folder amm2 = new Folder();
		amm2.setBaseFolderName("AMM2");

		Folder dsp = new Folder();
		dsp.setBaseFolderName("DSP");

		Folder dsp1 = new Folder();
		dsp1.setBaseFolderName("DSP1");

		File amm1File = new File();
		File amm2File = new File();
		File dsp1File = new File();

		amm1File.setFileName("AMM1.txt");
		amm2File.setFileName("AMM2.txt");
		dsp1File.setFileName("DSP1.txt");

		List<Folder> rootFolderList = new ArrayList<>();
		List<Folder> ammList = new ArrayList<>();
		List<Folder> dspList = new ArrayList<>();

		List<File> amm1List = new ArrayList<>();
		List<File> amm2List = new ArrayList<>();
		List<File> dsp1List = new ArrayList<>();

		amm1List.add(amm1File);
		amm2List.add(amm2File);
		dsp1List.add(dsp1File);

		amm1.setListOfFiles(amm1List);
		amm2.setListOfFiles(amm2List);
		dsp1.setListOfFiles(dsp1List);

		rootFolderList.add(amm);
		rootFolderList.add(dsp);
		rootFolder.setListOfFolders(rootFolderList);

		amm.setListOfFolders(ammList);
		ammList.add(amm1);
		ammList.add(amm2);

		dsp.setListOfFolders(dspList);
		dspList.add(dsp1);
		
		return rootFolder;
*/
	}
		
	   public static List<FileMeta> getFileList(String scope, HPCCWsClient hpccConnection, String hpccID) throws HPCCException {

	        List<FileMeta> results = new ArrayList<FileMeta>();

	        DFULogicalFile[] resultsArray;
	        try {
	        	
	        	try{
	        		
	        	DFULogicalFile[] files =  hpccConnection.getWsDFUClient().getFiles(scope);
//	        			hpccConnection.getFilenames(scope, hpccConnection.getThorCluster());
	        
	        	if(files == null || files.length == 0){
	        		
	        		Map<String, String> mapValues = formBasicECLForFile(scope, false, hpccConnection);
	        		
	        		for(Map.Entry<String, String> s: mapValues.entrySet()){
	        			
	        			org.hpccsystems.dsp.eclBuilder.domain.FileMeta fm = new org.hpccsystems.dsp.eclBuilder.domain.FileMeta();
	        			fm.setFileName(s.getKey());
	        			fm.setDisplayFileName(s.getValue());
	        			results.add(fm);
	        		}
	        		return results;
	        		
	        	}
	        	
	        	}catch(Exception e){
	        		
	        	}
	        
	            resultsArray = hpccConnection.getWsDFUClient().getFiles(scope);
//(scope, hpccConnection.getThorCluster());
	            FileMeta fileMeta;

	            for (DFULogicalFile hpccLogicalFile : resultsArray) {
	                fileMeta = new FileMeta();
	                if (hpccLogicalFile.getIsDirectory()) {
	                    fileMeta.setIsDirectory(true);
	                    fileMeta.setFileName(hpccLogicalFile.getDirectory());
//	                    fileMeta.setDisplayFileName(name);
	                    fileMeta = settingScope(scope, fileMeta, hpccLogicalFile);
	                } else {
	                    fileMeta.setIsDirectory(false);
	                    fileMeta.setFileName(hpccLogicalFile.getName());
//	                    fileMeta.setDisplayFileName(name);
	                    fileMeta.setScope(hpccLogicalFile.getName());
	                }
	                results.add(fileMeta);
	            }
	        } catch (Exception e) {
	            throw new HPCCException(Labels.getLabel("unableToFetchFileList"), e);
	        }

	        return results;
	    }
	   

	    private static FileMeta settingScope(String scope, FileMeta fileMeta, DFULogicalFile hpccLogicalFile) {
	        if (scope.length() > 0) {
	            fileMeta.setScope(scope + "::" + hpccLogicalFile.getName());
	        } else {
	            fileMeta.setScope("~" + hpccLogicalFile.getName());
	        }
	        return fileMeta;
	    }

	
	public static void buildTree(List<Folder> listOfFolders, Treechildren treeChildren, Button addButton, HPCCWsClient connection){
		for(Folder folders : listOfFolders){
			
			Treeitem item = new Treeitem();
			item.setValue(folders.getActualFolderName());
			treeChildren.appendChild(item);
			item.setCheckable(false);
			

			
			Treerow row = new Treerow(folders.getBaseFolderName());
			item.appendChild(row);
			Treechildren children = new Treechildren();
			item.appendChild(children);
			item.setOpen(false);
			
			if(isLogicalFileSel(folders.getActualFolderName(), connection)){
				item.setDraggable("true");
				item.setAttribute("type", "file");
				item.setImage("/ramps/icons/FileOpen.png");
			}else{
				item.setAttribute("type", "folder");
				item.setImage("/ramps/icons/FolderOpen.png");
			}
			
			if(null != folders.getListOfFolders()){
				children = new Treechildren();
				item.appendChild(children);
				buildTree(folders.getListOfFolders(), children, addButton, connection);
				
			} else if(null != folders.getListOfFiles()){
				children = new Treechildren();
				item.appendChild(children);
				
				for(File files : folders.getListOfFiles()){
					Treeitem childItem = new Treeitem(files.getFileName());
					childItem.setValue(files.getFileName());
					childItem.setDraggable("true");
					childItem.appendChild(new Image("/images/thumbnail/chart_db.png"));
					childItem.setAttribute("isLogicalFile", true);
					childItem.appendChild(new Treechildren());
					children.appendChild(childItem);
//					childItem.addForward("onDoubleClick", addButton, "onClick");
				}
			}
			
		}
	}
	
	public static boolean isLogicalFileSel(String fileName, HPCCWsClient hpccConnection) {
		try {
			DFULogicalFile[] files = hpccConnection.getWsDFUClient().getFiles(fileName);

			if (files == null || files.length == 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return true;
		}
	}
	
	public static Map<String, String> formBasicECLForFile(String logicalFile, boolean addOutput, HPCCWsClient connection) throws Exception {

		logicalFile = logicalFile.startsWith("~") ? logicalFile : "~" + logicalFile;
		String tempStrArr[] = logicalFile.split("::");
		String datasetName = tempStrArr[tempStrArr.length - 1];
		org.hpccsystems.ws.client.gen.wsdfu.v1_34.DFUFileDetail	dfuFileDetail;
		try {
			dfuFileDetail = ((DFUInfoResponse)connection.getWsDFUClient().getFileInfo(logicalFile, null)).getFileDetail(); 
					
//					((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getFileDetail(logicalFile,
//					connection, connection.getThorCluster());


			String record = dfuFileDetail.getEcl().replace(";\n", "@").replace("\n", "@").replace(",", "@");
			
			String tokens[] = record.replace("{", "").replace("}", "").split("@");
			
			List<String> invalidTokens = new ArrayList<String>();
			
			Map<String, String> validTokens = new HashMap<String,String>();
			
			invalidTokens.add("RECORD");
			invalidTokens.add("END");
			String[] tokenStr;
			for(String s : tokens){
				s = s.trim();
				tokenStr = s.split(" ");
				if(!invalidTokens.contains(s)){
					validTokens.put(tokenStr[1], tokenStr[1] + " (" + tokenStr[0] + ")");
				}
			}
			return validTokens;

		} catch (HPCCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	


}
