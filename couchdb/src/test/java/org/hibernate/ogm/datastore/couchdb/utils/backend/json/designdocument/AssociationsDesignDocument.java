/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.utils.backend.json.designdocument;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl.DesignDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

/**
 * Creates a CouchDB Design Document with a view and list used to retrieve the number of associations stored in the
 * database.
 * <p>
 * The map function of this view emits a record for each association. The reduce function counts the number of the
 * documents returned by the map function. The list function creates an easily consumable representation of the view
 * result.
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
public class AssociationsDesignDocument extends DesignDocument {

	public static final String DOCUMENT_ID = "associations";
	public static final String VIEW_NAME = "allAssociations";
	public static final String LIST_NAME = "count";

	/**
	 * The URL to use in the REST call in order to obtain the number of associations stored in the database
	 */
	public static final String ASSOCIATION_COUNT_PATH = "_design/" + DOCUMENT_ID + "/_list/" + LIST_NAME + "/" + VIEW_NAME;

	/**
	 * The JavaScript map function; for each association - embedded association, embedded collection or association in a
	 * dedicated document - value 1 will be emitted.
	 */
	private static final String MAP =
			"function(doc) {\n" +
			// association document
			"    if(doc." + Document.TYPE_DISCRIMINATOR_FIELD_NAME + " == \"" + AssociationDocument.TYPE_NAME + "\") {\n" +
			"        emit('association', 1);\n" +
			"    }\n" +
			// embedded association or collection; each embedded array is considered an association if it has a member named "..._id",
			// and as embedded collection otherwise
			"    else if(doc." + Document.TYPE_DISCRIMINATOR_FIELD_NAME + " == \"" + EntityDocument.TYPE_NAME + "\") {\n" +
			"        for(var propt in doc) {\n" +
			"            if( Object.prototype.toString.call( doc[propt] ) === '[object Array]' ) {\n" +
			"                var isAssociation = false;\n" +
			"                property:\n" +
			"                for (var i = 0; i < doc[propt].length; i++) {\n" +
			"                    for(var embeddedPropt in doc[propt][i]) {\n" +
			"                        if (embeddedPropt.indexOf(\"_id\") != -1) {\n" +
			"                            isAssociation = true;\n" +
			"                            break property;\n" +
			"                        }\n" +
			"                    }\n" +
			"                }\n" +
			"                if ( isAssociation) {\n" +
			"                    emit('inEntityAssociation', 1);\n" +
			"                }\n" +
			"                else {\n" +
			"                    emit('embeddedCollection', 1);\n" +
			"                }\n" +
			"            }\n" +
			"        }\n" +
			"    }\n" +
			"}";

	/**
	 * The JavaScript reduce function, return the length of the value returned by the map function, this value
	 * represents the number of the stored associations
	 */
	private static final String REDUCE = "_count";

	/**
	 * The JavaScript list function; simplifies the output of the view.
	 */
	private static final String LIST =
			"function (head, req) {\n" +
			"    associationDocumentCount = 0;\n" +
			"    inEntityAssociationCount = 0;\n" +
			"    embeddedCollectionCount = 0;\n" +
			"    while ( row = getRow() ) {\n" +
			"        if ( row.key == \"association\" ) {\n" +
			"            associationDocumentCount = row.value;\n" +
			"        }\n" +
			"        else if ( row.key == \"inEntityAssociation\" ) {\n" +
			"            inEntityAssociationCount = row.value;\n" +
			"        }\n" +
			"        else {\n" +
			"            embeddedCollectionCount = row.value;\n" +
			"        }\n" +
			"    }\n" +
			"    send(\n" +
			"        JSON.stringify( {\n" +
			"            associationDocumentCount : associationDocumentCount,\n" +
			"            inEntityAssociationCount : inEntityAssociationCount,\n" +
			"            embeddedCollectionCount : embeddedCollectionCount\n" +
			"        } )\n" +
			"    );\n" +
			"}";

	public AssociationsDesignDocument() {
		setId( DOCUMENT_ID );
		addView( VIEW_NAME, MAP, REDUCE );
		addList( LIST_NAME, LIST );
	}
}
