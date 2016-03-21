package com.sfdb;

import java.io.IOException;
import java.text.MessageFormat;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.scheduled.CronScheduledQueryBasedTemplateActionDefinition;
import org.alfresco.repo.calendar.CalendarModel;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.CronTriggerBean;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.json.JSONArray;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.sfdb.extended.CronScheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteAssociationWebscript extends AbstractWebScript implements ApplicationContextAware {
	
	private Logger logger = LoggerFactory.getLogger(DeleteAssociationWebscript.class);
	private NodeService nodeService;
	private SearchService searchService;
	private ApplicationContext context;

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse resp) throws IOException {
		
		System.out.println(DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"));
		if (true) return;		
		SearchParameters sp = new SearchParameters();
		sp.addStore(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
		// use CMIS-Alfresco, because Solr is not yet fully started (we can use fts-alfresco, but it should be
		// restricted (no PATH))
		// more info:
		// https://forums.alfresco.com/forum/developer-discussions/repository-services/executing-search-solr-alfresco-startup-02272015-0806
		sp.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
		sp.setQuery(MessageFormat.format("select * from ia:calendarEvent where ia:fromDate > ''{0}''",
				DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:SS")));
		ResultSet results = null;
		try {
			results = searchService.query(sp);
			for (ResultSetRow row : results) {
				NodeRef currentNodeRef = row.getNodeRef();
				logger.info("Cron calendar event started, name = '"
						+ nodeService.getProperty(currentNodeRef, CalendarModel.PROP_WHAT) + "', "
						+ currentNodeRef.toString());
			}
		} finally {
			if (results != null) {
				results.close();
			}
		}
		if (true) return;
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

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

}
