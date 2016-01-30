package com.sfdb;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.json.JSONArray;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import fr.becpg.model.BeCPGModel;
import fr.becpg.model.DataListModel;

public class PropsRenameWebscript extends AbstractWebScript {

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
		String listid =  req.getParameterValues("listid") == null ? null : req.getParameterValues("listid")[0];
		String newName = req.getParameterValues("newname") == null ? null : req.getParameterValues("newname")[0];
		JSONArray obj = new JSONArray();
		if (listid == null) {
			obj.put("required listid parameter is not set");
			String jsonString = obj.toString();
			resp.getWriter().write(jsonString);
			return;
		}
		ResultSet resultSet = searchService.query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),
				SearchService.LANGUAGE_LUCENE,
				MessageFormat.format("ASPECT:\"{0}\" OR TYPE:\"{1}\"", "bcpg:entityTplAspect", "pjt:project"));
		for (ResultSetRow row : resultSet) {
			NodeRef entity = row.getNodeRef();
			String entityName = nodeService.getProperty(entity, ContentModel.PROP_NAME).toString();
			if (!"ACL group".equals(entityName)) {
				for (ChildAssociationRef assoc : nodeService.getChildAssocs(entity, BeCPGModel.ASSOC_ENTITYLISTS,
						null)) {
					NodeRef dataListsFolder = assoc.getChildRef();
					List<String> list = new ArrayList<String>();
					if (!NodeRef.isNodeRef(listid)) {
						list.add(listid);
						for (ChildAssociationRef assoc2 : nodeService.getChildrenByName(dataListsFolder,
								ContentModel.ASSOC_CONTAINS, list)) {
							NodeRef dataList = assoc2.getChildRef();
							if (nodeService.getType(dataList).equals(DataListModel.TYPE_DATALIST)) {
								if (newName != null) {
									nodeService.setProperty(dataList, ContentModel.PROP_TITLE, newName);
								}
								obj.put(entityName);
							}
						}
					} else {
						
					}
					break;
				}
			}
		}
		resultSet.close();
		String jsonString = obj.toString();
		resp.getWriter().write(jsonString);
	}
}
