package org.hibernate.ogm.test.simpleentity;

import java.io.File;

import org.hibernate.cfg.Environment;

public abstract class AbstractServer implements ServerAware {

	protected static final String DEBUG_LOCATION = Environment.getProperties().getProperty(
			"hibernate.ogm.datastore.provider_debug_location" );

	public boolean deleteDirectories(File directory) {
		if ( directory.isDirectory() ) {
			for ( String subDirectory : directory.list() ) {
				boolean done = deleteDirectories( new File( directory, subDirectory ) );
				if ( !done ) {
					return false;
				}
			}

		}
		return directory.delete();
	}

	public abstract boolean removeAllEntries();
}
