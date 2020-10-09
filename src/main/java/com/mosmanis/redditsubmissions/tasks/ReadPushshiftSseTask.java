package com.mosmanis.redditsubmissions.tasks;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.mosmanis.redditsubmissions.dao.EventDao;

import reactor.core.publisher.Flux;


@Component
public class ReadPushshiftSseTask implements Runnable
{
	private static final Logger LOG = Logger.getLogger(ReadPushshiftSseTask.class.getName());

	private static final String KEEPALIVE_EVENT_NAME = "keepalive";
	private static final long KEEPALIVE_DURATION = 31;

	@Autowired
	private EventDao eventDao;

	@Override
	public void run()
	{
		final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
		final WebClient sseClient = WebClient.create("http://stream.pushshift.io/"); //move to properties
		final Flux<ServerSentEvent<String>> eventStream = sseClient.get().retrieve()
				.bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>()
				{
				});//TODO do not push
		eventStream.timeout(Duration.ofSeconds(KEEPALIVE_DURATION))
				.subscribe(content -> saveOrSkip(taskExecutor, content),
						error -> LOG.log(Level.WARNING, "Error receiving Pushshift SSE", error),
						() -> LOG.log(Level.INFO, "Completed"));
	}

	private void saveOrSkip(final ExecutorService taskExecutor, final ServerSentEvent<String> content)
	{
		if (KEEPALIVE_EVENT_NAME.equals(content.event()))
		{
			return;
		}
		taskExecutor.submit(new SavePushshiftSseTask(eventDao, content));
	}
}
