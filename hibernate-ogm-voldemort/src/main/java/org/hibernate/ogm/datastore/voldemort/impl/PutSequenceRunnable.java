/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.ogm.datastore.voldemort.impl;

import java.util.Map;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

import voldemort.versioning.ObsoleteVersionException;
import voldemort.versioning.Version;
import voldemort.versioning.Versioned;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class PutSequenceRunnable implements Runnable {
	private static final Log log = LoggerFactory.make();
	private final VoldemortDatastoreProvider provider;
	private final Object key;
	private final Object value;
	private final String storeName;
	private final boolean keyByteArray;
	private final boolean valueByteArray;

	public PutSequenceRunnable(VoldemortDatastoreProvider provider,
			String storeName, Object key, Object value, boolean keyByteArray,
			boolean valueByteArray) {
		this.provider = provider;
		this.storeName = storeName;
		this.key = key;
		this.value = value;
		this.keyByteArray = keyByteArray;
		this.valueByteArray = valueByteArray;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		Versioned v = this.provider.getValue(this.storeName, this.key, true);
		log.info("v here: " + v);
		if (v != null) {
			Map<String, Integer> m = (Map<String, Integer>) v.getValue();
			log.info("m: " + m + " value: " + this.value);
			if ((Integer) m.get(VoldemortDatastoreProvider.SEQUENCE_LABEL) < (Integer) ((Map<String, Integer>) this.value)
					.get(VoldemortDatastoreProvider.SEQUENCE_LABEL)) {
				log.info("about to add key: " + this.key + " value: "
						+ this.value);
				this.provider.putValue(this.storeName, this.key, this.value,
						this.keyByteArray, this.valueByteArray);
			}
		} else {
			this.provider.putValue(this.storeName, this.key, this.value,
					this.keyByteArray, this.valueByteArray);
		}
	}

	@Override
	public String toString() {
		return "about to put: key: " + this.key + " value: " + this.value
				+ " store name: " + this.storeName;
	}
}
