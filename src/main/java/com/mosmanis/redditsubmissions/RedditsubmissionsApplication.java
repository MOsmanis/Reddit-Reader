package com.mosmanis.redditsubmissions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.mosmanis.redditsubmissions.tasks.ReadPushshiftSseTask;


@SpringBootApplication
public class RedditsubmissionsApplication
{

	@Autowired
	ReadPushshiftSseTask readPushshiftSseTask;

	public static void main(final String[] args)
	{
		SpringApplication.run(RedditsubmissionsApplication.class, args);
	}

	@PostConstruct
	private void runPushshiftReader()
	{
		final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
		taskExecutor.execute(readPushshiftSseTask);
	}
}
