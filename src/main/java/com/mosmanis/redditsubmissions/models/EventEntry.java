package com.mosmanis.redditsubmissions.models;

import com.mosmanis.redditsubmissions.enums.RedditEventTypeEnum;


public class EventEntry
{
	private final int timestamp;
	private final RedditEventTypeEnum type;
	private final String user;
	private final String subreddit;

	public EventEntry(final int timestamp, final RedditEventTypeEnum type, final String user, final String subreddit)
	{
		this.timestamp = timestamp;
		this.type = type;
		this.user = user;
		this.subreddit = subreddit;
	}

	public int getTimestamp()
	{
		return timestamp;
	}

	public RedditEventTypeEnum getType()
	{
		return type;
	}

	public String getUser()
	{
		return user;
	}

	public String getSubreddit()
	{
		return subreddit;
	}

	@Override
	public String toString()
	{
		return String.join(",", Integer.toString(timestamp), type.toString(), user, subreddit);
	}
}
