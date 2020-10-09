package com.mosmanis.redditsubmissions.models;

import java.io.Serializable;


// user or subreddit
public class RedditEntity implements Serializable
{
	private final String name;
	private long activity;

	public RedditEntity(final String name, final long activity)
	{
		this.name = name;
		this.activity = activity;
	}

	public RedditEntity withIncrementedActivity()
	{
		activity++;
		return this;
	}

	public int compareActivityDescending(final RedditEntity o)
	{
		return Long.compare(o.activity, activity);
	}

	public String getName()
	{
		return name;
	}

	public long getActivity()
	{
		return activity;
	}
}
