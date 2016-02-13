package com.sfdb;

import java.io.IOException;

import org.alfresco.query.PagingResults;
import org.alfresco.service.cmr.discussion.DiscussionService;
import org.alfresco.service.cmr.discussion.PostInfo;
import org.alfresco.service.cmr.discussion.PostWithReplies;
import org.alfresco.service.cmr.discussion.TopicInfo;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.util.ScriptPagingDetails;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class DiscussionsCopyWebscript extends AbstractWebScript {

	private NodeService nodeService;
	private SearchService searchService;
	private DiscussionService discussionService;
	private SiteService siteService;

	protected static final int MAX_QUERY_ENTRY_COUNT = 1000;

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public void setDiscussionService(DiscussionService discussionService) {
		this.discussionService = discussionService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException, WebScriptException {
		JSONArray obj = new JSONArray();
		String sourcesite = req.getParameterValues("sourcesite") == null ? null
				: req.getParameterValues("sourcesite")[0];
		String targetsite = req.getParameterValues("targetsite") == null ? null
				: req.getParameterValues("targetsite")[0];
		if (sourcesite == null || targetsite == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "sourcesite or targetsite not defined");
		} else if (siteService.getSite(sourcesite) == null) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND,
					"sourcesite does not exist, site name =" + sourcesite);
		} else if (siteService.getSite(targetsite) == null) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND,
					"targetsite does not exist, site name =" + targetsite);
		}
		PagingResults<TopicInfo> topics = discussionService.listTopics(sourcesite, true,
				new ScriptPagingDetails(req, MAX_QUERY_ENTRY_COUNT));
		for (TopicInfo topic : topics.getPage()) {
			try {
				obj.put("copied topic: " + topic.getTitle());
				TopicInfo targetTopic = discussionService.createTopic(targetsite, topic.getTitle());
				PostInfo primaryPost = discussionService.getPrimaryPost(topic);
				PostInfo targetPost = discussionService.createPost(targetTopic, primaryPost.getContents());
				obj.put("copied primary post of topic: " + topic.getTitle());
				PostWithReplies pr = discussionService.listPostReplies(primaryPost, 10);
				recursiveReplies(pr, targetPost, obj, 1);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		String jsonString = obj.toString();
		res.getWriter().write(jsonString);
	}

	private void recursiveReplies(PostWithReplies post, PostInfo targetPost, JSONArray obj, int level)
			throws JSONException {
		for (PostWithReplies reply : post.getReplies()) {
			PostInfo newReply = discussionService.createReply(targetPost, reply.getPost().getContents());
			obj.put(level + " level reply copied");
			recursiveReplies(reply, newReply, obj, level + 1);
		}
	}
}
