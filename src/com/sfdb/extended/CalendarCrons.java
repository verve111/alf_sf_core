package com.sfdb.extended;

import java.io.InputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.calendar.CalendarModel;
import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class CalendarCrons extends BaseProcessorExtension implements ApplicationListener<ContextRefreshedEvent> {

	public static final QName _IA_CRON = QName.createQName(CalendarModel.CALENDAR_MODEL_URL, "cron");
	public static final QName _IA_CRON_IS_ACTIVE = QName.createQName(CalendarModel.CALENDAR_MODEL_URL, "cronIsActive");
	public static final QName _IA_CRON_RECIPIENTS = QName.createQName(CalendarModel.CALENDAR_MODEL_URL, "cronRecipients");
	private Logger logger = LoggerFactory.getLogger(CalendarCrons.class);
	private ApplicationContext context;
	private int counter;

	// synchronizedMap
	private Map<NodeRef, CronScheduled> map = Collections.synchronizedMap(new HashMap<NodeRef, CronScheduled>());

	private SearchService searchService;
	private NodeService nodeService;

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	/**
	 * Need to catch ContextRefreshedEvent, because AppContextAware and PostConstruct are a bit too early triggered.
	 */
	@Override
	public void onApplicationEvent(final ContextRefreshedEvent event) {
		AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<InputStream>() {
			@Override
			public InputStream doWork() throws Exception {
				context = event.getApplicationContext();
				init();
				return null;
			}
		}, AuthenticationUtil.getSystemUserName());
	}

	private void init() {
		SearchParameters sp = new SearchParameters();
		sp.addStore(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
		// use CMIS-Alfresco, because Solr is not yet fully started (we can use fts-alfresco, but it should be
		// restricted (no PATH))
		// more info:
		// https://forums.alfresco.com/forum/developer-discussions/repository-services/executing-search-solr-alfresco-startup-02272015-0806
		sp.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
		sp.setQuery(MessageFormat.format("select * from ia:calendarEvent where ia:fromDate > TIMESTAMP ''{0}''",
				DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")));
		ResultSet results = null;
		try {
			results = searchService.query(sp);
			for (ResultSetRow row : results) {
				NodeRef currentNodeRef = row.getNodeRef();
				String cronExpr = (String) nodeService.getProperty(currentNodeRef, _IA_CRON);
				Object o = nodeService.getProperty(currentNodeRef, _IA_CRON_IS_ACTIVE);
				boolean isActive = o != null && (Boolean) o == true;
				if (cronExpr != null && isActive) {
					startEvent(currentNodeRef, cronExpr);
				}
			}
		} finally {
			if (results != null) {
				results.close();
			}
		}
	}

	public CronScheduled startEvent(NodeRef nodeRef, String cronExpr) {
		Date startDate = (Date) nodeService.getProperty(nodeRef, CalendarModel.PROP_FROM_DATE);
		Object o = nodeService.getProperty(nodeRef, _IA_CRON_IS_ACTIVE);
		boolean isActive = o != null && (Boolean) o == true;
		boolean isStartAfter = startDate.after(new Date());
		CronScheduled cronScheduled = null;
		if (isStartAfter && isActive) {
			cronScheduled = getCronScheduled(nodeRef, cronExpr);
			map.put(nodeRef, cronScheduled);
			log("Started: cron calendar event", cronExpr, nodeRef);
		}
		return cronScheduled;
	}

	private CronScheduled getCronScheduled(NodeRef nodeRef, String cronExpr) {
		CronScheduled cronScheduled = (CronScheduled) context.getBean("CalendarCronScheduled", nodeRef, cronExpr,
				counter++);
		return cronScheduled;
	}

	public void updateEvent(NodeRef nodeRef, String oldCron, String newCron) {
		boolean cronChanged = oldCron != null && newCron != null && !oldCron.equals(newCron);
		// possibly changed start date
		Date startDate = (Date) nodeService.getProperty(nodeRef, CalendarModel.PROP_FROM_DATE);
		boolean isStartAfter = startDate.after(new Date());
		boolean isActive = (Boolean) nodeService.getProperty(nodeRef, _IA_CRON_IS_ACTIVE);
		CronScheduled cronScheduled = map.get(nodeRef);
		if (cronScheduled == null) {
			cronScheduled = startEvent(nodeRef, newCron);
		}
		if (cronScheduled != null) {
			if (isStartAfter && isActive) {
				if (cronChanged) {
					cronScheduled.setCronExpression(newCron);
					rescheduleJobWithNewCron(cronScheduled, newCron);
					logger.info("Cron changed, oldval =" + oldCron + ", newval=" + newCron + ", noderef=" + nodeRef);
				}
			} else {
				removeEvent(nodeRef);
			}
		}
	}

	public void removeEvent(NodeRef nodeRef) {
		if (nodeRef != null) {
			removeJob(nodeRef);
		}
	}

	private void removeJob(NodeRef nodeRef) {
		if (map.get(nodeRef) != null) {
			CronScheduled cron = map.get(nodeRef);
			try {
				cron.getScheduler().deleteJob(cron.getJobName(), cron.getJobGroup());
				map.remove(nodeRef);
				log("Removed: cron calendar event", "empty", nodeRef);
			} catch (SchedulerException e) {
				logger.error("CalendarCrons :: can't stop calendar cron");
			}
		}
	}

	private void rescheduleJobWithNewCron(CronScheduled cronScheduled, String newCron) {
		try {
			Trigger old = cronScheduled.getTrigger();
			CronTrigger trigger = new CronTrigger();
			trigger.setName(old.getName());
			trigger.setGroup(old.getGroup());
			trigger.setJobName(cronScheduled.getJobName());
			trigger.setJobGroup(cronScheduled.getJobGroup());
			trigger.setCronExpression(newCron);
			cronScheduled.getScheduler().rescheduleJob(old.getName(), old.getGroup(), trigger);
		} catch (SchedulerException e) {
			logger.error("CalendarCrons :: can't reschedule calendar cron");
		} catch (ParseException e) {
			logger.error("CalendarCrons :: cron expr can not be parsed/set");
		}
	}

	private void log(String prefix, String cronExpr, NodeRef nodeRef) {
		logger.info(prefix + ", name = '" + nodeService.getProperty(nodeRef, CalendarModel.PROP_WHAT) + "', cron ='"
				+ cronExpr + "', " + nodeRef.toString());
	}
}
