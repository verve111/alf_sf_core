package com.sfdb;

import java.io.IOException;

import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.json.JSONArray;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class DoclibPropUpdateWebscript extends AbstractWebScript {
	
	private SiteService siteService;
	private NodeService nodeService;

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}	
	
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		JSONArray obj = new JSONArray();
		String prop = req.getParameterValues("prop") == null ? null
				: req.getParameterValues("prop")[0];
		String value = req.getParameterValues("value") == null ? null
				: req.getParameterValues("value")[0];
		if (prop == null || value == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "prop or value not defined");
		}
		for (SiteInfo i : siteService.findSites(null, 0)) {
			String n = i.getShortName();
			NodeRef doclib = siteService.getContainer(n, SiteService.DOCUMENT_LIBRARY);
			if (nodeService.getProperty(doclib, SiteModel.PROP_COMPONENT_ID) == null) {
				nodeService.setProperty(doclib, SiteModel.PROP_COMPONENT_ID, SiteService.DOCUMENT_LIBRARY);
				obj.put(n);
			}
		}
		String jsonString = obj.toString();
		res.getWriter().write(jsonString);
	}

}
