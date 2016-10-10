package org.hpccsystems.dsp.ramps.api.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hpcc.HIPIE.dude.Property;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.controller.utils.HtmlGenerator;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.error.ErrorBlock;
import org.json.JSONArray;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zkoss.json.JSONObject;
import org.zkoss.zul.Filedownload;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Controller to handle forms
 * 
 */
@Controller
@RequestMapping("*.do")
public class FormController {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormController.class);
	private static final String STATUS = "status";

	@RequestMapping(value = "/jsonECLRun.do")
	public void getJSONdata(HttpServletRequest request, HttpServletResponse response) {

		try {

			String wuId = request.getParameter("wuId");

			String resultName = request.getParameter("resultName");

			String hpccID = request.getParameter("hpccId");

			HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);

			String wuresults = connection.getWorkunitResult(wuId, resultName);

			org.json.JSONObject resJson = XML.toJSONObject(wuresults);

			JSONArray jsonArr = (JSONArray) ((org.json.JSONObject) resJson.get("Dataset")).get("Row");

			JSONObject responseJSON = new JSONObject();

			responseJSON.put(STATUS, "success");

			responseJSON.put("formHtml", jsonArr.toString());

			response.getWriter().write(responseJSON.toString());

		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
		}

	}

	@RequestMapping(value = "/downloadECLDataReport.do")
	public void downloadECLDataReport(HttpServletRequest request, HttpServletResponse response) throws Exception {

		int rowid = 0;

		String wuId = request.getParameter("wuId");

		String resultName = request.getParameter("resultName");

		String hpccID = request.getParameter("hpccId");

		HPCCConnection connection1 = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccID);

		String wuresults1 = connection1.getWorkunitResult(wuId, resultName);

		org.json.JSONObject resJson1 = XML.toJSONObject(wuresults1);

		net.sf.json.JSONObject netJSON = net.sf.json.JSONObject.fromObject(resJson1.toString());

		net.sf.json.JSONArray jsonArr = (net.sf.json.JSONArray) ((net.sf.json.JSONObject) netJSON.get("Dataset"))
				.get("Row");

		File dir = new File(new java.io.File(".").getCanonicalPath());

		File downloadFile = new File(dir.getAbsolutePath() + "/" + "builderResult.xls");

		FileOutputStream out = new FileOutputStream(downloadFile);

		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFSheet spreadsheet = workbook.createSheet(resultName);

		XSSFRow row = spreadsheet.createRow(rowid++);

		Set<String> keys = ((net.sf.json.JSONObject) jsonArr.get(0)).keySet();

		Iterator<String> iter1 = keys.iterator();

		int cellid = 0;
		while (iter1.hasNext()) {
			Cell cell = row.createCell(cellid++);
			cell.setCellValue((iter1.next()));
		}

		net.sf.json.JSONArray a = jsonArr;
		net.sf.json.JSONObject objNew;
		ListIterator<?> listIter = ((net.sf.json.JSONArray) a).listIterator();
		List<Object> listObj = new ArrayList<Object>();
		while (listIter.hasNext()) {
			row = spreadsheet.createRow(rowid++);
			objNew = (net.sf.json.JSONObject) listIter.next();
			iter1 = keys.iterator();
			cellid = 0;
			String sss = Constants.EMPTY_STR;
			String key = Constants.EMPTY_STR;
			while (iter1.hasNext()) {
				key = iter1.next();
				listObj.add(objNew.get(key));
				Cell cell = row.createCell(cellid++);
				try {
					Object c = (objNew.get(key));
					if (c instanceof Boolean) {
						if ((Boolean) c) {
							sss = Constants.TRUE_STR;
						} else {
							sss = Constants.FALSE_STR;
						}
					} else {
						sss = (null != c) ? c.toString() : Constants.EMPTY_STR;
					}

					cell.setCellValue(sss);
				} catch (Exception e) {
					System.err.println(Constants.ERROR_STR + e.getMessage() + sss);
				}
			}

		}

		row = workbook.getSheetAt(0).getRow(0);

		for (int colNum = 0; colNum < row.getLastCellNum(); colNum++) {
			workbook.getSheetAt(0).autoSizeColumn(colNum);
		}


		if (workbook != null) {
			try {
				String fileName = "myfile_" + (new Random()).nextInt();
				response.setHeader("Pragma", "no-cache");
				response.setDateHeader("Expires", 0);
				response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + ".xlsx\"");
				workbook.write(response.getOutputStream());
				response.getOutputStream().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@RequestMapping(value = "/pluginsave.do")
	public void savePluginForm(HttpServletRequest request, HttpServletResponse response) {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		JSONObject responseJSON = new JSONObject();
		try {

			Map<String, String[]> tempParam = request.getParameterMap();
			Map<String, Property> reqParam = new HashMap<String, Property>();
			for (Entry<String, String[]> param : tempParam.entrySet()) {
				Property prop = new Property();
				for (int i = 0; i < param.getValue().length; i++) {
					prop.add(param.getValue()[i]);
				}
				reqParam.put(param.getKey(), prop);

			}
			LOGGER.debug("reqParam -> {}", reqParam);
			int pluginIndex = Integer.parseInt(reqParam.get(HtmlGenerator.PLUGIN_ID).get(0));
			String canonicalName = reqParam.get(HtmlGenerator.CANONICAL_NAME).get(0);
			String formHolderUuid = reqParam.get(HtmlGenerator.FORM_HOLDER_UUID).get(0);

			HttpSession session = request.getSession();
			@SuppressWarnings("unchecked")
			Map<String, Project> openProjects = (HashMap<String, Project>) session
					.getAttribute(Constants.OPEN_PROJECTS);
			Project project = openProjects.get(canonicalName);

			LOGGER.debug("Index of plugin to save -> {}", pluginIndex);
			LOGGER.debug("Plugin list -> {}", project.getPlugins());

			Plugin plugin = project.getPlugins().get(pluginIndex);

			Map<String, Property> paramMap = new HashMap<String, Property>();
			paramMap.putAll(reqParam);

			paramMap.remove(HtmlGenerator.PLUGIN_ID);
			paramMap.remove(HtmlGenerator.CANONICAL_NAME);

			ErrorBlock error = plugin.getContractInstance().setAllProperties(paramMap);
			String form = HtmlGenerator.generateForm(project, plugin, pluginIndex, formHolderUuid,
					project.isShowGlobalVariable());
			LOGGER.debug("HIPIE Error - {}\n Form\n{}", error, form);

			responseJSON.put(STATUS, "success");
			responseJSON.put("formHtml", form);

			response.getWriter().write(responseJSON.toJSONString());
		} catch (Exception e) {
			LOGGER.error(Constants.EXCEPTION, e);
			try {
				responseJSON.put(STATUS, "failed");
				responseJSON.put("message", e.getMessage());

				response.getWriter().write(responseJSON.toJSONString());
			} catch (IOException ex) {
				LOGGER.error(Constants.EXCEPTION, ex);
			}
		}

	}
}
