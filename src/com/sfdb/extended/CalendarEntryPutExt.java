package com.sfdb.extended;

import java.util.Map;

import org.alfresco.repo.web.scripts.calendar.CalendarEntryPut;
import org.alfresco.service.cmr.calendar.CalendarEntry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class CalendarEntryPutExt extends CalendarEntryPut {

	private CalendarCrons calendarCrons;
	private Logger logger = LoggerFactory.getLogger(CalendarEntryPutExt.class);

	public void setCalendarCrons(CalendarCrons calendarCrons) {
		this.calendarCrons = calendarCrons;
	}

	@Override
	protected Map<String, Object> executeImpl(SiteInfo site, String eventName, WebScriptRequest req, JSONObject json,
			Status status, Cache cache) {
		CalendarEntry entry = calendarService.getCalendarEntry(site.getShortName(), eventName);
		NodeRef nr = entry.getNodeRef();
		String oldCron = (String) nodeService.getProperty(nr, CalendarCrons._IA_CRON);
		// in super.executeImpl() _IA_CRON becomes null, set it below
		Map<String, Object> map = super.executeImpl(site, eventName, req, json, status, cache);
		// eventName = 1458215510951-3847.ics
		try {
			String newCron = getOrNull(json, "cron");
			if (newCron != null) {
				nodeService.setProperty(nr, CalendarCrons._IA_CRON, newCron);
			} else {
				logger.warn("cronExpr is null, noderef = " + nr);
			}
			calendarCrons.updateEvent(nr, oldCron, newCron);
		} catch (JSONException e) {
			logger.error("JSONException", e);
		}
		return map;
	}
}
