package com.sfdb;

import java.io.IOException;
import java.text.MessageFormat;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.json.JSONArray;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import fr.becpg.model.BeCPGModel;

public class ProjectCodeUpdateWebscript extends AbstractWebScript {

	private NodeService nodeService;
	private SearchService searchService;

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse resp) throws IOException {
		String oldid =  req.getParameterValues("oldid") == null ? null : req.getParameterValues("oldid")[0];
		String newid = req.getParameterValues("newid") == null ? null : req.getParameterValues("newid")[0];
		JSONArray obj = new JSONArray();
		if (oldid == null || newid == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "oldid or newid not defined");
		}
		ResultSet resultSet = searchService.query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),
				SearchService.LANGUAGE_LUCENE,
				MessageFormat.format("ASPECT:\"{0}\" OR TYPE:\"{1}\"", "bcpg:entityTplAspect", "pjt:project"));
		for (ResultSetRow row : resultSet) {
			NodeRef entity = row.getNodeRef();
			String entityName = nodeService.getProperty(entity, ContentModel.PROP_NAME).toString();
			if (!"ACL group".equals(entityName)) {
				String code = nodeService.getProperty(entity, BeCPGModel.PROP_CODE).toString();
				if (code != null && code.toLowerCase().contains(oldid.toLowerCase())) {
					code = code.toLowerCase().replace(oldid.toLowerCase(), newid);
					nodeService.setProperty(entity, BeCPGModel.PROP_CODE, code);
					obj.put(entityName);
				}
			}
		}
		resultSet.close();
		String jsonString = obj.toString();				
		resp.getWriter().write(jsonString);
	}

}
