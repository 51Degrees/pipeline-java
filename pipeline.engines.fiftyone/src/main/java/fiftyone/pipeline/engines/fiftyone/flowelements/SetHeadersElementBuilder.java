/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

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