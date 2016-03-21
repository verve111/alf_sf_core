package com.sfdb;

import java.io.IOException;
import java.text.MessageFormat;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class DeleteAssociationWebscript extends AbstractWebScript {
	
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
		String assocFullName = req.getParameterValues("assoc") == null ? "http://www.bcpg.fr/model/project/1.0%%partner" : req.getParameterValues("assoc")[0];
		String assocNamespace = assocFullName.split("%%")[0];
		String assocName = assocFullName.split("%%")[1];
		JSONArray obj = new JSONArray();
		ResultSet resultSet = searchService.query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),
				SearchService.LANGUAGE_LUCENE,
				MessageFormat.format("ASPECT:\"{0}\" OR TYPE:\"{1}\"", "bcpg:entityTplAspect", "pjt:project"));
		for (ResultSetRow row : resultSet) {
			NodeRef entity = row.getNodeRef();
			String entityName = nodeService.getProperty(entity, ContentModel.PROP_NAME).toString();
			if (!"ACL group".equals(entityName)) {
				QName assocQName = QName.createQName(assocNamespace, assocName);
				for (AssociationRef a : nodeService.getTargetAssocs(entity, assocQName)) {
					nodeService.removeAssociation(entity, a.getTargetRef(), assocQName);
					obj.put(entityName);
				}
			}
		}
		resultSet.close();
		String jsonString = obj.toString();
		resp.getWriter().write(jsonString);
	}

}
