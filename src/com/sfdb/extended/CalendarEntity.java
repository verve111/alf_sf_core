package com.sfdb.extended;

public class CalendarEntity {
	
	public String cronExpr;
	public String creator;
	public CronScheduled cronScheduled;
	
	public CalendarEntity(CronScheduled cronScheduled, String cronExpr, String creator) {
		super();
		this.cronScheduled = cronScheduled; 
		this.cronExpr = cronExpr;
		this.creator = creator;
	}
}
