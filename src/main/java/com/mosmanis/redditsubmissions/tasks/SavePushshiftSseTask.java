package com.mosmanis.redditsubmissions.tasks;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.codec.ServerSentEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mosmanis.redditsubmissions.dao.EventDao;
import com.mosmanis.redditsubmissions.enums.RedditEventTypeEnum;
import com.mosmanis.redditsubmissions.models.EventEntry;


public class SavePushshiftSseTask implements Runnable
{
	private static final Logger LOG = Logger.getLogger(SavePushshiftSseTask.class.getName());

	private final EventDao eventDao;
	private final ServerSentEvent<String> sse;

	public SavePushshiftSseTask(final EventDao eventDao, final ServerSentEvent<String> sse)
	{
		this.eventDao = eventDao;
		this.sse = sse;
	}

	@Override
	public void run()
	{
		try
		{
			final HashMap<String, Object> data = new ObjectMapper()
					.readValue(sse.data(), new TypeReference<HashMap<String, Object>>()
					{
					});//TODO do not push
			final String eventType = sse.event();
			if (eventType == null)
			{
				return;
			}
			final EventEntry event = new EventEntry((Integer) data.get("created_utc"),
					RedditEventTypeEnum.valueOf(eventType.toUpperCase()), (String) data.get("author"),
					(String) data.get("subreddit"));
			eventDao.add(event);
		}
		catch (final JsonProcessingException e)
		{
			LOG.log(Level.WARNING, "Could not parse sse 'data' json field : {0}", sse.data());
		}
	}
}
