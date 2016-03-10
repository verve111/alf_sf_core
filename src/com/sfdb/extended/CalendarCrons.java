package com.sfdb.extended;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.management.subsystems.ApplicationContextFactory;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class CalendarCrons implements ApplicationContextAware {
	
	private SearchService searchService;
	private NodeService nodeService;
	
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void init() {
		AuthenticationUtil.runAs(new RunAsWork<String>()

		   {

				public String doWork() throws Exception

				{

					return exec();

				}

		   }, AuthenticationUtil.getSystemUserName());		
	
	}
	
	private String exec() {

		String query = "+PATH:\"/app:company_home/st:sites//*/cm:calendar//*\" AND +@ia\\:fromDate:[NOW TO MAX]";
		ResultSet resultSet = searchService.query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),
				SearchService.LANGUAGE_LUCENE,
				query);
		for (ResultSetRow row : resultSet) {
			NodeRef entity = row.getNodeRef();
			System.out.println(nodeService.getProperty(entity, ContentModel.PROP_NAME));
		}
		resultSet.close();
		return null;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// TODO Auto-generated method stub
		
	}

}
