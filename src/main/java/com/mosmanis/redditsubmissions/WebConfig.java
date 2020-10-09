package com.mosmanis.redditsubmissions;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.mosmanis.redditsubmissions.converter.StringToIntervalEnumConverter;


@Configuration
public class WebConfig implements WebMvcConfigurer
{
	@Override
	public void addFormatters(final FormatterRegistry registry)
	{
		registry.addConverter(new StringToIntervalEnumConverter());
	}
}
