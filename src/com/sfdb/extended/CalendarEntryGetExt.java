package com.sfdb.extended;

import java.util.Map;

import org.alfresco.repo.web.scripts.calendar.CalendarEntryGet;
import org.alfresco.service.cmr.calendar.CalendarEntry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class CalendarEntryGetExt extends CalendarEntryGet {

	@Override
	protected Map<String, Object> executeImpl(SiteInfo site, String eventName, WebScriptRequest req, JSONObject json,
			Status status, Cache cache) {
		Map<String, Object> map = super.executeImpl(site, eventName, req, json, status, cache);
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) map.get("result");
		CalendarEntry entry = calendarService.getCalendarEntry(site.getShortName(), eventName);
		String cronExpr = (String) nodeService.getProperty(entry.getNodeRef(), CalendarCrons._IA_CRON);
		result.put("cronExpr", cronExpr);
		return map;
	}

}
