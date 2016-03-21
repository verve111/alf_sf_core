package com.sfdb.extended;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.action.scheduled.CronScheduledQueryBasedTemplateActionDefinition;
import org.alfresco.service.cmr.repository.NodeRef;

public class CronScheduled extends CronScheduledQueryBasedTemplateActionDefinition {
	
	private NodeRef node;
	//private int counter;
	private String cronExpression;
	private String jobName;
	private String triggerName;
	
	public CronScheduled(NodeRef node, String cronExpr, int counter) {
		this.node = node;
		this.cronExpression = cronExpr;
		//this.counter = counter;
		jobName = "JobCalendarCronScheduled" + counter;		
		triggerName = "triggerCalendarCronScheduled" + counter;
	}
	
	@Override
	public String getCronExpression() {
		return cronExpression;
	}

	@Override
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	@Override
	public String getJobName() {
		return jobName;
	}
	
	@Override
	public String getTriggerName() {
		return triggerName;
	}
	
	@Override
	public List<NodeRef> getNodes() {
		List<NodeRef> list = new ArrayList<NodeRef>();
		if (node != null) {
			list.add(node);
		}
		return list;
	}
}
