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
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class GetProjectMainTemplateWebscript extends AbstractWebScript {

	private NodeService nodeService;
	private SearchService searchService;

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		JSONObject obj = new JSONObject();
		ResultSet resultSet = searchService.query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),
				SearchService.LANGUAGE_LUCENE,
				MessageFormat.format(
						"ASPECT:\"{0}\" AND @bcpg\\:entityTplEnabled:true AND @bcpg\\:entityTplIsDefault:true",
						"bcpg:entityTplAspect"));
		boolean isFound = false;
		for (ResultSetRow row : resultSet) {
			NodeRef projectTpl = row.getNodeRef();
			if ("SFDB TEMPLATE".equals(nodeService.getProperty(projectTpl, ContentModel.PROP_NAME))) {
				try {
					obj.put("res", projectTpl);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				isFound = true;
				break;
			}
		}
		if (!isFound) {
			// put the rest 1st template
			for (ResultSetRow row : resultSet) {
				NodeRef projectTpl = row.getNodeRef();
				if (!"ACL group".equals(nodeService.getProperty(projectTpl, ContentModel.PROP_NAME))) {
					try {
						obj.put("res", projectTpl);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
		resultSet.close();
		String jsonString = obj.toString();
		res.getWriter().write(jsonString);
	}
}
