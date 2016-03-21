package com.sfdb.extended;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.alfresco.repo.web.scripts.calendar.AbstractCalendarWebScript;
import org.alfresco.service.cmr.calendar.CalendarEntry;
import org.alfresco.service.cmr.calendar.CalendarEntryDTO;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * 
 * exact copy, extension in the end
 *
 */

public class CalendarEntryPostExt extends AbstractCalendarWebScript {
	
	private CalendarCrons calendarCrons; 

	public void setCalendarCrons(CalendarCrons calendarCrons) {
		this.calendarCrons = calendarCrons;
	}

	@Override
	protected Map<String, Object> executeImpl(SiteInfo site, String eventName, WebScriptRequest req, JSONObject json,
			Status status, Cache cache) {
		final ResourceBundle rb = getResources();
		CalendarEntry entry = new CalendarEntryDTO();
		// TODO Handle All Day events properly, including timezones
		boolean isAllDay = false;
		String cron = null;
		try {
			// Grab the properties
			entry.setTitle(getOrNull(json, "what"));
			entry.setDescription(getOrNull(json, "desc"));
			entry.setLocation(getOrNull(json, "where"));
			entry.setSharePointDocFolder(getOrNull(json, "docfolder"));
			cron = getOrNull(json, "cron");

			// Handle the dates
			isAllDay = extractDates(entry, json);

			// Handle tags
			if (json.containsKey("tags")) {
				StringTokenizer st = new StringTokenizer((String) json.get("tags"), ",");
				while (st.hasMoreTokens()) {
					entry.getTags().add(st.nextToken());
				}
			}
		} catch (JSONException je) {
			String message = rb.getString(MSG_INVALID_JSON);
			return buildError(MessageFormat.format(message, je.getMessage()));
		}

		// Have it added
		entry = calendarService.createCalendarEntry(site.getShortName(), entry);

		// Generate the activity feed for this
		String dateOpt = addActivityEntry("created", entry, site, req, json);

		// Build the return object
		Map<String, Object> result = new HashMap<String, Object>();
		// eventname = 1458215510951-3847.ics
		result.put("name", entry.getTitle());
		result.put("desc", entry.getDescription());
		result.put("where", entry.getLocation());

		result.put("from", removeTimeZoneIfRequired(entry.getStart(), isAllDay, isAllDay));
		result.put("to", removeTimeZoneIfRequired(entry.getEnd(), isAllDay, isAllDay));

		String legacyDateFormat = "yyyy-MM-dd";
		String legacyTimeFormat = "HH:mm";
		result.put("legacyDateFrom", removeTimeZoneIfRequired(entry.getStart(), isAllDay, isAllDay, legacyDateFormat));
		result.put("legacyTimeFrom", removeTimeZoneIfRequired(entry.getStart(), isAllDay, isAllDay, legacyTimeFormat));
		result.put("legacyDateTo", removeTimeZoneIfRequired(entry.getEnd(), isAllDay, isAllDay, legacyDateFormat));
		result.put("legacyTimeTo", removeTimeZoneIfRequired(entry.getEnd(), isAllDay, isAllDay, legacyTimeFormat));

		result.put("uri", "calendar/event/" + site.getShortName() + "/" + entry.getSystemName() + dateOpt);

		result.put("tags", entry.getTags());
		result.put("allday", isAllDay);
		result.put("docfolder", entry.getSharePointDocFolder());

		// Replace nulls with blank strings for the JSON
		for (String key : result.keySet()) {
			if (result.get(key) == null) {
				result.put(key, "");
			}
		}

		// All done
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("result", result);
		
		// SFDB extension
		if (cron != null) {
			result.put("cronExpr", cron);
			nodeService.setProperty(entry.getNodeRef(), CalendarCrons._IA_CRON, cron);
			calendarCrons.startEvent(entry.getNodeRef(), cron);
		}
		
		return model;
	}
}
