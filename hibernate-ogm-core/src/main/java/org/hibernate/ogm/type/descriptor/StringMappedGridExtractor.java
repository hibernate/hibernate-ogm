package org.hibernate.ogm.type.descriptor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Nicolas Helleringer
 */
public class StringMappedGridExtractor<J> implements GridValueExtractor<J> {
	private static final Logger log = LoggerFactory.getLogger( BasicGridExtractor.class );
	private final GridTypeDescriptor gridTypeDescriptor;
	private final JavaTypeDescriptor<J> javaTypeDescriptor;

	public StringMappedGridExtractor( JavaTypeDescriptor<J> javaTypeDescriptor, GridTypeDescriptor gridTypeDescriptor ) {
		this.gridTypeDescriptor = gridTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public J extract(Map<String, Object> resultset, String name) {
		@SuppressWarnings( "unchecked" )
		final String result = (String) resultset.get( name );
		if ( result == null ) {
			log.trace( "found [null] as column [{}]", name );
			return null;
		}
		else {
			final J resultJ = javaTypeDescriptor.fromString(result);
			log.trace( "found [{}] as column [{}]", javaTypeDescriptor.extractLoggableRepresentation( resultJ ), name );
			return resultJ;
		}
	}
}
