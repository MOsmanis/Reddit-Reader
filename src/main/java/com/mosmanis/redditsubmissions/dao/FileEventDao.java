package com.mosmanis.redditsubmissions.dao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.mosmanis.redditsubmissions.enums.RedditEventTypeEnum;
import com.mosmanis.redditsubmissions.models.EventEntry;
import com.mosmanis.redditsubmissions.models.RedditActivity;
import com.mosmanis.redditsubmissions.models.RedditEntity;
import com.mosmanis.redditsubmissions.service.PushshiftEventService;


@Component
public class FileEventDao implements EventDao
{
	private static final Logger LOG = Logger.getLogger(FileEventDao.class.getName());

	private static final String SUBMISSIONS_FILENAME = "rs_events";
	private static final String COMMENTS_FILENAME = "rc_events";
	private static final String ACTIVITY_FILENAME = "all_time_activity";
	private static final String SUBREDDIT_FILENAME = "all_time_subreddits";
	private static final String USER_FILENAME = "all_time_users";

	private static final int EVENT_CSV_TIMESTAMP = 0;
	private static final int EVENT_CSV_TYPE = 1;
	private static final int EVENT_CSV_USER = 2;
	private static final int EVENT_CSV_SUBREDDIT = 3;
	private static final int EVENT_CSV_LENGTH = 4;

