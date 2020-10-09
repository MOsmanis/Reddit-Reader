package com.mosmanis.redditsubmissions.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mosmanis.redditsubmissions.dao.EventDao;
import com.mosmanis.redditsubmissions.models.RedditActivity;
import com.mosmanis.redditsubmissions.models.RedditEntity;


@Component
public class PushshiftEventService implements EventService
{
	@Autowired
	private EventDao eventDao;

	@Override
	public RedditActivity getRedditActivity(final long timestampFrom)
	{
		return eventDao.getRedditActivity(timestampFrom);
	}

	@Override
	public RedditActivity getAllTimeRedditActivity()
	{
		return eventDao.getAllTimeRedditActivity();
	}

	@Override
	public List<RedditEntity> getTop100ActiveSubreddits(final long timestampFrom)
	{
		final List<RedditEntity> subreddits = new ArrayList<>(eventDao.getSubreddits(timestampFrom));
		return getTop100RedditEntities(subreddits);
	}

	@Override
	public List<RedditEntity> getTop100ActiveUsers(final long timestampFrom)
	{
		final List<RedditEntity> users = new ArrayList<>(eventDao.getUsers(timestampFrom));
		return getTop100RedditEntities(users);
	}

	@Override
	public List<RedditEntity> getAllTimeTop100ActiveSubreddits()
	{
		final List<RedditEntity> subreddits = new ArrayList<>(eventDao.getAllTimeSubreddits());
		return getTop100RedditEntities(subreddits);
	}

	@Override
	public List<RedditEntity> getAllTimeTop100ActiveUsers()
	{
		final List<RedditEntity> users = new ArrayList<>(eventDao.getAllTimeUsers());
		return getTop100RedditEntities(users);
	}

	private List<RedditEntity> getTop100RedditEntities(final List<RedditEntity> users)
	{
		final Comparator<RedditEntity> comparator = Comparator.comparing(RedditEntity::getActivity).reversed()
				.thenComparing(RedditEntity::getName);
		users.sort(comparator);
		if (users.size() > 100)
		{
			return users.subList(0, 100);
		}
		return users;
	}
}
