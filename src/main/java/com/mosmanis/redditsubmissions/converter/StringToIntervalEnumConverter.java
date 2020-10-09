package com.mosmanis.redditsubmissions.converter;

import org.springframework.core.convert.converter.Converter;

import com.mosmanis.redditsubmissions.enums.IntervalEnum;


public class StringToIntervalEnumConverter implements Converter<String, IntervalEnum>
{
	@Override
	public IntervalEnum convert(final String s)
	{
		return IntervalEnum.valueOf(s.toUpperCase());
	}
}
