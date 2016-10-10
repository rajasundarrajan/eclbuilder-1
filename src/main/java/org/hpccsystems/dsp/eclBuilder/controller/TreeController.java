package org.hpccsystems.dsp.eclBuilder.controller;

import java.util.ArrayList;
import java.util.List;

import org.hpccsystems.dsp.eclBuilder.domain.File;
import org.hpccsystems.dsp.eclBuilder.domain.Folder;
import org.hpccsystems.dsp.eclBuilder.util.TreeCreation;
import org.hpccsystems.ws.client.HPCCWsClient;
import org.hpccsystems.ws.client.platform.Platform;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

public class TreeController extends SelectorComposer<Component>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Wire
	private Tree folderTree;
	
	@Wire
	private Button click; 
	
	private List<Folder> FoldersList = new ArrayList<>();
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		
        Platform platform = Platform.get("http", "10.173.147.1", 8010, "", "");

        HPCCWsClient connector = platform.getHPCCWSClient();

		
		Folder rootFolder = TreeCreation.populateTree("dsf", connector, "dsafds");
		Treechildren rootChildren = new Treechildren();
		
		Treeitem treeItem = new Treeitem();
		Treerow treeRow = new Treerow(rootFolder.getBaseFolderName());
		Treechildren children = new Treechildren();
		
		treeItem.appendChild(treeRow);
		treeItem.appendChild(children);
		
		rootChildren.appendChild(treeItem);
		
		folderTree.appendChild(rootChildren);
		
	}
	
	@Listen("onClick = #click")
	public void onClickAddChildren(Event event) {
		Folder newFolder = createNewFolder();
		newFolder.setBaseFolderName("Child");
		if(!(folderTree.getSelectedCount() > 0)){
			return;
		}
		
		if(null != newFolder.getListOfFolders()){
			for(Component children : folderTree.getTreechildren().getChildren().get(0).getChildren()){
				if(children instanceof Treechildren){
					Treechildren tc = new Treechildren();
					Treeitem newItem =  (Treeitem) folderTree.getSelectedItems().toArray()[0];
					if(newItem.getChildren().size() > 0){
						Treeitem newItem1 = new Treeitem();
						tc = ((Treechildren)newItem.getChildren().get(1));
						tc.appendChild(newItem1);
						newItem = newItem1;
					}
					Treerow newRow = new Treerow(newFolder.getBaseFolderName());
					Treechildren newChild = new Treechildren();
					newItem.appendChild(newRow);
					newItem.appendChild(newChild);
					children.appendChild(newItem);
					break;
				}
			}
		}
	}

	private Folder createNewFolder() {
		Folder newFolder = new Folder();
		newFolder.setBaseFolderName("New Folder");

		Folder insurance = new Folder();
		insurance.setBaseFolderName("Insurance");
		List<File> newListOfFiles = new ArrayList<>();

		File newFile = new File();
		newFile.setFileName("Insurance.txt");

		newListOfFiles.add(newFile);
		insurance.setListOfFiles(newListOfFiles);

		List<Folder> newListOfFolders = new ArrayList<>();
		newListOfFolders.add(insurance);

		newFolder.setListOfFolders(newListOfFolders);
		return newFolder;
	}


	/**
	 * @return the foldersList
	 */
	public List<Folder> getFoldersList() {
		return FoldersList;
	}


	/**
	 * @param foldersList the foldersList to set
	 */
	public void setFoldersList(List<Folder> foldersList) {
		FoldersList = foldersList;
	}
	
	
}
