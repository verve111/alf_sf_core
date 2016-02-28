package com.sfdb;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.NamespaceDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class SetPropWebscript extends AbstractWebScript {

	private NodeService nodeService;
	private DictionaryService dictionaryService;

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		JSONArray obj = new JSONArray();
		String nodeid = req.getParameterValues("id") == null ? null : req.getParameterValues("id")[0];
		String prop = req.getParameterValues("prop") == null ? null : req.getParameterValues("prop")[0];
		String value = req.getParameterValues("value") == null ? null : req.getParameterValues("value")[0];
		if (prop == null || value == null || nodeid == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "prop or value or nodeid not defined");
		}
		// StoreRef SPACES_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
		NodeRef nr = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeid);
		System.out.println(I18NUtil.getMessage("autonum.prefix.project.code"));
		if (!nodeService.exists(nr)) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "node is not found, id=" + nr.toString());
		} else {
			Collection<QName> models = dictionaryService.getAllModels();
			Iterator<QName> it = models.iterator();
			QName foundModel = null;
			outerloop: while (it.hasNext()) {
				QName model = it.next();
				Collection<QName> types = dictionaryService.getTypes(model);
				Iterator<QName> itTypes = types.iterator();
				while (itTypes.hasNext()) {
					QName type = itTypes.next();
					if (type.equals(nodeService.getType(nr))) {
						foundModel = model;
						break outerloop;
					}
				}
			}
			QName propQname = null;
			if (foundModel != null) {
				for (NamespaceDefinition nsDef : dictionaryService.getModel(foundModel).getNamespaces()) {
					if (nsDef.getPrefix().equals(prop.split(":")[0])) {
						String uri = nsDef.getUri();
						for (QName p : nodeService.getProperties(nr).keySet()) {
							if (p.equals(QName.createQName(uri, prop.split(":")[1]))) {
								propQname = p;
							}
						}
					}
				}
			}
			if (propQname != null) {
				nodeService.setProperty(nr, propQname, value);
				obj.put(MessageFormat.format(
						"Updated value ''{0}'' for nodeRef ''{1}'', propQName ''{2}'', node cm:name ''{3}''", value,
						nr.toString(), propQname.toString(), nodeService.getProperty(nr, ContentModel.PROP_NAME)));
			
			} else {
				obj.put("PropQname not found. Nothing is done.");
			}
			String jsonString = obj.toString();
			res.getWriter().write(jsonString);	
		}
	}

}
