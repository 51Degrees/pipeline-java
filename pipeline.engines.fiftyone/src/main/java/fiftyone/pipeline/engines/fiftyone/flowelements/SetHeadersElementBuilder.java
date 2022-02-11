package fiftyone.pipeline.engines.fiftyone.flowelements;

import org.slf4j.ILoggerFactory;

import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.fiftyone.data.SetHeadersData;

//! [class]
//! [constructor]
/**
 * Builder for {@link SetHeadersElement}
 */
@ElementBuilder
public class SetHeadersElementBuilder {
	private final ILoggerFactory loggerFactory;
	
	/**
	 * Constructor
	 * @param loggerFactory The logger factory to use.
	 */
	public SetHeadersElementBuilder(ILoggerFactory loggerFactory) {
		this.loggerFactory = loggerFactory;
	}
//! [constructor]

	/**
	 * Builder the {@link SetHeadersElement}
	 * @return {@link SetHeadersElement}
	 */
	public SetHeadersElement build() {
		return new SetHeadersElement(
			loggerFactory.getLogger(SetHeadersElement.class.getName()),
			new ElementDataFactory<SetHeadersData>() {
				@Override
				public SetHeadersData create(
					FlowData flowData,
					FlowElement<SetHeadersData, ?> flowElement) {
					return new SetHeadersData(
						loggerFactory.getLogger(
							SetHeadersData.class.getName()),
						flowData);
				}
			});
	}
}
//! [class]