package com.mosmanis.redditsubmissions;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mosmanis.redditsubmissions.enums.IntervalEnum;
import com.mosmanis.redditsubmissions.models.RedditActivity;
import com.mosmanis.redditsubmissions.models.RedditEntity;
import com.mosmanis.redditsubmissions.service.EventService;


/**
 * All endpoints have an optional parameter - interval
 * Supported values: 'minute' , 'five_minutes' , 'hour' , 'day' , 'all_time'
 * Not passing the parameter is equal to 'all_time'
 */
@RestController
@RequestMapping("/reddit")
public class RedditActivityController
{
	@Autowired
	EventService eventService;

	@GetMapping("/activity")
	public RedditActivity getRedditActivity(@RequestParam("interval") final Optional<IntervalEnum> interval)
	{
		if (interval.isEmpty() || IntervalEnum.ALL_TIME.equals(interval.get()))
		{
			return eventService.getAllTimeRedditActivity();
		}
		else
		{
			final long timestampFrom = getTimestampFrom(interval.get());
			return eventService.getRedditActivity(timestampFrom);
		}
	}

	@GetMapping("/subreddits")
	public List<RedditEntity> getTopSubreddits(@RequestParam("interval") final Optional<IntervalEnum> interval)
	{
		if (interval.isEmpty() || IntervalEnum.ALL_TIME.equals(interval.get()))
		{
			return eventService.getAllTimeTop100ActiveSubreddits();
		}
		else
		{
			final long timestampFrom = getTimestampFrom(interval.get());
			return eventService.getTop100ActiveSubreddits(timestampFrom);
		}
	}

	@GetMapping("/users")
	public List<RedditEntity> getTopUsers(@RequestParam("interval") final Optional<IntervalEnum> interval)
	{
		if (interval.isEmpty() || IntervalEnum.ALL_TIME.equals(interval.get()))
		{
			return eventService.getAllTimeTop100ActiveUsers();
		}
		else
		{
			final long timestampFrom = getTimestampFrom(interval.get());
			return eventService.getTop100ActiveUsers(timestampFrom);
		}
	}

	private long getTimestampFrom(final IntervalEnum interval)
	{
		final long now = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
		switch (interval)
		{
			case MINUTE:
				return now - TimeUnit.MINUTES.toSeconds(1);
			case FIVE_MINUTES:
				return now - TimeUnit.MINUTES.toSeconds(5);
			case HOUR:
				return now - TimeUnit.HOURS.toSeconds(1);
			case DAY:
				return now - TimeUnit.HOURS.toSeconds(24);
			default:
				return 0L;
		}
	}

	@ExceptionHandler(ConversionFailedException.class)
	public String handleConversionFailedException(final ConversionFailedException e)
	{
		if (IntervalEnum.class.equals(e.getTargetType().getType()))
		{
			final String[] intervalValues = Stream.of(IntervalEnum.values()).map(Enum::toString).toArray(String[]::new);
			final String availableIntervalParams = String.join(",", intervalValues);
			return String.format("No defined interval for '%s'. All available 'interval' values (case insensitive): %s",
					e.getValue(), availableIntervalParams);
		}
		return e.getMessage();
	}
}
