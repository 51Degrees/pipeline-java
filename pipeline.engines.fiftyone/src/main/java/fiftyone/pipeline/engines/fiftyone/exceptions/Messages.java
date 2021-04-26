package fiftyone.pipeline.engines.fiftyone.exceptions;

public class Messages {
	/**
	 * Exception message when a SetHeader property does not have name starts
	 * with 'SetHeader'
	 */
	public static final String EXCEPTION_SET_HEADERS_NOT_SET_HEADER =
		"Property name '%s' does not start with 'SetHeader'";
	/**
	 * Exception message when a SetHeader property is in wrong format.
	 */
	public static final String EXCEPTION_SET_HEADERS_WRONG_FORMAT =
		"Property name '%s' is not in the expected format (SetHeader[Component][HeaderName])";
}
