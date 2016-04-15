package com.sfdb;

import java.io.IOException;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;


public class IsUserDocLibOnlyWebscript extends AbstractWebScript {

	public static final QName _IS_DOC_LIB = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "isDocLib"); 
	
	private NodeService nodeService;
    private PersonService personService;
	private SiteService siteService;

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }
    
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse resp) throws IOException {
		boolean result = false;
		JSONObject obj = new JSONObject();
		String userName = req.getParameterValues("userid") == null ? null
				: req.getParameterValues("userid")[0];
		if (userName == null) {
			userName = AuthenticationUtil.getFullyAuthenticatedUser();
		}
		try {
			if (userName != null && personService.personExists(userName)) {
				NodeRef person = personService.getPerson(userName);
				Object o = nodeService.getProperty(person, _IS_DOC_LIB);
				if (o != null) {
					result = (Boolean) o;
					if (result && req.getParameterValues("getsiteid") != null) {
						obj.put("siteid", siteService.listSites(userName).get(0).getShortName());
					}
				}
			}
			obj.put("result", result);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String jsonString = obj.toString();
		resp.getWriter().write(jsonString);
	}
}
