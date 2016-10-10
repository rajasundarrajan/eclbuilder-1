package org.hpccsystems.dsp.eclBuilder.domain;

import java.util.List;

/**
 * 
 * @author Bhuvaneswari_L02
 *
 */
public class Folder {

	private List<File> listOfFiles;
	private List<Folder> listOfFolders;
	private String baseFolderName;
	private String actualFolderName;
	private boolean isFile = false;
	
	
	public boolean getIsFile() {
		return isFile;
	}
	public void setIsFile(boolean isFile) {
		this.isFile = isFile;
	}
	public String getActualFolderName() {
		return actualFolderName;
	}
	public void setActualFolderName(String actualFolderName) {
		this.actualFolderName = actualFolderName;
	}
	/**
	 * @return the listOfFiles
	 */
	public List<File> getListOfFiles() {
		return listOfFiles;
	}
	/**
	 * @param listOfFiles the listOfFiles to set
	 */
	public void setListOfFiles(List<File> listOfFiles) {
		this.listOfFiles = listOfFiles;
	}
	/**
	 * @return the listOfFolders
	 */
	public List<Folder> getListOfFolders() {
		return listOfFolders;
	}
	/**
	 * @param listOfFolders the listOfFolders to set
	 */
	public void setListOfFolders(List<Folder> listOfFolders) {
		this.listOfFolders = listOfFolders;
	}
	/**
	 * @return the baseFolderName
	 */
	public String getBaseFolderName() {
		return baseFolderName;
	}
	/**
	 * @param baseFolderName the baseFolderName to set
	 */
	public void setBaseFolderName(String baseFolderName) {
		this.baseFolderName = baseFolderName;
	}
	
	
}
