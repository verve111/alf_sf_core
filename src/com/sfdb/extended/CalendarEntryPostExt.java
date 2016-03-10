package com.sfdb.extended;

import java.util.Map;

import org.alfresco.repo.web.scripts.calendar.CalendarEntryPost;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class CalendarEntryPostExt extends CalendarEntryPost {

	@Override
	protected Map<String, Object> executeImpl(SiteInfo arg0, String arg1, WebScriptRequest arg2, JSONObject arg3,
			Status arg4, Cache arg5) {
		System.out.println("da");
		return super.executeImpl(arg0, arg1, arg2, arg3, arg4, arg5);
	}
}
