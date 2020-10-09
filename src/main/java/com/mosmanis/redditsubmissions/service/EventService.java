package com.mosmanis.redditsubmissions.service;

import java.util.List;

import com.mosmanis.redditsubmissions.models.RedditActivity;
import com.mosmanis.redditsubmissions.models.RedditEntity;


public interface EventService
{
	RedditActivity getRedditActivity(long timestampFrom);

	RedditActivity getAllTimeRedditActivity();

	List<RedditEntity> getTop100ActiveSubreddits(long timestampFrom);

	List<RedditEntity> getTop100ActiveUsers(long timestampFrom);

	List<RedditEntity> getAllTimeTop100ActiveSubreddits();

	List<RedditEntity> getAllTimeTop100ActiveUsers();
}