	@Override
	public RedditActivity getRedditActivity(final long timestampFrom)
	{
		try (final BufferedReader submissionReader = new BufferedReader(
				getResourceInputStreamReader(SUBMISSIONS_FILENAME));
				final BufferedReader commentReader = new BufferedReader(
						getResourceInputStreamReader(COMMENTS_FILENAME)))
		{
			final long submissions = readRedditActivity(submissionReader, timestampFrom);
			final long comments = readRedditActivity(commentReader, timestampFrom);
			return new RedditActivity(comments, submissions);
		}
		catch (final NumberFormatException e)
		{
			LOG.log(Level.WARNING, "Could not parse a timestamp in file", e);
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
		return new RedditActivity(0L, 0L);
	}

	@Override
	public RedditActivity getAllTimeRedditActivity()
	{
		try (final BufferedReader reader = new BufferedReader(getResourceInputStreamReader(ACTIVITY_FILENAME)))
		{
			final String comments = reader.readLine();
			if (comments == null)
			{
				return new RedditActivity(0L, 0L);
			}
			else
			{
				final String submissions = reader.readLine();
				return new RedditActivity(Long.parseLong(comments), Long.parseLong(submissions));
			}
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
		return new RedditActivity(0L, 0L);
	}

	@Override
	public Collection<RedditEntity> getSubreddits(final long timestampFrom)
	{
		return getActivitiesForEntities(EVENT_CSV_SUBREDDIT, timestampFrom);
	}

	@Override
	public Collection<RedditEntity> getAllTimeSubreddits()
	{
		final Collection<RedditEntity> subreddits = new ArrayList<>();
		try (final BufferedReader reader = new BufferedReader(getResourceInputStreamReader(SUBREDDIT_FILENAME)))
		{

			String line;
			while ((line = reader.readLine()) != null)
			{
				final String[] subredditActivity = line.split(",");
				if (subredditActivity.length != 2)
				{
					continue;
				}
				subreddits.add(new RedditEntity(subredditActivity[0], Long.parseLong(subredditActivity[1])));
			}
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
		return subreddits;
	}

	@Override
	public Collection<RedditEntity> getUsers(final long timestampFrom)
	{
		return getActivitiesForEntities(EVENT_CSV_USER, timestampFrom);
	}

	@Override
	public Collection<RedditEntity> getAllTimeUsers()
	{
		final Collection<RedditEntity> users = new ArrayList<>();
		try (final BufferedReader reader = new BufferedReader(getResourceInputStreamReader(USER_FILENAME)))
		{

			String line;
			while ((line = reader.readLine()) != null)
			{
				final String[] userActivity = line.split(",");
				if (userActivity.length != 2)
				{
					continue;
				}
				users.add(new RedditEntity(userActivity[0], Long.parseLong(userActivity[1])));
			}
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
		return users;
	}

	@Override
	public void add(final EventEntry event)
	{
		if (RedditEventTypeEnum.RC.equals(event.getType()))
		{
			addToFile(event, COMMENTS_FILENAME);
		}
		else
		{
			addToFile(event, SUBMISSIONS_FILENAME);
		}

	}

	private void addToFile(final EventEntry event, final String filename)
	{
		try (final BufferedReader reader = new BufferedReader(getResourceInputStreamReader(filename));
				final BufferedWriter tempWriter = new BufferedWriter(new FileWriter(getTemporaryFile(filename))))
		{
			checkForLinesOlderThanDay(reader, tempWriter, filename);
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}

		try (final BufferedWriter writer = new BufferedWriter(
				new FileWriter(getResourceFileUrl(filename).getFile(), true)))
		{
			writer.write(event.toString());
			writer.newLine();
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
		addRedditActivity(event.getType());
		addSubredditActivity(event.getSubreddit());
		addUserActivity(event.getUser());
	}

	private long readRedditActivity(final BufferedReader reader, final long timestampFrom) throws IOException
	{
		long activity = 0L;
		Optional<String[]> eventCsvOptional = readFirstEventAfterTimestamp(timestampFrom, reader);
		if (eventCsvOptional.isEmpty())
		{
			return activity;
		}
		activity++;

		String line;
		while ((line = reader.readLine()) != null)
		{
			eventCsvOptional = validateEventCsv(line);
			if (eventCsvOptional.isEmpty())
			{
				continue;
			}
			activity++;
		}
		return activity;
	}

	private Collection<RedditEntity> getActivitiesForEntities(final int entityNameCsvIndex, final long timestampFrom)
	{
		try (final BufferedReader commentsReader = new BufferedReader(getResourceInputStreamReader(COMMENTS_FILENAME));
				final BufferedReader submissionsReader = new BufferedReader(
						getResourceInputStreamReader(SUBMISSIONS_FILENAME)))
		{
			final Map<String, RedditEntity> redditEntityMap = new HashMap<>();
			readActivitiesForEntities(entityNameCsvIndex, timestampFrom, commentsReader, redditEntityMap);
			readActivitiesForEntities(entityNameCsvIndex, timestampFrom, submissionsReader, redditEntityMap);
			return redditEntityMap.values();
		}
		catch (final NumberFormatException e)
		{
			LOG.log(Level.WARNING, "Could not parse timestamp in file", e);
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	private void readActivitiesForEntities(final int entityNameCsvIndex, final long timestampFrom,
			final BufferedReader reader, final Map<String, RedditEntity> redditEntityMap) throws IOException
	{
		Optional<String[]> eventCsvOptional = readFirstEventAfterTimestamp(timestampFrom, reader);
		if (eventCsvOptional.isEmpty())
		{
			return;
		}
		final String entityName = eventCsvOptional.get()[entityNameCsvIndex];
		redditEntityMap.put(entityName, new RedditEntity(entityName, 1L));

		String line;
		while ((line = reader.readLine()) != null)
		{
			eventCsvOptional = validateEventCsv(line);
			if (eventCsvOptional.isEmpty())
			{
				continue;
			}
			incrementActivityFor(eventCsvOptional.get()[entityNameCsvIndex], redditEntityMap);
		}
	}

	private void incrementActivityFor(final String entityName, final Map<String, RedditEntity> redditEntityMap)
	{
		if (redditEntityMap.containsKey(entityName))
		{
			final RedditEntity entity = redditEntityMap.get(entityName);
			redditEntityMap.put(entityName, entity.withIncrementedActivity());
		}
		else
		{
			redditEntityMap.put(entityName, new RedditEntity(entityName, 1L));
		}
	}

	private Optional<String[]> readFirstEventAfterTimestamp(final long timestampFrom, final BufferedReader reader)
			throws IOException
	{
		Optional<String[]> validCsvLineOptional = readTillValidCsvLine(reader);
		if (validCsvLineOptional.isEmpty())
		{
			return Optional.empty();
		}

		long timestamp = Long.parseLong(validCsvLineOptional.get()[EVENT_CSV_TIMESTAMP]);
		if (timestamp >= timestampFrom)
		{
			return validCsvLineOptional;
		}
		String line;
		while ((line = reader.readLine()) != null)
		{
			validCsvLineOptional = validateEventCsv(line);
			if (validCsvLineOptional.isEmpty())
			{
				continue;
			}
			timestamp = Long.parseLong(validCsvLineOptional.get()[EVENT_CSV_TIMESTAMP]);
			if (timestamp >= timestampFrom)
			{
				return validCsvLineOptional;
			}
		}
		return Optional.empty();
	}

	private Optional<String[]> readTillValidCsvLine(final BufferedReader reader) throws IOException
	{
		String line;
		while ((line = reader.readLine()) != null)
		{
			final Optional<String[]> validEventCsv = validateEventCsv(line);
			if (validEventCsv.isPresent())
			{
				return validEventCsv;
			}
		}
		return Optional.empty();
	}

	private Optional<String[]> validateEventCsv(final String line)
	{
		if (line == null)
		{
			return Optional.empty();
		}
		final String[] eventCsv = line.split(",");
		if (eventCsv.length == EVENT_CSV_LENGTH)
		{
			return Optional.of(eventCsv);
		}
		LOG.log(Level.WARNING, "Event record line does not contain {0} csv values : {1}",
				new Object[] { EVENT_CSV_LENGTH, line });
		return Optional.empty();
	}

	private InputStreamReader getResourceInputStreamReader(final String filename) throws IOException
	{
		final URL eventDataResource = getResourceFileUrl(filename);
		if (eventDataResource == null)
		{
			final File file = new File(
					PushshiftEventService.class.getClassLoader().getResource(".").getFile() + filename);
			if (!file.createNewFile())
			{
				throw new IllegalStateException("Could not find or create resource file : " + filename);
			}
			else
			{
				final FileInputStream fileInputStream = new FileInputStream(file);
				return new InputStreamReader(fileInputStream);
			}
		}
		return new InputStreamReader(eventDataResource.openStream());
	}

	private void getFileLock(final FileInputStream fileInputStream) throws IOException
	{

		final FileChannel channel = fileInputStream.getChannel();
		FileLock lock = null;
		try
		{
			lock = channel.tryLock(0L, Long.MAX_VALUE, true);
		}
		catch (final OverlappingFileLockException e)
		{
			fileInputStream.close();
			channel.close();
		}

	}

	private URL getResourceFileUrl(final String filename)
	{
		return PushshiftEventService.class.getClassLoader().getResource(filename);
	}

	private File getTemporaryFile(final String filename) throws IOException
	{
		return getResourceFile(filename + "_temp");
	}

	private File getResourceFile(final String filename) throws IOException
	{
		final URL temporaryResource = PushshiftEventService.class.getClassLoader().getResource(filename);
		if (temporaryResource == null)
		{
			final File file = new File(
					PushshiftEventService.class.getClassLoader().getResource(".").getFile() + filename);
			if (!file.createNewFile())
			{
				throw new IllegalStateException("Could not find or create resource file : " + filename);
			}
			else
			{
				return file;
			}
		}
		return new File(temporaryResource.getPath());
	}

	private void addSubredditActivity(final String subreddit)
	{
		final HashMap<String, RedditEntity> subredditHashmap = new HashMap<>();
		try (final BufferedReader reader = new BufferedReader(getResourceInputStreamReader(SUBREDDIT_FILENAME)))
		{

			String line;
			while ((line = reader.readLine()) != null)
			{
				final String[] subredditActivity = line.split(",");
				if (subredditActivity.length != 2)
				{
					continue;
				}
				subredditHashmap.put(subredditActivity[0],
						new RedditEntity(subredditActivity[0], Long.parseLong(subredditActivity[1])));
			}
			if (subredditHashmap.containsKey(subreddit))
			{
				final RedditEntity subredditEntity = subredditHashmap.get(subreddit);
				subredditHashmap.put(subreddit, subredditEntity.withIncrementedActivity());
			}
			else
			{
				subredditHashmap.put(subreddit, new RedditEntity(subreddit, 1L));
			}
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}

		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(getResourceFile(SUBREDDIT_FILENAME))))
		{
			for (final RedditEntity entity : subredditHashmap.values())
			{
				writer.write(String.format("%s,%s", entity.getName(), entity.getActivity()));
				writer.newLine();
			}
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void addUserActivity(final String user)
	{
		final HashMap<String, RedditEntity> userHashmap = new HashMap<>();
		try (final BufferedReader reader = new BufferedReader(getResourceInputStreamReader(USER_FILENAME)))
		{

			String line;
			while ((line = reader.readLine()) != null)
			{
				final String[] subredditActivity = line.split(",");
				if (subredditActivity.length != 2)
				{
					continue;
				}
				userHashmap.put(subredditActivity[0],
						new RedditEntity(subredditActivity[0], Long.parseLong(subredditActivity[1])));
			}
			if (userHashmap.containsKey(user))
			{
				final RedditEntity subredditEntity = userHashmap.get(user);
				userHashmap.put(user, subredditEntity.withIncrementedActivity());
			}
			else
			{
				userHashmap.put(user, new RedditEntity(user, 1L));
			}
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}

		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(getResourceFile(USER_FILENAME))))
		{
			for (final RedditEntity entity : userHashmap.values())
			{
				writer.write(String.format("%s,%s", entity.getName(), entity.getActivity()));
				writer.newLine();
			}
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void addRedditActivity(final RedditEventTypeEnum type)
	{
		RedditActivity redditActivity = new RedditActivity(0L, 0L);
		try (final BufferedReader reader = new BufferedReader(getResourceInputStreamReader(ACTIVITY_FILENAME)))
		{
			final String comments = reader.readLine();

			if (comments == null)
			{
				redditActivity.increment(type.toString());
			}
			else
			{
				final String submissions = reader.readLine();
				redditActivity = new RedditActivity(Long.parseLong(comments), Long.parseLong(submissions));
				redditActivity.increment(type.toString());
			}
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(getResourceFile(ACTIVITY_FILENAME))))
		{
			writer.write(String.valueOf(redditActivity.getComments()));
			writer.newLine();
			writer.write(String.valueOf(redditActivity.getSubmissions()));
		}
		catch (final IOException e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void checkForLinesOlderThanDay(final BufferedReader reader, final BufferedWriter tempWriter,
			final String filename) throws IOException
	{
		final Optional<String[]> validCsvLine = readTillValidCsvLine(reader);
		if (validCsvLine.isPresent())
		{
			final int lineTimestamp = Integer.parseInt(validCsvLine.get()[EVENT_CSV_TIMESTAMP]);

			final long nowTimestamp = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
			final long dayBeforeTimestamp = nowTimestamp - TimeUnit.HOURS.toSeconds(24);

			if (lineTimestamp < dayBeforeTimestamp)
			{
				deleteLinesOlderThanDay(reader, tempWriter, dayBeforeTimestamp, filename);
			}
		}
	}

	private void deleteLinesOlderThanDay(final BufferedReader reader, final BufferedWriter tempWriter,
			final long dayBeforeTimestamp, final String filename) throws IOException
	{
		final Optional<String[]> csvLine = readFirstEventAfterTimestamp(dayBeforeTimestamp, reader);
		if (csvLine.isEmpty())
		{
			reader.close();
			final File eventFile = new File(getResourceFileUrl(filename).getPath());
			final boolean deleted = eventFile.delete();
			if (deleted)
			{
				eventFile.createNewFile();
			}
		}
		else
		{
			tempWriter.write(String.join(",", csvLine.get()));
			tempWriter.newLine();
			String line;
			while ((line = reader.readLine()) != null)
			{
				tempWriter.write(line);
				tempWriter.newLine();
			}
			reader.close();
			final File eventFile = new File(getResourceFileUrl(filename).getPath());
			final Path tempFilePath = getTemporaryFile(filename).toPath();
			final boolean deleted = eventFile.delete();
			if (deleted)
			{
				Files.move(tempFilePath, tempFilePath.resolveSibling(eventFile.getName()));
				getTemporaryFile(filename).delete();
			}
		}
	}
}
