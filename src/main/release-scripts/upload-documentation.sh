#!/usr/bin/env bash

RELEASE_VERSION=$1
VERSION_FAMILY=$2

unzip distribution/target/hibernate-ogm-$RELEASE_VERSION-dist.zip -d distribution/target/unpacked

# Add various metadata to the header

find distribution/target/unpacked/dist/docs/reference/ -name \*.html -exec sed -i 's@</title><link rel="stylesheet"@</title><!-- HibernateDoc.Meta --><meta name="description" content="Hibernate OGM, JPA for NoSQL datastores - Reference Documentation" /><meta name="keywords" content="hibernate, ogm, hibernate ogm, nosql, jpa, infinispan, mongodb, neo4j, cassandra, couchdb, ehcache, redis" /><meta name="viewport" content="width=device-width, initial-scale=1.0" /><link rel="canonical" href="https://docs.jboss.org/hibernate/stable/ogm/reference/en-US/html_single/" /><!-- /HibernateDoc.Meta --><link rel="stylesheet"@' {} \;

# Add the outdated content Javascript at the bottom of the pages

find distribution/target/unpacked/dist/docs/reference/ -name \*.html -exec sed -i 's@</body>@<!-- HibernateDoc.OutdatedContent --><script src="//code.jquery.com/jquery-3.1.0.min.js" integrity="sha256-cCueBR6CsyA4/9szpPfrX3s49M9vUU5BgtiJj06wt/s=" crossorigin="anonymous"></script><script src="/hibernate/_outdated-content/outdated-content.js" type="text/javascript"></script><script type="text/javascript">var jQuery_3_1 = $.noConflict(true); jQuery_3_1(document).ready(function() { HibernateDoc.OutdatedContent.install("ogm"); });</script><!-- /HibernateDoc.OutdatedContent --></body>@' {} \;

# Push the documentation to the doc server

rsync -rzh --progress --delete --protocol=28 distribution/target/unpacked/dist/docs/ filemgmt.jboss.org:/docs_htdocs/hibernate/ogm/$VERSION_FAMILY

# If the release is the new stable one, we need to update the doc server (outdated content descriptor and /stable/ symlink)

function version_gt() {
	test "$(echo "$@" | tr " " "\n" | sort -V | head -n 1)" != "$1";
}

if [[ $RELEASE_VERSION =~ .*\.Final ]]; then
	wget -q http://docs.jboss.org/hibernate/_outdated-content/ogm.json -O ogm.json
	if [ ! -s ogm.json ]; then
		echo "Error downloading the ogm.json descriptor. Exiting."
		exit 1
	fi
	CURRENT_STABLE_VERSION=$(cat ogm.json | jq -r ".stable")

	if [ "$CURRENT_STABLE_VERSION" != "$VERSION_FAMILY" ] && version_gt $VERSION_FAMILY $CURRENT_STABLE_VERSION; then
		cat ogm.json | jq ".stable = \"$VERSION_FAMILY\"" > ogm-updated.json
		if [ ! -s ogm-updated.json ]; then
			echo "Error updating the ogm.json descriptor. Exiting."
			exit 1
		fi

		scp ogm-updated.json filemgmt.jboss.org:docs_htdocs/hibernate/_outdated-content/ogm.json
		rm -f ogm-updated.json

		# update the symlink of stable to the latest release
		sftp filemgmt.jboss.org -b <<EOF
			cd docs_htdocs/hibernate/stable
			rm ogm
			ln -s ../ogm/$VERSION_FAMILY
		EOF
	fi
	rm -f ogm.json
fi

