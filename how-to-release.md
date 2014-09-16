# Releasing Hibernate OGM

## Requirements

Make sure you have:

1. **JDK 7** for the build (the created artifacts are compatible with Java 6)

2. **Maven** configured to use the JBoss repositories, with permissions to upload. Make sure your _settings.xml_ is configured accordingly or use the option _-s settings-example.xml_ when running the commands.

3. all the permissions required to upload the packages on:

  - [Nexus](https://repository.jboss.org/nexus/index.html): you can try to login on the Nexus web interface
  - [SourceForge](https://sourceforge.net): you need to have the authorization on the hibernate-ogm project
  - Documentation: you have to be able to connect via ssh to hibernate@filemgmt.jboss.org:/docs_htdocs/hibernate/ogm/[version]

## Release process

To prepare and release a new version of Hibernate OGM follow these steps (executed against the branch you intend to release):

### Preparation

Verify:

1. the project status on [Jenkins](http://ci.hibernate.org/view/OGM/)

2. there are no outstanding issues in JIRA

3. tests and artifacts:

   ```
       mvn clean install -s settings-example.xml
   ```

4. the distribution package as built by Maven (_distribution/target/hibernate-ogm-[version]-dist_).

   They should contain the appropriate dependencies, without duplicates. The creation of these directories is driven by the assembly plugin (see _distribution/src/main/assembly/dist.xml_) which is very specific and might break with the inclusion of new dependencies.

   Especially check the jar files in the subdirectories:
   - optional
   - required
   - provided

### Release

The Jenkins [release job](http://ci.hibernate.org/view/OGM/job/hibernate-ogm-release/) should be used for performing releases.
This parameterized job automates step 5 from this section as well as steps 1 and 2 from the "Publish" section.

1. [Release the version on JIRA](https://hibernate.atlassian.net/plugins/servlet/project-config/OGM/versions)

2. Update the _changelog.txt_ in project root from [JIRA's release notes](https://hibernate.atlassian.net/secure/ReleaseNote.jspa?projectId=10160)

3. Verify _readme.txt_:
   - content is up to date
   - links are not broken
   - current date and version number in the header

4. Commit any outstanding changes

5. Tag and build the release using the [maven release plugin](http://maven.apache.org/plugins/maven-release-plugin); During _release:prepare_ you will have to specify the tag name and release version:

   ```
       mvn release:prepare -s settings-example.xml
       mvn release:perform -s settings-example.xml
       git push upstream HEAD
       git push upstream <release-version>
   ```

6. Log in to [Nexus](https://repository.jboss.org/nexus):
   - check all artifacts you expect are there
   - close and release the repository. See [more details about using the staging repository](https://community.jboss.org/wiki/MavenDeployingARelease)
   - if there is a problem, drop the staging repo, fix the problem and re-deploy

### Publish

1. Upload the distribution packages to SourceForge (they should be under _target/checkout/target_). You need to be member of the Hibernate project team of Sourceforge to do that (Also see the [Sourceforge instructions](https://sourceforge.net/p/forge/documentation/Release%20Files%20for%20Download/)):
   - Copy the _changelog.txt_ (in _target/checkout/distribution/target/hibernate-ogm-[version]-dist_)
   - Copy the _readme.txt_ (in _target/checkout/distribution/target/hibernate-ogm-[version]-dist_)
   - Copy the _.zip distribution_ (in _target/checkout/distribution/target_)
   - Copy the _.tar.gz distribution_ (in _target/checkout/distribution/target_)
   - Copy the _.zip containing the JBoss Modules_. There are two .zip files:
     - for **EAP 6**: in _target/checkout/modules/eap6/target_
     - for **WildFly 8**: in _target/checkout/modules/wildfly/target_

1. Upload the documentation to [docs.jboss.org](http://docs.jboss.org/hibernate/ogm/). Do so using rsync (provided you are in the docs directory of the unpacked distribution):

   ```
       rsync -rzh --progress --delete \
             --protocol=28 docs/ hibernate@filemgmt.jboss.org:/docs_htdocs/hibernate/ogm/[version-family]
   ```

   or alternatively

   ```
       scp -r api hibernate@filemgmt.jboss.org:docs_htdocs/hibernate/ogm/[version-family]
       scp -r reference hibernate@filemgmt.jboss.org:docs_htdocs/hibernate/ogm/[version-family]
   ```

1. If it is a final release, you have to add the symbolic link _/docs_htdocs/hibernate/stable/ogm_.
   You can't create symlinks on the server so you either create it locally then rsync it up, or make a copy of the documentation in that URL.

1. Update the [community pages](http://community.jboss.org/en/hibernate/ogm).
In particular, update the [migration notes](https://community.jboss.org/wiki/HibernateOGMMigrationNotes).
When doing the latter, create an API change report by running `mvn clirr:clirr -pl core`.
The report created at _core/target/site/clirr-report.html_ provides an overview of all changed API/SPI types.
After you have created the change log, don't forget to update the _comparisonVersion_ in the configuration of the Maven Clirr plug-in in _pom.xml_.

### Announce

1. Blog about the release on [in.relation.to](http://in.relation.to/), make sure to use the tags **Hibernate OGM**, **Hibernate** and **news** for the blog entry.
   This way the blog will be featured on the [web-site](http://www.hibernate.org/ogm) and on the JBoss blog federation.

1. Update [hibernate.org](http://hibernate.org/) by adding a new release file to _data/projects/ogm/releases_.
   Remember to add a one line summary using the property _summary_.
   If you don't want to display an older release, set the property _displayed_ to false in the corresponding .yml file.
   When ready, deploy everything on production.

   Check:
   - http://www.hibernate.org/ogm/download
   - http://www.hibernate.org/ogm/documentation
 
1. Send email to _hibernate-dev_ and _hibernate-announce_.
   A quick sum up paragraph in the email is necessary before pointing to the blog entry.

1. Twitter
