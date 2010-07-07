package org.hibernate.ogm.type.descriptor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public class BasicGridExtractor<J> implements GridValueExtractor<J> {
	private static final Logger log = LoggerFactory.getLogger( BasicGridExtractor.class );
	private final GridTypeDescriptor gridTypeDescriptor;
	private final JavaTypeDescriptor<J> javaTypeDescriptor;

	public BasicGridExtractor( JavaTypeDescriptor<J> javaTypeDescriptor, GridTypeDescriptor gridTypeDescriptor ) {
		this.gridTypeDescriptor = gridTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public J extract(Map<String, Object> resultset, String name) {
		@SuppressWarnings( "unchecked" )
		final J result = (J) resultset.get( name );
		if ( result == null ) {
			log.trace( "found [null] as column [{}]", name );
			return null;
		}
		else {
			log.trace( "found [{}] as column [{}]", javaTypeDescriptor.extractLoggableRepresentation( result ), name );
			return result;
		}
	}
}
