package com.mosmanis.redditsubmissions.models;

import java.io.Serializable;

import com.mosmanis.redditsubmissions.enums.RedditEventTypeEnum;


public class RedditActivity implements Serializable
{
	long comments;
	long submissions;

	public RedditActivity(final long comments, final long submissions)
	{
		this.comments = comments;
		this.submissions = submissions;
	}

	public long getSubmissions()
	{
		return submissions;
	}

	public long getComments()
	{
		return comments;
	}

	public void increment(final String eventType)
	{
		switch (RedditEventTypeEnum.valueOf(eventType))
		{
			case RC:
				comments++;
				break;
			case RS:
				submissions++;
				break;
			default:
				break;
		}
	}
}
