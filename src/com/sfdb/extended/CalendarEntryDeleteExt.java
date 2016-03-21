package com.sfdb.extended;

import java.util.Map;

import org.alfresco.repo.web.scripts.calendar.CalendarEntryDelete;
import org.alfresco.service.cmr.calendar.CalendarEntry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class CalendarEntryDeleteExt extends CalendarEntryDelete {

	private CalendarCrons calendarCrons;
	//private Logger logger = LoggerFactory.getLogger(CalendarEntryDeleteExt.class);

	public void setCalendarCrons(CalendarCrons calendarCrons) {
		this.calendarCrons = calendarCrons;
	}

	@Override
	protected Map<String, Object> executeImpl(SiteInfo site, String eventName, WebScriptRequest req, JSONObject json,
			Status status, Cache cache) {
		CalendarEntry entry = calendarService.getCalendarEntry(site.getShortName(), eventName);
		if (entry != null) {
			calendarCrons.removeEvent(entry.getNodeRef());
		}
		return super.executeImpl(site, eventName, req, json, status, cache);
	}

}
