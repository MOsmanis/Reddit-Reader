package com.mosmanis.redditsubmissions.dao;

import java.util.Collection;

import com.mosmanis.redditsubmissions.models.EventEntry;
import com.mosmanis.redditsubmissions.models.RedditActivity;
import com.mosmanis.redditsubmissions.models.RedditEntity;


public interface EventDao
{
	RedditActivity getRedditActivity(long timestampFrom);

	RedditActivity getAllTimeRedditActivity();

	Collection<RedditEntity> getSubreddits(long timestampFrom);

	Collection<RedditEntity> getAllTimeSubreddits();

	Collection<RedditEntity> getUsers(long timestampFrom);

	Collection<RedditEntity> getAllTimeUsers();

	void add(EventEntry event);
}
